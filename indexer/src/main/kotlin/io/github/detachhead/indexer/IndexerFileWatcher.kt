package io.github.detachhead.indexer

import io.methvin.watcher.DirectoryChangeEvent
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * the abstract [Indexer.split] function returns an [Iterable] of [Token]s. while that's easier for
 * users, it's not very space-efficient. since we're storing the index in memory, we convert the
 * index to a [Map] of tokens to a [Set] of positions where the token is present in the file. this
 * prevents duplicated tokens in memory
 */
internal typealias Tokens = Map<String, Set<Int>>

internal class IndexerFileWatcher(paths: Set<Path>, val indexer: Indexer) : FileWatcher(paths) {
  /** all watched files should have an entry in the index. */
  val index = ConcurrentHashMap<Path, Tokens>()

  override fun onChange(event: ChangeEvent) {
    val path = event.path
    when (event.eventType) {
      DirectoryChangeEvent.EventType.CREATE,
      DirectoryChangeEvent.EventType.MODIFY -> {
        // we only care about create/modify events for individual files when updating the index
        if (!event.isDirectory) {
          index[path] = indexer.splitToMap(path)
        }
      }
      DirectoryChangeEvent.EventType.DELETE -> {
        if (event.isDirectory) {
          // if a directory is deleted, we may not get events for each of its children so we need to
          // delete them here. see FileWatcher for more info
          index.keys.forEach { if (it.isInDirectory(path)) index.remove(it) }
        }
        index.remove(path)
      }
      DirectoryChangeEvent.EventType.OVERFLOW -> {
        throw NotImplementedError("an overflow occurred on $path")
      }
    }
    indexer.onChange(event)
  }

  /**
   * we need to do an initial index of the current state of the watched files, otherwise the index
   * will only be populated with data from files that have changed since the watcher was started
   */
  suspend fun walkAndWatch(scope: CoroutineScope) {
    if (isWatchingFiles) {
      paths.forEach { index[it] = indexer.splitToMap(it) }
    } else {
      directory.forEachFastWalk { index[it] = indexer.splitToMap(it) }
    }
    scope.launch(Dispatchers.Default) { watch() }
  }

  override fun onError(error: Throwable, path: Path) {
    if (error is Error) {
      index.clear()
    }
    indexer.onError(error, path)
  }
}
