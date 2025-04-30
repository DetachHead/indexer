package io.github.detachhead.indexer.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ManageSearch
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import cafe.adriel.bonsai.core.Bonsai
import cafe.adriel.bonsai.core.node.Branch
import cafe.adriel.bonsai.core.tree.Tree
import cafe.adriel.bonsai.filesystem.FileSystemBonsaiStyle
import cafe.adriel.bonsai.filesystem.FileSystemTree
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.path
import java.nio.file.Path
import kotlin.io.path.Path
import kotlinx.coroutines.launch

private val indexer = SearchIndexer()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
  val pathsToWatch = remember { mutableStateListOf<Path>() }
  val coroutineScope = rememberCoroutineScope()
  fun watchPath(file: PlatformFile?) {
    if (file == null) return
    val path = Path(file.path)
    pathsToWatch.add(path)
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
            title = { Text("Top app bar") },
            actions = {
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
  ) {
    pathsToWatch.forEach {
      Tree<Any> {
        Branch(
            it.toString(),
            customIcon = { Icon(Icons.AutoMirrored.Filled.ManageSearch, "Watched file") }) {
              val tree = FileSystemTree(rootPath = it)
              Bonsai(
                  tree = tree,
                  // Custom style
                  style = FileSystemBonsaiStyle())
            }
      }
    }
  }
}
