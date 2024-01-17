/*
 * Copyright 2017 Google Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.wa2c.android.cifsdocumentsprovider.data.storage.jcifsng

import android.os.ProxyFileDescriptorCallback
import android.system.ErrnoException
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.common.utils.logE
import com.wa2c.android.cifsdocumentsprovider.common.values.AccessMode
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.utils.BackgroundBufferReader
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.utils.BackgroundBufferWriter
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.utils.checkAccessMode
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.utils.processFileIo
import jcifs.smb.SmbFile
import jcifs.smb.SmbRandomAccessFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

/**
 * Proxy File Callback for jCIFS-ng (Buffering IO)
 */
internal class JCifsNgProxyFileCallback(
    private val smbFile: SmbFile,
    private val accessMode: AccessMode,
    private val onFileRelease: suspend () -> Unit,
) : ProxyFileDescriptorCallback(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.IO + Job()

    /** File size */
    private val fileSize: Long by lazy {
        processFileIo(coroutineContext) { smbFile.length() }
    }

    private var reader: BackgroundBufferReader? = null

    private var writer: BackgroundBufferWriter? = null

    private var outputAccess: SmbRandomAccessFile? = null

    private fun getReader(): BackgroundBufferReader {
        return processFileIo(coroutineContext) {
            writer?.let {
                outputAccess?.close()
                outputAccess = null
                it.close()
                writer = null
                logD("Writer released")
            }

            reader ?: BackgroundBufferReader(fileSize) { start, array, off, len ->
                smbFile.openRandomAccess(accessMode.smbMode, SmbFile.FILE_SHARE_READ).use { access ->
                    access.seek(start)
                    access.read(array, off, len)
                }
            }.also {
                reader = it
                logD("Reader created")
            }
        }
    }

    private fun getWriter(): BackgroundBufferWriter {
        return processFileIo(coroutineContext) {
            reader?.let {
                it.close()
                reader = null
                logD("Reader released")
            }

            writer ?: BackgroundBufferWriter { start, array, off, len ->
                (outputAccess ?: smbFile.openRandomAccess(accessMode.smbMode, SmbFile.FILE_SHARE_WRITE).also { outputAccess = it }).let { access ->
                    access.seek(start)
                    access.write(array, off, len)
                }
            }.also {
                writer = it
                logD("Writer created")
            }
        }
    }

    @Throws(ErrnoException::class)
    override fun onGetSize(): Long {
        return fileSize
    }

    @Throws(ErrnoException::class)
    override fun onRead(offset: Long, size: Int, data: ByteArray): Int {
        return processFileIo(coroutineContext) {
            getReader().readBuffer(offset, size, data)
        }
    }

    @Throws(ErrnoException::class)
    override fun onWrite(offset: Long, size: Int, data: ByteArray): Int {
        return processFileIo(coroutineContext) {
            checkAccessMode(accessMode)
            getWriter().writeBuffer(offset, size, data)
        }
    }

    @Throws(ErrnoException::class)
    override fun onFsync() {
        // Nothing to do
    }

    @Throws(ErrnoException::class)
    override fun onRelease() {
        logD("onRelease")
        processFileIo(coroutineContext) {
            reader?.close()
            writer?.close()
            try {
                outputAccess?.close()
            } catch (e: Exception) {
                logE(e)
            }
            onFileRelease()
        }
    }

}
