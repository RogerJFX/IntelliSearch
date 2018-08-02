package de.crazything.search

import de.crazything.search.entity.{PkDataSet, SearchResult}

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

object CommonSearcherFilterHandlers {

  class TaskHandler[I, T <: PkDataSet[I]](filterFn: (SearchResult[I, T]) => Boolean,
                                                  sr: SearchResult[I, T],
                                                  buffer: ListBuffer[SearchResult[I, T]],
                                                  callback: () => Unit) extends Runnable {
    override def run(): Unit = {
      val success = filterFn(sr)
      if (success) {
        buffer.append(sr)
      }
      callback()
    }
  }

  class FutureHandler[I, T <: PkDataSet[I]](filterFn: (SearchResult[I, T]) => Future[Boolean],
                                            sr: SearchResult[I, T],
                                            buffer: ListBuffer[SearchResult[I, T]],
                                            callback: () => Unit)(ec: ExecutionContext) extends Runnable {
    implicit val exc: ExecutionContext = ec
    override def run(): Unit = {
      filterFn(sr).onComplete {
        case Success(bool) =>
          if (bool) {
            buffer.append(sr)
          }
          callback()
        case _ =>
          callback()
      }
    }
  }
}
