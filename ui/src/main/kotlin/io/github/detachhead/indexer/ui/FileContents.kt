package io.github.detachhead.indexer.ui

import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import io.github.detachhead.indexer.utils.textRangeForSubstring

@Composable
fun FileContents(
    content: String,
    searchText: String,
    modifier: Modifier = Modifier,
) {
  TextField(
      value =
          TextFieldValue(text = content, selection = textRangeForSubstring(content, searchText)),
      readOnly = true,
      onValueChange = {
        // do nothing, it's readonly
      },
      modifier = modifier)
}
