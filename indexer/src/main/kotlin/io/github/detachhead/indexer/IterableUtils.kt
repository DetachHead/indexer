package io.github.detachhead.indexer

/** whether the given predicate returns the same value for each item */
internal fun <T, U> Iterable<T>.allEqual(predicate: (T) -> U): Boolean {
  var expectedValue: U? = null
  forEachIndexed { index, value ->
    val predicateResult = predicate(value)
    if (index == 0) {
      expectedValue = predicateResult
    } else if (predicateResult != expectedValue) {
      return@allEqual false
    }
  }
  return true
}

internal fun <K, V> Map<K, V>.splitInto(n: Int): List<Map<K, V>> {
  if (isEmpty()) {
    return List(n) { emptyMap() }
  }
  val groups = this.toList().withIndex().groupBy { it.index % n }
  return List(n) { i -> groups[i]?.associate { it.value } ?: emptyMap() }
}
