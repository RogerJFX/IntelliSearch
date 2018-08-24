package de.crazything.search.utils

import java.util.concurrent.TimeoutException
import java.util.{Timer, TimerTask}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future, Promise}

/*
  Copied from stackoverflow.com. Nothing to change. Very nice and somehow old school. Like that.
 */
object FutureUtil {

  val timer: Timer = new Timer(true)

  def futureWithTimeout[T](future: Future[T], timeout: FiniteDuration)(implicit ec: ExecutionContext): Future[T] = {

    val p = Promise[T]

    val timerTask = new TimerTask() {
      def run() : Unit = {
        p.tryFailure(new TimeoutException())
      }
    }

    timer.schedule(timerTask, timeout.toMillis)

    future.map {
      a =>
        if(p.trySuccess(a)) {
          timerTask.cancel()
        }
    }.recover {
        case e: Exception =>
          if(p.tryFailure(e)) {
            timerTask.cancel()
          }
      }

    p.future
  }

}
