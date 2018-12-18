package de.crazything.search.ml.tuning

trait Tuner {

  def tune(terms: Seq[String], delta: Int): Unit

  protected val vector: Array[Float]

  def reset(): Unit

  def boostAt(index: Int): Float

  val threshold: Int

}
