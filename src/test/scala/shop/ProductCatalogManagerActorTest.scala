package shop

import java.net.URI

import akka.actor.{ActorSelection, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import shop.product_catalog_server.ProductCatalogManagerActor.Get
import shop.product_catalog_server.{ProductCatalog, ProductCatalogManagerActor}

class ProductCatalogManagerActorTest extends TestKit(ActorSystem("CartSystemSpec", ConfigFactory.load("client.conf")))
  with WordSpecLike with Matchers with BeforeAndAfterAll with ImplicitSender {

  "ProductCatalogActor" must {

    "be have items from file" in {
      val actorRef = TestActorRef[ProductCatalogManagerActor](Props(new ProductCatalogManagerActor(ProductCatalog.ready)))
      val actor = actorRef.underlyingActor
      actor.productCatalog.items(new URI("0000040822938")).name shouldBe "Fanta orange"
    }

    "be searchable" in {
      val actorRef = TestActorRef[ProductCatalogManagerActor](Props(new ProductCatalogManagerActor(ProductCatalog.ready)))
      val actor = actorRef.underlyingActor
      val list = actor.productCatalog.searchForItems(List("Fantasy"))
      list.scoredItems.head._1.name shouldBe "Fetish Fantasy Series Suede Fantasy Kit Purple"
      list.scoredItems.size shouldBe 10

    }

    "be the same as the one from remote selection" in {
      val actorRef = system.actorOf(Props(new ProductCatalogManagerActor(ProductCatalog.ready)))
      val actorSelection: ActorSelection = system.actorSelection("akka.tcp://actorSystem@127.0.0.1:2553/user/productCatalog")

      actorSelection ! Get shouldBe actorRef ! Get
    }

  }

  override def afterAll(): Unit = {
    system.terminate
  }
}