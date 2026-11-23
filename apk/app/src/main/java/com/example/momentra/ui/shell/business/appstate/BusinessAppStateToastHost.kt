package com.example.momentra.ui.shell.business.appstate

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Copy for the Operating “Setup activated” toast after business moment activate. */
object BusinessSetupActivateToastCopy {
    const val DEFAULT_TITLE = "Setup activated"

    fun subtitle(momentTitle: String, momentTypeCode: String?): String {
        val name = momentTitle.trim()
        val code = momentTypeCode?.trim()?.uppercase()
        val livePhrase = when (code) {
            "DISTRIBUTION_DEALERSHIP" -> "Distribution is live"
            "TRADING_WHOLESALE" -> "Trading is live"
            "CLIENT_CAMPAIGN" -> "Campaign is live"
            else -> "is live"
        }
        return "$name · $livePhrase · opening Pulse"
    }
}

/** Which Obsidian toaster (Figma 2:1375) to show in the Business shell. */
sealed class BusinessAppStateToast {
    data class Success(val text: String = "Moment activated") : BusinessAppStateToast()
    data class SetupActivated(val title: String, val subtitle: String) : BusinessAppStateToast()
    data class OfflineQueue(val queuedCount: Int = 2) : BusinessAppStateToast()
    data class Error(val code: String = "SYNC_TIMEOUT_504") : BusinessAppStateToast()
}

/**
 * Top-of-shell host for Obsidian system toasts.
 *
 * Keep in lockstep with `BusinessAppStateToastHost` on iOS.
 */
@Composable
fun BusinessAppStateToastHost(
    toast: BusinessAppStateToast?,
    onRetry: () -> Unit = {},
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (toast == null) return
    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        when (toast) {
            is BusinessAppStateToast.Success -> AppStateSuccessToast(text = toast.text)
            is BusinessAppStateToast.SetupActivated ->
                AppStateSetupActivatedToast(
                    title = toast.title,
                    subtitle = toast.subtitle,
                    onDismiss = onDismiss,
                )
            is BusinessAppStateToast.OfflineQueue ->
                AppStateOfflineQueueToast(queuedCount = toast.queuedCount)
            is BusinessAppStateToast.Error ->
                AppStateErrorToast(code = toast.code, onRetry = onRetry)
        }
    }
}
