package shop

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import shop.actors.{CustomerActor, PaymentService, PaymentServiceActor}

class PaymentServiceActorTest extends TestKit(ActorSystem("PaymentServiceSpec", ConfigFactory.load("client.conf")))
  with WordSpecLike with Matchers with BeforeAndAfterAll with ImplicitSender {

  "PaymentServiceActor" must {

    "send message PaymentConfirmed" in {
      val actorRef = TestActorRef[PaymentServiceActor](Props(new PaymentServiceActor(PaymentMethods.TransferPayment)))
      actorRef ! PaymentService.DoPayment(40)
      expectMsgType[CustomerActor.PaymentConfirmed]
    }
  }

  override def afterAll(): Unit = {
    system.terminate
  }
}