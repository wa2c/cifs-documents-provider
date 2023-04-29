package com.wa2c.android.cifsdocumentsprovider.data.smbj

import android.os.ProxyFileDescriptorCallback
import android.system.ErrnoException
import android.system.OsConstants
import com.hierynomus.smbj.share.File
import com.wa2c.android.cifsdocumentsprovider.common.processFileIo
import com.wa2c.android.cifsdocumentsprovider.common.values.AccessMode

/**
 * Proxy File Callback for SMBJ (Normal IO)
 */
class SmbjProxyFileCallbackSafe(
    private val file: File,
    private val mode: AccessMode,
) : ProxyFileDescriptorCallback() {

    override fun onGetSize(): Long {
        return file.fileInformation.standardInformation.endOfFile
    }

    override fun onRead(offset: Long, size: Int, data: ByteArray): Int {
        return processFileIo {
            file.read(data, offset, 0, size)
        }
    }

    override fun onWrite(offset: Long, size: Int, data: ByteArray): Int {
        if (mode != AccessMode.W) { throw ErrnoException("Writing is not permitted", OsConstants.EBADF) }
        return processFileIo {
            //file.write(data, offset, 0, size)
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