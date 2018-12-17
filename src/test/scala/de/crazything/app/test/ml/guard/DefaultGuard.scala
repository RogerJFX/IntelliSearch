package de.crazything.app.test.ml.guard

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class DefaultGuard(config: GuardConfig) extends Guard {
  
  private val maxClickedAs = config.maxClickedAs
  
  private val maxPosition = config.maxPosition

  private val minTimeGap = config.minTimeGap

  private val maxTimeFouls = config.maxTimeFouls

  private val blacklisted = ListBuffer[String]()

  val map = new mutable.HashMap[String, UserProtocol]()

  override def search(ip: String, terms: Seq[String]): Unit = {
    val userProtocol = map.getOrElseUpdate(ip, UserProtocol())
    userProtocol.lastSearch = terms
  }

  override def pass(ip: String, terms: Seq[String], position: Int, clickedAs: Int): Boolean = {
    val userProtocol = map.getOrElseUpdate(ip, UserProtocol())
    val timeOk = timestampOk(userProtocol.lastTs, minTimeGap)
    userProtocol.lastTs = timeOk._2
    if(!timeOk._1) {
      userProtocol.numFouls += 1
      if(userProtocol.numFouls > maxTimeFouls) {
        blacklisted += ip
      }
    }
    if(!timeOk._1 || position > maxPosition || clickedAs > maxClickedAs || blacklisted.contains(ip)) {
      false
    } else {
      true
    }
  }
}
