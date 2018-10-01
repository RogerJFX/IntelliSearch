package de.crazything.app.test

import de.crazything.app.test.helpers.DataProvider
import de.crazything.app.{GermanLanguage, Person, PersonFactoryDE}
import de.crazything.search.{CommonIndexer, CommonSearcher, DirectoryContainer, QueryConfig}
import org.scalatest._

class CommonDeleteDataTest extends AsyncFlatSpec with Matchers with BeforeAndAfterEach with QueryConfig with FilterAsync
  with GermanLanguage with DirectoryContainer {

  private val indexName = "DELETE_TEST_DATA"

  //private val standardPerson = Person(-1, "Herr", "firstName", "lastName", "street", "city")

  override def beforeEach =
    CommonIndexer.index(DataProvider.readVerySimplePersons(), PersonFactoryDE, indexName)


  "Persons" should "find Scharnofske" in {
    CommonSearcher.searchAsync(input = standardPerson.copy(lastName = "Scharnofske"), factory = PersonFactoryDE,
      searcherOption = indexName).map(result => {
      assert(result.length == 1)
    })
  }

  it should "not find Scharnofske after deleting entry with forceFlush = true" in {
    val personToDelete = standardPerson.copy(lastName = "Scharnofske")
    val searchResult = CommonSearcher.search(personToDelete, PersonFactoryDE, searcherOption = indexName)
    assert(searchResult.length == 1)
    val foundPerson: Person = searchResult.head.found
    CommonIndexer.deleteDataAsync(Seq(foundPerson), PersonFactoryDE, indexName, true).map(_ => {
      val afterResult = CommonSearcher.search(personToDelete, PersonFactoryDE,
        searcherOption = indexName)
      assert(afterResult.isEmpty)
    })

  }

  it should "not find Scharnofske after deleting entry with forceFlush = false" in {
    val personToDelete = standardPerson.copy(lastName = "Scharnofske")
    val searchResult = CommonSearcher.search(personToDelete, PersonFactoryDE, searcherOption = indexName)
    assert(searchResult.length == 1)
    val foundPerson: Person = searchResult.head.found
    CommonIndexer.deleteDataAsync(Seq(foundPerson), PersonFactoryDE, indexName, false).map(_ => {
      val afterResult = CommonSearcher.search(personToDelete, PersonFactoryDE,
        searcherOption = indexName)
      assert(afterResult.isEmpty)
    })

  }

  it should "throw an exception after mock." in {
    val personToDelete = standardPerson.copy(lastName = "Mayer")
    val searchResult = CommonSearcher.search(personToDelete, PersonFactoryDE, searcherOption = indexName)
    val foundPerson: Person = searchResult.head.found
    recoverToSucceededIf[Exception](
      CommonIndexer.deleteDataAsync(Seq(foundPerson), PersonFactoryAll, indexName, false).map(_ => {
        val afterResult = CommonSearcher.search(personToDelete, PersonFactoryDE,
          searcherOption = indexName)
        assert(afterResult.isEmpty)
      })
    )
  }
}
