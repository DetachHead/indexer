@file:Suppress("MatchingDeclarationName")

package io.github.detachhead.indexer

import java.nio.file.Path
import kotlinx.coroutines.delay

/**
 * exception used in tests so that any unexpected errors are thrown and fail the test so that we
 * don't have to manually handle them
 */
internal class IndexingException(exception: Exception, path: Path) :
    Exception("the following exception occurred while indexing $path: $exception")

/** waits for an event to be picked up by the file watcher */
suspend fun waitForFileWatcher() =
    // TODO: can we come up with a better way to do this other than this hardcoded delay?
    delay(1000L)
