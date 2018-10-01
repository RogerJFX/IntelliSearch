package de.crazything.search

import de.crazything.search.entity.{PkDataSet, QueryCriteria}
import de.crazything.search.persistence.IPersistence
import org.apache.lucene.document._
import org.apache.lucene.search.Query

abstract class AbstractTypeFactory[I, T <: PkDataSet[I]] extends QueryConfig with IPersistence[I, T] {

  protected def addPkField(document: Document, fieldName: String, value: I): Unit = {
    // if we have a custom PK (so I), you have to override toString and implement fromString.
    //TODO: think about creating some PK wrapper with methods toString/fromString.
    document.add(new StoredField(fieldName, value.toString))
    document.add(new Field("_" + fieldName, value.toString, StringField.TYPE_NOT_STORED))
  }

  protected def addField(document: Document, fieldName: String, value: String): Unit = {
    document.add(new Field(fieldName, value, StringField.TYPE_NOT_STORED))
    addTextField(document, s"$fieldName$PHONETIC_SUFFIX", value)
  }

  protected def addTextField(document: Document, fieldName: String, value: String): Unit = {
    document.add(new Field(fieldName, value, TextField.TYPE_NOT_STORED))
  }

  protected def addStoredField(document: Document, fieldName: String, value: String): Unit = {
    document.add(new Field(fieldName, value, StringField.TYPE_STORED))
  }

  def createInstanceFromDocument(doc: Document): Option[PkDataSet[I]]

  def putData(data: Seq[T]): Seq[T]

  def populateDocument(document: Document, dataSet: T): Unit

  def createQuery(t: T): Query

  def selectQueryCreator: (QueryCriteria, T) => Query

  def getPkFieldnameAsString(): String

}
