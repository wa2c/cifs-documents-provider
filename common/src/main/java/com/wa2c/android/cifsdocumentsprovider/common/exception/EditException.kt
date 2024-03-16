package com.wa2c.android.cifsdocumentsprovider.common.exception

/**
 * Edit exception.
 */
sealed class EditException : RuntimeException() {
    class InputRequiredException : EditException()
    class InvalidIdException : EditException()
    class DuplicatedIdException : EditException()
}

/**
 * Key check exception.
 */
sealed class KeyCheckException : RuntimeException() {
    class AccessFailedException : KeyCheckException()
    class InvalidException : KeyCheckException()
}
