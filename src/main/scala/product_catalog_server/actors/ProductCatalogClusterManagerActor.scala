package product_catalog_server.actors

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.{Actor, ActorRef, ActorSystem, Props, Terminated}
import com.typesafe.config.ConfigFactory
import product_catalog_server.{ClusterNodeRegistration, ProductCatalogJobFailed, SearchForItems}

import scala.language.postfixOps

class ProductCatalogClusterManagerActor extends Actor {

  var clusterNodes = IndexedSeq.empty[ActorRef]
  var jobCounter = 0

  def receive = {
    case job: SearchForItems if clusterNodes.isEmpty =>
      sender() ! ProductCatalogJobFailed("Service unavailable, try again later", job)

    case job: SearchForItems =>
      jobCounter += 1
      clusterNodes(jobCounter % clusterNodes.size) forward job

    case ClusterNodeRegistration if !clusterNodes.contains(sender()) =>
      context watch sender()
      clusterNodes = clusterNodes :+ sender()

    case Terminated(a) =>
      clusterNodes = clusterNodes.filterNot(_ == a)
  }
}
//#frontend

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