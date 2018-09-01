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
  object DataContainer {

    private case class Data(data: Seq[T]) {
      private[DataContainer] def findById(id: P): Option[T] = data.find(d => d.getId == id)
    }

    private[this] val dataRef: AtomicReference[Data] = new AtomicReference[Data]()

    /**
      * Store the data that later is searched.
      *
      * @param data The data.
      */
    def setData(data: Seq[T]): Unit = {
      dataRef.set(Data(data))
    }

    /**
      * Select * from Seq where id = ´id´ .
      *
      * Note: this method should become slow when data becomes bigger. Read the comment above.
      *
      * @param id Id of data set.
      * @return Found data set.
      */
    def findById(id: P): PkDataSet[P] = {
      dataRef.get().findById(id).get
    }

  }

}
