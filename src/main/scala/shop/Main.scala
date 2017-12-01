package shop

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import shop.actors.{PaymentService, PaymentServiceActor}

object Main extends App {

  val clientsystem = ActorSystem("main-system", ConfigFactory.load("client.conf"))
  val paymentService = clientsystem.actorOf(Props(new PaymentServiceActor(PaymentMethods.TransferPayment)))

  paymentService ! PaymentService.DoPayment(40)

}