package com.wa2c.android.cifsdocumentsprovider.presentation.ui.folder

import android.content.res.Configuration
import android.net.Uri
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wa2c.android.cifsdocumentsprovider.domain.model.CifsFile
import com.wa2c.android.cifsdocumentsprovider.presentation.R
import com.wa2c.android.cifsdocumentsprovider.presentation.ui.common.Theme

/**
 * Folder Screen
 */
@Composable
fun FolderListScreen(
    fileList: List<CifsFile>,
    currentFile: CifsFile?,
    isLoading: Boolean,
    onClickItem: (CifsFile) -> Unit,
    onClickSet: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxHeight()
    ) {
        Box(
            modifier = Modifier.weight(weight = 1f, fill = true)
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = !isLoading,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                LazyColumn {
                    items(items = fileList) { file ->
                        FolderItem(
                            cifsFile = file,
                            onClick = { onClickItem(file) },
                        )
                    }
                }
            }
        }

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
            Text(
                text = currentFile?.uri?.toString() ?: "",
                maxLines = 1,
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
            )
            Button(
                onClick = onClickSet,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text(text = stringResource(id = R.string.folder_set))
            }
        }
    }
}

@Composable
private fun FolderItem(
    cifsFile: CifsFile,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable(enabled = true, onClick = onClick)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_folder),
            "Folder",
            modifier = Modifier.size(40.dp),
        )
        Text(
            text = cifsFile.name,
            fontSize = 15.sp,
            modifier = Modifier
                .padding(start = 8.dp)
        )

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
        FolderListScreen(
            fileList = listOf(
                CifsFile(
                    name = "example1.txt",
                    uri = Uri.parse("smb://example/"),
                    size = 128,
                    lastModified = 0,
                    isDirectory = true,
                ),
                CifsFile(
                    name = "example2example2example2example2example2example2.txt",
                    uri = Uri.parse("smb://example/"),
                    size = 128,
                    lastModified = 0,
                    isDirectory = true,
                ),
                CifsFile(
                    name = "example3example3example3example3example3example3example3example3example3example3.txt",
                    uri = Uri.parse("smb://example/"),
                    size = 128,
                    lastModified = 0,
                    isDirectory = true,
                )
            ),
            currentFile = CifsFile(
                name = "",
                uri = Uri.parse("smb://example/test"),
                size = 0,
                lastModified = 0,
                isDirectory = true,
            ),
            isLoading = false,
            onClickItem = {},
            onClickSet = {},
        )
    }
}
