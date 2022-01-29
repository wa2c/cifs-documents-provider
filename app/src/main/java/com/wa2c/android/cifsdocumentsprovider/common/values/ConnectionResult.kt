package com.wa2c.android.cifsdocumentsprovider.common.values

/**
 * Server connection result
 */
sealed class ConnectionResult {
    object Success: ConnectionResult()
    data class Warning(
        val cause: Throwable = RuntimeException()
    ): ConnectionResult()
    data class Failure(
        val cause: Throwable = RuntimeException()
    ): ConnectionResult()
}