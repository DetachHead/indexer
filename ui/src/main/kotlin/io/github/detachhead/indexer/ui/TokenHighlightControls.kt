package io.github.detachhead.indexer.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun TokenHighlightControls(
    matchCount: Int,
    highlightedIndex: Int,
    onChange: (Int) -> Unit,
) {
  IconButtonWithTooltip(
      icon = Icons.AutoMirrored.Outlined.ArrowBack,
      tooltip = "Previous match",
      onClick = { onChange(highlightedIndex - 1) },
      disabledReason = if (highlightedIndex <= 0) "No previous matches" else null)
  Text(
      text = "${highlightedIndex + 1} of $matchCount",
  )
  IconButtonWithTooltip(
      icon = Icons.AutoMirrored.Outlined.ArrowForward,
      tooltip = "Next match",
      onClick = { onChange(highlightedIndex + 1) },
      disabledReason = if (highlightedIndex >= matchCount - 1) "No more matches" else null)
}
