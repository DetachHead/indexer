package io.github.detachhead.indexer

import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolute

/** whether the path is located inside the specified directory, including if it's nested */
internal fun Path.isInDirectory(directory: Path) =
    this.normalize().toString().startsWith(directory.absolute().toString())

/** most of the time, you want to call both [Path.normalize] and [Path.absolute] */
internal fun Path.fix() = normalize().absolute()

/** the current working directory */
internal fun cwd() = Path(System.getProperty("user.dir"))
