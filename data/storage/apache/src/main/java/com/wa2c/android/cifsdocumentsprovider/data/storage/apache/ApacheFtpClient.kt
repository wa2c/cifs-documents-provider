package com.wa2c.android.cifsdocumentsprovider.data.storage.apache

import android.os.ProxyFileDescriptorCallback
import android.util.LruCache
import com.wa2c.android.cifsdocumentsprovider.common.utils.getCause
import com.wa2c.android.cifsdocumentsprovider.common.utils.isDirectoryUri
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.common.utils.logW
import com.wa2c.android.cifsdocumentsprovider.common.utils.rename
import com.wa2c.android.cifsdocumentsprovider.common.values.AccessMode
import com.wa2c.android.cifsdocumentsprovider.common.values.CONNECTION_TIMEOUT
import com.wa2c.android.cifsdocumentsprovider.common.values.ConnectionResult
import com.wa2c.android.cifsdocumentsprovider.common.values.READ_TIMEOUT
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageAccess
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageClient
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageConnection
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.vfs2.FileNotFoundException
import org.apache.commons.vfs2.FileObject
import org.apache.commons.vfs2.FileSystemOptions
import org.apache.commons.vfs2.Selectors
import org.apache.commons.vfs2.VFS
import org.apache.commons.vfs2.auth.StaticUserAuthenticator
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder
import org.apache.commons.vfs2.provider.ftp.FtpFileSystemConfigBuilder
import org.apache.commons.vfs2.provider.ftp.FtpFileType
import java.io.IOException
import java.time.Duration

class ApacheFtpClient constructor(
    private val openFileLimit: Int,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
): StorageClient {

    private val fileManager = VFS.getManager()

    /** Context cache */
    private val contextCache = object : LruCache<StorageConnection, FileSystemOptions>(openFileLimit) {
        override fun entryRemoved(evicted: Boolean, key: StorageConnection?, oldValue: FileSystemOptions?, newValue: FileSystemOptions?) {
            super.entryRemoved(evicted, key, oldValue, newValue)
            logD("Context Removed: $key")
        }
    }

    /**
     * Get context
     */
    private fun getContext(
        connection: StorageConnection,
        ignoreCache: Boolean,
    ): FileSystemOptions {
        if (!ignoreCache) {
            contextCache[connection]?.let {
                return it
            }
        }

        val options = FileSystemOptions()

        DefaultFileSystemConfigBuilder.getInstance().also { builder ->
            builder.setUserAuthenticator(
                options,
                StaticUserAuthenticator(connection.domain, connection.user, connection.password)
            )
        }

        FtpFileSystemConfigBuilder.getInstance().also { builder ->
            builder.setPassiveMode(options, true)
            builder.setSoTimeout(options, Duration.ofMillis(CONNECTION_TIMEOUT.toLong()))
            builder.setConnectTimeout(options,  Duration.ofMillis(CONNECTION_TIMEOUT.toLong()))
            builder.setDataTimeout(options,  Duration.ofMillis(READ_TIMEOUT.toLong()))
            builder.setControlEncoding(options, "UTF-8")
            builder.setFileType(options, FtpFileType.BINARY)
        }

        logD("Context created: $options")
        contextCache.put(connection, options)
        return options
    }

    /**
     * Get file object
     */
    private suspend fun getFileObject(access: StorageAccess, ignoreCache: Boolean = false): FileObject? {
        return withContext(dispatcher) {
            fileManager.resolveFile(access.uri, getContext(access.connection, ignoreCache))
        }
    }

    private suspend fun FileObject.toStorageFile(): StorageFile {
        val urlText = url.toString()
        return withContext(dispatcher) {
            val isDir = urlText.isDirectoryUri || isFolder
            StorageFile(
                name = name.baseName,
                uri = urlText,
                size = if (isDir || !isFile) 0 else content.size,
                lastModified = content.lastModifiedTime,
                isDirectory = isDir,
            )
        }
    }

    override suspend fun checkConnection(access: StorageAccess): ConnectionResult {
        return withContext(dispatcher) {
            try {
                getChildren(access, true) ?: throw IOException()
                ConnectionResult.Success
            } catch (e: Exception) {
                logW(e)
                val c = e.getCause()
                if (e is FileNotFoundException) {
                    // Warning
                    ConnectionResult.Warning(c)
                } else {
                    // Failure
                    ConnectionResult.Failure(c)
                }
            } finally {
                contextCache.remove(access.connection)
            }
        }
    }

    override suspend fun getFile(access: StorageAccess, ignoreCache: Boolean): StorageFile? {
        return withContext(dispatcher) {
            getFileObject(access, ignoreCache)?.toStorageFile()
        }
    }

    override suspend fun getChildren(
        access: StorageAccess,
        ignoreCache: Boolean,
    ): List<StorageFile> {
        return withContext(dispatcher) {
            getFileObject(access, true)?.children
                ?.filter { it.exists() }
                ?.map { it.toStorageFile() } ?: emptyList()
        }
    }

    override suspend fun createFile(access: StorageAccess, mimeType: String?): StorageFile? {
        return withContext(dispatcher) {
            getFileObject(access, true)?.let { fo ->
                fo.createFile()
                fo.toStorageFile()
            }
        }
    }

    override suspend fun copyFile(
        sourceAccess: StorageAccess,
        targetAccess: StorageAccess,
    ): StorageFile? {
        return withContext(dispatcher) {
            val source = getFileObject(targetAccess, true) ?: return@withContext null
            val target = getFileObject(targetAccess, false) ?: return@withContext null
            target.copyFrom(source, Selectors.SELECT_SELF_AND_CHILDREN)
            target.toStorageFile()
        }
    }

    override suspend fun renameFile(access: StorageAccess, newName: String): StorageFile? {
        return withContext(dispatcher) {
            val targetUri = access.uri.rename(newName)
            val source = getFileObject(access, true) ?: return@withContext null
            val target = getFileObject(access.copy(currentUri = targetUri.rename(newName)), true) ?: return@withContext null
            source.moveTo(target)
            target.toStorageFile()
        }
    }

    override suspend fun moveFile(
        sourceAccess: StorageAccess,
        targetAccess: StorageAccess,
    ): StorageFile? {
        return withContext(dispatcher) {
            val source = getFileObject(targetAccess, true) ?: return@withContext null
            val target = getFileObject(targetAccess, false) ?: return@withContext null
            source.moveTo(target)
            target.toStorageFile()
        }
    }

    override suspend fun deleteFile(access: StorageAccess): Boolean {
        return withContext(dispatcher) {
            getFileObject(access, true)?.delete()
            true
        }
    }

    override suspend fun getFileDescriptor(
        access: StorageAccess,
        mode: AccessMode,
        onFileRelease: suspend () -> Unit,
    ): ProxyFileDescriptorCallback? {
        TODO("Not yet implemented")
    }

    override suspend fun close() {
        contextCache.evictAll()
        fileManager.close()
    }


}
