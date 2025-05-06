package io.github.detachhead.indexer.utils

import io.github.detachhead.indexer.WhitespaceIndexer

private fun isBinaryContent(content: String): Boolean {
  for (char in content) {
    if (char < '\u0009' || char in '\u000E'..'\u001F' || char == '\u007F') {
      return true // non-printable/control characters
    }
  }
  return false
}

class SearchIndexer : WhitespaceIndexer() {
  override fun split(fileContent: String): Set<String> =
      if (isBinaryContent(fileContent)) {
        emptySet()
      } else {
        super.split(fileContent)
      }
}
