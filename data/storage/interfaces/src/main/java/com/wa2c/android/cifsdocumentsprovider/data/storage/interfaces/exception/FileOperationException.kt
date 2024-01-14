package com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.exception

import java.io.IOException

sealed class FileOperationException(message: String) : IOException(message) {
    class WritingNotPermittedException : FileOperationException("File writing is not permitted.")

    class RandomAccessNotPermittedException : FileOperationException("This type does not support random writing.")
}
