package shop.product_catalog_server

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import akka.routing._
import shop.actors._

import scala.collection.mutable.ListBuffer

class ProductCatalogManagerActor(var productCatalog: ProductCatalog) extends Actor with ActorLogging {
  import ProductCatalogManagerActor._
  val no_routees = 5
  var details: JobDetails = _ // id, number of of finished workers

  var router: Router = {
    val routees = ListBuffer[ActorRefRoutee]()
    for (p <- productCatalog.splitCatalog(no_routees); i <- 0 to no_routees) {
      val worker = context.actorOf(Props(new ProductCatalogWorkerActor(i, self, new ProductCatalog(p))))
      context watch worker
      routees += ActorRefRoutee(worker)
    }
    Router(BroadcastRoutingLogic(), routees.toIndexedSeq)
  }

  override def receive: PartialFunction[Any, Unit] = {
    case SearchForItems(words) =>
      details = JobDetails(0, CatalogSearchResults(List()), sender())
      router.route(ProductCatalogWorkerActor.SearchForItems(words), sender())

    case SearchResults(catalogSearchResults) =>
      val bestResults: CatalogSearchResults = mergeResults(details.bestResults, catalogSearchResults)
      if (details.workersFinished == no_routees) {
        details.sender ! bestResults.scoredItems.unzip._1
      } else {
        details = JobDetails(details.workersFinished + 1, bestResults, details.sender)
      }

    case Get => sender() ! ProductCatalogStatus(productCatalog)

    case Terminated(a) ⇒
      router = router.removeRoutee(a)
      val r = context.actorOf(Props[ProductCatalogWorkerActor])
      context watch r
      router = router.addRoutee(r)
  }

  def mergeResults(results: CatalogSearchResults, results1: CatalogSearchResults): CatalogSearchResults = {
    val items = results.scoredItems ++ results1.scoredItems
    CatalogSearchResults(items.sortWith((item1: (Item, Int), item2: (Item, Int)) => item1._2 > item2._2).take(10))
  }
}

object ProductCatalogManagerActor {

  final case class SearchForItems(words: List[String])

  case class SearchResults(catalogSearchResults: CatalogSearchResults)

  case object Get

  case class ProductCatalogStatus(productCatalog: ProductCatalog)
}

case class JobDetails(workersFinished: Int, bestResults: CatalogSearchResults, sender: ActorRef)