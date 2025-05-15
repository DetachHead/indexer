package io.github.detachhead.indexer

/** base class for all exceptions in the indexer library */
public sealed class IndexerError(message: String) : Exception(message)

/** unexpected exceptions, likely a bug in the library */
public class InternalIndexerError(message: String) :
    IndexerError("an unexpected error occurred in the indexer library: $message")

/** likely a user error */
public sealed class UserIndexerError(message: String) : IndexerError(message)
