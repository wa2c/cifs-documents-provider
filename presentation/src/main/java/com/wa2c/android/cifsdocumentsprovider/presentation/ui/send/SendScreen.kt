package com.wa2c.android.cifsdocumentsprovider.presentation.ui.send

import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wa2c.android.cifsdocumentsprovider.common.values.SendDataState
import com.wa2c.android.cifsdocumentsprovider.domain.model.SendData
import com.wa2c.android.cifsdocumentsprovider.presentation.R
import com.wa2c.android.cifsdocumentsprovider.presentation.ext.getSummaryText
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.Theme

/**
 * Send Screen
 */
@Composable
fun SendScreen(
    sendDataList: List<SendData>,
    onClickCancel: (SendData) -> Unit,
    onClickRetry: (SendData) -> Unit,
    onClickRemove: (SendData) -> Unit,
    onClickCancelAll: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxHeight()
    ) {
        Box(
            modifier = Modifier.weight(weight = 1f, fill = true)
        ) {
            LazyColumn {
                items(items = sendDataList) { sendData ->
                    SendDataItem(
                        sendData = sendData,
                        onClickCancel = onClickCancel,
                        onClickRetry = onClickRetry,
                        onClickRemove = onClickRemove,
                    )
                    Divider(thickness = 0.5.dp, color = Theme.DividerColor)
                }
            }
        }

        Divider(thickness = 1.dp, color = Theme.DividerColor)

        Column(
            modifier = Modifier
                .padding(8.dp),
        ) {
            Button(
                onClick = onClickCancelAll,
                shape = RoundedCornerShape(4.dp),
                enabled = sendDataList.any { it.state.isCancelable },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text(text = stringResource(id = R.string.send_action_cancel))
            }
        }
    }
}

@Composable
private fun SendDataItem(
    sendData: SendData,
    onClickCancel: (SendData) -> Unit,
    onClickRetry: (SendData) -> Unit,
    onClickRemove: (SendData) -> Unit,
) {
    val showPopup = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = true, onClick = {
                showPopup.value = true
            })
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = sendData.name,
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = sendData.getSummaryText(context = LocalContext.current),
            style = MaterialTheme.typography.bodyMedium,
        )
        LinearProgressIndicator(
            progress = (sendData.progress.toFloat() / 100),
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (sendData.state.inProgress) 1f else 0f),
        )
    }

    if (showPopup.value) {
        DropdownMenu(
            expanded = showPopup.value ,
            onDismissRequest = {
                showPopup.value = false
            }
        ) {
            if (sendData.state.isCancelable) {
                DropdownMenuItem(
                    text = { Text(stringResource(id = R.string.send_action_cancel)) },
                    onClick = {
                        onClickCancel(sendData)
                        showPopup.value = false
                    }
                )
            }
            if (sendData.state.isRetryable) {
                DropdownMenuItem(
                    text = { Text(stringResource(id = R.string.send_action_retry)) },
                    onClick = {
                        onClickRetry(sendData)
                        showPopup.value = false
                    }
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(id = R.string.send_action_remove)) },
                onClick = {
                    onClickRemove(sendData)
                    showPopup.value = false
                }
            )
        }
    }
}


/**
 * Preview
 */
@Preview(
    name = "Preview",
    group = "Group",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun SendScreenPreview() {
    Theme.AppTheme {
        SendScreen(
            sendDataList = listOf(
                SendData(
                    id = "1",
                    name = "Sample1.jpg",
                    size = 1000000,
                    mimeType = "image/jpeg",
                    sourceUri = Uri.parse("smb://pc1/send"),
                    targetUri = Uri.parse("smb://pc2/receive"),
                    state = SendDataState.PROGRESS,
                    progressSize = 500000,
                ),
                SendData(
                    id = "2",
                    name = "Sample2.MP3",
                    size = 2000000,
                    mimeType = "audio/mpeg",
                    sourceUri = Uri.parse("smb://pc1/send"),
                    targetUri = Uri.parse("smb://pc2/receive"),
                    state = SendDataState.READY,
                    progressSize = 0,
                ),
                SendData(
                    id = "3",
                    name = "sample3.mp4",
                    size = 3000000,
                    mimeType = "video/mp4",
                    sourceUri = Uri.parse("smb://pc1/send"),
                    targetUri = Uri.parse("smb://pc2/receive"),
                    state = SendDataState.CANCEL,
                    progressSize = 2000000,
                ),
            ),
            onClickCancel = {},
            onClickRetry = {},
            onClickRemove = {},
            onClickCancelAll = {},
        )
    }
}
