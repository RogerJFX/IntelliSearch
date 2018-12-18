package de.crazything.search.ml.tuning

import java.util.concurrent.atomic.AtomicReference

import scala.util.Random

class DedicatedTuner(config: TunerConfig) extends Tuner {

  private val oldNotification = new AtomicReference[Notification[Float]](null)

  override val vector: Array[Float] = Array.fill[Float](config.querySize)(config.initialBoost)

  private val random = new Random()

  // boostStep, minBoost, maxBoost
  private val tuningProps: Array[(Float, Float, Float)] = {
    val result = Array.ofDim[(Float, Float, Float)](config.querySize)
    for(i <- result.indices) {
      val dedicated = config.dedicated.find(d => d.queryIndex == i)
      dedicated match {
        case Some(d) =>
          result(i) = (d.boostStep, d.minBoost, d.maxBoost)
          vector(i) = d.initialBoost
        case None =>
          result(i) = (config.boostStep, config.minBoost, config.maxBoost)
      }
    }
    result
  }

  override def tune(terms: Seq[String], delta: Int): Unit = {
    val old = oldNotification.getAndSet(Notification(terms, delta, vector.clone()))
    if (old != null && old.delta < delta) {
      for (i <- vector.indices) {
        vector(i) = old.tuning(i)
      }
    }
    val index = random.nextInt(config.querySize)
    val up = random.nextBoolean()
    if (up && vector(index) + tuningProps(index)._1 <= tuningProps(index)._3) {
      vector(index) += tuningProps(index)._1
    } else if (!up && vector(index) - tuningProps(index)._1 >= tuningProps(index)._2) {
      vector(index) -= tuningProps(index)._1
    }
  }

  override def boostAt(index: Int): Float = vector(index)

  override val threshold: Int = config.threshold

  override def reset(): Unit = {
    oldNotification.set(null)
    for (i <- vector.indices) {
      val dedicated = config.dedicated.find(d => d.queryIndex == i)
      dedicated match {
        case Some(d) =>
          vector(i) = d.initialBoost
        case None =>
          vector(i) = config.initialBoost
      }
    }
  }
}
