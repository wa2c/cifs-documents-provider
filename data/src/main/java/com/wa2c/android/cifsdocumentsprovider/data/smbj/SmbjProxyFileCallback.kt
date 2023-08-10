package com.wa2c.android.cifsdocumentsprovider.data.smbj

import android.os.Build
import android.os.ProxyFileDescriptorCallback
import android.system.ErrnoException
import android.system.OsConstants
import com.hierynomus.smbj.share.File
import com.wa2c.android.cifsdocumentsprovider.common.processFileIo
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.common.utils.logE
import com.wa2c.android.cifsdocumentsprovider.common.values.AccessMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import java.io.BufferedInputStream
import kotlin.coroutines.CoroutineContext

/**
 * Proxy File Callback for SMBJ (Buffering IO)
 */
class SmbjProxyFileCallback(
    private val file: File,
    private val mode: AccessMode,
    private val onFileRelease: () -> Unit,
) : ProxyFileDescriptorCallback(), CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + Job()

    /** File size */
    private val fileSize: Long by lazy { file.fileInformation.standardInformation.endOfFile }

    private var position: Long = 0
    private var bufferedInputStream: BufferedInputStream? = null

    /**
     * Get BufferedInputStream
     */
    private fun getInputStream(current: Long): BufferedInputStream {
        return bufferedInputStream?.let { stream ->
            if (position == current) {
                stream
            } else {
                try { stream.close() } catch (e: Exception) { logE(e) }
                null
            }
        } ?: let {
            BufferedInputStream(file.inputStream, READ_BUFFER_SIZE).also {
                it.skip(current)
                position = current
                bufferedInputStream = it
            }
        }
    }

    @Throws(ErrnoException::class)
    override fun onGetSize(): Long {
        return fileSize
    }

    @Throws(ErrnoException::class)
    override fun onRead(offset: Long, size: Int, data: ByteArray): Int {
        return processFileIo {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getInputStream(offset).readNBytes(data, 0, size).also {
                    position += it
                }
            } else {
                getInputStream(offset).read(data, 0, size).also {
                    position += it
                }
            }
        }
    }

    @Throws(ErrnoException::class)
    override fun onWrite(offset: Long, size: Int, data: ByteArray): Int {
        if (mode != AccessMode.W) { throw ErrnoException("Writing is not permitted", OsConstants.EBADF) }
        return processFileIo {
            file.writeAsync(data, offset, 0, size)
            size
        }
    }
    
    @Throws(ErrnoException::class)
    override fun onFsync() {
        // Nothing to do
    }

    @Throws(ErrnoException::class)
    override fun onRelease() {
        logD("onRelease: ${file.uncPath}")
        processFileIo {
            logD("release begin")
            try { bufferedInputStream?.close() } catch (e: Exception) { logE(e) }
            onFileRelease()
            logD("release end")
        }
    }

    companion object {
        const val READ_BUFFER_SIZE = 8 * 1024 * 1024
    }
}