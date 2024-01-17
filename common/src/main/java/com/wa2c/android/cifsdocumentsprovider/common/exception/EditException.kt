package com.wa2c.android.cifsdocumentsprovider.common.exception

/**
 * Edit exception.
 */
sealed class EditException : RuntimeException() {
    class InputRequiredException : EditException()
    class InvalidIdException : EditException()
    class DuplicatedIdException : EditException()
}
