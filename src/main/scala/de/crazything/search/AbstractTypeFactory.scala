package de.crazything.search

import org.apache.lucene.document.Document
import org.apache.lucene.search.Query

abstract class AbstractTypeFactory[I, T <: PkDataSet[I]] extends QueryConfig {

  def createInstanceFromDocument(doc: Document): PkDataSet[I]

  def setDataPool(data: Seq[T]): Unit

  def populateDocument(document: Document, dataSet: T): Unit

  def createQuery(t: T, queryEnable: Int): Query
}
