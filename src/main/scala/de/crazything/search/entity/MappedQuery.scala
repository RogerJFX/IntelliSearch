package de.crazything.search.entity

// TODO: add some xPath like syntax and parse it to this class.
case class MappedQuery(clazzName: String, attributeRxs: Seq[(String, String)] = Seq(), minScore: Option[Float] = None, maxScore: Option[Float] = None) {

  def filter[I, T <: PkDataSet[I]] (searchResults: Seq[SearchResult[I, T]]) : Seq[SearchResult[I, T]] = {
    searchResults.filter(sr => {
      checkClassname(sr) && checkScores(sr) && checkAttributes(sr)
    })
  }

  private def checkClassname[I, T <: PkDataSet[I]](sr: SearchResult[I, T]): Boolean = {
    sr.found.getClass.getSimpleName == clazzName
  }

  private def checkAttributes[I, T <: PkDataSet[I]](sr: SearchResult[I, T]): Boolean = {
    val obj: T = sr.found
    val clazz: Class[_ <: T] = sr.found.getClass

    val refused: Boolean = attributeRxs.exists(p => {
      val field = clazz.getDeclaredField(p._1)
      field.setAccessible(true)
      val value = field.get(obj) + ""
      !value.matches(p._2)
    })

    !refused
  }

  private def checkScores[I, T <: PkDataSet[I]](sr: SearchResult[I, T]): Boolean = {
    val minChecked: Boolean = minScore match {
      case Some(score) => score <= sr.score
      case _ => true
    }
    val maxChecked: Boolean = maxScore match {
      case Some(score) => score >= sr.score
      case _ => true
    }

    minChecked && maxChecked
  }

}
