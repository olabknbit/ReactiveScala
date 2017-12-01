package shop
import java.net.URI

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import shop.actors.Item
import shop.product_catalog_server.ProductCatalogManagerActor.{SearchForItems, SearchResults}
import shop.product_catalog_server.{CatalogSearchResults, ProductCatalog, ProductCatalogManagerActor}
import spray.json.{DefaultJsonProtocol, DeserializationException, JsArray, JsNumber, JsString, JsValue, RootJsonFormat}

import scala.concurrent.duration._
import scala.io.StdIn


case class Words(items: List[String])

// collect your json format instances into a support trait:
object JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val wordsFormat = jsonFormat1(Words) // contains List[String]

  implicit object ItemFormat extends RootJsonFormat[Item] {
    def write(i: Item) =
      JsArray(JsString(i.id.toString), JsString(i.name), JsNumber(i.price), JsNumber(i.count))

    def read(value: JsValue) = value match {
      case JsArray(Vector(JsString(id), JsString(name), JsNumber(price), JsNumber(count))) =>
        new Item(new URI(name), name, price, count.intValue())
      case _ => throw new DeserializationException("Item expected")
    }
  }
  implicit val catalogFormat = jsonFormat1(CatalogSearchResults)
  implicit val searchFromat = jsonFormat1(SearchResults)
}



object ProductCatalogServer extends App{
  import JsonSupport._
  implicit val system = ActorSystem("product_catalog_system", ConfigFactory.load("product_catalog_server.conf"))
  implicit val materializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.dispatcher
  private implicit val timeout: Timeout = 15 seconds
  val actorRef = system.actorOf(Props(new ProductCatalogManagerActor(ProductCatalog.ready)))
  val route =
    path("search") {
      put {
        decodeRequest {
          entity(as[Words]) { (words) ⇒
            val result = ToResponseMarshallable((actorRef ? SearchForItems(words.items)).mapTo[SearchResults])
            println(result)
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


