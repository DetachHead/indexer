package io.github.detachhead.indexer

import kotlin.test.Test

class IterableUtilsTest {
  @Test
  fun allEqual() {
    assert(listOf("a", "b", "c").allEqual { it.length })
    assert(!listOf("a", "b", "cd").allEqual { it.length })
  }
}
