package shop

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory

import scala.io.StdIn

object PaymentServiceServer {
  def main(args: Array[String]) {

    implicit val system = ActorSystem("payment-system", ConfigFactory.load("payment_service_server.conf"))
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    val route =
      pathPrefix("doPayment" / IntNumber) { amount =>
        post {
          println(s"Got post request: paying $amount PLN")
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, amount.toString))
        }
      }

    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}