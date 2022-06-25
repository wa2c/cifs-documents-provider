package com.wa2c.android.cifsdocumentsprovider.common.values

/**
 * Server connection result
 */
sealed class ConnectionResult {
    /** Success */
    object Success: ConnectionResult()
    /** Warning */
    data class Warning(
        val cause: Throwable = RuntimeException()
    ): ConnectionResult()
    /** Failure */
    data class Failure(
        val cause: Throwable = RuntimeException()
    ): ConnectionResult()
}