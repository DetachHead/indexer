package io.github.detachhead.indexer

import kotlin.test.Test

class IterableUtilsTests {
  @Test
  fun allEqual() {
    assert(listOf("a", "b", "c").allEqual { it.length })
    assert(!listOf("a", "b", "cd").allEqual { it.length })
  }
}
