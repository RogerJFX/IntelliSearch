package de.crazything.search

import java.util.concurrent.atomic.AtomicReference

import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.store.Directory

import scala.collection.concurrent

object DirectoryContainer extends MagicSettings {

  private val _defaultSearcher: AtomicReference[Option[IndexSearcher]] = new AtomicReference[Option[IndexSearcher]](None)

  val searcherMap: concurrent.Map[String, Option[IndexSearcher]] = concurrent.TrieMap()

  def setDirectory(name: String, dir: Directory): Unit = {
    val searcher: Option[IndexSearcher] = if(dir == null) {
      None
    } else {
      val reader: DirectoryReader = DirectoryReader.open(dir)
      Some(new IndexSearcher(reader))
    }
    searcherMap.put(name, searcher)
    if(name == DEFAULT_DIRECTORY_NAME) {
      _defaultSearcher.set(searcher)
    }
  }

  def pickSearcher(name: String): Option[IndexSearcher] = {
    val sOpt: Option[Option[IndexSearcher]] = searcherMap.get(name)
    if(sOpt.isEmpty) {
      None
    } else {
      sOpt.get // may still be None
    }
  }

  def defaultSearcher: Option[IndexSearcher] = _defaultSearcher.get()
}

trait DirectoryContainer {
  implicit def stringToSearcher(name: String): Option[IndexSearcher] = DirectoryContainer.pickSearcher(name)
}
