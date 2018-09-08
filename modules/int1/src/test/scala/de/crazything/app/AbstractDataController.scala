package de.crazything.app

import de.crazything.app.MediumDataController.urlFromUriSocial
import de.crazything.app.NettyRunner.{jsonString2T, t2JsonString}
import de.crazything.app.Person._
import de.crazything.search.entity._
import de.crazything.search.ext.MappingSearcher
import de.crazything.search.{AbstractTypeFactory, CommonSearcher, DirectoryContainer, MagicSettings}
import de.crazything.service.RestClient
import play.api.mvc.{Action, AnyContent, Request, Results}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

abstract class AbstractDataController extends MagicSettings with DirectoryContainer{

  protected def personFactory: AbstractTypeFactory[Int, Person]

  protected val searchDirectoryName: String = DEFAULT_DIRECTORY_NAME

  protected val queryCriteria: Option[QueryCriteria] = None

  protected def combineFacebookScored(basePerson: SearchResult[Int, Person]): Future[Seq[SearchResult[Int, SocialPerson]]] = {
    val restResponse: Future[SearchResultCollection[Int, SocialPerson]] =
      RestClient.post[Person, SearchResultCollection[Int, SocialPerson]](urlFromUriSocial("findSocialForScored"),
        basePerson.obj)
    restResponse.map(res => res.entries)
  }

  def test = Action {
    Results.Ok("It works! I got base data 4u.")
  }

  def findBaseData = Action {
    request: Request[AnyContent] => {
      val person: Person = jsonString2T[Person](request.body.asJson.get.toString())
      val searchResult: Seq[SearchResult[Int, Person]] =
        CommonSearcher.search(input = person, factory = personFactory, queryCriteria = queryCriteria,
          searcherOption = searchDirectoryName)
      val strSearchResult: String =
        t2JsonString[SearchResultCollection[Int, Person]](SearchResultCollection(searchResult))
      Results.Created(strSearchResult).as("application/json")
    }
  }

  def findBaseDataForWithSocial: Action[AnyContent] = Action.async {
    request => {
      val person: Person = jsonString2T[Person](request.body.asJson.get.toString())
      MappingSearcher.search(input = person, factory = personFactory, queryCriteria = queryCriteria,
        searcherOption = searchDirectoryName,
        mapperFn = combineFacebookScored, secondLevelTimeout = 5.minutes)
        .map((searchResult: Seq[MappedResults[Int, Int, Person, SocialPerson]]) => {
          val sequence: Seq[PersonWithSocialResults] =
            searchResult.map(sr => PersonWithSocialResults(sr.target, sr.results))
          val strSearchResult: String =
            t2JsonString[PersonWithSocialPersonsCollection](PersonWithSocialPersonsCollection(sequence))
          Results.Created(strSearchResult).as("application/json")
        })
    }
  }

  def mapSocial2Base: Action[AnyContent] = Action.async {
    request => {
      val person: Person = jsonString2T[Person](request.body.asJson.get.toString())
      MappingSearcher.search(input = person, factory = personFactory, queryCriteria = queryCriteria,
        searcherOption = searchDirectoryName,
        mapperFn = combineFacebookScored, secondLevelTimeout = 5.minutes, maxHits = 20).map(searchResult => {

        val strSearchResult: String =
          t2JsonString[MappedResultsCollection[Int, Int, Person, SocialPerson]](MappedResultsCollection(searchResult))
        Results.Created(strSearchResult).as("application/json")
      })
    }
  }
}
