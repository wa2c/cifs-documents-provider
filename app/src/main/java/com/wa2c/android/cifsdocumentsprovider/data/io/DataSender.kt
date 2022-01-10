package com.wa2c.android.cifsdocumentsprovider.data.io

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.wa2c.android.cifsdocumentsprovider.common.values.BUFFER_SIZE
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataSender @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Get DocumentFile
     */
    fun getDocumentFile(uri: Uri): DocumentFile? {
        return if (DocumentsContract.isTreeUri(uri)) {
            DocumentFile.fromTreeUri(context, uri)
        } else {
            DocumentFile.fromSingleUri(context, uri)
        }
    }

    /**
     * Send single file
     * @return False if canceled
     */
    fun sendFile(
        sourceUri: Uri,
        targetUri: Uri,
        bufferSize: Int = BUFFER_SIZE,
        updateProgress: (progressSize: Long) -> Boolean
    ): Boolean {
        val buffer = ByteArray(bufferSize)
        var progressSize = 0L
        (context.contentResolver.openInputStream(sourceUri) ?: return false).use { input ->
            (context.contentResolver.openOutputStream(targetUri) ?: return false).use { output ->
                while (true) {
                    val length = input.read(buffer)
                    if (length <= 0) break // End of Data
                    output.write(buffer, 0, length)
                    progressSize += length
                    if (!updateProgress(progressSize)) return false
                }
            }
        }
        return true
    }
}