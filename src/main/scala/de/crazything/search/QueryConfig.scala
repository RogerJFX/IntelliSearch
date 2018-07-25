package de.crazything.search

trait QueryConfig {

  protected object QueryEnabled {
    val EXACT = 1
    val REGEX = 2
    val PHONETIC = 4
    val FUZZY = 8

    val ALL: Int = EXACT | REGEX | PHONETIC | FUZZY
  }

  protected object Boost {
    val EXACT = 10
    val REGEX = 7
    val PHONETIC = 4
    val FUZZY = 1
  }

  val FUZZY_MAX_EDITS = 2

  protected val PHONETIC_SUFFIX = "_PH"

}
