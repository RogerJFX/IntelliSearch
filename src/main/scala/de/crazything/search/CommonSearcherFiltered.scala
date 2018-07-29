package de.crazything.search

import java.util.concurrent.atomic.AtomicInteger

import de.crazything.search.entity.{PkDataSet, QueryCriteria, SearchResult}

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, Promise}
import scala.util.{Failure, Success}

/**
  * Combine searches with other filters, maybe other searches.
  */
object CommonSearcherFiltered {

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

  /**
    *
    * Both, the search and filtering is async. Might once be helpful, if comparable data for filtering process
    * comes from somewhere remote.
    *
    * Normally searchAsync will do, so if you plan to filter right here.
    *
    * @param input Search input
    * @param factory Factory to be provided.
    * @param queryCriteria Optional criteria to help factory to decide which query to select.
    * @param maxHits Max hits of search process before filtering. Default 500.
    * @param filterFn The filter function as future.
    * @param filterTimeout Timeout for filtering. Keep in mind there may lots of async filter processes. This Duration
    *                      covers the time of all(!) processes. So it should not be too small. Default is infinite.
    * @tparam I PK type of data object
    * @tparam T type of data object
    * @return Sequence of SearchResult as Future.
    */
  def searchAsyncAsync[I, T <: PkDataSet[I]](input: T,
                                             factory: AbstractTypeFactory[I, T],
                                             queryCriteria: Option[QueryCriteria] = None,
                                             maxHits: Int = MAGIC_NUM_DEFAULT_HITS,
                                             filterFn: (SearchResult[I, T]) => Future[Boolean],
                                             filterTimeout: Duration = Duration.Inf): Future[Seq[SearchResult[I, T]]] = {
    val searchResult: Future[Seq[SearchResult[I, T]]] = CommonSearcher.searchAsync(input, factory, queryCriteria, maxHits)
    val promise: Promise[Seq[SearchResult[I, T]]] = Promise[Seq[SearchResult[I, T]]]
    searchResult.onComplete {
//      case Success(res) =>
//        promise.success(res.filter(sr => {
//          // No need for sorting again here. It is not concurrent...
//          Await.result(filterFn(sr), filterTimeout)
//        }))
//      case Success(res) => promise.success(for {
//        x <- res if Await.result(filterFn(x), filterTimeout)
//      } yield x)
      case Success(res) =>
        val future: Future[Seq[SearchResult[I, T]]] = doFilterAsync(res, filterFn)
        future.onComplete{
          case Success(resF) => promise.success(resF)
          case _ => throw new RuntimeException("Fuck it")
        }
       // promise.success(Await.result(doFilterAsync(res, filterFn), filterTimeout))
      case Failure(t) => throw new RuntimeException("Future failed in searchAsyncWithAsyncFilter", t)
      case _ => throw new RuntimeException("Unknown Error in searchAsyncWithAsyncFilter")
    }
    promise.future
  }
  // private desperation...
    private def doFilterAsync[I, T <: PkDataSet[I]](raw: Seq[SearchResult[I, T]],
                                                    filterFn: (SearchResult[I, T]) => Future[Boolean])
    : Future[Seq[SearchResult[I, T]]] = {
      val promise: Promise[Seq[SearchResult[I, T]]] = Promise[Seq[SearchResult[I, T]]]
      val buffer: ListBuffer[SearchResult[I, T]] = ListBuffer[SearchResult[I, T]]()
      val counter = new AtomicInteger()
      val len = raw.length
      def checkLen(count: Int): Unit = if (count == len) promise.success(buffer)
      raw.foreach(sr => {
        println(s"Before future $sr")
        filterFn(sr).onComplete {
          case Success(troo) =>
            if (troo) {
              println(s"FUTURE SUCCESS $sr")
              buffer.append(sr)
            }
            checkLen(counter.incrementAndGet())
          case _ => throw new RuntimeException("Whatever. Later. I'm pissed at this very moment.")
        }
        println("After future")
      })
      promise.future
    }
}
