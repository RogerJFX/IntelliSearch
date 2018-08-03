package de.crazything.app.test

import de.crazything.app.test.helpers.{CustomMocks, DataProvider}
import de.crazything.app.{Person, PersonFactoryDE}
import de.crazything.search._
import de.crazything.search.entity.{QueryCriteria, SearchResult}
import org.scalatest._
import scala.concurrent.duration._
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.TimeoutException

class FilterAsyncTest extends AsyncFlatSpec with BeforeAndAfter with FilterAsync {

  private val logger: Logger = LoggerFactory.getLogger("de.crazything.app.test.FilterAsyncTest")

  before {
    CommonIndexer.index(DataProvider.readVerySimplePersons(), PersonFactoryDE)
  }

  "Async search with sync filter" should "exclude Mayer living not in Frankfurt" in {
    CommonSearcherFiltered.searchAsync(input = standardPerson.copy(lastName = "Mayer"), factory = PersonFactoryDE,
      filterFn = filterFrankfurt).map(result => {
      assert(result.isEmpty)
    })
  }

  it should "throw an exception if filter does." in {
    recoverToSucceededIf[Exception](
      CommonSearcherFiltered.searchAsync(input = standardPerson.copy(lastName = "Hösl"), factory = PersonFactoryDE,
        filterFn = filterException).map(result => {
        assert(result.length == 1)
      })
    )
  }

  it should "throw an exception if directory is not loaded." in {
    val nullSearcherName = "SimpleTest-NullSearcher"
    DirectoryContainer.setDirectory(nullSearcherName, null)
    recoverToSucceededIf[Exception](
      CommonSearcherFiltered.searchAsync(input = standardPerson.copy(lastName = "Hösl"), factory = PersonFactoryDE,
        filterFn = filterFrankfurt, searcherOption = DirectoryContainer.pickSearcher(nullSearcherName)).map(result => {
        assert(result.length == 1)
      })
    )
  }

  it should "pass Hösl living in Frankfurt" in {
    //CommonIndexer.index(DataProvider.readVerySimplePersons(), PersonFactoryDE)
    CommonSearcherFiltered.searchAsync(input = standardPerson.copy(lastName = "Hösl"), factory = PersonFactoryDE,
      filterFn = filterFrankfurt).map(result => {
      assert(result.length == 1)
    })
  }

  "Async search with async filter" should "exclude Mayer living not in Frankfurt" in {
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

  it should "throw an exception if directory is not loaded (async, async)." in {
    recoverToSucceededIf[Exception](
      CommonSearcherFiltered.searchAsyncAsync(input = standardPerson.copy(lastName = "Hösl"), factory = PersonFactoryDE,
        filterFn = filterFrankfurt, searcherOption =
          DirectoryContainer.pickSearcher("I bet there is no searcher for this string")).map(result => {
        assert(result.length == 1)
      })
    )
  }

  it should "throw an exception if filter does (async, async)." in {
    recoverToSucceededIf[Exception](
      CommonSearcherFiltered.searchAsyncAsync(input = standardPerson.copy(lastName = "Hösl"), factory = PersonFactoryDE,
        filterFn = filterException).map(result => {
        assert(result.length == 1)
      })
    )
  }

  "Combined searches" should "get only the Author" in {

    def filterRoger(result: SearchResult[Int, Person]): Boolean = {
      // normally another Factory/Directory - just a check on some other data source
      CommonSearcher.search(input = standardPerson.copy(lastName = result.obj.lastName, firstName = "Roger"),
        factory = PersonFactoryAll, queryCriteria = Some(QueryCriteria("dummy"))).nonEmpty
    }
    CommonSearcherFiltered.searchAsyncAsync(input = standardPerson, factory = PersonFactoryAll,
      filterFn = filterRoger, filterTimeout = 3.seconds).map(result => {
      assert(result.length == 1)
    })

  }

  // Make sure async processing does not destroy order of results.
  "Results" should "be sorted async/future (fast)" in {
    CommonSearcherFiltered.searchAsyncAsyncFuture(input = standardPerson, factory = PersonFactoryAll,
      filterFn = filterTrueFuture, filterTimeout = 10.seconds).map(result => {
      checkOrder(result)
      assert(result.length == 6)
    })
  }

  it should "be empty after false filter" in {
    CommonSearcherFiltered.searchAsyncAsyncFuture(input = standardPerson, factory = PersonFactoryAll,
      filterFn = filterFalseFuture, filterTimeout = 10.seconds).map(result => {
      assert(result.isEmpty)
    })
  }

  it should "work on two threads(async/future)" in {
    CustomMocks.mockObjectFieldAsync("de.crazything.search.CommonSearcherFiltered", "processors", filterAvailProcessors(2), {
      CommonSearcherFiltered.searchAsyncAsyncFuture(input = standardPerson, factory = PersonFactoryAll,
        filterFn = filterTrueFuture, filterTimeout = 10.seconds).map(result => {
        checkOrder(result)
        assert(result.length == 6)
      })
    })
  }

  it should "throw an exception if filter does (asyncAsyncFuture, 2 threads)." in {
    // Works, because tests within a suite are performed sequentially
    CustomMocks.mockObjectFieldAsync("de.crazything.search.CommonSearcherFiltered", "processors", filterAvailProcessors(2), {
      recoverToSucceededIf[Exception](
        CommonSearcherFiltered.searchAsyncAsyncFuture(input = standardPerson.copy(lastName = "Hösl"), factory = PersonFactoryDE,
          filterFn = filterExceptionFuture).map(result => {
          assert(result.length == 1)
        })
      )
    })
  }

  it should "throw TimeoutException" in {
    logger.warn("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
    logger.warn("We expect TimeoutException in this test, and RejectedExecutionException as well. So no worries.")
    logger.warn("Reason: we shut down the ThreadPool immediately after a TimeoutException. So the remaining Tasks cannot be executed.")
    logger.warn("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
    recoverToSucceededIf[TimeoutException](
      CommonSearcherFiltered.searchAsyncAsyncFuture(input = standardPerson, factory = PersonFactoryAll,
        filterFn = filterTrueFuture, filterTimeout = 600.millis).map(result => {
        assert(result.length == 6)
      }))
  }

  it should "throw an exception if filter does (asyncAsyncFuture)." in {
    recoverToSucceededIf[Exception](
      CommonSearcherFiltered.searchAsyncAsyncFuture(input = standardPerson.copy(lastName = "Hösl"), factory = PersonFactoryDE,
        filterFn = filterExceptionFuture).map(result => {
        assert(result.length == 1)
      })
    )
  }

  it should "be sorted async/blocking (fast)" in {
    CommonSearcherFiltered.searchAsyncAsync(input = standardPerson, factory = PersonFactoryAll,
      filterFn = filterTrue).map(result => {
      checkOrder(result)
      assert(result.length == 6)
    })
  }

  it should "work on 4 threads(async/blocking)" in {
    CustomMocks.mockObjectFieldAsync("de.crazything.search.CommonSearcherFiltered", "processors", filterAvailProcessors(4), {
      CommonSearcherFiltered.searchAsyncAsync(input = standardPerson, factory = PersonFactoryAll,
        filterFn = filterTrue, filterTimeout = 10.seconds).map(result => {
        checkOrder(result)
        assert(result.length == 6)
      })
    })
  }

  it should "be sorted sync/blocking (slow)" in {
    CommonSearcherFiltered.searchAsync(input = standardPerson, factory = PersonFactoryAll,
      filterFn = filterTrue).map(result => {
      checkOrder(result)
      assert(result.length == 6)
    })
  }

}
