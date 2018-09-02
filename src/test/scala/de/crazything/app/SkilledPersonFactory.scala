package de.crazything.app

import de.crazything.search.CustomQuery._
import de.crazything.search.entity.{PkDataSet, QueryCriteria}
import de.crazything.search.persistence.InMemoryData
import de.crazything.search.{AbstractTypeFactory, QueryConfig}
import org.apache.lucene.document.Document
import org.apache.lucene.search.Query

import scala.collection.mutable.ListBuffer

class SkilledPersonFactory extends AbstractTypeFactory[Int, SkilledPerson] with QueryConfig
  with NoLanguage with InMemoryData [Int, SkilledPerson] {

  import SkilledPersonFactory.{
    populateDocument => populateDocumentS,
    createQuery => createQueryS,
    selectQueryCreator => selectQueryCreatorS}

  override def createInstanceFromDocument(doc: Document): PkDataSet[Int] = dataContainer.findById(doc.get(SkilledPersonFactory.PK).toInt)

  override def setDataPool(data: Seq[SkilledPerson]): Unit = dataContainer.setData(data)

  override def populateDocument(document: Document, dataSet: SkilledPerson): Unit = populateDocumentS(document, dataSet)

  override def createQuery(t: SkilledPerson): Query = createQueryS(t)

  override def selectQueryCreator: (QueryCriteria, SkilledPerson) => Query = selectQueryCreatorS

}

object SkilledPersonFactory extends AbstractTypeFactory[Int, SkilledPerson] with QueryConfig
  with NoLanguage with InMemoryData [Int, SkilledPerson] {

  private[app] val PK = "id"

  private[app] val FIRST_NAME = "firstName"
  private[app] val LAST_NAME = "lastName"

  private[app] val CSV_SKILLS = "skills"


  override def createInstanceFromDocument(doc: Document): PkDataSet[Int] = {
    dataContainer.findById(doc.get(PK).toInt)
  }

  override def setDataPool(data: Seq[SkilledPerson]): Unit = dataContainer.setData(data)

  override def populateDocument(document: Document, person: SkilledPerson): Unit = {
    addPkField(document, PK, person.id)
    addField(document, FIRST_NAME, person.firstName.getOrElse(""))
    addField(document, LAST_NAME, person.lastName.getOrElse(""))
    person.skills match {
      case Some(skillSeq) => addTextField(document,CSV_SKILLS, skillSeq.mkString(";"))
      case _ => //addField (document, CSV_SKILLS, "")
    }
  }

  override def createQuery(person: SkilledPerson): Query = {
    val buffer = ListBuffer[Query]()
    person.skills match {
      case Some(realSkills) =>
        realSkills.foreach(skill =>
          buffer.append(
            (CSV_SKILLS, skill).exact,
            (CSV_SKILLS, skill).fuzzy
          )
        )
      case _ =>
    }
    buffer
  }

  override def selectQueryCreator: (QueryCriteria, SkilledPerson) => Query = (_, person) => createQuery(person)

}
