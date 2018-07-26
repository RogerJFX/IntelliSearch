package de.crazything.app

import de.crazything.search.language.GermanPhonetic
import org.apache.lucene.analysis.Analyzer

trait GermanLanguage {

  implicit val phoneticAnalyzer: Analyzer = GermanPhonetic.phoneticAnalyzer

}
