package de.crazything.app.test.helpers

import de.crazything.search.DirectoryContainer
import org.apache.lucene.index.DirectoryReader

object IndexCheckerUtil {

    def checkIndex(name: String, fieldName: String): Unit = {
      val directory = DirectoryContainer.pickDirectoryForName(name)
      val reader: DirectoryReader = DirectoryReader.open(directory)
      for(i <- 0 until reader.maxDoc()) {
        println(reader.document(i).get(fieldName))
      }
    }

}
