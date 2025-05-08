package io.github.detachhead.indexer.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import java.nio.file.Path

@Composable
fun App() {
  val watchedPaths = remember { mutableStateListOf<Path>() }
  Indexer(watchedPaths = watchedPaths, onAddWatchedPaths = { watchedPaths.addAll(it) })
}
