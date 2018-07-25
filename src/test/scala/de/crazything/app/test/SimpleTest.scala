package de.crazything.app.test

import de.crazything.app.test.helpers.DataProvider
import de.crazything.app.{Person, PersonFactory}
import de.crazything.search.{GermanIndexer, GermanSearcher, QueryConfig, SearchResult}
import org.scalatest.FlatSpec
import org.slf4j.LoggerFactory

import scala.collection.mutable.ListBuffer

class SimpleTest extends FlatSpec with QueryConfig{

  private val logger = LoggerFactory.getLogger(classOf[SimpleTest])

  GermanIndexer.index(DataProvider.readPersons(), PersonFactory)

  val results: ListBuffer[SearchResult[Int, Person]] = new ListBuffer[SearchResult[Int, Person]]()

  // Just to make sure, nothing unexpected happens during development.
  private[this] val expectedScores: Map[String, Float] = Map[String, Float](
    ("Reißer", 31.370277F), // => Reißer
    ("Rayßer-Fuzzy-HEAD", 15.207415F),
    ("Rayßer-Fuzzy-LAST", 0.8317767F), // => Mayer!
    ("Rayßer-Phon", 7.2832184F), // => Reißer
    ("Raisr", 7.2832184F),
    ("Müller-Lüdenscheidt", 38.248436F), // => Müller-Lüdenscheidt
    ("Muller-Ludenscheid", 11.901843F),
    ("Filosof", 7.2832184F) // => Philosoph
  )

  private[this] def checkScore(name: String, score: Float) = assert(expectedScores(name) == score)

  "Persons" should "find Reißer" in {
    val searchResult = GermanSearcher.search(Person(-1, "Herr", "firstName", "Reißer", "street", "city"), PersonFactory)
    logger.debug(s"Reißer: $searchResult")
    assert(searchResult.length == 1)
    checkScore("Reißer", searchResult.head.score)
    results.appendAll(searchResult)
  }

  it should "find two results for Rayßer" in {
    val searchResult = GermanSearcher.search(Person(-1, "Herr", "firstName", "Rayßer", "street", "city"), PersonFactory)
    logger.debug(s"Rayßer (with fuzzy): $searchResult")
    assert(searchResult.length == 2)
    checkScore("Rayßer-Fuzzy-HEAD", searchResult.head.score)
    checkScore("Rayßer-Fuzzy-LAST", searchResult.last.score)
    results.appendAll(searchResult)
  }

  it should "find one result for Rayßer" in {
    val searchResult = GermanSearcher.search(Person(-1, "Herr", "firstName", "Rayßer", "street", "city"), PersonFactory,
      Some(QueryEnabled.EXACT | QueryEnabled.PHONETIC))
    logger.debug(s"Rayßer (without fuzzy): $searchResult")
    assert(searchResult.length == 1)
    checkScore("Rayßer-Phon", searchResult.head.score)
    results.appendAll(searchResult)
  }

  it should "find Raisr" in {
    val searchResult = GermanSearcher.search(Person(-1, "Herr", "firstName", "Raisr", "street", "city"), PersonFactory)
    logger.debug(s"Raisr: $searchResult")
    assert(searchResult.length == 1)
    checkScore("Raisr", searchResult.head.score)
    results.appendAll(searchResult)
  }

  it should "find Müller-Lüdenscheidt" in {
    val searchResult = GermanSearcher.search(Person(-1, "Herr", "firstName", "Müller-Lüdenscheidt", "street", "city"), PersonFactory)
    logger.debug(s"Müller-Lüdenscheidt: $searchResult")
    assert(searchResult.length == 1)
    checkScore("Müller-Lüdenscheidt", searchResult.head.score)
    results.appendAll(searchResult)
  }

  it should "find Muller-Ludenscheid" in {
    val searchResult = GermanSearcher.search(Person(-1, "Herr", "firstName", "Muller-Ludenscheid", "street", "city"), PersonFactory)
    logger.debug(s"Muller-Ludenscheid: $searchResult")
    assert(searchResult.length == 1)
    checkScore("Muller-Ludenscheid", searchResult.head.score)
    results.appendAll(searchResult)
  }

  it should "find Filosof" in {
    val searchResult = GermanSearcher.search(Person(-1, "Herr", "firstName", "Filosof", "street", "city"), PersonFactory)
    logger.debug(s"Filosof: $searchResult")
    assert(searchResult.length == 1)
    checkScore("Filosof", searchResult.head.score)
    results.appendAll(searchResult)
  }

  it should "fail for 'Dr. Klöbner'" in {
    val searchResult = GermanSearcher.search(Person(-1, "Herr", "firstName", "Dr. Klöbner", "street", "city"), PersonFactory)
    logger.debug(s"Dr. Klöbner: $searchResult")
    assert(searchResult.isEmpty) // Nothing found => fail.
  }

  it should "fail fuzzy Filosof" in {
    val searchResult = GermanSearcher.search(Person(-1, "Herr", "firstName", "Filosof", "street", "city"), PersonFactory,
      Some(QueryEnabled.FUZZY))
    logger.debug(s"Filosof (only fuzzy): $searchResult")
    assert(searchResult.isEmpty) // Nothing found => fail.
  }

  "Results" should "be reasonably sorted" in {
    assert(results.length == 8)
    val sorted: Seq[SearchResult[Int, Person]] = results.sortBy(r => -r.score)
    assert(sorted.head.obj.lastName == "Müller-Lüdenscheidt") // Better than Raißer due to more characters.
    assert(sorted.last.obj.lastName == "Mayer") // we never searched for Mayer, so this is a weak guess (fuzzy)
  }

}
