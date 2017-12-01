package shop.actors.events

import java.util.Date

import akka.actor.ActorRef
import shop.{DeliveryMethod, PaymentMethod}

sealed trait CheckoutEvent

object CheckoutEvents {
  final case class CheckoutStarted(time: Date) extends CheckoutEvent

  final case class CheckoutCancelled(time: Date) extends CheckoutEvent

  final case class DeliveryMethodSelected(deliveryMethod: DeliveryMethod, time: Date) extends CheckoutEvent

  final case class PaymentMethodSelected(paymentMethod: PaymentMethod, time: Date, sender: ActorRef) extends CheckoutEvent

  final case class PaymentReceived(amount: Int, time: Date) extends CheckoutEvent

}
