package de.crazything.search

import de.crazything.search.entity.PkDataSet
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.store.Directory

object CommonIndexer extends AbstractIndexer {

  override protected def putDirectoryReference(directory: Directory, name: String): Unit = {
    DirectoryContainer.setDirectory(name, directory)
  }

  def index[I, T <: PkDataSet[I]](data: Seq[T],
                                  factory: AbstractTypeFactory[I, T],
                                  name: String = DEFAULT_DIRECTORY_NAME)
                                 (implicit phoneticAnalyzer: Analyzer): Unit =
    createIndex(phoneticAnalyzer, data, factory, name)

}
