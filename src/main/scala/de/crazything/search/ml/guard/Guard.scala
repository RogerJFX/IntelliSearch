package de.crazything.search.ml.guard

trait Guard {

  protected case class UserProtocol(var lastSearch: Seq[String] = Seq(), var lastTs: Long = 0L, var numFouls: Int = 0)

  def search(ip: String, terms: Seq[String]): Unit

  def pass(ip: String, terms: Seq[String], position: Int, clickedAs: Int): Boolean

  def seqEquals(a: Seq[String], b: Seq[String]): Boolean = a.toSet == b.toSet

  def timestampOk(last: Long, allowedGap: Long): (Boolean, Long) = {
    val now = System.currentTimeMillis()
    (now - last > allowedGap, now)
  }

}
