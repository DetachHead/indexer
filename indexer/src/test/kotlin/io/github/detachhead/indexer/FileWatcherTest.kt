package io.github.detachhead.indexer

import io.methvin.watcher.DirectoryChangeEvent
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.createFile
import kotlin.io.path.div
import kotlin.io.path.relativeTo
import kotlin.io.path.writeText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

internal class TestFileWatcher(paths: Set<Path>) : FileWatcher(paths) {
  val loggedEvents = mutableListOf<DirectoryChangeEvent>()

  override fun onChange(event: DirectoryChangeEvent) {
    loggedEvents.add(event)
  }
}

internal suspend fun runWithWatcher(vararg paths: Path, block: suspend TestFileWatcher.() -> Unit) {
  val watcher = TestFileWatcher(paths.toSet())
  try {
    watcher.block()
  } finally {
    watcher.close()
  }
}

class FileWatcherTests {
  @TempDir lateinit var tempDir: Path

  @Test
  fun `can watch directories`() = runBlocking {
    runWithWatcher(tempDir) {
      launch(Dispatchers.Default) {
        println("starting watcher in ${Thread.currentThread().name}")
        watch()
      }
      waitForFileWatcher()
      val newFile = (tempDir / "asdf").createFile()
      waitForFileWatcher()
      assert(loggedEvents.size == 1)
      assert(loggedEvents[0].path() == newFile)
    }
  }

  @Test
  fun `can watch files`() = runBlocking {
    val file1 = tempDir / "file1"
    val file2 = tempDir / "file2"
    val ignoredFile = tempDir / "file3"
    val files = listOf(file1, file2, ignoredFile)
    files.map { it.createFile() }
    runWithWatcher(file1, file2) {
      launch(Dispatchers.Default) { watch() }
      waitForFileWatcher()
      assert(loggedEvents.isEmpty())
      files.map { it.writeText("asdf") }
      waitForFileWatcher()
      assert(loggedEvents.map { it.path() } == listOf(file1, file2))
    }
  }

  @Test
  fun `relative paths`() = runBlocking {
    val file = (tempDir / "file1").relativeTo(cwd())
    file.createFile()
    runWithWatcher(file) {
      launch(Dispatchers.Default) { watch() }
      waitForFileWatcher()
      assert(loggedEvents.isEmpty())
      file.writeText("asdf")
      waitForFileWatcher()
      assert(loggedEvents.map { it.path() } == listOf(file.normalize().absolute()))
    }
  }
}
