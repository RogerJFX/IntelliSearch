package de.crazything.app

import de.crazything.search.entity.PkDataSet

case class Person(id: Int, salutation: String, firstName: String, lastName: String, street: String, city: String)
  extends PkDataSet[Int](id)
