package de.crazything.app

import java.util.concurrent.atomic.AtomicReference

import de.crazything.search._
import de.crazything.search.entity.{PkDataSet, QueryCriteria}
import org.apache.lucene.document._
import org.apache.lucene.search._
import org.slf4j.LoggerFactory
import de.crazything.search.CustomQuery.{data2Query, seq2Query}

object SocialPersonFactory extends AbstractTypeFactory[Int, SocialPerson] with QueryConfig with GermanLanguage with GermanRegexReplace{

  // private val logger = LoggerFactory.getLogger(SocialPersonFactory.getClass)

  private[app] val PK = "id"

  private[app] val SALUTATION = "salutation"
  private[app] val FIRST_NAME = "firstName"
  private[app] val LAST_NAME = "lastName"
  private[app] val FACEBOOK_ID = "facebookId"
  private[app] val TWITTER_ID = "twitterId"

  val customEnabledQuery_Name = "customEnabledQuery_lastName"
  val customQuery_FirstAndLastName = "customQuery_FirstAndLastName"

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

//  {
//    criteria.queryName match {
//      case `customEnabledQuery_Name` => createSuperCustomQuery(person, criteria.queryEnableOpt)
//      case `customQuery_FirstAndLastName` => createFirstAndLastNameQuery(person)
//      case _ =>
//        logger.warn("No matching query name found. Falling back to standard `createQuery`")
//        createQuery(person)
//    }
//
//  }

  object DataContainer {

    case class Data(data: Seq[SocialPerson]) {
      def findById(id: Int): Option[SocialPerson] = data.find(d => d.id == id)
    }

    private val dataRef: AtomicReference[Data] = new AtomicReference[Data]()

    def setData(data: Seq[SocialPerson]): Unit = {
      dataRef.set(Data(data))
    }

    def findById(id: Int): SocialPerson = {
      dataRef.get().findById(id).get
    }

  }

}
