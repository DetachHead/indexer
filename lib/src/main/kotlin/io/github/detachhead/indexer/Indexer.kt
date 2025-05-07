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

/**
 * a [Map] of [Path]s representing the file the tokens were found in to the corresponding [Token]s.
 * the [Token]s will always be sorted in ascending order of their appearance in the file
 */
public typealias SearchResults = Map<Path, List<Token>>

private typealias SplitFunction = (String) -> Tokens

/**
 * the abstract [Indexer.split] function returns an [Iterable] of [Token]s. while that's easier for
 * users, it's not very space-efficient. since we're storing the index in memory, we convert the
 * index to a [Map] of tokens to a [Set] of positions where the token is present in the file. this
 * prevents duplicated tokens in memory
 */
private typealias Tokens = Map<String, Set<Int>>

internal class IndexedFile(val path: Path, val split: SplitFunction) {
  val index: Tokens by lazy { split(path.readText()) }

  operator fun contains(token: String) = token in index
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
    val newWatcher = IndexerFileWatcher(setOf(path), split = ::splitToMap)
    watchers.add(newWatcher)
    newWatcher.walkAndWatch(scope)
    return true
  }

  /** a custom mechanism for splitting the file content into tokens */
  public abstract fun split(fileContent: String): Iterable<Token>

  private fun splitToMap(fileContent: String): Tokens =
      split(fileContent).groupBy({ it.value }) { it.position }.mapValues { it.value.toSet() }

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
        watchers.flatMap { it.index }.splitInto(Runtime.getRuntime().availableProcessors())
    // TODO: this is pretty wacky, can it be simplified at all?
    indexEntryChunks
        .mapIndexed { index, indexEntries ->
          async(Dispatchers.Default) {
            indexEntries.mapNotNull { entry ->
              if (tokens.filterFunction { token -> token in entry }) {
                entry.path to
                    entry.index.entries
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
