package com.wa2c.android.cifsdocumentsprovider.presentation.ext

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit

@Composable
fun Dp.toSp() = with(LocalDensity.current) {  this@toSp.toSp() }

@Composable
fun TextUnit.toDp() = with(LocalDensity.current) {  this@toDp.toDp() }
