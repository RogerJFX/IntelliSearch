package de.crazything.search

trait FieldRegexReplace {

  def regexTerms: Option[Seq[String]] = None

  val createRegexTerm: (String) => String = (origin) =>
    regexTerms match {
      case Some(terms) =>
        terms.foldLeft(origin)((r, c) => r.replaceAll(c, c))
      case _ =>
        origin
    }


}
