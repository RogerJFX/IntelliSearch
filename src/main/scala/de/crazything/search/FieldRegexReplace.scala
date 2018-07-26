package de.crazything.search

trait FieldRegexReplace {

  def regexTerms: Seq[String]

  val createRegexTerm: (String) => String = (origin) => regexTerms.foldLeft(origin)((r, c) => r.replaceAll(c, c))



}
