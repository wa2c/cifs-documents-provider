package com.wa2c.android.cifsdocumentsprovider

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

/**
 * Crashlytics tree
 */
class CrashlyticsTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?,message: String, t: Throwable?) {
        if (priority < Log.WARN)
            return

        FirebaseCrashlytics.getInstance().let { crashlytics ->
            crashlytics.setCustomKey(CRASHLYTICS_KEY_PRIORITY, priority)
            crashlytics.setCustomKey(CRASHLYTICS_KEY_TAG, tag ?: "")
            crashlytics.setCustomKey(CRASHLYTICS_KEY_MESSAGE, message)
            crashlytics.recordException(t ?: Exception(message))
        }
    }

    companion object {
        private const val CRASHLYTICS_KEY_PRIORITY = "priority"
        private const val CRASHLYTICS_KEY_TAG = "tag"
        private const val CRASHLYTICS_KEY_MESSAGE = "message"
    }
}
