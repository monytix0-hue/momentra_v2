package com.example.momentra.ui.shell.group.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.momentra.ui.shell.components.MomentraWindowSize
import com.example.momentra.ui.shell.components.rememberMomentraWindowSize

/**
 * Shared scroll scaffold for bottom-nav tab content.
 * AppShell already places content above the nav bar — do not add phantom bottom padding.
 */
@Composable
fun ActiveTabScrollScaffold(
    background: Color,
    modifier: Modifier = Modifier,
    window: MomentraWindowSize = rememberMomentraWindowSize(),
    contentPadding: PaddingValues = window.contentPadding(),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(window.sectionSpacing),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(background)
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = verticalArrangement,
        content = content,
    )
}
