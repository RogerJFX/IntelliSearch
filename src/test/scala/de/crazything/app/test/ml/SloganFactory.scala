package de.crazything.app.test.ml

import de.crazything.app.analyze.NoLanguage
import de.crazything.search.AbstractTypeFactory
import de.crazything.search.CustomQuery._
import de.crazything.search.entity.{PkDataSet, QueryCriteria}
import de.crazything.search.ml.BoostAdvisor
import de.crazything.search.ml.guard.{DefaultGuard, GuardConfig}
import de.crazything.search.ml.tuning.{SimpleTuner, TunerConfig}
import de.crazything.search.persistence.InMemoryDAO
import org.apache.lucene.document.Document
import org.apache.lucene.search.Query

object SloganFactory extends AbstractTypeFactory[Int, Slogan] with NoLanguage with InMemoryDAO [Int, Slogan] {

  private[app] val PK = "id"

  private[app] val FIRST_NAME = "firstName"
  private[app] val LAST_NAME = "lastName"
  private[app] val SLOGAN_1 = "slogan1"
  private[app] val SLOGAN_2 = "slogan2"
  private[app] val SLOGAN_3 = "slogan3"

  override def createInstanceFromDocument(doc: Document): Option[PkDataSet[Int]] = findById(doc.get(PK).toInt)

  override def putData(data: Seq[Slogan]): Seq[Slogan] = setData(data)

  override def populateDocument(document: Document, dataSet: Slogan): Unit = {
    addPkField(document, PK, dataSet.id)
    addField(document, FIRST_NAME, dataSet.firstName)
    addField(document, LAST_NAME, dataSet.lastName)
    addField(document, SLOGAN_1, dataSet.slogan1)
    addField(document, SLOGAN_2, dataSet.slogan2)
    addField(document, SLOGAN_3, dataSet.slogan3)
  }

  private val tuner = new SimpleTuner(TunerConfig(9))

  private val ba = new BoostAdvisor(tuner, new DefaultGuard(GuardConfig()))

  def notifyFeedback(ip: String, position: Int, clickedAs: Int): Unit = ba.notifyFeedback(ip, position, clickedAs)

  def resetTuning(): Unit = tuner.reset()

  override def createQuery(t: Slogan): Query = {
    Seq(
      (SLOGAN_1, t.slogan1, ba.boost(0)).wildcard.should,
      (SLOGAN_1, t.slogan2, ba.boost(1)).wildcard.should,
      (SLOGAN_1, t.slogan3, ba.boost(2)).wildcard.should,

      (SLOGAN_2, t.slogan1, ba.boost(3)).wildcard.should,
      (SLOGAN_2, t.slogan2, ba.boost(4)).wildcard.should,
      (SLOGAN_2, t.slogan3, ba.boost(5)).wildcard.should,

      (SLOGAN_3, t.slogan1, ba.boost(6)).wildcard.should,
      (SLOGAN_3, t.slogan2, ba.boost(7)).wildcard.should,
      (SLOGAN_3, t.slogan3, ba.boost(8)).wildcard.should
    )
  }

  override def selectQueryCreator: (QueryCriteria, Slogan) => Query = (_, slogan) => createQuery(slogan)

  override def getPkFieldnameAsString: String = PK
}
