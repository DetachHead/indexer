/**
 * we define our own version of bonsai's `TreeScope.FileSystemTree` and `TreeScope.FileSystemNode`
 * because their versions are private, but we want to use them directly because we have a filesystem
 * tree as a node inside a regular tree
 */
package io.github.detachhead.indexer.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import cafe.adriel.bonsai.core.node.Branch
import cafe.adriel.bonsai.core.node.Leaf
import cafe.adriel.bonsai.core.tree.TreeScope
import okio.FileSystem
import okio.Path

private val fileSystem = FileSystem.SYSTEM

@Composable
fun TreeScope.FileSystemTree(rootPath: Path, selfInclude: Boolean = false) {
  if (selfInclude) {
    FileSystemNode(rootPath)
  } else {
    fileSystem.listOrNull(rootPath)?.forEach { path -> FileSystemNode(path) }
  }
}

@Composable
fun TreeScope.FileSystemNode(path: Path) {
  if (fileSystem.metadata(path).isDirectory) {
    Branch(content = path, name = path.name, customIcon = { Icon(Icons.Outlined.Folder, "File") }) {
      FileSystemTree(path)
    }
  } else {
    Leaf(
        content = path,
        name = path.name,
        customIcon = { Icon(Icons.AutoMirrored.Outlined.InsertDriveFile, "Folder") })
  }
}
