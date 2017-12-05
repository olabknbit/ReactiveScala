package product_catalog_server

import java.net.URI

import akka.actor.{ActorSelection, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import product_catalog_server.actors.ProductCatalogManagerActor
import product_catalog_server.actors.ProductCatalogManagerActor.Get

class ProductCatalogManagerActorTest extends TestKit(ActorSystem("ClusterSystem", ConfigFactory.load("cluster.conf")))
  with WordSpecLike with Matchers with BeforeAndAfterAll with ImplicitSender {

  "ProductCatalogActor" must {

    "be have items from file" in {
      val actorRef = TestActorRef[ProductCatalogManagerActor](Props(new ProductCatalogManagerActor(system, ProductCatalog.ready)))
      val actor = actorRef.underlyingActor
      actor.productCatalog.items(new URI("0000040822938")).name shouldBe "Fanta orange"
    }

    "be searchable" in {
      val productCatalog = ProductCatalog.ready
      val list = productCatalog.searchForItems(List("Fanta", "Face", "Glow"))
      list.scoredItems.head._1.name shouldBe "Tantastic Face & Body Baked Bronzer Fantastic Sun Glow 03"
      list.scoredItems.size shouldBe 10

    }

    "be the same as the one from remote selection" in {
      val actorRef = system.actorOf(Props(new ProductCatalogManagerActor(system, ProductCatalog.ready)))
      val actorSelection: ActorSelection = system.actorSelection("akka.tcp://actorSystem@127.0.0.1:2553/user/productCatalog")

      actorSelection ! Get shouldBe actorRef ! Get
    }

  }

  override def afterAll(): Unit = {
    system.terminate
  }
}