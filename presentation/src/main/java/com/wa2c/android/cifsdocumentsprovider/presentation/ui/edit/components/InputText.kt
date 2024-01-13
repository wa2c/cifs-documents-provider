package com.wa2c.android.cifsdocumentsprovider.presentation.ui.edit.components

import android.content.res.Configuration
import android.view.KeyEvent
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.Theme
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.autofill
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.moveFocusOnEnter
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.moveFocusOnTab


/**
 * Input text
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun InputText(
    title: String,
    hint: String,
    state: MutableState<String?>,
    focusManager: FocusManager,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Text,
        imeAction = ImeAction.Next,
    ),
    autofillType: AutofillType? = null,
    @DrawableRes iconResource: Int? = null,
    onClickButton: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Theme.SizeSS)
    ) {
        OutlinedTextField(
            value = state.value ?: "",
            label = { Text(title) },
            enabled = enabled,
            placeholder = { Text(hint) },
            onValueChange = { value ->
                state.value = if (keyboardOptions.keyboardType == KeyboardType.Number) value.filter { it.isDigit() } else value
            },
            keyboardOptions = keyboardOptions,
            visualTransformation = if (keyboardOptions.keyboardType == KeyboardType.Password) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            maxLines = 1,
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically)
                .moveFocusOnEnter(focusManager)
                .moveFocusOnTab(focusManager)
                .autofill(
                    autofillTypes = autofillType?.let { listOf(it) } ?: emptyList(),
                    onFill = { state.value = it }
                )
            ,
        )
        iconResource?.let {res ->
            Button(
                shape = RoundedCornerShape(Theme.SizeSS),
                contentPadding = PaddingValues(0.dp),
                enabled = enabled,
                modifier = Modifier
                    .size(52.dp, 52.dp)
                    .padding(top = Theme.SizeSS, start = Theme.SizeSS)
                    .align(Alignment.CenterVertically)
                    .moveFocusOnEnter(focusManager)
                    .moveFocusOnTab(focusManager)
                    .onPreviewKeyEvent {
                        when (it.nativeKeyEvent.keyCode) {
                            KeyEvent.KEYCODE_SPACE -> {
                                if (it.type == KeyEventType.KeyUp) onClickButton()
                                true
                            }

                            else -> {
                                false
                            }
                        }
                    }
                ,
                onClick = onClickButton,
            ) {
                Icon(
                    painter = painterResource(id = res),
                    contentDescription = title,
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Preview(
    name = "Preview",
    group = "Group",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun InputTextPreview() {
    Theme.AppTheme {
        val state = remember { mutableStateOf<String?>("Input") }
        InputText(
            title = "Title",
            hint = "Hint",
            state = state,
            focusManager = LocalFocusManager.current,
            enabled = true,

        )
    }
}
