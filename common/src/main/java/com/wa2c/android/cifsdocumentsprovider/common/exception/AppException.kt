package com.wa2c.android.cifsdocumentsprovider.common.exception

sealed class AppException(e: Exception?): RuntimeException(e) {
    sealed class Settings(e: Exception?) : AppException(e) {
        class Empty: Settings(null)
        class Import(e: Exception? = null) : Settings(e)
        class Export(e: Exception? = null): Settings(e)
    }
}
