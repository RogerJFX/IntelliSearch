package de.crazything.search.ext

import de.crazything.search.entity.{PkDataSet, QueryCriteria, SearchResult}
import de.crazything.search.{AbstractTypeFactory, CommonSearcher, DirectoryContainer, MagicSettings}
import org.apache.lucene.search.IndexSearcher

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

import scala.concurrent.ExecutionContext.Implicits.global

object FilteringSearcherBulk extends AbstractFilteringSearcher with MagicSettings{

  def search[I, T <: PkDataSet[I]]
  (input: T,
   factory: AbstractTypeFactory[I, T],
   searcherOption: Option[IndexSearcher] = DirectoryContainer.defaultSearcher,
   queryCriteria: Option[QueryCriteria] = None,
   maxHits: Int = MAGIC_NUM_DEFAULT_HITS_FILTERED,
   offset: Int = 0,
   filterFn: (Seq[SearchResult[I, T]]) => Future[Seq[Boolean]],
   secondLevelTimeout: FiniteDuration = MAGIC_ONE_DAY): Future[Seq[SearchResult[I, T]]] = {
    def secondLevelClass(res: Seq[SearchResult[I, T]]): FilterMaster[I, T] = new FilterAsyncFutureBulk(res, filterFn)
    val searchResult: Future[Seq[SearchResult[I, T]]] = CommonSearcher.searchAsync(input, factory, queryCriteria, maxHits, offset, searcherOption)
    processFirstLevel(searchResult, secondLevelClass, secondLevelTimeout)
  }

  private class FilterAsyncFutureBulk[I, T <: PkDataSet[I]](raw: Seq[SearchResult[I, T]],
                                                            filterFn: (Seq[SearchResult[I, T]]) => Future[Seq[Boolean]])
    extends FilterMaster[I, T] {
    override def createFuture(): Future[Seq[SearchResult[I, T]]] = {
      filterFn(raw).map(assoc => {
        val list: ListBuffer[SearchResult[I, T]] = ListBuffer()
        for(i <- raw.indices) {
          if(assoc(i)) {
            list.append(raw(i))
          }
        }
        list
      })
    }
  }
}
