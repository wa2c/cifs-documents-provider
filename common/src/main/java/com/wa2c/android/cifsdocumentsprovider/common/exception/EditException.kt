package com.wa2c.android.cifsdocumentsprovider.common.exception

sealed class Edit(e: Exception?): RuntimeException(e) {
    sealed class SaveCheck(e: Exception?) : Edit(e) {
        class InputRequiredException : SaveCheck(null)
        class InvalidIdException: SaveCheck(null)
        class DuplicatedIdException : SaveCheck(null)
    }

    sealed class KeyCheck(e: Exception?) : Edit(e) {
        class AccessFailedException(e: Exception? = null) : KeyCheck(e)
        class InvalidException(e: Exception? = null) : KeyCheck(e)
    }
}
