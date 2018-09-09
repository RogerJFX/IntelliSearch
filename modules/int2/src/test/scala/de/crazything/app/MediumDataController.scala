package de.crazything.app

import de.crazything.app.test.helpers.DataProvider
import de.crazything.search.entity.QueryCriteria
import de.crazything.search.{AbstractTypeFactory, CommonIndexer}

object MediumDataController extends AbstractDataController with GermanLanguage {

  override protected val socialPersonFactory: AbstractTypeFactory[Int, SocialPerson] = new SocialPersonFactory()

  override protected val searchDirectoryName = "bigData"

  override protected val queryCriteria: Option[QueryCriteria] =
    Some(QueryCriteria(SocialPersonFactory.customQuery_FirstAndLastName, None))

  CommonIndexer.index(DataProvider.readSocialPersonsResourceBig(), socialPersonFactory, searchDirectoryName)

}
