package com.wa2c.android.cifsdocumentsprovider.presentation.ui.common

import android.view.KeyEvent
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalAutofillTree

/**
 * Autofill
 * ref: https://bryanherbst.com/2021/04/13/compose-autofill/
 */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.autofill(
    autofillTypes: List<AutofillType>,
    onFill: ((String) -> Unit),
) = composed {
    val autofill = LocalAutofill.current
    val autofillNode = AutofillNode(onFill = onFill, autofillTypes = autofillTypes)
    LocalAutofillTree.current += autofillNode

    this.onGloballyPositioned {
        autofillNode.boundingBox = it.boundsInWindow()
    }.onFocusChanged { focusState ->
        autofill?.run {
            if (focusState.isFocused) {
                requestAutofillForNode(autofillNode)
            } else {
                cancelAutofillForNode(autofillNode)
            }
        }
    }
}

/**
 * Move focus on Enter key
 */
fun Modifier.moveFocusOnEnter(focusManager: FocusManager) = composed {
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
}

/**
 * Move focus on Tab key
 */
fun Modifier.moveFocusOnTab(focusManager: FocusManager) = composed {
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
}