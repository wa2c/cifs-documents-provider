package com.wa2c.android.cifsdocumentsprovider.presentation.ui.common

import android.view.KeyEvent
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

/**
 * Move focus on Enter key
 */
fun Modifier.moveFocusOnEnter(focusManager: FocusManager) =
    onPreviewKeyEvent { key ->
        when (key.nativeKeyEvent.keyCode) {
            KeyEvent.KEYCODE_ENTER -> {
                if (key.type == KeyEventType.KeyDown) focusManager.moveFocus(FocusDirection.Next)
                true
            }
            else -> {
                false
            }
        }
    }

/**
 * Move focus on Tab key
 */
fun Modifier.moveFocusOnTab(focusManager: FocusManager) =
    onPreviewKeyEvent { key ->
        when (key.nativeKeyEvent.keyCode) {
            KeyEvent.KEYCODE_TAB -> {
                if (key.type == KeyEventType.KeyDown) {
                    if (key.isShiftPressed) {
                        focusManager.moveFocus(FocusDirection.Previous)
                    } else {
                        focusManager.moveFocus(FocusDirection.Next)
                    }
                }
                true
            }
            else -> {
                false
            }
        }
    }

/**
 * Enabled style
 */
fun Modifier.enabledStyle(enabled: Boolean): Modifier {
    return if (enabled) this else this.alpha(0.5f)
}
