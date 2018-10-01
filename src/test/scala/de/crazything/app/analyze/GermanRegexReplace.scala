package de.crazything.app.analyze

import de.crazything.search.FieldRegexReplace

trait GermanRegexReplace extends FieldRegexReplace {

  override val regexTerms = Seq(
    "(a|e)(i|j|y)e?",
    "(ss|ß)",
    "(oe|ö)",
    "(c|k)"
  )

}
