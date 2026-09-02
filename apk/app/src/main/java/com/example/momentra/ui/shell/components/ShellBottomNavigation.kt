package com.example.momentra.ui.shell.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
    NavigationBar(
        modifier = modifier
            .testTag(MaestroIds.BOTTOM_NAV),
        containerColor = ShellTokens.BottomBarBackground,
        tonalElevation = 0.dp
    ) {
        NavigationBarItem(
            selected = selected == BottomDestination.PULSE,
            onClick = { onSelect(BottomDestination.PULSE) },
            icon = {
                Image(
                    painter = painterResource(R.drawable.ic_nav_pulse),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    colorFilter = ColorFilter.tint(if (selected == BottomDestination.PULSE) accent else ShellTokens.BottomUnselected)
                )
            },
            label = {
                Text(
                    text = BottomDestination.PULSE.label,
                    fontSize = 10.sp,
                    fontWeight = if (selected == BottomDestination.PULSE) FontWeight.Bold else FontWeight.Medium
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedTextColor = accent,
                unselectedTextColor = ShellTokens.BottomUnselected,
                indicatorColor = Color.Transparent
            ),
            modifier = Modifier.testTag(MaestroIds.BOTTOM_PULSE)
        )
        NavigationBarItem(
            selected = selected == BottomDestination.MOMENTS,
            onClick = { onSelect(BottomDestination.MOMENTS) },
            icon = {
                Image(
                    painter = painterResource(R.drawable.ic_nav_moments),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    colorFilter = ColorFilter.tint(if (selected == BottomDestination.MOMENTS) accent else ShellTokens.BottomUnselected)
                )
            },
            label = {
                Text(
                    text = BottomDestination.MOMENTS.label,
                    fontSize = 10.sp,
                    fontWeight = if (selected == BottomDestination.MOMENTS) FontWeight.Bold else FontWeight.Medium
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedTextColor = accent,
                unselectedTextColor = ShellTokens.BottomUnselected,
                indicatorColor = Color.Transparent
            ),
            modifier = Modifier.testTag(MaestroIds.BOTTOM_MOMENTS)
        )
        NavigationBarItem(
            selected = selected == BottomDestination.CREATE,
            onClick = { onSelect(BottomDestination.CREATE) },
            icon = {
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
            },
            label = {
                Text(
                    text = BottomDestination.CREATE.label,
                    fontSize = 10.sp,
                    fontWeight = if (selected == BottomDestination.CREATE) FontWeight.Bold else FontWeight.Medium
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedTextColor = accent,
                unselectedTextColor = ShellTokens.BottomUnselected,
                indicatorColor = Color.Transparent
            ),
            modifier = Modifier.testTag(MaestroIds.BOTTOM_QUICKADD)
        )
        NavigationBarItem(
            selected = selected == BottomDestination.LIFE,
            onClick = { onSelect(BottomDestination.LIFE) },
            icon = {
                Image(
                    painter = painterResource(R.drawable.ic_nav_life),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    colorFilter = ColorFilter.tint(if (selected == BottomDestination.LIFE) accent else ShellTokens.BottomUnselected)
                )
            },
            label = {
                Text(
                    text = BottomDestination.LIFE.label,
                    fontSize = 10.sp,
                    fontWeight = if (selected == BottomDestination.LIFE) FontWeight.Bold else FontWeight.Medium
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedTextColor = accent,
                unselectedTextColor = ShellTokens.BottomUnselected,
                indicatorColor = Color.Transparent
            ),
            modifier = Modifier.testTag(MaestroIds.BOTTOM_LIFE)
        )
        NavigationBarItem(
            selected = selected == BottomDestination.MEMORY,
            onClick = { onSelect(BottomDestination.MEMORY) },
            icon = {
                Image(
                    painter = painterResource(R.drawable.ic_nav_memory),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    colorFilter = ColorFilter.tint(if (selected == BottomDestination.MEMORY) accent else ShellTokens.BottomUnselected)
                )
            },
            label = {
                Text(
                    text = BottomDestination.MEMORY.label,
                    fontSize = 10.sp,
                    fontWeight = if (selected == BottomDestination.MEMORY) FontWeight.Bold else FontWeight.Medium
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedTextColor = accent,
                unselectedTextColor = ShellTokens.BottomUnselected,
                indicatorColor = Color.Transparent
            ),
            modifier = Modifier.testTag(MaestroIds.BOTTOM_MEMORY)
        )
    }
}

val BottomDestination.label: String
    get() = when (this) {
        BottomDestination.PULSE -> "Pulse"
        BottomDestination.MOMENTS -> "Moments"
        BottomDestination.CREATE -> "Quickadds"
        BottomDestination.LIFE -> "Life"
        BottomDestination.MEMORY -> "Memory"
    }
