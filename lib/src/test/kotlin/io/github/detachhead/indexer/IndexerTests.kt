package io.github.detachhead.indexer

import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.writeText
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class IndexerTests {
  @TempDir lateinit var tempDir: Path

  @Test
  fun `indexes files created before the watcher started`() = runBlocking {
    val fileWithToken = tempDir / "asdf"
    fileWithToken.writeText("foo bar baz")
    (tempDir / "asdf2").writeText("a b c")
    val indexer = WhitespaceIndexer()
    indexer.watchPath(tempDir)
    waitForFileWatcher()
    assert(indexer.searchForToken("bar") == setOf(fileWithToken))
  }

  @Test
  fun `indexes files created while the watcher is running`() = runBlocking {
    val indexer = WhitespaceIndexer()
    indexer.watchPath(tempDir)
    waitForFileWatcher()
    val fileWithToken = tempDir / "asdf"
    fileWithToken.writeText("foo bar baz")
    waitForFileWatcher()
    assert(indexer.searchForToken("bar") == setOf(fileWithToken))
  }

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  inner class SearchTests {
    @TempDir lateinit var tempDir: Path

    lateinit var fileWithToken: Path
    lateinit var indexer: WhitespaceIndexer

    @BeforeEach
    fun setUp() = runBlocking {
      indexer = WhitespaceIndexer()
      indexer.watchPath(tempDir)
      waitForFileWatcher()
      fileWithToken = tempDir / "asdf"
      fileWithToken.writeText("foo bar baz")
      waitForFileWatcher()
    }

    @Test
    fun searchForAllTokens() = runTest {
      assert(indexer.searchForAllTokens(setOf("bar", "asdf")) == emptySet<Path>())
      assert(indexer.searchForAllTokens(setOf("bar", "baz")) == setOf(fileWithToken))
    }

    @Test
    fun searchForAnyTokens() = runTest {
      assert(indexer.searchForAnyTokens(setOf("bar", "asdf")) == setOf(fileWithToken))
      assert(indexer.searchForAllTokens(setOf("asdf", "fdsa")) == emptySet<Path>())
    }
  }
}
