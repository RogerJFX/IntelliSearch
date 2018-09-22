package de.crazything.search

import de.crazything.search.entity.{PkDataSet, QueryCriteria, SearchResult, SearchResultCollection}
import de.crazything.search.ext.MappingSearcher.MAGIC_ONE_DAY
import de.crazything.search.utils.FutureUtil
import de.crazything.service.RestClient
import org.apache.lucene.search.{IndexSearcher, Query, ScoreDoc}
import play.api.libs.json.{Format, OFormat}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

object CommonSearcher extends MagicSettings{

  def search[I, T <: PkDataSet[I]](input: T,
                                   factory: AbstractTypeFactory[I, T],
                                   queryCriteria: Option[QueryCriteria] = None,
                                   maxHits: Int = MAGIC_NUM_DEFAULT_HITS,
                                   searcherOption: Option[IndexSearcher] = DirectoryContainer.defaultSearcher): Seq[SearchResult[I, T]] = {
    searcherOption match {
      case Some(searcher) =>
        val query: Query =
          queryCriteria match {
            case None => factory.createQuery(input)
            case Some(qeOpt) => factory.selectQueryCreator(qeOpt, input)
          }

        val hits: Array[ScoreDoc] = searcher.search(query, maxHits).scoreDocs

        hits.flatMap(hit => {
          val o: Option[PkDataSet[I]] = factory.createInstanceFromDocument(searcher.doc(hit.doc))
          hits.filter(_ => o.isDefined).map(_ => SearchResult[I, T](o.get.asInstanceOf[T], hit.score))
        })

      case None => throw new IllegalStateException("Nobody told us to have a directory reference. No yet finished? " +
        "Anything async? We should fix this then")
    }

  }

  def searchAsync[I, T <: PkDataSet[I]](input: T,
                                        factory: AbstractTypeFactory[I, T],
                                        queryCriteria: Option[QueryCriteria] = None,
                                        maxHits: Int = MAGIC_NUM_DEFAULT_HITS,
                                        searcherOption: Option[IndexSearcher] = DirectoryContainer.defaultSearcher)
                                       (implicit ec: ExecutionContext): Future[Seq[SearchResult[I, T]]] = Future {
    search(input, factory, queryCriteria, maxHits, searcherOption)
  }

  def searchRemote[I, T <: PkDataSet[I]](input: T,
                                         url: String,
                                         timeout: FiniteDuration = MAGIC_ONE_DAY)
                                        (implicit fmt: OFormat[T],
                                         rmt: OFormat[SearchResultCollection[I, T]],
                                         ec: ExecutionContext): Future[Seq[SearchResult[I, T]]] = {
    val promise = Promise[Seq[SearchResult[I, T]]]()
    val postFuture: Future[SearchResultCollection[I, T]] = RestClient.post[T, SearchResultCollection[I, T]](url, input)
    val timingOutFuture = FutureUtil.futureWithTimeout(postFuture, timeout)
    timingOutFuture.onComplete {
      case Success(r: SearchResultCollection[I, T]) => promise.success(r.entries)
      case Failure(t) => promise.failure(t)
    }
    promise.future
  }

}
