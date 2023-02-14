package com.wa2c.android.cifsdocumentsprovider.common

import android.system.ErrnoException
import android.system.OsConstants
import com.wa2c.android.cifsdocumentsprovider.common.utils.logE
import kotlinx.coroutines.runBlocking
import java.io.IOException


/**
 * Proxy Callback process
 */
@Throws(ErrnoException::class)
fun <T> processFileIo(process: () -> T): T {
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