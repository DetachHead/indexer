package io.github.detachhead.indexer.ui

import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

@Composable
fun FileContents(
    content: String,
    selection: TextRange?,
    modifier: Modifier = Modifier,
) {
  TextField(
      value = TextFieldValue(text = content, selection = selection ?: TextRange.Zero),
      readOnly = true,
      onValueChange = {
        // do nothing, it's readonly
      },
      modifier = modifier)
}
