package de.crazything.search

import de.crazything.search.entity.PkDataSet
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
    document.add(new Field(s"$fieldName$PHONETIC_SUFFIX", value, TextField.TYPE_NOT_STORED))
  }

  def createInstanceFromDocument(doc: Document): PkDataSet[I]

  def setDataPool(data: Seq[T]): Unit

  def populateDocument(document: Document, dataSet: T): Unit

  def createQuery(t: T): Query

  /**
    * Normally this should not be used. Just if anybody want's to customize, that means temporarily
    * disable some type of query.
    *
    * Note: if you want to use this method and pass any non empty Option for queryEnable, you must
    * override this method, otherwise an exception will be thrown. If you only pass 'None', createQuery(T) will
    * be called.
    *
    * @param t The instance
    * @param queryEnable Queries to be enabled (or disabled, if not in mask)
    * @return Normally a BooleanQuery
    */
  def createQuery(t: T, queryEnable: Option[Int] = None): Query = queryEnable match {
    case None => createQuery(t)
    case _ => throw new RuntimeException("Please override createQuery with queryEnable option")
  }
}
