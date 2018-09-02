package de.crazything.app

import de.crazything.app.test.helpers.DataProvider
import de.crazything.search.{AbstractTypeFactory, CommonIndexer}
import play.api.mvc.{Action, Results}

object MediumDataController extends AbstractDataController with GermanLanguage {

  override protected val socialPersonFactory: AbstractTypeFactory[Int, SocialPerson] = new SocialPersonFactory()

  override protected val searchDirectoryName = "bigData"

  CommonIndexer.index(DataProvider.readSocialPersonsResourceBig(), socialPersonFactory, searchDirectoryName)

  def testBig = Action {
    Results.Ok("It works! I got social data 4u. :: " + socialPersonFactory.getDataPoolSize())
  }
}
