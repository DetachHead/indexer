package io.github.detachhead.indexer

import io.methvin.watcher.DirectoryChangeEvent
import io.methvin.watcher.DirectoryWatcher
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class FileWatcherException(message: String) :
    Exception("all paths specified to ${FileWatcher::class.simpleName} $message")

/**
 * note that [DirectoryChangeEvent.EventType] is part of the
 * [directory-watcher](https://github.com/gmethvin/directory-watcher) library which is included as
 * an API dependency of this library
 */
public data class ChangeEvent(
    val eventType: DirectoryChangeEvent.EventType,
    val path: Path,
    val isDirectory: Boolean
)

/**
 * wrapper on top of [DirectoryWatcher] with the following improvements:
 * - supports watching individual files as well as directories
 * - always fires an [onChange] event for children of a directory that's been renamed (with
 *   [DirectoryChangeEvent.EventType.MODIFY], but not necessarily the corresponding
 *   [DirectoryChangeEvent.EventType.DELETE] events for the old names because if the
 *   [DirectoryWatcher] did not emit the event it's impossible for us to know what the old names
 *   were).
 *
 * note that when a directory is created/renamed, duplicated events for its children may be emitted.
 * this is because the underlying file watcher library doesn't always emit these events (on windows
 * at least), so we re-emit them to be safe.
 */
internal abstract class FileWatcher(
    paths: Set<Path>,
    private val onError: (Exception, Path) -> Unit
) {
  internal val watcher: DirectoryWatcher

  private val scope = CoroutineScope(Dispatchers.IO)

  /**
   * the paths to watch that we actually care about. for example if we are watching a single file we
   * need to specify its parent directory to the [watcher], then filter out events related to files
   * we don't care about
   */
  val paths = paths.map { it.fix() }.toMutableSet()

  /** the directory being watched by [watcher] */
  val directory: Path

  /**
   * whether the watcher is watching a directory normally using [DirectoryWatcher] (`false`) or
   * watching individual files (`true`)
   */
  val isWatchingFiles: Boolean

  init {
    val firstPath = this.paths.iterator().next()
    isWatchingFiles =
        if (this.paths.count() == 1) {
          if (firstPath.isDirectory()) {
            directory = firstPath
            false
          } else {
            directory = firstPath.parent
            true
          }
        } else {
          directory = firstPath.parent
          if (!this.paths.allEqual { it.parent }) {
            throw FileWatcherException("must have the same parent directory (expected $directory)")
          }
          if (!this.paths.all { it.isRegularFile() }) {
            throw FileWatcherException("must be files if more than one path is specified")
          }
          true
        }

    this.watcher =
        DirectoryWatcher.builder()
            .path(directory)
            .listener { event ->
              val path = event.path()
              if (!isWatchingFiles
              // since DirectoryWatcher doesn't support watching an individual files we watch the
              // whole directory instead
              // and only call the event if the file we care about was changed
              || path in this.paths) {
                val eventType = event.eventType()
                // on windows, when a directory is renamed, a DELETE event is emitted for all of its
                // children, but the corresponding CREATE event for the new directory name does not
                // have CREATE events for its children.

                // however if a new directory is created, a CREATE event IS emitted for all of its
                // children. unfortunately this means we can't tell whether we need to walk the
                // contents of the directory, so we always do so just to be safe, even though this
                // will sometimes result in duplicated events.

                // since i only have a windows machine, i don't know how it works on other operating
                // systems so this implementation is based on my testing on windows
                scope.launch {
                  if (event.isDirectory && eventType == DirectoryChangeEvent.EventType.CREATE) {
                    path.forEachFastWalk { onChange(ChangeEvent(eventType, it, it.isDirectory())) }
                  }
                  try {
                    onChange(ChangeEvent(eventType, path, event.isDirectory))
                  } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                    onError(e, path)
                  }
                }
              }
            }
            .build()
  }

  abstract fun onChange(event: ChangeEvent)

  open fun watch() = watcher.watch()

  fun close() = watcher.close()
}
