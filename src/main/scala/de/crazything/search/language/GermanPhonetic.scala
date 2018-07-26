package de.crazything.search.language

import org.apache.commons.codec.language.ColognePhonetic
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.phonetic.PhoneticFilter
import org.apache.lucene.analysis.standard.StandardTokenizer

object GermanPhonetic extends Phonetic {

  override lazy val phoneticAnalyzer: Analyzer = (_: String) => {
    val tokenizer = new StandardTokenizer
    val filter = new PhoneticFilter(tokenizer, new ColognePhonetic(), true)
    new Analyzer.TokenStreamComponents(tokenizer, filter)
  }

}
