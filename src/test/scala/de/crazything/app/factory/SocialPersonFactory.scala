package de.crazything.app.factory

import de.crazything.app.analyze.{GermanLanguage, GermanRegexReplace}
import de.crazything.app.entity.SocialPerson
import de.crazything.search.CustomQuery.{data2Query, seq2Query}
import de.crazything.search._
import de.crazything.search.entity.QueryCriteria
import de.crazything.search.persistence.InMemoryDAO
import org.apache.lucene.document._
import org.apache.lucene.search._
import org.slf4j.LoggerFactory

class SocialPersonFactory extends AbstractTypeFactory[Int, SocialPerson] with QueryConfig
  with GermanLanguage with GermanRegexReplace with InMemoryDAO [Int, SocialPerson] {

  import SocialPersonFactory.{createQuery => createQueryS, getPkFieldnameAsString => getPkFieldnameAsStringS, populateDocument => populateDocumentS, selectQueryCreator => selectQueryCreatorS}

  override def createInstanceFromDocument(doc: Document): Option[SocialPerson] = {
    findById(doc.get(SocialPersonFactory.PK).toInt)
  }

  override def putData(data: Seq[SocialPerson]): Seq[SocialPerson] = {
    setData(data)
  }

  override def populateDocument(document: Document, dataSet: SocialPerson): Unit = populateDocumentS(document, dataSet)

  override def createQuery(t: SocialPerson): Query = createQueryS(t)

  override def selectQueryCreator: (QueryCriteria, SocialPerson) => Query = selectQueryCreatorS

  override def getPkFieldnameAsString(): String = getPkFieldnameAsStringS()
}


object SocialPersonFactory extends AbstractTypeFactory[Int, SocialPerson] with QueryConfig
  with GermanLanguage with GermanRegexReplace with InMemoryDAO [Int, SocialPerson] {

  private val logger = LoggerFactory.getLogger(SocialPersonFactory.getClass)

  private[app] val PK = "id"

  private[app] val SALUTATION = "salutation"
  private[app] val FIRST_NAME = "firstName"
  private[app] val LAST_NAME = "lastName"
  private[app] val FACEBOOK_ID = "facebookId"
  private[app] val TWITTER_ID = "twitterId"

  val customQuery_FirstAndLastName = "customQuery_FirstAndLastName"

  override def createInstanceFromDocument(doc: Document): Option[SocialPerson] = {
    findById(doc.get(PK).toInt)
  }

  override def putData(data: Seq[SocialPerson]): Seq[SocialPerson] = {
    setData(data)
  }

  override def populateDocument(document: Document, person: SocialPerson): Unit = {

    addPkField(document, PK, person.id)

    addField(document, FIRST_NAME, person.firstName)
    addField(document, LAST_NAME, person.lastName)
    if(person.facebookId.isDefined)
      addField(document, FACEBOOK_ID, person.facebookId.get)
    if(person.twitterId.isDefined)
      addField(document, TWITTER_ID, person.twitterId.get)

  }

  override def createQuery(person: SocialPerson): Query = Seq(
    (LAST_NAME, person.lastName).exact,
    (LAST_NAME, createRegexTerm(person.lastName), Boost.REGEX).regex,
    (LAST_NAME, person.lastName, Boost.PHONETIC).phonetic,
    (LAST_NAME, person.lastName, Boost.FUZZY, FUZZY_MAX_EDITS).fuzzy
  )

  override val selectQueryCreator:(QueryCriteria, SocialPerson) => Query = (criteria, person) => {
    criteria.queryName match {
      case `customQuery_FirstAndLastName` =>
        Seq(
          (LAST_NAME, createRegexTerm(person.lastName), Boost.EXACT*2).regex,
          (FIRST_NAME, createRegexTerm(person.firstName), Boost.EXACT*2).regex,

          (LAST_NAME, person.lastName, Boost.PHONETIC / 2F).phonetic,
          (FIRST_NAME, person.firstName, Boost.PHONETIC / 2F).phonetic
        )

      case _ =>
        logger.warn("No matching query name found. Falling back to standard `createQuery`")
        createQuery(person)
    }

  }

  override def getPkFieldnameAsString(): String = PK
}
