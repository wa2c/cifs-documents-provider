package com.wa2c.android.cifsdocumentsprovider.common.utils

import timber.log.Timber

/**
 * Initialize log
 */
fun initLog(isDebug: Boolean) {
    // Set logger
    if (isDebug) {
        Timber.plant(Timber.DebugTree())
    }
}

/** Output the verbose message */
fun logV(obj: Any?, vararg args: Any?) = run {
    if (obj is Throwable) { Timber.asTree().v(obj) }
    Timber.asTree().v(obj.toString(), *args)
}
/** Output the debug message */
fun logD(obj: Any?, vararg args: Any?) = run {
    if (obj is Throwable) { Timber.asTree().d(obj) }
    Timber.asTree().d(obj.toString(), *args)
}
/** Output the info message */
fun logI(obj: Any?, vararg args: Any?) = run {
    if (obj is Throwable) { Timber.asTree().i(obj) }
    Timber.asTree().i(obj.toString(), *args)
}
/** Output the warning message */
fun logW(obj: Any?, vararg args: Any?) = run {
    if (obj is Throwable) { Timber.asTree().w(obj) }
    Timber.asTree().w(obj.toString(), *args)
}
/** Output the error message */
fun logE(obj: Any?, vararg args: Any?) = run {
    if (obj is Throwable) { Timber.asTree().e(obj) }
    Timber.asTree().e(obj.toString(), *args)
}
