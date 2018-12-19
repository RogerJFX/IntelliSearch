package de.crazything.search.ext

import de.crazything.search.{AbstractTypeFactory, CommonSearcher, DirectoryContainer}
import de.crazything.search.entity.{PkDataSet, QueryCriteria, SearchResult}
import de.crazything.search.ext.FilteringSearcher.MAGIC_NUM_DEFAULT_HITS_FILTERED
import org.apache.lucene.search.IndexSearcher

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

trait SimpleFiltering {

  /**
    * Common filtered search. Both, searching and filtering are blocking. Good for simple local operations.
    *
    * @param input not sufficient input to search similarities for.
    * @param factory The factory
    * @param searcherOption Another index as default to search? Ok then. Might even be a String due to implicits.
    * @param queryCriteria Local criteria
    * @param maxHits Local max hits.
    * @param filterFn The filtering function to pass.
    * @tparam I Type of primary key of type T
    * @tparam T Type to search for
    * @return Just the filtered sequence of results.
    */
  def simpleSearch[I, T <: PkDataSet[I]](input: T,
                                         factory: AbstractTypeFactory[I, T],
                                         searcherOption: Option[IndexSearcher] = DirectoryContainer.defaultSearcher,
                                         queryCriteria: Option[QueryCriteria] = None,
                                         maxHits: Int = MAGIC_NUM_DEFAULT_HITS_FILTERED,
                                         offset: Int = 0,
                                         filterFn: SearchResult[I, T] => Boolean): Seq[SearchResult[I, T]] = {
    val searchResult: Seq[SearchResult[I, T]] = CommonSearcher.search(input, factory, queryCriteria, maxHits, offset, searcherOption)
    searchResult.filter((sr: SearchResult[I, T]) => filterFn(sr))
  }
  /**
    * Common filtered search. Searching is async, filtering blocking. Good for simple filter functions.
    *
    * @param input not sufficient input to search similarities for.
    * @param factory The factory
    * @param searcherOption Another index as default to search? Ok then. Might even be a String due to implicits.
    * @param queryCriteria Local criteria
    * @param maxHits Local max hits.
    * @param filterFn The filtering function to pass.
    * @tparam I Type of primary key of type T
    * @tparam T Type to search for
    * @return Just the filtered sequence of results.
    */
  def simpleSearchAsync[I, T <: PkDataSet[I]](input: T,
                                              factory: AbstractTypeFactory[I, T],
                                              searcherOption: Option[IndexSearcher] = DirectoryContainer.defaultSearcher,
                                              queryCriteria: Option[QueryCriteria] = None,
                                              maxHits: Int = MAGIC_NUM_DEFAULT_HITS_FILTERED,
                                              offset: Int = 0,
                                              filterFn: SearchResult[I, T] => Boolean)(implicit ec: ExecutionContext): Future[Seq[SearchResult[I, T]]] = {
    val searchResult: Future[Seq[SearchResult[I, T]]] = CommonSearcher.searchAsync(input, factory, queryCriteria, maxHits, offset, searcherOption)
    val promise: Promise[Seq[SearchResult[I, T]]] = Promise[Seq[SearchResult[I, T]]]
    searchResult.onComplete {
      case Success(res) =>
        try {
          val result = res.filter(sr => filterFn(sr))
          promise.success(result)
        } catch {
          case exc: Exception => promise.failure(exc)
        }
      case Failure(t) => promise.failure(t)
    }
    promise.future
  }


}
