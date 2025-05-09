package io.github.detachhead.indexer

import java.nio.file.Path
import java.util.concurrent.ConcurrentSkipListSet
import kotlin.io.path.createDirectories
import kotlin.io.path.createDirectory
import kotlin.io.path.createFile
import kotlin.io.path.div
import kotlin.io.path.walk
import kotlin.test.Test
import kotlin.time.measureTime
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.io.TempDir

class PathUtilsTest {
  @TempDir lateinit var tempDir: Path

  @Test
  fun isInDirectory() {
    val directory = (tempDir / "dir").createDirectory()
    val fileInDir = (directory / "foo").createFile()
    assert(fileInDir.isInDirectory(directory))

    val fileInOtherDir = ((tempDir / "otherDir").createDirectory() / "bar").createFile()
    assert(!fileInOtherDir.isInDirectory(directory))
  }

  @Test
  fun forEachFastWalk() = runBlocking {
    val files = setOf(tempDir / "foo/bar/baz", tempDir / "foo/bar/qux", tempDir / "foo/asdf")
    files.forEach {
      it.parent.createDirectories()
      it.createFile()
    }
    val foundFiles = ConcurrentSkipListSet<Path>()
    tempDir.forEachFastWalk { foundFiles.add(it) }
    assert(foundFiles == files)
  }

  @Test
  fun `forEachFastWalk is faster than regular walk`() = runTest {
    // create a ton of files so that the time difference is noticeable
    (1..10)
        .map { tempDir / it.toString() }
        .forEach { dir ->
          dir.createDirectory()
          (1..1_000).forEach { (dir / it.toString()).createFile() }
        }
    val fastWalkResult = ConcurrentSkipListSet<Path>()
    val fastWalkDuration = measureTime { tempDir.forEachFastWalk { fastWalkResult.add(it) } }

    var walkResult: Set<Path>
    val walkDuration = measureTime { walkResult = tempDir.walk().toSet() }

    println("fast walk took $fastWalkDuration, regular walk took $walkDuration")
    // make sure it was at least 1.25x faster. in my testing it seems to be roughly 6 times faster
    // on a real life large directory, but we use a much lower number here because we don't want the
    // test to be flaky and the difference might not be as high on some machines
    assert(walkDuration / fastWalkDuration >= 1.25)

    // sanity check
    assert(walkResult == fastWalkResult)
  }
}
