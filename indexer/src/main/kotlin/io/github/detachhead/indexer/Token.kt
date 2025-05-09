package io.github.detachhead.indexer

public data class Token(public val value: String, public val position: Int) {
  public val range: IntRange by lazy { position..(position + value.length) }
}
