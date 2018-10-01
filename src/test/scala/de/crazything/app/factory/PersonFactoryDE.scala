package de.crazything.app.factory

import de.crazything.app.entity.Person
import de.crazything.search._
import de.crazything.search.entity.{PkDataSet, QueryCriteria}
import de.crazything.search.persistence.InMemoryDAO
import org.apache.lucene.document._
import org.apache.lucene.search._
import org.slf4j.LoggerFactory

class PersonFactoryDE extends AbstractTypeFactory[Int, Person] with PersonFactoryDEQueries with InMemoryDAO[Int, Person]{

  import PersonFactoryDE.{createQuery => createQueryS, getPkFieldnameAsString => getPkFieldnameAsStringS, populateDocument => populateDocumentS, selectQueryCreator => selectQueryCreatorS}

  override def createInstanceFromDocument(doc: Document): Option[PkDataSet[Int]] = {
    findById(doc.get(PersonFactoryDE.PK).toInt)
  }

  override def putData(data: Seq[Person]): Seq[Person] = {
    setData(data)
  }

  override def populateDocument(document: Document, dataSet: Person): Unit = populateDocumentS(document, dataSet)

  override def createQuery(t: Person): Query = createQueryS(t)

  override def selectQueryCreator: (QueryCriteria, Person) => Query = selectQueryCreatorS

  override def getPkFieldnameAsString(): String = getPkFieldnameAsStringS()
}

object PersonFactoryDE extends AbstractTypeFactory[Int, Person] with PersonFactoryDEQueries with InMemoryDAO[Int, Person]{

  private val logger = LoggerFactory.getLogger(PersonFactoryDE.getClass)

  private[app] val PK = "id"

  private[app] val SALUTATION = "salutation"
  private[app] val FIRST_NAME = "firstName"
  private[factory] val LAST_NAME = "lastName"
  private[app] val STREET = "street"
  private[app] val CITY = "city"

  val customEnabledQuery_Name = "customEnabledQuery_lastName"
  val customQuery_FirstAndLastName = "customQuery_FirstAndLastName"
  val cascadedQuery_FirstAndLastName = "cascadedQuery_FirstAndLastName"

  override def createInstanceFromDocument(doc: Document): Option[PkDataSet[Int]] = {
    findById(doc.get(PK).toInt)
  }

  override def putData(data: Seq[Person]): Seq[Person] = {
    setData(data)
  }

  override def populateDocument(document: Document, person: Person): Unit = {

    addPkField(document, PK, person.id)

    // DON'T! Just for Tests.
    addStoredField(document, LAST_NAME, person.lastName)

    addField(document, SALUTATION, person.salutation)
    addField(document, FIRST_NAME, person.firstName)
    addField(document, LAST_NAME, person.lastName)
    addField(document, STREET, person.street)
    addField(document, CITY, person.city)

  }

  override def createQuery(person: Person): Query = doCreateStandardQuery(person)

  override val selectQueryCreator:(QueryCriteria, Person) => Query = (criteria, person) => {
    criteria.queryName match {
      case `customEnabledQuery_Name` => createSuperCustomQuery(person, criteria.queryEnableOpt)
      case `customQuery_FirstAndLastName` => createFirstAndLastNameQuery(person)
      case `cascadedQuery_FirstAndLastName` => doCreateCascadedStandardQuery(person)
      case _ =>
        logger.warn("No matching query name found. Falling back to standard `createQuery`")
        createQuery(person)
    }

  }

  override def getPkFieldnameAsString(): String = PK
}
