package com.wa2c.android.cifsdocumentsprovider.common.exception

import java.io.IOException

/**
 * Storage exception.
 */
sealed class StorageException(message: String?, cause: Throwable?) : IOException(message, cause) {
    class Error : StorageException {
        constructor(cause: Throwable) : super(cause.localizedMessage, cause)
        constructor(message: String) : super(message, null)
    }

    sealed class File(message: String?, cause: Throwable?) : StorageException(message, cause) {
        class NotFound(cause: Throwable? = null) : File(cause?.localizedMessage ?: "File is not found.", cause)
        class DocumentId(cause: Throwable? = null) : File(cause?.localizedMessage ?: "Invalid document id.", cause)
    }

    sealed class Operation(message: String?, cause: Throwable?) : StorageException(message, cause) {
        class Unsupported(cause: Throwable) : Operation(cause.localizedMessage ?: "Unsupported operation.", cause)
        class AccessMode(cause: Throwable? = null) : Operation("Writing is not allowed in reading mode.", cause)
        class ReadOnly(cause: Throwable? = null) : Operation("Writing is not allowed in options.", cause)
        class RandomAccessNotPermitted(cause: Throwable? = null) : Operation("This type does not support random writing.", cause)
    }

    sealed class Security(message: String?, cause: Throwable?, val id: String) : StorageException(message, cause) {
        class Auth(cause: Throwable, id: String) : Security(cause.localizedMessage ?: "Authentication failed.", cause, id)
        class UnknownHost(cause: Throwable, id: String) : Security(cause.localizedMessage ?: "Unknown host.", cause, id)
    }

    sealed class Transaction(message: String?, cause: Throwable?) : StorageException(message, cause) {
        class HostNotFound(cause: Throwable) : Transaction(cause.localizedMessage ?: "Host not found.", cause)
        class Timeout(cause: Throwable) : Transaction(cause.localizedMessage ?:  "Connection timeout.", cause)
        class Network(cause: Throwable) : Transaction(cause.localizedMessage ?: "Network disconnected.", cause)
    }

}
