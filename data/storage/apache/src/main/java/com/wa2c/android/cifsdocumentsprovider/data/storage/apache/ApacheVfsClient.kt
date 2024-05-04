package com.wa2c.android.cifsdocumentsprovider.data.storage.apache

import android.os.ProxyFileDescriptorCallback
import android.util.LruCache
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.JSchUnknownHostKeyException
import com.wa2c.android.cifsdocumentsprovider.common.exception.StorageException
import com.wa2c.android.cifsdocumentsprovider.common.utils.isDirectoryUri
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.common.utils.logE
import com.wa2c.android.cifsdocumentsprovider.common.utils.logW
import com.wa2c.android.cifsdocumentsprovider.common.utils.throwStorageCommonException
import com.wa2c.android.cifsdocumentsprovider.common.values.AccessMode
import com.wa2c.android.cifsdocumentsprovider.common.values.OPEN_FILE_LIMIT_MAX
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageClient
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageConnection
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageFile
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.StorageRequest
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.utils.getCause
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.utils.rename
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.vfs2.FileNotFolderException
import org.apache.commons.vfs2.FileNotFoundException
import org.apache.commons.vfs2.FileObject
import org.apache.commons.vfs2.FileSystemException
import org.apache.commons.vfs2.FileSystemOptions
import org.apache.commons.vfs2.Selectors
import org.apache.commons.vfs2.VFS
import org.apache.commons.vfs2.auth.StaticUserAuthenticator
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder

abstract class ApacheVfsClient(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
): StorageClient {

    private val fileManager = VFS.getManager()

    /** Context cache */
    private val contextCache = object : LruCache<StorageConnection, FileSystemOptions>(OPEN_FILE_LIMIT_MAX) {
        override fun entryRemoved(evicted: Boolean, key: StorageConnection?, oldValue: FileSystemOptions?, newValue: FileSystemOptions?) {
            super.entryRemoved(evicted, key, oldValue, newValue)
            logD("Context Removed: $key")
        }
    }

    /**
     * Apply options
     */
    protected abstract fun applyOptions(
        options: FileSystemOptions,
        storageConnection: StorageConnection,
    )

    /**
     * Get context
     */
    private fun getContext(
        storageConnection: StorageConnection,
        ignoreCache: Boolean,
    ): FileSystemOptions {
        if (!ignoreCache) {
            contextCache[storageConnection]?.let {
                return it
            }
        }

        val options = FileSystemOptions()

        DefaultFileSystemConfigBuilder.getInstance().also { builder ->
            if (!storageConnection.isAnonymous) {
                builder.setUserAuthenticator(
                    options,
                    StaticUserAuthenticator(null, storageConnection.user, storageConnection.password)
                )
            } else {
                builder.setUserAuthenticator(
                    options,
                    StaticUserAuthenticator(null, ANONYMOUS, null)
                )
            }
        }

        applyOptions(options, storageConnection)

        logD("Context created: $options")
        contextCache.put(storageConnection, options)
        return options
    }

    /**
     * Get file object
     */
    private fun getFileObject(
        request: StorageRequest,
        ignoreCache: Boolean = false,
        existsRequired: Boolean = false,
    ): FileObject {
        val connection = request.connection
        return fileManager.resolveFile(request.uri, getContext(connection, ignoreCache)).also {
            if (existsRequired && !it.exists()) throw StorageException.File.NotFound()
        }
    }

    /**
     * Convert to StorageFile
     */
    private fun FileObject.toStorageFile(): StorageFile {
        val urlText = url.toString()
        val isDir = urlText.isDirectoryUri || isFolder
        return StorageFile(
            name = name.baseName,
            uri = urlText,
            size = if (isDir || !isFile) 0 else content.size,
            lastModified = content.lastModifiedTime,
            isDirectory = isDir,
        )
    }

    private suspend fun <T> runHandling(
        request: StorageRequest,
        block: suspend CoroutineScope.() -> T,
    ): T {
        return withContext(dispatcher) {
            try {
                return@withContext block()
            } catch (e: Exception) {
                logE(e)
                val c = e.getCause()
                c.throwStorageCommonException()
                when (c) {
                    is FileNotFoundException,
                    is FileNotFolderException -> {
                        throw StorageException.File.NotFound(c)
                    }
                    is JSchUnknownHostKeyException -> {
                        throw StorageException.Security.UnknownHost(c, request.connection.id)
                    }
                    is JSchException -> {
                        throw StorageException.Security.Auth(c, request.connection.id)
                    }
                    is FileSystemException -> {
                        if (c.code == "vfs.provider.ftp/login.error") {
                            throw StorageException.Security.Auth(c, request.connection.id)
                        } else if (c.code == "vfs.provider.sftp/connect.error") {
                            throw StorageException.Transaction.HostNotFound(c)
                        }
                    }
                }
                throw StorageException.Error(e)
            }
        }
    }

    override suspend fun getFile(
        request: StorageRequest,
        ignoreCache: Boolean,
    ): StorageFile {
        return runHandling(request) {
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
        return runHandling(request) {
            getFileObject(request, ignoreCache = ignoreCache, existsRequired = true).use { file ->
                file.children.filter { it.exists() }.map { it.toStorageFile() }
            }
        }
    }

    override suspend fun createDirectory(
        request: StorageRequest,
    ): StorageFile {
        return runHandling(request) {
            getFileObject(request).use { file ->
                file.createFolder()
                file.toStorageFile()
            }
        }
    }

    override suspend fun createFile(
        request: StorageRequest,
    ): StorageFile {
        return runHandling(request) {
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
        return runHandling(sourceRequest) {
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
        return runHandling(request) {
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
        return runHandling(sourceRequest) {
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
        return runHandling(request) {
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

    override suspend fun getProxyFileDescriptorCallback(
        request: StorageRequest,
        mode: AccessMode,
        onFileRelease: suspend () -> Unit,
    ): ProxyFileDescriptorCallback {
        return runHandling(request) {
            val file = getFileObject(request, existsRequired = true).takeIf { it.isFile } ?: throw StorageException.File.NotFound()
            val release: suspend () -> Unit = {
                try { file.close() } catch (e: Exception) { logE(e) }
                onFileRelease()
            }
            ApacheProxyFileCallback(file, mode, release)
        }
    }

    override suspend fun removeCache(request: StorageRequest?): Boolean {
        return if (request == null) {
            contextCache.evictAll()
            true
        } else {
            contextCache.remove(request.connection) != null
        }
    }

    override suspend fun close() {
        removeCache()
        fileManager.close()
    }

    companion object {
        const val ANONYMOUS = "anonymous"
    }

}
