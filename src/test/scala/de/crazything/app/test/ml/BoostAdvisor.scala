package de.crazything.app.test.ml

import java.util.concurrent.atomic.AtomicReference


import scala.util.Random

class BoostAdvisor(querySize: Int) {

  private case class Notification(delta: Int, tuning: Array[Float])

  private val boostStep = 0.1F

  private val oldNotification = new AtomicReference[Notification](null)

  private val vector = Array.fill[Float](querySize)(10F)

  private val actIfDeltaLargerThan = 10

  private val random = new Random()

  // Very simple yet and not sufficient
  private def tune(notification: Notification): Unit = {
    val old = oldNotification.getAndSet(notification)
    if (old != null && old.delta < notification.delta) {
      for (i <- vector.indices) {
        vector(i) = old.tuning(i)
      }
    }
    if (random.nextBoolean()) {
      vector(random.nextInt(querySize)) += boostStep
    } else {
      vector(random.nextInt(querySize)) -= boostStep
    }
  }

//  // Analyze deeper? Sure we have to, if we do not want to get victims of attacks
//  import scala.collection.mutable.ListBuffer
//
//  private val buffer = ListBuffer[Notification]()
//
//  private def tuneFuture(): Unit = {
//    val lastBest: Int = buffer.lastIndexWhere(a => a.delta == buffer.minBy(e => e.delta).delta)
//    buffer.remove(lastBest, buffer.length - lastBest)
//    if (buffer.length > 1) {
//      val rand = random.nextInt(querySize)
//      vector(rand) += boostStep
//    }
//  }

  private[ml] def boost(index: Int): Float = vector(index)

  def notifyFeedback(position: Int, clickedAs: Int, actDelta: Int = actIfDeltaLargerThan): Boolean = {
    val delta = Math.abs(position - clickedAs)
    if (delta < actDelta) {
      false
    } else {
      tune(Notification(delta, vector.clone()))
      true
    }
  }
}
