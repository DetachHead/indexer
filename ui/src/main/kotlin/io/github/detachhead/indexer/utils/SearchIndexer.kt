package io.github.detachhead.indexer.utils

import io.github.detachhead.indexer.Indexer

class SearchIndexer : Indexer() {
  override fun split(fileContent: String): Set<String> = fileContent.split(" ").toSet()
}
