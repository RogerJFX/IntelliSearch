package de.crazything.app.test

import de.crazything.app.test.helpers.DataProvider
import de.crazything.app.{GermanLanguage, Person, PersonFactoryDE}
import de.crazything.search.{CommonIndexer, CommonSearcher, DirectoryContainer, QueryConfig}
import org.scalatest.{FlatSpec, Matchers}

class CommonDeleteDataTest extends FlatSpec with Matchers with QueryConfig with GermanLanguage with DirectoryContainer{
  
  val indexName = "DELETE_TEST_DATA"

  val standardPerson = Person(-1, "Herr", "firstName", "lastName", "street", "city")

  CommonIndexer.index(DataProvider.readVerySimplePersons(), PersonFactoryDE, indexName)

  "Persons" should "find Scharnofske" in {
    val searchResult = CommonSearcher.search(input = standardPerson.copy(lastName = "Scharnofske"), factory = PersonFactoryDE,
      searcherOption = indexName)
    assert(searchResult.length == 1)
  }

  it should "not find Scharnofske after deleting entry" in {
    val personToDelete = standardPerson.copy(lastName = "Scharnofske")
    val searchResult = CommonSearcher.search(personToDelete, PersonFactoryDE, searcherOption = indexName)
    assert(searchResult.length == 1)
    val foundPerson: Person = searchResult.head.found
    CommonIndexer.deleteData(Seq(foundPerson), PersonFactoryDE, indexName, true)
    val afterResult = CommonSearcher.search(personToDelete, PersonFactoryDE,
      searcherOption = indexName)
    assert(afterResult.isEmpty)
  }

//  it should "not find Hösl after deleting entry" in {
//    val personToDelete = standardPerson.copy(lastName = "Hösl")
//    val searchResult = CommonSearcher.search(personToDelete, PersonFactoryDE, searcherOption = indexName)
//    assert(searchResult.length == 1)
//    val foundPerson: Person = searchResult.head.found
//    CommonIndexer.deleteData(Seq(foundPerson), PersonFactoryDE, indexName, true )
//    val afterResult = CommonSearcher.search(personToDelete, PersonFactoryDE,
//      searcherOption = indexName)
//    assert(afterResult.isEmpty)
//  }
}
