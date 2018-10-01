package de.crazything.app.test

import java.util.concurrent.RejectedExecutionException

import de.crazything.app.analyze.GermanLanguage
import de.crazything.app.entity.Person
import de.crazything.search.QueryConfig
import de.crazything.search.entity.SearchResult
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}

trait FilterAsyncTestMethods extends QueryConfig with GermanLanguage {

  private val logger: Logger = LoggerFactory.getLogger("de.crazything.app.test.FilterAsyncTest")

  val standardPerson = Person(-1, "Herr", "firstName", "lastName", "street", "city")

  protected val availProcessors: Int = Runtime.getRuntime.availableProcessors()

  protected def filterAvailProcessors(requested: Int): Int = if (requested > availProcessors) availProcessors else requested

  def filterFrankfurt(result: SearchResult[Int, Person]): Boolean = result.found.city.contains("Frankfurt")

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

  protected def filterTrueFuture(result: SearchResult[Int, Person]): Future[Boolean] = filterBooleanFuture(result, filterResult = true)
  protected def filterFalseFuture(result: SearchResult[Int, Person]): Future[Boolean] = filterBooleanFuture(result, filterResult = false)

}
