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

internal fun <T> List<T>.splitInto(n: Int): List<List<T>> {
  if (isEmpty()) {
    return List(n) { emptyList() } // Return n empty lists if the original is empty
  }
  val groups = this.withIndex().groupBy { it.index % n }
  return List(n) { i -> groups[i]?.map { it.value } ?: emptyList() }
}
