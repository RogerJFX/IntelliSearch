package de.crazything.search

import java.util.concurrent.{ExecutorService, Executors, TimeUnit}
import java.util.concurrent.atomic.AtomicInteger

import de.crazything.search.entity.{PkDataSet, QueryCriteria, SearchResult}
import de.crazything.search.utils.FutureUtil

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent._
import scala.util.{Failure, Success}

/**
  * Combine searches with other filters, maybe other searches.
  */
object CommonSearcherFiltered {

  import scala.concurrent.ExecutionContext.Implicits.global

  private val MAGIC_NUM_DEFAULT_HITS = 500 // ???

  def search[I, T <: PkDataSet[I]](input: T,
                                   factory: AbstractTypeFactory[I, T],
                                   queryCriteria: Option[QueryCriteria] = None,
                                   maxHits: Int = MAGIC_NUM_DEFAULT_HITS,
                                   filterFn: (SearchResult[I, T]) => Boolean): Seq[SearchResult[I, T]] = {
    val searchResult: Seq[SearchResult[I, T]] = CommonSearcher.search(input, factory, queryCriteria, maxHits)
    searchResult.filter((sr: SearchResult[I, T]) => filterFn(sr))
  }

  def searchAsync[I, T <: PkDataSet[I]](input: T,
                                        factory: AbstractTypeFactory[I, T],
                                        queryCriteria: Option[QueryCriteria] = None,
                                        maxHits: Int = MAGIC_NUM_DEFAULT_HITS,
                                        filterFn: (SearchResult[I, T]) => Boolean): Future[Seq[SearchResult[I, T]]] = {
    val searchResult: Future[Seq[SearchResult[I, T]]] = CommonSearcher.searchAsync(input, factory, queryCriteria, maxHits)
    val promise: Promise[Seq[SearchResult[I, T]]] = Promise[Seq[SearchResult[I, T]]]
    searchResult.onComplete {
      case Success(res) => promise.success(res.filter(sr => filterFn(sr)))
      case Failure(t) => throw new RuntimeException("Future failed in searchAsyncWithFilter", t)
      case _ => throw new RuntimeException("Unknown Error in searchAsyncWithFilter")
    }
    promise.future
  }

  private def doSearchAsyncAsync[I, T <: PkDataSet[I]](input: T,
                                                       factory: AbstractTypeFactory[I, T],
                                                       queryCriteria: Option[QueryCriteria] = None,
                                                       maxHits: Int = MAGIC_NUM_DEFAULT_HITS,
                                                       getFilterClass: (Seq[SearchResult[I, T]]) => Filter[I, T],
                                                       filterTimeout: FiniteDuration = ONE_DAY): Future[Seq[SearchResult[I, T]]] = {
    val searchResult: Future[Seq[SearchResult[I, T]]] = CommonSearcher.searchAsync(input, factory, queryCriteria, maxHits)
    val promise: Promise[Seq[SearchResult[I, T]]] = Promise[Seq[SearchResult[I, T]]]
    searchResult.onComplete {
      case Success(res) =>
        val filterClass: Filter[I, T] = getFilterClass(res)
        val finalResultFuture = FutureUtil.futureWithTimeout(filterClass.createFuture(), filterTimeout)
        finalResultFuture.onComplete {
          case Success(finalResult) =>
            if (finalResult.nonEmpty) {
              promise.success(finalResult.sortBy(r => -r.score))
            } else {
              promise.success(Seq())
            }
          case Failure(t: TimeoutException) =>
            filterClass.onTimeoutException(t)
            promise.failure(t)
          case Failure(x) => promise.failure(x)
          case _ => throw new RuntimeException("Unknown Error in searchAsyncWithAsyncFilter")
        }
      case Failure(t) => throw new RuntimeException("Future failed in searchAsyncWithAsyncFilter", t)
      case _ => throw new RuntimeException("Unknown Error in searchAsyncWithAsyncFilter")
    }
    promise.future
  }

  private val ONE_DAY = Duration.create(1, TimeUnit.DAYS)

  /**
    * EXPERIMENTAL
    *
    * Both, the search and filtering is async. Might once be helpful, if comparable data for filtering process
    * comes from somewhere remote.
    *
    * Normally searchAsync will do, so if you plan to filter right here.
    *
    * @param input         Search input
    * @param factory       Factory to be provided.
    * @param queryCriteria Optional criteria to help factory to decide which query to select.
    * @param maxHits       Max hits of search process before filtering. Default 500.
    * @param filterFn      The filter function as future.
    * @param filterTimeout Timeout for filtering. Keep in mind there may lots of async filter processes. This Duration
    *                      covers the time of all(!) processes. So it should not be too small. Default is one day.
    * @tparam I PK type of data object
    * @tparam T type of data object
    * @return Sequence of SearchResult as Future.
    */
  def searchAsyncAsync[I, T <: PkDataSet[I]](input: T,
                                             factory: AbstractTypeFactory[I, T],
                                             queryCriteria: Option[QueryCriteria] = None,
                                             maxHits: Int = MAGIC_NUM_DEFAULT_HITS,
                                             filterFn: (SearchResult[I, T]) => Boolean,
                                             filterTimeout: FiniteDuration = ONE_DAY): Future[Seq[SearchResult[I, T]]] = {
    def getFilterClass(res: Seq[SearchResult[I, T]]): Filter[I, T] = new FilterAsync(res, filterFn)
    doSearchAsyncAsync(input, factory, queryCriteria, maxHits, getFilterClass, filterTimeout)
  }

  // In order to use akka later.
  def searchAsyncAsyncFuture[I, T <: PkDataSet[I]](input: T,
                                                   factory: AbstractTypeFactory[I, T],
                                                   queryCriteria: Option[QueryCriteria] = None,
                                                   maxHits: Int = MAGIC_NUM_DEFAULT_HITS,
                                                   filterFn: (SearchResult[I, T]) => Future[Boolean],
                                                   filterTimeout: FiniteDuration = ONE_DAY): Future[Seq[SearchResult[I, T]]] = {
    def getFilterClass(res: Seq[SearchResult[I, T]]): Filter[I, T] = new FilterAsyncFuture(res, filterFn)
    doSearchAsyncAsync(input, factory, queryCriteria, maxHits, getFilterClass, filterTimeout)
  }

  //TODO: should be configurable
  val processors: Int = Runtime.getRuntime.availableProcessors()

  private trait Filter[I, T <: PkDataSet[I]] {
    val pool: ExecutorService = Executors.newFixedThreadPool(processors)

    val promise: Promise[Seq[SearchResult[I, T]]] = Promise[Seq[SearchResult[I, T]]]
    val buffer: ListBuffer[SearchResult[I, T]] = ListBuffer[SearchResult[I, T]]()
    val counter = new AtomicInteger()
    val procCount = new AtomicInteger()

    def onTimeoutException(exc: Exception): Unit = {
      pool.shutdownNow()
    }

    def createFuture(): Future[Seq[SearchResult[I, T]]]

    protected def doCreateFuture(raw: Seq[SearchResult[I, T]], body: (Int) => Unit): Future[Seq[SearchResult[I, T]]] = {
      body(raw.length)
      promise.future
    }
  }

  private class FilterAsync[I, T <: PkDataSet[I]](raw: Seq[SearchResult[I, T]],
                                                  filterFn: (SearchResult[I, T]) => Boolean) extends Filter[I, T] {
    override def createFuture(): Future[Seq[SearchResult[I, T]]] = {
      doCreateFuture(raw, (len: Int) => {
        def checkLenInc(): Unit = if (counter.incrementAndGet() == len) {
          promise.success(buffer)
          pool.shutdown()
        } else if (procCount.get() < len) {
          pool.execute(new TaskHandler(filterFn, raw(procCount.getAndIncrement()), buffer, () => checkLenInc()))
        }

        val shorter = if (processors < len) processors else len
        for (i <- 0 until shorter) {
          procCount.incrementAndGet()
          pool.execute(new TaskHandler(filterFn, raw(i), buffer, () => checkLenInc()))
        }
      })
    }
  }

  private class FilterAsyncFuture[I, T <: PkDataSet[I]](raw: Seq[SearchResult[I, T]],
                                                        filterFn: (SearchResult[I, T]) => Future[Boolean])
    extends Filter[I, T] {
    override def createFuture(): Future[Seq[SearchResult[I, T]]] = {
      doCreateFuture(raw, (len: Int) => {
        def checkLenInc(): Unit = if (counter.incrementAndGet() == len) {
          promise.success(buffer)
          pool.shutdown()
        } else if (procCount.get() < len) {
          pool.execute(new FutureHandler(filterFn, raw(procCount.getAndIncrement()), buffer, () => checkLenInc()))
        }

        val shorter = if (processors < len) processors else len
        for (i <- 0 until shorter) {
          procCount.incrementAndGet()
          pool.execute(new FutureHandler(filterFn, raw(i), buffer, () => checkLenInc()))
        }
      })
    }
  }

  private class TaskHandler[I, T <: PkDataSet[I]](filterFn: (SearchResult[I, T]) => Boolean,
                                                  sr: SearchResult[I, T],
                                                  buffer: ListBuffer[SearchResult[I, T]],
                                                  callback: () => Unit) extends Runnable {
    override def run(): Unit = {
      val success = filterFn(sr)
      if (success) {
        buffer.append(sr)
      }
      callback()
    }
  }

  private class FutureHandler[I, T <: PkDataSet[I]](filterFn: (SearchResult[I, T]) => Future[Boolean],
                                                    sr: SearchResult[I, T],
                                                    buffer: ListBuffer[SearchResult[I, T]],
                                                    callback: () => Unit) extends Runnable {
    override def run(): Unit = {
      filterFn(sr).onComplete {
        case Success(bool) =>
          if (bool) {
            buffer.append(sr)
          }
          callback()
        case _ =>
          callback()
      }
    }
  }

}
