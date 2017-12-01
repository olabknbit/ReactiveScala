package shop.actors

import java.net.URI
import java.util.Date
import scala.concurrent.duration._

import akka.actor.{ActorLogging, Props, Timers}
import akka.event.LoggingReceive
import akka.persistence.{PersistentActor, RecoveryCompleted}
import shop.actors.events.{CartEvent, CartEvents}
import shop.actors.events.CartEvents.{CartEmptied, CheckoutStarted, ItemAdded, ItemRemoved}
import shop.utils.TimeProvider

class CartManagerActor(id: Int, var cart: Cart, timeout: Int = 1200) extends PersistentActor with Timers with ActorLogging {

  import CartManagerActor._

  override def persistenceId = s"cart-$id"

  override def receiveCommand: Receive = empty

  private def empty: Receive = LoggingReceive {
    case AddItem(item) =>
      persist(ItemAdded(item, TimeProvider.current))(handleEvent)
      log.info(s"Item $item added to Cart")

    case Get => sender() ! CartStatus(cart)
  }

  private def nonEmpty: Receive = {
    case AddItem(item) =>
      persist(ItemAdded(item, TimeProvider.current))(handleEvent)
      log.info(s"Item ${item.id} added to Cart")

    case RemoveItem(item, count) =>
      persist(ItemRemoved(item, count, TimeProvider.current))(handleEvent)
      log.info(s"Item $id removed from Cart")

    case EmptyCart =>
      persist(CartEmptied)(handleEvent)
      log.info("Cart emptied")

    case StartCheckout =>
      log.info("Start checkout in cart")
      val checkoutActor = context.actorOf(Props(new CheckoutActor(10)))
      checkoutActor ! CheckoutActor.StartCheckout
      persist(CheckoutStarted(checkoutActor, TimeProvider.current))(handleEvent)

      deferAsync(id)(_ => sender() ! CustomerActor.CheckoutStarted(checkoutActor))
      log.info("after sending customer.checkoutStarted")

    case Get => sender() ! CartStatus(cart)
  }

  private def checkoutStarted: Receive = {
    case CancelCheckout =>
      log.info("Checkout cancelled")
      persist(CartEvents.CheckoutCancelled)(handleEvent)

    case CheckoutClosed =>
      log.info("Checkout closed in cart")
      persist(CartEvents.CheckoutClosed)(handleEvent)
      deferAsync(id) { _ =>
        context.parent ! CustomerActor.CartEmpty
      }
  }

  override def unhandled(message: Any): Unit = {
    log.warning(s"Unhandled message $message")
  }

  private def handleEvent(event: CartEvent): Unit = event match {
    case ItemAdded(item, date) =>
      timers.startSingleTimer(CartTimerKey, EmptyCart, getTimeout(date) seconds)
      cart = cart.addItem(item)
      context.become(nonEmpty)

    case ItemRemoved(item, count, date) =>
      timers.startSingleTimer(CartTimerKey, EmptyCart, getTimeout(date) seconds)
      cart = cart.removeItem(item, count)
      if(cart.isEmpty) {
        context.become(empty)
      } else context.become(nonEmpty)

    case CartEmptied =>
      cart = Cart.empty
      context.become(empty)

    case CartEvents.CheckoutClosed =>
      timers.cancel(CheckoutTimerKey)
      cart = Cart.empty
      context.become(empty)

    case CartEvents.CheckoutStarted(_, date) =>
      timers.cancel(CartTimerKey)
      timers.startSingleTimer(CheckoutTimerKey, EmptyCart, getTimeout(date) seconds)
      context.become(checkoutStarted)

    case CartEvents.CheckoutCancelled =>
      context.become(nonEmpty)

  }

  override def receiveRecover: PartialFunction[Any, Unit] = {
    case RecoveryCompleted => //ignore
    case event: CartEvent => handleEvent(event)
  }

  private def getTimeout(eventDate: Date) = Math.max((timeout * 1000 + eventDate.getTime - TimeProvider.current.getTime) / 1000, 0)

}

object CartManagerActor {

  final case class AddItem(item: Item)

  final case class RemoveItem(item: Item, count: Int)

  case object CheckoutClosed

  case object StartCheckout

  case object CancelCheckout

  case object Get

  private case object EmptyCart

  private case object CartTimerKey

  private case object CheckoutTimerKey

  case class CartStatus(cart: Cart)
}
/**
  * @param id: unique item identifier (java.net.URI)
  */
case class Item(id: URI, name: String, price: BigDecimal, count: Int)
case class Cart(items: Map[URI, Item]) {
  def addItem(it: Item): Cart = {
    val currentCount = if (items contains it.id) items(it.id).count else 0
    copy(items = items.updated(it.id, it.copy(count = currentCount + it.count)))
  }
  def removeItem(it: Item, cnt: Int): Cart = {
    val newCount = Math.max(items.get(it.id).map(_.count - cnt).getOrElse(0), 0)
    if(newCount == 0) {
      copy(items = items.filter(_._1 != it.id))
    } else copy(items = items.updated(it.id, it.copy(count = newCount)))
  }

  def isEmpty: Boolean = items.isEmpty
}

object Cart {
  val empty: Cart = Cart(Map.empty)
}