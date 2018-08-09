package de.crazything.search

import de.crazything.search.entity.{PkDataSet, SearchResult}

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object CommonSearcherCombineHandlers {

  class CombineHandler[I1, I2, T1 <: PkDataSet[I1], T2 <: PkDataSet[I2]](filterFn: (SearchResult[I1, T1]) => Future[Seq[SearchResult[I2, T2]]],
                                            sr: SearchResult[I1, T1],
                                            buffer: ListBuffer[(SearchResult[I1, T1], Seq[SearchResult[I2, T2]])],
                                            callback: () => Unit,
                                            onFilterException: (Throwable) => Unit)(ec: ExecutionContext) extends Runnable {
    implicit val exc: ExecutionContext = ec

    override def run(): Unit = {
      filterFn(sr).onComplete {
        case Success(remoteResult) =>
          buffer.append((sr, remoteResult))
          callback()
        case Failure(t) =>
          onFilterException(t)
      }
    }
  }

}
