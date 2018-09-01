package de.crazything.app

import de.crazything.app.test.helpers.DataProvider
import de.crazything.search.{AbstractTypeFactory, CommonIndexer}

object LittleDataController extends AbstractDataController with GermanLanguage {

  override protected def socialPersonFactory: AbstractTypeFactory[Int, SocialPerson] = SocialPersonFactory

  CommonIndexer.index(DataProvider.readSocialPersonsResource(), socialPersonFactory)

}
