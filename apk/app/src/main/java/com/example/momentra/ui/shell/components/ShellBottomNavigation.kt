package com.example.momentra.ui.shell.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R
import com.example.momentra.domain.BottomDestination
import com.example.momentra.ui.shell.maestro.MaestroIds
import com.example.momentra.ui.shell.tour.TourTargetId
import com.example.momentra.ui.shell.tour.tourTarget
import com.example.momentra.ui.theme.ShellTokens

/**
 * Native Material [NavigationBar] with Figma shell icons.
 *
 * Selected accent follows the active moment palette (Dist `#EA580C`, Trading emerald, …).
 * TRD/DST-PULSE-v12: 44 dp FAB + soft accent glow, 4 dp selected dot, slate idle `#64748B`.
 */
@Composable
fun ShellBottomNavigation(
    selected: BottomDestination,
    onSelect: (BottomDestination) -> Unit,
    accent: Color,
    /** When PERSONAL, CREATE tab shows "Add" instead of "Quickadds". */
    createLabel: String = BottomDestination.CREATE.label,
    unselected: Color = ShellTokens.BottomUnselected,
    barBackground: Color = ShellTokens.BottomBarBackground,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier
            .tourTarget(TourTargetId.BOTTOM_NAV)
            .testTag(MaestroIds.BOTTOM_NAV),
        containerColor = barBackground,
        tonalElevation = 0.dp,
    ) {
        ShellNavItem(
            destination = BottomDestination.PULSE,
            selected = selected,
            accent = accent,
            unselected = unselected,
            onSelect = onSelect,
            iconRes = R.drawable.ic_nav_pulse,
            tour = TourTargetId.TAB_PULSE,
            testTag = MaestroIds.BOTTOM_PULSE,
        )
        ShellNavItem(
            destination = BottomDestination.MOMENTS,
            selected = selected,
            accent = accent,
            unselected = unselected,
            onSelect = onSelect,
            iconRes = R.drawable.ic_nav_moments,
            tour = TourTargetId.TAB_MOMENTS,
            testTag = MaestroIds.BOTTOM_MOMENTS,
        )
        NavigationBarItem(
            selected = selected == BottomDestination.CREATE,
            onClick = { onSelect(BottomDestination.CREATE) },
            icon = {
                // Figma center FAB: 44 dp filled accent circle + soft outer glow (TRD/DST-PULSE-v12).
                Box(
                    modifier = Modifier
                        .size(52.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .shadow(
                                elevation = 16.dp,
                                shape = CircleShape,
                                ambientColor = accent.copy(alpha = 0.35f),
                                spotColor = accent.copy(alpha = 0.55f),
                            )
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
            },
            label = {
                Text(
                    text = createLabel,
                    fontSize = 10.sp,
                    fontWeight = if (selected == BottomDestination.CREATE) FontWeight.Bold else FontWeight.Medium,
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedTextColor = accent,
                unselectedTextColor = unselected,
                indicatorColor = Color.Transparent,
            ),
            modifier = Modifier
                .tourTarget(TourTargetId.BOTTOM_CREATE)
                .testTag(MaestroIds.BOTTOM_QUICKADD),
        )
        ShellNavItem(
            destination = BottomDestination.LIFE,
            selected = selected,
            accent = accent,
            unselected = unselected,
            onSelect = onSelect,
            iconRes = R.drawable.ic_nav_life,
            tour = TourTargetId.TAB_LIFE,
            testTag = MaestroIds.BOTTOM_LIFE,
        )
        ShellNavItem(
            destination = BottomDestination.MEMORY,
            selected = selected,
            accent = accent,
            unselected = unselected,
            onSelect = onSelect,
            iconRes = R.drawable.ic_nav_memory,
            tour = TourTargetId.TAB_MEMORY,
            testTag = MaestroIds.BOTTOM_MEMORY,
        )
    }
}

@Composable
private fun RowScope.ShellNavItem(
    destination: BottomDestination,
    selected: BottomDestination,
    accent: Color,
    unselected: Color,
    onSelect: (BottomDestination) -> Unit,
    iconRes: Int,
    tour: TourTargetId,
    testTag: String,
) {
    val isSelected = selected == destination
    val tint = if (isSelected) accent else unselected
    NavigationBarItem(
        selected = isSelected,
        onClick = { onSelect(destination) },
        icon = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    colorFilter = ColorFilter.tint(tint),
                )
                // Figma selected tab: 4 dp accent dot under the icon.
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) accent else Color.Transparent),
                )
            }
        },
        label = {
            Text(
                text = destination.label,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            )
        },
        colors = NavigationBarItemDefaults.colors(
            selectedTextColor = accent,
            unselectedTextColor = unselected,
            indicatorColor = Color.Transparent,
        ),
        modifier = Modifier
            .tourTarget(tour)
            .testTag(testTag),
    )
}

val BottomDestination.label: String
    get() = when (this) {
        BottomDestination.PULSE -> "Pulse"
        BottomDestination.MOMENTS -> "Moments"
        BottomDestination.CREATE -> "Quickadds"
        BottomDestination.LIFE -> "Life"
        BottomDestination.MEMORY -> "Memory"
    }
