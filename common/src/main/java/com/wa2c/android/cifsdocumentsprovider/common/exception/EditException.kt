package com.wa2c.android.cifsdocumentsprovider.common.exception

sealed class EditException(e: Exception?): RuntimeException(e) {
    sealed class SaveCheck(e: Exception?) : EditException(e) {
        class InputRequiredException : SaveCheck(null)
        class InvalidIdException: SaveCheck(null)
        class DuplicatedIdException : SaveCheck(null)
    }

    sealed class KeyCheck(e: Exception?) : EditException(e) {
        class AccessFailedException(e: Exception? = null) : KeyCheck(e)
        class InvalidException(e: Exception? = null) : KeyCheck(e)
    }
}
