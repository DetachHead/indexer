package io.github.detachhead.indexer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import io.github.detachhead.indexer.SearchResults
import io.github.detachhead.indexer.utils.SearchIndexer
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.path
import io.methvin.watcher.DirectoryChangeEvent
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// TODO: ui unit tests

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
  val coroutineScope = rememberCoroutineScope()

  val watchedPaths = remember { mutableStateListOf<Path>() }
  var searchResults by remember { mutableStateOf<SearchResults?>(null) }
  var searchText by remember { mutableStateOf("") }
  var openFile by remember { mutableStateOf<Path?>(null) }
  var openFileContent by remember { mutableStateOf("") }
  var highlightedTokenIndex by remember { mutableStateOf(-1) }
  var allFiles by remember { mutableStateOf(emptySet<Path>()) }

  fun closeFile() {
    openFile = null
    openFileContent = ""
    highlightedTokenIndex = -1
  }

  suspend fun search(indexer: SearchIndexer) {
    val result = indexer.searchForAllTokens(searchText.split(" ").filter { it != "" }.toSet())
    if (openFile !in result) {
      closeFile()
    }
    searchResults = result
  }

  val indexer = remember {
    SearchIndexer {
      val path = it.path()
      when (it.eventType()) {
        DirectoryChangeEvent.EventType.CREATE -> {
          allFiles = allFiles + path
          coroutineScope.launch { search(this@SearchIndexer) }
        }
        DirectoryChangeEvent.EventType.MODIFY -> {
          coroutineScope.launch {
            search(this@SearchIndexer)
            if (openFile == path && searchResults?.contains(path) == true) {
              openFileContent = path.readText()
              // we go back to the first search result in the file in case tokens were
              // rearranged/deleted
              highlightedTokenIndex = 0
            }
          }
        }
        DirectoryChangeEvent.EventType.DELETE -> {
          if (openFile == path) {
            closeFile()
          }
          allFiles = allFiles - path
        }
        DirectoryChangeEvent.EventType.OVERFLOW -> {
          TODO("what is overflow?")
        }
      }
    }
  }
  val tokensForCurrentFile = searchResults?.get(openFile)

  suspend fun watchPath(file: PlatformFile?) {
    if (file == null) return
    val path = Path(file.path)
    watchedPaths.add(path)
    searchText = ""
    indexer.watchPath(path)
    allFiles = indexer.allFiles()
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
                  onQueryChange = {
                    searchText = it
                    coroutineScope.launch { search(indexer) }
                  })
              IconButtonWithTooltip(
                  icon = Icons.Outlined.Folder,
                  tooltip = "Watch folder",
                  onClick = {
                    coroutineScope.launch(Dispatchers.IO) {
                      val directory = FileKit.openDirectoryPicker()
                      watchPath(directory)
                    }
                  })
              IconButtonWithTooltip(
                  icon = Icons.AutoMirrored.Outlined.InsertDriveFile,
                  tooltip = "Watch file",
                  onClick = {
                    coroutineScope.launch {
                      val files = FileKit.openFilePicker(mode = FileKitMode.Multiple())
                      files?.forEach { watchPath(it) }
                    }
                  })
            })
      },
      bottomBar = {
        BottomAppBar(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
          if (tokensForCurrentFile != null) {
            TokenHighlightControls(
                matchCount = tokensForCurrentFile.count(),
                highlightedIndex = highlightedTokenIndex,
                onChange = { highlightedTokenIndex = it })
          }
        }
      }) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          Row(modifier = Modifier.fillMaxWidth()) {
            WatchedPathsTree(
                watchedPaths = watchedPaths,
                allPaths = searchResults?.keys ?: allFiles,
                onOpenFile = {
                  openFile = it
                  openFileContent = it.readText()
                  highlightedTokenIndex = 0
                },
                modifier = Modifier.weight(1f))
            FileContents(
                content = openFileContent,
                selection =
                    tokensForCurrentFile?.getOrNull(highlightedTokenIndex)?.let {
                      TextRange(it.range.first, it.range.last)
                    },
                modifier = Modifier.weight(2.25f).fillMaxHeight())
          }
        }
      }
}
