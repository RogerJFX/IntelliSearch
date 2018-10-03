package de.crazything.search.ext

import de.crazything.search.entity.{PkDataSet, SearchResult}
import de.crazything.search.ext.FilteringSearcher.MAGIC_ONE_DAY
import de.crazything.search.utils.FutureUtil
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Future, Promise, TimeoutException}
import scala.util.{Failure, Success}

abstract class AbstractFilteringSearcher {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  private def processSecondLevel[I1, T1 <: PkDataSet[I1]]
  (primaryResult: Seq[SearchResult[I1, T1]],
   filterClass: (Seq[SearchResult[I1, T1]]) => FilterMaster[I1, T1],
   filterTimeout: FiniteDuration = MAGIC_ONE_DAY,
   promise: Promise[Seq[SearchResult[I1, T1]]]): Unit = {

    val filteringClass: FilterMaster[I1, T1] = filterClass(primaryResult)
    val finalResultFuture = FutureUtil.futureWithTimeout(filteringClass.createFuture(), filterTimeout)

    finalResultFuture.onComplete {
      case Success(finalResult) =>
        if (finalResult.nonEmpty) {
          promise.success(finalResult.sortBy(res => -res.score))
        } else {
          promise.success(Seq())
        }
      case Failure(t: TimeoutException) =>
        filteringClass.onTimeoutException(t)
        promise.failure(t)
      case Failure(x) => promise.failure(x)
    }

  }

  protected def processFirstLevel[I, T <: PkDataSet[I]]
  (primaryResultFuture: Future[Seq[SearchResult[I, T]]],
   getFilterClass: (Seq[SearchResult[I, T]]) => FilterMaster[I, T],
   filterTimeout: FiniteDuration = MAGIC_ONE_DAY): Future[Seq[SearchResult[I, T]]] = {
    val promise: Promise[Seq[SearchResult[I, T]]] = Promise[Seq[SearchResult[I, T]]]
    primaryResultFuture.onComplete {
      case Success(res) =>
        processSecondLevel(res, getFilterClass, filterTimeout, promise)
      case Failure(t) => promise.failure(t)
    }
    promise.future
  }

  protected trait FilterMaster[I, T <: PkDataSet[I]]{
    def createFuture(): Future[Seq[SearchResult[I, T]]]
    def onTimeoutException(exc: Exception): Unit = logger.error("Timeout", exc)
  }
}
