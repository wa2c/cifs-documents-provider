package com.wa2c.android.cifsdocumentsprovider.common.exception

sealed class Edit: RuntimeException() {
    sealed class SaveCheck : Edit() {
        class InputRequiredException : SaveCheck()
        class InvalidIdException : SaveCheck()
        class DuplicatedIdException : SaveCheck()
    }

    sealed class KeyCheck : Edit() {
        class AccessFailedException : KeyCheck()
        class InvalidException : KeyCheck()
    }
}
