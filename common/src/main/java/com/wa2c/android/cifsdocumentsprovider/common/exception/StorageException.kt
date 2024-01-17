package com.wa2c.android.cifsdocumentsprovider.common.exception

import java.io.IOException

/**
 * Edit exception.
 */
sealed class StorageException(message: String) : IOException(message) {
    class FileNotFoundException : StorageException("File is not found.")

    class AccessModeException : StorageException("Writing is not allowed in reading mode.")

    class ReadOnlyException : StorageException("Writing is not allowed in options.")

    class DocumentIdException : StorageException("Invalid document id.")

    class RandomAccessNotPermittedException : StorageException("This type does not support random writing.")
}
