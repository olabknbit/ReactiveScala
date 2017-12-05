package product_catalog_server

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class ProductCatalogServerTest extends TestKit(ActorSystem("CartSystemSpec", ConfigFactory.load("server.conf")))
  with WordSpecLike with Matchers with BeforeAndAfterAll with ImplicitSender {

  "ProductCatalogServer" must {

    // TODO write tests

  }

  override def afterAll(): Unit = {
    system.terminate
  }
}