package io.github.detachhead.indexer.ui

import io.github.detachhead.indexer.Indexer

class SearchIndexer : Indexer() {
  override fun split(fileContent: String): List<String> = fileContent.split(" ")
}
