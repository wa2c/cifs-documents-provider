package com.wa2c.android.cifsdocumentsprovider.presentation.ui.edit

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wa2c.android.cifsdocumentsprovider.common.utils.getContentUri
import com.wa2c.android.cifsdocumentsprovider.common.utils.getSmbUri
import com.wa2c.android.cifsdocumentsprovider.common.values.StorageType
import com.wa2c.android.cifsdocumentsprovider.presentation.R
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.labelRes
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.OptionItem
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.Theme

/**
 * Edit Screen
 */
@Composable
fun EditScreen(
    nameState: MutableState<String?>,
    storageState: MutableState<StorageType>,
    domainState: MutableState<String?>,
    hostState: MutableState<String?>,
    portState: MutableState<String?>,
    enableDfsState: MutableState<Boolean>,
    userState: MutableState<String?>,
    passwordState: MutableState<String?>,
    anonymousState: MutableState<Boolean>,
    folderState: MutableState<String?>,
    safeTransferState: MutableState<Boolean>,
    extensionState: MutableState<Boolean>,
    onClickSet: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .weight(1f)
        ) {

            InputText(
                title = stringResource(id = R.string.edit_name_title),
                hint = stringResource(id = R.string.edit_name_hint),
                state = nameState,
            )

            InputOption(
                title = stringResource(id = R.string.edit_storage_title),
                items = StorageType.values()
                    .map { OptionItem(it, stringResource(id = it.labelRes)) },
                state = storageState,
            )

            InputText(
                title = stringResource(id = R.string.edit_domain_title),
                hint = stringResource(id = R.string.edit_domain_hint),
                state = domainState,
            )

            InputText(
                title = stringResource(id = R.string.edit_host_title),
                hint = stringResource(id = R.string.edit_host_hint),
                state = hostState,
                iconResource = R.drawable.ic_search,
            ) {

            }

            InputText(
                title = stringResource(id = R.string.edit_port_title),
                hint = stringResource(id = R.string.edit_port_hint),
                state = portState,
            )

            InputCheck(
                title = stringResource(id = R.string.edit_enable_dfs_label),
                state = enableDfsState,
            )

            InputText(
                title = stringResource(id = R.string.edit_user_title),
                hint = stringResource(id = R.string.edit_user_hint),
                state = userState,
            )

            InputText(
                title = stringResource(id = R.string.edit_password_title),
                hint = stringResource(id = R.string.edit_password_hint),
                state = passwordState,
            )

            InputCheck(
                title = stringResource(id = R.string.edit_anonymous_label),
                state = anonymousState,
            )

            InputText(
                title = stringResource(id = R.string.edit_folder_title),
                hint = stringResource(id = R.string.edit_folder_hint),
                state = folderState,
                iconResource = R.drawable.ic_folder,
            ) {

            }

            Text("option")

            InputCheck(
                title = stringResource(id = R.string.edit_option_safe_transfer_label),
                state = safeTransferState,
            )

            InputCheck(
                title = stringResource(id = R.string.edit_option_extension_label),
                state = extensionState,
            )

            Text("url")

            Text( getSmbUri(hostState.value, portState.value, folderState.value, true) )

            Text(getContentUri(hostState.value, portState.value, folderState.value))

        }


        Divider(thickness = 1.dp, color = Theme.DividerColor)

        Column(
            modifier = Modifier
                .padding(8.dp),
        ) {

            OutlinedButton(
                onClick = onClickSet,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text(text = stringResource(id = R.string.edit_check_connection_button))
            }

            Button(
                onClick = onClickSet,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text(text = stringResource(id = R.string.edit_save_button))
            }
        }
    }
}

/**
 * Input text
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputText(
    title: String,
    hint: String,
    state: MutableState<String?>,
    @DrawableRes iconResource: Int? = null,
    onClickButton: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    ) {
        OutlinedTextField(
            value = state.value ?: "",
            label = { Text(title) },
            placeholder = { Text(hint) },
            onValueChange = { state.value = it },
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically)
        )
        iconResource?.let {res ->
            Button(
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .size(52.dp, 52.dp)
                    .padding(top = 4.dp, start = 4.dp)
                    .align(Alignment.CenterVertically)
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

/**
 * Input Option
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> InputOption(
    title: String,
    items: List<OptionItem<T>>,
    state: MutableState<T>,
) {

    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .wrapContentWidth()
            .padding(top = 4.dp)
    ) {
        OutlinedTextField(
            value = items.first { it.item == state.value }.label,
            label = { Text(title) },
            readOnly = true,
            onValueChange = {
            },
            modifier = Modifier
                .onFocusChanged {
                    expanded = it.isFocused
                }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            },
            modifier = Modifier
                .fillMaxWidth()
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.label) },
                    onClick = {
                        state.value = item.item
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Input check
 */
@Composable
fun InputCheck(
    title: String,
    state: MutableState<Boolean>,
) {
    Row(
        Modifier
            .toggleable(
                value = state.value,
                role = Role.Checkbox,
                onValueChange = { state.value = !state.value }
            )
            .padding(16.dp)
            .padding(top = 4.dp)
            .fillMaxWidth()
    ) {
        Checkbox(
            checked = state.value,
            onCheckedChange = null
        )
        Text(
            text = title,
            Modifier
                .weight(1f)
                .padding(start = 8.dp)
        )
    }
}

/**
 * Preview
 */
@SuppressLint("UnrememberedMutableState")
@Preview(
    name = "Preview",
    group = "Group",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun EditScreenPreview() {
    Theme.AppTheme {
        EditScreen(
            nameState = mutableStateOf("name1"),
            storageState = mutableStateOf(StorageType.SMBJ),
            domainState = mutableStateOf(""),
            hostState = mutableStateOf("pc1"),
            portState = mutableStateOf(""),
            enableDfsState = mutableStateOf(false),
            userState = mutableStateOf("user"),
            passwordState = mutableStateOf("password"),
            anonymousState = mutableStateOf(false),
            folderState = mutableStateOf("/test"),
            safeTransferState = mutableStateOf(false),
            extensionState = mutableStateOf(false),
            onClickSet = {},
        )
    }
}
