package de.crazything.search.ml.tuning

case class Notification[T](terms: Seq[String], delta: Int, tuning: Array[T])
