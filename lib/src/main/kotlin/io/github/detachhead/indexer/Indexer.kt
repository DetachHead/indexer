package io.github.detachhead.indexer

import io.methvin.watcher.DirectoryChangeEvent
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.io.path.walk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private typealias SplitFunction = (String) -> List<String>

internal class IndexedFile(val path: Path, val split: SplitFunction) {
  // TODO: should the index be a set instead?
  val index: List<String> by lazy { split(path.readText()) }
}

internal class IndexerFileWatcher(paths: Set<Path>, val split: (String) -> List<String>) :
    FileWatcher(paths) {
  /**
   * all watched files should have an entry in the index. if the entry is `null`, it means the file
   * has not been indexed yet and it will be lazily evaluated
   */
  val index = mutableSetOf<IndexedFile>()

  override fun onChange(event: DirectoryChangeEvent?) {
    val path = event?.path()
    if (path == null) TODO("when is the event null?")

    when (event.eventType()) {
      DirectoryChangeEvent.EventType.CREATE,
      DirectoryChangeEvent.EventType.MODIFY -> index.add(IndexedFile(path, split))
      DirectoryChangeEvent.EventType.DELETE -> index.removeIf { it.path == path }
      DirectoryChangeEvent.EventType.OVERFLOW -> TODO("what causes overflow?")
    }
  }

  override fun watch() {
    // need to do an initial index of the current state of the watched files, otherwise the index
    // will only be populated with data from files that have changed since the watcher was started
    // TODO: can we avoid converting the result of walk to a set?
    // TODO: multithreaded walk?
    val allFiles = if (isWatchingFiles) paths else directory.walk().toSet()
    index.addAll(allFiles.map { IndexedFile(it, split) })
    super.watch()
  }
}

public abstract class Indexer {
  internal val watchers = mutableSetOf<IndexerFileWatcher>()

  internal val scope = CoroutineScope(Dispatchers.Default)

  /**
   * adds the specified path to the list of paths to be watched / indexed
   *
   * @return `true` if the path was added, `false` if it was already being watched
   */
  public fun watchPath(path: Path): Boolean {
    // if there's already a watcher watching this exact path, do nothing
    if (watchers.any { path in it.paths }) {
      return false
    }
    // if this is a file and there's already a watcher watching its parent directory, just add that
    // file to the list
    // files for the existing watcher
    if (path.isRegularFile()) {
      val existingWatcher = watchers.find { it.directory == path.parent }
      if (existingWatcher != null) {
        existingWatcher.paths.add(path)
        return true
      }
    }
    val newWatcher = IndexerFileWatcher(setOf(path), split = ::split)
    watchers.add(newWatcher)
    scope.launch(Dispatchers.Default) { newWatcher.watch() }
    return true
  }

  /** a custom mechanism for splitting the file content into tokens */
  public abstract fun split(fileContent: String): List<String>

  /**
   * searches for files that contain the specified token and returns a list of files that contain
   * the token
   */
  public fun searchForToken(token: String): Set<Path> {
    // TODO: multithreaded search
    // TODO: detect duplicates across watchers?
    val index = watchers.flatMap { it.index }
    return index.filter { token in it.index }.map { it.path }.toSet()
  }

  // TODO: insert some other search functions too eg. For searching a file that contains a specified
  // sequence of tokens,
  //  file that contains some of the specified tokens, etc?
}
