package io.github.detachhead.indexer.utils

import java.nio.file.Path

data class PathTree(val path: Path, val children: Set<PathTree>)

/**
 * a directory tree structure that does not look at the files on disk to construct the structure.
 * instead, you pass it all the file paths, then it constructs it based on the paths alone.
 *
 * this means the paths must have been fully resolved beforehand.
 */
fun pathTree(rootPath: Path, allPaths: Set<Path>): PathTree {

  fun nested(rootPath: Path, allPaths: Set<Path>): PathTree {
    val children = allPaths.filter { it.parent == rootPath }.toSet()
    return PathTree(rootPath, children.map { nested(it, allPaths) }.toSet())
  }
  return nested(rootPath, (allPaths + allPaths.flatMap { it.getParents() }).toSet())
}

private fun Path.getParents(): Set<Path> {
  val parents = mutableSetOf<Path>()
  var current = this.parent
  while (current != null) {
    parents.add(current)
    current = current.parent
  }
  return parents
}
