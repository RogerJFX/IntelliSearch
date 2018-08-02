package de.crazything.search

import java.util.concurrent.atomic.AtomicReference

import de.crazything.search.entity.{PkDataSet, QueryCriteria, SearchResult}
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.search.{IndexSearcher, Query, ScoreDoc}
import org.apache.lucene.store.Directory

import scala.concurrent.{ExecutionContext, Future}

object CommonSearcher extends MagicSettings{

  private val searcherRef: AtomicReference[Option[IndexSearcher]] = new AtomicReference[Option[IndexSearcher]](None)

  def setDirectory(dir: Directory): Unit =
    if (dir == null) {
      searcherRef.set(None)
    } else {
      val reader: DirectoryReader = DirectoryReader.open(dir)
      searcherRef.set(Some(new IndexSearcher(reader)))
    }

  def search[I, T <: PkDataSet[I]](input: T,
                                   factory: AbstractTypeFactory[I, T],
                                   queryCriteria: Option[QueryCriteria] = None,
                                   maxHits: Int = MAGIC_NUM_DEFAULT_HITS,
                                   searcherOption: Option[IndexSearcher] = searcherRef.get()): Seq[SearchResult[I, T]] = {
    //val searcherOption = searcherRef.get()

    searcherOption match {
      case Some(searcher) => {
        val query: Query =
          queryCriteria match {
            case None => factory.createQuery(input)
            case Some(qeOpt) => factory.selectQueryCreator(qeOpt, input)
          }

        val hits: Array[ScoreDoc] = searcher.search(query, maxHits).scoreDocs
        hits.map(hit => {
          val hitDoc = searcher.doc(hit.doc)
          SearchResult[I, T](factory.createInstanceFromDocument(hitDoc).asInstanceOf[T], hit.score)
        })
      }
      case None => throw new IllegalStateException("Nobody told us to have a directory reference. No yet finished? " +
        "Anything async? We should fix this then")
    }

  }

  def searchAsync[I, T <: PkDataSet[I]](input: T,
                                        factory: AbstractTypeFactory[I, T],
                                        queryCriteria: Option[QueryCriteria] = None,
                                        maxHits: Int = MAGIC_NUM_DEFAULT_HITS,
                                        searcherOption: Option[IndexSearcher] = searcherRef.get())
                                       (implicit ec: ExecutionContext): Future[Seq[SearchResult[I, T]]] = Future {
    search(input, factory, queryCriteria, maxHits, searcherOption)
  }


}
