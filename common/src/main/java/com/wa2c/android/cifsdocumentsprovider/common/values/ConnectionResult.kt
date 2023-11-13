package com.wa2c.android.cifsdocumentsprovider.common.values

/**
 * Server connection result
 */
sealed class ConnectionResult {

    abstract val cause: Throwable?

    /** Success */
    data object Success: ConnectionResult() {
        override val cause: Throwable? = null
    }
    /** Warning */
    data class Warning(
        override val cause: Throwable = RuntimeException()
    ): ConnectionResult()
    /** Failure */
    data class Failure(
        override val cause: Throwable = RuntimeException()
    ): ConnectionResult()
}
