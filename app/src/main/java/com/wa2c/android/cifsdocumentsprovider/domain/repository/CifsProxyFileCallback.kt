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
package com.wa2c.android.cifsdocumentsprovider.domain.repository

import android.os.ProxyFileDescriptorCallback
import android.system.ErrnoException
import android.system.OsConstants
import com.wa2c.android.cifsdocumentsprovider.common.utils.logE
import com.wa2c.android.cifsdocumentsprovider.common.values.AccessMode
import com.wa2c.android.cifsdocumentsprovider.data.BackgroundBufferReader
import com.wa2c.android.cifsdocumentsprovider.data.BackgroundBufferWriter
import jcifs.smb.SmbException
import jcifs.smb.SmbFile
import jcifs.smb.SmbRandomAccessFile
import kotlinx.coroutines.*
import java.io.IOException
import kotlin.coroutines.CoroutineContext

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

    @Throws(ErrnoException::class)
    override fun onGetSize(): Long {
        return fileSeize
    }

    @Throws(ErrnoException::class)
    override fun onRead(offset: Long, size: Int, data: ByteArray): Int {
        try {
            writer?.cancelBuffering()
            val r = reader ?: BackgroundBufferReader(fileSeize) {
                smbFile.openInputStream(SmbFile.FILE_SHARE_READ)
            }.also {
                reader = it
            }
            return r.readBuffer(offset, size, data)
        } catch (e: IOException) {
            throwErrnoException(e)
        }
        return 0
    }

    @Throws(ErrnoException::class)
    override fun onWrite(offset: Long, size: Int, data: ByteArray): Int {
        try {
            if (mode != AccessMode.W) {
                throw SmbException("Writing is not permitted")
            }

            reader?.cancelLoading()
            val w = writer ?: BackgroundBufferWriter(fileSeize) {
                smbFile.openOutputStream(false, SmbFile.FILE_SHARE_WRITE)
            }.also {
                writer = it
            }

            return w.writeBuffer(offset, size, data)
        } catch (e: IOException) {
            throwErrnoException(e)
        }
        return 0
    }

    @Throws(ErrnoException::class)
    override fun onFsync() {
        // Nothing to do
    }

    override fun onRelease() {
        try {
            reader?.cancelLoading()
            writer?.cancelBuffering()
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
