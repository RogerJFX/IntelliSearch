package de.crazything.app.test

import de.crazything.app.test.helpers.DataProvider
import de.crazything.app.{GermanLanguage, Person, PersonFactoryDE}
import de.crazything.search.entity.{QueryCriteria, SearchResult}
import de.crazything.search.{CommonIndexer, CommonSearcher, DirectoryContainer, QueryConfig}
import org.scalatest.{FlatSpec, Matchers}
import org.slf4j.LoggerFactory

import scala.collection.mutable.ListBuffer

class CommonSearchTest extends FlatSpec with Matchers with QueryConfig with GermanLanguage {

  private val logger = LoggerFactory.getLogger(classOf[CommonSearchTest])



  val results: ListBuffer[SearchResult[Int, Person]] = new ListBuffer[SearchResult[Int, Person]]()

  val standardPerson = Person(-1, "Herr", "firstName", "lastName", "street", "city")

  // Just to make sure, nothing unexpected happens during development.
  private[this] val expectedScores: Map[String, Float] = Map[String, Float](
    ("Reißer", 49.444176F), // => Reißer
    ("Rayßer-Fuzzy-HEAD", 16.06861F),
    ("Rayßer-Fuzzy-LAST", 0.9242671F), // => Mayer!
    ("Rayßer-Phon", 8.041645F), // => Reißer
    ("Raisr", 8.041645F),
    ("Müller-Lüdenscheidt", 57.003563F), // => Müller-Lüdenscheidt
    ("Muller-Ludenscheid", 13.08847F),
    ("Filosof", 8.041645F), // => Philosoph
    ("Theodor Wiesengrund Philosoph", 520.53973F),// => Theodor Wiesengrund Philosoph (Full match first and last name!)
    ("Theodor Wiesengrund Adorno", 79.63601F) // full match first name
  )

  private[this] def checkScore(name: String, score: Float) = assert(expectedScores(name) == score)

  CommonIndexer.index(DataProvider.readVerySimplePersons(), PersonFactoryDE)

  "Searcher" should "fail with no directory" in {
    val nullSearcherName = "SimpleTest-NullSearcher"
    DirectoryContainer.setDirectory(nullSearcherName, null)
    an [IllegalStateException] should be thrownBy {
      val searchResult: Seq[SearchResult[Int, Person]] = CommonSearcher.search(standardPerson.copy(lastName = "Reißer"), PersonFactoryDE,
        searcherOption = DirectoryContainer.pickSearcherForName(nullSearcherName))
      assert(searchResult.length == 1)
    }
  }

  it should "fail with unknown directory" in {
    val nullSearcherName = "SimpleTest-UnknownSearcher"
    an [IllegalStateException] should be thrownBy {
      val searchResult = CommonSearcher.search(standardPerson.copy(lastName = "Reißer"), PersonFactoryDE,
        searcherOption = DirectoryContainer.pickSearcherForName(nullSearcherName))
      assert(searchResult.length == 1)
    }
  }

  "Persons" should "find Reißer" in {
    val searchResult = CommonSearcher.search(standardPerson.copy(lastName = "Reißer"), PersonFactoryDE)
    logger.debug(s"Reißer: $searchResult")
    assert(searchResult.length == 1)
    checkScore("Reißer", searchResult.head.score)
    results.appendAll(searchResult)
  }

  it should "find two results for Rayßer" in {
    val searchResult = CommonSearcher.search(standardPerson.copy(lastName = "Rayßer"), PersonFactoryDE)
    logger.debug(s"Rayßer (with fuzzy): $searchResult")
    assert(searchResult.length == 2)
    checkScore("Rayßer-Fuzzy-HEAD", searchResult.head.score)
    checkScore("Rayßer-Fuzzy-LAST", searchResult.last.score)
    results.appendAll(searchResult)
  }

  it should "find one result for Rayßer" in {
    val searchResult = CommonSearcher.search(standardPerson.copy(lastName = "Rayßer"), PersonFactoryDE,
      Some(QueryCriteria(PersonFactoryDE.customEnabledQuery_Name, Some(QueryEnabled.EXACT | QueryEnabled.PHONETIC))))
    logger.debug(s"Rayßer (without fuzzy): $searchResult")
    assert(searchResult.length == 1)
    checkScore("Rayßer-Phon", searchResult.head.score)
    results.appendAll(searchResult)
  }

  it should "find Raisr" in {
    val searchResult = CommonSearcher.search(standardPerson.copy(lastName = "Raisr"), PersonFactoryDE)
    logger.debug(s"Raisr: $searchResult")
    assert(searchResult.length == 1)
    checkScore("Raisr", searchResult.head.score)
    results.appendAll(searchResult)
  }

  it should "find Müller-Lüdenscheidt" in {
    val searchResult = CommonSearcher.search(standardPerson.copy(lastName = "Müller-Lüdenscheidt"), PersonFactoryDE)
    logger.debug(s"Müller-Lüdenscheidt: $searchResult")
    assert(searchResult.length == 1)
    checkScore("Müller-Lüdenscheidt", searchResult.head.score)
    results.appendAll(searchResult)
  }

  it should "find Muller-Ludenscheid" in {
    val searchResult = CommonSearcher.search(standardPerson.copy(lastName = "Muller-Ludenscheid"), PersonFactoryDE)
    logger.debug(s"Muller-Ludenscheid: $searchResult")
    assert(searchResult.length == 1)
    checkScore("Muller-Ludenscheid", searchResult.head.score)
    results.appendAll(searchResult)
  }

  it should "find Filosof" in {
    val searchResult = CommonSearcher.search(standardPerson.copy(lastName = "Filosof"), PersonFactoryDE)
    logger.debug(s"Filosof: $searchResult")
    assert(searchResult.length == 1)
    checkScore("Filosof", searchResult.head.score)
    results.appendAll(searchResult)
  }

  it should "fail for 'Dr. Klöbner'" in {
    val searchResult = CommonSearcher.search(standardPerson.copy(lastName = "Dr. Klöbner"), PersonFactoryDE)
    logger.debug(s"Dr. Klöbner: $searchResult")
    assert(searchResult.isEmpty) // Nothing found => fail.
  }

  it should "fail fuzzy Filosof" in {
    val searchResult = CommonSearcher.search(standardPerson.copy(lastName = "Filosof"), PersonFactoryDE,
      Some(QueryCriteria(PersonFactoryDE.customEnabledQuery_Name, Some(QueryEnabled.FUZZY))))
    logger.debug(s"Filosof (only fuzzy): $searchResult")
    assert(searchResult.isEmpty) // Nothing found => fail.
  }

  it should "find Theodor Wiesengrund Philosoph" in {
    val searchResult = CommonSearcher.search(standardPerson.copy(lastName = "Philosoph", firstName="Theodor Wiesengrund"), PersonFactoryDE,
      Some(QueryCriteria(PersonFactoryDE.customQuery_FirstAndLastName)))
    logger.debug(s"Theodor Wiesengrund Philosoph: $searchResult")
    assert(searchResult.length == 1)
    checkScore("Theodor Wiesengrund Philosoph", searchResult.head.score)
    results.appendAll(searchResult)
  }

  it should "even find Theodor Wiesengrund Adorno" in {
    val searchResult = CommonSearcher.search(standardPerson.copy(lastName = "Adorno", firstName="Theodor Wiesengrund"), PersonFactoryDE,
      Some(QueryCriteria(PersonFactoryDE.customQuery_FirstAndLastName)))
    logger.debug(s"Theodor Wiesengrund Adorno: $searchResult")
    assert(searchResult.length == 1)
    checkScore("Theodor Wiesengrund Adorno", searchResult.head.score)
    results.appendAll(searchResult)
  }

  "Results" should "be reasonably sorted" in {
    assert(results.length == 10)
    val sorted: Seq[SearchResult[Int, Person]] = results.sortBy(r => -r.score)
    assert(sorted.head.found.lastName == "Philosoph" && sorted.head.found.firstName == "Theodor Wiesengrund") // Better than Raißer due to more characters.
    assert(sorted.last.found.lastName == "Mayer") // we never searched for Mayer, so this is a weak guess (fuzzy)
  }

}
