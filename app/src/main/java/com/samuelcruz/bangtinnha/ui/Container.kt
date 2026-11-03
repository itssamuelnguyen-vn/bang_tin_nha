package com.samuelcruz.bangtinnha.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.samuelcruz.bangtinnha.BangTinNhaApp
import com.samuelcruz.bangtinnha.core.AppContainer

/** The app's AppContainer, from any screen. */
@Composable
fun appContainer(): AppContainer = (LocalContext.current.applicationContext as BangTinNhaApp).container
