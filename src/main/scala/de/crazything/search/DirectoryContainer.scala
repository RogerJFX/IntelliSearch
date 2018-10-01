package de.crazything.search

import java.util.concurrent.atomic.AtomicReference

import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.store.Directory

import scala.collection.concurrent

object DirectoryContainer extends MagicSettings {

  private val _defaultSearcher: AtomicReference[Option[IndexSearcher]] = new AtomicReference[Option[IndexSearcher]](None)

  private val searcherMap: concurrent.Map[String, Option[IndexProps]] = concurrent.TrieMap()

  def setDirectory(name: String, dir: Directory): Unit = {
    this.synchronized {
      val searcher: Option[IndexProps] = if (dir == null) {
        None
      } else {
        val reader: DirectoryReader = DirectoryReader.open(dir)
        Some(IndexProps(dir, new IndexSearcher(reader)))
      }

      val oldSearcher: Option[Option[IndexProps]] = searcherMap.get(name)

      searcherMap.put(name, searcher)

      if (name == DEFAULT_DIRECTORY_NAME && searcher.nonEmpty) {
        _defaultSearcher.set(Some(searcher.get.searcher))
      }
      /*
      I really think of leaving it like so or similar, even if it smells like hell.
     */
      oldSearcher match {
        case Some(opt) => opt match {
          case Some(s) => new Thread() {
            override def run(): Unit = {
              Thread.sleep(DEFAULT_MILLIS_TO_CLOSE_OLD_READER) // F... the InterruptedException
              s.searcher.getIndexReader.close()
            }
          }.start()
          case _ =>
        }
        case _ =>
      }
    }
  }

  def pickSearcherForName(name: String): Option[IndexSearcher] = {
    val sOpt: Option[Option[IndexProps]] = searcherMap.get(name)
    if (sOpt.isEmpty || sOpt.get.isEmpty) {
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
