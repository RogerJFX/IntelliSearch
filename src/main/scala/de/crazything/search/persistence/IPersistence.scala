package de.crazything.search.persistence

import de.crazything.search.entity.PkDataSet

trait IPersistence[P, T <: PkDataSet[P]]  {
  /**
    * Store the data that later is searched. Even updates data.
    *
    * Just pass some data. It will be merged.
    *
    * @param data The data.
    * @return The possibly old data, that was updated. Useful for rollbacks.
    */
  protected def setData(data: Seq[T]): Seq[T]

  /**
    * Remove data from persistence context.
    *
    * @param data Data to remove
    */
  def deleteData(data: Seq[T]): Unit

  /**
    * Select * from Seq where id = ´id´ .
    *
    * @param id Id of data set.
    * @return Found data set.
    */
  def findById(id: P): Option[PkDataSet[P]]
}
