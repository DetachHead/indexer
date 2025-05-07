package io.github.detachhead.indexer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun TokenHighlightControls(
    matchCount: Int,
    highlightedIndex: Int,
    onChange: (Int) -> Unit,
) {
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.End,
      verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onChange(highlightedIndex - 1) }, enabled = highlightedIndex > 0) {
          Icon(Icons.Outlined.SkipPrevious, "Previous occurrence")
        }
        Text(
            text = "${highlightedIndex + 1} of $matchCount",
        )
        IconButton(
            onClick = { onChange(highlightedIndex + 1) },
            enabled = highlightedIndex < matchCount - 1) {
              Icon(Icons.Outlined.SkipNext, "Next occurrence")
            }
      }
}
