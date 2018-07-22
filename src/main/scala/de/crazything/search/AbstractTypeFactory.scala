package de.crazything.search

import org.apache.lucene.document.Document
import org.apache.lucene.search.Query

abstract class AbstractTypeFactory[T <: PkDataSet] {

  def createInstanceFromDocument(id: Int, doc: Document): T

  def setDataPool(data: Seq[T]): Unit

  def populateDocument(document: Document, dataSet: T): Unit

  def createQuery(t: T): Query
}
