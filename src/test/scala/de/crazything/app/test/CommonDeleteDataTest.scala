package de.crazything.app.test

import de.crazything.app.test.helpers.DataProvider
import de.crazything.app.{GermanLanguage, Person, PersonFactoryDE}
import de.crazything.search.{CommonIndexer, CommonSearcher, QueryConfig}
import org.scalatest.{FlatSpec, Matchers}

class CommonDeleteDataTest extends FlatSpec with Matchers with QueryConfig with GermanLanguage {

  val standardPerson = Person(-1, "Herr", "firstName", "lastName", "street", "city")

  CommonIndexer.index(DataProvider.readVerySimplePersons(), PersonFactoryDE)

  "Persons" should "find Scharnofske" in {
    val searchResult = CommonSearcher.search(standardPerson.copy(lastName = "Scharnofske"), PersonFactoryDE)
    assert(searchResult.length == 1)
  }

  it should "not find Scharnofske after deleting entry" in {
    val personToDelete = standardPerson.copy(lastName = "Scharnofske")

    val searchResult = CommonSearcher.search(personToDelete, PersonFactoryDE)

    assert(searchResult.length == 1)
    val foundPerson: Person = searchResult.head.found

    CommonIndexer.deleteData(Seq(foundPerson), PersonFactoryDE )

    val afterResult = CommonSearcher.search(personToDelete, PersonFactoryDE)

    assert(afterResult.isEmpty)
  }
}
