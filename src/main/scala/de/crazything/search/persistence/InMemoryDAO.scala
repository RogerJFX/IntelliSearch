package de.crazything.search.persistence

import java.util.concurrent.atomic.AtomicReference

import de.crazything.search.entity.PkDataSet

/**
  * If we do not want to store data in lucene index, but only the id, we have to store data somewhere else.
  * Here we store it in memory. This is sufficient for smaller data. If data becomes huge, another strategy should
  * be preferred. Like some database or even a big data solution. Even another Lucene index (filesystem then) seems
  * reasonable.
  *
  * Note: although data is not stored in lucene, it is still indexed.
  *
  * @tparam P Type of Primary Key.
  * @tparam T Type of entity to store.
  */
trait InMemoryDAO[P, T <: PkDataSet[P]] extends IPersistence[P, T] {

  private case class Data(data: Seq[T]) {
    private[InMemoryDAO] def findById(id: P): Option[T] = data.find(d => d.getId == id)
  }

  // Empty. Never be null, dude.
  private[this] val dataRef: AtomicReference[Data] = new AtomicReference[Data](Data(Seq()))

  // insert or update. OR: init!
  override def setData(data: Seq[T]): Seq[T] = {
    val removedData = extractRemovedData(data)
    dataRef.set(Data(extractRemainingData(data) ++ data))
    removedData
  }

  // delete
  override def deleteData(data: Seq[T]): Unit = {
    dataRef.set(Data(extractRemainingData(data)))
  }

  // select
  override def findById(id: P): Option[T] = {
    dataRef.get().findById(id)
  }

  private[this] def extractRemainingData(incomingData: Seq[T]): Seq[T] = {
    val oldData: Seq[T] = dataRef.get().data
    val ids: Seq[P] = incomingData.map(dd => dd.getId)
    oldData.filter(od => !ids.contains(od.getId))
  }

  private[this] def extractRemovedData(incomingData: Seq[T]): Seq[T] = {
    val oldData: Seq[T] = dataRef.get().data
    val ids: Seq[P] = incomingData.map(dd => dd.getId)
    oldData.filter(od => ids.contains(od.getId))
  }

}
