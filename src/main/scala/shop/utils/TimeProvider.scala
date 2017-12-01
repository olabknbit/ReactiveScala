package shop.utils

import java.time.{LocalDateTime, ZoneId}
import java.util.Date

object TimeProvider {
  def current: Date = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant)
}
