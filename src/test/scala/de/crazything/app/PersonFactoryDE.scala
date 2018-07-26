package de.crazything.app

import de.crazything.search._
import de.crazything.search.entity.PkDataSet
import org.apache.lucene.document._
import org.apache.lucene.search._

// TODO: get rid of nasty lucene imports. We don't need em here. So f...[beep] create some implicits in the API
object PersonFactoryDE extends AbstractTypeFactory[Int, Person] with GermanRegexReplace with GermanLanguage {

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

  override def populateDocument(document: Document, person: Person): Unit = {

    addPkField(document, PK, person.id)

    addField(document, SALUTATION, person.salutation)
    addField(document, FIRST_NAME, person.firstName)
    addField(document, LAST_NAME, person.lastName)
    addField(document, STREET, person.street)
    addField(document, CITY, person.city)

  }

  /**
    * The normal method.
    *
    * We have the chance to pass a custom boost factor and a custom fuzzyMaxEdit (aka Levenstein range).
    *
    * The only thing we can't do here in comparison is disabling queries be passing a filter. Normally we
    * don't need to do this since this is a factory class.
    *
    * @param person Our search object
    * @return The desired BooleanQuery
    */
  override def createQuery(person: Person): Query = {

    import CustomQuery.{data2Query, seq2Query}

    Seq(
      (LAST_NAME, person.lastName).exact,
      (LAST_NAME, createRegexTerm(person.lastName), Boost.REGEX).regex,
      (LAST_NAME, person.lastName, Boost.PHONETIC).phonetic,
      (LAST_NAME, person.lastName, Boost.FUZZY, FUZZY_MAX_EDITS).fuzzy
    )

  }

  /**
    * Custom method. We have a param queryEnableOpt here. So if we pass Some(QueryEnabled.REGEX), only the RegexQuery
    * will perform.
    *
    * Maybe deprecated in some later version.
    *
    * @param person Our search object
    * @param queryEnableOpt Selection of enabled types of Query.
    * @return Normally a BooleanQuery
    */
  override def createQuery(person: Person, queryEnableOpt: Option[Int] = Some(QueryEnabled.ALL)): Query = {

    val queryEnable = queryEnableOpt.get // cannot be empty

    import CustomQuery.data2Query

    val queryBuilder = new BooleanQuery.Builder()

    if (checkEnabled(queryEnable, QueryEnabled.EXACT)) {
      // OR customized: queryBuilder.add((LAST_NAME, person.lastName, 10000).exact, BooleanClause.Occur.SHOULD) -> Boost 10000
      queryBuilder.add((LAST_NAME, person.lastName).exact, BooleanClause.Occur.SHOULD)
    }

    if (checkEnabled(queryEnable, QueryEnabled.REGEX)) {
      queryBuilder.add((LAST_NAME, createRegexTerm(person.lastName), Boost.REGEX).regex, BooleanClause.Occur.SHOULD)
    }

    if (checkEnabled(queryEnable, QueryEnabled.PHONETIC)) {
      queryBuilder.add((LAST_NAME, person.lastName, Boost.PHONETIC).phonetic, BooleanClause.Occur.SHOULD)
    }

    if (checkEnabled(queryEnable, QueryEnabled.FUZZY)) {
      // OR without maxEdits: queryBuilder.add((LAST_NAME, person.lastName, Boost.FUZZY).fuzzy, BooleanClause.Occur.SHOULD)
      // In this case FUZZY_MAX_EDITS would be fetched as default from QueryConfig
      queryBuilder.add((LAST_NAME, person.lastName, Boost.FUZZY, FUZZY_MAX_EDITS).fuzzy, BooleanClause.Occur.SHOULD)
    }

    queryBuilder.build()

  }

  private[this] def checkEnabled(queryOr: Int, queryEnabled: Int): Boolean = (queryOr & queryEnabled) == queryEnabled

}
