package de.crazything.search.ml.guard

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class DefaultGuard(config: GuardConfig) extends Guard {

  private val blacklistedIps = ListBuffer[String]()

  private val map = new mutable.HashMap[String, UserProtocol]()

//  override def search(ip: String, terms: Seq[String]): Unit = {
//    val userProtocol = map.getOrElseUpdate(ip, UserProtocol())
//    userProtocol.lastSearch = terms
//  }

  // TODO: Bullshit method! Must become better. Just make up your dirty mind, sexy MF!
  override def pass(ip: String, terms: Seq[String], position: Int, clickedAs: Int): Boolean = {
    if (blacklistedIps.contains(ip) || position > config.maxPosition || clickedAs > config.maxClickedAs) {
      false
    } else {
      val userProtocol = map.getOrElseUpdate(ip, UserProtocol())
      val timeOk = timestampOk(userProtocol.lastTs, config.minTimeGap)
      userProtocol.lastTs = timeOk._2
      if (!timeOk._1) {
        userProtocol.numFouls += 1
        if (userProtocol.numFouls > config.maxTimeFouls) {
          blacklistedIps += ip
        }
      }
      timeOk._1
    }
  }
}
