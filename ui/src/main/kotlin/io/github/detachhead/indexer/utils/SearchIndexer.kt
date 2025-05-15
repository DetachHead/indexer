package io.github.detachhead.indexer.utils

import io.github.detachhead.indexer.ChangeEvent
import io.github.detachhead.indexer.Token
import io.github.detachhead.indexer.WhitespaceIndexer
import java.nio.file.Path

private fun String.isBinaryContent(): Boolean {
  for (char in this) {
    if (char < '\u0009' || char in '\u000E'..'\u001F' || char == '\u007F') {
      return true // non-printable/control characters
    }
  }
  return false
}

class SearchIndexer(
    private val onChangeFunction: SearchIndexer.(ChangeEvent) -> Unit,
    private val onErrorFunction: SearchIndexer.(Throwable, Path) -> Unit
) : WhitespaceIndexer() {
  override fun split(fileContent: String): List<Token> =
      if (fileContent.isBinaryContent()) {
        emptyList()
      } else {
        super.split(fileContent)
      }

  override fun onChange(event: ChangeEvent) {
    onChangeFunction(event)
  }

  override fun onError(error: Throwable, path: Path) {
    onErrorFunction(error, path)
  }
}
