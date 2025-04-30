package io.github.detachhead.indexer.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    searchText: String,
    onSearch: (String) -> Unit,
    onQueryChange: (String) -> Unit,
) {
  // Controls expansion state of the search bar
  var expanded by rememberSaveable { mutableStateOf(false) }

  SearchBar(
      inputField = {
        SearchBarDefaults.InputField(
            query = searchText,
            onQueryChange = onQueryChange,
            onSearch = {
              onSearch(searchText)
              expanded = false
            },
            expanded = expanded,
            onExpandedChange = { expanded = it },
            placeholder = { Text("Search") })
      },
      expanded = expanded,
      onExpandedChange = { expanded = it },
  ) {}
}
