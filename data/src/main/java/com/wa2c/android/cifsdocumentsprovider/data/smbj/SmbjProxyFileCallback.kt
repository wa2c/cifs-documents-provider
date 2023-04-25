package com.wa2c.android.cifsdocumentsprovider.data.smbj

import android.os.ProxyFileDescriptorCallback
import android.system.ErrnoException
import android.system.OsConstants
import com.hierynomus.smbj.share.File
import com.wa2c.android.cifsdocumentsprovider.common.processFileIo
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.common.values.AccessMode
import com.wa2c.android.cifsdocumentsprovider.data.io.BackgroundBufferReader

/**
 * Proxy File Callback for SMBJ
 */
class SmbjProxyFileCallback(
    private val file: File,
    private val mode: AccessMode,
) : ProxyFileDescriptorCallback() {

    private var reader: BackgroundBufferReader? = null

    private fun getReader(): BackgroundBufferReader {
        return reader ?: BackgroundBufferReader(onGetSize()) { start, array, off, len ->
            file.inputStream.use {
                it.skip(start)
                it.read(array, off, len)
            }
        }.also {
            reader = it
            logD("Reader created")
        }
    }

    override fun onGetSize(): Long {
        return file.fileInformation.standardInformation.endOfFile
    }

    override fun onRead(offset: Long, size: Int, data: ByteArray): Int {
        return processFileIo {
            //getReader().readBuffer(offset, size, data)
            file.read(data, offset, 0, size)
        }
    }

    override fun onWrite(offset: Long, size: Int, data: ByteArray): Int {
        if (mode != AccessMode.W) { throw ErrnoException("Writing is not permitted", OsConstants.EBADF) }
        return processFileIo {
            //file. write(data, offset, 0, size)
            file.writeAsync(data, offset, 0, size)
            size
        }
    }
    
    @Throws(ErrnoException::class)
    override fun onFsync() {
        // Nothing to do
    }

    override fun onRelease() {
        processFileIo {
            file.close()
            file.diskShare.close()
            file.diskShare.treeConnect.session.close()
            0
        }
    }
}