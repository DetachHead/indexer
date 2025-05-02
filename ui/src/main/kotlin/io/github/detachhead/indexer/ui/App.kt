package io.github.detachhead.indexer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.detachhead.indexer.utils.SearchIndexer
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.path
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlinx.coroutines.launch

// TODO: ui unit tests

// TODO: update tree view when changes are made to the files on disk
private val indexer = SearchIndexer()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
  val watchedPaths = remember { mutableStateListOf<Path>() }
  var filteredPaths by remember { mutableStateOf<Set<Path>?>(null) }
  var searchText by remember { mutableStateOf("") }
  var openFileContent by remember { mutableStateOf("") }
  val coroutineScope = rememberCoroutineScope()
  fun watchPath(file: PlatformFile?) {
    if (file == null) return
    val path = Path(file.path)
    watchedPaths.add(path)
    searchText = ""
    indexer.watchPath(path)
  }
  Scaffold(
      topBar = {
        TopAppBar(
            colors =
                topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
            title = { Text("Indexer") },
            actions = {
              SearchBar(
                  searchText,
                  onQueryChange = { searchText = it },
                  onSearch = {
                    // TODO: better search
                    filteredPaths = indexer.searchForToken(it)
                  })
              IconButton(
                  onClick = {
                    coroutineScope.launch {
                      val directory = FileKit.openDirectoryPicker()
                      watchPath(directory)
                    }
                  }) {
                    Icon(Icons.Outlined.Folder, "Watch folder")
                  }
              IconButton(
                  onClick = {
                    coroutineScope.launch {
                      val files = FileKit.openFilePicker(mode = FileKitMode.Multiple())
                      files?.forEach { watchPath(it) }
                    }
                  }) {
                    Icon(Icons.Outlined.FileCopy, "Watch file")
                  }
            })
      },
  ) { innerPadding ->
    Column(
        modifier = Modifier.padding(innerPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Row(modifier = Modifier.fillMaxWidth()) {
        WatchedPathsTree(
            paths = watchedPaths,
            onlyIncludePaths = filteredPaths,
            onOpenFile = { openFileContent = it.readText() },
            modifier = Modifier.weight(1f))
        FileContents(
            content = openFileContent,
            searchText = searchText,
            modifier = Modifier.weight(2.25f).fillMaxHeight())
      }
    }
  }
}
