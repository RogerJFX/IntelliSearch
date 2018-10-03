package de.crazything.search.ext

import de.crazything.search.entity.{MappedResults, PkDataSet, SearchResult}
import de.crazything.search.ext.MappingSearcher.MAGIC_ONE_DAY
import de.crazything.search.utils.FutureUtil
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Future, Promise, TimeoutException}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

abstract class AbstractMappingSearcher {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  private def processSecondLevel[I1, I2, T1 <: PkDataSet[I1], T2 <: PkDataSet[I2]]
  (primaryResult: Seq[SearchResult[I1, T1]],
   mapperClass: (Seq[SearchResult[I1, T1]]) => MapperMaster[I1, I2, T1, T2],
   secondLevelTimeout: FiniteDuration = MAGIC_ONE_DAY,
   promise: Promise[Seq[MappedResults[I1, I2, T1, T2]]]): Unit = {

    val mappingClass: MapperMaster[I1, I2, T1, T2] = mapperClass(primaryResult)
    val finalResultFuture = FutureUtil.futureWithTimeout(mappingClass.createFuture(), secondLevelTimeout)

    finalResultFuture.onComplete {
      case Success(finalResult) =>
        promise.success(finalResult.sortBy(res => -res.target.score))
      case Failure(t: TimeoutException) =>
        mappingClass.onTimeoutException(t)
        promise.failure(t)
      case Failure(x) => promise.failure(x)
    }

  }

  protected def processFirstLevel[I1, I2, T1 <: PkDataSet[I1], T2 <: PkDataSet[I2]]
  (primaryResultFuture: Future[Seq[SearchResult[I1, T1]]],
   mapperClass: (Seq[SearchResult[I1, T1]]) => MapperMaster[I1, I2, T1, T2],
   secondLevelTimeout: FiniteDuration = MAGIC_ONE_DAY): Future[Seq[MappedResults[I1, I2, T1, T2]]] = {
    val promise: Promise[Seq[MappedResults[I1, I2, T1, T2]]] = Promise[Seq[MappedResults[I1, I2, T1, T2]]]
    primaryResultFuture.onComplete {
      case Success(res) =>
        processSecondLevel(res, mapperClass, secondLevelTimeout, promise)
      case Failure(t) => promise.failure(t)
    }
    promise.future
  }

  protected trait MapperMaster[I1, I2, T1 <: PkDataSet[I1], T2 <: PkDataSet[I2]] {
    def createFuture(): Future[Seq[MappedResults[I1, I2, T1, T2]]]
    def onTimeoutException(exc: Exception): Unit = logger.error("Timeout", exc)
  }
}
