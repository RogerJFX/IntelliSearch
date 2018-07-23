package de.crazything.app

import de.crazything.app.helpers.DataProvider
import de.crazything.search.{GermanIndexer, GermanSearcher, QueryConfig, SearchResult}
import org.scalatest.FlatSpec
import org.slf4j.LoggerFactory

import scala.collection.mutable.ListBuffer

class SimpleTest extends FlatSpec with QueryConfig{

  private val logger = LoggerFactory.getLogger(classOf[SimpleTest])

  GermanIndexer.index(DataProvider.readPersons(), PersonFactory)

  val results: ListBuffer[SearchResult[Int, Person]] = new ListBuffer[SearchResult[Int, Person]]()

  "Persons" should "find Reißer" in {
    val searchResult = GermanSearcher.search(Person(-1, "Herr", "firstName", "Reißer", "street", "city"), PersonFactory)
    logger.debug(s"Reißer: $searchResult")
    assert(searchResult.length == 1)
    results.appendAll(searchResult)
  }

  it should "find two results for Rayßer" in {
    val searchResult = GermanSearcher.search(Person(-1, "Herr", "firstName", "Rayßer", "street", "city"), PersonFactory)
    logger.debug(s"Rayßer (with fuzzy): $searchResult")
    assert(searchResult.length == 2)
    results.appendAll(searchResult)
  }

  it should "find one result for Rayßer" in {
    val searchResult = GermanSearcher.search(Person(-1, "Herr", "firstName", "Rayßer", "street", "city"), PersonFactory,
      QueryEnabled.TERM | QueryEnabled.PHONETIC)
    logger.debug(s"Rayßer (without fuzzy): $searchResult")
    assert(searchResult.length == 1)
    results.appendAll(searchResult)
  }

  it should "find Raisr" in {
    val searchResult = GermanSearcher.search(Person(-1, "Herr", "firstName", "Raisr", "street", "city"), PersonFactory)
    logger.debug(s"Raisr: $searchResult")
    assert(searchResult.length == 1)
    results.appendAll(searchResult)
  }

  it should "find Müller-Lüdenscheidt" in {
    val searchResult = GermanSearcher.search(Person(-1, "Herr", "firstName", "Müller-Lüdenscheidt", "street", "city"), PersonFactory)
    logger.debug(s"Müller-Lüdenscheidt: $searchResult")
    assert(searchResult.length == 1)
    results.appendAll(searchResult)
  }

  it should "find Muller-Ludenscheid" in {
    val searchResult = GermanSearcher.search(Person(-1, "Herr", "firstName", "Muller-Ludenscheid", "street", "city"), PersonFactory)
    logger.debug(s"Muller-Ludenscheid: $searchResult")
    assert(searchResult.length == 1)
    results.appendAll(searchResult)
  }

  it should "find Filosof" in {
    val searchResult = GermanSearcher.search(Person(-1, "Herr", "firstName", "Filosof", "street", "city"), PersonFactory)
    logger.debug(s"Filosof: $searchResult")
    assert(searchResult.length == 1)
    results.appendAll(searchResult)
  }

  it should "fail for 'Dr. Klöbner'" in {
    val searchResult = GermanSearcher.search(Person(-1, "Herr", "firstName", "Dr. Klöbner", "street", "city"), PersonFactory)
    logger.debug(s"Dr. Klöbner: $searchResult")
    assert(searchResult.isEmpty) // Nothing found => fail.
  }

  "Results" should "be reasonably sorted" in {
    assert(results.length == 8)
    val sorted: Seq[SearchResult[Int, Person]] = results.sortBy(r => -r.score)
    assert(sorted.head.obj.lastName == "Müller-Lüdenscheidt") // Better than Raißer due to more characters.
    assert(sorted.last.obj.lastName == "Mayer") // we never searched for Mayer, so this is a weak guess (fuzzy)
  }

}
