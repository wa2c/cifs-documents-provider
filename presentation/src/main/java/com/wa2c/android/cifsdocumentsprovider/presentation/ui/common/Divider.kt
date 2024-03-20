package com.wa2c.android.cifsdocumentsprovider.presentation.ui.common

import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
fun DividerWide() = HorizontalDivider(thickness = 2.dp, color = Theme.Colors.Divider)

@Composable
fun DividerNormal() = HorizontalDivider(thickness = 1.dp, color = Theme.Colors.Divider)

@Composable
fun DividerThin() = HorizontalDivider(thickness = 0.5.dp, color = Theme.Colors.Divider)
