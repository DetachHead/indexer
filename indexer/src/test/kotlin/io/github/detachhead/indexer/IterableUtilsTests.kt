package io.github.detachhead.indexer

import kotlin.test.Test

class IterableUtilsTests {
  @Test
  fun allMatch() {
    assert(listOf("a", "b", "c").allMatch { it.length })
    assert(!listOf("a", "b", "cd").allMatch { it.length })
  }
}
