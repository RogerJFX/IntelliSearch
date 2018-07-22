package de.crazything.search

import java.util.concurrent.atomic.AtomicReference

import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.search.{IndexSearcher, Query, ScoreDoc}
import org.apache.lucene.store.Directory

object GermanSearcher {

  private val searcherRef: AtomicReference[IndexSearcher] = new AtomicReference[IndexSearcher]()

  def setDirectory(dir: Directory): Unit = {
    val reader: DirectoryReader = DirectoryReader.open(dir)
    searcherRef.set(new IndexSearcher(reader))
  }

  def search[T <: PkDataSet](input: T, factory: AbstractTypeFactory[T], numHits: Int = 100): Seq[T] = {
    val searcher = searcherRef.get()
    val query: Query = factory.createQuery(input)
    val hits: Array[ScoreDoc] = searcher.search(query, numHits).scoreDocs
    hits.map(hit => {
      val hitDoc = searcher.doc(hit.doc)
      factory.createInstanceFromDocument(hitDoc.get("id").toInt, hitDoc)
    })
  }

}
