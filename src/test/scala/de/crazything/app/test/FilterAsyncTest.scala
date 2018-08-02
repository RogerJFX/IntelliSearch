package de.crazything.app.test

import java.util.concurrent.RejectedExecutionException

import de.crazything.app.test.helpers.{CustomMocks, DataProvider}
import de.crazything.app.{GermanLanguage, Person, PersonFactoryDE}
import de.crazything.search._
import de.crazything.search.entity.{PkDataSet, QueryCriteria, SearchResult}
import org.apache.lucene.document.Document
import org.apache.lucene.search.Query
import org.scalatest._
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{Future, Promise, TimeoutException}

class FilterAsyncTest extends AsyncFlatSpec with BeforeAndAfter with QueryConfig with GermanLanguage {

  private val logger: Logger = LoggerFactory.getLogger("de.crazything.app.test.FilterAsyncTest")

  val standardPerson = Person(-1, "Herr", "firstName", "lastName", "street", "city")

  private def filterFrankfurt(result: SearchResult[Int, Person]): Boolean = result.obj.city.contains("Frankfurt")

  val availProcessors = Runtime.getRuntime.availableProcessors()

  private def filterAvailProcessors(requested: Int) = if (requested > availProcessors) availProcessors else requested

  private def filterFrankfurtAsync(result: SearchResult[Int, Person]): Boolean = {
    Thread.sleep(500) // Come on! Just half a second...
    filterFrankfurt(result)
  }

  private def filterException(result: SearchResult[Int, Person]): Boolean = {
    throw new RuntimeException("Howdy, I don't like this request.")
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

  object PersonFactoryAll extends AbstractTypeFactory[Int, Person] {

    import de.crazything.search.CustomQuery._

    override def createInstanceFromDocument(doc: Document): PkDataSet[Int] = PersonFactoryDE.createInstanceFromDocument(doc)

    override def setDataPool(data: Seq[Person]): Unit = ???

    override def populateDocument(document: Document, dataSet: Person): Unit = ???

    override def createQuery(t: Person): Query = {

      Seq(
        ("lastName", "Hösl").exact, // should is default
        ("firstName", "Fr*").wildcard,
        ("lastName", ".*").regex,
        ("lastName", "Mayer").phonetic)
    }

    override def selectQueryCreator: (QueryCriteria, Person) => Query = (criteria, person) => {
      if (criteria.queryName == "dummy") {
        Seq(
          ("firstName", "Roger").exact.must,
          ("lastName", person.lastName).exact.must,
          ("lastName", "Flintstone").exact.mustNot, // ridiculous - just for the code coverage...
          ("lastName", "ABCABCABCABCABCABCABCABCABCABC").exact.should // ridiculous - ...
        )
      } else createQuery(person)
    }
  }

  "Combined searches" should "get only the Author" in {

    def filterRoger(result: SearchResult[Int, Person]): Boolean = {
      // normally another Factory/Directory - just a check on some other data source
      CommonSearcher.search(input = standardPerson.copy(lastName = result.obj.lastName, firstName = "Roger"),
        factory = PersonFactoryAll, queryCriteria = Some(QueryCriteria("dummy"))).nonEmpty
    }

    import scala.concurrent.duration._
    CommonSearcherFiltered.searchAsyncAsync(input = standardPerson, factory = PersonFactoryAll,
      filterFn = filterRoger, filterTimeout = 3.seconds).map(result => {
      assert(result.length == 1)
    })

  }

  def checkOrder(seq: Seq[SearchResult[Int, Person]]): Unit = {
    def check(head: SearchResult[Int, Person], tail: Seq[SearchResult[Int, Person]]): Unit = {
      val nextHead = tail.head
      assert(head.score >= nextHead.score)
      val nextTail = tail.tail
      if (nextTail.nonEmpty) {
        check(nextHead, nextTail)
      }
    }

    check(seq.head, seq.tail)
  }

  // blocking
  def filterTrue(result: SearchResult[Int, Person]): Boolean = {
    val timeout: Long = scala.util.Random.nextInt(500).toLong + 500L
    try {
      Thread.sleep(timeout)
    } catch {
      case _: Exception => // ignore
    }
    true
  }

  import scala.concurrent.duration._

  // non blocking
  private def filterTrueFuture(result: SearchResult[Int, Person]): Future[Boolean] = {

    val p = Promise[Boolean]

    Future.apply {
      val timeout: Long = scala.util.Random.nextInt(500).toLong + 500L
      import java.util.concurrent.ScheduledThreadPoolExecutor
      val executor = new ScheduledThreadPoolExecutor(1)
      import java.util.concurrent.TimeUnit

      executor.schedule(new Runnable() {
        override def run(): Unit = {
          try {
            p.success(true)
          } catch { // No chance. Some instance seems to swallow the Exception (Test: "should throw TimeoutException")
            case ree: RejectedExecutionException =>
              logger.debug("An execution was rejected due to a too small timeout of {}, message is {}", timeout, ree.getMessage)
            case e: Exception => // ignore
              logger.debug("An execution has occurred, message is {}", e.getMessage)
            case _: Throwable => logger.error("WTF")
          }
        }
      }, timeout, TimeUnit.MILLISECONDS)
    }
    p.future
  }

  private def filterExceptionFuture(result: SearchResult[Int, Person]): Future[Boolean] = Future {
    throw new RuntimeException("Howdy, I don't like this request.")
  }

  // Make sure async processing does not destroy order of results.
  "Results" should "be sorted async/future (fast)" in {
    CommonSearcherFiltered.searchAsyncAsyncFuture(input = standardPerson, factory = PersonFactoryAll,
      filterFn = filterTrueFuture, filterTimeout = 10.seconds).map(result => {
      checkOrder(result)
      assert(result.length == 6)
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
    // Wonder, if this works reliably.
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
