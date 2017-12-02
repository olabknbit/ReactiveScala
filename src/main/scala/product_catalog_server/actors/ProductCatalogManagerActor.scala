package product_catalog_server.actors

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, RootActorPath, Terminated}
import akka.cluster.ClusterEvent.{CurrentClusterState, MemberUp}
import akka.cluster.{Cluster, Member, MemberStatus}
import akka.routing._
import com.typesafe.config.ConfigFactory
import product_catalog_server._
import shop.actors._

import scala.collection.mutable.ListBuffer

class ProductCatalogManagerActor(val system: ActorSystem, var productCatalog: ProductCatalog) extends Actor with ActorLogging {

  import ProductCatalogManagerActor._

  val no_routees = 5
  var details: JobDetails = _ // id, number of of finished workers

  var router: Router = {
    val routees = ListBuffer[ActorRefRoutee]()
    for (p <- productCatalog.splitCatalog(no_routees); i <- 0 to no_routees) {
      val worker = system.actorOf(Props(new ProductCatalogWorkerActor(i, self, new ProductCatalog(p))))
      context watch worker
      routees += ActorRefRoutee(worker)
    }
    Router(BroadcastRoutingLogic(), routees.toIndexedSeq)
  }

  val cluster = Cluster(system)

  // subscribe to cluster changes, MemberUp
  // re-subscribe when restart
  override def preStart(): Unit = {
    println("preStart")
    cluster.subscribe(self, classOf[MemberUp])
  }

  override def postStop(): Unit = cluster.unsubscribe(self)

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

    case state: CurrentClusterState =>
      state.members.filter(_.status == MemberStatus.Up) foreach register

    case MemberUp(m) => register(m)
  }

  def register(member: Member): Unit =
    if (member.hasRole("frontend")) {
      println("Registering member")
      system.actorSelection(RootActorPath(member.address) / "user" / "frontend") ! BackendRegistration
    }

  def mergeResults(results: CatalogSearchResults, results1: CatalogSearchResults): CatalogSearchResults = {
    val items = results.scoredItems ++ results1.scoredItems
    CatalogSearchResults(items.sortWith((item1: (Item, Int), item2: (Item, Int)) => item1._2 > item2._2).take(10))
  }
}

object ProductCatalogManagerActor {
  case object Get

  case class ProductCatalogStatus(productCatalog: ProductCatalog)

  case class JobDetails(workersFinished: Int, bestResults: CatalogSearchResults, sender: ActorRef)

  def main(args: Array[String]): Unit = {
    // Override the configuration of the port when specified as program argument
    val port = if (args.isEmpty) "0" else args(0)
    val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port").
      withFallback(ConfigFactory.parseString("akka.cluster.roles = [backend]")).
      withFallback(ConfigFactory.load("cluster"))

    val system: ActorSystem = ActorSystem("ClusterSystem", config)
    system.actorOf(Props(new ProductCatalogManagerActor(system, ProductCatalog.ready)), name = "backend")
    println("Product Catalog Manager Actor")
  }
}