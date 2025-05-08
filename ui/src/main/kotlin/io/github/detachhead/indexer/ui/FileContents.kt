package io.github.detachhead.indexer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

@Composable
fun FileContents(
    content: String,
    selection: TextRange?,
    modifier: Modifier = Modifier,
) {
  BasicTextField(
      value = TextFieldValue(text = content, selection = selection ?: TextRange.Zero),
      readOnly = true,
      onValueChange = {
        // do nothing, it's readonly
      },
      textStyle = TextStyle(fontFamily = FontFamily.Monospace),
      modifier =
          modifier
              .background(MaterialTheme.colorScheme.surfaceVariant)
              .padding(horizontal = 16.dp)
              .testTag("fileContents"),
  )
}
