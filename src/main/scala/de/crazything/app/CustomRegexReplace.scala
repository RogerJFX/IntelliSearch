package de.crazything.app

import de.crazything.search.FieldRegexReplace

trait CustomRegexReplace extends FieldRegexReplace {

  override val regexTerms = Some(Seq(
    "(a|e)(i|j|y)e?",
    "(ss|ß)"
  ))

}
