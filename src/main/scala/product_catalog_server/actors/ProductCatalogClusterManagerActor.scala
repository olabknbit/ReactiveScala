package product_catalog_server.actors

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.{Actor, ActorRef, ActorSystem, Props, Terminated}
import com.typesafe.config.ConfigFactory
import product_catalog_server.{BackendRegistration, ProductCatalogJobFailed, SearchForItems}

import scala.language.postfixOps

class ProductCatalogClusterManagerActor extends Actor {

  var backends = IndexedSeq.empty[ActorRef]
  var jobCounter = 0

  def receive = {
    case job: SearchForItems if backends.isEmpty =>
      sender() ! ProductCatalogJobFailed("Service unavailable, try again later", job)

    case job: SearchForItems =>
      jobCounter += 1
      backends(jobCounter % backends.size) forward job

    case BackendRegistration if !backends.contains(sender()) =>
      context watch sender()
      backends = backends :+ sender()

    case Terminated(a) =>
      backends = backends.filterNot(_ == a)
  }
}
//#frontend

object ProductCatalogClusterManagerActor {
  def run(args: Array[String]): ActorRef = {
    // Override the configuration of the port when specified as program argument
    val port = if (args.isEmpty) "0" else args(0)
    val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port").
      withFallback(ConfigFactory.parseString("akka.cluster.roles = [frontend]")).
      withFallback(ConfigFactory.load("cluster"))

    val system = ActorSystem("ClusterSystem", config)
    val frontend = system.actorOf(Props[ProductCatalogClusterManagerActor], name = "frontend")

    val counter = new AtomicInteger
    import system.dispatcher
    println("front is run")
    frontend
  }
}