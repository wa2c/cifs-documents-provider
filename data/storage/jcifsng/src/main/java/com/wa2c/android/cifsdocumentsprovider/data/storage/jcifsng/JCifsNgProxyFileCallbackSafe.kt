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
import com.wa2c.android.cifsdocumentsprovider.common.values.AccessMode
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.utils.checkAccessMode
import com.wa2c.android.cifsdocumentsprovider.data.storage.interfaces.utils.processFileIo
import jcifs.smb.SmbFile
import jcifs.smb.SmbRandomAccessFile
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

/**
 * Proxy File Callback for jCIFS-ng (Normal IO)
 */
internal class JCifsNgProxyFileCallbackSafe(
    private val smbFile: SmbFile,
    private val accessMode: AccessMode,
    private val onFileRelease: suspend () -> Unit,
) : ProxyFileDescriptorCallback(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.IO + Job()

    /** File size */
    private val fileSize: Long by lazy {
        processFileIo(coroutineContext) { access.length() }
    }

    private val accessLazy: Lazy<SmbRandomAccessFile> = lazy {
        processFileIo(coroutineContext) {
            smbFile.openRandomAccess(accessMode.smbMode)
        }
    }

    private val access: SmbRandomAccessFile get() = accessLazy.value

    @Throws(ErrnoException::class)
    override fun onGetSize(): Long {
        return fileSize
    }

    @Throws(ErrnoException::class)
    override fun onRead(offset: Long, size: Int, data: ByteArray): Int {
        return processFileIo(coroutineContext) {
            access.seek(offset)
            // if End-Of-File (-1) then return 0 bytes read
            maxOf(0, access.read(data, 0, size))
        }
    }

    @Throws(ErrnoException::class)
    override fun onWrite(offset: Long, size: Int, data: ByteArray): Int {
        return processFileIo(coroutineContext) {
            checkAccessMode(accessMode)
            access.seek(offset)
            access.write(data, 0, size)
            size
        }
    }

    @Throws(ErrnoException::class)
    override fun onFsync() {
        // Nothing to do
    }

    @Throws(ErrnoException::class)
    override fun onRelease() {
        processFileIo(coroutineContext) {
            if (accessLazy.isInitialized()) access.close()
            onFileRelease()
        }
    }

}
