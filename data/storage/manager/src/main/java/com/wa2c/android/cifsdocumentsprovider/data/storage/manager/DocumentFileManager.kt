package com.wa2c.android.cifsdocumentsprovider.data.storage.manager

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.wa2c.android.cifsdocumentsprovider.common.utils.getFileName
import com.wa2c.android.cifsdocumentsprovider.common.utils.logE
import com.wa2c.android.cifsdocumentsprovider.common.values.BUFFER_SIZE
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageFile
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.FileNotFoundException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentFileManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Get DocumentFile
     */
    private fun getDocumentFile(uri: Uri): DocumentFile? {
        return if (DocumentsContract.isTreeUri(uri)) {
            DocumentFile.fromTreeUri(context, uri) ?: return null
        } else {
            DocumentFile.fromSingleUri(context, uri) ?: return null
        }
    }

    /**
     * Get StorageFile from DocumentFile URI
     */
    fun getStorageFileAndMimeType(uri: Uri): Pair<StorageFile, String>? {
        val file = getDocumentFile(uri) ?: return null
        return Pair(
            StorageFile(
                name = file.name ?: file.uri.getFileName(context),
                size = file.length(),
                uri = file.uri.toString(),
                isDirectory = file.isDirectory,
            ),
            file.type?.ifEmpty { null } ?: OTHER_MIME_TYPE,
        )
    }

    /**
     * Get output file URI
     */
    fun getPermittedFileUri(outputUri: Uri, outputName: String, mimeType: String): Uri? {
        return getDocumentFile(outputUri)?.let { df ->
            if (df.isDirectory) {
                val file = df.findFile(outputName)
                file?.uri ?: df.createFile(mimeType, outputName)?.uri
                // NOTE : build URI may not have access permission
            } else {
                df.uri
            }
        }
    }

    fun loadFile(uri: String): ByteArray {
        return context.contentResolver.openInputStream(uri.toUri())?.use {
            it.readBytes()
        } ?: throw FileNotFoundException()
    }

    /**
     * True if target exists.
     */
    fun existsFile(uri: Uri, name: String): Boolean {
        return getDocumentFile(uri)?.let { target ->
            if (target.isDirectory) {
                target.findFile(name)
            } else {
                target
            }?.let { file ->
                file.exists() && file.length() > 0
            }
        } ?: false
    }

    fun deleteFile(targetUri: Uri): Boolean {
        return try {
            DocumentsContract.deleteDocument(context.contentResolver, targetUri)
        } catch (e: Exception) {
            logE(e)
            false
        }
    }

    /**
     * Send single file
     * @return False if canceled
     */
    suspend fun sendFile(
        sourceUri: Uri,
        targetUri: Uri,
        bufferSize: Int = BUFFER_SIZE,
        updateProgress: suspend (progressSize: Long) -> Boolean
    ): Boolean {
        val buffer = ByteArray(bufferSize)
        var progressSize = 0L
        (context.contentResolver.openInputStream(sourceUri) ?: return false).use { input ->
            (context.contentResolver.openOutputStream(targetUri)
                ?: return false).use { output ->
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

    companion object {
        private const val OTHER_MIME_TYPE =  "application/octet-stream"
    }

}
