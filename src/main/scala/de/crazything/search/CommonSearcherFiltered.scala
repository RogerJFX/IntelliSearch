package de.crazything.search

import java.util.concurrent.atomic.AtomicInteger

import de.crazything.search.entity.{PkDataSet, QueryCriteria, SearchResult}

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

import scala.concurrent.ExecutionContext.Implicits.global

/*
  * Global ExecutionContext did it in the end. I'm pissed at the moment. Was in trouble, since method
  * searchAsyncAsync did not want to work.
  *
  * TODO: Just get calm again and sweep a little bit.
  */
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
                                        filterFn: (SearchResult[I, T]) => Boolean)
                                        //(implicit ec: ExecutionContext)
  : Future[Seq[SearchResult[I, T]]] = {
    val searchResult: Future[Seq[SearchResult[I, T]]] = CommonSearcher.searchAsync(input, factory, queryCriteria, maxHits)
    val promise: Promise[Seq[SearchResult[I, T]]] = Promise[Seq[SearchResult[I, T]]]
    searchResult.onComplete {
      case Success(res) => promise.success(res.filter(sr => filterFn(sr)))
      case Failure(t) => throw new RuntimeException("Future failed in searchAsyncWithFilter", t)
      case _ => throw new RuntimeException("Unknown Error in searchAsyncWithFilter")
    }
    promise.future
  }

  def searchAsyncAsync[I, T <: PkDataSet[I]](input: T,
                                             factory: AbstractTypeFactory[I, T],
                                             queryCriteria: Option[QueryCriteria] = None,
                                             maxHits: Int = MAGIC_NUM_DEFAULT_HITS,
                                             filterFn: (SearchResult[I, T]) => Future[Boolean],
                                             filterTimeout: Duration = Duration.Inf)
                                             //(implicit ec: ExecutionContext)
  : Future[Seq[SearchResult[I, T]]] = {
    val searchResult: Future[Seq[SearchResult[I, T]]] = CommonSearcher.searchAsync(input, factory, queryCriteria, maxHits)
    val promise: Promise[Seq[SearchResult[I, T]]] = Promise[Seq[SearchResult[I, T]]]
    searchResult.onComplete {
      case Success(res) => promise.success(res.filter(sr => Await.result(filterFn(sr), filterTimeout)))
//      case Success(res) => promise.success(for {
//        x <- res if Await.result(filterFn(x), filterTimeout)
//      } yield x)
//      case Success(res) => promise.success(Await.result(doFilterAsync(res, filterFn), filterTimeout))
      case Failure(t) => throw new RuntimeException("Future failed in searchAsyncWithAsyncFilter", t)
      case _ => throw new RuntimeException("Unknown Error in searchAsyncWithAsyncFilter")
    }
    promise.future
  }

  // private desperation...
//  private def doFilterAsync[I, T <: PkDataSet[I]](raw: Seq[SearchResult[I, T]],
//                                                  filterFn: (SearchResult[I, T]) => Future[Boolean])
//                                               //  (implicit ec: ExecutionContext)
//  : Future[Seq[SearchResult[I, T]]] = {
//    val promise: Promise[Seq[SearchResult[I, T]]] = Promise[Seq[SearchResult[I, T]]]
//    val buffer: ListBuffer[SearchResult[I, T]] = ListBuffer[SearchResult[I, T]]()
//    val counter = new AtomicInteger()
//    val len = raw.length
//    def checkLen(count: Int): Unit = if (count == len) promise.success(buffer)
//    raw.foreach(sr => {
//      filterFn(sr).onComplete {
//        case Success(b) =>
//          if (b) {
//            buffer.append(sr)
//          }
//          checkLen(counter.incrementAndGet())
//        case _ => throw new RuntimeException("Whatever. Later. I'm pissed at this very moment.")
//      }
//    })
//    promise.future
//  }

}
