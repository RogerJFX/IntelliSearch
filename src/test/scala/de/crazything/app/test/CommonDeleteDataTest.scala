package de.crazything.app.test

import de.crazything.app.test.helpers.DataProvider
import de.crazything.app.{GermanLanguage, Person, PersonFactoryDE}
import de.crazything.search.{CommonIndexer, CommonSearcher, DirectoryContainer, QueryConfig}
import org.scalatest.{FlatSpec, Matchers}

class CommonDeleteDataTest extends FlatSpec with Matchers with QueryConfig with GermanLanguage with DirectoryContainer{

  val standardPerson = Person(-1, "Herr", "firstName", "lastName", "street", "city")

  CommonIndexer.index(DataProvider.readVerySimplePersons(), PersonFactoryDE, "DELETE_TEST_DATA")

  "Persons" should "find Scharnofske" ignore {
    val searchResult = CommonSearcher.search(input = standardPerson.copy(lastName = "Scharnofske"), factory = PersonFactoryDE,
      searcherOption = "DELETE_TEST_DATA")
    assert(searchResult.length == 1)
  }

  it should "not find Scharnofske after deleting entry" ignore {
    val personToDelete = standardPerson.copy(lastName = "Scharnofske")
    val searchResult = CommonSearcher.search(personToDelete, PersonFactoryDE, searcherOption = "DELETE_TEST_DATA")
    assert(searchResult.length == 1)
    val foundPerson: Person = searchResult.head.found
    CommonIndexer.deleteData(Seq(foundPerson), PersonFactoryDE, "DELETE_TEST_DATA" )
    val afterResult = CommonSearcher.search(personToDelete, PersonFactoryDE,
      searcherOption = "DELETE_TEST_DATA")
    assert(afterResult.isEmpty)
  }
}
