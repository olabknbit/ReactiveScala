package shop.actors.events

import java.util.Date

import akka.actor.ActorRef
import shop.actors.Item

sealed trait CartEvent

object CartEvents {

  final case class ItemAdded(item: Item, date: Date) extends CartEvent

  final case class ItemRemoved(item: Item, count: Int, date: Date) extends CartEvent

  final case class CheckoutStarted(checkoutActor: ActorRef, date: Date) extends CartEvent

  case object CheckoutCancelled extends CartEvent

  case object CheckoutClosed extends CartEvent

  case object CartEmptied extends CartEvent
}