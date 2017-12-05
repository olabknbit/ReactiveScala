package product_catalog_server

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import product_catalog_server.actors.{ProductCatalogClusterManagerActor, ProductCatalogManagerActor}
import product_catalog_server.utils.Words

import scala.concurrent.duration._
import scala.io.StdIn


object ProductCatalogServer extends App {

  import product_catalog_server.utils.JsonSupport._

  implicit val system = ActorSystem("ClusterSystem", ConfigFactory.load("cluster.conf"))
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher
  private implicit val timeout: Timeout = 15 seconds
  val actorRef = ProductCatalogClusterManagerActor.run(Seq("2555").toArray)
  ProductCatalogManagerActor.main(Seq("2556").toArray)
  ProductCatalogManagerActor.main(Array.empty)
  ProductCatalogManagerActor.main(Array.empty)

  val route =
    path("search") {
      put {
        decodeRequest {
          entity(as[Words]) { (words) ⇒
            val result = ToResponseMarshallable((actorRef ? SearchForItems(words.items)).mapTo[SearchResults])
            complete(result)
          }
        }
      }
    }

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8081)

  println(s"Server online at http://localhost:8081/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done
}


