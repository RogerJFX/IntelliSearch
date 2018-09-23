package de.crazything.search

import java.util.concurrent.atomic.AtomicReference

import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.store.Directory

import scala.collection.concurrent

// TODO: fix this crap!
object DirectoryContainer extends MagicSettings {

  private val _defaultSearcher: AtomicReference[Option[IndexSearcher]] = new AtomicReference[Option[IndexSearcher]](None)

  private val searcherMap: concurrent.Map[String, Option[IndexProps]] = concurrent.TrieMap()

  def setDirectory(name: String, dir: Directory): Unit = {
    val searcher: Option[IndexProps] = if(dir == null) {
      None
    } else {
      val reader: DirectoryReader = DirectoryReader.open(dir)
      Some(IndexProps(dir, new IndexSearcher(reader)))
    }
    //TODO: old readers must be closed. Does not seem to work so far. Some tests still fail.
//    val oldSearcher: Option[Option[IndexProps]] = searcherMap.get(name)
//    oldSearcher match {
//      case Some(opt) => opt match {
//        case Some(s) => s.searcher.getIndexReader.close()
//        case _ =>
//      }// opt.get.searcher.getIndexReader.close()
//      case _ =>
//    }

    searcherMap.put(name, searcher)
    if(name == DEFAULT_DIRECTORY_NAME && searcher.nonEmpty) {
      _defaultSearcher.set(Some(searcher.get.searcher))
    }
  }

  def pickSearcherForName(name: String): Option[IndexSearcher] = {
    val sOpt: Option[Option[IndexProps]] = searcherMap.get(name)
    if(sOpt.isEmpty || sOpt.get.isEmpty) {
      None
    } else {
      Some(sOpt.get.get.searcher) // Oh no! Is it a dog's fart? Change this, dude!
    }
  }

  def pickDirectoryForName(name: String): Directory = {
    searcherMap.get(name) match {
      case Some(res) => res.get.directory
      case _ => throw new RuntimeException("No directory for this name.")
    }
  }

  def defaultSearcher: Option[IndexSearcher] = _defaultSearcher.get()

  case class IndexProps(directory: Directory, searcher: IndexSearcher)
}

trait DirectoryContainer {
  implicit def stringToSearcher(name: String): Option[IndexSearcher] = DirectoryContainer.pickSearcherForName(name)
}
