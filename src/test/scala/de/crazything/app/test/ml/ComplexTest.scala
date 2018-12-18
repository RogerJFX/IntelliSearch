package de.crazything.app.test.ml

import java.net.URL
import java.nio.file.{Files, Path, Paths}

import de.crazything.app.analyze.NoLanguage
import de.crazything.app.helpers.DataProvider
import de.crazything.search.entity.SearchResult
import de.crazything.search.ml.tuning.{DedicatedTuner, TunerConfig}
import de.crazything.search.{CommonIndexer, CommonSearcher, QueryConfig}
import de.crazything.service.QuickJsonParser
import org.scalatest.FlatSpec

import scala.util.Random

class ComplexTest extends FlatSpec with QueryConfig with NoLanguage with QuickJsonParser{

  val preferredId = 4716
  // 4716;Ted;Insley;streamline ubiquitous initiatives;enable sexy functionalities;whiteboard wireless systems
  val standardSlogan = Slogan(-1, "Roger", "Francis", "*innovate*", "*embrace*", "*systems*")

  val tunerConfig: TunerConfig = {
    import scala.collection.JavaConverters._
    val url: URL = this.getClass.getResource("/conf/complexSlogan.tuning.json")
    val path: Path = Paths.get(url.toURI)
    val lines: Seq[String] = Files.readAllLines(path).asScala
    val complete = lines.mkString("\n")
    jsonString2T[TunerConfig](complete)
  }

  val sloganFactory = new SloganFactory(new DedicatedTuner(tunerConfig))

  CommonIndexer.index(DataProvider.readSlogans(), sloganFactory)

  def findPosition(searchResult: Seq[SearchResult[Int, Slogan]]): Int = searchResult.indexWhere(s => s.found.id == preferredId)

  private def search() = CommonSearcher.search(standardSlogan.copy(slogan1 = "*sexy*"), sloganFactory, maxHits = 1000)

  private val random = new Random()

  private def randomIp(): String = s"${random.nextInt(255)}.${random.nextInt(255)}.${random.nextInt(255)}.${random.nextInt(255)}"


  "Boost" should "promote a result due to users' acceptance. Dedicated boosting, which here is a bad option." in {

    val initialSearchResult = search()
    val initialPosition = findPosition(initialSearchResult)
    assert(initialPosition == 48)

    for (_ <- 0 to 200) {
      val searchResult = search()
      val position = findPosition(searchResult)
      sloganFactory.notifyFeedback(randomIp(), position,1)
    }

    val searchResult = search()
    val position = findPosition(searchResult)
    println(s"Position: $position")

    // Yes, since we have a symmetrical test, this is the worse boost method here..
    assert(position <= 32)

  }

  it should "reset properly" in {
    sloganFactory.resetTuning()
    val initialSearchResult = search()
    val initialPosition = findPosition(initialSearchResult)
    assert(initialPosition == 48)
  }
}
