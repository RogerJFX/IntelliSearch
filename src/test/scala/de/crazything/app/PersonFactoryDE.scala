package de.crazything.app

import de.crazything.search._
import de.crazything.search.entity.{PkDataSet, QueryCriteria}
import de.crazything.search.persistence.InMemoryData
import org.apache.lucene.document._
import org.apache.lucene.search._
import org.slf4j.LoggerFactory

class PersonFactoryDE extends AbstractTypeFactory[Int, Person] with PersonQueries with InMemoryData[Int, Person]{

  import PersonFactoryDE.{populateDocument => populateDocumentS,
    createQuery => createQueryS,
    selectQueryCreator => selectQueryCreatorS}

  override def createInstanceFromDocument(doc: Document): PkDataSet[Int] = {
    dataContainer.findById(doc.get(PersonFactoryDE.PK).toInt)
  }

  override def setDataPool(data: Seq[Person]): Unit = {
    dataContainer.setData(data)
  }

  override def populateDocument(document: Document, dataSet: Person): Unit = populateDocumentS(document, dataSet)

  override def createQuery(t: Person): Query = createQueryS(t)

  override def selectQueryCreator: (QueryCriteria, Person) => Query = selectQueryCreatorS

}

object PersonFactoryDE extends AbstractTypeFactory[Int, Person] with PersonQueries with InMemoryData[Int, Person]{

  private val logger = LoggerFactory.getLogger(PersonFactoryDE.getClass)

  private[app] val PK = "id"

  private[app] val SALUTATION = "salutation"
  private[app] val FIRST_NAME = "firstName"
  private[app] val LAST_NAME = "lastName"
  private[app] val STREET = "street"
  private[app] val CITY = "city"

  val customEnabledQuery_Name = "customEnabledQuery_lastName"
  val customQuery_FirstAndLastName = "customQuery_FirstAndLastName"

  override def createInstanceFromDocument(doc: Document): PkDataSet[Int] = {
    dataContainer.findById(doc.get(PK).toInt)
  }

  override def setDataPool(data: Seq[Person]): Unit = {
    dataContainer.setData(data)
  }

  override def populateDocument(document: Document, person: Person): Unit = {

    addPkField(document, PK, person.id)

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
      case _ =>
        logger.warn("No matching query name found. Falling back to standard `createQuery`")
        createQuery(person)
    }

  }

}
