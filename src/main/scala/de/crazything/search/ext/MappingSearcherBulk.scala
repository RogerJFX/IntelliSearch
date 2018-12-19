package de.crazything.search.ext

import de.crazything.search.entity.{MappedResults, PkDataSet, QueryCriteria, SearchResult}
import de.crazything.search.{AbstractTypeFactory, CommonSearcher, DirectoryContainer, MagicSettings}
import org.apache.lucene.search.IndexSearcher

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Use this MappingSearcher, if there are many results to be mapped. Unlike the "normal" MappingSearcher, the
  * mapping is being processed in a conventional loop internally.
  *
  * The "normal" MappingSearcher would handle any single target SearchResult and start a search upon this data on another
  * index. This might become very expensive. If there are remote calls, it soon will fail, since the underlying IO would
  * break after some time.
  *
  * But be aware the mapper function to pass must not shuffle due to e.g. async internal processing. So
  * the result sequence must be right in the same order as the target sequence.
  *
  * Again mind, what we have in our sources:
  *
  * {{{
  *
  *   mappingFn(raw).map(assoc => {
  *         val list: ListBuffer[MappedResults[I1, I2, T1, T2]] = ListBuffer()
  *         for(i <- raw.indices) {
  *           list.append(MappedResults(raw(i), assoc(i)))
  *         }
  *         list
  *       })
  *
  * }}}
  *
  * So it is an absolute MUST to have both sequences in right order to assemble securely.
  *
  */
object MappingSearcherBulk extends AbstractMappingSearcher with MagicSettings{

  def search[I1, I2, T1 <: PkDataSet[I1], T2 <: PkDataSet[I2]]
  (input: T1,
   factory: AbstractTypeFactory[I1, T1],
   searcherOption: Option[IndexSearcher] = DirectoryContainer.defaultSearcher,
   queryCriteria: Option[QueryCriteria] = None,
   maxHits: Int = MAGIC_NUM_DEFAULT_HITS_FILTERED,
   offset: Int = 0,
   mapperFn: Seq[SearchResult[I1, T1]] => Future[Seq[Seq[SearchResult[I2, T2]]]],
   secondLevelTimeout: FiniteDuration = MAGIC_ONE_DAY)
  : Future[Seq[MappedResults[I1, I2, T1, T2]]] = {
    def secondLevelClass(res: Seq[SearchResult[I1, T1]]): MapperMaster[I1, I2, T1, T2] = new MapperAsyncFutureBulk(res, mapperFn)

    val searchResult: Future[Seq[SearchResult[I1, T1]]] =
      CommonSearcher.searchAsync(input, factory, queryCriteria, maxHits, offset, searcherOption)
    processFirstLevel(searchResult, secondLevelClass, secondLevelTimeout)
  }

  private class MapperAsyncFutureBulk[I1, I2, T1 <: PkDataSet[I1], T2 <: PkDataSet[I2]]
  (raw: Seq[SearchResult[I1, T1]],
   mappingFn: Seq[SearchResult[I1, T1]] => Future[Seq[Seq[SearchResult[I2, T2]]]])
    extends MapperMaster[I1, I2, T1, T2] {
    override def createFuture(): Future[Seq[MappedResults[I1, I2, T1, T2]]] = {
      mappingFn(raw).map(assoc => {
        val list: ListBuffer[MappedResults[I1, I2, T1, T2]] = ListBuffer()
        for(i <- raw.indices) {
          list.append(MappedResults(raw(i), assoc(i)))
        }
        list
      })
    }
  }
}
