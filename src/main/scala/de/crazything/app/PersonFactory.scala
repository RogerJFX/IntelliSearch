package de.crazything.app

import de.crazything.search._
import org.apache.lucene.document._
import org.apache.lucene.search._

object PersonFactory extends AbstractTypeFactory[Int, Person] with CustomRegexReplace with QueryConfig {

  private[this] val PK = "id"

  private[this] val SALUTATION = "salutation"
  private[this] val FIRST_NAME = "firstName"
  private[this] val LAST_NAME = "lastName"
  private[this] val STREET = "street"
  private[this] val CITY = "city"

  override def createInstanceFromDocument(doc: Document): PkDataSet[Int] = {
    DataContainer.findById(doc.get(PK).toInt)
  }

  override def setDataPool(data: Seq[Person]): Unit = {
    DataContainer.setData(data)
  }

  /**
    * Ok, got it! Here's the hook: the phonetic analyzer does not feel responsible for StringFields but only for TextFields.
    * So we have to add the Class' fields twice. This is done in AbstractTypeFactory.addField.
    *
    * @param document The Lucene document to populate
    * @param person Object to be added to document.
    */
  override def populateDocument(document: Document, person: Person): Unit = {

    addPkField(document, PK, person.id)

    addField(document, SALUTATION, person.salutation)
    addField(document, FIRST_NAME, person.firstName)
    addField(document, LAST_NAME, person.lastName)
    addField(document, STREET, person.street)
    addField(document, CITY, person.city)

  }

  override def createQuery(person: Person, queryEnable: Int = QueryEnabled.ALL): Query = {

    import CustomQuery._

    val queryBuilder = new BooleanQuery.Builder()

    if (checkEnabled(queryEnable, QueryEnabled.TERM)) {
      queryBuilder.add((LAST_NAME, person.lastName, Boost.TERM).exact, BooleanClause.Occur.SHOULD)
    }

    if (checkEnabled(queryEnable, QueryEnabled.REGEX)) {
      queryBuilder.add((LAST_NAME, createRegexTerm(person.lastName), Boost.REGEX).regex, BooleanClause.Occur.SHOULD)
    }

    if (checkEnabled(queryEnable, QueryEnabled.PHONETIC)) {
      queryBuilder.add((LAST_NAME, person.lastName, Boost.PHONETIC).phonetic, BooleanClause.Occur.SHOULD)
    }

    if (checkEnabled(queryEnable, QueryEnabled.FUZZY)) {
      queryBuilder.add((LAST_NAME, person.lastName, Boost.FUZZY).fuzzy, BooleanClause.Occur.SHOULD)
    }

    queryBuilder.build()

  }

  private[this] def checkEnabled(queryOr: Int, queryEnabled: Int): Boolean = (queryOr & queryEnabled) == queryEnabled

}
