package de.crazything.search.ext

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ExecutorService, Executors}

import de.crazything.search.entity.{PkDataSet, QueryCriteria, SearchResult}
import de.crazything.search.ext.RunnableHandlers.FilterFutureHandler
import de.crazything.search.{AbstractTypeFactory, CommonSearcher, DirectoryContainer, MagicSettings}
import org.apache.lucene.search.IndexSearcher
import play.api.libs.json.OFormat

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future, Promise}

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Combine searches with other filters, maybe other searches.
  * So the search results maybe even filtered by remote search results.
  */
object FilteringSearcher extends AbstractFilteringSearcher with SimpleFiltering with MagicSettings {

  /**
    * Common filtered search. First we search an input right here, so locally (we should have an index of our own then).
    * Both, searching and filtering are async.
    *
    * @param input not sufficient input to search similarities for.
    * @param factory The factory
    * @param searcherOption Another index as default to search? Ok then. Might even be a String due to implicits.
    * @param queryCriteria Local criteria
    * @param maxHits Local max hits.
    * @param offset Offset of search results.
    * @param filterFn The filtering function to pass.
    * @param secondLevelTimeout remote timeout
    * @tparam I Type of primary key of type T
    * @tparam T Type to search for
    * @return Just the filtered sequence of results.
    */
  def search[I, T <: PkDataSet[I]]
  (input: T,
   factory: AbstractTypeFactory[I, T],
   searcherOption: Option[IndexSearcher] = DirectoryContainer.defaultSearcher,
   queryCriteria: Option[QueryCriteria] = None,
   maxHits: Int = MAGIC_NUM_DEFAULT_HITS_FILTERED,
   offset: Int = 0,
   filterFn: SearchResult[I, T] => Future[Boolean],
   secondLevelTimeout: FiniteDuration = MAGIC_ONE_DAY): Future[Seq[SearchResult[I, T]]] = {
    def secondLevelClass(res: Seq[SearchResult[I, T]]): Filter[I, T] = new FilterAsyncFuture(res, filterFn)
    val searchResult: Future[Seq[SearchResult[I, T]]] = CommonSearcher.searchAsync(input, factory, queryCriteria, maxHits, offset, searcherOption)
    processFirstLevel(searchResult, secondLevelClass, secondLevelTimeout)
  }



  /**
    * Used for remote searches. Pass some initial future, and we will mal it done here.
    *
    * @param initialFuture So you give me some future search result and i am doing the rest for you, dude.
    * @param filterFn The filtering function to pass.
    * @param secondLevelTimeout Timeout for the probable remote request.
    * @param fmt Should not bother you. It's implicit.
    * @tparam I Type of primary key of type T
    * @tparam T Type to search for
    * @return Just the filtered sequence of results.
    */
  def searchFuture[I, T <: PkDataSet[I]]
  (initialFuture: Future[Seq[SearchResult[I, T]]],
   filterFn: SearchResult[I, T] => Future[Boolean],
   secondLevelTimeout: FiniteDuration = MAGIC_ONE_DAY)
  (implicit fmt: OFormat[T]): Future[Seq[SearchResult[I, T]]] = {
    def secondLevelClass(res: Seq[SearchResult[I, T]]): Filter[I, T] = new FilterAsyncFuture(res, filterFn)
    processFirstLevel(initialFuture, secondLevelClass, secondLevelTimeout)
  }

  /**
    * Used for remote searches. Pass some URL as String and an input case class inheriting PkDataSet and we should
    * be fine.
    *
    * @param input not sufficient input to search similarities for.
    * @param url URL to send the initial request to.
    * @param firstLevelTimeout Time to wait for the first level response.
    * @param filterFn The filtering function to pass.
    * @param secondLevelTimeout Timeout for the probable remote request.
    * @param fmt Should not bother you. It's implicit.
    * @tparam I Type of primary key of type T
    * @tparam T Type to search for
    * @return Just the filtered sequence of results.
    */
  def searchRemote[I, T <: PkDataSet[I]]
  (input: T,
   url: String,
   firstLevelTimeout: FiniteDuration = MAGIC_ONE_DAY,
   filterFn: SearchResult[I, T] => Future[Boolean],
   secondLevelTimeout: FiniteDuration = MAGIC_ONE_DAY)
  (implicit fmt: OFormat[T]): Future[Seq[SearchResult[I, T]]] = {
    def secondLevelClass(res: Seq[SearchResult[I, T]]): Filter[I, T] = new FilterAsyncFuture(res, filterFn)
    val searchResult: Future[Seq[SearchResult[I, T]]] = CommonSearcher.searchRemote[I, T](input, url, firstLevelTimeout)
    processFirstLevel(searchResult, secondLevelClass, secondLevelTimeout)
  }

  // Do not make this private. We have to mock it in some tests.
  val processors: Int = Runtime.getRuntime.availableProcessors()



  private trait Filter[I, T <: PkDataSet[I]] extends FilterMaster[I, T]{
    // Yes, we can do this here. We take care of the pool in our createFuture methods.
    val pool: ExecutorService = Executors.newFixedThreadPool(processors)
    val ec: ExecutionContext = ExecutionContext.fromExecutorService(pool)
    val promise: Promise[Seq[SearchResult[I, T]]] = Promise[Seq[SearchResult[I, T]]]
    val buffer: ListBuffer[SearchResult[I, T]] = ListBuffer[SearchResult[I, T]]()
    val counter = new AtomicInteger()
    val procCount = new AtomicInteger()

    override def onTimeoutException(exc: Exception): Unit = {
      pool.shutdownNow()
    }

    def onFilterException(exc: Throwable): Unit = {
      if (!promise.isCompleted) {
        promise.failure(exc)
        pool.shutdownNow()
      }
    }

    protected def doCreateFuture(raw: Seq[SearchResult[I, T]], body: Int => Unit): Future[Seq[SearchResult[I, T]]] = {
      body(raw.length)
      promise.future
    }
  }

  private class FilterAsyncFuture[I, T <: PkDataSet[I]](raw: Seq[SearchResult[I, T]],
                                                        filterFn: SearchResult[I, T] => Future[Boolean])
    extends Filter[I, T] {
    override def createFuture(): Future[Seq[SearchResult[I, T]]] = {
      doCreateFuture(raw, (len: Int) => {
        def checkLenInc(): Unit = this.synchronized {
          if (counter.incrementAndGet() == len) {
            promise.success(buffer)
            pool.shutdown()
          } else if (procCount.get() < len) {
            pool.execute(new FilterFutureHandler(filterFn, raw(procCount.getAndIncrement()), buffer, () => checkLenInc(), onFilterException)(ec))
          }
        }

        val shorter = if (processors < len) processors else len
        for (i <- 0 until shorter) {
          procCount.incrementAndGet()
          pool.execute(new FilterFutureHandler(filterFn, raw(i), buffer, () => checkLenInc(), onFilterException)(ec))
        }
      })
    }
  }

}
