package com.example.momentra.ui.shell.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Edge-to-edge dialog. The dialog window reports no navigation inset, so the content is padded
 * from the activity's navigation bar, and never less than a gesture-bar floor. Top insets stay
 * with the caller so headers are not padded twice.
 */
@Composable
fun MomentraFullscreenDialog(
    onDismissRequest: () -> Unit,
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnClickOutside,
        ),
    ) {
        val dialogView = LocalView.current
        SideEffect {
            val window = (dialogView.parent as? DialogWindowProvider)?.window ?: return@SideEffect
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            window.setDimAmount(0f)
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
        val bottom = with(LocalDensity.current) {
            activityNavigationBottomPx(dialogView.context).toDp()
        }.coerceAtLeast(48.dp)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = bottom),
        ) {
            content()
        }
    }
}

private fun activityNavigationBottomPx(context: Context): Int {
    val decor = context.findActivity()?.window?.decorView ?: return 0
    val insets = ViewCompat.getRootWindowInsets(decor) ?: return 0
    return insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
