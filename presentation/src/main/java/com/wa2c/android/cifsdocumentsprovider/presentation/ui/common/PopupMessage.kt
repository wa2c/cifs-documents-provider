package com.wa2c.android.cifsdocumentsprovider.presentation.ui.common

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.labelRes
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
    abstract val type: PopupMessageType?

    /** Error */
    abstract val error: Throwable?

    @Parcelize
    data class Text(
        /** Text */
        val text: CharSequence,
        override val type: PopupMessageType?,
        override val error: Throwable? = null,
    ) : PopupMessage()

    @Parcelize
    data class Resource(
        /** Text */
        @StringRes val res: Int,
        override val type: PopupMessageType?,
        override val error: Throwable? = null,
    ) : PopupMessage()
}

/**
 * Message icon
 */
enum class PopupMessageType {
    /** Success */
    Success,

    /** Warning */
    Warning,

    /** Error */
    Error,
}

fun CoroutineScope.showPopup(
    snackbarHostState: SnackbarHostState,
    text: String,
    type: PopupMessageType? = null,
    error: Throwable? = null,
) {
    launch {
        snackbarHostState.currentSnackbarData?.dismiss()
        val visual = MessageSnackbarVisual.create(
            popupMessage = PopupMessage.Text(
                text = text,
                type = type,
                error = error,
            )
        )
        snackbarHostState.showSnackbar(visual)
    }
}

fun CoroutineScope.showPopup(
    snackbarHostState: SnackbarHostState,
    @StringRes stringRes: Int,
    type: PopupMessageType? = null,
    error: Throwable? = null,
) {
    launch {
        snackbarHostState.currentSnackbarData?.dismiss()
        val visual = MessageSnackbarVisual.create(
            popupMessage = PopupMessage.Resource(
                res = stringRes,
                type = type,
                error = error,
            )
        )
        snackbarHostState.showSnackbar(visual)
    }
}

fun CoroutineScope.showError(
    snackbarHostState: SnackbarHostState,
    error: Throwable? = null,
    @StringRes stringRes: Int? = null,
) {
    val labelRes = stringRes ?: error.labelRes
    showPopup(snackbarHostState, labelRes, PopupMessageType.Error, error)
}
