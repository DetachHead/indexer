package io.github.detachhead.indexer

import io.methvin.watcher.DirectoryChangeEvent
import io.methvin.watcher.DirectoryWatcher
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile

internal class FileWatcherException(message: String) :
    Exception("all paths specified to ${FileWatcher::class.simpleName} $message")

/**
 * wrapper on top of [DirectoryWatcher] that supports watching individual files as well as
 * directories
 */
internal abstract class FileWatcher(paths: Set<Path>) {
  internal val watcher: DirectoryWatcher

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
              if (!isWatchingFiles
              // since DirectoryWatcher doesn't support watching an individual files we watch the
              // whole directory instead
              // and only call the event if the file we care about was changed
              || event.path() in this.paths) {
                onChange(event)
              }
            }
            .build()
  }

  abstract fun onChange(event: DirectoryChangeEvent?)

  open fun watch() = watcher.watch()

  fun close() = watcher.close()
}
