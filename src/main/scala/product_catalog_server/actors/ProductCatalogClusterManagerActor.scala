package product_catalog_server.actors

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.{Actor, ActorRef, ActorSystem, Props, Terminated}
import com.typesafe.config.ConfigFactory
import product_catalog_server._

import scala.language.postfixOps

class ProductCatalogClusterManagerActor extends Actor {

  var clusterNodes = IndexedSeq.empty[ActorRef]
  var idsMap = Map[ActorRef, String]()
  var statsActorRef: ActorRef = _
  var loggingActorRef: ActorRef = _
  var jobCounter = 0

  def receive = {
    case job: SearchForItems if clusterNodes.isEmpty =>
      sender() ! ProductCatalogJobFailed("Service unavailable, try again later", job)

    case job: SearchForItems =>
      jobCounter += 1
      clusterNodes(jobCounter % clusterNodes.size) forward job

    case job: GetStats =>
      statsActorRef forward job

    case ClusterNodeRegistration(id: String) if !clusterNodes.contains(sender()) =>
      context watch sender()
      idsMap += (sender() -> id)
      clusterNodes = clusterNodes :+ sender()

    case StatsActorRegistration(id: String) =>
      context watch sender()
      idsMap += (sender() -> id)
      statsActorRef = sender()

    case LoggingActorRegistration(id: String) =>
      context watch sender()
      idsMap += (sender() -> id)
      loggingActorRef = sender()

    case Terminated(a) if clusterNodes.contains(a) =>
      clusterNodes = clusterNodes.filterNot(_ == a)
      ProductCatalogManagerActor.main(Seq("0", idsMap.getOrElse(a, default = "0")).toArray)

    case Terminated(a) if statsActorRef == a =>
      ProductCatalogStatsActor.main(Seq("0", idsMap.getOrElse(a, default = "stats")).toArray)

    case Terminated(a) if loggingActorRef == a =>
      ProductCatalogLoggingActor.main(Seq("0", idsMap.getOrElse(a, default = "logs")).toArray)
  }
}

object ProductCatalogClusterManagerActor {
  def run(args: Array[String]): ActorRef = {
    // Override the configuration of the port when specified as program argument
    val port = if (args.isEmpty) "0" else args(0)
    val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port").
      withFallback(ConfigFactory.parseString("akka.cluster.roles = [clusterManager]")).
      withFallback(ConfigFactory.load("cluster"))

    val system = ActorSystem("ClusterSystem", config)
    val clusterManager = system.actorOf(Props[ProductCatalogClusterManagerActor], name = "clusterManager")

    val counter = new AtomicInteger
    import system.dispatcher
    clusterManager
  }
}