package com.example.momentra.ui.shell.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.domain.AppContext
import com.example.momentra.ui.shell.maestro.MaestroIds
import com.example.momentra.ui.theme.ShellTokens
import com.example.momentra.ui.theme.shell.ContextThemes
import com.example.momentra.ui.theme.shell.GlobalTheme

/**
 * Bootstrap-driven Context Switcher — renders only [supportedContexts].
 * Uses ContextTheme.contextAccent (never MomentTheme.primary).
 */
@Composable
fun ContextSwitcher(
    selectedContext: AppContext,
    supportedContexts: List<AppContext>,
    onSelect: (AppContext) -> Unit,
    modifier: Modifier = Modifier,
) {
    val contexts = supportedContexts.ifEmpty { listOf(AppContext.PERSONAL) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(ShellTokens.TopBarBackground)
            .padding(horizontal = 12.dp)
            .padding(top = 2.dp, bottom = 6.dp)
            .heightIn(min = ShellTokens.ContextSwitcherHeight)
            .testTag(MaestroIds.CONTEXT_SWITCHER),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        contexts.forEach { context ->
            val isSelected = context == selectedContext
            val accent = ContextThemes.of(context).contextAccent
            val id = when (context) {
                AppContext.PERSONAL -> MaestroIds.CONTEXT_PERSONAL
                AppContext.GROUP -> MaestroIds.CONTEXT_GROUP
                AppContext.BUSINESS -> MaestroIds.CONTEXT_BUSINESS
                AppContext.CIRCLE -> MaestroIds.CONTEXT_CIRCLE
            }
            Text(
                text = context.label,
                color = if (isSelected) Color.White else GlobalTheme.contextUnselected,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) accent else Color.Transparent)
                    .clickable { onSelect(context) }
                    .testTag(id)
                    .semantics {
                        role = Role.Tab
                        selected = isSelected
                        contentDescription = "Switch to ${context.label}"
                    }
                    .padding(horizontal = 4.dp, vertical = 6.dp),
            )
        }
    }
}

val AppContext.label: String
    get() = when (this) {
        AppContext.PERSONAL -> "Personal"
        AppContext.GROUP -> "Group"
        AppContext.BUSINESS -> "Business"
        AppContext.CIRCLE -> "Circle"
    }
