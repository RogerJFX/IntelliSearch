package de.crazything.search

import org.apache.lucene.document._
import org.apache.lucene.search.Query

abstract class AbstractTypeFactory[I, T <: PkDataSet[I]] extends FieldRegexReplace with QueryConfig{



  protected def addPkField(document: Document, fieldName: String, value: I): Unit = {
    // if we have a custom PK (so I), you have to override toString and implement fromString.
    //TODO: think about creating some PK wrapper with methods toString/fromString.
    document.add(new StoredField(fieldName, value.toString))
  }

  protected def addField(document: Document, fieldName: String, value: String): Unit = {
    document.add(new Field(fieldName, value, StringField.TYPE_NOT_STORED))
    document.add(new Field(s"$fieldName$PHONETIC", value, TextField.TYPE_NOT_STORED))
  }

  def createInstanceFromDocument(doc: Document): PkDataSet[I]

  def setDataPool(data: Seq[T]): Unit

  def populateDocument(document: Document, dataSet: T): Unit

  def createQuery(t: T, queryEnable: Int): Query
}
