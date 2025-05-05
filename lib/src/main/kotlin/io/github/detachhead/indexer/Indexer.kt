package io.github.detachhead.indexer

import io.methvin.watcher.DirectoryChangeEvent
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private typealias SplitFunction = (String) -> Set<String>

internal class IndexedFile(val path: Path, val split: SplitFunction) {
  // TODO: this fills up the RAM with the contents of all files in the directory when searching,
  //  which crashes on large directories
  val index: Set<String> by lazy { split(path.readText()) }
}

internal class IndexerFileWatcher(paths: Set<Path>, val split: SplitFunction) : FileWatcher(paths) {
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

  /**
   * we need to do an initial index of the current state of the watched files, otherwise the index
   * will only be populated with data from files that have changed since the watcher was started
   */
  suspend fun walkAndWatch(scope: CoroutineScope) {
    if (isWatchingFiles) {
      index.addAll(paths.map { IndexedFile(it, split) })
    } else {
      directory.forEachFastWalk { index.add(IndexedFile(it, split)) }
    }
    scope.launch(Dispatchers.Default) { watch() }
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
  public suspend fun watchPath(path: Path): Boolean {
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
    newWatcher.walkAndWatch(scope)
    return true
  }

  /** a custom mechanism for splitting the file content into tokens */
  public abstract fun split(fileContent: String): Set<String>

  /** searches for files that contain the specified token */
  public suspend fun searchForToken(token: String): Set<Path> = searchForAllTokens(setOf(token))

  /** searches for files that contain all the specified tokens */
  public suspend fun searchForAllTokens(tokens: Set<String>): Set<Path> = search {
    tokens.all { token -> token in it.index }
  }

  /** searches for files that contain at least one of the specified tokens */
  public suspend fun searchForAnyTokens(tokens: Set<String>): Set<Path> = search {
    tokens.any { token -> token in it.index }
  }

  private suspend fun search(predicate: (IndexedFile) -> Boolean): Set<Path> = coroutineScope {
    val indexEntryChunks =
        watchers.flatMap { it.index }.splitInto(Runtime.getRuntime().availableProcessors())
    indexEntryChunks
        .mapIndexed { index, indexEntries ->
          async(Dispatchers.Default) {
            indexEntries.map { entry -> if (predicate(entry)) entry.path else null }
          }
        }
        .awaitAll()
        .flatten()
        .filterNotNull()
        .toSet()
  }
}
