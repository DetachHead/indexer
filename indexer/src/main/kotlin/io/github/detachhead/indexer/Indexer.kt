package io.github.detachhead.indexer

import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * a [Map] of [Path]s representing the file the tokens were found in to the corresponding [Token]s.
 * the [Token]s will always be sorted in ascending order of their appearance in the file
 */
public typealias SearchResults = Map<Path, List<Token>>

/**
 * main class for the indexer library. an instance of [Indexer] can watch multiple "root paths"
 * which can be either files or directories. this class can also be extended with custom text
 * splitting functionality by overriding [split]
 */
public abstract class Indexer {
  internal val watchers = mutableSetOf<IndexerFileWatcher>()

  internal val scope = CoroutineScope(Dispatchers.Default)

  /**
   * all root paths watched by the indexer. note that this does mean all files that have been
   * identified by the indexer, only the paths that were explicitly added using [watchPath]. use
   * [allFiles] for that instead.
   */
  public fun watchedRootPaths(): Set<Path> = watchers.flatMap { it.rootPaths }.toSet()

  private fun getWatcherForRootPath(path: Path) = watchers.find { path in it.rootPaths }

  private fun getWatcherContainingPath(path: Path) =
      watchers.find { watcher -> watcher.rootPaths.any { path.isInDirectory(it) } }

  /**
   * adds the specified path to the list of paths to be watched / indexed. if the [rootPath] is
   * already being watched either directly or indirectly as a child of an already watched root path,
   * this function will do nothing and return `false`.
   *
   * @return `true` if the path was added, `false` if it was already being watched
   */
  public suspend fun watchPath(rootPath: Path): Boolean {
    // if this path is already being watched by an existing watcher, do nothing
    if (getWatcherContainingPath(rootPath) != null) {
      return false
    }

    // if we are already watching a path inside this directory, unwatch it first so we can replace
    // it with this one to prevent duplicates
    val watchedPathToReplace = watchedRootPaths().find { it.isInDirectory(rootPath) }

    // if this is a file and there's already a watcher watching its parent directory, just add that
    // file to the list
    // files for the existing watcher
    if (rootPath.isRegularFile()) {
      val existingWatcher = watchers.find { it.directory == rootPath.parent }
      if (existingWatcher != null) {
        existingWatcher.rootPaths.add(rootPath)
        return true
      }
    }
    val newWatcher = IndexerFileWatcher(setOf(rootPath), indexer = this)
    watchers.add(newWatcher)
    try {
      newWatcher.walkAndWatch(scope)
    } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
      newWatcher.onError(e, rootPath)
      unwatchPathStrict(rootPath)
      return false
    }
    // we wait until after the initial indexing is successful, otherwise we would need to re-watch
    // the original path if it failed
    if (watchedPathToReplace != null) {
      unwatchPathStrict(watchedPathToReplace)
    }
    return true
  }

  /**
   * unwatches the specified [rootPath]. note that only root paths can be unwatched (ie. the paths
   * returned from [watchedRootPaths])
   */
  public fun unwatchPath(rootPath: Path): Boolean {
    val watcher = getWatcherForRootPath(rootPath)
    if (watcher == null) {
      return false
    }
    if (watcher.isWatchingFiles) {
      // we are just removing a file from the watched directory instead of deleting the watcher
      // itself
      watcher.rootPaths.remove(rootPath)
      watcher.index.remove(rootPath)
    }
    watcher.close()
    watchers.remove(watcher)
    return true
  }

  /**
   * stricter version of [unwatchPath] that raises an exception if the root path is not being
   * watched. the public version of this function doesn't do this because if the path isn't being
   * watched then it's already in the desired state, so it's unlikely that the user would want to
   * throw an exception here.
   */
  internal fun unwatchPathStrict(rootPath: Path) {
    if (!unwatchPath(rootPath)) {
      throw InternalIndexerError("attempted to unwatch $rootPath but it's not being watched")
    }
  }

  /** all files found by the indexer */
  public fun allFiles(): Set<Path> = watchers.flatMap { it.index.keys }.toSet()

  /**
   * any actions to be performed when a file is added, removed or modified, in addition to updating
   * the index. this method will always be called *after* the index has been updated.
   *
   * note:
   * - if a directory is added or modified, an event is emitted for the directory itself as well as
   *   its children
   * - if a directory is removed, an event is emitted for the directory itself, but an event may not
   *   be emitted for its children
   * - duplicate events can occur. this is due to the fact that the underlying file watcher library
   *   behaves differently on windows depending on whether a directory was added or renamed.
   */
  public open fun onChange(event: ChangeEvent) {}

  /**
   * any errors raised during the indexing / file watching process.
   *
   * note:
   * - if the error occurs during the initial indexing of a watched root path, it will be assumed
   *   that the path cannot be watched and [unwatchPath] will automatically be called on the
   *   [rootPath]
   * - if the error was a [OutOfMemoryError], [unwatchPath] will automatically be called on the
   *   [rootPath] to free up memory
   *
   * @param error the error that was raised
   * @param rootPath the watched [Path] that the error occurred on
   */
  public abstract fun onError(error: Throwable, rootPath: Path)

  /** a custom mechanism for splitting the file content into tokens */
  public abstract fun split(fileContent: String): Iterable<Token>

  internal fun splitToMap(path: Path): Tokens =
      split(path.readText()).groupBy({ it.value }) { it.position }.mapValues { it.value.toSet() }

  /**
   * searches for files that contain the specified token
   *
   * @return a [Map] of [Path]s to a [List] of indexes representing the positions in the file where
   *   the token was found. does not return [SearchResults] as that type contains the token values,
   *   which is redundant here because this method searches for one token
   */
  public suspend fun searchForToken(token: String): Map<Path, List<Int>> =
      searchForAllTokens(setOf(token)).mapValues { searchResult ->
        searchResult.value.map { it.position }
      }

  /** searches for files that contain all the specified tokens */
  public suspend fun searchForAllTokens(tokens: Set<String>): SearchResults =
      search(tokens, Iterable<String>::all)

  /** searches for files that contain at least one of the specified tokens */
  public suspend fun searchForAnyTokens(tokens: Set<String>): SearchResults =
      search(tokens, Iterable<String>::any)

  private suspend fun search(
      tokens: Set<String>,
      filterFunction: Iterable<String>.(((String) -> Boolean)) -> Boolean
  ): SearchResults = coroutineScope {
    val indexEntryChunks =
        watchers
            .flatMap { it.index.toList() }
            .toMap()
            .splitInto(Runtime.getRuntime().availableProcessors())
    indexEntryChunks
        .mapIndexed { index, indexEntries ->
          async(Dispatchers.Default) {
            indexEntries.mapNotNull { entry ->
              if (tokens.filterFunction { token -> token in entry.value }) {
                entry.key to
                    entry.value.entries
                        // filter out tokens that weren't in the search query
                        .filter { it.key in tokens }
                        .flatMap { entry -> entry.value.map { Token(entry.key, it) } }
                        // for user convenience, ensure that the tokens are sorted in order of
                        // appearance. we do this here because it isn't guaranteed that a custom
                        // split function won't order the tokens differently
                        .sortedBy { it.position }
              } else {
                null
              }
            }
          }
        }
        .awaitAll()
        .flatten()
        .toMap()
  }
}
