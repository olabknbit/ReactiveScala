package shop.product_catalog_server

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.event.LoggingReceive
import shop.product_catalog_server.ProductCatalogManagerActor.SearchResults

class ProductCatalogWorkerActor(val id: Int, val creator: ActorRef, var productCatalog: ProductCatalog) extends Actor with ActorLogging {
  import ProductCatalogWorkerActor._

  def receive: Receive = LoggingReceive {
    case SearchForItems(words) =>
      log.info(s"I got to search for $words")
      sender() ! SearchResults(productCatalog.searchForItems(words))
  }
}

object ProductCatalogWorkerActor {
  case class SearchForItems(words: List[String])
}
