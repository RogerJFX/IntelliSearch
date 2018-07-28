package de.crazything.app.test

import de.crazything.app.{GermanLanguage, Person, PersonFactoryDE}
import de.crazything.app.test.helpers.DataProvider
import de.crazything.search.entity.SearchResult
import de.crazything.search.{CommonIndexer, CommonSearcherFiltered, QueryConfig}
import org.scalatest.{AsyncFlatSpec, BeforeAndAfter}

import scala.concurrent.Future

class FilterAsyncTest extends AsyncFlatSpec with BeforeAndAfter with QueryConfig with GermanLanguage {

  val standardPerson = Person(-1, "Herr", "firstName", "lastName", "street", "city")

  private def filterFrankfurt(result: SearchResult[Int, Person]): Boolean = result.obj.city.contains("Frankfurt")

  private def filterFrankfurtAsync(result: SearchResult[Int, Person]): Future[Boolean] = Future {
    Thread.sleep(500) // Come on! Just half a second...
    filterFrankfurt(result)
  }

  before {
    CommonIndexer.index(DataProvider.readVerySimplePersons(), PersonFactoryDE)
  }

  "Async search with sync filter" should "exclude Mayer living not in Frankfurt" in {
    CommonSearcherFiltered.searchAsync(input = standardPerson.copy(lastName = "Mayer"), factory = PersonFactoryDE,
      filterFn = filterFrankfurt).map(result => {
      assert(result.isEmpty)
    })
  }

  it should "pass Hösl living in Frankfurt" in {
    CommonSearcherFiltered.searchAsync(input = standardPerson.copy(lastName = "Hösl"), factory = PersonFactoryDE,
      filterFn = filterFrankfurt).map(result => {
      assert(result.length == 1)
    })
  }

  "Async search with async filter" should "exclude Mayer living not in Frankfurt" in {
    import scala.concurrent.duration._
    CommonSearcherFiltered.searchAsyncAsync(input = standardPerson.copy(lastName = "Mayer"), factory = PersonFactoryDE,
      filterFn = filterFrankfurtAsync, filterTimeout = 10.seconds).map(result => {
      assert(result.isEmpty)
    })
  }

  it should "pass Hösl living in Frankfurt" in {
    CommonSearcherFiltered.searchAsyncAsync(input = standardPerson.copy(lastName = "Hösl"), factory = PersonFactoryDE,
      filterFn = filterFrankfurtAsync).map(result => {
      assert(result.length == 1)
    })
  }

}
