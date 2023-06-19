package com.wa2c.android.cifsdocumentsprovider.presentation.ui.common

import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
fun DividerNormal() = Divider(thickness = 1.dp, color = Theme.Colors.Divider)

@Composable
fun DividerThin() = Divider(thickness = 0.5.dp, color = Theme.Colors.Divider)