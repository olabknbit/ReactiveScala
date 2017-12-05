package product_catalog_server.actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, RootActorPath}
import akka.cluster.ClusterEvent.{CurrentClusterState, MemberUp}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Subscribe, SubscribeAck}
import akka.cluster.{Cluster, Member, MemberStatus}
import com.typesafe.config.ConfigFactory
import product_catalog_server._
import product_catalog_server.actors.ProductCatalogStatsActor.{Stat, Stats}

class ProductCatalogStatsActor(val id: String, val system: ActorSystem) extends Actor with ActorLogging {
  val mediator = DistributedPubSub(system).mediator

  mediator ! Subscribe("stats", self)

  val cluster = Cluster(system)

  var stats = Map[String, Int]()

  override def preStart(): Unit = {
    cluster.subscribe(self, classOf[MemberUp])
  }

  override def postStop(): Unit = cluster.unsubscribe(self)

  override def receive: PartialFunction[Any, Unit] = {
    case s: String ⇒
      val count = stats.getOrElse(s, default = 0) + 1
      stats += (s -> count)

    case SubscribeAck(Subscribe("stats", None, `self`)) ⇒
      log.info("subscribing")

    case GetStats() =>
      val statistics = for {
        key <- stats.keySet
      } yield Stat(key, stats(key))
      sender() ! Stats(statistics.toList)

    case state: CurrentClusterState =>
      state.members.filter(_.status == MemberStatus.Up) foreach register

    case MemberUp(m) => register(m)
  }

  def register(member: Member): Unit =
    if (member.hasRole("clusterManager")) {
      system.actorSelection(RootActorPath(member.address) / "user" / "clusterManager") ! StatsActorRegistration(id)
    }
}

object ProductCatalogStatsActor {

  case class Stats(stats: List[Stat])

  case class Stat(id: String, count: Int)

  def main(args: Array[String]): Unit = {
    // Override the configuration of the port when specified as program argument
    val port = if (args.isEmpty) "0" else args(0)
    val id = if (args.length < 2) "0" else args(1)
    val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port").
      withFallback(ConfigFactory.parseString("akka.cluster.roles = [stats]")).
      withFallback(ConfigFactory.load("cluster"))

    val system: ActorSystem = ActorSystem("ClusterSystem", config)
    system.actorOf(Props(new ProductCatalogStatsActor(id, system)), name = "stats")
  }
}