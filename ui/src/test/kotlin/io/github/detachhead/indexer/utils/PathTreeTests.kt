package io.github.detachhead.indexer.utils

import kotlin.io.path.Path
import kotlin.test.Test

class PathTreeTests {

  @Test
  fun `pathTree function`() {
    val rootPath = Path("foo")
    val paths =
        listOf(Path("foo/baz"), Path("foo/bar/qux"), Path("foo/bar/asdf"), Path("foo/a/b/c"))
    assert(
        pathTree(rootPath, paths.toSet()) ==
            PathTree(rootPath) {
              setOf(
                  PathTree(Path("foo/bar")) {
                    setOf(
                        PathTree(Path("foo/bar/qux")) { emptySet() },
                        PathTree(Path("foo/bar/asdf")) { emptySet() })
                  },
                  PathTree(Path("foo/baz")) { emptySet() },
                  PathTree(Path("foo/a")) {
                    setOf(
                        PathTree(Path("foo/a/b")) {
                          setOf(PathTree(Path("foo/a/b/c")) { emptySet() })
                        })
                  })
            })
  }
}
