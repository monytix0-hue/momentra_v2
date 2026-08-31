package com.example.momentra.ui.shell.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R
import com.example.momentra.domain.BottomDestination
import com.example.momentra.ui.shell.maestro.MaestroIds
import com.example.momentra.ui.theme.ShellTokens

/**
 * Bottom nav with Figma icons. Selected accent follows Context Switcher color.
 */
@Composable
fun ShellBottomNavigation(
    selected: BottomDestination,
    onSelect: (BottomDestination) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ShellTokens.BottomBarBackground)
            .navigationBarsPadding()
            .testTag(MaestroIds.BOTTOM_NAV),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(ShellTokens.BottomBarHeight)
                .padding(horizontal = 12.dp)
                .padding(top = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            LabeledTab(
                destination = BottomDestination.PULSE,
                selected = selected,
                iconRes = R.drawable.ic_nav_pulse,
                accent = accent,
                onSelect = onSelect,
                modifier = Modifier.weight(1f),
            )
            LabeledTab(
                destination = BottomDestination.MOMENTS,
                selected = selected,
                iconRes = R.drawable.ic_nav_moments,
                accent = accent,
                onSelect = onSelect,
                modifier = Modifier.weight(1f),
            )
            CreateFab(
                selected = selected == BottomDestination.CREATE,
                accent = accent,
                onClick = { onSelect(BottomDestination.CREATE) },
                modifier = Modifier.weight(1f),
            )
            LabeledTab(
                destination = BottomDestination.LIFE,
                selected = selected,
                iconRes = R.drawable.ic_nav_life,
                accent = accent,
                onSelect = onSelect,
                modifier = Modifier.weight(1f),
            )
            LabeledTab(
                destination = BottomDestination.MEMORY,
                selected = selected,
                iconRes = R.drawable.ic_nav_memory,
                accent = accent,
                onSelect = onSelect,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun LabeledTab(
    destination: BottomDestination,
    selected: BottomDestination,
    iconRes: Int,
    accent: Color,
    onSelect: (BottomDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSelected = destination == selected
    val tint = if (isSelected) accent else ShellTokens.BottomUnselected
    val tabId = when (destination) {
        BottomDestination.PULSE -> MaestroIds.BOTTOM_PULSE
        BottomDestination.MOMENTS -> MaestroIds.BOTTOM_MOMENTS
        BottomDestination.CREATE -> MaestroIds.BOTTOM_QUICKADD
        BottomDestination.LIFE -> MaestroIds.BOTTOM_LIFE
        BottomDestination.MEMORY -> MaestroIds.BOTTOM_MEMORY
    }
    Column(
        modifier = modifier
            .clickable { onSelect(destination) }
            .testTag(tabId)
            .semantics {
                role = Role.Tab
                this.selected = isSelected
                contentDescription = destination.label
            }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            colorFilter = ColorFilter.tint(tint),
        )
        Text(
            text = destination.label,
            color = tint,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Composable
private fun CreateFab(
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag(MaestroIds.BOTTOM_QUICKADD)
            .semantics {
                role = Role.Button
                contentDescription = "Quickadds"
                this.selected = selected
            }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(accent),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_shell_plus),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                colorFilter = ColorFilter.tint(Color.White),
            )
        }
    }
}

val BottomDestination.label: String
    get() = when (this) {
        BottomDestination.PULSE -> "Pulse"
        BottomDestination.MOMENTS -> "Moments"
        BottomDestination.CREATE -> "quickadds"
        BottomDestination.LIFE -> "Life"
        BottomDestination.MEMORY -> "Memory"
    }
