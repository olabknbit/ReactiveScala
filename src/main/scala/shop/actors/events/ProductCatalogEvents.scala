package shop.actors.events

import java.util.Date

import shop.actors.Item

sealed trait ProductCatalogEvent

object ProductCatalogEvents {

  final case class ItemsSearchedFor(items: Seq[Item], date: Date) extends ProductCatalogEvent

}