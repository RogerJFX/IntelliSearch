package de.crazything.app

import de.crazything.search.CustomQuery.{data2Query, seq2Query}
import de.crazything.search._
import de.crazything.search.entity.{PkDataSet, QueryCriteria}
import de.crazything.search.persistence.InMemoryData
import org.apache.lucene.document._
import org.apache.lucene.search._

class SocialPersonFactory extends AbstractTypeFactory[Int, SocialPerson] with QueryConfig
  with GermanLanguage with GermanRegexReplace with InMemoryData [Int, SocialPerson] {

  import SocialPersonFactory.{populateDocument => populateDocumentS,
    createQuery => createQueryS,
    selectQueryCreator => selectQueryCreatorS}

  override def createInstanceFromDocument(doc: Document): PkDataSet[Int] = {
    DataContainer.findById(doc.get(PersonFactoryDE.PK).toInt)
  }

  override def setDataPool(data: Seq[SocialPerson]): Unit = {
    DataContainer.setData(data)
  }

  override def populateDocument(document: Document, dataSet: SocialPerson): Unit = populateDocumentS(document, dataSet)

  override def createQuery(t: SocialPerson): Query = createQueryS(t)

  override def selectQueryCreator: (QueryCriteria, SocialPerson) => Query = selectQueryCreatorS
}


object SocialPersonFactory extends AbstractTypeFactory[Int, SocialPerson] with QueryConfig
  with GermanLanguage with GermanRegexReplace with InMemoryData [Int, SocialPerson] {

  // private val logger = LoggerFactory.getLogger(SocialPersonFactory.getClass)

  private[app] val PK = "id"

  private[app] val SALUTATION = "salutation"
  private[app] val FIRST_NAME = "firstName"
  private[app] val LAST_NAME = "lastName"
  private[app] val FACEBOOK_ID = "facebookId"
  private[app] val TWITTER_ID = "twitterId"

  override def createInstanceFromDocument(doc: Document): PkDataSet[Int] = {
    DataContainer.findById(doc.get(PK).toInt)
  }

  override def setDataPool(data: Seq[SocialPerson]): Unit = {
    DataContainer.setData(data)
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

  override val selectQueryCreator:(QueryCriteria, SocialPerson) => Query = (criteria, person) => createQuery(person)

}
