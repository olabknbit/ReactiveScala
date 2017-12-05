package product_catalog_server.simulations

import product_catalog_server.utils.Words

import scala.util.Random

object TestData extends SimulationConfig {

  val words = getRequiredStringList("data.words")

  def getSearchWords: Words = {
    val num = Random.nextInt(4) + 1
    val wordsList = for {
      i <- 1 until num
    } yield words.get(Random.nextInt(words.size))
    print(wordsList.toList)
    Words(wordsList.toList)
  }

  val searchWordsLists = List.fill(threads)(getSearchWords)
}