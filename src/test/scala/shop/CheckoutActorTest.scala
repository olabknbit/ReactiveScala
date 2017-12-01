package shop

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import shop.actors.CartManagerActor.CheckoutClosed
import shop.actors.CheckoutActor
import shop.actors.CheckoutActor._
import shop.actors.CustomerActor.PaymentServiceStarted

class CheckoutActorTest extends TestKit(ActorSystem("CartSystemSpec", ConfigFactory.load("client.conf"))) with WordSpecLike
  with Matchers with BeforeAndAfterAll with ImplicitSender {

  "CheckoutActor" must {

    "send message CheckoutCancelled when timer was exceeded" in {
      val actorRef = system.actorOf(Props(new CheckoutActor(13, 2)))
      actorRef ! StartCheckout
      Thread sleep 2000
      actorRef ! SelectDeliveryMethod(DeliveryMethods.CourierDelivery)
      expectMsg(CheckoutAlreadyCancelled)
    }

    "correctly save timers" in {
      val actorRef = system.actorOf(Props(new CheckoutActor(12, 2)))
      actorRef ! StartCheckout
      Thread sleep 1000
      system.stop(actorRef)
      val newActorRef = system.actorOf(Props(new CheckoutActor(12, 2)))
      Thread sleep 1200
      newActorRef ! SelectDeliveryMethod(DeliveryMethods.CourierDelivery)
      expectMsg(CheckoutAlreadyCancelled)
    }

    "send message PaymentServiceStarted when payment method was selected " in {
      val actorRef = system.actorOf(Props(new CheckoutActor(11)))
      actorRef ! StartCheckout
      actorRef ! SelectDeliveryMethod(DeliveryMethods.CourierDelivery)
      actorRef ! SelectPaymentMethod(PaymentMethods.CashPayment)
      expectMsgType[PaymentServiceStarted]
    }

    "send message CheckoutClosed when PaymentReceived was received " in {
      val actorRef = system.actorOf(Props(new CheckoutActor(10)))
      actorRef ! StartCheckout
      actorRef ! SelectDeliveryMethod(DeliveryMethods.CourierDelivery)
      actorRef ! SelectPaymentMethod(PaymentMethods.CashPayment)
      expectMsgType[PaymentServiceStarted]
      actorRef ! PaymentReceived(100)
      expectMsg(CheckoutClosed)
    }

    "send message CheckoutClosed to its parent when PaymentReceived was received" in {
      val parent = TestProbe()
      val checkout = parent.childActorOf(Props(new CheckoutActor(9)))
      checkout ! StartCheckout
      checkout ! SelectDeliveryMethod(DeliveryMethods.CourierDelivery)
      checkout ! SelectPaymentMethod(PaymentMethods.CashPayment)
      checkout ! PaymentReceived(100)
      parent.expectMsg(CheckoutClosed)
    }

  }

  override def afterAll(): Unit = {
    system.terminate
  }
}
