package de.crazything.app.test.ml

import de.crazything.app.analyze.NoLanguage
import de.crazything.app.helpers.DataProvider
import de.crazything.search.entity.SearchResult
import de.crazything.search.{CommonIndexer, CommonSearcher, QueryConfig}
import org.scalatest.{FlatSpec, Matchers}

import scala.util.Random

class FirstTest extends FlatSpec with Matchers with QueryConfig with NoLanguage {

  val preferredId = 4716

  val standardSlogan = Slogan(-1, "Roger", "Francis", "*innovate*", "*embrace*", "*systems*")

  CommonIndexer.index(DataProvider.readSlogans(), SloganFactory)

  def findPosition(searchResult: Seq[SearchResult[Int, Slogan]]): Int = searchResult.indexWhere(s => s.found.id == preferredId)

  private def search() = CommonSearcher.search(standardSlogan.copy(slogan1 = "*sexy*"), SloganFactory, maxHits = 1000)

  private val random = new Random()

  private def randomIp(): String = s"${random.nextInt(255)}.${random.nextInt(255)}.${random.nextInt(255)}.${random.nextInt(255)}"


  "Boost" should "promote a result due to users' acceptance" in {

    val initialSearchResult = search()
    val initialPosition = findPosition(initialSearchResult)
    assert(initialPosition == 54)

    for (_ <- 0 to 200) {
      val searchResult = search()
      val position = findPosition(searchResult)
      println(s"Position: $position")
      SloganFactory.notifyFeedback(randomIp(), position,1)
    }

    val searchResult = search()
    val position = findPosition(searchResult)
    assert(position <= 10)

  }

  it should "do nothing with only one ip" in {
    SloganFactory.resetTuning()
    val initialSearchResult = search()
    val initialPosition = findPosition(initialSearchResult)
    assert(initialPosition == 54)

    for (_ <- 0 to 200) {
      val searchResult = search()
      val position = findPosition(searchResult)
      SloganFactory.notifyFeedback("192.169.0.1", position,1)
    }

    val searchResult = search()
    val position = findPosition(searchResult)
    assert(position > 40)

  }
}
