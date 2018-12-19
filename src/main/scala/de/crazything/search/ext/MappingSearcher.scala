package de.crazything.search.ext

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ExecutorService, Executors}

import de.crazything.search.entity.{MappedResults, PkDataSet, QueryCriteria, SearchResult}
import de.crazything.search.ext.RunnableHandlers.MapperFutureHandler
import de.crazything.search.{AbstractTypeFactory, CommonSearcher, DirectoryContainer, MagicSettings}
import org.apache.lucene.search.IndexSearcher
import play.api.libs.json.OFormat

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future, Promise}

/**
  * What MappingSearcher does:
  * 1. We have a initial search result.
  * 2. Now we want to ask another searcher to associate his results to our initial result.
  * 3. So we have a tree of results, all scored by Lucene.
  * 3. We than may iterate over the those results to decide lately what to do with them.
  *
  * A MappedResult consists of one primary search result and a sequence of other results associated to the primary result.
  */
object MappingSearcher extends AbstractMappingSearcher with MagicSettings {

  import scala.concurrent.ExecutionContext.Implicits.global

  def search[I1, I2, T1 <: PkDataSet[I1], T2 <: PkDataSet[I2]]
  (input: T1,
   factory: AbstractTypeFactory[I1, T1],
   searcherOption: Option[IndexSearcher] = DirectoryContainer.defaultSearcher,
   queryCriteria: Option[QueryCriteria] = None,
   maxHits: Int = MAGIC_NUM_DEFAULT_HITS_FILTERED,
   offset: Int = 0,
   mapperFn: SearchResult[I1, T1] => Future[Seq[SearchResult[I2, T2]]],
   secondLevelTimeout: FiniteDuration = MAGIC_ONE_DAY)
  : Future[Seq[MappedResults[I1, I2, T1, T2]]] = {
    def secondLevelClass(res: Seq[SearchResult[I1, T1]]): Mapper[I1, I2, T1, T2] = new MapperAsyncFuture(res, mapperFn)

    val searchResult: Future[Seq[SearchResult[I1, T1]]] =
      CommonSearcher.searchAsync(input, factory, queryCriteria, maxHits, offset, searcherOption)
    processFirstLevel(searchResult, secondLevelClass, secondLevelTimeout)
  }

  def searchFuture[I1, I2, T1 <: PkDataSet[I1], T2 <: PkDataSet[I2]]
  (initialFuture: Future[Seq[SearchResult[I1, T1]]],
   mapperFn: SearchResult[I1, T1] => Future[Seq[SearchResult[I2, T2]]],
   secondLevelTimeout: FiniteDuration = MAGIC_ONE_DAY)
  : Future[Seq[MappedResults[I1, I2, T1, T2]]] = {
    def secondLevelClass(res: Seq[SearchResult[I1, T1]]): Mapper[I1, I2, T1, T2] = new MapperAsyncFuture(res, mapperFn)

    processFirstLevel(initialFuture, secondLevelClass, secondLevelTimeout)
  }

  def searchRemote[I1, I2, T1 <: PkDataSet[I1], T2 <: PkDataSet[I2]]
  (input: T1,
   url: String,
   firstLevelTimeout: FiniteDuration = MAGIC_ONE_DAY,
   mapperFn: SearchResult[I1, T1] => Future[Seq[SearchResult[I2, T2]]],
   secondLevelTimeout: FiniteDuration = MAGIC_ONE_DAY)
  (implicit fmt: OFormat[T1])
  : Future[Seq[MappedResults[I1, I2, T1, T2]]] = {
    def secondLevelClass(res: Seq[SearchResult[I1, T1]]): Mapper[I1, I2, T1, T2] = new MapperAsyncFuture(res, mapperFn)

    val searchResult: Future[Seq[SearchResult[I1, T1]]] =
      CommonSearcher.searchRemote[I1, T1](input, url, firstLevelTimeout)
    processFirstLevel(searchResult, secondLevelClass, secondLevelTimeout)
  }

  val processors: Int = Runtime.getRuntime.availableProcessors()

  private trait Mapper[I1, I2, T1 <: PkDataSet[I1], T2 <: PkDataSet[I2]] extends MapperMaster[I1, I2, T1, T2]{
    // Yes, we can do this here. We take care of the pool in our createFuture methods.
    //val processors: Int
    val pool: ExecutorService = Executors.newFixedThreadPool(processors)
    val ec: ExecutionContext = ExecutionContext.fromExecutorService(pool)
    val promise: Promise[Seq[MappedResults[I1, I2, T1, T2]]] = Promise[Seq[MappedResults[I1, I2, T1, T2]]]
    val buffer: ListBuffer[MappedResults[I1, I2, T1, T2]] = ListBuffer[MappedResults[I1, I2, T1, T2]]()
    val counter = new AtomicInteger()
    val procCount = new AtomicInteger()

    override def onTimeoutException(exc: Exception): Unit = {
      pool.shutdownNow()
    }

    def onCombineException(exc: Throwable): Unit = {
      if (!promise.isCompleted) {
        promise.failure(exc)
        pool.shutdownNow()
      }
    }

    protected def doCreateFuture(raw: Seq[SearchResult[I1, T1]],
                                 body: Int => Unit): Future[Seq[MappedResults[I1, I2, T1, T2]]] = {
      body(raw.length)
      promise.future
    }
  }

  private class MapperAsyncFuture[I1, I2, T1 <: PkDataSet[I1], T2 <: PkDataSet[I2]]
  (raw: Seq[SearchResult[I1, T1]],
   mappingFn: SearchResult[I1, T1] => Future[Seq[SearchResult[I2, T2]]])
    extends Mapper[I1, I2, T1, T2] {
    override def createFuture(): Future[Seq[MappedResults[I1, I2, T1, T2]]] = {
      doCreateFuture(raw, (len: Int) => {
        def checkLenInc(): Unit =
          this.synchronized { //TODO: really?
            if (counter.incrementAndGet() == len) {
              promise.success(buffer)
              pool.shutdown()
            } else if (procCount.get() < len) {
              pool.execute(new MapperFutureHandler(mappingFn, raw(procCount.getAndIncrement()),
                buffer, () => checkLenInc(), onCombineException)(ec))
            }
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
