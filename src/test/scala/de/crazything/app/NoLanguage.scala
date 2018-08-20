package de.crazything.app

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer

trait NoLanguage {

  implicit val phoneticAnalyzer: Analyzer = new StandardAnalyzer()

}
