package io.github.detachhead.indexer.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    searchText: String,
    onQueryChange: (String) -> Unit,
) {
  TextField(
      value = searchText,
      onValueChange = onQueryChange,
      placeholder = { Text("Search for words") },
      leadingIcon = { Icon(Icons.Outlined.Search, "Search") },
      shape = RoundedCornerShape(56.dp / 2),
      colors =
          TextFieldDefaults.colors(
              focusedIndicatorColor = Color.Transparent,
              unfocusedIndicatorColor = Color.Transparent,
              disabledIndicatorColor = Color.Transparent,
              errorIndicatorColor = Color.Transparent,
              unfocusedContainerColor = MaterialTheme.colorScheme.background,
              focusedContainerColor = MaterialTheme.colorScheme.background),
  )
}
