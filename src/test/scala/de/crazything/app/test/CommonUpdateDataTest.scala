package de.crazything.app.test

import de.crazything.app.factory.{MockingPersonFactory, PersonFactoryDE}
import de.crazything.app.helpers.DataProvider
import de.crazything.app.analyze.GermanLanguage
import de.crazything.app.entity.Person
import de.crazything.search.{CommonIndexer, CommonSearcher, DirectoryContainer, QueryConfig}
import org.scalatest.{AsyncFlatSpec, Matchers}

class CommonUpdateDataTest extends AsyncFlatSpec with Matchers with QueryConfig with GermanLanguage with DirectoryContainer {

  private val indexName = "UPDATE_TEST_DATA"

  private val standardPerson = Person(-1, "Herr", "firstName", "lastName", "street", "city")

  CommonIndexer.index(DataProvider.readVerySimplePersons(), PersonFactoryDE, indexName)

  "Persons" should "find Scharnofske" in {
    CommonSearcher.searchAsync(input = standardPerson.copy(lastName = "Scharnofske"), factory = PersonFactoryDE,
      searcherOption = indexName).map(result => {
      assert(result.length == 1)
    })
  }

  it should "find Alexander der Große instead of Scharnofske after updating entry" in {
    val personToUpdate = standardPerson.copy(lastName = "Scharnofske")
    val searchResult = CommonSearcher.search(personToUpdate, PersonFactoryDE, searcherOption = indexName)
    assert(searchResult.length == 1)
    val foundPerson: Person = searchResult.head.found
    CommonIndexer.updateDataAsync(Seq(
      foundPerson.copy(firstName = "Alexander", lastName = "der Große")
    ), PersonFactoryDE, indexName).map(_ => {
      val updatedFound = CommonSearcher.search(standardPerson.copy(lastName = "der Große"), PersonFactoryDE,
        searcherOption = indexName)
      println(updatedFound)
      assert(updatedFound.length == 1)

      val afterResultNoMore = CommonSearcher.search(personToUpdate, PersonFactoryDE,
        searcherOption = indexName)
      println(afterResultNoMore)
      assert(afterResultNoMore.isEmpty)
    })
  }

  it should "throw an exception after mock." in {
    val personToUpdate = standardPerson.copy(lastName = "der Große")
    val searchResult = CommonSearcher.search(personToUpdate, PersonFactoryDE, searcherOption = indexName)
    val foundPerson: Person = searchResult.head.found
    recoverToSucceededIf[Exception](
      CommonIndexer.updateDataAsync(Seq(
        foundPerson.copy(firstName = "Alexander", lastName = "Scharnofske")
      ), MockingPersonFactory, indexName).map(_ => {
        val updatedFound = CommonSearcher.search(standardPerson.copy(lastName = "der Große"), PersonFactoryDE,
          searcherOption = indexName)
        println(updatedFound)
        assert(updatedFound.length == 1)
      })
    )
  }

}
