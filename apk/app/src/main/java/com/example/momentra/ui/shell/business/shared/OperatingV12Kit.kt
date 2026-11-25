package com.example.momentra.ui.shell.business.shared

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R
import com.example.momentra.data.api.OperatingBoardMetricDto
import com.example.momentra.data.api.OperatingBoardSectionDto
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.theme.PlusJakartaSans
import com.example.momentra.ui.theme.generated.BusinessTokens
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min

/**
 * Shared TRD/DST v12 widget kit (`TRD-PULSE-v12` … `TRD-HUB-v11`).
 *
 * Mirrors iOS `OperatingV12Kit.swift`: same palette, money register, card chrome,
 * tiles, severity rows, and heroes. Verticals differ only by [OperatingV12Theme].
 */
object OperatingV12 {
    val surface = Color(0xFF151D2E)
    val pulseSurface = Color(0xFF111827)
    val title = Color(0xFFF1F5F9)
    val sectionTitle = Color(0xFFF8FAFC)
    val subtitle = Color(0xFF94A3B8)
    val viewAll = Color(0xFF34D399)
    val cyan = Color(0xFF06B6D4)
    val green = Color(0xFF22C55E)
    val amber = Color(0xFFF59E0B)
    val red = Color(0xFFEF4444)
    val deepInk = Color(0xFF04110C)
    val hairline = Color.White.copy(alpha = 0.06f)
    val inset = Color.Black.copy(alpha = 0.28f)
    val row = Color.Black.copy(alpha = 0.24f)

    private val inflowKeys = setOf(
        "progress", "toCollect", "collected", "sales", "netSales", "committed", "repeat", "delivered",
    )
    private val flightKeys = setOf(
        "outstanding", "credits", "tax", "net", "inTransit", "months", "records",
    )
    private val pressureKeys = setOf(
        "payable", "toPay", "difference", "issues", "overdue", "low", "returns",
    )

    fun tone(tone: String?, key: String, accent: Color): Color = when {
        key in inflowKeys -> accent
        key in flightKeys -> cyan
        key in pressureKeys -> red
        else -> when ((tone ?: "NORMAL").uppercase()) {
            "POSITIVE" -> accent
            "WARNING" -> amber
            else -> title
        }
    }

    fun bar(badge: String?): Color = when (badge) {
        "Overdue", "Cancelled", "Rejected" -> red
        "Unavailable", "Part", "Due", "On hold", "Transit", "Open order" -> amber
        "Settled", "Done", "Delivered", "Alert", "Open", "Pattern" -> green
        else -> cyan
    }
}

/** Vertical palette + copy for Trading / Distribution / Manufacturing / MTO. */
data class OperatingV12Theme(
    val accent: Color,
    val canvas: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val label: String,
    val partyNoun: String,
    val motionLine: String,
    val isTrading: Boolean,
    /** `trading` | `distribution` | `manufacturing` | `madeToOrder` */
    val verticalKey: String = if (isTrading) "trading" else "distribution",
    /** Pulse section card fill — Own plum `#2A1030`; others keep kit default. */
    val surface: Color = OperatingV12.pulseSurface,
    /** Canvas gradient start wash — Own `#1E1B4B` → [canvas]. */
    val canvasTop: Color = canvas,
    /** Metric / list inset — Own `#18081C`. */
    val inset: Color = OperatingV12.inset,
    /** Soft link / View all — Own `#E879F9`; others keep emerald kit default. */
    val softLink: Color = OperatingV12.viewAll,
) {
    val isManufacturing: Boolean
        get() = verticalKey == "manufacturing" || verticalKey == "madeToOrder"
    val isMadeToOrder: Boolean
        get() = verticalKey == "madeToOrder"

    companion object {
        fun trading() = tradingForScreen(OperatingTab.PULSE)

        /** Per-tab Trading emerald palette from `color.domain.trading.screens.*` tokens. */
        fun tradingForScreen(tab: OperatingTab): OperatingV12Theme {
            val c = BusinessTokens.Colors
            val (canvas, surface, heroStart, heroEnd) = when (tab) {
                OperatingTab.PULSE -> ScreenPalette(
                    c.TRADINGPulseCanvas, c.TRADINGPulseSurface,
                    c.TRADINGPulseHeroStart, c.TRADINGPulseHeroEnd,
                )
                OperatingTab.MOMENTS -> ScreenPalette(
                    c.TRADINGMomentsCanvas, c.TRADINGMomentsSurface,
                    c.TRADINGMomentsHeroStart, c.TRADINGMomentsHeroEnd,
                )
                OperatingTab.LIFE -> ScreenPalette(
                    c.TRADINGLifeCanvas, c.TRADINGLifeSurface,
                    c.TRADINGLifeHeroStart, c.TRADINGLifeHeroEnd,
                )
                OperatingTab.MEMORY -> ScreenPalette(
                    c.TRADINGMemoryCanvas, c.TRADINGMemorySurface,
                    c.TRADINGMemoryHeroStart, c.TRADINGMemoryHeroEnd,
                )
                OperatingTab.QUICK_ADD -> ScreenPalette(
                    c.TRADINGQuickAddCanvas, c.TRADINGQuickAddSurface,
                    c.TRADINGQuickAddHeroStart, c.TRADINGQuickAddHeroEnd,
                )
            }
            return OperatingV12Theme(
                accent = c.TRADINGAccent,
                canvas = canvas,
                heroStart = heroStart,
                heroEnd = heroEnd,
                label = "Trading",
                partyNoun = "Buyers",
                motionLine = "Source. Sell. Collect.",
                isTrading = true,
                verticalKey = "trading",
                surface = surface,
            )
        }

        private data class ScreenPalette(
            val canvas: Color,
            val surface: Color,
            val heroStart: Color,
            val heroEnd: Color,
        )

        fun distribution() = distributionForScreen(OperatingTab.PULSE)

        /** Per-tab Distribution orange palette from `color.domain.distribution.screens.*` tokens. */
        fun distributionForScreen(tab: OperatingTab): OperatingV12Theme {
            val c = BusinessTokens.Colors
            val (canvas, surface, heroStart, heroEnd) = when (tab) {
                OperatingTab.PULSE -> ScreenPalette(
                    c.DISTRIBUTIONPulseCanvas, c.DISTRIBUTIONPulseSurface,
                    c.DISTRIBUTIONPulseHeroStart, c.DISTRIBUTIONPulseHeroEnd,
                )
                OperatingTab.MOMENTS -> ScreenPalette(
                    c.DISTRIBUTIONMomentsCanvas, c.DISTRIBUTIONMomentsSurface,
                    c.DISTRIBUTIONMomentsHeroStart, c.DISTRIBUTIONMomentsHeroEnd,
                )
                OperatingTab.LIFE -> ScreenPalette(
                    c.DISTRIBUTIONLifeCanvas, c.DISTRIBUTIONLifeSurface,
                    c.DISTRIBUTIONLifeHeroStart, c.DISTRIBUTIONLifeHeroEnd,
                )
                OperatingTab.MEMORY -> ScreenPalette(
                    c.DISTRIBUTIONMemoryCanvas, c.DISTRIBUTIONMemorySurface,
                    c.DISTRIBUTIONMemoryHeroStart, c.DISTRIBUTIONMemoryHeroEnd,
                )
                OperatingTab.QUICK_ADD -> ScreenPalette(
                    c.DISTRIBUTIONQuickAddCanvas, c.DISTRIBUTIONQuickAddSurface,
                    c.DISTRIBUTIONQuickAddHeroStart, c.DISTRIBUTIONQuickAddHeroEnd,
                )
            }
            return OperatingV12Theme(
                accent = c.DISTRIBUTIONAccent,
                canvas = canvas,
                heroStart = heroStart,
                heroEnd = heroEnd,
                label = "Distribution",
                partyNoun = "Dealers",
                motionLine = "Source. Move. Settle.",
                isTrading = false,
                verticalKey = "distribution",
                surface = surface,
                canvasTop = c.DISTRIBUTIONCanvasTop,
                inset = c.DISTRIBUTIONInset,
                softLink = c.DISTRIBUTIONAccentSoft,
            )
        }

        fun manufacturing() = OperatingV12Theme(
            accent = Color(0xFFF59E0B),
            canvas = Color(0xFF18181B),
            // Domain manufacturing.heroStart / heroEnd (MFG-PULSE-v12)
            heroStart = Color(0xFFF59E0B),
            heroEnd = Color(0xFFB45309),
            label = "Manufacturing",
            partyNoun = "Buyers",
            motionLine = "Plan. Make. Fulfil.",
            isTrading = false,
            verticalKey = "manufacturing",
        )

        fun madeToOrder() = OperatingV12Theme(
            accent = Color(0xFFF59E0B),
            canvas = Color(0xFF18181B),
            heroStart = Color(0xFFFBBF24),
            heroEnd = Color(0xFFF59E0B),
            label = "Make to order",
            partyNoun = "Customers",
            motionLine = "Quote. Confirm. Make. Invoice.",
            isTrading = false,
            verticalKey = "madeToOrder",
        )

        /** Client campaign — indigo `#6366F1` (MKT-PULSE-v12); canvas `#0F172A` from business.tokens. */
        fun marketingClient() = OperatingV12Theme(
            accent = Color(0xFF6366F1),
            canvas = Color(0xFF0F172A),
            heroStart = Color(0xFF6366F1),
            heroEnd = Color(0xFF8B5CF6),
            label = "Client Campaign",
            partyNoun = "Clients",
            motionLine = "Plan. Launch. Measure.",
            isTrading = false,
            verticalKey = "marketingClient",
            canvasTop = Color(0xFF1E1B4B),
        )

        /**
         * Own business campaign — magenta `#D946EF` → amber `#F59E0B` (MKT-OWN-PULSE-v12 `2582:29187`).
         * Canvas wash `#1E1B4B` → `#0F172A` is shared dark indigo, not Client accent.
         */
        fun marketingOwn() = OperatingV12Theme(
            accent = Color(0xFFD946EF),
            canvas = Color(0xFF0F172A),
            heroStart = Color(0xFFD946EF),
            heroEnd = Color(0xFFF59E0B),
            label = "Own Business Campaign",
            partyNoun = "Vendors",
            motionLine = "Plan. Launch. Measure.",
            isTrading = false,
            verticalKey = "marketingOwn",
            surface = Color(0xFF2A1030),
            canvasTop = Color(0xFF1E1B4B),
            inset = Color(0xFF18081C),
            softLink = Color(0xFFE879F9),
        )

        fun forSubtype(subtype: String?): OperatingV12Theme = when (subtype?.uppercase()) {
            "DISTRIBUTION_DEALERSHIP" -> distribution()
            "STANDARD_PRODUCTION" -> manufacturing()
            "MADE_TO_ORDER" -> madeToOrder()
            "CLIENT_CAMPAIGN" -> marketingClient()
            "OWN_BUSINESS_CAMPAIGN" -> marketingOwn()
            else -> trading()
        }
    }

    val isMarketing: Boolean
        get() = verticalKey == "marketingClient" || verticalKey == "marketingOwn"

    val isMarketingOwn: Boolean
        get() = verticalKey == "marketingOwn"
}

/**
 * Money display for populated boards.
 * Default [grouped]: Indian commas + max 2 decimals (`₹9,40,000`, `₹86,000.50`).
 * [compact] kept for rare L/K/Cr shorthand call sites.
 */
object OperatingV12Money {
    fun parse(raw: String?, currency: String? = null): Double? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return null
        val code = (currency ?: "INR").uppercase()
        val numeric = value
            .replace(code, "", ignoreCase = true)
            .replace("₹", "")
            .replace(",", "")
            .replace("−", "-")
            .trim()
        return numeric.toDoubleOrNull()
    }

    fun compact(raw: String?, currency: String?): String {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return "—"
        val amount = parse(value, currency) ?: return value
        return compact(amount, currency)
    }

    fun compact(amount: Double, currency: String?): String {
        val code = (currency ?: "INR").uppercase()
        val symbol = if (code == "INR") "₹" else "$code "
        val magnitude = abs(amount)
        val sign = if (amount < 0) "−" else ""
        fun trim(text: String) = if (text.endsWith(".0")) text.dropLast(2) else text
        return when {
            magnitude == 0.0 -> "${symbol}0"
            magnitude < 1_000 -> symbol + sign + String.format(Locale.US, "%.0f", magnitude)
            magnitude < 100_000 -> symbol + sign + trim(String.format(Locale.US, "%.1fK", magnitude / 1_000))
            magnitude < 10_000_000 -> symbol + sign + trim(String.format(Locale.US, "%.1fL", magnitude / 100_000))
            else -> symbol + sign + trim(String.format(Locale.US, "%.1fCr", magnitude / 10_000_000))
        }
    }

    fun grouped(amount: Double, currency: String?): String {
        val code = (currency ?: "INR").uppercase()
        val symbol = if (code == "INR") "₹" else "$code "
        val sign = if (amount < 0) "−" else ""
        val magnitude = abs(amount)
        return symbol + sign + indianGroupedNumber(magnitude)
    }

    /** Indian grouping (`##,##,###.##`) with at most 2 fraction digits — JVM-locale independent. */
    internal fun indianGroupedNumber(magnitude: Double): String {
        val hasFraction = abs(magnitude - magnitude.toLong().toDouble()) > 1e-9
        val scaled = if (hasFraction) {
            String.format(Locale.US, "%.2f", magnitude)
        } else {
            String.format(Locale.US, "%.0f", magnitude)
        }
        val parts = scaled.split('.', limit = 2)
        val intPart = parts[0]
        val frac = parts.getOrNull(1)
        val grouped = buildString {
            val digits = intPart
            if (digits.length <= 3) {
                append(digits)
            } else {
                val last3 = digits.takeLast(3)
                var rest = digits.dropLast(3)
                val chunks = ArrayList<String>()
                while (rest.length > 2) {
                    chunks.add(0, rest.takeLast(2))
                    rest = rest.dropLast(2)
                }
                if (rest.isNotEmpty()) chunks.add(0, rest)
                append(chunks.joinToString(","))
                append(',')
                append(last3)
            }
        }
        return if (frac != null) "$grouped.$frac" else grouped
    }

    fun grouped(raw: String?, currency: String?): String {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return "—"
        val amount = parse(value, currency) ?: return value
        return grouped(amount, currency)
    }

    fun number(raw: String?): Double? = parse(raw)

    fun amount(raw: String?, currency: String?): String = grouped(raw, currency)

    fun tile(metric: OperatingBoardMetricDto, currency: String?): String {
        val isCount = metric.unit.equals("count", ignoreCase = true)
        val text = if (isCount) {
            val raw = metric.value?.trim().orEmpty()
            if (raw.isEmpty()) "—"
            else parse(raw, currency)?.let { indianGroupedNumber(abs(it)) } ?: raw
        } else {
            grouped(metric.value, currency)
        }
        val unit = metric.unit?.takeIf { it.isNotBlank() && !isCount }
        return if (unit == null) text else "$text $unit"
    }
}

object OperatingV12Date {
    private val day = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private fun parse(raw: String?): LocalDate? =
        raw?.takeIf { it.length >= 10 }?.let { runCatching { LocalDate.parse(it.take(10), day) }.getOrNull() }

    fun short(raw: String?): String? =
        parse(raw)?.format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH))

    fun monthYear(raw: String?): String? =
        parse(raw)?.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH))

    fun monthDayYear(raw: String?): String? =
        parse(raw)?.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))

    fun today(): String =
        LocalDate.now().format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))

    fun fiscalYear(start: String?): String {
        val year = start?.take(4)?.toIntOrNull() ?: return "FY 2026–27"
        return "FY $year–${String.format("%02d", (year + 1) % 100)}"
    }
}

object OperatingV12Metric {
    fun value(section: OperatingBoardSectionDto?, key: String, currency: String?): String? {
        val metric = section?.metrics?.firstOrNull { it.key == key } ?: return null
        return OperatingV12Money.tile(metric, currency)
    }
}

object OperatingV12Tiles {
    fun ordered(
        section: OperatingBoardSectionDto?,
        keys: List<String>,
        limit: Int,
        currency: String?,
    ): List<OperatingBoardMetricDto> {
        val metrics = section?.metrics.orEmpty()
        if (metrics.isEmpty()) return emptyList()
        val picked = mutableListOf<OperatingBoardMetricDto>()
        keys.forEach { key -> metrics.firstOrNull { it.key == key }?.let { picked += it } }
        metrics.forEach { metric ->
            if (picked.none { it.key == metric.key }) picked += metric
        }
        return picked.take(limit)
    }
}

fun OperatingProjectionDto.pulseGroup(code: String) =
    pulseMetrics?.firstOrNull { it.code == code }

/**
 * True when Pulse has setup or live signal worth showing (budget / target / GST /
 * feeds) instead of the Obsidian "No pulse yet" takeover. Fresh ACTIVE moments with
 * setup prefs must keep the normal Pulse chrome even before the first sale.
 */
fun OperatingProjectionDto.hasUsefulPulseChrome(): Boolean {
    if (activity.isNotEmpty() || attention.isNotEmpty()) return true
    if (paymentReadiness.isNotEmpty()) return true
    if (budget?.configured == true) return true
    if (target?.configured == true) return true
    if (gstTracking.equals("YES", ignoreCase = true)) return true
    return false
}

@Composable
fun OperatingV12Card(
    theme: OperatingV12Theme,
    modifier: Modifier = Modifier,
    fill: Color = OperatingV12.surface,
    borderOpacity: Float = 0.28f,
    radius: Dp = 20.dp,
    padding: Dp = 16.dp,
    spacing: Dp = 14.dp,
    content: @Composable () -> Unit,
) {
    // Twin shadow locked for TRD/DST-v12 Figma chrome:
    // elevation 12, black ambient 0.35, Trading accent glow 0.18 / Dist 0.32, default radius 20.
    val glow = if (theme.isTrading) 0.18f else 0.32f
    val shape = RoundedCornerShape(radius)
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.35f),
                spotColor = theme.accent.copy(alpha = glow),
            )
            .border(1.dp, theme.accent.copy(alpha = borderOpacity), shape),
        shape = shape,
        colors = CardDefaults.elevatedCardColors(containerColor = fill)
    ) {
        Column(
            modifier = Modifier.padding(padding),
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) {
            content()
        }
    }
}

@Composable
fun OperatingV12Chip(
    label: String,
    tone: Color = OperatingV12.subtitle,
    dot: Boolean = false,
) {
    Row(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(tone.copy(alpha = 0.16f))
            .border(1.dp, tone.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (dot) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(tone))
        }
        Text(label, color = tone, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, maxLines = 1)
    }
}

@Composable
fun OperatingV12StatusChip(label: String, accent: Color = OperatingV12.green) {
    val tone = when (label) {
        "Overdue", "Cancelled", "Rejected" -> OperatingV12.red
        "Unavailable", "Part", "Due", "On hold", "Open order", "Transit" -> OperatingV12.amber
        "Settled", "Done", "Delivered", "Alert", "Open", "Pattern" -> accent
        else -> OperatingV12.subtitle
    }
    Text(
        label,
        color = tone,
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(tone.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
fun OperatingV12WidgetHeader(
    title: String,
    subtitle: String?,
    onViewAll: (() -> Unit)? = null,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .then(if (onViewAll != null) Modifier.clickable(onClick = onViewAll) else Modifier),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, color = OperatingV12.title, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        if (!subtitle.isNullOrBlank()) {
            Text(subtitle, color = OperatingV12.subtitle, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Medium, fontSize = 12.sp)
        }
        if (onViewAll != null) {
            Text(
                "View all ›",
                color = OperatingV12.viewAll,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
fun OperatingV12Tile(
    label: String,
    value: String,
    tone: Color = OperatingV12.title,
    fill: Color = OperatingV12.inset,
    radius: Dp = 12.dp,
    labelTone: Color = OperatingV12.subtitle,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(radius)
    Surface(
        modifier = modifier.border(1.dp, OperatingV12.hairline, shape),
        shape = shape,
        color = fill,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(label, color = labelTone, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Medium, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(value, color = tone, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun OperatingV12TileGrid(
    theme: OperatingV12Theme,
    metrics: List<OperatingBoardMetricDto>,
    columns: Int = 3,
    currency: String?,
    fill: Color = OperatingV12.inset,
) {
    if (metrics.isEmpty()) return
    val width = columns.coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        metrics.chunked(width).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { metric ->
                    OperatingV12Tile(
                        label = metric.label,
                        value = OperatingV12Money.tile(metric, currency),
                        tone = OperatingV12.tone(metric.tone, metric.key, theme.accent),
                        fill = fill,
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(width - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun OperatingV12Row(
    title: String,
    subtitle: String? = null,
    trailing: String? = null,
    chip: String? = null,
    bar: Color = OperatingV12.cyan,
    fill: Color = OperatingV12.row,
    accent: Color = OperatingV12.green,
    onClick: (() -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(fill)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = 12.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.width(3.dp).height(28.dp).clip(RoundedCornerShape(2.dp)).background(bar))
        Box(Modifier.size(6.dp).clip(CircleShape).background(bar))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = OperatingV12.title, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 2)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, color = OperatingV12.subtitle, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Medium, fontSize = 11.sp, maxLines = 2)
            }
        }
        if (!trailing.isNullOrBlank()) {
            Text(trailing, color = OperatingV12.viewAll, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
        }
        if (!chip.isNullOrBlank()) {
            OperatingV12StatusChip(chip, accent)
        }
    }
}

@Composable
fun OperatingV12QuietRow(title: String, bar: Color = OperatingV12.green) {
    OperatingV12Row(title = title, bar = bar)
}

@Composable
fun OperatingV12Why(text: String?, tone: Color = OperatingV12.cyan) {
    val body = text?.trim().orEmpty()
    if (body.isEmpty()) return
    Text(
        if (body.startsWith("Why:")) body else "Why: $body",
        color = tone.copy(alpha = 0.6f),
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun OperatingV12QuickActions(
    theme: OperatingV12Theme,
    tinted: Boolean = false,
    radius: Dp = 14.dp,
    onOpen: (String) -> Unit,
) {
    data class Action(val code: String, val icon: Int, val label: String, val cyan: Boolean)
    // DST/TRD-PULSE-v12: thin vector icons (not emoji); Sale carries the selected accent dot.
    val actions = listOf(
        Action("SALE", TradingChromeIcons.pulseActionDrawable("SALE"), "Sale", false),
        Action("PURCHASE", TradingChromeIcons.pulseActionDrawable("PURCHASE"), "Purchase", true),
        Action("COLLECTION", TradingChromeIcons.pulseActionDrawable("COLLECTION"), "Collection", false),
        Action("PAYMENT", TradingChromeIcons.pulseActionDrawable("PAYMENT"), "Payment", false),
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        actions.forEach { action ->
            val tone = if (action.cyan) OperatingV12.cyan else theme.accent
            // Removed the hardcoded 'index == 0' highlight so all buttons are uniform
            val selected = false
            val shape = RoundedCornerShape(radius)
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .shadow(
                        elevation = if (selected) 10.dp else 0.dp,
                        shape = shape,
                        ambientColor = theme.accent.copy(alpha = 0.35f),
                        spotColor = theme.accent.copy(alpha = 0.45f),
                    )
                    .border(
                        1.dp,
                        if (selected) theme.accent.copy(alpha = 0.55f) else tone.copy(alpha = if (tinted) 0.3f else 0.28f),
                        shape,
                    ),
                shape = shape,
                color = if (tinted) tone.copy(alpha = 0.12f) else OperatingV12.inset,
                onClick = { onOpen(action.code) },
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        painter = painterResource(action.icon),
                    contentDescription = null,
                    tint = if (selected) theme.accent else tone,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    action.label,
                    color = OperatingV12.title,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    maxLines = 1,
                )
            }
            }
        }
    }
}

@Composable
fun OperatingV12PulseHero(
    theme: OperatingV12Theme,
    chips: List<String>,
    title: String,
    subtitle: String,
    badge: String,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(theme.heroStart, theme.heroEnd.copy(alpha = 0.8f)),
                    start = Offset(0.32f, 0f),
                    end = Offset(0.68f, Float.POSITIVE_INFINITY),
                ),
            )
            .padding(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                chips.forEach { chip ->
                    Text(
                        chip,
                        color = Color.White,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = Color.White, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Black, fontSize = 32.sp, maxLines = 2)
                Text(subtitle, color = Color.White.copy(alpha = 0.8f), fontFamily = PlusJakartaSans, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text(
                    badge,
                    color = Color.White,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .size(40.dp)
                .shadow(6.dp, RoundedCornerShape(12.dp), ambientColor = theme.accent.copy(alpha = 0.35f), spotColor = theme.accent.copy(alpha = 0.35f))
                .clip(RoundedCornerShape(12.dp))
                .background(theme.canvas.copy(alpha = 0.9f))
                .border(1.dp, theme.accent, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_operating_hero_swap),
                contentDescription = null,
                tint = theme.accent,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

data class OperatingV12ChipSpec(val label: String, val tone: Color = OperatingV12.subtitle, val dot: Boolean = false)
data class OperatingV12KpiSpec(val label: String, val value: String, val tone: Color = OperatingV12.title)

/**
 * Figma `TRD-HUB-v11` Hub Hero (`5:16497`) — Trading Quick Adds only.
 * Live meta: business · FY · currency · GST on|off.
 */
@Composable
fun OperatingV12HubHero(
    model: OperatingProjectionDto,
    theme: OperatingV12Theme,
) {
    val business = model.title.orEmpty().ifBlank { theme.label }
    val fy = OperatingV12Date.fiscalYear(model.startDate)
    val currency = model.currency ?: "INR"
    val gstLabel = if (model.gstTracking.equals("YES", ignoreCase = true)) "GST on" else "GST off"
    val meta = "$business · $fy · $currency · $gstLabel"
    val fill = Color(0xFF151D2E)
    Box(
        Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 24.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color.Black.copy(alpha = 0.6f),
                spotColor = Color.Black.copy(alpha = 0.6f),
            )
            .shadow(
                elevation = 40.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = theme.accent.copy(alpha = 0.12f),
                spotColor = OperatingV12.cyan.copy(alpha = 0.18f),
            )
            .clip(RoundedCornerShape(24.dp))
            .background(fill)
            .border(1.dp, OperatingV12.cyan.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                "QUICK ADDS · v11.1",
                color = OperatingV12.cyan,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.2.sp,
            )
            Text(
                "Quick Adds",
                color = Color.White,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
            )
            Text(
                theme.motionLine,
                color = theme.accent,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
            Text(
                meta,
                color = Color(0xFF94A3B8),
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
            )
            Text(
                "Dispatch moves goods on a route; Record Sale raises the buyer invoice.",
                color = Color(0xFF94A3B8),
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
            )
        }
        // Inset top highlight (Figma white 12% hairline).
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.12f)),
        )
    }
}

@Composable
fun OperatingV12Hero(
    theme: OperatingV12Theme,
    eyebrow: String,
    title: String,
    titleSize: Int = 24,
    chips: List<OperatingV12ChipSpec> = emptyList(),
    kpis: List<OperatingV12KpiSpec> = emptyList(),
) {
    OperatingV12Card(theme = theme, borderOpacity = 0.45f, spacing = 14.dp) {
        Text(
            eyebrow,
            color = theme.accent,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            letterSpacing = 1.2.sp,
        )
        Text(title, color = Color.White, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Black, fontSize = titleSize.sp, maxLines = 2)
        if (chips.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                chips.forEach { OperatingV12Chip(it.label, it.tone, it.dot) }
            }
        }
        if (kpis.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                kpis.forEach { kpi ->
                    OperatingV12Tile(
                        label = kpi.label,
                        value = kpi.value,
                        tone = kpi.tone,
                        fill = OperatingV12.inset,
                        labelTone = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
fun OperatingV12Ring(
    fraction: Double,
    caption: String,
    note: String,
    accent: Color,
) {
    Box(Modifier.size(92.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(92.dp)) {
            val stroke = 8.dp.toPx()
            drawArc(
                color = Color.White.copy(alpha = 0.08f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
                size = Size(size.width - stroke, size.height - stroke),
                topLeft = Offset(stroke / 2, stroke / 2),
            )
            drawArc(
                color = accent,
                startAngle = -90f,
                sweepAngle = (360f * min(1.0, maxOf(0.02, fraction))).toFloat(),
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
                size = Size(size.width - stroke, size.height - stroke),
                topLeft = Offset(stroke / 2, stroke / 2),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(caption, color = Color.White, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Black, fontSize = 22.sp)
            Text(note, color = Color.White.copy(alpha = 0.55f), fontFamily = PlusJakartaSans, fontWeight = FontWeight.Medium, fontSize = 10.sp)
        }
    }
}

@Composable
fun OperatingV12ActionTile(
    theme: OperatingV12Theme,
    title: String,
    subtitle: String,
    icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    locked: String? = null,
) {
    val enabled = locked == null
    val shape = RoundedCornerShape(16.dp)
    ElevatedCard(
        modifier = modifier
            .height(112.dp)
            .shadow(
                if (enabled) 6.dp else 0.dp,
                shape,
                ambientColor = Color.Black.copy(alpha = 0.4f),
                spotColor = theme.accent.copy(alpha = if (theme.isTrading) 0.25f else 0.4f),
            )
            .border(1.dp, theme.accent.copy(alpha = if (enabled) 0.35f else 0.15f), shape),
        shape = shape,
        colors = CardDefaults.elevatedCardColors(containerColor = OperatingV12.surface),
        onClick = onClick,
        enabled = enabled,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
            Modifier
                .size(32.dp)
                .shadow(
                    if (enabled) 6.dp else 0.dp,
                    RoundedCornerShape(10.dp),
                    ambientColor = theme.accent.copy(alpha = 0.5f),
                    spotColor = theme.accent.copy(alpha = 0.55f),
                )
                .clip(RoundedCornerShape(10.dp))
                .background(theme.accent.copy(alpha = if (enabled) 0.16f else 0.08f))
                .border(1.dp, theme.accent.copy(alpha = if (enabled) 0.45f else 0.2f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(icon),
                contentDescription = null,
                tint = if (enabled) theme.accent else OperatingV12.subtitle,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            title,
            color = if (enabled) OperatingV12.title else OperatingV12.subtitle,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            maxLines = 2,
        )
        Text(
            locked ?: subtitle,
            color = OperatingV12.subtitle,
            fontFamily = PlusJakartaSans,
            fontSize = 10.sp,
            maxLines = 2,
        )
    }
    }
}

@Composable
fun OperatingV12SectionHead(
    title: String,
    trailing: String? = null,
    trailingTone: Color = OperatingV12.cyan,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = OperatingV12.sectionTitle, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
        if (trailing != null) {
            Text(trailing, color = trailingTone, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
        }
    }
}
