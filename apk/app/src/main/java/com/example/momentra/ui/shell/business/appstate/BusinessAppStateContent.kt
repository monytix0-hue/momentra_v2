package com.example.momentra.ui.shell.business.appstate

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.ui.theme.PlusJakartaSans
import com.example.momentra.ui.theme.generated.BusinessIcons

/**
 * Renders one app-state body: glow mesh, hero indicator badge, title, body copy, refraction glass
 * detail card and the action stack. Every geometry value comes from [BusinessAppStateTokens]; the
 * accent comes from [BusinessAppStateAccent].
 *
 * Rebuilt against Figma "Obsidian & Neon Refraction" (`2852:39645`).
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
    val tone = if (spec.phase == BusinessAppStatePhase.Error) AppStateTone.Error else AppStateTone.Normal

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val viewportHeight = maxHeight

        // Figma root background for every App States frame.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(BusinessAppStateTokens.Page),
        )
        AppStateGlow(
            accent = accentColor,
            tone = tone,
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
                AppStateOfflineBanner(text = BusinessAppStateSpecs.OFFLINE_BANNER)
            }

            AppStateMarkRing(
                accent = accentColor,
                icon = spec.icon.imageVector,
                // Figma: the 120pt hero maps onto the 112dp badge; No-company uses 124dp.
                size = if (spec.screen == BusinessAppStateScreen.NoCompany) 124.dp else BusinessAppStateTokens.HeroSize,
                tone = tone,
                dotGlyph = spec.dotGlyph,
            )

            AppStateTitle(spec.title)
            if (spec.body.isNotBlank()) {
                AppStateBodyText(spec.body)
            }

            if (spec.errorCode != null) {
                AppStateErrorCodePill(code = spec.errorCode, accent = errorAccent)
            }

            if (spec.rows.isNotEmpty() && spec.eyebrow != null) {
                AppStateGlassCard {
                    // The Offline frame re-tints the eyebrow to #94A4B8.
                    AppStateEyebrow(
                        text = spec.eyebrow,
                        accent = if (spec.screen == BusinessAppStateScreen.Offline) {
                            BusinessAppStateTokens.TextMuted
                        } else {
                            accentColor
                        },
                    )
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

/**
 * Loading body: hero badge, title, then a single refraction glass skeleton box holding three
 * shimmering placeholder bars, followed by a 110dp trailing spacer.
 *
 * Figma removed the pulsing-dot label row and the 2-tile + 3-card grid, so both are gone here.
 */
@Composable
fun BusinessAppStateLoadingBody(
    label: String,
    accent: BusinessAppStateAccent,
    modifier: Modifier = Modifier,
) {
    val accentColor = accent.color
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val viewportHeight = maxHeight

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(BusinessAppStateTokens.Page),
        )
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
            AppStateMarkRing(
                accent = accentColor,
                icon = AppStateIcon.Sparkle.imageVector,
            )

            AppStateTitle(label)

            AppStateSkeletonBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BusinessAppStateTokens.SkeletonBoxHeight),
            ) {
                AppStateSkeletonBar(width = 160.dp)
                AppStateSkeletonBar(width = 300.dp)
                AppStateSkeletonBar(width = 220.dp)
            }

            Spacer(Modifier.height(BusinessAppStateTokens.SkeletonTrailingSpace))
        }
    }
}

/**
 * Hub empty body: six equal capture tiles in a three-column glass grid. The amber highlight and
 * the muted 60% tiles are gone — every tile carries the same weight.
 */
@Composable
fun BusinessAppStateHubBody(
    accent: BusinessAppStateAccent,
    vertical: BusinessAppStateVertical,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
    /** Secondary CTA "Back to Pulse" from the Figma Hub frame. */
    onBackToPulse: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val spec = BusinessAppStateSpecs.spec(BusinessAppStateScreen.Hub, BusinessAppStatePhase.Empty)
    val tiles = BusinessAppStateSpecs.hubTileLabels(vertical)
    val accentColor = accent.color

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val viewportHeight = maxHeight
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(BusinessAppStateTokens.Page),
        )
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
            // Every tile carries equal weight in the Obsidian frame — no amber badge.
            AppStateMarkRing(
                accent = accentColor,
                icon = spec.icon.imageVector,
                dotGlyph = spec.dotGlyph,
            )
            AppStateTitle(spec.title)
            AppStateBodyText(spec.body)

            tiles.chunked(3).forEach { rowTiles ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(BusinessAppStateTokens.HubColumnGap),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    rowTiles.forEach { (icon, label) ->
                        HubTile(
                            icon = icon,
                            label = label,
                            onClick = onPrimary,
                            modifier = Modifier.width(BusinessAppStateTokens.HubTileWidth),
                        )
                    }
                    if (rowTiles.size < 3) {
                        repeat(3 - rowTiles.size) {
                            Spacer(Modifier.width(BusinessAppStateTokens.HubTileWidth))
                        }
                    }
                }
            }

            // Figma Hub frame carries a CTA pair under the tile grid; the grid itself replaces
            // `spec.rows`, so the labels still live on the spec.
            spec.primaryLabel?.let { label ->
                Spacer(Modifier.height(2.dp))
                AppStatePrimaryButton(label = label, onClick = onPrimary)
            }
            spec.secondaryLabel?.let { label ->
                AppStateSecondaryButton(label = label, onClick = onBackToPulse)
            }

            spec.footnote?.let {
                AppStateFootnote(it)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HubTile(
    icon: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = BusinessAppStateTokens
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(t.HubTileLabelGap),
        modifier = modifier
            .height(t.HubTileHeight)
            .clip(RoundedCornerShape(t.HubTileRadius))
            // Figma draws the tile as a flat `white 5%` wash with an emerald @25% hairline.
            .background(t.HubTileFill)
            .border(
                1.dp,
                t.HubTileStroke,
                RoundedCornerShape(t.HubTileRadius),
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = t.HubTileVerticalPadding),
    ) {
        val res = BusinessIcons.res(icon)
        if (res != null) {
            Icon(
                painter = painterResource(res),
                contentDescription = null,
                tint = t.Emerald,
                modifier = Modifier.size(t.HubIconSize),
            )
        }
        Text(
            text = label,
            color = Color.White,
            fontSize = t.HubLabelSize.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

/**
 * Sheet variant of the empty state, used by the Pulse "Needs Attention" view-all sheet.
 * Figma: sheet fills from the bottom with a scrim behind it, radius 22, emerald stroke.
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
    val sheetShape = RoundedCornerShape(
        topStart = BusinessAppStateTokens.CardRadius,
        topEnd = BusinessAppStateTokens.CardRadius,
    )

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
                .clip(sheetShape)
                .background(BusinessAppStateTokens.GlassCardFill)
                .border(
                    BusinessAppStateTokens.GlassCardStrokeWidth,
                    BusinessAppStateTokens.GlassCardStroke,
                    sheetShape,
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
                    .background(BusinessAppStateTokens.Emerald.copy(alpha = 0.30f)),
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
                                    BusinessAppStateTokens.Emerald.copy(alpha = 0.20f)
                                } else {
                                    Color.White.copy(alpha = 0.06f)
                                },
                            )
                            .border(
                                1.dp,
                                if (selected) {
                                    BusinessAppStateTokens.Emerald.copy(alpha = 0.80f)
                                } else {
                                    BusinessAppStateTokens.SecondaryButtonStroke
                                },
                                RoundedCornerShape(BusinessAppStateTokens.ButtonRadius),
                            )
                            .clickable(role = Role.Button) { onSelectFilter(filter) }
                            .padding(horizontal = 16.dp, vertical = 9.dp),
                    ) {
                        Text(
                            text = filter,
                            color = if (selected) BusinessAppStateTokens.Emerald else BusinessAppStateTokens.TextMuted,
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
                AppStateMarkRing(accent = accentColor, icon = spec.icon.imageVector)
                AppStateTitle(spec.title)
                AppStateBodyText(spec.body)
                spec.secondaryLabel?.let { AppStateSecondaryButton(it, onShowAll, filled = true) }
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
