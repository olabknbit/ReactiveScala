package shop

import java.net.URI

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import shop.actors.CartManagerActor._
import shop.actors.CustomerActor.CheckoutStarted
import shop.actors.{Cart, CartManagerActor, Item}

class CartManagerActorTest extends TestKit(ActorSystem("CartSystemSpec", ConfigFactory.load("client.conf")))
  with WordSpecLike with Matchers with BeforeAndAfterAll with ImplicitSender {

  "CartActor" must {

    "send message CheckoutStarted " in {
      val uri = new URI("item1")
      val item = Item(uri, "item1", BigDecimal.apply(100), 12)
      val actorRef = system.actorOf(Props(new CartManagerActor(8, Cart.empty)))
      actorRef ! AddItem(item)
      actorRef ! StartCheckout
      expectMsgType[CheckoutStarted]
    }

    "be empty at start" in {
      val actorRef = TestActorRef[CartManagerActor](Props(new CartManagerActor(2, Cart.empty)))
      val actor = actorRef.underlyingActor
      actor.cart shouldBe empty
    }

    "add elements when message add item was received" in {
      val uri = new URI("item1")
      val item = Item(uri, "item1", BigDecimal.apply(100), 12)
      val actorRef = system.actorOf(Props(new CartManagerActor(9, Cart.empty)))
      actorRef ! AddItem(item)
      actorRef ! Get
      expectMsgPF() {
        case CartStatus(items) => items.items should have size 1
      }
    }

    "correctly recover persisted events" in {
      val uri = new URI("item1")
      val item = Item(uri, "item1", BigDecimal.apply(100), 12)
      val actorRef = system.actorOf(Props(new CartManagerActor(9, Cart.empty)))
      actorRef ! AddItem(item)
      Thread sleep 300
      system.stop(actorRef)
      val newActorRef = system.actorOf(Props(new CartManagerActor(9, Cart.empty)))
      Thread sleep 300
      newActorRef ! Get
      expectMsgPF() {
        case CartStatus(items) => items.items should have size 1
      }
    }

    "be emptied when timeout was exceeded" in {
      val actorRef = system.actorOf(Props(new CartManagerActor(4, Cart.empty, 2)))
      actorRef ! AddItem(Item(new URI("item2"), "item2", BigDecimal.apply(100), 12))
      actorRef ! AddItem(Item(new URI("item3"), "item3", BigDecimal.apply(100), 12))
      Thread sleep 2000
      actorRef ! Get
      expectMsgPF() {
        case CartStatus(items) => items.isEmpty shouldBe true
      }
    }

    "correctly persist timeout" in {
      val actorRef = system.actorOf(Props(new CartManagerActor(5, Cart.empty, 2)))
      actorRef ! AddItem(Item(new URI("item2"), "item2", BigDecimal.apply(100), 12))
      actorRef ! AddItem(Item(new URI("item3"), "item3", BigDecimal.apply(100), 12))
      Thread sleep 1000
      system.stop(actorRef)
      val newActorRef = system.actorOf(Props(new CartManagerActor(5, Cart.empty, 2)))
      Thread sleep 1200
      newActorRef ! Get
      expectMsgPF() {
        case CartStatus(items) => items.isEmpty shouldBe true
      }
    }

    "correctly remove item when message remove item was received" in {
      val actorRef = system.actorOf(Props(new CartManagerActor(7, Cart.empty, 2)))
      actorRef ! AddItem(Item(new URI("item1"), "item1", BigDecimal.apply(100), 12))
      actorRef ! RemoveItem(Item(new URI("item1"), "item1", BigDecimal.apply(100), 12), 12)
      actorRef ! Get
      expectMsgPF() {
        case CartStatus(items) => items.isEmpty shouldBe true
      }
    }

    "remove all items when checkout was successfully closed" in {
      val actorRef = system.actorOf(Props(new CartManagerActor(6, Cart.empty, 2)))
      actorRef ! AddItem(Item(new URI("item1"), "item1", BigDecimal.apply(100), 12))
      actorRef ! StartCheckout
      expectMsgType[CheckoutStarted]
      actorRef ! CheckoutClosed
      actorRef ! Get
      expectMsgPF() {
        case CartStatus(items) => items.isEmpty shouldBe true
      }
    }
  }

  override def afterAll(): Unit = {
    system.terminate
  }
}
