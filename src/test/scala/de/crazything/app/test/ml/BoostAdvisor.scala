package de.crazything.app.test.ml

import de.crazything.app.test.ml.guard.{DefaultGuard, Guard, GuardConfig}
import de.crazything.app.test.ml.tuning.{Notification, Tuner}

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
