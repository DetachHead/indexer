package io.github.detachhead.indexer

/** a basic indexer that splits on whitespace */
public abstract class WhitespaceIndexer : Indexer() {
  override fun split(fileContent: String): List<Token> =
      Regex("\\S+").findAll(fileContent).map { Token(it.value, it.range.first) }.toList()
}
