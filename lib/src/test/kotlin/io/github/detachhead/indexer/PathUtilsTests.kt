package io.github.detachhead.indexer

import java.nio.file.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.createFile
import kotlin.io.path.div
import kotlin.test.Test
import org.junit.jupiter.api.io.TempDir

class PathUtilsTests {
  @TempDir lateinit var tempDir: Path

  @Test
  fun isInDirectory() {
    val directory = (tempDir / "dir").createDirectory()
    val fileInDir = (directory / "foo").createFile()
    assert(fileInDir.isInDirectory(directory))

    val fileInOtherDir = ((tempDir / "otherDir").createDirectory() / "bar").createFile()
    assert(!fileInOtherDir.isInDirectory(directory))
  }
}
