package com.example.momentra.ui.shell.business.shared

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.OperatingActivityDto
import com.example.momentra.data.api.OperatingBoardMetricDto
import com.example.momentra.data.api.OperatingBoardSectionDto
import com.example.momentra.data.api.OperatingPaymentDto
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.theme.PlusJakartaSans

/**
 * Shared palette for the Trading and Distribution view-all surfaces. Both verticals
 * render the same sheet skeleton and differ only in accent and copy, exactly as the
 * Figma TRD-SHEET / DST-SHEET frames do.
 */
data class OperatingSheetTheme(
    val accent: Color,
    val accentSoft: Color,
    val canvas: Color,
    val surface: Color,
    val inset: Color,
    val border: Color,
    val ink: Color,
    val body: Color,
    val warning: Color,
    val negative: Color,
)

internal val OperatingSheetWarning = Color(0xFFF59E0B)
internal val OperatingSheetNegative = Color(0xFFF87171)

/**
 * One view-all surface: the section it reads, the Figma title, and the sentence shown
 * when nothing has been recorded yet.
 */
data class OperatingSheetSpec(
    val code: String,
    val title: String,
    val eyebrow: String,
    val emptyTitle: String,
    val emptyBody: String,
    val pulseSurface: Boolean = false,
)

/** Which pulse surface a sheet reads when it is not backed by a board section. */
enum class OperatingPulseSurface { TARGET_BUDGET, FINANCE, PAYMENT_READINESS, ATTENTION, ACTIVITY, GST }

@Composable
fun OperatingSheetFrame(
    title: String,
    eyebrow: String,
    model: OperatingProjectionDto,
    theme: OperatingSheetTheme,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    OperatingSheetScrim(
        onDismiss = onDismiss,
        accent = theme.accent,
        surface = theme.canvas,
        border = theme.border,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 17.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth().height(22.dp), horizontalArrangement = Arrangement.Center) {
                Box(
                    Modifier
                        .width(40.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    title,
                    color = theme.ink,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 24.sp,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "Close",
                    color = theme.body,
                    fontFamily = PlusJakartaSans,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
            if (eyebrow.isNotBlank()) {
                Text(eyebrow, color = theme.body, fontFamily = PlusJakartaSans, fontSize = 12.sp)
            }
            content()
            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * The quantified tiles a section carries. A missing measurement is rendered verbatim
 * as `Not recorded` — never as a zero the operator could mistake for a real reading.
 */
@Composable
fun OperatingMetricTiles(
    metrics: List<OperatingBoardMetricDto>,
    theme: OperatingSheetTheme,
    columns: Int = 2,
) {
    if (metrics.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        metrics.chunked(columns.coerceAtLeast(1)).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { tile ->
                    OperatingMetricTile(tile, theme, Modifier.weight(1f))
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun OperatingMetricTile(
    tile: OperatingBoardMetricDto,
    theme: OperatingSheetTheme,
    modifier: Modifier = Modifier,
) {
    val tone = when (tile.tone) {
        "WARNING" -> theme.warning
        "POSITIVE" -> theme.accentSoft
        else -> theme.ink
    }
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(theme.inset)
            .border(1.dp, theme.border, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(tile.label, color = theme.body, fontFamily = PlusJakartaSans, fontSize = 11.sp)
        Text(
            if (tile.unit.isNullOrBlank() || tile.value == "Not recorded") tile.value else "${tile.value} ${tile.unit}",
            color = tone,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
        )
    }
}

/** The Figma `Why: …` footnote. It explains a gap and never restates the tiles. */
@Composable
fun OperatingWhyFootnote(text: String?, theme: OperatingSheetTheme) {
    val body = text?.takeIf { it.isNotBlank() } ?: return
    Text("Why: $body", color = theme.body, fontFamily = PlusJakartaSans, fontSize = 12.sp)
}

/** `Open` / `Part` / `Overdue` / `Settled` / `Done` / `Transit` / `On hold`. */
@Composable
fun OperatingBadgeChip(badge: String?, theme: OperatingSheetTheme) {
    val label = badge?.takeIf { it.isNotBlank() } ?: return
    val tone = when (label) {
        "Overdue", "Cancelled" -> theme.negative
        "On hold", "Open", "Approval", "Part", "Transit" -> theme.warning
        "Settled", "Delivered", "Done" -> theme.accentSoft
        else -> theme.body
    }
    Text(
        label,
        color = tone,
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .border(1.dp, tone.copy(alpha = 0.5f), RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/** One board row with its status chip and optional amount. */
@Composable
fun OperatingBoardRowRow(
    row: OperatingActivityDto,
    model: OperatingProjectionDto,
    theme: OperatingSheetTheme,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable { OperatingChromeState.editing = row }
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                row.title.ifBlank { row.eventType.lowercase().replaceFirstChar { it.uppercase() } },
                color = theme.ink,
                fontFamily = PlusJakartaSans,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f),
            )
            OperatingBadgeChip(row.badge, theme)
        }
        val subtitle = row.subtitle?.takeIf { it.isNotBlank() }
            ?: listOfNotNull(row.counterpartyName, row.channelName, row.quantity?.let { q ->
                listOfNotNull(q, row.unit).joinToString(" ")
            }).filter { it.isNotBlank() }.joinToString(" · ")
        if (subtitle.isNotBlank()) {
            Text(subtitle, color = theme.body, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        }
        if (model.financeVisible && !row.amount.isNullOrBlank()) {
            Text(
                formatOperatingAmount(model.currency, row.amount, true),
                color = theme.accent,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
        }
    }
}

/**
 * The body of a view-all sheet for one board section. Tiles come from `metrics`, the
 * gap explanation from `why`, and the rows from the event list — nothing is parsed
 * out of the prose `detail` string.
 */
@Composable
fun OperatingBoardSectionBody(
    section: OperatingBoardSectionDto?,
    spec: OperatingSheetSpec,
    model: OperatingProjectionDto,
    theme: OperatingSheetTheme,
) {
    if (section == null) {
        BusinessEmptyState(
            title = spec.emptyTitle,
            body = spec.emptyBody,
            accent = theme.accent,
            glyph = "📋",
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OperatingMetricTiles(section.metrics, theme)
        OperatingWhyFootnote(section.why, theme)
        if (section.rows.isEmpty()) {
            BusinessEmptyState(
                title = spec.emptyTitle,
                body = spec.emptyBody,
                accent = theme.accent,
                glyph = "📋",
                compact = true,
            )
        } else {
            Column {
                section.rows.forEach { row -> OperatingBoardRowRow(row, model, theme) }
            }
        }
    }
}

/** Tiles for one of the six Pulse surfaces. */
@Composable
fun OperatingPulseTiles(
    surface: OperatingPulseSurface,
    model: OperatingProjectionDto,
    theme: OperatingSheetTheme,
) {
    val group = model.pulseMetrics?.firstOrNull { it.code == surface.name }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OperatingMetricTiles(group?.metrics ?: emptyList(), theme)
        OperatingWhyFootnote(group?.why, theme)
    }
}

@Composable
fun OperatingPaymentRows(
    rows: List<OperatingPaymentDto>,
    model: OperatingProjectionDto,
    theme: OperatingSheetTheme,
) {
    if (rows.isEmpty()) {
        BusinessEmptyState(
            title = "Nothing awaiting settlement",
            body = "Open balances appear here as they are recorded.",
            accent = theme.accent,
            glyph = "💸",
            compact = true,
        )
        return
    }
    Column {
        rows.forEach { item ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable { OperatingChromeState.settling = item }
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.title.ifBlank { item.counterpartyName ?: "Open balance" },
                        color = theme.ink,
                        fontFamily = PlusJakartaSans,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f),
                    )
                    OperatingBadgeChip(
                        when (item.dueBucket) {
                            "OVERDUE" -> "Overdue"
                            "DUE_SOON" -> "Due soon"
                            "OPEN" -> "Open"
                            else -> null
                        },
                        theme,
                    )
                }
                val meta = listOfNotNull(
                    item.counterpartyName?.takeIf { it.isNotBlank() },
                    item.dueAt?.take(10)?.let { "Due $it" },
                ).joinToString(" · ")
                if (meta.isNotBlank()) {
                    Text(meta, color = theme.body, fontFamily = PlusJakartaSans, fontSize = 12.sp)
                }
                if (model.financeVisible) {
                    Text(
                        formatOperatingAmount(model.currency, item.amount, true),
                        color = theme.accent,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}

/** Resolve a section anywhere in the projection, by code. */
fun OperatingProjectionDto.sectionByCode(code: String): OperatingBoardSectionDto? =
    (boards?.moments.orEmpty() + boards?.life.orEmpty() + boards?.memory.orEmpty())
        .firstOrNull { it.code == code }
