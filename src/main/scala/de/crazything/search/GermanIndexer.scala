package de.crazything.search

import org.apache.commons.codec.language.ColognePhonetic
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.phonetic.PhoneticFilter
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.store.Directory
import org.slf4j.LoggerFactory

object GermanIndexer extends AbstractIndexer {

  private val logger = LoggerFactory.getLogger(GermanIndexer.getClass)

  private val analyzer = new StandardAnalyzer()

  val phoneticAnalyzer: Analyzer = (fieldName: String) => {
    logger.debug(s"creating component for field $fieldName")
    import org.apache.lucene.analysis.standard.StandardTokenizer
    val tokenizer = new StandardTokenizer
    val filter = new PhoneticFilter(tokenizer, new ColognePhonetic(), true)
    new Analyzer.TokenStreamComponents(tokenizer, filter)
  }

  override protected def putDirectoryReference(directory: Directory): Unit = {
    GermanSearcher.setDirectory(directory)
  }

  def index[I, T <: PkDataSet[I]](data: Seq[T], factory: AbstractTypeFactory[I, T]): Unit = {
    createIndex(phoneticAnalyzer, data, factory)
  }

}
