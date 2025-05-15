/**
 * we define our own version of bonsai's filesystem tree because we have a filesystem tree as a node
 * inside a regular tree, and we also want to also support filtering based on search results
 */
package io.github.detachhead.indexer.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import cafe.adriel.bonsai.core.Bonsai
import cafe.adriel.bonsai.core.node.Branch
import cafe.adriel.bonsai.core.node.Leaf
import cafe.adriel.bonsai.core.tree.Tree
import cafe.adriel.bonsai.core.tree.TreeScope
import io.github.detachhead.indexer.utils.PathTree
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import okio.Path.Companion.toOkioPath

@Composable
private fun TreeScope.Folder(path: okio.Path, children: @Composable TreeScope.() -> Unit = {}) {
  Branch(
      content = path,
      name = path.name,
      customIcon = { Icon(Icons.Outlined.Folder, "Folder") },
      children = children)
}

@Composable
private fun TreeScope.File(path: okio.Path) {
  Leaf(
      content = path,
      name = path.name,
      customIcon = { Icon(Icons.AutoMirrored.Outlined.InsertDriveFile, "File") },
  )
}

@Composable
private fun TreeScope.FilteredPathsTree(trees: List<PathTree>) {
  trees
      .sortedBy { it.path.name }
      .forEach {
        key(it.path) {
          if (it.path.isDirectory()) {
            Folder(it.path.toOkioPath()) { FilteredPathsTree(it.children.toList()) }
          } else {
            File(it.path.toOkioPath())
          }
        }
      }
}

@Composable
fun WatchedPathsTree(
    watchedPaths: List<PathTree>,
    onOpenFile: (Path) -> Unit,
    modifier: Modifier = Modifier,
) {
  val tree =
      Tree<okio.Path> {
        watchedPaths.forEach { watchedPath ->
          val okioPath = watchedPath.path.toOkioPath()
          key(okioPath) {
            if (watchedPath.path.isRegularFile()) {
              Leaf(
                  okioPath,
                  customIcon = { Icon(Icons.AutoMirrored.Filled.ManageSearch, "Watched file") },
              )
            } else {
              Branch(
                  okioPath,
                  customIcon = { Icon(Icons.AutoMirrored.Filled.ManageSearch, "Watched folder") }) {
                    FilteredPathsTree(watchedPath.children.toList())
                  }
            }
          }
        }
      }
  Bonsai(
      modifier = modifier,
      tree = tree,
      onClick = { node ->
        tree.clearSelection()
        tree.toggleExpansion(node)
        val value = node.content
        val path = value.toNioPath()
        if (path.isRegularFile()) {
          onOpenFile(path)
        }
      })
}
