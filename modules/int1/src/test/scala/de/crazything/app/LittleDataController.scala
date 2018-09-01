package de.crazything.app

import de.crazything.app.test.helpers.DataProvider
import de.crazything.search.{AbstractTypeFactory, CommonIndexer}

object LittleDataController extends AbstractDataController with Network with GermanLanguage{

  override protected val personFactory: AbstractTypeFactory[Int, Person] = PersonFactoryDE

  CommonIndexer.index(DataProvider.readVerySimplePersonsResource(), personFactory)


}
