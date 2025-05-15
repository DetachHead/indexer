package io.github.detachhead.indexer.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.nio.file.Path

@Composable
fun App() {
  // this is a separate component to Indexer so that the unit tests can pass the watchedPaths into
  // the indexer
  var watchedPaths by remember { mutableStateOf(LinkedHashSet<Path>()) }
  Indexer(watchedPaths = watchedPaths, onChangeWatchedPaths = { watchedPaths = it })
}
