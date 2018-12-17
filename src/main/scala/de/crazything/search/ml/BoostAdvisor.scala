package de.crazything.search.ml

import de.crazything.search.ml.guard.{DefaultGuard, Guard, GuardConfig}
import de.crazything.search.ml.tuning.{Notification, Tuner}

class BoostAdvisor(tuner: Tuner, guard: Guard = new DefaultGuard(GuardConfig())) {

  def boost(index: Int): Float = tuner.boostAt(index)

  def notifyFeedback(ip: String, position: Int, clickedAs: Int, threshold: Int = tuner.threshold): Unit = {
    if (guard.pass(ip, Seq(), position, clickedAs)) {
      val delta = Math.abs(position - clickedAs)
      if (delta > threshold) {
        tuner.tune(Notification(Seq(), delta, tuner.vector.clone()))
      }
    }
  }
}
