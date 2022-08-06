package com.wa2c.android.cifsdocumentsprovider.data.io

import android.os.ProxyFileDescriptorCallback
import android.system.ErrnoException
import android.system.OsConstants
import com.hierynomus.smbj.share.File
import com.wa2c.android.cifsdocumentsprovider.common.utils.logE
import com.wa2c.android.cifsdocumentsprovider.common.values.AccessMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import java.io.IOException
import kotlin.coroutines.CoroutineContext

class SmbjProxyFileCallback(
    private val file: File,
    private val mode: AccessMode,
) : ProxyFileDescriptorCallback(), CoroutineScope {

    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    override fun onGetSize(): Long {
        return file.fileInformation.standardInformation.endOfFile
    }

    override fun onRead(offset: Long, size: Int, data: ByteArray): Int {
        return processFileIo {
            file.read(data, offset, 0, size)
        }
    }

    override fun onWrite(offset: Long, size: Int, data: ByteArray): Int {
        return processFileIo {
            file.write(data, offset, 0, size)
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

    @Throws(ErrnoException::class)
    private fun processFileIo(process: () -> Int): Int {
        return try {
            runBlocking { process() }
        } catch (e: IOException) {
            logE(e)
            if (e.cause is ErrnoException) {
                throw (e.cause as ErrnoException)
            } else {
                throw ErrnoException("I/O", OsConstants.EIO, e)
            }
        }
    }
}