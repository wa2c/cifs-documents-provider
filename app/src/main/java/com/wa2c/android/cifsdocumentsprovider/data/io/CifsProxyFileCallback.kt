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
package com.wa2c.android.cifsdocumentsprovider.data.io

import android.os.ProxyFileDescriptorCallback
import android.system.ErrnoException
import android.system.OsConstants
import com.wa2c.android.cifsdocumentsprovider.common.utils.logD
import com.wa2c.android.cifsdocumentsprovider.common.utils.logE
import com.wa2c.android.cifsdocumentsprovider.common.values.AccessMode
import jcifs.smb.SmbException
import jcifs.smb.SmbFile
import jcifs.smb.SmbRandomAccessFile
import kotlinx.coroutines.runBlocking
import java.io.IOException

/**
 * CIFS Proxy File Callback
 */
class CifsProxyFileCallback(
    private val smbFile: SmbFile,
    private val mode: AccessMode
) : ProxyFileDescriptorCallback() {

    /**
     * File size
     */
    private val fileSeize: Long by lazy {
        runBlocking {
            try {
                runBlocking { smbFile.length() }
            } catch (e: IOException) {
                throwErrnoException(e)
                0
            }
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

        return reader
            ?: BackgroundBufferReader(fileSeize) { start, array, off, len ->
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

        return writer
            ?: BackgroundBufferWriter { start, array, off, len ->
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
        try {
            return getReader().readBuffer(offset, size, data)
        } catch (e: IOException) {
            throwErrnoException(e)
        }
        return 0
    }

    @Throws(ErrnoException::class)
    override fun onWrite(offset: Long, size: Int, data: ByteArray): Int {
        try {
            if (mode != AccessMode.W) { throw SmbException("Writing is not permitted") }
            return getWriter().writeBuffer(offset, size, data)
        } catch (e: IOException) {
            throwErrnoException(e)
        }
        return 0
    }

    @Throws(ErrnoException::class)
    override fun onFsync() {
        // Nothing to do
    }

    @Throws(ErrnoException::class)
    override fun onRelease() {
        logD("onRelease")
        try {
            reader?.close()
            writer?.close()
            outputAccess?.close()
            smbFile.close()
        } catch (e: IOException) {
            throwErrnoException(e)
        }
    }

    @Throws(ErrnoException::class)
    private fun throwErrnoException(e: IOException) {
        logE(e)

        // Hack around that SambaProxyFileCallback throws ErrnoException rather than IOException
        // assuming the underlying cause is an ErrnoException.
        if (e.cause is ErrnoException) {
            throw (e.cause as ErrnoException?)!!
        } else {
            throw ErrnoException("I/O", OsConstants.EIO, e)
        }
    }

}
