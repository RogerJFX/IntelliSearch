package de.crazything.app

import de.crazything.app.test.helpers.DataProvider
import de.crazything.search.{AbstractTypeFactory, CommonIndexer}

object MediumDataController extends AbstractDataController with Network with GermanLanguage {

  override protected val personFactory: AbstractTypeFactory[Int, Person] = new PersonFactoryDE()

  CommonIndexer.index(DataProvider.readVerySimplePersonsResourceBig(), personFactory, "bigData")
}
