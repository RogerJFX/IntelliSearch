package de.crazything.app

import de.crazything.app.test.helpers.DataProvider
import de.crazything.search.entity.{SearchResult, SearchResultCollection}
import de.crazything.search.{AbstractTypeFactory, CommonIndexer}
import de.crazything.service.RestClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object MediumDataController extends AbstractDataController with Network with GermanLanguage {

  override protected def combineFacebookScored(basePerson: SearchResult[Int, Person]): Future[Seq[SearchResult[Int, SocialPerson]]] = {
    val restResponse: Future[SearchResultCollection[Int, SocialPerson]] =
      RestClient.post[Person, SearchResultCollection[Int, SocialPerson]](urlFromUriSocial("findSocialForScoredBig"),
        basePerson.obj)
    restResponse.map(res => res.entries)
  }

  override protected val personFactory: AbstractTypeFactory[Int, Person] = new PersonFactoryDE()

  override protected val searchDirectoryName = "bigData"

  CommonIndexer.index(DataProvider.readVerySimplePersonsResourceBig(), personFactory, searchDirectoryName)
}
