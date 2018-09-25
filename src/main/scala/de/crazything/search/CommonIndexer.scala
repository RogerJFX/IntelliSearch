package de.crazything.search

import de.crazything.search.entity.PkDataSet
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.document.Document
import org.apache.lucene.index._
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

//  private def writer4DirName(name: String)(implicit phoneticAnalyzer: Analyzer): IndexWriter = {
//    val directory = DirectoryContainer.pickDirectoryForName(name)
//    val config = new IndexWriterConfig(phoneticAnalyzer)
//    new IndexWriter(directory, config)
//  }

  //TODO: not sure at the moment, if the exception handling is proper.
  /**
    * This is the common, but not recommended way to update data.
    *
    * Recommended is to setup a complete new index in parallel and change reference in the end.
    *
    * @param data New Data to write. It can be data to update or even new data.
    * @param factory Factory to pass.
    * @param name Name of directory.
    * @param phoneticAnalyzer Custom analyzer.
    * @tparam I Type of PK
    * @tparam T Type of Data.
    */
  def updateData[I, T <: PkDataSet[I]](data: Seq[T],
                                       factory: AbstractTypeFactory[I, T],
                                       name: String = DEFAULT_DIRECTORY_NAME,
                                       doFlush: Boolean =true)
                                      (implicit phoneticAnalyzer: Analyzer): Unit = {
    this.synchronized {
      val directory = DirectoryContainer.pickDirectoryForName(name)
      val config = new IndexWriterConfig(phoneticAnalyzer)
      val writer = new IndexWriter(directory, config)
      var oldData: Seq[T] = Seq()
      try {
        doDeleteData(data, factory, writer)
        val dataBuffer = new ListBuffer[T]
        data.foreach(dataSet => {
          dataBuffer.append(dataSet)
          val document: Document = new Document
          factory.populateDocument(document, dataSet)
          writer.addDocument(document)
        })
        writer.commit()
        if(doFlush) {
          writer.flush()
        }
        oldData = factory.setData(dataBuffer)
      } catch {
        case e: Exception =>
          writer.rollback()
          factory.setData(oldData)
          logger.error("Unable to update data of Lucene directory. Rolling back.", e)
      }
      writer.close()
      putDirectoryReference(directory, name)
    }
  }

  /**
    * This is the common, but not recommended way to delete data. Think of frequently create a complete
    * new index instead.
    *
    * Note: normally there is no need to flush immediately if there is data stored somewhere in a database or
    * where so ever. This is, because e.g. the CommonSearcher filters for existence of data in this store.
    * Thus even if the data tmp. remains in the index, it will not come up anymore.
    *
    * @param data             The data to delete.
    * @param factory          Factory that can handle the data to pass.
    * @param name             Directory name default is the main directory.
    * @param forceFlush       If true, the writer will flush after commit. This is expensive. Nevertheless defaults to true!
    * @param phoneticAnalyzer The custom analyzer
    * @tparam I Type of PK
    * @tparam T Type of data
    */
  def deleteData[I, T <: PkDataSet[I]](data: Seq[T],
                                       factory: AbstractTypeFactory[I, T],
                                       name: String = DEFAULT_DIRECTORY_NAME,
                                       forceFlush: Boolean = false)
                                      (implicit phoneticAnalyzer: Analyzer): Unit = {
    this.synchronized {
      val directory = DirectoryContainer.pickDirectoryForName(name)
      val config = new IndexWriterConfig(phoneticAnalyzer)
      val writer = new IndexWriter(directory, config)
      try {
        doDeleteData(data, factory, writer)
        if (forceFlush) {
          writer.flush()
        }
        factory.deleteData(data)
      } catch {
        case e: Exception =>
          writer.close()
          throw new RuntimeException("Unable to delete data from Lucene directory. Rolling back.", e)
      }
      writer.close()
      if (forceFlush)
        putDirectoryReference(directory, name)
    }
  }


}
