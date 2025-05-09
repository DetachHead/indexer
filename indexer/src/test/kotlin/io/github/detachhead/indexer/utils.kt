package io.github.detachhead.indexer

import kotlinx.coroutines.delay

/** waits for an event to be picked up by the file watcher */
suspend fun waitForFileWatcher() =
    // TODO: can we come up with a better way to do this other than this hardcoded delay?
    delay(1000L)
