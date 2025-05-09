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
internal class IndexerTest {
  @TempDir lateinit var tempDir: Path

  @Test
  fun `indexes files created before the watcher started`() = runBlocking {
    val fileWithToken = tempDir / "asdf"
    fileWithToken.writeText("foo bar baz")
    (tempDir / "asdf2").writeText("a b c")
    val indexer = WhitespaceIndexer()
    indexer.watchPath(tempDir)
    waitForFileWatcher()
    assert(indexer.searchForToken("bar") == mapOf(fileWithToken to listOf(4)))
  }

  @Test
  fun `indexes files created while the watcher is running`() = runBlocking {
    val indexer = WhitespaceIndexer()
    indexer.watchPath(tempDir)
    waitForFileWatcher()
    val fileWithToken = tempDir / "asdf"
    fileWithToken.writeText("foo bar baz")
    waitForFileWatcher()
    assert(indexer.searchForToken("bar") == mapOf(fileWithToken to listOf(4)))
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
    fun searchForToken() = runTest {
      assert(indexer.searchForToken("bar") == mapOf(fileWithToken to listOf(4)))
    }

    @Test
    fun searchForAllTokens() = runTest {
      assert(indexer.searchForAllTokens(setOf("bar", "asdf")) == emptyMap<Path, List<Token>>())
      assert(
          indexer.searchForAllTokens(setOf("bar", "baz")) ==
              mapOf(fileWithToken to listOf(Token("bar", 4), Token("baz", 8))))
    }

    @Test
    fun searchForAnyTokens() = runTest {
      assert(
          indexer.searchForAnyTokens(setOf("bar", "asdf")) ==
              mapOf(fileWithToken to listOf(Token("bar", 4))))
      assert(indexer.searchForAllTokens(setOf("asdf", "fdsa")) == emptyMap<Path, List<Token>>())
    }
  }
}
