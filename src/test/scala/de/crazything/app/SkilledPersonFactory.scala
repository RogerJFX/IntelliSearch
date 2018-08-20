package de.crazything.app

import java.util.concurrent.atomic.AtomicReference

import de.crazything.search.CustomQuery._
import de.crazything.search.entity.{PkDataSet, QueryCriteria}
import de.crazything.search.{AbstractTypeFactory, QueryConfig}
import org.apache.lucene.document.Document
import org.apache.lucene.search.Query

import scala.collection.mutable.ListBuffer

object SkilledPersonFactory extends AbstractTypeFactory[Int, SkilledPerson] with QueryConfig with NoLanguage{

  private[app] val PK = "id"

  private[app] val FIRST_NAME = "firstName"
  private[app] val LAST_NAME = "lastName"

  private[app] val CSV_SKILLS = "skills"


  override def createInstanceFromDocument(doc: Document): PkDataSet[Int] = {
    DataContainer.findById(doc.get(PK).toInt)
  }

  override def setDataPool(data: Seq[SkilledPerson]): Unit = DataContainer.setData(data)

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

  object DataContainer {

    case class Data(data: Seq[SkilledPerson]) {
      def findById(id: Int): Option[SkilledPerson] = data.find(d => d.id == id)
    }

    private val dataRef: AtomicReference[Data] = new AtomicReference[Data]()

    def setData(data: Seq[SkilledPerson]): Unit = {
      dataRef.set(Data(data))
    }

    def findById(id: Int): SkilledPerson = {
      dataRef.get().findById(id).get
    }

  }
}
