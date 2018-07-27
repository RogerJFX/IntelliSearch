package de.crazything.search

trait QueryConfig {

  protected object QueryEnabled {
    val EXACT = 1
    val WILDCARD = 2
    val REGEX = 4
    val PHONETIC = 8
    val FUZZY = 16

    val ALL: Int = EXACT | WILDCARD | REGEX | PHONETIC | FUZZY
  }

  protected object Boost {
    val EXACT: Float = 20F
    val WILDCARD: Float = 10F
    val REGEX: Float = 7F
    val PHONETIC: Float = 4F
    val FUZZY: Float = 1F
  }

  protected val FUZZY_MAX_EDITS = 2

  protected val PHONETIC_SUFFIX = "_PH"

}
