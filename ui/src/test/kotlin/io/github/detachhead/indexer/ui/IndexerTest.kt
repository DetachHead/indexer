package io.github.detachhead.indexer.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import java.nio.file.Path
import kotlin.io.path.createFile
import kotlin.io.path.deleteExisting
import kotlin.io.path.div
import kotlin.io.path.relativeTo
import kotlin.io.path.writeText
import kotlin.test.Test
import org.junit.jupiter.api.io.TempDir

@OptIn(ExperimentalTestApi::class)
class IndexerTest {
  @TempDir lateinit var tempDir: Path

  fun ComposeUiTest.expandTreeAndOpenFile(watchedPath: Path, file: Path) {
    // expand the root node with the watched path
    onNodeWithText(watchedPath.toString()).performClick()
    // traverse the tree, expanding each node until we get to the file
    file.relativeTo(watchedPath).forEach { onNodeWithText(it.toString()).performClick() }
  }

  fun ComposeUiTest.createAppAndWatchTempDir() {
    setContent {
      val watchedPaths = remember { mutableStateListOf(tempDir) }
      Indexer(watchedPaths, onAddWatchedPaths = { watchedPaths.addAll(it) })
    }
  }

  fun ComposeUiTest.setupWithTestFile(fileContent: String): Path {
    val file = tempDir / "asdf"
    val fileContent = fileContent
    file.writeText(fileContent)
    createAppAndWatchTempDir()
    return file
  }

  @Test
  fun `open file`() = runComposeUiTest {
    val fileContent = "foo bar baz"
    val file = setupWithTestFile(fileContent)
    expandTreeAndOpenFile(tempDir, file)
    onNodeWithTag("fileContents").assertTextEquals(fileContent)
  }

  @Test
  fun search() = runComposeUiTest {
    val fileContent = "foo bar baz"
    val file = setupWithTestFile(fileContent)
    onNodeWithText("Search for words").performTextInput(fileContent)
    expandTreeAndOpenFile(tempDir, file)
    onNodeWithText("1 of 3").assertExists()
  }

  @Test
  fun `ui updates when new file is created`() = runComposeUiTest {
    createAppAndWatchTempDir()
    val file = tempDir / "asdf"
    file.createFile()
    expandTreeAndOpenFile(tempDir, file)
  }

  @Test
  fun `ui updates when file content changes`() = runComposeUiTest {
    val file = setupWithTestFile("foo bar baz")
    expandTreeAndOpenFile(tempDir, file)
    val newFileContent = "new content"
    file.writeText(newFileContent)
    onNodeWithTag("fileContents").assertTextEquals(newFileContent)
  }

  @Test
  fun `ui updates when file is deleted`() = runComposeUiTest {
    val fileContent = "foo bar baz"
    val file = setupWithTestFile(fileContent)
    expandTreeAndOpenFile(tempDir, file)
    file.deleteExisting()
    // if the file content is empty that means the file is no longer open because it was removed
    // from the tree
    onNodeWithTag("fileContents").assertTextEquals("")
  }

  @Test
  fun `highlighted token index updates correctly`() = runComposeUiTest {
    val fileContent = "foo foo foo bar"
    val file = setupWithTestFile(fileContent)
    onNodeWithText("Search for words").performTextInput("foo")
    expandTreeAndOpenFile(tempDir, file)
    onNodeWithText("1 of 3").assertExists()
    repeat(2) { onNodeWithTag("Next match").performClick() }
    onNodeWithText("3 of 3").assertExists()
    onNodeWithText("foo").performTextReplacement("bar")
    onNodeWithText("1 of 1").assertExists()
  }
}
