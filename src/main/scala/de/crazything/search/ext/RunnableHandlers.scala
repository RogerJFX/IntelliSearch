package de.crazything.search.ext

import de.crazything.search.entity.{MappedResults, PkDataSet, SearchResult}

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
  * Collection of runnable handlers. We had to outsource it here because we need to pass
  * the execution context for Future handlers explicitly.
  */
object RunnableHandlers {

  class FilterHandler[I, +T <: PkDataSet[I]](filterFn: (SearchResult[I, T]) => Boolean,
                                             sr: SearchResult[I, T],
                                             buffer: ListBuffer[SearchResult[I, T]],
                                             callback: () => Unit,
                                             onFilterException: (Exception) => Unit) extends Runnable {
    override def run(): Unit = {
      try {
        val success = filterFn(sr)
        if (success) {
          buffer.append(sr)
        }
        callback()
      } catch {
        case exc: Exception => onFilterException(exc)
      }
    }
  }

  class FilterFutureHandler[I, +T <: PkDataSet[I]]
  (filterFuture: (SearchResult[I, T]) => Future[Boolean],
   sr: SearchResult[I, T],
   buffer: ListBuffer[SearchResult[I, T]],
   callback: () => Unit,
   onFilterException: (Throwable) => Unit)(ec: ExecutionContext) extends Runnable {

    implicit val exc: ExecutionContext = ec

    override def run(): Unit = {
      filterFuture(sr).onComplete {
        case Success(bool) =>
          if (bool) {
            buffer.append(sr)
          }
          callback()
        case Failure(t) =>
          onFilterException(t)
      }
    }
  }

  class MapperFutureHandler[I1, I2, +T1 <: PkDataSet[I1], +T2 <: PkDataSet[I2]]
  (mapperFuture: (SearchResult[I1, T1]) => Future[Seq[SearchResult[I2, T2]]],
   sr: SearchResult[I1, T1],
   buffer: ListBuffer[MappedResults[I1, I2, T1, T2]],
   callback: () => Unit,
   onFilterException: (Throwable) => Unit)(ec: ExecutionContext) extends Runnable {

    implicit val exc: ExecutionContext = ec

    override def run(): Unit = {
      mapperFuture(sr).onComplete {
        case Success(remoteResult) =>
          buffer.append(MappedResults(sr, remoteResult))
          callback()
        case Failure(t) =>
          onFilterException(t)
      }
    }
  }

}
