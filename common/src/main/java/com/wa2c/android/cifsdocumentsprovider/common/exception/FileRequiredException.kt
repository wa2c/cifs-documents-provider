package com.wa2c.android.cifsdocumentsprovider.common.exception

import java.io.IOException

class FileRequiredException : IOException {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
}
