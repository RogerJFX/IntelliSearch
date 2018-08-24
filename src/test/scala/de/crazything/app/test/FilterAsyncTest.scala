package de.crazything.app.test

import de.crazything.app.PersonFactoryDE
import de.crazything.app.test.helpers.{CustomMocks, DataProvider}
import de.crazything.search._
import de.crazything.search.ext.FilteringSearcher
import org.scalatest._
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.TimeoutException
import scala.concurrent.duration._

class FilterAsyncTest extends AsyncFlatSpec with BeforeAndAfter with FilterAsync {

  private val logger: Logger = LoggerFactory.getLogger("de.crazything.app.test.FilterAsyncTest")

  before {
    CommonIndexer.index(DataProvider.readVerySimplePersons(), PersonFactoryDE)
  }

  "Async search with sync filter" should "exclude Mayer living not in Frankfurt" in {
    FilteringSearcher.searchAsync(input = standardPerson.copy(lastName = "Mayer"), factory = PersonFactoryDE,
      filterFn = filterFrankfurt).map(result => {
      assert(result.isEmpty)
    })
  }

  it should "throw an exception if filter does." in {
    recoverToSucceededIf[Exception](
      FilteringSearcher.searchAsync(input = standardPerson.copy(lastName = "Hösl"), factory = PersonFactoryDE,
        filterFn = filterException).map(result => {
        assert(result.length == 1)
      })
    )
  }

  it should "throw an exception if directory is not loaded." in {
    val nullSearcherName = "SimpleTest-NullSearcher"
    DirectoryContainer.setDirectory(nullSearcherName, null)
    recoverToSucceededIf[Exception](
      FilteringSearcher.searchAsync(input = standardPerson.copy(lastName = "Hösl"), factory = PersonFactoryDE,
        filterFn = filterFrankfurt, searcherOption = DirectoryContainer.pickSearcher(nullSearcherName)).map(result => {
        assert(result.length == 1)
      })
    )
  }

  it should "pass Hösl living in Frankfurt" in {
    //CommonIndexer.index(DataProvider.readVerySimplePersons(), PersonFactoryDE)
    FilteringSearcher.searchAsync(input = standardPerson.copy(lastName = "Hösl"), factory = PersonFactoryDE,
      filterFn = filterFrankfurt).map(result => {
      assert(result.length == 1)
    })
  }

  // Make sure async processing does not destroy order of results.
  "Results" should "be sorted async/future (fast)" in {
    FilteringSearcher.searchAsyncAsyncFuture(input = standardPerson, factory = PersonFactoryAll,
      filterFn = filterTrueFuture, secondLevelTimeout = 10.seconds).map(result => {
      checkOrder(result)
      assert(result.length == 6)
    })
  }

  it should "be empty after false filter" in {
    FilteringSearcher.searchAsyncAsyncFuture(input = standardPerson, factory = PersonFactoryAll,
      filterFn = filterFalseFuture, secondLevelTimeout = 10.seconds).map(result => {
      assert(result.isEmpty)
    })
  }

  it should "work on two threads(async/future)" in {
    CustomMocks.mockObjectFieldAsync("de.crazything.search.ext.FilteringSearcher", "processors", filterAvailProcessors(2), {
      FilteringSearcher.searchAsyncAsyncFuture(input = standardPerson, factory = PersonFactoryAll,
        filterFn = filterTrueFuture, secondLevelTimeout = 10.seconds).map(result => {
        checkOrder(result)
        assert(result.length == 6)
      })
    })
  }

  it should "throw an exception if filter does (asyncAsyncFuture, 2 threads)." in {
    // Works, because tests within a suite are performed sequentially
    CustomMocks.mockObjectFieldAsync("de.crazything.search.ext.FilteringSearcher", "processors", filterAvailProcessors(2), {
      recoverToSucceededIf[Exception](
        FilteringSearcher.searchAsyncAsyncFuture(input = standardPerson.copy(lastName = "Hösl"), factory = PersonFactoryDE,
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
      FilteringSearcher.searchAsyncAsyncFuture(input = standardPerson, factory = PersonFactoryAll,
        filterFn = filterTrueFuture, secondLevelTimeout = 600.millis).map(result => {
        assert(result.length == 6)
      }))
  }

  it should "throw an exception if filter does (asyncAsyncFuture)." in {
    recoverToSucceededIf[Exception](
      FilteringSearcher.searchAsyncAsyncFuture(input = standardPerson.copy(lastName = "Hösl"), factory = PersonFactoryDE,
        filterFn = filterExceptionFuture).map(result => {
        assert(result.length == 1)
      })
    )
  }

  it should "be sorted sync/blocking (slow)" in {
    FilteringSearcher.searchAsync(input = standardPerson, factory = PersonFactoryAll,
      filterFn = filterTrue).map(result => {
      checkOrder(result)
      assert(result.length == 6)
    })
  }

}
