package io.github.detachhead.indexer

import java.nio.file.Path
import kotlin.io.path.absolute

// TODO: split this into separate files if we end up with multiple path related utils, iterable
// related utils, etc

/** whether the path is located inside the specified directory, including if it's nested */
internal fun Path.isInDirectory(directory: Path) =
    this.normalize().toString().startsWith(directory.absolute().toString())

/** whether the given predicate returns the same value for each item */
internal fun <T, U> Iterable<T>.allMatch(predicate: (T) -> U): Boolean {
  var expectedValue: U? = null
  forEachIndexed { index, value ->
    val predicateResult = predicate(value)
    if (index == 0) {
      expectedValue = predicateResult
    } else if (predicateResult != expectedValue) {
      return@allMatch false
    }
  }
  return true
}

internal class CombineMapsException(key: Any?) :
    Exception("multiple entries for key `$key` were found when combining maps")

/**
 * combines all items from the provided [maps] into one map
 *
 * @param K the key type
 * @param V the value type
 * @param maps list of maps to combine
 * @param allowDuplicates if `true` and the same key is present in multiple maps, the most recent
 *   one is used. if `false`, an exception is raised when duplicates are detected
 */
internal fun <K, V> combineMaps(maps: List<Map<K, V>>, allowDuplicates: Boolean = true): Map<K, V> =
    mutableMapOf<K, V>().apply {
      for (map in maps) {
        if (allowDuplicates) {
          putAll(map)
        } else {
          map.forEach { key, value ->
            if (contains(key)) throw CombineMapsException(key)
            set(key, value)
          }
        }
      }
    }
