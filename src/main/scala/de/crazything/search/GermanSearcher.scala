package de.crazything.search

import java.util.concurrent.atomic.AtomicReference

import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.search.{IndexSearcher, Query, ScoreDoc}
import org.apache.lucene.store.Directory

object GermanSearcher extends QueryConfig{

  private val searcherRef: AtomicReference[IndexSearcher] = new AtomicReference[IndexSearcher]()

  def setDirectory(dir: Directory): Unit = {
    val reader: DirectoryReader = DirectoryReader.open(dir)
    searcherRef.set(new IndexSearcher(reader))
  }

  def search[I, T <: PkDataSet[I]](input: T,
                                   factory: AbstractTypeFactory[I, T],
                                   queriesEnabled: Int = QueryEnabled.ALL,
                                   maxHits: Int = 100): Seq[SearchResult[I, T]] = {
    val searcher = searcherRef.get()
    val query: Query = factory.createQuery(input, queriesEnabled)
    val hits: Array[ScoreDoc] = searcher.search(query, maxHits).scoreDocs
    hits.map(hit => {
      val hitDoc = searcher.doc(hit.doc)
      SearchResult[I, T](factory.createInstanceFromDocument(hitDoc).asInstanceOf[T], hit.score)
    })
  }

}
