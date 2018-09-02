package de.crazything.app

import de.crazything.app.NettyRunner.{jsonString2T, t2JsonString}
import de.crazything.search.entity.{SearchResult, SearchResultCollection}
import de.crazything.search.{AbstractTypeFactory, CommonSearcher, DirectoryContainer, MagicSettings}
import play.api.mvc.{Action, Results}

abstract class AbstractDataController extends MagicSettings with DirectoryContainer{

  protected val searchDirectoryName: String = DEFAULT_DIRECTORY_NAME

  // Might become even a Cassandra based factory later.
  protected def socialPersonFactory: AbstractTypeFactory[Int, SocialPerson]

  def test = Action {
    Results.Ok("It works! I got social data 4u.")
  }

  def findSocialForScored = Action {
    request => {
      val person: Person = jsonString2T[Person](request.body.asJson.get.toString())
      val socialPerson: SocialPerson = SocialPerson(-1, person.firstName, person.lastName)
      val searchResult: Seq[SearchResult[Int, SocialPerson]] =
        CommonSearcher.search(input = socialPerson, factory = socialPersonFactory, searcherOption = searchDirectoryName)

      val strSearchResult: String =
        t2JsonString[SearchResultCollection[Int, SocialPerson]](SearchResultCollection(searchResult))
      Results.Created(strSearchResult).as("application/json")
    }
  }

}
