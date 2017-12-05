package product_catalog_server.utils

import java.net.URI

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import product_catalog_server.actors.ProductCatalogStatsActor.{Stat, Stats}
import product_catalog_server.{CatalogSearchResults, SearchResults}
import shop.actors.Item
import spray.json.{DefaultJsonProtocol, DeserializationException, JsArray, JsNumber, JsString, JsValue, RootJsonFormat}

case class Words(items: List[String])

object JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val wordsFormat: RootJsonFormat[Words] = jsonFormat1(Words)

  implicit object ItemFormat extends RootJsonFormat[Item] {
    def write(i: Item) =
      JsArray(JsString(i.id.toString), JsString(i.name), JsNumber(i.price), JsNumber(i.count))

    def read(value: JsValue): Item = value match {
      case JsArray(Vector(JsString(id), JsString(name), JsNumber(price), JsNumber(count))) =>
        Item(new URI(name), name, price, count.intValue())
      case _ => throw DeserializationException("Item expected")
    }
  }

  implicit val catalogFormat: RootJsonFormat[CatalogSearchResults] = jsonFormat1(CatalogSearchResults)
  implicit val searchFormat: RootJsonFormat[SearchResults] = jsonFormat1(SearchResults)
  implicit val statFormat: RootJsonFormat[Stat] = jsonFormat2(Stat)
  implicit val statsFormat: RootJsonFormat[Stats] = jsonFormat1(Stats)
}