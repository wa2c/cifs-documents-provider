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
import jcifs.smb.SmbFile
import jcifs.smb.SmbRandomAccessFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import java.io.IOException
import kotlin.coroutines.CoroutineContext

/**
 * CIFS Proxy File Callback
 */
class CifsProxyFileCallback(
    private val smbFile: SmbFile,
    private val mode: AccessMode
) : ProxyFileDescriptorCallback(), CoroutineScope {

    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    private var isAccessOpened = false
    private val access: SmbRandomAccessFile by lazy {
        runBlocking {
            smbFile.openRandomAccess(mode.smbMode).also {
                isAccessOpened = true
            }
        }
    }

    @Throws(ErrnoException::class)
    override fun onGetSize(): Long {
        try {
            return runBlocking { access.length() }
        } catch (e: IOException) {
            throwErrnoException(e)
        }
        return 0
    }

    @Throws(ErrnoException::class)
    override fun onRead(offset: Long, size: Int, data: ByteArray): Int {
        try {
            return runBlocking {
                access.seek(offset)
                access.read(data, 0, size)
            }
        } catch (e: IOException) {
            throwErrnoException(e)
        }
        return 0
    }

    @Throws(ErrnoException::class)
    override fun onWrite(offset: Long, size: Int, data: ByteArray): Int {
        try {
            return runBlocking {
                access.seek(offset)
                access.write(data, 0, size)
                size
            }
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
            job.complete()
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
