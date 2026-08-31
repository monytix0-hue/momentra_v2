package com.example.momentra.ui.shell.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.ui.shell.maestro.MaestroIds
import com.example.momentra.ui.theme.MomentraBrandColors
import com.example.momentra.ui.theme.ShellTokens

/**
 * Figma moment / module switcher chrome.
 * Collapsed by default — title row only; tap to expand module pills.
 */
@Composable
fun MomentSwitcher(
    selectedTitle: String?,
    selectedMomentId: String?,
    activeMoments: List<Pair<String, String>>,
    isEmpty: Boolean,
    isLoading: Boolean,
    onSelectMoment: (String) -> Unit = {},
    onSettings: () -> Unit = {},
    accent: Color = MomentraBrandColors.Cta,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val title = when {
        isLoading -> "Loading moments…"
        isEmpty -> "No moments yet"
        selectedTitle.isNullOrBlank() -> "Select moment"
        else -> selectedTitle
    }
    val pills = when {
        isLoading || isEmpty -> emptyList()
        activeMoments.isNotEmpty() -> activeMoments
        !selectedTitle.isNullOrBlank() && selectedMomentId != null -> listOf(selectedMomentId to selectedTitle)
        else -> emptyList()
    }
    val canExpand = pills.size > 1
    val canOpenSettings = selectedMomentId != null && !isEmpty && !isLoading

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ShellTokens.TopBarBackground)
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ShellTokens.ModuleCardBackground)
            .testTag(MaestroIds.MOMENT_SWITCHER)
            .semantics { contentDescription = "Moment switcher: $title" },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = canExpand) { expanded = !expanded },
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(accent),
                )
                Text(
                    text = title,
                    color = MomentraBrandColors.TextOnDark,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = "Moment settings",
                    tint = accent.copy(alpha = if (canOpenSettings) 1f else 0.4f),
                    modifier = Modifier
                        .size(28.dp)
                        .clickable(enabled = canOpenSettings, onClick = onSettings)
                        .padding(6.dp)
                        .testTag("moment.switcher.settings"),
                )
                if (canExpand) {
                    Icon(
                        if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse moment switcher" else "Expand moment switcher",
                        tint = MomentraBrandColors.TextOnDark,
                        modifier = Modifier
                            .size(28.dp)
                            .clickable { expanded = !expanded }
                            .padding(5.dp),
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = expanded && pills.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column(
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "When Module.",
                    color = ShellTokens.EmptyBody,
                    fontSize = 11.sp,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    pills.forEach { (momentId, pill) ->
                        val selected = momentId == selectedMomentId
                        Text(
                            text = pill,
                            color = if (selected) Color.Black else accent,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (selected) accent else Color.Transparent)
                                .border(
                                    width = 1.dp,
                                    color = accent,
                                    shape = RoundedCornerShape(999.dp),
                                )
                                .clickable { onSelectMoment(momentId) }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
}
