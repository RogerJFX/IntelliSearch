package de.crazything.search.persistence

import de.crazything.search.entity.PkDataSet

trait IPersistence[P, T <: PkDataSet[P]]  {
  /**
    * Store the data that later is searched.
    *
    * @param data The data.
    */
  def setData(data: Seq[T]): Unit

  /**
    * Select * from Seq where id = ´id´ .
    *
    * @param id Id of data set.
    * @return Found data set.
    */
  def findById(id: P): PkDataSet[P]
}
