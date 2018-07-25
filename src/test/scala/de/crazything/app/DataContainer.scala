package de.crazything.app

import java.util.concurrent.atomic.AtomicReference

object DataContainer {

  case class Data(data: Seq[Person]) {

    def findById(id: Int): Option[Person] = data.find(d => d.id == id)

  }

  private val dataRef: AtomicReference[Data] = new AtomicReference[Data]()

  def setData(data: Seq[Person]): Unit = {
    dataRef.set(Data(data))
  }

  def findById(id: Int): Person = {
    dataRef.get().findById(id).get
  }

}
