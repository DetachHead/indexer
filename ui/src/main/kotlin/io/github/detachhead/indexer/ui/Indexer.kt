package io.github.detachhead.indexer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.LinearProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import io.github.detachhead.indexer.SearchResults
import io.github.detachhead.indexer.utils.SearchIndexer
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.path
import io.methvin.watcher.DirectoryChangeEvent
import java.awt.Desktop
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Indexer(watchedPaths: List<Path>, onAddWatchedPaths: suspend (List<Path>) -> Unit) {
  val coroutineScope = rememberCoroutineScope()

  var loadingText by remember { mutableStateOf<String?>(null) }
  var searchResults by remember { mutableStateOf<SearchResults?>(null) }
  var openFile by remember { mutableStateOf<Path?>(null) }
  var openFileContent by remember { mutableStateOf("") }
  var highlightedTokenIndex by remember { mutableStateOf(-1) }
  val allFiles = remember { mutableStateListOf<Path>() }
  var searchText by remember { mutableStateOf("") }

  fun closeFile() {
    openFile = null
    openFileContent = ""
    highlightedTokenIndex = -1
  }

  suspend fun search(indexer: SearchIndexer) {
    val tokens = searchText.split(" ").filter { it != "" }.toSet()
    if (tokens.isEmpty()) {
      // don't bother searching for nothing, so just show all files instead
      searchResults = null
    } else {
      val result = indexer.searchForAllTokens(tokens)
      if (openFile !in result) {
        closeFile()
      }
      searchResults = result
    }
  }

  val indexer = remember {
    SearchIndexer {
      val path = it.path()
      when (it.eventType()) {
        DirectoryChangeEvent.EventType.CREATE -> {
          allFiles.add(path)
          coroutineScope.launch { search(this@SearchIndexer) }
        }
        DirectoryChangeEvent.EventType.MODIFY -> {
          coroutineScope.launch {
            search(this@SearchIndexer)
            if (openFile == path && searchResults?.contains(path) != false) {
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
          allFiles.remove(path)
          coroutineScope.launch { search(this@SearchIndexer) }
        }
        DirectoryChangeEvent.EventType.OVERFLOW -> {
          TODO("what is overflow?")
        }
      }
    }
  }
  val tokensForCurrentFile = searchResults?.get(openFile)

  suspend fun watchPaths(paths: List<Path>) {
    if (paths.isEmpty()) return
    loadingText = "Indexing"
    paths.forEach { indexer.watchPath(it) }
    allFiles.apply {
      clear()
      addAll(indexer.allFiles())
    }
    loadingText = null
  }

  LaunchedEffect(Unit) { watchPaths(watchedPaths) }

  suspend fun addWatchedPaths(paths: List<Path>) {
    watchPaths(paths)
    onAddWatchedPaths(paths)
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
              IconButtonWithTooltip(
                  icon = sparkle,
                  tooltip = "Try the New Search Experience, powered by AI!",
                  onClick = {
                    val urlEncodedSearchText =
                        URLEncoder.encode(searchText, StandardCharsets.UTF_8.toString())
                    Desktop.getDesktop()
                        .browse(
                            URI(
                                "https://chat.openai.com/?model=gpt-4&prompt=$urlEncodedSearchText"))
                  })
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
                      if (directory != null) {
                        addWatchedPaths(listOf(Path(directory.path)))
                      }
                    }
                  })
              IconButtonWithTooltip(
                  icon = Icons.AutoMirrored.Outlined.InsertDriveFile,
                  tooltip = "Watch file",
                  onClick = {
                    coroutineScope.launch {
                      val files = FileKit.openFilePicker(mode = FileKitMode.Multiple())
                      if (files != null) {
                        addWatchedPaths(files.map { Path(it.path) })
                      }
                    }
                  })
            })
      },
      bottomBar = {
        Box {
          BottomAppBar(
              containerColor = MaterialTheme.colorScheme.primaryContainer,
              contentColor = MaterialTheme.colorScheme.primary,
          ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                  if (loadingText != null) {
                    Text(text = "$loadingText...", Modifier.padding(start = 30.dp))
                  }
                  Spacer(modifier = Modifier.weight(1f))
                  if (tokensForCurrentFile != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End) {
                          TokenHighlightControls(
                              matchCount = tokensForCurrentFile.count(),
                              highlightedIndex = highlightedTokenIndex,
                              onChange = { highlightedTokenIndex = it })
                        }
                  }
                }
          }
          if (loadingText != null) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                // we're intentionally using material 2's progress indicator because it has square
                // edges which i think looks better when it fills the max width, so we have to
                // explicitly tell it to use the material 3 colours
                color = MaterialTheme.colorScheme.primary,
                backgroundColor = MaterialTheme.colorScheme.primaryContainer)
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
                allPaths = searchResults?.keys ?: allFiles.toSet(),
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
