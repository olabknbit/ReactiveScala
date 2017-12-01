package shop

import java.net.URI

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import shop.actors.CheckoutActor.SelectPaymentMethod
import shop.actors.CustomerActor._
import shop.actors.{CustomerActor, Item}

class CustomerActorTest extends TestKit(ActorSystem("CartSystemSpec", ConfigFactory.load("client.conf")))
  with WordSpecLike with Matchers with BeforeAndAfterAll with ImplicitSender {

  "CustomerActor" must {

    "Be able to do payments" in {
      val actorRef = system.actorOf(Props(new CustomerActor()))
      actorRef ! Start

      actorRef ! AddItemToCart(Item(new URI("item1"), "item1", BigDecimal.apply(100), 12))
      actorRef ! CustomerActor.StartCartCheckout
      expectMsgType[CheckoutStarted]
      actorRef ! CustomerActor.SelectDeliveryMethod(DeliveryMethods.CourierDelivery)
      actorRef ! SelectPaymentMethod(PaymentMethods.CashPayment)
      actorRef ! CustomerActor.DoPayment(50)
      expectMsgType[PaymentConfirmed]
    }

  }

  override def afterAll(): Unit = {
    system.terminate
  }
}