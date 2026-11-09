package com.example.momentra.ui.shell.business.restaurant

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R
import com.example.momentra.ui.theme.RestaurantEmber

/**
 * Shared visual primitives for the restaurant section, matching Figma `2487:37611`.
 *
 * Every Pulse card (`#292524`, 18% ember stroke) and every Moments / Life / Memory board
 * is the same surface, so they share one `EmberCard` implementation.
 */

enum class EmberTone { Pulse, Board }

/** 3-stop vertical wash behind every restaurant screen. */
@Composable
fun EmberScreenBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to RestaurantEmber.screenTop,
                    0.35f to RestaurantEmber.screenMid,
                    1f to RestaurantEmber.screenBottom,
                ),
            ),
    ) { content() }
}

/** 1dp white 5% line pinned to the top edge, clipped by the card shape. */
private fun emberInnerHighlight(): Modifier = Modifier.drawWithContent {
    drawContent()
    drawRect(color = Color.White.copy(alpha = 0.05f), topLeft = Offset(0f, 0f), size = Size(size.width, 1f))
}

/**
 * Card with the Figma ember double shadow (`0 8px 28px rgba(234,88,12,.18)` +
 * `0 2px 6px rgba(0,0,0,.35)`) plus the `inset 0 1px 0 rgba(255,255,255,.05)` highlight.
 */
@Composable
fun EmberCard(
    modifier: Modifier = Modifier,
    tone: EmberTone = EmberTone.Pulse,
    strokeAlpha: Float? = null,
    gap: Int = 14,
    padding: Int = 16,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val alpha = strokeAlpha ?: 0.18f
    val shape = RestaurantEmber.rCard
    val interactive = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Column(
        modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.32f),
                spotColor = Color.Black.copy(alpha = 0.35f),
            )
            .shadow(
                elevation = 10.dp,
                shape = shape,
                clip = false,
                ambientColor = RestaurantEmber.ember.copy(alpha = 0.18f),
                spotColor = RestaurantEmber.ember.copy(alpha = 0.18f),
            )
            .clip(shape)
            .background(RestaurantEmber.card)
            .border(1.dp, RestaurantEmber.ember.copy(alpha = alpha), shape)
            .then(emberInnerHighlight())
            .then(interactive)
            .padding(padding.dp),
        verticalArrangement = Arrangement.spacedBy(gap.dp),
        content = { content() },
    )
}

/** Screen title (24/Bold) + gold role line (12/Medium). */
@Composable
fun EmberScreenHeader(
    title: String,
    roleLine: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = RestaurantEmber.screenTitle(), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(roleLine, style = RestaurantEmber.roleLine(), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Filled wine chip + outline chips (`Nov 2026 · INR`, `GST on`) + optional `+ Quick Adds`. */
@Composable
fun EmberContextChips(
    subtypeLabel: String,
    periodLine: String,
    gstOn: Boolean,
    modifier: Modifier = Modifier,
    quickAdds: Boolean = false,
    onQuickAdds: (() -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledEmberChip(subtypeLabel)
        OutlineEmberChip(periodLine)
        OutlineEmberChip(if (gstOn) "GST on" else "GST off")
        if (quickAdds && onQuickAdds != null) {
            Text(
                "+ Quick Adds",
                style = RestaurantEmber.chipLabel(false).copy(color = Color.White),
                maxLines = 1,
                modifier = Modifier
                    .shadow(
                        elevation = 6.dp,
                        shape = RestaurantEmber.rPill,
                        clip = false,
                        ambientColor = RestaurantEmber.ember.copy(alpha = 0.45f),
                        spotColor = RestaurantEmber.ember.copy(alpha = 0.45f),
                    )
                    .clip(RestaurantEmber.rPill)
                    .background(RestaurantEmber.ember)
                    .clickable(onClick = onQuickAdds)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
fun FilledEmberChip(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = RestaurantEmber.chipLabel(true).copy(color = Color.White),
        maxLines = 1,
        modifier = modifier
            .clip(RestaurantEmber.rPill)
            .background(RestaurantEmber.wine)
            .border(1.dp, RestaurantEmber.wine, RestaurantEmber.rPill)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
fun OutlineEmberChip(text: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val click = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Text(
        text,
        style = RestaurantEmber.chipLabel(false),
        maxLines = 1,
        modifier = modifier
            .clip(RestaurantEmber.rPill)
            .background(RestaurantEmber.chip)
            .border(1.dp, Color.White.copy(alpha = 0.06f), RestaurantEmber.rPill)
            .then(click)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

/** Card header: bold title, optional trailing chip, optional `View all ›`. */
@Composable
fun EmberSectionHeader(
    title: String,
    chipText: String? = null,
    onViewAll: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = RestaurantEmber.sectionTitle(), maxLines = 1, modifier = Modifier.weight(1f))
        if (chipText != null) FilledEmberChip(chipText)
        if (onViewAll != null) {
            Text(
                "View all ›",
                style = RestaurantEmber.viewAll(),
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(onClick = onViewAll)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }
    }
}

/** Board header: title + gold trailing note + optional `View all ›`. */
@Composable
fun EmberBoardHeader(
    title: String,
    trailingNote: String? = null,
    onViewAll: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = RestaurantEmber.sectionTitle(), maxLines = 1, modifier = Modifier.weight(1f))
        if (trailingNote != null) {
            Text(trailingNote, style = RestaurantEmber.viewAll(), maxLines = 1)
        }
        if (onViewAll != null) {
            Text(
                "View all ›",
                style = RestaurantEmber.viewAll(),
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(onClick = onViewAll)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }
    }
}

/** `#1c1917` metric chip with an 11/Medium label and an 18/Bold value. */
@Composable
fun EmberMetricChip(
    label: String,
    value: String,
    valueColor: Color = RestaurantEmber.textPrimary,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(RestaurantEmber.rMetric)
            .background(RestaurantEmber.surfaceWell)
            .border(1.dp, Color.White.copy(alpha = 0.06f), RestaurantEmber.rMetric)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, style = RestaurantEmber.metricLabel(), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, style = RestaurantEmber.metricValue(valueColor), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** KPI tile inside a board card (`rgba(0,0,0,.24)` + 6% hairline). */
@Composable
fun EmberKpiChip(
    label: String,
    value: String,
    valueColor: Color = RestaurantEmber.textPrimary,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(RestaurantEmber.rRow)
            .background(Color.Black.copy(alpha = 0.24f))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RestaurantEmber.rRow)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, style = RestaurantEmber.kpiBigLabel(), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, style = RestaurantEmber.kpiBig(valueColor), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Row with a 3dp coloured left bar (`list row` in the Attention / Commitments cards). */
@Composable
fun EmberAccentRow(
    title: String,
    caption: String,
    accent: Color,
    trailingValue: String? = null,
    trailingColor: Color = RestaurantEmber.textPrimary,
    severityLabel: String? = null,
    severityColor: Color = accent,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    leadingIcon: Int? = null,
) {
    val click = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Row(
        modifier
            .fillMaxWidth()
            .clip(RestaurantEmber.rRow)
            .background(RestaurantEmber.surfaceWell)
            .then(
                Modifier.drawWithContent {
                    drawContent()
                    drawRect(color = accent, topLeft = Offset(0f, 0f), size = Size(3.dp.toPx(), size.height))
                },
            )
            .then(click)
            .padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(RestaurantEmber.ember.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(leadingIcon), contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = RestaurantEmber.rowTitle(), maxLines = 2)
            Text(caption, style = RestaurantEmber.rowCaption(), maxLines = 2)
        }
        if (trailingValue != null) {
            Text(trailingValue, style = RestaurantEmber.rowValue(trailingColor), maxLines = 1)
        }
        if (severityLabel != null) EmberSeverityChip(severityLabel, severityColor)
    }
}

/** Divider row used inside Pulse cards (bottom hairline `#44403c`). */
@Composable
fun EmberDividedRow(
    title: String,
    caption: String?,
    trailingValue: String,
    trailingColor: Color = RestaurantEmber.textPrimary,
    captionColor: Color = RestaurantEmber.textBody,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val click = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Row(
        modifier
            .fillMaxWidth()
            .then(
                Modifier.drawWithContent {
                    drawContent()
                    drawRect(
                        color = RestaurantEmber.divider,
                        topLeft = Offset(0f, size.height - 1f),
                        size = Size(size.width, 1f),
                    )
                },
            )
            .then(click)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = RestaurantEmber.rowTitle(), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!caption.isNullOrBlank()) {
                Text(caption, style = RestaurantEmber.rowCaption(captionColor), maxLines = 1)
            }
        }
        Text(trailingValue, style = RestaurantEmber.rowValue(trailingColor), maxLines = 1)
    }
}

/** Tonal severity pill (`Unavailable`, `Overdue`, `Alert`, `Part`, `Open`). */
@Composable
fun EmberSeverityChip(label: String, color: Color, modifier: Modifier = Modifier, bordered: Boolean = true) {
    val shape = RoundedCornerShape(100.dp)
    Text(
        label,
        style = RestaurantEmber.severity().copy(color = color),
        maxLines = 1,
        modifier = modifier
            .clip(shape)
            .background(RestaurantEmber.tone(color))
            .then(if (bordered) Modifier.border(1.dp, RestaurantEmber.toneStroke(color), shape) else Modifier)
            .padding(horizontal = 9.dp, vertical = 4.dp),
    )
}

/** Solid tinted status pill (`● On track`). */
@Composable
fun EmberStatusPill(label: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        label,
        style = RestaurantEmber.severity().copy(color = color),
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(RestaurantEmber.tone(color))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/** `Why` + body at 50% — the honesty rule line under each Pulse card. */
@Composable
fun EmberWhyCaption(body: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = 2.dp, top = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "Why",
            style = RestaurantEmber.why().copy(fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.5f)),
            maxLines = 1,
        )
        Text(body, style = RestaurantEmber.why(), modifier = Modifier.weight(1f))
    }
}

/** Board variant: ember `Why` on an optional ember-tinted note box. */
@Composable
fun EmberWhyNote(body: String, modifier: Modifier = Modifier, tinted: Boolean = false) {
    val base = if (tinted) {
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(RestaurantEmber.ember.copy(alpha = 0.10f))
            .border(1.dp, RestaurantEmber.ember.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(10.dp)
    } else {
        Modifier.padding(horizontal = 2.dp)
    }
    Row(
        modifier
            .fillMaxWidth()
            .then(base),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "Why",
            style = RestaurantEmber.why().copy(color = RestaurantEmber.ember, fontWeight = FontWeight.Bold),
            maxLines = 1,
        )
        Text(body, style = RestaurantEmber.why().copy(color = RestaurantEmber.textBody), modifier = Modifier.weight(1f))
    }
}

/**
 * Health ring from Figma `2400:29677`: 92dp track (white 8%) + ember progress arc
 * with a 6px ember glow, 8.28pt stroke.
 */
@Composable
fun EmberHealthRing(
    percentLabel: String,
    caption: String,
    fraction: Float?,
    modifier: Modifier = Modifier,
) {
    Box(modifier.size(92.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 8.28.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = Color.White.copy(alpha = 0.08f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke),
            )
            if (fraction != null) {
                val sweep = fraction.coerceIn(0f, 1f) * 360f
                drawArc(
                    color = RestaurantEmber.ember.copy(alpha = 0.25f),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(inset - 3f, inset - 3f),
                    size = Size(arcSize.width + 6f, arcSize.height + 6f),
                    style = Stroke(width = stroke + 6f, cap = StrokeCap.Round),
                )
                drawArc(
                    color = RestaurantEmber.ember,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                percentLabel,
                style = RestaurantEmber.metricValue().copy(fontWeight = FontWeight.ExtraBold, fontSize = 22.sp),
                maxLines = 1,
            )
            Text(
                caption,
                style = RestaurantEmber.rowCaption(Color.White.copy(alpha = 0.55f)).copy(fontSize = 10.sp),
                maxLines = 1,
            )
        }
    }
}

/** Rows inside a board card: a 3dp ember left bar, big caption, trailing status. */
@Composable
fun EmberBoardRow(
    eyebrow: String,
    value: String,
    valueColor: Color = RestaurantEmber.textPrimary,
    trailing: String,
    trailingColor: Color = RestaurantEmber.textPrimary,
    accent: Color = RestaurantEmber.ember,
    severity: String? = null,
    severityColor: Color? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val click = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.24f))
            .then(
                Modifier.drawWithContent {
                    drawContent()
                    drawRect(color = accent, topLeft = Offset(0f, 0f), size = Size(3.dp.toPx(), size.height))
                },
            )
            .then(click)
            .padding(start = 12.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier
                .weight(1f)
                .clip(RestaurantEmber.rRow)
                .background(Color.Black.copy(alpha = 0.24f))
                .border(1.dp, Color.White.copy(alpha = 0.06f), RestaurantEmber.rRow)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(eyebrow, style = RestaurantEmber.kpiBigLabel(), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(value, style = RestaurantEmber.kpiBig(valueColor), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(trailing, style = RestaurantEmber.rowValue(trailingColor), maxLines = 1)
        if (severity != null && severityColor != null) {
            EmberSeverityChip(severity, severityColor, bordered = false)
        }
    }
}

/** Board hero used by Moments / Life / Memory. */
@Composable
fun EmberBoardHero(
    eyebrow: String,
    headline: String,
    chips: @Composable () -> Unit,
    kpis: @Composable () -> Unit,
    strokeAlpha: Float = 0.22f,
    modifier: Modifier = Modifier,
) {
    EmberCard(modifier = modifier, tone = EmberTone.Board, strokeAlpha = strokeAlpha, gap = 14, padding = 16) {
        Text(eyebrow, style = RestaurantEmber.boardEyebrow(), maxLines = 1)
        Text(headline, style = RestaurantEmber.boardHeadline(24), maxLines = 2, overflow = TextOverflow.Ellipsis)
        chips()
        kpis()
    }
}

/** Small hero chip (`Restaurant`, `Service open`, `vs Oct 2026`). */
@Composable
fun EmberBoardHeroChip(
    text: String,
    color: Color,
    dot: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(100.dp))
            .background(RestaurantEmber.tone(color))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(100.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dot) {
            Canvas(Modifier.size(6.dp)) { drawCircle(color) }
        }
        Text(text, style = RestaurantEmber.boardChip().copy(color = color), maxLines = 1)
    }
}

/** White pill CTA used by the Pulse / Quick Add hero (`+  Record Sale`). */
@Composable
fun EmberCta(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RestaurantEmber.rCardTight
    Row(
        modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = shape,
                clip = false,
                ambientColor = RestaurantEmber.ember.copy(alpha = 0.55f),
                spotColor = RestaurantEmber.ember.copy(alpha = 0.55f),
            )
            .clip(shape)
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = RestaurantEmber.storyCtaTitle().copy(color = RestaurantEmber.ember),
            maxLines = 1,
        )
    }
}

/** Section label + trailing note over the Quick Add grids (`Core · 9` … `RFC Food journey`). */
@Composable
fun EmberGridSectionHeader(title: String, note: String, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = RestaurantEmber.sectionTitle(), maxLines = 1)
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.08f)),
        )
        Text(
            note,
            style = RestaurantEmber.rowCaption(RestaurantEmber.gold)
                .copy(fontWeight = FontWeight.Medium, fontSize = 11.sp),
            maxLines = 1,
        )
    }
}

/** A Quick Add tile: 112dp tall, 32dp icon well, 14/Bold label, 10/Regular caption. */
@Composable
fun EmberQuickAddTile(
    iconRes: Int,
    label: String,
    caption: String,
    locked: String?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val shape = RestaurantEmber.rTile
    val open = locked == null
    val click = if (onClick != null && open) Modifier.clickable(onClick = onClick) else Modifier
    Column(
        modifier
            .height(112.dp)
            .shadow(
                elevation = if (open) 8.dp else 0.dp,
                shape = shape,
                clip = false,
                ambientColor = RestaurantEmber.ember.copy(alpha = 0.18f),
                spotColor = RestaurantEmber.ember.copy(alpha = 0.18f),
            )
            .clip(shape)
            .background(RestaurantEmber.tile)
            .border(1.dp, if (open) RestaurantEmber.ember else Color.White.copy(alpha = 0.08f), shape)
            .then(click)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(RestaurantEmber.rTileIcon)
                    .background(if (open) RestaurantEmber.ember else RestaurantEmber.hairline),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(iconRes),
                    contentDescription = null,
                    tint = if (open) Color.White else RestaurantEmber.textFaint,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.weight(1f))
        }
        Text(
            label,
            style = RestaurantEmber.tileLabel().copy(color = if (open) RestaurantEmber.textPrimary else RestaurantEmber.textFaint),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            locked ?: caption,
            style = RestaurantEmber.tileCaption().copy(color = if (open) RestaurantEmber.textBody else RestaurantEmber.amber.copy(alpha = 0.8f)),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Empty state in the ember language — never shows a fabricated `0%`. */
@Composable
fun EmberEmpty(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    glyph: String = "◇",
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RestaurantEmber.rRow)
            .background(RestaurantEmber.surfaceWell)
            .border(1.dp, Color.White.copy(alpha = 0.06f), RestaurantEmber.rRow)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(glyph, color = RestaurantEmber.ember, fontSize = 18.sp)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = RestaurantEmber.rowTitle())
            Text(body, style = RestaurantEmber.rowCaption())
        }
    }
}

/** Thin hairline used between stacked board rows. */
@Composable
fun EmberDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(RestaurantEmber.divider))
}

/** Icon well used by sheet headers: rounded ember-tinted square. */
@Composable
fun EmberIconWell(iconRes: Int, modifier: Modifier = Modifier, size: Int = 36) {
    val shape = RoundedCornerShape((size * 0.33f).dp)
    Box(
        modifier
            .size(size.dp)
            .clip(shape)
            .background(RestaurantEmber.ember.copy(alpha = 0.16f))
            .border(1.dp, RestaurantEmber.ember.copy(alpha = 0.45f), shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(iconRes),
            contentDescription = null,
            tint = RestaurantEmber.ember,
            modifier = Modifier.size((size * 0.55f).dp),
        )
    }
}

/** Chip used by the POS / KOT mode rows: filled ember when selected, `#44403c` otherwise. */
@Composable
fun EmberModeChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    locked: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(999.dp)
    val enabled = locked == null
    val click = if (onClick != null && enabled) Modifier.clickable(onClick = onClick) else Modifier
    Text(
        text,
        style = RestaurantEmber.chipLabel(selected).copy(
            color = when {
                locked != null -> RestaurantEmber.textFaint
                selected -> Color.White
                else -> RestaurantEmber.textBody
            },
        ),
        maxLines = 1,
        modifier = modifier
            .clip(shape)
            .background(if (selected) RestaurantEmber.ember else RestaurantEmber.chip)
            .border(
                1.dp,
                when {
                    locked != null -> RestaurantEmber.hairline.copy(alpha = 0.5f)
                    selected -> RestaurantEmber.ember
                    else -> Color.Transparent
                },
                shape,
            )
            .then(click)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

/** Filled ember CTA used in every sheet footer. */
@Composable
fun EmberPrimaryButton(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RestaurantEmber.rRow
    Text(
        label,
        style = RestaurantEmber.storyCtaTitle().copy(color = if (enabled) Color.White else RestaurantEmber.textFaint),
        maxLines = 1,
        modifier = modifier
            .fillMaxWidth()
            .height(51.dp)
            .shadow(
                elevation = if (enabled) 10.dp else 0.dp,
                shape = shape,
                clip = false,
                ambientColor = RestaurantEmber.ember.copy(alpha = 0.45f),
                spotColor = RestaurantEmber.ember.copy(alpha = 0.45f),
            )
            .clip(shape)
            .background(if (enabled) RestaurantEmber.ember else RestaurantEmber.surfaceWell)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 15.dp, horizontal = 12.dp),
    )
}

/** Tile icons (all real Figma vectors in `res/drawable`). */
object RestaurantTileIcons {
    val sale = R.drawable.ic_rest_tile_sale
    val purchase = R.drawable.ic_rest_tile_purchase
    val collection = R.drawable.ic_rest_tile_collection
    val payment = R.drawable.ic_rest_tile_payment
    val expense = R.drawable.ic_rest_tile_expense
    val stock = R.drawable.ic_rest_tile_stock
    val order = R.drawable.ic_rest_tile_order
    val status = R.drawable.ic_rest_tile_status
    val budget = R.drawable.ic_rest_tile_budget
    val vendors = R.drawable.ic_rest_tile_vendors
    val menu = R.drawable.ic_rest_tile_menu
    val stations = R.drawable.ic_rest_tile_stations
    val invite = R.drawable.ic_rest_tile_invite
    val issue = R.drawable.ic_rest_tile_issue
    val hold = R.drawable.qa_shield
    val transfer = R.drawable.qa_truck
    val reprint = R.drawable.qa_receipt
    val heroGlyph = R.drawable.ic_rest_hero_glyph
    val plus = R.drawable.ic_rest_plus
    val tabPulse = R.drawable.ic_rest_tab_pulse
    val tabMoments = R.drawable.ic_rest_tab_moments
    val tabQuickAdd = R.drawable.ic_rest_tab_quickadd
    val tabLife = R.drawable.ic_rest_tab_life
    val tabMemory = R.drawable.ic_rest_tab_memory
    val qrGlyph = R.drawable.ic_rest_qr_glyph
    val qrBg = R.drawable.ic_rest_qr_bg
    val chevronUp = R.drawable.ic_rest_chevron_up
    val statusDot = R.drawable.ic_rest_status_dot
    val activeDot = R.drawable.ic_rest_active_dot
}
