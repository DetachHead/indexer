import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.createFile
import kotlin.io.path.div
import kotlin.test.Test
import kotlin.test.assertFailsWith

class UtilsTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun isInDirectory() {
        val dir = (tempDir / "dir").createDirectory()
        val fileInDir = (dir / "foo").createFile()
        assert(fileInDir.isInDirectory(dir))

        val fileInOtherDir = ((tempDir / "otherDir").createDirectory() / "bar").createFile()
        assert(!fileInOtherDir.isInDirectory(dir))
    }

    @Test
    fun allMatch() {
        assert(listOf("a", "b", "c").allMatch { it.length })
        assert(!listOf("a", "b", "cd").allMatch { it.length })
    }

    @Test
    fun `combineMaps allowDuplicates=true`() {
        assert(
            combineMaps(listOf(mapOf("a" to "b", "c" to "d"), mapOf("e" to "f", "c" to "z"))) == mapOf(
                "a" to "b",
                "c" to "z",
                "e" to "f"
            )
        )
    }

    @Test
    fun `combineMaps allowDuplicates=false`() {
        assertFailsWith<CombineMapsException> {
            combineMaps(
                listOf(mapOf("a" to "b", "c" to "d"), mapOf("e" to "f", "c" to "z")),
                allowDuplicates = false
            )
        }

    }
}
