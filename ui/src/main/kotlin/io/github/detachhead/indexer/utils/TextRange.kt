package io.github.detachhead.indexer.utils

import androidx.compose.ui.text.TextRange

fun textRangeForSubstring(content: String, substring: String): TextRange {
  val startIndex = content.indexOf(substring)
  if (startIndex < 0) {
    return TextRange.Zero
  }
  return TextRange(startIndex, startIndex + substring.length)
}
