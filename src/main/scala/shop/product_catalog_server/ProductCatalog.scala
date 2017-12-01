package shop.product_catalog_server

import java.net.URI

import shop.actors.Item

import scala.collection.mutable.ListBuffer
import scala.io.Source

case class ProductCatalog(items: Map[URI, Item]) {
  def splitCatalog(no_routees: Int) = {
    var maps = items.splitAt(items.size / no_routees)
    var splitMaps = ListBuffer[Map[URI, Item]]()
    for (i <- 1 until no_routees) {
      splitMaps += maps._1
      maps = maps._2.splitAt(maps._2.size / (no_routees - i))
    }
    splitMaps.toList
  }

  def searchForItems(words: List[String]): CatalogSearchResults = {
    val ii = items.values.toList.sortWith(score(_, words) > score(_, words)).take(10)
    val scores = for (i <- ii) yield score(i, words)
    CatalogSearchResults(ii zip scores)
  }

  def score(item: Item, words: List[String]): Int = {
    var score = 0
    for (word <- words) {
      if (item.name.contains(word))
        score +=1
    }
    score
  }
}

object ProductCatalog {
  private val random = scala.util.Random
  val ready = ProductCatalog(
    Source.fromFile("src/main/resources/query_result")
      .getLines
      .toList
      .tail
      .map(line => Item(getURIFromLine(line), getNameFromLine(line), random.nextInt(100), random.nextInt(1000)))
      .map(item => item.id -> item)
      .toMap
  )

  private def getURIFromLine(line: String) = {
    val cols = line.split(",")
    if (cols.nonEmpty) {
      new URI(cols(0).replaceAll("\"", ""))
    }
    else {
      new URI("--")
    }
  }

  private def getNameFromLine(line: String) = {
    val cols = line.split(",")
    if (cols.size > 1) {
      cols(1).replaceAll("\"", "")
    }
    else {
      "--"
    }
  }
}

case class CatalogSearchResults(scoredItems: List[(Item, Int)])