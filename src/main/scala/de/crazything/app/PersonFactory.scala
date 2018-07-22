package de.crazything.app

import de.crazything.search.{AbstractTypeFactory, FieldModify, GermanIndexer}
import org.apache.lucene.document._
import org.apache.lucene.index.Term
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.{Query, RegexpQuery, TermQuery}
import org.apache.lucene.util.Version

object PersonFactory extends AbstractTypeFactory[Person] with FieldModify{

  val SALUTATION = "salutation"
  val FIRST_NAME = "firstName"
  val LAST_NAME = "lastName"
  val STREET = "street"
  val CITY = "city"

  override def createInstanceFromDocument(id: Int, doc: Document): Person = {
    DataContainer.findById(id)
  }

  override def setDataPool(data: Seq[Person]): Unit = {
    DataContainer.setData(data)
  }

  override def populateDocument(document: Document, dataSet: Person): Unit = {
    document.add(new StoredField("id", dataSet.id))
    document.add(new Field(SALUTATION, prepareField(dataSet.salutation), TextField.TYPE_NOT_STORED))
    document.add(new Field(FIRST_NAME, prepareField(dataSet.firstName), TextField.TYPE_NOT_STORED))
    document.add(new Field(LAST_NAME, prepareField(dataSet.lastName), TextField.TYPE_NOT_STORED))
    document.add(new Field(STREET, prepareField(dataSet.street), TextField.TYPE_NOT_STORED))
    document.add(new Field(CITY, prepareField(dataSet.city), TextField.TYPE_NOT_STORED))
  }

  override def createQuery(t: Person): Query = {
    //new RegexpQuery(new Term(LAST_NAME, prepareField(t.lastName)))
    // println(prepareField(t.lastName))
    //new TermQuery(new Term(LAST_NAME, prepareField(t.lastName)))

    val parser: QueryParser = new QueryParser(LAST_NAME, GermanIndexer.phoneticAnalyzer)
    parser.parse(prepareField(t.lastName))
  }

}
