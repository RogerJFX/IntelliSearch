package de.crazything.app

import de.crazything.app.helpers.DataProvider
import de.crazything.search.{GermanIndexer, GermanSearcher}
import org.scalatest.FlatSpec
import org.slf4j.LoggerFactory

class SimpleTest extends FlatSpec {

  private val logger = LoggerFactory.getLogger("SimpleTest")

  GermanIndexer.index(DataProvider.readPersons(), PersonFactory)

  "Persons" should "find Reißer" in {
    val searchResult = GermanSearcher.search(Person(-1, "Herr", "firstName", "Reißer", "street", "city"), PersonFactory, 10)
    logger.debug(s"Reißer: $searchResult")
    assert(searchResult.length == 1)
  }

  it should "find Raisr" in {
    val searchResult = GermanSearcher.search(Person(-1, "Herr", "firstName", "Raisr", "street", "city"), PersonFactory, 10)
    logger.debug(s"Raisr: $searchResult")
    assert(searchResult.length == 1)
  }

  it should "find Müller-Lüdenscheidt" in {
    val searchResult = GermanSearcher.search(Person(-1, "Herr", "firstName", "Müller-Lüdenscheidt", "street", "city"), PersonFactory, 10)
    logger.debug(s"Müller-Lüdenscheidt: $searchResult")
    assert(searchResult.length == 1)
  }

  it should "find Muller-Ludenscheid" in {
    val searchResult = GermanSearcher.search(Person(-1, "Herr", "firstName", "Muller-Ludenscheid", "street", "city"), PersonFactory, 10)
    logger.debug(s"Muller-Ludenscheid: $searchResult")
    assert(searchResult.length == 1)
  }

  it should "find Filosof" in {
    val searchResult = GermanSearcher.search(Person(-1, "Herr", "firstName", "Filosof", "street", "city"), PersonFactory, 10)
    logger.debug(s"Filosof: $searchResult")
    assert(searchResult.length == 1)
  }

  it should "fail for 'Dr. Klöbner'" in {
    val searchResult = GermanSearcher.search(Person(-1, "Herr", "firstName", "Dr. Klöbner", "street", "city"), PersonFactory, 10)
    logger.debug(s"Dr. Klöbner: $searchResult")
    assert(searchResult.isEmpty) // Nothing found => fail.
  }

}
