package de.crazything.search.language

import org.apache.lucene.analysis.Analyzer

trait Phonetic {

  protected def phoneticAnalyzer: Analyzer

}
