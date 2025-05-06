package io.github.detachhead.indexer

/** a basic indexer that splits on whitespace */
public open class WhitespaceIndexer : Indexer() {
  override fun split(fileContent: String): Set<String> =
      fileContent.split(" ", "\n").filter { it != "" }.toSet()
}
