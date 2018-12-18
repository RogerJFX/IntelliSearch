package de.crazything.app.factory

import de.crazything.app.analyze.NoLanguage
import de.crazything.app.entity.Person
import de.crazything.search.AbstractTypeFactory
import de.crazything.search.entity.{PkDataSet, QueryCriteria}
import org.apache.lucene.document.Document
import org.apache.lucene.search.Query

object MockingPersonFactory extends AbstractTypeFactory[Int, Person] with NoLanguage {

  import de.crazything.search.CustomQuery._

  override def createInstanceFromDocument(doc: Document): Option[Person] = PersonFactoryDE.createInstanceFromDocument(doc)

  override def putData(data: Seq[Person]): Seq[Person] = {
    throw new Exception("Foo")
  }

  override def deleteData(data: Seq[Person]): Unit = throw new RuntimeException("bar")

  override def populateDocument(document: Document, dataSet: Person): Unit = PersonFactoryDE.populateDocument(document, dataSet)

  override def createQuery(t: Person): Query = {

    Seq(
      ("lastName", "Hösl").exact, // should is default
      ("firstName", "Fr*").wildcard,
      ("lastName", ".*").regex,
      ("lastName", "Mayer").phonetic)
  }

  override def selectQueryCreator: (QueryCriteria, Person) => Query = (criteria, person) => {
    if (criteria.queryName == "dummy") {
      Seq(
        ("firstName", "Roger").exact.must,
        ("lastName", person.lastName).exact.must,
        ("lastName", "Flintstone").exact.mustNot, // ridiculous - just for the code coverage...
        ("lastName", "ABCABCABCABCABCABCABCABCABCABC").exact.should // ridiculous - ...
      )
    } else createQuery(person)
  }

  /**
    * Store the data that later is searched.
    *
    * @param data The data.
    */
  override def setData(data: Seq[Person]): Seq[Person] = {
    println("Arsch")
    PersonFactoryDE.setData(data)
  }

  /**
    * Select * from Seq where id = ´id´ .
    *
    * @param id Id of data set.
    * @return Found data set.
    */
  override def findById(id: Int): Option[PkDataSet[Int]] = PersonFactoryDE.findById(id)



  override def getPkFieldnameAsString(): String = "id"
}

