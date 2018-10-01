package de.crazything.app

import de.crazything.app.analyze.GermanLanguage
import de.crazything.app.entity.SocialPerson
import de.crazything.app.factory.SocialPersonFactory
import de.crazything.app.helpers.DataProvider
import de.crazything.search.{AbstractTypeFactory, CommonIndexer}

object LittleDataController extends AbstractDataController with GermanLanguage {

  override protected def socialPersonFactory: AbstractTypeFactory[Int, SocialPerson] = SocialPersonFactory

  CommonIndexer.index(DataProvider.readSocialPersonsResource(), socialPersonFactory)

}
