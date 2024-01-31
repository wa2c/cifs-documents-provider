package com.wa2c.android.cifsdocumentsprovider.data.storage.apache

import android.os.ProxyFileDescriptorCallback
import android.util.LruCache
import com.wa2c.android.cifsdocumentsprovider.common.exception.StorageException
import com.wa2c.android.cifsdocumentsprovider.common.utils.isDirectoryUri
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.common.utils.logE
import com.wa2c.android.cifsdocumentsprovider.common.utils.logW
import com.wa2c.android.cifsdocumentsprovider.common.values.AccessMode
import com.wa2c.android.cifsdocumentsprovider.common.values.CONNECTION_TIMEOUT
import com.wa2c.android.cifsdocumentsprovider.common.values.ConnectionResult
import com.wa2c.android.cifsdocumentsprovider.common.values.READ_TIMEOUT
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageClient
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageConnection
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageFile
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageRequest
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.utils.getCause
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.utils.rename
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.vfs2.FileNotFolderException
import org.apache.commons.vfs2.FileNotFoundException
import org.apache.commons.vfs2.FileObject
import org.apache.commons.vfs2.FileSystemOptions
import org.apache.commons.vfs2.Selectors
import org.apache.commons.vfs2.VFS
import org.apache.commons.vfs2.auth.StaticUserAuthenticator
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder
import org.apache.commons.vfs2.provider.ftp.FtpFileSystemConfigBuilder
import org.apache.commons.vfs2.provider.ftp.FtpFileType
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder
import java.time.Duration

class ApacheFtpClient(
    private val isFtps: Boolean,
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
        ftpConnection: StorageConnection.Ftp,
        ignoreCache: Boolean,
    ): FileSystemOptions {
        if (!ignoreCache) {
            contextCache[ftpConnection]?.let {
                return it
            }
        }

        val options = FileSystemOptions()

        DefaultFileSystemConfigBuilder.getInstance().also { builder ->
            if (!ftpConnection.isAnonymous) {
                builder.setUserAuthenticator(
                    options,
                    StaticUserAuthenticator(null, ftpConnection.user, ftpConnection.password)
                )
            } else {
                builder.setUserAuthenticator(
                    options,
                    StaticUserAuthenticator(null, ANONYMOUS, null)
                )
            }
        }

        // FTP settings
        FtpFileSystemConfigBuilder.getInstance().also { builder ->
            builder.setPassiveMode(options, !ftpConnection.isActiveMode)
            builder.setSoTimeout(options, Duration.ofMillis(CONNECTION_TIMEOUT.toLong()))
            builder.setConnectTimeout(options, Duration.ofMillis(CONNECTION_TIMEOUT.toLong()))
            builder.setDataTimeout(options, Duration.ofMillis(READ_TIMEOUT.toLong()))
            builder.setFileType(options, FtpFileType.BINARY)
            builder.setControlEncoding(options, ftpConnection.encoding)
        }

        // FTPS settings
        if (isFtps) {
            SftpFileSystemConfigBuilder.getInstance().also { builder ->
                builder.setConnectTimeout(options, Duration.ofMillis(CONNECTION_TIMEOUT.toLong()))
                builder.setStrictHostKeyChecking(options, "no")
                builder.setPreferredAuthentications(options, "password")
                builder.setUserDirIsRoot(options, true)
                builder.setFileNameEncoding(options, ftpConnection.encoding)
            }
        }

        logD("Context created: $options")
        contextCache.put(ftpConnection, options)
        return options
    }

    /**
     * Get file object
     */
    private suspend fun getFileObject(
        request: StorageRequest,
        ignoreCache: Boolean = false,
        existsRequired: Boolean = false,
    ): FileObject {
        return withContext(dispatcher) {
            val connection = request.connection as StorageConnection.Ftp
            fileManager.resolveFile(request.uri, getContext(connection, ignoreCache)).also {
                if (existsRequired && !it.exists()) throw StorageException.FileNotFoundException()
            }
        }
    }

    /**
     * Convert to StorageFile
     */
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

    override suspend fun checkConnection(
        request: StorageRequest,
    ): ConnectionResult {
        return withContext(dispatcher) {
            try {
                getChildren(request, ignoreCache = true)
                ConnectionResult.Success
            } catch (e: Exception) {
                logW(e)
                val c = e.getCause()
                when (c) {
                    is FileNotFoundException,
                    is FileNotFolderException,
                    is StorageException -> {
                        // Warning
                        ConnectionResult.Warning(c)
                    }
                    else -> {
                        // Failure
                        ConnectionResult.Failure(c)
                    }
                }
            } finally {
                contextCache.remove(request.connection)
            }
        }
    }

    override suspend fun getFile(
        request: StorageRequest,
        ignoreCache: Boolean,
    ): StorageFile {
        return withContext(dispatcher) {
            if (request.isRoot || request.isShareRoot) {
                StorageFile(
                    name = request.connection.name,
                    request.connection.uri,
                    0,
                    0,
                    true,
                )
            } else {
                getFileObject(request, ignoreCache = ignoreCache, existsRequired = true).use { file ->
                    file.toStorageFile()
                }
            }
        }
    }

    override suspend fun getChildren(
        request: StorageRequest,
        ignoreCache: Boolean,
    ): List<StorageFile> {
        return withContext(dispatcher) {
            getFileObject(request, ignoreCache = ignoreCache, existsRequired = true).use { file ->
                file.children.filter { it.exists() }.map { it.toStorageFile() }
            }
        }
    }

    override suspend fun createDirectory(
        request: StorageRequest,
    ): StorageFile {
        return withContext(dispatcher) {
            getFileObject(request).use { file ->
                file.createFolder()
                file.toStorageFile()
            }
        }
    }

    override suspend fun createFile(
        request: StorageRequest,
    ): StorageFile {
        return withContext(dispatcher) {
            getFileObject(request).use { file ->
                file.createFile()
                file.toStorageFile()
            }
        }
    }

    override suspend fun copyFile(
        sourceRequest: StorageRequest,
        targetRequest: StorageRequest,
    ): StorageFile {
        return withContext(dispatcher) {
            getFileObject(sourceRequest, existsRequired = true).use { source ->
                getFileObject(targetRequest).use { target ->
                    target.copyFrom(source, Selectors.SELECT_SELF_AND_CHILDREN)
                    target.toStorageFile()
                }
            }
        }
    }

    override suspend fun renameFile(
        request: StorageRequest,
        newName: String,
    ): StorageFile {
        return withContext(dispatcher) {
            getFileObject(request, existsRequired = true).use { source ->
                val targetUri = request.uri.rename(newName)
                getFileObject(request.replacePathByUri(targetUri)).use { target ->
                    source.moveTo(target)
                    target.toStorageFile()
                }
            }
        }
    }

    override suspend fun moveFile(
        sourceRequest: StorageRequest,
        targetRequest: StorageRequest,
    ): StorageFile {
        return withContext(dispatcher) {
            getFileObject(sourceRequest, existsRequired = true).use { source ->
                getFileObject(targetRequest).use { target ->
                    source.moveTo(target)
                    target.toStorageFile()
                }
            }
        }
    }

    override suspend fun deleteFile(
        request: StorageRequest,
    ): Boolean {
        return withContext(dispatcher) {
            try {
                getFileObject(request, existsRequired = true).use { file ->
                    file.delete()
                }
                true
            } catch (e: Exception) {
                logW(e)
                false
            }
        }
    }

    override suspend fun getFileDescriptor(
        request: StorageRequest,
        mode: AccessMode,
        onFileRelease: suspend () -> Unit,
    ): ProxyFileDescriptorCallback {
        return withContext(dispatcher) {
            val file = getFileObject(request, existsRequired = true).takeIf { it.isFile } ?: throw StorageException.FileNotFoundException()
            val release: suspend () -> Unit = {
                try { file.close() } catch (e: Exception) { logE(e) }
                onFileRelease()
            }
            ApacheFtpProxyFileCallback(file, mode, release)
        }
    }

    override suspend fun close() {
        contextCache.evictAll()
        fileManager.close()
    }

    companion object {
        private const val ANONYMOUS = "anonymous"
    }

}
