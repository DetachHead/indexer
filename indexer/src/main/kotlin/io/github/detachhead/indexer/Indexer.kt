package io.github.detachhead.indexer

import io.methvin.watcher.DirectoryChangeEvent
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * a [Map] of [Path]s representing the file the tokens were found in to the corresponding [Token]s.
 * the [Token]s will always be sorted in ascending order of their appearance in the file
 */
public typealias SearchResults = Map<Path, List<Token>>

/**
 * the abstract [Indexer.split] function returns an [Iterable] of [Token]s. while that's easier for
 * users, it's not very space-efficient. since we're storing the index in memory, we convert the
 * index to a [Map] of tokens to a [Set] of positions where the token is present in the file. this
 * prevents duplicated tokens in memory
 */
private typealias Tokens = Map<String, Set<Int>>

internal class IndexerFileWatcher(paths: Set<Path>, val indexer: Indexer) : FileWatcher(paths) {
  /** all watched files should have an entry in the index. */
  val index = ConcurrentHashMap<Path, Tokens>()

  override fun onChange(event: DirectoryChangeEvent?) {
    val path = event?.path()
    if (path == null) TODO("when is the event null?")
    when (event.eventType()) {
      DirectoryChangeEvent.EventType.CREATE,
      DirectoryChangeEvent.EventType.MODIFY -> index[path] = indexer.splitToMap(path)
      DirectoryChangeEvent.EventType.DELETE -> index.remove(path)
      DirectoryChangeEvent.EventType.OVERFLOW -> TODO("what causes overflow?")
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
    val newWatcher = IndexerFileWatcher(setOf(path), indexer = this)
    watchers.add(newWatcher)
    newWatcher.walkAndWatch(scope)
    return true
  }

  /** all files found by the indexer */
  public fun allFiles(): Set<Path> = watchers.flatMap { it.index.keys }.toSet()

  /**
   * any actions to be performed when a file is added, removed or modified, in addition to updating
   * the index. this method will always be called *after* the index has been updated.
   *
   * note that [DirectoryChangeEvent] is part of the
   * [directory-watcher](https://github.com/gmethvin/directory-watcher) library which is included as
   * an API dependency of this library
   */
  public open fun onChange(event: DirectoryChangeEvent) {}

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
