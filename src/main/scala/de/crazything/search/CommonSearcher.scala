package de.crazything.search

import java.util.concurrent.atomic.AtomicReference

import de.crazything.search.entity.{PkDataSet, SearchResult}
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.search.{IndexSearcher, Query, ScoreDoc}
import org.apache.lucene.store.Directory

object CommonSearcher {

  private val MAGIC_NUM_DEFAULT_HITS = 100

  private val searcherRef: AtomicReference[Option[IndexSearcher]] = new AtomicReference[Option[IndexSearcher]]()

  def setDirectory(dir: Directory): Unit = {
    val reader: DirectoryReader = DirectoryReader.open(dir)
    searcherRef.set(Some(new IndexSearcher(reader)))
  }

  def search[I, T <: PkDataSet[I]](input: T,
                                   factory: AbstractTypeFactory[I, T],
                                   queriesEnabled: Option[Int] = None,
                                   maxHits: Int = MAGIC_NUM_DEFAULT_HITS): Seq[SearchResult[I, T]] = {
    val searcherOption = searcherRef.get()

    searcherOption match {
      case Some(searcher) => {
        val query: Query =
          queriesEnabled match {
            case None => factory.createQuery(input)
            case qeOpt => factory.createQuery(input, qeOpt)
          }

        val hits: Array[ScoreDoc] = searcher.search(query, maxHits).scoreDocs
        hits.map(hit => {
          val hitDoc = searcher.doc(hit.doc)
          SearchResult[I, T](factory.createInstanceFromDocument(hitDoc).asInstanceOf[T], hit.score)
        })
      }
      case None => throw new RuntimeException("Nobody told us to have a searcher reference. No yet finished? " +
        "Anything async? We should fix this then")
    }
  }

}
