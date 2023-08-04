package com.wa2c.android.cifsdocumentsprovider.presentation.ui.common

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

data class MessageSnackbarVisual(
    override val message: String,
    override val actionLabel: String?,
    override val duration: SnackbarDuration,
    override val withDismissAction: Boolean,
    val popupMessage: PopupMessage,
) : SnackbarVisuals {
    companion object {
        fun create(popupMessage: PopupMessage): MessageSnackbarVisual {
            return MessageSnackbarVisual(
                message = "",
                actionLabel = null,
                duration = SnackbarDuration.Short,
                withDismissAction = false,
                popupMessage = popupMessage,
            )
        }
    }
}


/**
 * Message
 */
sealed class PopupMessage : Parcelable {
    /** Type */
    abstract val type: PopupMessageType

    /** Error */
    abstract val error: Throwable?

    @Parcelize
    data class Text(
        /** Text */
        val text: CharSequence,
        override val type: PopupMessageType,
        override val error: Throwable? = null,
    ) : PopupMessage()

    @Parcelize
    data class Resource(
        /** Text */
        @StringRes val res: Int,
        override val type: PopupMessageType,
        override val error: Throwable? = null,
    ) : PopupMessage()
}

/**
 * Message icon
 */
enum class PopupMessageType {
    /** Normal */
    Normal,

    /** Success */
    Success,

    /** Warning */
    Warning,

    /** Error */
    Error,
}

fun CoroutineScope.showPopup(snackbarHostState: SnackbarHostState, popupMessage: PopupMessage?) {
    launch {
        snackbarHostState.currentSnackbarData?.dismiss()
        popupMessage?.let { snackbarHostState.showSnackbar(MessageSnackbarVisual.create(it)) }
    }
}


