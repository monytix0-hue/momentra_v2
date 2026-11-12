package com.example.momentra.ui.shell.business.service

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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R
import com.example.momentra.ui.theme.ServicesTeal

/**
 * Shared visual primitives for the retail section, matching Figma `2487:37758`.
 * Both the Pulse cards (`#2a1a1f`, 28% rose) and the board cards (`#1e293b`, 40–50% rose)
 * are the same component with a different tone, so they share one implementation.
 */

enum class TealTone { Pulse, Board }

/** 3-stop vertical wash behind every retail screen. */
@Composable
fun TealScreenBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to ServicesTeal.screenTop,
                    0.35f to ServicesTeal.screenMid,
                    1f to ServicesTeal.screenBottom,
                ),
            ),
    ) { content() }
}

/**
 * Card with the Figma double drop shadow:
 * `0 8px 28px -4px rgba(244,63,94,.18)` + `0 2px 6px rgba(0,0,0,.35)`
 * plus the `inset 0 1px 0 rgba(255,255,255,.05)` top highlight.
 */
@Composable
fun TealCard(
    modifier: Modifier = Modifier,
    tone: TealTone = TealTone.Pulse,
    strokeAlpha: Float? = null,
    gap: Int = 14,
    padding: Int = 16,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val fill = if (tone == TealTone.Pulse) ServicesTeal.card else ServicesTeal.card
    val defaultAlpha = if (tone == TealTone.Pulse) 0.28f else 0.40f
    val alpha = strokeAlpha ?: defaultAlpha
    val shape = if (tone == TealTone.Pulse) ServicesTeal.rCard else ServicesTeal.rCard
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
                ambientColor = ServicesTeal.teal.copy(alpha = 0.18f),
                spotColor = ServicesTeal.teal.copy(alpha = 0.18f),
            )
            .clip(shape)
            .background(fill)
            .border(1.dp, ServicesTeal.teal.copy(alpha = alpha), shape)
            .then(innerHighlight())
            .then(interactive)
            .padding(padding.dp),
        verticalArrangement = Arrangement.spacedBy(gap.dp),
        content = { content() },
    )
}

/** 1dp white 5% line pinned to the top edge, clipped by the card shape. */
private fun innerHighlight(): Modifier = Modifier.drawWithContent {
    drawContent()
    drawRect(
        color = Color.White.copy(alpha = 0.05f),
        topLeft = Offset(0f, 0f),
        size = Size(size.width, 1f),
    )
}

/** Screen title + amber role line. */
@Composable
fun ScreenHeader(
    title: String,
    roleLine: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = ServicesTeal.screenTitle(), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(roleLine, style = ServicesTeal.roleLine(), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Filled rose chip + outline chips (`Nov 2026 · INR`, `GST on`). */
@Composable
fun ContextChips(
    subtypeLabel: String,
    periodLine: String,
    gstOn: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTealChip(subtypeLabel)
        OutlineChip(periodLine)
        OutlineChip(if (gstOn) "GST on" else "GST off")
    }
}

@Composable
fun FilledTealChip(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = ServicesTeal.chipLabel(true),
        maxLines = 1,
        modifier = modifier
            .shadow(
                elevation = 6.dp,
                shape = ServicesTeal.rPill,
                clip = false,
                ambientColor = ServicesTeal.tealDeep.copy(alpha = 0.4f),
                spotColor = ServicesTeal.tealDeep.copy(alpha = 0.4f),
            )
            .clip(ServicesTeal.rPill)
            .background(ServicesTeal.teal)
            .border(1.dp, ServicesTeal.teal, ServicesTeal.rPill)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
fun OutlineChip(text: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val click = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Text(
        text,
        style = ServicesTeal.chipLabel(false),
        maxLines = 1,
        modifier = modifier
            .clip(ServicesTeal.rPill)
            .background(ServicesTeal.chip)
            .border(1.dp, ServicesTeal.tileStroke, ServicesTeal.rPill)
            .then(click)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

/** Card header: bold title, optional trailing chip, optional `View all ›`. */
@Composable
fun SectionHeader(
    title: String,
    chipText: String? = null,
    chipColor: Color = ServicesTeal.teal,
    onViewAll: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = ServicesTeal.sectionTitle(),
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        if (chipText != null) FilledTealChip(chipText)
        if (onViewAll != null) {
            Text(
                "View all ›",
                style = ServicesTeal.viewAll(),
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(onClick = onViewAll)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }
    }
}

/** Card header for slate boards: `Sales & Orders` + amber eyebrow on the right of a hairline. */
@Composable
fun BoardHeader(
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
        Text(title, style = ServicesTeal.sectionTitle(), maxLines = 1, modifier = Modifier.weight(1f))
        if (trailingNote != null) {
            Text(trailingNote, style = ServicesTeal.viewAll().copy(color = ServicesTeal.amber), maxLines = 1)
        }
        if (onViewAll != null) {
            Text(
                "View all ›",
                style = ServicesTeal.viewAll(),
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(onClick = onViewAll)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }
    }
}

/** `#170d11` metric chip with an 11/Medium label and an 18/Bold value. */
@Composable
fun MetricChip(
    label: String,
    value: String,
    valueColor: Color = ServicesTeal.textPrimary,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(ServicesTeal.rRow)
            .background(ServicesTeal.surfaceWell)
            .border(1.dp, ServicesTeal.chipStroke, ServicesTeal.rRow)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, style = ServicesTeal.metricLabel(), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, style = ServicesTeal.metricValue(valueColor), maxLines = 1)
    }
}

/** KPI tile that sits inside a slate board card (`rgba(0,0,0,.28)` + 6% white hairline). */
@Composable
fun KpiChip(
    label: String,
    value: String,
    valueColor: Color = ServicesTeal.textPrimary,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(ServicesTeal.rRow)
            .background(ServicesTeal.surfaceWell)
            .border(1.dp, ServicesTeal.chipStroke, ServicesTeal.rRow)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, style = ServicesTeal.kpiBigLabel(), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, style = ServicesTeal.kpiBig(valueColor), maxLines = 1)
    }
}

/** Row with a 3dp coloured left bar (`list row` in the Attention / Open-Commitments cards). */
@Composable
fun AccentRow(
    title: String,
    caption: String,
    accent: Color,
    trailingValue: String? = null,
    trailingColor: Color = ServicesTeal.textPrimary,
    severityLabel: String? = null,
    severityColor: Color = accent,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val click = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Row(
        modifier
            .fillMaxWidth()
            .clip(ServicesTeal.rRow)
            .background(ServicesTeal.surfaceWell)
            .then(
                Modifier.drawWithContent {
                    drawContent()
                    drawRect(
                        color = accent,
                        topLeft = Offset(0f, 0f),
                        size = Size(3.dp.toPx(), size.height),
                    )
                },
            )
            .then(click)
            .padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = ServicesTeal.rowTitle(), maxLines = 2)
            Text(caption, style = ServicesTeal.rowCaption(), maxLines = 2)
        }
        if (trailingValue != null) {
            Text(trailingValue, style = ServicesTeal.rowValue(trailingColor), maxLines = 1)
        }
        if (severityLabel != null) SeverityChip(severityLabel, severityColor)
    }
}

/** Divider row used inside Pulse cards (bottom hairline `#4c0519` at 60%). */
@Composable
fun DividedRow(
    title: String,
    caption: String?,
    trailingValue: String,
    trailingColor: Color = ServicesTeal.textPrimary,
    captionColor: Color = ServicesTeal.textBody,
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
                        color = ServicesTeal.divider,
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
            Text(title, style = ServicesTeal.rowTitle(), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!caption.isNullOrBlank()) {
                Text(caption, style = ServicesTeal.rowCaption(captionColor), maxLines = 1)
            }
        }
        Text(trailingValue, style = ServicesTeal.rowValue(trailingColor), maxLines = 1)
    }
}

/** Tonal severity pill (`Unavailable`, `Overdue`, `Alert`, `Part`, `Open`). */
@Composable
fun SeverityChip(label: String, color: Color, modifier: Modifier = Modifier, bordered: Boolean = true) {
    val shape = RoundedCornerShape(100.dp)
    Text(
        label,
        style = ServicesTeal.severity().copy(color = color),
        maxLines = 1,
        modifier = modifier
            .clip(shape)
            .background(ServicesTeal.tone(color))
            .then(if (bordered) Modifier.border(1.dp, ServicesTeal.toneStroke(color), shape) else Modifier)
            .padding(horizontal = 9.dp, vertical = 4.dp),
    )
}

/** Solid tinted status pill (`● On track`, `Part`) — no border in the design. */
@Composable
fun StatusPill(label: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        label,
        style = ServicesTeal.severity().copy(color = color),
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(ServicesTeal.tone(color))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/** `Why` + body, faded 50% — the honesty rule line under each Pulse card. */
@Composable
fun WhyCaption(body: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = 2.dp, top = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "Why",
            style = ServicesTeal.why().copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = Color.White.copy(alpha = 0.5f)),
            maxLines = 1,
        )
        Text(body, style = ServicesTeal.why(), modifier = Modifier.weight(1f))
    }
}

/**
 * SVC sheet Why note — lime “Why” + cream body on a lime-tinted plate
 * (Figma `why-note` on masters / QA sheets).
 */
@Composable
fun WhyNote(body: String, modifier: Modifier = Modifier, tinted: Boolean = false) {
    val base = if (tinted) {
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(ServicesTeal.lime.copy(alpha = 0.12f))
            .border(1.dp, ServicesTeal.lime.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
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
        Text("Why", style = ServicesTeal.whyLabel(), maxLines = 1)
        Text(body, style = ServicesTeal.why(), modifier = Modifier.weight(1f))
    }
}

/** Sheet grab — 40×4 muted teal bar (Figma SVC masters). */
@Composable
fun TealSheetGrabber(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().height(22.dp), horizontalArrangement = Arrangement.Center) {
        Box(
            Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(ServicesTeal.grab),
        )
    }
}

/** Title + ✕ close + optional subtitle used by every SVC master sheet. */
@Composable
fun TealSheetHeader(
    title: String,
    subtitle: String?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            title,
            style = ServicesTeal.sheetTitle(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Box(
            Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(ServicesTeal.chip)
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Text("✕", color = ServicesTeal.grab, fontSize = 13.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        }
    }
    subtitle?.takeIf { it.isNotBlank() }?.let {
        Text(it, style = ServicesTeal.sheetSubtitle(), maxLines = 2)
    }
}

/**
 * Mode / category chip.
 * - Default (QA modes): selected = teal fill + lime border + dark ink
 * - [limeSelected] (categories / invite-style): selected = lime fill + lime border
 * Idle = deep teal fill + chipBorder + muted label (SVC Figma).
 */
@Composable
fun TealModeChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    limeSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val click = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    val fill = when {
        selected && limeSelected -> ServicesTeal.lime
        selected -> ServicesTeal.teal
        else -> ServicesTeal.chip
    }
    Text(
        text,
        style = ServicesTeal.chipLabel(selected).copy(
            color = if (selected) ServicesTeal.inkOnTeal else ServicesTeal.textBody,
            fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold
            else androidx.compose.ui.text.font.FontWeight.Medium,
        ),
        maxLines = 1,
        modifier = modifier
            .clip(ServicesTeal.rPill)
            .background(fill)
            .border(
                1.dp,
                if (selected) ServicesTeal.lime else ServicesTeal.chipBorder,
                ServicesTeal.rPill,
            )
            .then(click)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

/** Sticky footer CTA — teal→lime gradient + caption (Done / Save …). */
@Composable
fun TealGradientCta(
    label: String,
    caption: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Column(
        modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val shape = ServicesTeal.rCta
        Box(
            Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (enabled) 12.dp else 0.dp,
                    shape = shape,
                    clip = false,
                    ambientColor = ServicesTeal.lime.copy(alpha = 0.55f),
                    spotColor = ServicesTeal.lime.copy(alpha = 0.55f),
                )
                .clip(shape)
                .background(
                    if (enabled) {
                        Brush.horizontalGradient(listOf(ServicesTeal.teal, ServicesTeal.lime))
                    } else {
                        Brush.horizontalGradient(listOf(ServicesTeal.chip, ServicesTeal.chip))
                    },
                )
                .clickable(enabled = enabled, onClick = onClick)
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                label,
                style = ServicesTeal.storyCtaTitle().copy(
                    color = if (enabled) ServicesTeal.inkOnTeal else ServicesTeal.grab,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                ),
                maxLines = 1,
            )
        }
        Text(caption, style = ServicesTeal.footerCaption(), maxLines = 2)
    }
}

/** Saved toast copy locked to SVC Figma. */
@Composable
fun TealSavedToast(modifier: Modifier = Modifier) {
    Text(
        "✓ Saved · sheet closes and returns back",
        style = ServicesTeal.toastSaved(),
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * Health ring from the Figma SVGs: 92dp track (white 8%) + rose progress arc
 * with a 6px rose glow, 8.28pt stroke.
 */
@Composable
fun HealthRing(
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
                val sweep = (fraction.coerceIn(0f, 1f)) * 360f
                drawArc(
                    color = ServicesTeal.teal.copy(alpha = 0.25f),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(inset - 3f, inset - 3f),
                    size = Size(arcSize.width + 6f, arcSize.height + 6f),
                    style = Stroke(width = stroke + 6f, cap = StrokeCap.Round),
                )
                drawArc(
                    color = ServicesTeal.teal,
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
                style = ServicesTeal.heroMetric().copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                    fontSize = 22.sp,
                ),
                maxLines = 1,
            )
            Text(
                caption,
                style = ServicesTeal.rowCaption(Color.White.copy(alpha = 0.55f)).copy(fontSize = 10.sp),
                maxLines = 1,
            )
        }
    }
}

/** Rows inside a board card: a 3dp rose left bar, big caption, trailing status. */
@Composable
fun BoardRow(
    eyebrow: String,
    value: String,
    valueColor: Color,
    trailing: String,
    trailingColor: Color,
    accent: Color = ServicesTeal.teal,
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
                .clip(ServicesTeal.rRow)
                .background(ServicesTeal.surfaceWell)
                .border(1.dp, ServicesTeal.chipStroke, ServicesTeal.rRow)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(eyebrow, style = ServicesTeal.kpiBigLabel(), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(value, style = ServicesTeal.kpiBig(valueColor), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(trailing, style = ServicesTeal.rowValue(trailingColor), maxLines = 1)
        if (severity != null && severityColor != null) {
            SeverityChip(severity, severityColor, bordered = false)
        }
    }
}

/** Board hero used by Moments / Life / Memory. */
@Composable
fun BoardHero(
    eyebrow: String,
    headline: String,
    chips: @Composable () -> Unit,
    kpis: @Composable () -> Unit,
    strokeAlpha: Float = 0.45f,
    modifier: Modifier = Modifier,
) {
    TealCard(modifier = modifier, tone = TealTone.Board, strokeAlpha = strokeAlpha, gap = 14, padding = 16) {
        Text(eyebrow, style = ServicesTeal.boardEyebrow(), maxLines = 1)
        Text(headline, style = ServicesTeal.boardHeadline(22), maxLines = 2, overflow = TextOverflow.Ellipsis)
        chips()
        kpis()
    }
}

/** Small hero chip (Retail / Store open / vs Oct 2026). */
@Composable
fun BoardHeroChip(
    text: String,
    color: Color,
    dot: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(100.dp))
            .background(ServicesTeal.tone(color))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(100.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dot) {
            Canvas(Modifier.size(6.dp)) { drawCircle(color) }
        }
        Text(text, style = ServicesTeal.boardChip().copy(color = color), maxLines = 1)
    }
}

/** Teal CTA used by the Quick Add hero (`+ Finalize Charge`). */
@Composable
fun TealCta(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = true,
) {
    val shape = RoundedCornerShape(16.dp)
    // SVC-HUB hero CTA is cream `#FEF3C7` with teal ink (not white).
    val cream = ServicesTeal.whyBody
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (filled) cream else cream.copy(alpha = 0.12f))
            .then(
                if (filled) Modifier.shadow(
                    elevation = 10.dp,
                    shape = shape,
                    clip = false,
                    ambientColor = ServicesTeal.lime.copy(alpha = 0.6f),
                    spotColor = ServicesTeal.lime.copy(alpha = 0.6f),
                ) else Modifier,
            )
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = ServicesTeal.storyCtaTitle().copy(
                color = ServicesTeal.teal,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            ),
            maxLines = 1,
        )
    }
}

/** Section label + trailing note over the Quick Add grids (`Core · 9` … `Service journey`). */
@Composable
fun GridSectionHeader(title: String, note: String, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = ServicesTeal.sectionTitle(), maxLines = 1)
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.08f)),
        )
        Text(
            note,
            // Figma hub trailing notes use body teal `#99F6E4`, not amber.
            style = ServicesTeal.rowCaption(ServicesTeal.textBody).copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium).copy(fontSize = 11.sp),
            maxLines = 1,
        )
    }
}

/** A Quick Add tile: 112dp tall, 32dp icon well, 14/Bold label, 10/Regular caption. */
@Composable
fun QuickAddTile(
    iconRes: Int,
    label: String,
    caption: String,
    locked: String?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(16.dp)
    val click = if (onClick != null && locked == null) Modifier.clickable(onClick = onClick) else Modifier
    Column(
        modifier
            .height(112.dp)
            .shadow(
                elevation = 8.dp,
                shape = shape,
                clip = false,
                ambientColor = ServicesTeal.teal.copy(alpha = 0.15f),
                spotColor = ServicesTeal.teal.copy(alpha = 0.15f),
            )
            .clip(shape)
            .background(ServicesTeal.tile)
            .border(1.dp, if (locked == null) ServicesTeal.tileStroke else Color.White.copy(alpha = 0.08f), shape)
            .then(click)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(ServicesTeal.tone(ServicesTeal.teal))
                    .border(1.dp, ServicesTeal.toneStroke(ServicesTeal.teal), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(iconRes),
                    contentDescription = null,
                    tint = if (locked == null) ServicesTeal.teal else ServicesTeal.textFaint,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.weight(1f))
        }
        Text(
            label,
            style = ServicesTeal.tileLabel().copy(color = if (locked == null) ServicesTeal.textPrimary else ServicesTeal.textMuted),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            locked ?: caption,
            style = ServicesTeal.tileCaption().copy(color = if (locked == null) ServicesTeal.textMuted else ServicesTeal.amber.copy(alpha = 0.8f)),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Empty state in the rose language — never shows a fabricated `0%`. */
@Composable
fun TealEmpty(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    glyph: String = "◇",
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(ServicesTeal.rRow)
            .background(ServicesTeal.surfaceWell)
            .border(1.dp, Color.White.copy(alpha = 0.06f), ServicesTeal.rRow)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(glyph, color = ServicesTeal.teal, fontSize = 18.sp)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = ServicesTeal.rowTitle())
            Text(body, style = ServicesTeal.rowCaption())
        }
    }
}

/** Thin rose hairline used between stacked board rows. */
@Composable
fun TealDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(ServicesTeal.divider),
    )
}

/** Icon well used by sheet headers: 32dp rounded rose tinted square. */
@Composable
fun SheetIconWell(iconRes: Int, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ServicesTeal.tone(ServicesTeal.teal))
            .border(1.dp, ServicesTeal.toneStroke(ServicesTeal.teal), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(iconRes), contentDescription = null, tint = ServicesTeal.teal, modifier = Modifier.size(20.dp))
    }
}

/** Standard tile icons (all real Figma vectors in res/drawable). */
object ServiceTileIcons {
    val sale = R.drawable.ic_retail_tile_sale
    val purchase = R.drawable.ic_retail_tile_purchase
    val collection = R.drawable.ic_retail_tile_collection
    val payment = R.drawable.ic_retail_tile_payment
    val expense = R.drawable.ic_retail_tile_expense
    val stock = R.drawable.ic_retail_tile_stock
    val order = R.drawable.ic_retail_tile_order
    val status = R.drawable.ic_retail_tile_status
    val budget = R.drawable.ic_retail_tile_budget
    val suppliers = R.drawable.ic_retail_tile_suppliers
    val catalogue = R.drawable.ic_retail_tile_catalogue
    val locations = R.drawable.ic_retail_tile_locations
    val invite = R.drawable.ic_retail_tile_invite
    val issue = R.drawable.ic_retail_tile_issue
}
