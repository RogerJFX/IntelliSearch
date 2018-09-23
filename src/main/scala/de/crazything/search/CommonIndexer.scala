package de.crazything.search

import de.crazything.search.entity.PkDataSet
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.document.Document
import org.apache.lucene.index._
import org.apache.lucene.search.Query
import org.apache.lucene.store.Directory
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable.ListBuffer

object CommonIndexer extends AbstractIndexer with QueryConfig {

  private val logger: Logger = LoggerFactory.getLogger(CommonIndexer.getClass)

  override protected def putDirectoryReference(directory: Directory, name: String): Unit = {
    DirectoryContainer.setDirectory(name, directory)
  }

  def index[I, T <: PkDataSet[I]](data: Seq[T],
                                  factory: AbstractTypeFactory[I, T],
                                  name: String = DEFAULT_DIRECTORY_NAME)
                                 (implicit phoneticAnalyzer: Analyzer): Unit =
    createIndex(phoneticAnalyzer, data, factory, name)

  //TODO: do not flush! Just implement some flush method with a warning inside.
  def updateData[I, T <: PkDataSet[I]](data: Seq[T],
                                       factory: AbstractTypeFactory[I, T],
                                       name: String = DEFAULT_DIRECTORY_NAME)
                                      (implicit phoneticAnalyzer: Analyzer): Unit = {
    val directory = DirectoryContainer.pickDirectoryForName(name)
    val config = new IndexWriterConfig(phoneticAnalyzer)
    // TODO: not included to transaction. MUST BE CHANGED.
    deleteData(data, factory, name, forceFlush = false) // Lucene does nothing else than deleting and adding.
    val writer = new IndexWriter(directory, config)
    try {
      val dataBuffer = new ListBuffer[T]
      data.foreach(dataSet => {
        dataBuffer.append(dataSet)
        val document: Document = new Document
        factory.populateDocument(document, dataSet)
        writer.addDocument(document)
      })
      writer.commit()
      writer.flush()
      factory.setData(dataBuffer)
    } catch {
      case e: Exception =>
        writer.rollback()
        logger.error("Unable updating data from Lucene directory. Rolling back.", e)
    }
    writer.close()
    DirectoryContainer.setDirectory(name, directory)
  }


  //TODO: move me. This is just a poc.
  def deleteData[I, T <: PkDataSet[I]](data: Seq[T],
                                       factory: AbstractTypeFactory[I, T],
                                       name: String = DEFAULT_DIRECTORY_NAME,
                                       forceFlush: Boolean = true)
                                      (implicit phoneticAnalyzer: Analyzer): Unit = {
    import de.crazything.search.CustomQuery._
    this.synchronized {
      val directory = DirectoryContainer.pickDirectoryForName(name)
      val config = new IndexWriterConfig(phoneticAnalyzer)
      val writer = new IndexWriter(directory, config)

      val pks: Seq[I] = data.map(d => d.getId)
      try {
        val deleteQuery: Query = pks.map(pk => ("_id", pk + "").exact.must)
        writer.deleteDocuments(deleteQuery)
        //writer.forceMergeDeletes(true)
        writer.commit()
        if(forceFlush)
          writer.flush()
        factory.deleteData(data)
      } catch {
        case e: Exception =>
          writer.rollback()
          writer.close()
          throw new RuntimeException("Unable to delete data from Lucene directory. Rolling back.", e)
      }
      writer.close()
      if(forceFlush)
        DirectoryContainer.setDirectory(name, directory)
    }
  }

//  def checkIndex(name: String): Unit = {
//    val directory = DirectoryContainer.pickDirectoryForName(name)
//    val reader: DirectoryReader = DirectoryReader.open(directory)
//    for(i <- 0 until reader.maxDoc()) {
//      println(reader.document(i).get("lastName"))
//    }
//  }


}
