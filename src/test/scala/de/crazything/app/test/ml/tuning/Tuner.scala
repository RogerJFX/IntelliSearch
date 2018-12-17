package de.crazything.app.test.ml.tuning

trait Tuner {

  def tune(notification: Notification): Unit

  def reset(): Unit

  def boostAt(index: Int): Float

  val vector: Array[Float]

  val threshold: Int

}
