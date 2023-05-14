package com.wa2c.android.cifsdocumentsprovider.presentation.ui.host

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wa2c.android.cifsdocumentsprovider.domain.model.HostData
import com.wa2c.android.cifsdocumentsprovider.presentation.R
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.Theme

/**
 * Host Screen
 */
@Composable
fun HostScreen(
    hostList: List<HostData>,
    isInit: Boolean,
    onClickItem: (HostData) -> Unit,
    onClickSet: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxHeight()
    ) {
        Box(
            modifier = Modifier.weight(weight = 1f, fill = true)
        ) {
            LazyColumn {
                items(items = hostList) { hostData ->
                    HostItem(
                        hostData = hostData,
                        onClick = { onClickItem(hostData) },
                    )
                    Divider(thickness = 0.5.dp, color = Theme.DividerColor)
                }
            }
        }

        if (isInit) {
            Spacer(
                modifier = Modifier
                    .height(1.dp)
                    .fillMaxWidth()
                    .background(color = Theme.DividerColor)
            )

            Column(
                modifier = Modifier
                    .padding(8.dp),
            ) {
                Button(
                    onClick = onClickSet,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(text = stringResource(id = R.string.host_set_manually))
                }
            }
        }
    }
}

@Composable
private fun HostItem(
    hostData: HostData,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = true, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_host),
            "Host",
            modifier = Modifier.size(48.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp)
        ) {

            Text(
                text = hostData.hostName,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = hostData.ipAddress,
                style = MaterialTheme.typography.bodyMedium,
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
private fun FolderScreenPreview() {
    Theme.AppTheme {
        HostScreen(
            hostList = listOf(
                HostData(
                    hostName = "Host1",
                    ipAddress = "192.168.0.1",
                    detectionTime = 0,
                ),
                HostData(
                    hostName = "Host2",
                    ipAddress = "192.168.0.2",
                    detectionTime = 0,
                ),
                HostData(
                    hostName = "192.168.0.3",
                    ipAddress = "192.168.0.3",
                    detectionTime = 0,
                ),
            ),
            isInit = true,
            onClickItem = {},
            onClickSet = {},
        )
    }
}
