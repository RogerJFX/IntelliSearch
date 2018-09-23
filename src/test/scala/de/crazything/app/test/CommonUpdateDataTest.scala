package de.crazything.app.test

import de.crazything.app.test.helpers.DataProvider
import de.crazything.app.{GermanLanguage, Person, PersonFactoryDE}
import de.crazything.search.{CommonIndexer, CommonSearcher, DirectoryContainer, QueryConfig}
import org.scalatest.{FlatSpec, Matchers}

class CommonUpdateDataTest extends FlatSpec with Matchers with QueryConfig with GermanLanguage with DirectoryContainer{

  val indexName = "UPDATE_TEST_DATA"

  val standardPerson = Person(-1, "Herr", "firstName", "lastName", "street", "city")

  CommonIndexer.index(DataProvider.readVerySimplePersons(), PersonFactoryDE, indexName)

  "Persons" should "find Scharnofske" in {
    val searchResult = CommonSearcher.search(input = standardPerson.copy(lastName = "Scharnofske"), factory = PersonFactoryDE,
      searcherOption = indexName)
    assert(searchResult.length == 1)
  }

  it should "not find Scharnofske after updating entry" in {
    val personToUpdate = standardPerson.copy(lastName = "Scharnofske")
    val searchResult = CommonSearcher.search(personToUpdate, PersonFactoryDE, searcherOption = indexName)
    assert(searchResult.length == 1)
    val foundPerson: Person = searchResult.head.found
    CommonIndexer.updateData(Seq(
      foundPerson.copy(firstName = "Alexander", lastName="der Große")
//      ,
//      standardPerson.copy(firstName="Karl"),
//      standardPerson.copy(firstName="Heinz")
    ), PersonFactoryDE, indexName)

    val updatedFound = CommonSearcher.search(standardPerson.copy(lastName = "der Große"), PersonFactoryDE,
      searcherOption = indexName)
    println(updatedFound)
    assert(updatedFound.length == 1)

    val afterResultNoMore = CommonSearcher.search(personToUpdate, PersonFactoryDE,
      searcherOption = indexName)
    println(afterResultNoMore)
    assert(afterResultNoMore.isEmpty)
  }

}
