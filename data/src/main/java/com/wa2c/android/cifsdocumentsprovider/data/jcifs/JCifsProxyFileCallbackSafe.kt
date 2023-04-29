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
import com.wa2c.android.cifsdocumentsprovider.common.values.AccessMode
import jcifs.smb.SmbFile
import jcifs.smb.SmbRandomAccessFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

/**
 * Proxy File Callback for jCIFS-ng (Normal IO)
 */
internal class JCifsProxyFileCallbackSafe(
    private val smbFile: SmbFile,
    private val mode: AccessMode
) : ProxyFileDescriptorCallback(), CoroutineScope {

    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    private var isAccessOpened = false
    private val access: SmbRandomAccessFile by lazy {
        processFileIo {
            smbFile.openRandomAccess(mode.smbMode).also {
                isAccessOpened = true
            }
        }
    }

    /**
     * File size
     */
    private val fileSeize: Long by lazy {
        processFileIo {
            access.length()
        }
    }

    @Throws(ErrnoException::class)
    override fun onGetSize(): Long {
        return fileSeize
    }

    @Throws(ErrnoException::class)
    override fun onRead(offset: Long, size: Int, data: ByteArray): Int {
        return processFileIo {
            access.seek(offset)
            access.read(data, 0, size)
        }
    }

    @Throws(ErrnoException::class)
    override fun onWrite(offset: Long, size: Int, data: ByteArray): Int {
        if (mode != AccessMode.W) { throw ErrnoException("Writing is not permitted", OsConstants.EBADF) }
        return processFileIo {
            access.seek(offset)
            access.write(data, 0, size)
            size
        }
    }

    @Throws(ErrnoException::class)
    override fun onFsync() {
        // Nothing to do
    }

    override fun onRelease() {
        processFileIo {
            if (isAccessOpened) access.close()
            smbFile.close()
            job.complete()
        }
    }

}
