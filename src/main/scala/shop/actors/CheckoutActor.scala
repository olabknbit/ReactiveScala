package shop.actors

import java.util.Date

import akka.actor.{ActorLogging, ActorRef, Props, Timers}
import akka.event.LoggingReceive
import akka.persistence.{PersistentActor, RecoveryCompleted}
import shop.actors.CartManagerActor.CheckoutClosed
import shop.actors.events.CheckoutEvents.{CheckoutCancelled, CheckoutStarted, DeliveryMethodSelected, PaymentMethodSelected}
import shop.actors.events.{CheckoutEvent, CheckoutEvents}
import shop.utils.TimeProvider
import shop.{DeliveryMethod, PaymentMethod}

import scala.concurrent.duration._


class CheckoutActor(id: Int, timeout: Long = 1200) extends PersistentActor with Timers with ActorLogging {

  private var paymentMethod: Option[PaymentMethod] = _
  private var deliveryMethod: Option[DeliveryMethod] = _

  import CheckoutActor._

  override def persistenceId = s"checkout-$id"

  override def receiveRecover = {
    case RecoveryCompleted => //ignore

    case event: CheckoutEvent => handleEvent(event)
  }

  private def handleEvent(event: CheckoutEvent) = event match {
    case DeliveryMethodSelected(method, date) =>
      timers.cancel(PaymentTimerKey)
      timers.startSingleTimer(PaymentTimerKey, CancelCheckout, getTimeout(date) seconds)
      deliveryMethod = Some(method)
      context.become(selectingPaymentMethod)

    case PaymentMethodSelected(method, date, send) =>
      timers.startSingleTimer(PaymentTimerKey, CancelCheckout, getTimeout(date) seconds)
      paymentMethod = Some(method)
      context.become(processingPayment(send))

    case CheckoutStarted(date) =>
      timers.startSingleTimer(PaymentTimerKey, CancelCheckout, getTimeout(date) seconds)
      context.become(selectingDelivery)

    case CheckoutEvents.PaymentReceived(amount, date) =>
      timers.cancel(PaymentTimerKey)
      context.become(checkoutClosed)

    case CheckoutCancelled(date) =>
      context.become(checkoutCancelled)
  }

  override def receiveCommand: Receive = {
    case StartCheckout =>
      persist(CheckoutStarted(TimeProvider.current))(handleEvent)
  }

  private def selectingDelivery: Receive = common orElse {
    case SelectDeliveryMethod(method) =>
      log.info(s"Delivery method selected: $method")
      persist(DeliveryMethodSelected(method, TimeProvider.current))(handleEvent)
  }


  private def selectingPaymentMethod: Receive = common orElse {
    case SelectPaymentMethod(method) =>
      val send = sender()
      log.info(s"Payment method selected: $method")
      persist(PaymentMethodSelected(method, TimeProvider.current, send))(handleEvent)
      deferAsync(id){ _ =>
        val paymentServiceActor = context.actorOf(Props(new PaymentServiceActor(method)))
        send ! CustomerActor.PaymentServiceStarted(paymentServiceActor)
      }
  }


  private def processingPayment(customerActorRef: ActorRef): Receive = common orElse {
    case PaymentReceived(amount) =>
      persist(CheckoutEvents.PaymentReceived(amount, TimeProvider.current))(handleEvent)
      deferAsync(id) { _ =>
        log.info("Payment Received by checkout")
        customerActorRef ! CheckoutClosed
        context.parent ! CheckoutClosed
      }
  }


  private def common: Receive = LoggingReceive {
    case CancelCheckout =>
      persist(CheckoutEvents.CheckoutCancelled(TimeProvider.current))(handleEvent)
  }

  private def checkoutCancelled: Receive = {
    case _ => sender() ! CheckoutAlreadyCancelled
  }

  private def checkoutClosed: Receive = {
    case _ =>
  }

  override def unhandled(message: Any): Unit = {
    log.warning(s"Unhandled message $message")
  }

  private def getTimeout(eventDate: Date) = (timeout * 1000 + eventDate.getTime - TimeProvider.current.getTime) / 1000


}

object CheckoutActor {

  case object StartCheckout

  case object CancelCheckout

  final case class SelectDeliveryMethod(deliveryMethod: DeliveryMethod)

  final case class SelectPaymentMethod(paymentMethod: PaymentMethod)

  case class PaymentReceived(amount: Int)

  private case object PaymentTimerKey

  case object CheckoutAlreadyCancelled
}
