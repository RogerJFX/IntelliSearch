package de.crazything.app.test

import de.crazything.app.analyze.GermanLanguage
import de.crazything.app.entity.Person
import de.crazything.app.factory.PersonFactoryDE
import de.crazything.app.helpers.DataProvider
import de.crazything.search.entity.QueryCriteria
import de.crazything.search.{CommonIndexer, CommonSearcher, QueryConfig}
import org.scalatest.{FlatSpec, Matchers}
import org.slf4j.LoggerFactory

class CommonSearchCascadedTest extends FlatSpec with Matchers with QueryConfig with GermanLanguage {

  private val logger = LoggerFactory.getLogger(classOf[CommonSearchCascadedTest])

  private val standardPerson = Person(-1, "Herr", "firstName", "lastName", "street", "city")

  CommonIndexer.index(DataProvider.readVerySimplePersons(), PersonFactoryDE)

  "Searcher" should "find Adorno in cascaded must query" in {
    val searchResult = CommonSearcher.search(standardPerson.copy(lastName = "Philosoph", firstName="Theodor Wiesengrund"), PersonFactoryDE,
      Some(QueryCriteria(PersonFactoryDE.cascadedQuery_FirstAndLastName)))
    assert(searchResult.length == 1)
  }

  it should "not find Adorno in cascaded must query" in {
    val searchResult = CommonSearcher.search(standardPerson.copy(lastName = "Filosoph", firstName="Theodor Wiesengrund"), PersonFactoryDE,
      Some(QueryCriteria(PersonFactoryDE.cascadedQuery_FirstAndLastName)))
    assert(searchResult.isEmpty)
  }

  it should "find Franz Mayer in cascaded must query due to regex search" in {
    val searchResult = CommonSearcher.search(standardPerson.copy(lastName = "Meier", firstName="Franz"), PersonFactoryDE,
      Some(QueryCriteria(PersonFactoryDE.cascadedQuery_FirstAndLastName)))
    assert(searchResult.length == 1)
  }


}
