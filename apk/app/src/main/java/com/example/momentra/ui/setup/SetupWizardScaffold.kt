package com.example.momentra.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun SetupWizardScaffold(
    backgroundColor: androidx.compose.ui.graphics.Color = SetupTokens.BgPrimary,
    footer: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val navBottom = with(density) {
        WindowInsets.navigationBars.getBottom(this).toDp()
    }
    // Dialog hosts sometimes report 0 insets; keep a gesture-nav floor so Activate stays tappable.
    val footerBottomPad = if (navBottom > 0.dp) navBottom else 28.dp

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 8.dp),
            ) {
                content()
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(bottom = footerBottomPad),
        ) {
            footer()
        }
    }
}
