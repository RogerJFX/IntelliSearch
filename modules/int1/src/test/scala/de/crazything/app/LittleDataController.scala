package de.crazything.app

import de.crazything.app.analyze.GermanLanguage
import de.crazything.app.entity.Person
import de.crazything.app.factory.PersonFactoryDE
import de.crazything.app.helpers.DataProvider
import de.crazything.app.itest.Network
import de.crazything.search.{AbstractTypeFactory, CommonIndexer}

object LittleDataController extends AbstractDataController with Network with GermanLanguage{

  override protected val personFactory: AbstractTypeFactory[Int, Person] = PersonFactoryDE

  CommonIndexer.index(DataProvider.readVerySimplePersonsResource(), personFactory)


}
