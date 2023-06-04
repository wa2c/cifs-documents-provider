package com.wa2c.android.cifsdocumentsprovider.presentation.ui.common

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.wa2c.android.cifsdocumentsprovider.presentation.R
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

@Composable
fun RowScope.MessageIcon(type: PopupMessageType?) {
    when (type) {
        PopupMessageType.Success -> {
            Icon(
                painter = painterResource(id = R.drawable.ic_check_ok),
                contentDescription = "Success",
                tint = colorResource(id = R.color.ic_check_ok),
                modifier = Modifier
                    .padding(end = Theme.SizeM)
                    .align(Alignment.CenterVertically)
            )
        }

        PopupMessageType.Warning -> {
            Icon(
                painter = painterResource(id = R.drawable.ic_check_wn),
                contentDescription = "Warning",
                tint = colorResource(id = R.color.ic_check_wn),
                modifier = Modifier
                    .padding(end = Theme.SizeM)
                    .align(Alignment.CenterVertically)
            )
        }

        PopupMessageType.Error -> {
            Icon(
                painter = painterResource(id = R.drawable.ic_check_ng),
                contentDescription = "Error",
                tint = colorResource(id = R.color.ic_check_ng),
                modifier = Modifier
                    .padding(end = Theme.SizeM)
                    .align(Alignment.CenterVertically)
            )
        }
        PopupMessageType.Normal -> {}
        null -> {}
    }
}

@Composable
fun AppSnackbar(message: PopupMessage) {
    Snackbar(
        modifier = Modifier
            .padding(Theme.SizeS)
    ) {
        Row {
            MessageIcon(type = message.type)
            val text = when (message) {
                is PopupMessage.Resource -> stringResource(id = message.res)
                is PopupMessage.Text -> message.text
            }
            Column(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
            ) {
                Text(
                    text = text.toString(),
                    fontWeight = FontWeight.Bold,
                )
                val cause = message.error?.localizedMessage?.substringAfter(": ")
                if (cause != null) {
                    Text(
                        text = cause,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
fun CoroutineScope.showPopup(snackbarHostState: SnackbarHostState, popupMessage: PopupMessage?) {
    launch {
        snackbarHostState.currentSnackbarData?.dismiss()
        popupMessage?.let { snackbarHostState.showSnackbar(MessageSnackbarVisual.create(it)) }
    }
}


