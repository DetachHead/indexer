import io.methvin.watcher.DirectoryChangeEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createFile
import kotlin.io.path.div
import kotlin.io.path.writeText

internal class TestFileWatcher(vararg paths: Path) : FileWatcher(*paths) {
    val loggedEvents = mutableListOf<DirectoryChangeEvent>()
    override fun onChange(event: DirectoryChangeEvent?) {
        if (event !== null) {
            loggedEvents.add(event)
        }
    }
}

internal suspend fun runWithWatcher(vararg paths: Path, block: suspend TestFileWatcher.() -> Unit) {
    val watcher = TestFileWatcher(*paths)
    try {
        watcher.block()
    } finally {
        println("closing")
        watcher.close()
        println("closed")
    }
}

class FileWatcherTests {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `can watch directories`() = runBlocking {
        //TODO: handling for if the directory doesn't exist yet?
        //TODO: handling for if the directory gets deleted. currently it closes the watcher when that happens
        runWithWatcher(tempDir) {
            launch(Dispatchers.Default) {
                println("starting watcher in ${Thread.currentThread().name}")
                watch()
            }
            // TODO: is there a better way to wait for the event than hardcoded delays?
            delay(1000L)
            val newFile = (tempDir / "asdf").createFile()
            delay(1000L)
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
        // TODO: currently fails if the file doesnt exist yet, is this behavior ok?
        files.map { it.createFile() }
        runWithWatcher(file1, file2) {
            launch(Dispatchers.Default) {
                watch()
            }
            // TODO: is there a better way to wait for the event than hardcoded delays?
            delay(1000L)
            assert(loggedEvents.isEmpty())
            files.map { it.writeText("asdf") }
            delay(1000L)
            assert(loggedEvents.map { it.path() } == listOf(file1, file2))
        }
    }
}


