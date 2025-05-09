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
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wa2c.android.cifsdocumentsprovider.presentation.R
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.Theme
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
    value: String?,
    focusManager: FocusManager,
    enabled: Boolean = true,
    readonly: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Text,
        imeAction = ImeAction.Next,
    ),
    autofillType: ContentType? = null,
    @DrawableRes iconResource: Int? = null,
    onClickButton: () -> Unit = {},
    onChange: (String?) -> Unit,
) {
    val isPassword = keyboardOptions.keyboardType == KeyboardType.Password
    var passwordVisible: Boolean by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Theme.Sizes.SS)
    ) {
        OutlinedTextField(
            value = value ?: "",
            label = { Text(title) },
            enabled = enabled,
            readOnly = readonly,
            placeholder = { Text(hint) },
            onValueChange = { value ->
                onChange(if (keyboardOptions.keyboardType == KeyboardType.Number) value.filter { it.isDigit() } else value)
            },
            keyboardOptions = keyboardOptions,
            visualTransformation = if (isPassword && !passwordVisible) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            trailingIcon = if (isPassword) {
                {
                    IconButton(
                        onClick = { passwordVisible = !passwordVisible }
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_visibility),
                            contentDescription = "Password visibility",
                        )
                    }
                }
            } else {
                null
            },
            maxLines = 1,
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically)
                .moveFocusOnEnter(focusManager)
                .moveFocusOnTab(focusManager)
                .semantics { autofillType?.let { contentType = it } },
        )
        iconResource?.let { res ->
            Button(
                shape = RoundedCornerShape(Theme.Sizes.SS),
                contentPadding = PaddingValues(0.dp),
                enabled = enabled,
                modifier = Modifier
                    .size(52.dp, 52.dp)
                    .padding(top = Theme.Sizes.SS, start = Theme.Sizes.SS)
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
                    imageVector = ImageVector.vectorResource(id = res),
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
        InputText(
            title = "Title",
            hint = "Hint",
            value = "Input",
            focusManager = LocalFocusManager.current,
            enabled = true,
        ) {
        }
    }
}
