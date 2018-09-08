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
trait InMemoryData[P, T <: PkDataSet[P]] {

  /**
    * Just some namespace.
    */
  protected class DataContainer extends IPersistence[P, T]{

    private case class Data(data: Seq[T]) {
      private[DataContainer] def findById(id: P): T = data.find(d => d.getId == id)
        .getOrElse(throw new RuntimeException(s"Something completely impossible happened here. " +
          s"Corrupted directory? Missing id was $id"))
    }

    private[this] val dataRef: AtomicReference[Data] = new AtomicReference[Data]()

    override def setData(data: Seq[T]): Unit = {
      dataRef.set(Data(data))
    }

    override def findById(id: P): PkDataSet[P] = {
      dataRef.get().findById(id)
    }

  }

  protected lazy val dataContainer = new DataContainer

}
