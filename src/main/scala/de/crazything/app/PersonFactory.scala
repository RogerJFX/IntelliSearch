package de.crazything.app

import de.crazything.search._
import org.apache.lucene.document._
import org.apache.lucene.index.Term
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search._

object PersonFactory extends AbstractTypeFactory[Int, Person] with CustomRegexReplace with QueryConfig {

  val PK = "id"

  val SALUTATION = "salutation"
  val FIRST_NAME = "firstName"
  val LAST_NAME = "lastName"
  val STREET = "street"
  val CITY = "city"



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

    val parser: QueryParser = new QueryParser(s"$LAST_NAME$PHONETIC", GermanIndexer.phoneticAnalyzer)
    val phoneticQuery = parser.parse(person.lastName)

    val queryBuilder = new BooleanQuery.Builder()

    if (checkEnabled(queryEnable, QueryEnabled.TERM)) {
      queryBuilder.add(new BoostQuery(new TermQuery(new Term(LAST_NAME, person.lastName)), Boost.TERM),
        BooleanClause.Occur.SHOULD)
    }

    if (checkEnabled(queryEnable, QueryEnabled.REGEX)) {
      queryBuilder.add(new BoostQuery(new RegexpQuery(new Term(LAST_NAME, createRegexTerm(person.lastName))), Boost.REGEX),
        BooleanClause.Occur.SHOULD)
    }

    if (checkEnabled(queryEnable, QueryEnabled.PHONETIC)) {
      queryBuilder.add(new BoostQuery(phoneticQuery, Boost.PHONETIC), BooleanClause.Occur.SHOULD)
    }

    if (checkEnabled(queryEnable, QueryEnabled.FUZZY)) {
      queryBuilder.add(new BoostQuery(new FuzzyQuery(new Term(LAST_NAME, person.lastName), FUZZY_MAX_EDITS), Boost.FUZZY),
        BooleanClause.Occur.SHOULD)
    }

    queryBuilder.build()

  }

  private[this] def checkEnabled(queryOr: Int, queryEnabled: Int): Boolean = (queryOr & queryEnabled) == queryEnabled

}
