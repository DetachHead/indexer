package io.github.detachhead.indexer

import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.writeText
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

private class TestIndexer : Indexer() {
  override fun split(fileContent: String): Set<String> = fileContent.split(" ").toSet()
}

class IndexerTests {
  @TempDir lateinit var tempDir: Path

  @Test
  fun `indexes files created before the watcher started`() = runBlocking {
    val fileWithToken = tempDir / "asdf"
    fileWithToken.writeText("foo bar baz")
    (tempDir / "asdf2").writeText("a b c")
    val indexer = TestIndexer()
    indexer.watchPath(tempDir)
    delay(1000L) // TODO: can we avoid delay?
    assert(indexer.searchForToken("bar") == setOf(fileWithToken))
  }

  @Test
  fun `indexes files created while the watcher is running`() = runBlocking {
    val indexer = TestIndexer()
    indexer.watchPath(tempDir)
    delay(1000L) // TODO: can we avoid delay?
    val fileWithToken = tempDir / "asdf"
    fileWithToken.writeText("foo bar baz")
    delay(1000L)
    assert(indexer.searchForToken("bar") == setOf(fileWithToken))
  }
}
