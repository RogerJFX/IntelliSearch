package de.crazything.search

import de.crazything.search.entity.PkDataSet
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.document.Document
import org.apache.lucene.index.{IndexWriter, IndexWriterConfig}
import org.apache.lucene.search.Query
import org.apache.lucene.store._

abstract class AbstractIndexer extends MagicSettings {

  protected def createIndex[I, T <: PkDataSet[I]](analyzer: Analyzer,
                                                  data: Seq[T],
                                                  factory: AbstractTypeFactory[I, T],
                                                  name: String): Unit = {
    val config = new IndexWriterConfig(analyzer)
    val directory = new RAMDirectory
    val writer = new IndexWriter(directory, config)
    doIndex(writer, data, factory)
    writer.close()
    factory.setDataPool(data)
    putDirectoryReference(directory, name)
  }

  protected def doDeleteData[I, T <: PkDataSet[I]](data: Seq[T],
                                         factory: AbstractTypeFactory[I, T],
                                         writer: IndexWriter)
                                        (implicit phoneticAnalyzer: Analyzer): Unit = {
    import de.crazything.search.CustomQuery._
    val pks: Seq[I] = data.map(d => d.getId)
    val deleteQuery: Query = pks.map(pk => (s"_${factory.getPkFieldnameAsString()}", s"$pk").exact.must)
    try {
      writer.deleteDocuments(deleteQuery)
      writer.commit()
    } catch {
      case e: Exception =>
        writer.rollback()
        throw new RuntimeException("Unable to delete data from Lucene directory. Rolling back.", e)
    }
  }

  private def doIndex[I, T <: PkDataSet[I]](writer: IndexWriter, data: Seq[T], factory: AbstractTypeFactory[I, T]): Unit = {
    data.foreach(dataSet => {
      val document: Document = new Document
      factory.populateDocument(document, dataSet)
      writer.addDocument(document)
    })
  }

  protected def putDirectoryReference(directory: Directory, name: String)

}
