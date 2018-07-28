package de.crazything.app.test

import de.crazything.app.test.helpers.DataProvider
import de.crazything.app.{GermanLanguage, Person, PersonFactoryDE}
import de.crazything.search.entity.SearchResult
import de.crazything.search.{CommonIndexer, CommonSearcherFiltered, QueryConfig}
import org.scalatest.FlatSpec

class FilterTest extends FlatSpec with QueryConfig with GermanLanguage {

  private def filterFrankfurt(result: SearchResult[Int, Person]): Boolean = result.obj.city.contains("Frankfurt")

  val standardPerson = Person(-1, "Herr", "firstName", "lastName", "street", "city")

  CommonIndexer.index(DataProvider.readPersons(), PersonFactoryDE)

  "Filter" should "exclude Mayer living not in Frankfurt" in {
    val searchResult =
      CommonSearcherFiltered.search(standardPerson.copy(lastName = "Mayer"), PersonFactoryDE, filterFn = filterFrankfurt)
    assert(searchResult.isEmpty)
  }

  it should "pass Hösl living in Frankfurt" in {
    val searchResult =
      CommonSearcherFiltered.search(standardPerson.copy(lastName = "Hösl"), PersonFactoryDE, filterFn = filterFrankfurt)
    assert(searchResult.length == 1)
  }

}
