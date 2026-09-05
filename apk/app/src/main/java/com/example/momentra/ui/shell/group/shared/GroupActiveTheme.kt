package com.example.momentra.ui.shell.group.shared

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.security.BalanceMask
import com.example.momentra.ui.theme.PlusJakartaSans
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.min

/** Figma Group active tokens — 575:14165 family. */
object GroupActiveTheme {
    val Bg = Color(0xFF131313)
    val Brand = Color(0xFFFFB598)
    val BrandSoft = Color(0x66FFB598)
    val Text = Color(0xFFE5E2E1)
    val Secondary = Color(0xFFDFC0B4)
    val Card = Color(0xFF201F1F)
    val Border = Color(0xFF2E2A28)
    val AccentOrange = Color(0xFFFF7A3D)
    val CardRadius = 18.dp
    val HeroGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF3D2A24), Color(0xFF131313), Color(0xFF1A1512)),
    )
    val ChipGradient = Brush.horizontalGradient(listOf(Color(0x33FFB598), Color(0x221A1512)))
}

/** Trip sheet / finance chrome — Figma 1257:8668 family. */
object TripSheetTokens {
    val Bg = Color(0xFF1C1A24)
    val Field = Color(0xFF252332)
    val Border = Color(0xFF323042)
    val Muted = Color(0xFF9E9AA8)
    val Text = Color(0xFFFFFFFF)
    val Accent = Color(0xFFFF7A3D) // Figma orange CTA
    val AccentEnd = Color(0xFFFFB598)
    val CardRadius = 18.dp
}

object GroupFinanceFormat {
    fun parseAmount(raw: String?): BigDecimal = try {
        raw?.replace(",", "")?.toBigDecimal() ?: BigDecimal.ZERO
    } catch (_: Exception) {
        BigDecimal.ZERO
    }

    fun utilizationPercent(expenseTotal: String?, budgetTotal: String?): Int {
        val budget = parseAmount(budgetTotal)
        if (budget <= BigDecimal.ZERO) return 0
        val expense = parseAmount(expenseTotal)
        return min(
            100,
            expense.multiply(BigDecimal(100))
                .divide(budget, 0, RoundingMode.HALF_UP)
                .toInt(),
        )
    }

    fun symbolFor(currencyCode: String): String =
        GroupTravelCurrencyCatalog.symbol(currencyCode)

    fun formatMoney(raw: String?, currencyCode: String = "INR"): String {
        val value = parseAmount(raw)
        if (value <= BigDecimal.ZERO && (raw.isNullOrBlank() || raw == "0" || raw.startsWith("0."))) {
            return "—"
        }
        val prefix = symbolFor(currencyCode)
        val symbols = DecimalFormatSymbols(Locale.US).apply { groupingSeparator = ',' }
        val pattern = if (value.stripTrailingZeros().scale() <= 0) "#,##0" else "#,##0.##"
        return prefix + DecimalFormat(pattern, symbols).format(value)
    }

    fun compactMoney(raw: String?, currencyCode: String = "INR"): String {
        val value = parseAmount(raw)
        val prefix = symbolFor(currencyCode)
        return when {
            value >= BigDecimal(100000) -> {
                val k = value.divide(BigDecimal(1000), 0, RoundingMode.HALF_UP)
                "${prefix}${k}K"
            }
            value > BigDecimal.ZERO -> formatMoney(raw, currencyCode)
            else -> "—"
        }
    }

    /** Partitioned line e.g. "₹80,000 + $1,200 + €400" — no FX conversion. */
    fun formatPartitionedAmounts(
        amounts: List<Pair<String, String>>,
        compact: Boolean = false,
        hide: Boolean = false,
        separator: String = " + ",
    ): String {
        if (hide) return "••••"
        val parts = amounts.mapNotNull { (code, raw) ->
            val formatted = if (compact) compactMoney(raw, code) else formatMoney(raw, code)
            formatted.takeIf { it != "—" }
        }
        return parts.joinToString(separator).ifBlank { "—" }
    }

    fun resolvePrimaryTotal(
        totals: List<com.example.momentra.data.api.GroupFinanceTotalDto>,
        preferredCurrency: String? = null,
    ): com.example.momentra.data.api.GroupFinanceTotalDto? {
        if (totals.isEmpty()) return null
        preferredCurrency?.let { pref ->
            totals.firstOrNull { it.currencyCode.equals(pref, ignoreCase = true) }?.let { return it }
        }
        return totals.firstOrNull { parseAmount(it.budgetTotal) > BigDecimal.ZERO } ?: totals.first()
    }

    fun budgetPartitionLine(
        totals: List<com.example.momentra.data.api.GroupFinanceTotalDto>,
        compact: Boolean = false,
        hide: Boolean = false,
    ): String = formatPartitionedAmounts(
        totals.map { it.currencyCode to (it.budgetTotal ?: "0") },
        compact = compact,
        hide = hide,
    )

    fun expensePartitionLine(
        totals: List<com.example.momentra.data.api.GroupFinanceTotalDto>,
        compact: Boolean = false,
        hide: Boolean = false,
    ): String = formatPartitionedAmounts(
        totals.map { it.currencyCode to (it.expenseTotal ?: "0") },
        compact = compact,
        hide = hide,
    )

    fun positionsForParticipant(
        positions: List<com.example.momentra.data.api.GroupFinancePositionDto>,
        participantId: String?,
    ): List<com.example.momentra.data.api.GroupFinancePositionDto> {
        if (participantId.isNullOrBlank()) return emptyList()
        return positions.filter { it.participantId == participantId }
    }

    fun viewerBalanceHeadline(
        viewer: com.example.momentra.data.api.GroupFinancePositionDto?,
        allPositions: List<com.example.momentra.data.api.GroupFinancePositionDto>,
        hide: Boolean,
    ): Pair<String, String?> {
        if (viewer == null) return "No balance yet" to null
        val primaryCode = viewer.currencyCode
        val primaryNet = parseAmount(viewer.netPosition)
        val headline = when {
            primaryNet < BigDecimal.ZERO ->
                "You owe ${BalanceMask.mask(formatMoney(primaryNet.abs().toPlainString(), primaryCode), hide)}"
            primaryNet > BigDecimal.ZERO ->
                "You are owed ${BalanceMask.mask(formatMoney(viewer.netPosition, primaryCode), hide)}"
            else -> "You're settled up"
        }
        val others = positionsForParticipant(allPositions, viewer.participantId)
            .filter { !it.currencyCode.equals(primaryCode, ignoreCase = true) }
            .mapNotNull { pos ->
                val net = parseAmount(pos.netPosition)
                if (net.compareTo(BigDecimal.ZERO) == 0) null
                else formatMoney(net.abs().toPlainString(), pos.currencyCode)
            }
        val incl = if (others.isEmpty() || hide) {
            null
        } else {
            "incl. ${others.joinToString(" + ")}"
        }
        return headline to incl
    }

    fun groupPositionsByParticipant(
        positions: List<com.example.momentra.data.api.GroupFinancePositionDto>,
    ): List<Pair<String, List<com.example.momentra.data.api.GroupFinancePositionDto>>> =
        positions.groupBy { it.participantId }.toList()
}

/** Trip pulse destination labels from pulse widget or setup prefill. */
object TripPulseDestinations {
    fun fromWidget(widget: Map<String, Any?>?): List<String> {
        if (widget == null) return emptyList()
        @Suppress("UNCHECKED_CAST")
        val placesRaw = widget["places"] as? List<*>
        val fromPlaces = placesRaw?.mapNotNull { item ->
            when (item) {
                is Map<*, *> -> item["label"]?.toString()?.takeIf { it.isNotBlank() }
                else -> null
            }
        }.orEmpty()
        if (fromPlaces.isNotEmpty()) return fromPlaces
        for (key in listOf("destinationText", "destination_text", "destination")) {
            widget[key]?.toString()?.takeIf { it.isNotBlank() && it != "null" }?.let { return listOf(it) }
        }
        return emptyList()
    }

    fun fromPrefill(prefill: com.example.momentra.data.api.GroupSetupPrefillDto?): List<String> {
        if (prefill == null) return emptyList()
        val fromPlaces = prefill.places.orEmpty().mapNotNull { it.label?.takeIf { l -> l.isNotBlank() } }
        if (fromPlaces.isNotEmpty()) return fromPlaces
        return prefill.destinationText?.takeIf { it.isNotBlank() }?.let { listOf(it) }.orEmpty()
    }

    fun heroSubtitle(places: List<String>, peopleCount: Int): String = when {
        places.size > 1 -> "${places.size} destinations · $peopleCount people"
        places.size == 1 -> "${places.first()} · $peopleCount people"
        else -> "Trip · $peopleCount people"
    }
}

@Composable
fun GroupActiveLoading(modifier: Modifier = Modifier) {
    Box(modifier.background(GroupActiveTheme.Bg), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = GroupActiveTheme.Brand)
    }
}

@Composable
fun GroupHeroHeader(
    title: String,
    subtitle: String,
    meta: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(GroupActiveTheme.HeroGradient)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            title,
            color = GroupActiveTheme.Text,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = PlusJakartaSans,
        )
        Text(subtitle, color = GroupActiveTheme.Secondary, fontSize = 13.sp, fontFamily = PlusJakartaSans)
        meta?.let {
            Text(it, color = GroupActiveTheme.Brand, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
        }
    }
}

@Composable
fun GroupSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    badge: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(GroupActiveTheme.Card)
            .border(1.dp, GroupActiveTheme.Border, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                color = GroupActiveTheme.Text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
            badge?.invoke()
        }
        content()
    }
}

@Composable
fun GroupComingSoonBadge(modifier: Modifier = Modifier) {
    Text(
        "Coming Soon",
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(GroupActiveTheme.BrandSoft)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        color = GroupActiveTheme.Brand,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = PlusJakartaSans,
    )
}

@Composable
fun GroupApiGapBadge(modifier: Modifier = Modifier) {
    Text(
        "API_GAP",
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(Color(0x33F87171))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        color = Color(0xFFF87171),
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = PlusJakartaSans,
    )
}

@Composable
fun GroupEmptySection(message: String, detail: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(message, color = GroupActiveTheme.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
        Text(detail, color = GroupActiveTheme.Secondary, fontSize = 12.sp, fontFamily = PlusJakartaSans)
    }
}

@Composable
fun GroupMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF181716))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, color = GroupActiveTheme.Secondary, fontSize = 11.sp, fontFamily = PlusJakartaSans)
        Text(value, color = GroupActiveTheme.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
    }
}

@Composable
fun GroupQuickChip(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.45f)
            .clip(RoundedCornerShape(100.dp))
            .background(GroupActiveTheme.ChipGradient)
            .border(1.dp, GroupActiveTheme.Border, RoundedCornerShape(100.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(label, color = GroupActiveTheme.Text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
    }
}

@Composable
fun GroupProgressRing(
    percent: Int,
    centerLabel: String,
    centerSub: String,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
) {
    val displayPercent = percent.coerceIn(0, 100)
    val infinite = rememberInfiniteTransition(label = "pulseRing")
    val pulse by infinite.animateFloat(
        initialValue = 0.92f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulseScale",
    )
    val scale = if (animate) pulse else 1f
    Box(modifier.size(120.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size((120 * scale).dp)) {
            val stroke = 10.dp.toPx()
            val pad = stroke / 2
            drawArc(
                color = Color(0xFF2A2624),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(pad, pad),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            if (displayPercent > 0) {
                drawArc(
                    color = GroupActiveTheme.Brand,
                    startAngle = -90f,
                    sweepAngle = 360f * (displayPercent / 100f),
                    useCenter = false,
                    topLeft = Offset(pad, pad),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(centerLabel, color = GroupActiveTheme.Text, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans)
            Text(centerSub, color = GroupActiveTheme.Secondary, fontSize = 11.sp, fontFamily = PlusJakartaSans)
        }
    }
}

@Composable
fun GroupProgressBar(percent: Int, modifier: Modifier = Modifier) {
    val p = percent.coerceIn(0, 100)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(Color(0xFF2A2624)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(p / 100f)
                .height(8.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(GroupActiveTheme.Brand),
        )
    }
}

@Composable
fun GroupPulsingIcon(
    glyph: String,
    modifier: Modifier = Modifier,
) {
    val infinite = rememberInfiniteTransition(label = "settingsPulse")
    val alpha by infinite.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "iconAlpha",
    )
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(GroupActiveTheme.Card)
            .border(1.dp, GroupActiveTheme.Border, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, color = GroupActiveTheme.Brand.copy(alpha = alpha), fontSize = 16.sp)
    }
}

@Composable
fun GroupCtaButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.45f)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (enabled) Brush.horizontalGradient(listOf(GroupActiveTheme.Brand, Color(0xFFE89574)))
                else Brush.horizontalGradient(listOf(Color(0xFF3A3533), Color(0xFF3A3533))),
            )
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color(0xFF131313), fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = PlusJakartaSans)
    }
}
