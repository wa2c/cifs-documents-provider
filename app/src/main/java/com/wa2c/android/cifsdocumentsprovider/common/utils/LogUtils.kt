package com.wa2c.android.cifsdocumentsprovider.common.utils

import timber.log.Timber

/** Log a verbose message */
fun logV(message: Any?, vararg args: Any?) = Timber.asTree().v(message.toString(), *args)
/** Log a debug message */
fun logD(message: Any?, vararg args: Any?) = Timber.asTree().d(message.toString(), *args)
/** Log an info message */
fun logI(message: Any?, vararg args: Any?) = Timber.asTree().i(message.toString(), *args)
/** Log a warning message */
fun logW(message: Any?, vararg args: Any?) = Timber.asTree().w(message.toString(), *args)
/** Log an error message */
fun logE(message: Any?, vararg args: Any?) = fun() {
    if (message is Throwable) { Timber.asTree().e(message) }
    Timber.asTree().e(message.toString(), *args)
}
