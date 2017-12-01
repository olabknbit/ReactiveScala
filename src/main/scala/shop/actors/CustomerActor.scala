package shop.actors

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props, Stash}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import shop.actors.CartManagerActor.{AddItem, CheckoutClosed, RemoveItem}
import shop.{DeliveryMethod, PaymentMethod}

import scala.concurrent.duration._

class CustomerActor extends Actor with ActorLogging with Stash {

  import CustomerActor._

  private var cart: ActorRef = _
  private implicit val timeout: Timeout = 15 seconds
  private implicit val ec = context.dispatcher
  private implicit var cartManagerActorIds: Int = 0

  override def receive: Receive = {
    case Start =>
      log.info("CustomerActor started")
      cart = context.actorOf(Props(new CartManagerActor(cartManagerActorIds + 1, Cart.empty)))
      cartManagerActorIds += 1
      context.become(started)
  }

  private def started: Receive = {
    case AddItemToCart(item) =>
      cart ! AddItem(item)
      log.info("Adding item to cart")

    case RemoveItemFromCart(item, count) =>
      cart ! RemoveItem(item, count)

    case StartCartCheckout =>
      context.become(waitingForCheckoutStart)
      (cart ? CartManagerActor.StartCheckout)
        .mapTo[CheckoutStarted]
        .pipeTo(self)
  }

  private def waitingForCheckoutStart: Receive = {
    case CheckoutStarted(actorRef) =>
      log.info("Checkout Started")
      context.become(checkoutStarted(actorRef))
  }

  private def checkoutStarted(checkoutActor: ActorRef): Receive = {
    case SelectDeliveryMethod(method) =>
      log.info("Selecting delivery method")
      checkoutActor ! CheckoutActor.SelectDeliveryMethod(method)

    case SelectPaymentMethod(method) =>
      log.info("Selecting payment method")
      context.become(waitingForPaymentServiceStart)
      checkoutActor ! CheckoutActor.SelectPaymentMethod(method)

  }

  private def waitingForPaymentServiceStart: Receive = {
    case PaymentServiceStarted(paymentServiceActorRef) =>
      log.info("Payment service started")
      context.become(paymentServiceStarted(paymentServiceActorRef))
  }

  private def paymentServiceStarted(paymentServiceActorRef: ActorRef): Receive = {
    case DoPayment(amount) =>
      log.info("Payment made")
      context.become(waitingForPaymentConfirmation())
      (paymentServiceActorRef ? PaymentService.DoPayment(amount))
        .mapTo[PaymentConfirmed]
        .pipeTo(self)
  }

  private def waitingForPaymentConfirmation(): Receive = {
    case PaymentConfirmed() =>
      log.info("Payment confirmed")
      context.become(waitingForClosingCheckout())
      unstashAll()

    case PaymentUnsuccessful() =>
      log.info("Payment unsuccessful")
      context.become(errorState())

    case _ => stash()
  }

  private def waitingForClosingCheckout(): Receive = {
    case CheckoutClosed =>
      log.info("Checkout closed in customer")
      context.become(waitingForEmptyCart())
      unstashAll()

    case _ => stash()
  }

  private def waitingForEmptyCart(): Receive = {
    case CartEmpty =>
      log.info("Checkout finished")
      context.become(started)
  }

  private def errorState(): Receive = {
    case Finish =>
      log.info("Finishing after error")
      self ! PoisonPill
    case Start =>
      log.info("Starting again after error")
      context.become(started)
  }

  override def unhandled(message: Any): Unit = {
    log.info(s"unhandled message $message")
  }


}

object CustomerActor {

  case object Start

  final case class CheckoutStarted(checkoutActor: ActorRef)

  final case class AddItemToCart(item: Item)

  final case class RemoveItemFromCart(item: Item, count: Int)

  case object StartCartCheckout

  final case class SelectDeliveryMethod(deliveryMethod: DeliveryMethod)

  final case class SelectPaymentMethod(paymentMethod: PaymentMethod)

  final case class PaymentServiceStarted(paymentServiceActorRef: ActorRef)

  case class DoPayment(amount: Int)

  case object CartEmpty

  final case class PaymentConfirmed()

  final case class PaymentUnsuccessful()

  case object Finish
}