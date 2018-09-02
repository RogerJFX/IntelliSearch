package de.crazything.app.test

import java.util.concurrent.RejectedExecutionException

import de.crazything.app.{GermanLanguage, Person, PersonFactoryDE}
import de.crazything.search.{AbstractTypeFactory, QueryConfig}
import de.crazything.search.entity.{PkDataSet, QueryCriteria, SearchResult}
import org.apache.lucene.document.Document
import org.apache.lucene.search.Query
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{Future, Promise}

import scala.concurrent.ExecutionContext.Implicits.global

trait FilterAsync extends QueryConfig with GermanLanguage {

  private val logger: Logger = LoggerFactory.getLogger("de.crazything.app.test.FilterAsyncTest")

  val standardPerson = Person(-1, "Herr", "firstName", "lastName", "street", "city")

  val availProcessors = Runtime.getRuntime.availableProcessors()

  def filterAvailProcessors(requested: Int) = if (requested > availProcessors) availProcessors else requested

  def filterFrankfurt(result: SearchResult[Int, Person]): Boolean = result.obj.city.contains("Frankfurt")

  def filterFrankfurtAsync(result: SearchResult[Int, Person]): Boolean = {
    Thread.sleep(500) // Come on! Just half a second...
    filterFrankfurt(result)
  }

  def filterException(result: SearchResult[Int, Person]): Boolean = {
    throw new RuntimeException("Howdy, I don't like this request.")
  }

  def filterExceptionFuture(result: SearchResult[Int, Person]): Future[Boolean] = Future {
    throw new RuntimeException("Howdy, I don't like this request.")
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

  // non blocking
  private def filterBooleanFuture(result: SearchResult[Int, Person], filterResult: Boolean): Future[Boolean] = {

    val p = Promise[Boolean]

    Future.apply {
      val timeout: Long = scala.util.Random.nextInt(500).toLong + 500L
      import java.util.concurrent.ScheduledThreadPoolExecutor
      val executor = new ScheduledThreadPoolExecutor(1)
      import java.util.concurrent.TimeUnit

      executor.schedule(new Runnable() {
        override def run(): Unit = {
          try {
            p.success(filterResult)
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

  def filterTrueFuture(result: SearchResult[Int, Person]) = filterBooleanFuture(result, true)
  def filterFalseFuture(result: SearchResult[Int, Person]) = filterBooleanFuture(result, false)

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

    override def getDataPoolSize(): Int = 0
  }
}
