package com.example.momentra.ui.shell.business.appstate

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.ui.theme.PlusJakartaSans
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close

/**
 * Renders one app-state body: glow, hero mark ring, title, body copy, glass detail card and the
 * action stack. Every geometry value comes from [BusinessAppStateTokens]; the accent comes from
 * [BusinessAppStateAccent].
 */
@Composable
fun BusinessAppStateBody(
    spec: AppStateSpec,
    accent: BusinessAppStateAccent,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accentColor = accent.color
    val errorAccent = BusinessAppStateTokens.ErrorRing

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val viewportHeight = maxHeight

        AppStateGlow(
            accent = accentColor,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 24.dp),
        )

        // Figma centres the State body (justify-center) and only overflows on the tall variants,
        // so keep the column at least viewport height and let it scroll past that.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(
                BusinessAppStateTokens.BodyGap,
                Alignment.CenterVertically,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = viewportHeight)
                .padding(horizontal = BusinessAppStateTokens.ScreenHorizontalPadding)
                .padding(vertical = 24.dp),
        ) {

            if (spec.screen == BusinessAppStateScreen.Offline) {
                OfflineBanner()
            }

            val ringAccent = if (spec.phase == BusinessAppStatePhase.Error) errorAccent else accentColor
            AppStateMarkRing(
                accent = ringAccent,
                icon = spec.icon.imageVector,
                size = if (spec.screen == BusinessAppStateScreen.NoCompany) 132.dp else 120.dp,
            )

            AppStateTitle(spec.title)
            if (spec.body.isNotBlank()) {
                AppStateBodyText(spec.body)
            }

            if (spec.errorCode != null) {
                AppStateErrorCodePill(code = spec.errorCode, accent = errorAccent)
            }

            if (spec.rows.isNotEmpty() && spec.eyebrow != null) {
                AppStateGlassCard(accent = accentColor) {
                    AppStateEyebrow(spec.eyebrow, accentColor)
                    spec.rows.forEach { row ->
                        AppStateGlassRow(
                            label = row.label,
                            value = row.value,
                            valueColor = if (row.tone == ValueTone.Warning) {
                                BusinessAppStateTokens.Warning
                            } else {
                                BusinessAppStateTokens.Text
                            },
                        )
                    }
                }
            }

            if (spec.primaryLabel != null || spec.secondaryLabel != null) {
                Spacer(Modifier.height(2.dp))
            }
            spec.primaryLabel?.let { label ->
                AppStatePrimaryButton(label = label, onClick = onPrimary)
            }
            spec.secondaryLabel?.let { label ->
                AppStateSecondaryButton(label = label, onClick = onSecondary)
            }
            spec.footnote?.let { AppStateFootnote(it) }
        }
    }
}

/** Amber banner shown above the ring on the Offline state. */
@Composable
private fun OfflineBanner(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BusinessAppStateTokens.WarningSurface)
            .border(1.dp, BusinessAppStateTokens.WarningBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp),
    ) {
        Text(
            text = "⌁",
            color = BusinessAppStateTokens.Warning,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
        Text(
            text = BusinessAppStateSpecs.OFFLINE_BANNER,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
    }
}

/**
 * Loading body: label row with a pulsing accent dot, then two skeleton tiles and three
 * skeleton cards. Figma aligns this body to the top rather than centring it.
 */
@Composable
fun BusinessAppStateLoadingBody(
    label: String,
    accent: BusinessAppStateAccent,
    modifier: Modifier = Modifier,
) {
    val accentColor = accent.color
    Box(modifier = modifier.fillMaxSize()) {
        AppStateGlow(
            accent = accentColor,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 24.dp),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(BusinessAppStateTokens.BodyGap),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = BusinessAppStateTokens.ScreenHorizontalPadding)
                .padding(vertical = 28.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppStateSpinnerDot(accentColor)
                Text(
                    text = label,
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(2) {
                    AppStateSkeletonBox(accent = accentColor, modifier = Modifier.width(172.dp)) {
                        AppStateSkeletonBar(width = 70.dp, height = 26.dp, radius = 8.dp)
                        AppStateSkeletonBar(width = 90.dp, height = 10.dp, radius = 5.dp)
                    }
                }
            }

            repeat(3) {
                AppStateSkeletonBox(accent = accentColor, modifier = Modifier.fillMaxWidth()) {
                    AppStateSkeletonBar(width = 120.dp, height = 12.dp)
                    AppStateSkeletonBar(width = 220.dp, height = 28.dp, radius = 8.dp)
                    AppStateSkeletonBar(width = 300.dp, height = 12.dp)
                    AppStateSkeletonBar(width = 240.dp, height = 12.dp)
                }
            }
        }
    }
}

/**
 * Hub empty body: six capture tiles in a two-column grid. The first tile carries the accent
 * treatment, the rest are muted until the first capture lands.
 */
@Composable
fun BusinessAppStateHubBody(
    accent: BusinessAppStateAccent,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spec = BusinessAppStateSpecs.spec(BusinessAppStateScreen.Hub, BusinessAppStatePhase.Empty)
    val tiles = BusinessAppStateSpecs.hubTileLabels(accent)
    val accentColor = accent.color

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val viewportHeight = maxHeight
        AppStateGlow(
            accent = accentColor,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 24.dp),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(
                BusinessAppStateTokens.BodyGap,
                Alignment.CenterVertically,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = viewportHeight)
                .padding(horizontal = BusinessAppStateTokens.ScreenHorizontalPadding)
                .padding(vertical = 24.dp),
        ) {
            AppStateMarkRing(accent = accentColor, icon = spec.icon.imageVector)
            AppStateTitle(spec.title)
            AppStateBodyText(spec.body)

            tiles.chunked(2).forEachIndexed { rowIndex, rowTiles ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    rowTiles.forEachIndexed { colIndex, (icon, label) ->
                        val isFirst = rowIndex == 0 && colIndex == 0
                        HubTile(
                            icon = icon,
                            label = label,
                            accent = accentColor,
                            highlighted = isFirst,
                            onClick = onPrimary,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (rowTiles.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            spec.footnote?.let { AppStateFootnote(it) }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HubTile(
    icon: AppStateIcon,
    label: String,
    accent: Color,
    highlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .height(73.dp)
            .clip(RoundedCornerShape(BusinessAppStateTokens.CardRadius))
            .background(
                if (highlighted) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.05f),
            )
            .border(
                1.dp,
                if (highlighted) accent.copy(alpha = 0.60f) else Color.White.copy(alpha = 0.12f),
                RoundedCornerShape(BusinessAppStateTokens.CardRadius),
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon.imageVector,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Sheet variant of the empty state, used by the Pulse "Needs Attention" view-all sheet.
 * Figma: sheet fills from y=150 with a scrim behind it, radius 20, accent stroke.
 */
@Composable
fun BusinessAppStateViewAllSheet(
    accent: BusinessAppStateAccent,
    selectedFilter: String,
    onSelectFilter: (String) -> Unit,
    onClose: () -> Unit,
    onShowAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spec = BusinessAppStateSpecs.spec(BusinessAppStateScreen.ViewAll, BusinessAppStatePhase.Empty)
    val accentColor = accent.color
    val filters = listOf("All", "Open", "Due soon", "Closed")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(role = Role.Button, onClick = onClose),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(BusinessAppStateTokens.Sheet)
                .border(
                    1.dp,
                    accentColor.copy(alpha = 0.35f),
                    RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                )
                .clickable(enabled = false) {}
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.30f)),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                    AppStateEyebrow(spec.eyebrow.orEmpty(), accentColor)
                    Text(
                        text = "Needs Attention",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = PlusJakartaSans,
                    )
                }
                AppStateIconButton(
                    icon = Icons.Outlined.Close,
                    contentDescription = "Close",
                    onClick = onClose,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                filters.forEach { filter ->
                    val selected = filter == selectedFilter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(BusinessAppStateTokens.ButtonRadius))
                            .background(
                                if (selected) {
                                    Brush.horizontalGradient(
                                        listOf(
                                            BusinessAppStateTokens.CtaStart,
                                            BusinessAppStateTokens.CtaEnd,
                                        ),
                                    )
                                } else {
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.10f),
                                            Color.White.copy(alpha = 0.10f),
                                        ),
                                    )
                                },
                            )
                            .border(
                                1.dp,
                                if (selected) Color.Transparent else Color.White.copy(alpha = 0.22f),
                                RoundedCornerShape(BusinessAppStateTokens.ButtonRadius),
                            )
                            .clickable(role = Role.Button) { onSelectFilter(filter) }
                            .padding(horizontal = 16.dp, vertical = 9.dp),
                    ) {
                        Text(
                            text = filter,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = PlusJakartaSans,
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(BusinessAppStateTokens.BodyGap),
                modifier = Modifier.fillMaxWidth(),
            ) {
                AppStateMarkRing(accent = accentColor, icon = spec.icon.imageVector, size = 112.dp)
                AppStateTitle(spec.title)
                AppStateBodyText(spec.body)
                spec.secondaryLabel?.let { AppStateSecondaryButton(it, onShowAll) }
                spec.footnote?.let { AppStateFootnote(it) }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

/** Small helper so the body can be width-limited on wide screens without breaking the shell. */
@Composable
fun AppStateCenteredColumn(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(BusinessAppStateTokens.BodyGap),
        modifier = modifier
            .widthIn(max = BusinessAppStateTokens.BodyWidth)
            .fillMaxWidth(),
        content = content,
    )
}
