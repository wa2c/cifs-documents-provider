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
package com.wa2c.android.cifsdocumentsprovider.data.jcifs

import android.os.ProxyFileDescriptorCallback
import android.system.ErrnoException
import android.system.OsConstants
import com.wa2c.android.cifsdocumentsprovider.common.processFileIo
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.common.values.AccessMode
import com.wa2c.android.cifsdocumentsprovider.data.io.BackgroundBufferReader
import com.wa2c.android.cifsdocumentsprovider.data.io.BackgroundBufferWriter
import jcifs.smb.SmbFile
import jcifs.smb.SmbRandomAccessFile

/**
 * Proxy File Callback for jCIFS-ng (Buffering IO)
 */
internal class JCifsProxyFileCallback(
    private val smbFile: SmbFile,
    private val mode: AccessMode
) : ProxyFileDescriptorCallback() {

    /**
     * File size
     */
    private val fileSeize: Long by lazy {
        processFileIo {
            smbFile.length()
        }
    }

    private var reader: BackgroundBufferReader? = null

    private var writer: BackgroundBufferWriter? = null

    private var outputAccess: SmbRandomAccessFile? = null

    private fun getReader(): BackgroundBufferReader {
        writer?.let {
            outputAccess?.close()
            outputAccess = null
            it.close()
            writer = null
            logD("Writer released")
        }

        return reader ?: BackgroundBufferReader(fileSeize) { start, array, off, len ->
            smbFile.openRandomAccess(mode.smbMode, SmbFile.FILE_SHARE_READ).use { access ->
                access.seek(start)
                access.read(array, off, len)
            }
        }.also {
            reader = it
            logD("Reader created")
        }
    }

    private fun getWriter(): BackgroundBufferWriter {
        reader?.let {
            it.close()
            reader = null
            logD("Reader released")
        }

        return writer ?: BackgroundBufferWriter { start, array, off, len ->
            (outputAccess ?: smbFile.openRandomAccess(mode.smbMode, SmbFile.FILE_SHARE_WRITE).also { outputAccess = it }).let { access ->
                access.seek(start)
                access.write(array, off, len)
            }
        }.also {
            writer = it
            logD("Writer created")
        }
    }

    @Throws(ErrnoException::class)
    override fun onGetSize(): Long {
        return fileSeize
    }

    @Throws(ErrnoException::class)
    override fun onRead(offset: Long, size: Int, data: ByteArray): Int {
        return processFileIo {
            getReader().readBuffer(offset, size, data)
        }
    }

    @Throws(ErrnoException::class)
    override fun onWrite(offset: Long, size: Int, data: ByteArray): Int {
        if (mode != AccessMode.W) { throw ErrnoException("Writing is not permitted", OsConstants.EBADF) }
        return processFileIo {
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
        processFileIo {
            reader?.close()
            writer?.close()
            outputAccess?.close()
            smbFile.close()
            0
        }
    }

}
