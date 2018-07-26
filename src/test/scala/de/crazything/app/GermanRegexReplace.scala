package de.crazything.app

import de.crazything.search.FieldRegexReplace

trait GermanRegexReplace extends FieldRegexReplace {

  override val regexTerms = Seq(
    "(a|e)(i|j|y)e?",
    "(ss|ß)"
  )

}
