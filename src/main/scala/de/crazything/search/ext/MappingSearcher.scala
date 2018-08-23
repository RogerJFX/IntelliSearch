package de.crazything.search.ext

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ExecutorService, Executors}

import de.crazything.search.entity.{PkDataSet, QueryCriteria, SearchResult}
import de.crazything.search.ext.RunnableHandlers.MapperFutureHandler
import de.crazything.search.utils.FutureUtil
import de.crazything.search.{AbstractTypeFactory, CommonSearcher, DirectoryContainer, MagicSettings}
import org.apache.lucene.search.IndexSearcher

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future, Promise, TimeoutException}
import scala.util.{Failure, Success}

object MappingSearcher extends MagicSettings {

  import scala.concurrent.ExecutionContext.Implicits.global

  private def callSecondLevel[I1, I2, T1 <: PkDataSet[I1], T2 <: PkDataSet[I2]]
  (searchResult: Seq[SearchResult[I1, T1]],
   mapperClass: (Seq[SearchResult[I1, T1]]) => Combine[I1, I2, T1, T2],
   secondLevelTimeout: FiniteDuration = ONE_DAY,
   promise: Promise[Seq[(SearchResult[I1, T1], Seq[SearchResult[I2, T2]])]]): Unit = {

    val mappingClass: Combine[I1, I2, T1, T2] = mapperClass(searchResult)
    val finalResultFuture = FutureUtil.futureWithTimeout(mappingClass.createFuture(), secondLevelTimeout)

    finalResultFuture.onComplete {
      case Success(finalResult) =>
//        if (finalResult.nonEmpty) { // should never be empty.
          promise.success(finalResult.sortBy(res => res._1.score))
//        } else {
//          promise.success(Seq())
//        }
      case Failure(t: TimeoutException) =>
        mappingClass.onTimeoutException(t)
        promise.failure(t)
      case Failure(x) => promise.failure(x)
    }

  }

  private def doMap[I1, I2, T1 <: PkDataSet[I1], T2 <: PkDataSet[I2]]
  (input: T1,
   factory: AbstractTypeFactory[I1, T1],
   searcherOption: Option[IndexSearcher] = DirectoryContainer.defaultSearcher,
   queryCriteria: Option[QueryCriteria] = None,
   maxHits: Int = MAGIC_NUM_DEFAULT_HITS_FILTERED,
   mapperClass: (Seq[SearchResult[I1, T1]]) => Combine[I1, I2, T1, T2],
   secondLevelTimeout: FiniteDuration = ONE_DAY): Future[Seq[(SearchResult[I1, T1], Seq[SearchResult[I2, T2]])]] = {
    val searchResult: Future[Seq[SearchResult[I1, T1]]] = CommonSearcher.searchAsync(input, factory, queryCriteria, maxHits, searcherOption)
    val promise: Promise[Seq[(SearchResult[I1, T1], Seq[SearchResult[I2, T2]])]] = Promise[Seq[(SearchResult[I1, T1], Seq[SearchResult[I2, T2]])]]
    searchResult.onComplete {
      case Success(res) =>
        callSecondLevel(res, mapperClass, secondLevelTimeout, promise)
      case Failure(t) => promise.failure(t)
    }
    promise.future
  }

  def searchMapping[I1, I2, T1 <: PkDataSet[I1], T2 <: PkDataSet[I2]]
  (input: T1,
   factory: AbstractTypeFactory[I1, T1],
   searcherOption: Option[IndexSearcher] = DirectoryContainer.defaultSearcher,
   queryCriteria: Option[QueryCriteria] = None,
   maxHits: Int = MAGIC_NUM_DEFAULT_HITS_FILTERED,
   mapperFn: (SearchResult[I1, T1]) => Future[Seq[SearchResult[I2, T2]]],
   secondLevelTimeout: FiniteDuration = ONE_DAY)
  : Future[Seq[(SearchResult[I1, T1], Seq[SearchResult[I2, T2]])]] = {
    def secondLevelClass(res: Seq[SearchResult[I1, T1]]): Combine[I1, I2, T1, T2] = new MapperAsyncFuture(res, mapperFn)

    doMap(input, factory, searcherOption, queryCriteria, maxHits, secondLevelClass, secondLevelTimeout)
  }

  val processors: Int = Runtime.getRuntime.availableProcessors()

  trait Combine[I1, I2, T1 <: PkDataSet[I1], T2 <: PkDataSet[I2]] {
    // Yes, we can do this here. We take care of the pool in our createFuture methods.
    val pool: ExecutorService = Executors.newFixedThreadPool(processors)
    val ec: ExecutionContext = ExecutionContext.fromExecutorService(pool)
    val promise: Promise[Seq[(SearchResult[I1, T1], Seq[SearchResult[I2, T2]])]] = Promise[Seq[(SearchResult[I1, T1], Seq[SearchResult[I2, T2]])]]
    val buffer: ListBuffer[(SearchResult[I1, T1], Seq[SearchResult[I2, T2]])] = ListBuffer[(SearchResult[I1, T1], Seq[SearchResult[I2, T2]])]()
    val counter = new AtomicInteger()
    val procCount = new AtomicInteger()

    def onTimeoutException(exc: Exception): Unit = {
      pool.shutdownNow()
    }

    def onCombineException(exc: Throwable): Unit = {
      if (!promise.isCompleted) {
        promise.failure(exc)
        pool.shutdownNow()
      }
    }

    def createFuture(): Future[Seq[(SearchResult[I1, T1], Seq[SearchResult[I2, T2]])]]

    protected def doCreateFuture(raw: Seq[SearchResult[I1, T1]], body: (Int) => Unit): Future[Seq[(SearchResult[I1, T1], Seq[SearchResult[I2, T2]])]] = {
      body(raw.length)
      promise.future
    }
  }

  private class MapperAsyncFuture[I1, I2, T1 <: PkDataSet[I1], T2 <: PkDataSet[I2]]
  (raw: Seq[SearchResult[I1, T1]],
   mappingFn: (SearchResult[I1, T1]) => Future[Seq[SearchResult[I2, T2]]])
    extends Combine[I1, I2, T1, T2] {
    override def createFuture(): Future[Seq[(SearchResult[I1, T1], Seq[SearchResult[I2, T2]])]] = {
      doCreateFuture(raw, (len: Int) => {
        def checkLenInc(): Unit = if (counter.incrementAndGet() == len) {
          promise.success(buffer)
          pool.shutdown()
        } else if (procCount.get() < len) {
          pool.execute(new MapperFutureHandler(mappingFn, raw(procCount.getAndIncrement()), buffer, () => checkLenInc(), onCombineException)(ec))
        }

        val shorter = if (processors < len) processors else len
        for (i <- 0 until shorter) {
          procCount.incrementAndGet()
          pool.execute(new MapperFutureHandler(mappingFn, raw(i), buffer, () => checkLenInc(), onCombineException)(ec))
        }
      })
    }
  }

}
