package de.crazything.search

import java.net.URL
import java.nio.file.{Path, Paths}

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.document.Document
import org.apache.lucene.index.{IndexWriter, IndexWriterConfig}
import org.apache.lucene.store._

abstract class AbstractIndexer {

//  private[this] val url: URL = classOf[AbstractIndexer].getResource("/")
//  private[this] val target = new URL(url, "pindex")
//  private[this] val path: Path = Paths.get(target.toURI)

  protected def createIndex[T <: PkDataSet](analyzer: Analyzer, data: Seq[T], factory: AbstractTypeFactory[T]): Unit = {
    val config = new IndexWriterConfig(analyzer)
    val directory = /*new SimpleFSDirectory(path)*/new RAMDirectory
    val writer = new IndexWriter(directory, config)
    doIndex(writer, data, factory)
    writer.close()
    factory.setDataPool(data)
    putDirectoryReference(directory)
  }



  private def doIndex[T <: PkDataSet](writer: IndexWriter, data: Seq[T], factory: AbstractTypeFactory[T]): Unit = {
    data.foreach(dataSet => {
      val document: Document = new Document
      factory.populateDocument(document, dataSet)
      writer.addDocument(document)
    })
  }

  protected def putDirectoryReference(directory: Directory)

}
