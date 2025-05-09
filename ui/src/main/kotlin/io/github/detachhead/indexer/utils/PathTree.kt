package io.github.detachhead.indexer.utils

import java.nio.file.Path

class PathTree(val path: Path, private val getChildren: () -> Set<PathTree>) {
  val children by lazy { getChildren() }

  override fun equals(other: Any?) =
      other is PathTree && path == other.path && children == other.children

  override fun hashCode(): Int = path.hashCode()
}

/**
 * a directory tree structure that does not look at the files on disk to construct the structure.
 * instead, you pass it all the file paths, then it constructs it based on the paths alone.
 *
 * this means the paths must have been fully resolved beforehand.
 */
// this could be made more efficient if the paths were specified as a list in the order that they
// were walked, but the api returns the paths as keys in a Map which is unordered, so we can't
fun pathTree(rootPath: Path, allPaths: Set<Path>): PathTree {
  fun nested(rootPath: Path, allPaths: Set<Path>): PathTree {
    val children = allPaths.filter { it.parent == rootPath }
    return PathTree(rootPath) { children.map { nested(it, allPaths) }.toSet() }
  }
  // add parents because the specified paths are only for files and does not include parent
  // directories
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
