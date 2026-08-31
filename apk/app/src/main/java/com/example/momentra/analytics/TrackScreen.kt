package com.example.momentra.analytics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

@Composable
fun TrackScreen(
    screenName: String,
    screenClass: String = screenName,
    content: @Composable () -> Unit = {},
) {
    DisposableEffect(screenName) {
        MomentraAnalytics.get().onScreenEnter(screenName, screenClass)
        onDispose { MomentraAnalytics.get().onScreenExit(screenName) }
    }
    content()
}

fun trackWidget(screenName: String, widgetName: String, action: String = "tap") {
    MomentraAnalytics.get().trackWidget(screenName, widgetName, action)
}
