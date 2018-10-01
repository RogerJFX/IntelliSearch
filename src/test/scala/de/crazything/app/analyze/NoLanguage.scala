package de.crazything.app.analyze

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer

trait NoLanguage {

  implicit val phoneticAnalyzer: Analyzer = new StandardAnalyzer()

}
