package io.github.detachhead.indexer

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
