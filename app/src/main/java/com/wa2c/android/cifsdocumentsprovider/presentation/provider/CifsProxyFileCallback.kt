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
package com.wa2c.android.cifsdocumentsprovider.presentation.provider

import android.os.ProxyFileDescriptorCallback
import android.system.ErrnoException
import android.system.OsConstants
import com.wa2c.android.cifsdocumentsprovider.common.utils.logE
import jcifs.smb.SmbFile
import jcifs.smb.SmbRandomAccessFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.math.min

/**
 * CIFS Proxy File Callback
 */
class CifsProxyFileCallback(
    private val smbFile: SmbFile,
    private val mode: String
) : ProxyFileDescriptorCallback() {

    private var isAccessOpened = false
    private val access: SmbRandomAccessFile by lazy {
        runBlocking { withContext(Dispatchers.IO) {
            smbFile.openRandomAccess(mode).also {
                isAccessOpened = true
            }
        } }
    }

    @Throws(ErrnoException::class)
    override fun onGetSize(): Long {
        try {
            return access.length()
        } catch (e: IOException) {
            throwErrnoException(e)
        }
        return 0
    }

    @Throws(ErrnoException::class)
    override fun onRead(offset: Long, size: Int, data: ByteArray): Int {
        try {
            access.seek(offset)
            return access.read(data, 0, size)
        } catch (e: IOException) {
            throwErrnoException(e)
        }
        return 0
    }

    @Throws(ErrnoException::class)
    override fun onWrite(offset: Long, size: Int, data: ByteArray): Int {
        try {
            val maxSize = (data.size - offset).toInt().also { if (it <= 0) return 0 }
            access.seek(offset)
            access.write(data, 0, size)
            return min(size, maxSize)
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
            if (isAccessOpened) access.close()
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
