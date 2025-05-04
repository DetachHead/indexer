package io.github.detachhead.indexer

import java.nio.file.Path
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** whether the path is located inside the specified directory, including if it's nested */
internal fun Path.isInDirectory(directory: Path) =
    this.normalize().toString().startsWith(directory.absolute().toString())

/** most of the time, you want to call both [Path.normalize] and [Path.absolute] */
internal fun Path.fix() = normalize().absolute()

/** the current working directory */
internal fun cwd() = Path(System.getProperty("user.dir"))

/** faster version of `Path.walk` */
internal suspend fun Path.fastWalk(): Set<Path> = coroutineScope {
  val result = ConcurrentLinkedQueue<Path>()
  val semaphore = Semaphore(permits = 64)

  suspend fun walk(dir: Path) {
    semaphore.withPermit {
      for (entry in dir.listDirectoryEntries()) {
        if (entry.isDirectory()) {
          launch { walk(entry) }
        } else {
          result.add(entry)
        }
      }
    }
  }

  walk(this@fastWalk)
  this.coroutineContext.job.children.forEach { it.join() }
  result.toSet()
}
