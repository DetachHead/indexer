import io.methvin.watcher.DirectoryChangeEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.io.path.walk

internal class IndexerFileWatcher(vararg paths: Path, val split: (String) -> List<String>) : FileWatcher(*paths) {
    val index = ConcurrentHashMap<Path, List<String>>()
    override fun onChange(event: DirectoryChangeEvent?) {
        val path = event?.path()
        if (path == null)
            TODO("when is the event null?")

        when (event.eventType()) {
            DirectoryChangeEvent.EventType.CREATE, DirectoryChangeEvent.EventType.MODIFY -> updateIndexForFile(path)
            DirectoryChangeEvent.EventType.DELETE -> index.remove(path)
            DirectoryChangeEvent.EventType.OVERFLOW -> TODO("what causes overflow?")
        }
    }

    override fun watch() {
        // need to do an initial index of the current state of the watched files, otherwise the index will only be
        // populated with data from files that have changed since the watcher was started
        if (isWatchingFiles)
            paths.forEach { updateIndexForFile(it) }
        else
            directory.walk().forEach { updateIndexForFile(it) }
        super.watch()
    }

    private fun updateIndexForFile(path: Path) {
        index[path] = split(path.readText())
    }
}

public abstract class Indexer {
    internal val watchers = mutableSetOf<IndexerFileWatcher>()

    internal val scope = CoroutineScope(Dispatchers.Default)

    /**
     * adds the specified path to the list of paths to be watched / indexed
     *
     * @return `true` if the path was added, `false` if it was already being watched
     */
    public fun watchPath(path: Path): Boolean {
        // if there's already a watcher watching this exact path, do nothing
        if (watchers.any { path in it.paths }) {
            return false
        }
        // if this is a file and there's already a watcher watching its parent directory, just add that file to the list
        // files for the existing watcher
        if (path.isRegularFile()) {
            val existingWatcher = watchers.find { it.directory == path.parent }
            if (existingWatcher != null) {
                existingWatcher.paths.add(path)
                return true
            }
        }
        val newWatcher = IndexerFileWatcher(path, split = ::split)
        watchers.add(newWatcher)
        scope.launch(Dispatchers.Default) {
            newWatcher.watch()
        }
        return true
    }

    /**
     * a custom mechanism for splitting the file content into tokens
     */
    public abstract fun split(fileContent: String): List<String>

    /**
     * searches for files that contain the specified token and returns a list of files that contain the token
     */
    public fun searchForToken(token: String): Set<Path> {
        // TODO: multithreaded search
        val index = combineMaps(watchers.map { it.index }, allowDuplicates = false)
        return index.filter { entry -> token in entry.value }.keys
    }

    // TODO: insert some other search functions too eg. For searching a file that contains a specified sequence of tokens,
    //  file that contains some of the specified tokens, etc?
}
