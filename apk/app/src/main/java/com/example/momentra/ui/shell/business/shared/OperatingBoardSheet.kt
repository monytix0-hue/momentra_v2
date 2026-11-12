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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    /**
     * Own Marketing view-all (`2584:29250+`): denser sticky chrome — period vs status chip
     * treatments, 20sp bold title, magenta grab/close, slate sheet fill.
     */
    val ownStickyChrome: Boolean = false,
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
) {
    val figmaId: String get() = OperatingSheetChrome.figmaId(code)
    val statusFilters: List<String> get() = OperatingSheetChrome.statusFilters(code)
    val footerCta: String get() = OperatingSheetChrome.footerCta(code)
    val periodFilters: List<String> get() = OperatingSheetChrome.periodFilters
}

/** Sticky-header chrome shared by every DST-SHEET / TRD-SHEET view-all surface. */
object OperatingSheetChrome {
    val periodFilters = listOf("This month", "Last month", "Custom range ›")

    /**
     * Figma `*-SHEET · View all · …` node id.
     *
     * Default (no [vertical]) keeps DST `2561:*` for the shared catalogue, fills retail-family /
     * marketing board codes that DST never shipped, and keeps MFG `production` → `2549:30941`.
     * Pass a subtype / family key to prefer that vertical's native frame when one exists.
     */
    fun figmaId(code: String, vertical: String? = null): String {
        val native = vertical?.let { verticalFigmaId(code, it) }.orEmpty()
        if (native.isNotBlank()) return native
        return when (code) {
            "TARGET_BUDGET" -> "2561:29337"
            "FINANCE" -> "2561:29430"
            "PAYMENT_READINESS" -> "2561:29507"
            "ATTENTION" -> "2561:29581"
            "ACTIVITY" -> "2561:29673"
            "GST" -> "2561:29759"
            "orders" -> "2561:29845"
            "stock" -> "2561:29941"
            "network" -> "2561:30031"
            "M4" -> "2561:30116"
            "L1" -> "2561:30214"
            "L2" -> "2561:30294"
            "L3" -> "2561:30398"
            "L4" -> "2561:30492"
            "ME1" -> "2561:30588"
            "ME2" -> "2561:30680"
            "fulfilment" -> "2561:30773"
            "ME4" -> "2561:30852"
            // MFG Life · Production Operations — dedicated Production view-all.
            "production" -> "2549:30941"
            // Retail-family / marketing board codes with no DST frame — nearest native View-all.
            "sales" -> "2513:30297" // RTL Life · Sales & Orders
            "services" -> "2538:30740" // SVC Life · Bookings & Charges
            "appointments" -> "2538:30740"
            "channels" -> "2574:29995" // MKT Moments · Channels & Network
            "customers" -> "2513:30041" // RTL Moments · Business Network
            "M1" -> "2574:29810" // MKT Moments · Campaign / Moment
            "M2" -> "2574:29906" // MKT Moments · People
            "M3" -> "2574:29995"
            "L5" -> "2574:30453" // MKT Life · People Today
            "ME3" -> "2513:30751" // RTL Memory · People & Activity
            else -> ""
        }
    }

    /** Vertical-native `*-SHEET · View all` ids from Figma `TzLvwVwlPbeVB8ug1zB3GM`. */
    private fun verticalFigmaId(code: String, vertical: String): String {
        val key = vertical.uppercase()
        return when {
            key.contains("TRADING") || key == "TRD" -> when (code) {
                "TARGET_BUDGET" -> "2566:33638"
                "FINANCE" -> "2566:33689"
                "PAYMENT_READINESS" -> "2566:33760"
                "ATTENTION" -> "2566:33822"
                "ACTIVITY" -> "2566:33889"
                "GST" -> "2566:33960"
                "orders" -> "2566:34018"
                "stock" -> "2566:34096"
                "network" -> "2566:34169"
                "M4" -> "2566:34237"
                "L1" -> "2566:34317"
                "L2" -> "2566:34380"
                "L3" -> "2566:34466"
                "L4" -> "2566:34542"
                "ME1" -> "2566:34620"
                "ME2" -> "2566:34695"
                "fulfilment" -> "2566:34770"
                "ME4" -> "2566:34832"
                else -> ""
            }
            key.contains("RETAIL") || key == "RTL" -> when (code) {
                "TARGET_BUDGET" -> "2513:29334"
                "ATTENTION" -> "2513:29426"
                "PAYMENT_READINESS" -> "2513:29520"
                "FINANCE" -> "2513:29601"
                "ACTIVITY" -> "2513:29688"
                "GST" -> "2513:29770"
                "M1" -> "2513:29859"
                "M2" -> "2513:29948"
                "network", "customers", "M3" -> "2513:30041"
                "M4" -> "2513:30126"
                "L1" -> "2513:30217"
                "sales", "L2" -> "2513:30297"
                "L3" -> "2513:30394"
                "L4", "stock" -> "2513:30481"
                "ME1" -> "2513:30570"
                "ME2" -> "2513:30665"
                "ME3" -> "2513:30751"
                "ME4" -> "2513:30830"
                else -> ""
            }
            key.contains("RESTAURANT") || key.contains("FOOD") || key == "RST" -> when (code) {
                "TARGET_BUDGET" -> "2482:29487"
                "ATTENTION" -> "2482:29639"
                "PAYMENT_READINESS" -> "2482:29795"
                "FINANCE" -> "2482:29938"
                "ACTIVITY" -> "2482:30085"
                "GST" -> "2482:30227"
                "M1" -> "2482:30376"
                "M2" -> "2482:30525"
                "network", "M3" -> "2482:30680"
                "M4" -> "2482:30827"
                "L1" -> "2482:30978"
                "sales", "L2" -> "2482:31120"
                "L3" -> "2482:31277"
                "L4" -> "2482:31424"
                "ME1" -> "2482:31573"
                "ME2" -> "2482:31730"
                "ME3" -> "2482:31876"
                "ME4" -> "2482:32017"
                else -> ""
            }
            (key.contains("SERVICE") && !key.contains("CAMPAIGN")) || key == "SVC" || key == "CONSUMER_SERVICE" -> when (code) {
                "TARGET_BUDGET" -> "2538:29731"
                "ATTENTION" -> "2538:29832"
                "PAYMENT_READINESS" -> "2538:29917"
                "FINANCE" -> "2538:29999"
                "ACTIVITY" -> "2538:30095"
                "GST" -> "2538:30186"
                "M1" -> "2538:30284"
                "M2" -> "2538:30382"
                "network", "M3", "customers" -> "2538:30473"
                "M4" -> "2538:30559"
                "L1" -> "2538:30659"
                "services", "appointments", "L2" -> "2538:30740"
                "L3" -> "2538:30846"
                "L4" -> "2538:30942"
                "ME1" -> "2538:31038"
                "ME2" -> "2538:31131"
                "ME3" -> "2538:31226"
                "ME4" -> "2538:31306"
                else -> ""
            }
            key.contains("MADE_TO_ORDER") || key == "MTO" -> when (code) {
                "TARGET_BUDGET" -> "2694:29807"
                "ATTENTION" -> "2694:29905"
                "PAYMENT_READINESS" -> "2694:29992"
                "FINANCE" -> "2694:30072"
                "ACTIVITY" -> "2694:30165"
                "GST" -> "2694:30258"
                "M1" -> "2694:30353"
                "M2" -> "2694:30453"
                "network", "M3", "customers" -> "2694:30547"
                "M4" -> "2694:30633"
                "L1" -> "2694:30732"
                "orders", "L2" -> "2694:30814"
                "L3" -> "2694:30922"
                "production", "L4" -> "2694:31015"
                "ME1" -> "2694:31116"
                "ME2" -> "2694:31209"
                "ME3" -> "2694:31302"
                "ME4" -> "2694:31382"
                else -> ""
            }
            key.contains("PRODUCTION") || key.contains("MANUFACTURING") || key == "MFG" || key == "STANDARD_PRODUCTION" -> when (code) {
                "TARGET_BUDGET" -> "2549:29728"
                "ATTENTION" -> "2549:29829"
                "PAYMENT_READINESS" -> "2549:29915"
                "FINANCE" -> "2549:29997"
                "ACTIVITY" -> "2549:30093"
                "GST" -> "2549:30184"
                "M1" -> "2549:30282"
                "M2" -> "2549:30380"
                "network", "M3" -> "2549:30471"
                "M4" -> "2549:30558"
                "L1" -> "2549:30658"
                "orders", "L2" -> "2549:30739"
                "L3" -> "2549:30845"
                "production", "L4" -> "2549:30941"
                "ME1" -> "2549:31039"
                "ME2" -> "2549:31132"
                "ME3" -> "2549:31227"
                "ME4" -> "2549:31307"
                else -> ""
            }
            key.contains("OWN") || key == "OWN_BUSINESS_CAMPAIGN" || key == "MKT_OWN" -> when (code) {
                "TARGET_BUDGET" -> "2584:29250"
                "ATTENTION" -> "2584:29346"
                "PAYMENT_READINESS" -> "2584:29426"
                "FINANCE" -> "2584:29503"
                "ACTIVITY" -> "2584:29594"
                "M1" -> "2584:30079"
                "M2" -> "2584:30172"
                "M3", "channels" -> "2584:30261"
                "M4" -> "2584:30346"
                "L1" -> "2584:30432"
                "L2" -> "2584:30508"
                "L3" -> "2584:30612"
                "L5" -> "2584:30703"
                "ME1" -> "2584:30790"
                "ME2" -> "2584:30880"
                "ME3" -> "2584:30970"
                "ME4" -> "2584:31048"
                else -> ""
            }
            key.contains("CAMPAIGN") || key.contains("MARKETING") || key == "MKT" || key == "CLIENT_CAMPAIGN" -> when (code) {
                "TARGET_BUDGET" -> "2574:29365"
                "ATTENTION" -> "2574:29464"
                "PAYMENT_READINESS" -> "2574:29547"
                "FINANCE" -> "2574:29627"
                "ACTIVITY" -> "2574:29721"
                "M1" -> "2574:29810"
                "M2" -> "2574:29906"
                "M3", "channels" -> "2574:29995"
                "M4" -> "2574:30080"
                "L1" -> "2574:30176"
                "L2" -> "2574:30255"
                "L3" -> "2574:30359"
                "L5" -> "2574:30453"
                "ME1" -> "2574:30547"
                "ME2" -> "2574:30638"
                "ME3" -> "2574:30731"
                "ME4" -> "2574:30809"
                else -> ""
            }
            else -> ""
        }
    }

    fun statusFilters(code: String): List<String> = when (code) {
        "FINANCE", "PAYMENT_READINESS", "ATTENTION", "orders", "sales", "purchases", "services", "appointments", "M4", "L3" ->
            listOf("Open", "Overdue", "Settled")
        "ME1", "ME2", "ME3", "fulfilment", "ME4" ->
            listOf("All", "Open", "Settled")
        "production" -> listOf("All", "Open", "Done")
        else -> listOf("All", "Open", "Done")
    }

    fun footerCta(code: String): String = when (code) {
        "PAYMENT_READINESS", "L3" -> "Record payment"
        "ATTENTION" -> "Resolve all"
        "orders", "sales", "purchases", "services", "appointments", "M4" -> "Resolve commitments"
        "production" -> "Open Quick Add"
        "ME1", "ME2", "ME3", "fulfilment", "ME4" -> "Open Memory"
        else -> "Open Quick Add"
    }

    fun footerAction(code: String): String = when (footerCta(code)) {
        "Record payment" -> "PAYMENT"
        "Resolve all", "Resolve commitments" -> "ATTENTION"
        "Open Memory" -> "MEMORY"
        else -> "QUICK_ADD"
    }

    fun matchesStatus(filter: String, badge: String?): Boolean {
        if (filter == "All") return true
        val b = badge.orEmpty().lowercase()
        return when (filter) {
            "Open" -> b.contains("open") || b.contains("alert") || b.contains("part") ||
                b.contains("transit") || b.contains("unavailable") || b.isBlank()
            "Done" -> b.contains("done") || b.contains("delivered")
            "Settled" -> b.contains("settled") || b.contains("paid") || b.contains("done")
            "Overdue" -> b.contains("overdue") || b.contains("critical")
            else -> true
        }
    }
}

/** Which pulse surface a sheet reads when it is not backed by a board section. */
enum class OperatingPulseSurface { TARGET_BUDGET, FINANCE, PAYMENT_READINESS, ATTENTION, ACTIVITY, GST }

private val sheetMonthFmt = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)

@Composable
fun OperatingSheetFrame(
    title: String,
    eyebrow: String,
    model: OperatingProjectionDto,
    theme: OperatingSheetTheme,
    onDismiss: () -> Unit,
    sheetCode: String? = null,
    visibleCount: (selectedStatus: String) -> Int = { 0 },
    totalCount: Int = 0,
    onFooterAction: ((String) -> Unit)? = null,
    content: @Composable (selectedStatus: String) -> Unit,
) {
    val filters = sheetCode?.let { OperatingSheetChrome.statusFilters(it) }.orEmpty()
    var period by remember {
        mutableStateOf(
            when (OperatingChromeState.periodFilter) {
                "last_month" -> "Last month"
                "custom" -> "Custom range ›"
                else -> OperatingSheetChrome.periodFilters.first()
            },
        )
    }
    var status by remember(sheetCode) { mutableStateOf(filters.firstOrNull() ?: "All") }
    var pickingCustomBound by remember { mutableStateOf<String?>(null) }
    val month = runCatching {
        YearMonth.parse(model.startDate?.take(7) ?: "").format(sheetMonthFmt)
    }.getOrElse { "This month" }
    val contextLine = "${model.title?.takeIf { it.isNotBlank() } ?: "Business"} · $month · full activity, newest first"
    val footerCta = sheetCode?.let { OperatingSheetChrome.footerCta(it) }
    val shown = visibleCount(status)
    val customFrom = OperatingChromeState.periodFrom
    val customTo = OperatingChromeState.periodTo

    val own = theme.ownStickyChrome
    val edgePad = if (own) 21.dp else 17.dp
    OperatingSheetScrim(
        onDismiss = onDismiss,
        accent = theme.accent,
        surface = theme.canvas,
        border = theme.border,
        topCorner = if (own) 20.dp else 24.dp,
    ) {
        Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = edgePad)
                    .padding(top = if (own) 14.dp else 12.dp),
                verticalArrangement = Arrangement.spacedBy(if (own) 14.dp else 10.dp),
            ) {
                Row(Modifier.fillMaxWidth().height(22.dp), horizontalArrangement = Arrangement.Center) {
                    Box(
                        Modifier
                            .width(if (own) 44.dp else 40.dp)
                            .height(if (own) 4.dp else 5.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(
                                if (own) theme.accent.copy(alpha = 0.55f)
                                else Color.White.copy(alpha = 0.2f),
                            ),
                    )
                }
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        title,
                        color = theme.ink,
                        fontFamily = PlusJakartaSans,
                        fontWeight = if (own) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = if (own) 20.sp else 24.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "✕",
                        color = theme.body,
                        fontFamily = PlusJakartaSans,
                        fontWeight = if (own) FontWeight.Bold else FontWeight.Normal,
                        fontSize = if (own) 14.sp else 16.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .then(
                                if (own) {
                                    Modifier
                                        .background(Color.White.copy(alpha = 0.06f))
                                        .border(1.dp, theme.border, RoundedCornerShape(999.dp))
                                } else {
                                    Modifier
                                },
                            )
                            .clickable(onClick = onDismiss)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
                Text(contextLine, color = theme.body, fontFamily = PlusJakartaSans, fontSize = 12.sp)
                if (eyebrow.isNotBlank()) {
                    Text(eyebrow, color = theme.body, fontFamily = PlusJakartaSans, fontSize = 12.sp)
                }
                OperatingSheetChipRow(
                    options = OperatingSheetChrome.periodFilters,
                    selected = period,
                    theme = theme,
                    style = if (own) OperatingSheetChipStyle.Period else OperatingSheetChipStyle.Default,
                    onSelect = { next ->
                        period = next
                        OperatingChromeState.applySheetPeriod(next)
                    },
                )
                if (period == "Custom range ›") {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "From ${customFrom ?: "…"}",
                            color = theme.ink,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(999.dp))
                                .background(theme.inset)
                                .border(1.dp, theme.border, RoundedCornerShape(999.dp))
                                .clickable { pickingCustomBound = "from" }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                        )
                        Text(
                            "To ${customTo ?: "…"}",
                            color = theme.ink,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(999.dp))
                                .background(theme.inset)
                                .border(1.dp, theme.border, RoundedCornerShape(999.dp))
                                .clickable { pickingCustomBound = "to" }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                        )
                    }
                }
                if (filters.isNotEmpty()) {
                    OperatingSheetChipRow(
                        options = filters,
                        selected = status,
                        theme = theme,
                        style = if (own) OperatingSheetChipStyle.Status else OperatingSheetChipStyle.Default,
                        onSelect = { status = it },
                    )
                }
                }
                if (own) {
                    Spacer(Modifier.height(12.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.08f)),
                    )
                }
            }
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = edgePad)
                    .padding(top = if (own) 14.dp else 0.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                content(status)
                Spacer(Modifier.height(16.dp))
            }
            if (footerCta != null) {
                val code = sheetCode.orEmpty()
                if (own) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.08f)),
                    )
                }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = edgePad, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(if (own) 10.dp else 8.dp),
                ) {
                    Text(
                        "Showing $shown of $totalCount · tap a row for detail",
                        color = theme.body,
                        fontFamily = PlusJakartaSans,
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        footerCta,
                        color = if (own) Color.White else theme.canvas,
                        fontFamily = PlusJakartaSans,
                        fontWeight = if (own) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 15.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(theme.accent)
                            .clickable {
                                val action = OperatingSheetChrome.footerAction(code)
                                if (onFooterAction != null) {
                                    onFooterAction(action)
                                } else {
                                    when (action) {
                                        "PAYMENT" -> OperatingChromeState.quickAddHint = "payment"
                                        "MEMORY" -> OperatingChromeState.storyConfigOpen = true
                                        "ATTENTION" -> OperatingChromeState.quickAddHint = "issue"
                                        else -> OperatingChromeState.quickAddHint = "sale"
                                    }
                                }
                                onDismiss()
                            }
                            .padding(vertical = 14.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }
        pickingCustomBound?.let { bound ->
            val initial = runCatching {
                LocalDate.parse(
                    if (bound == "from") customFrom else customTo,
                )
            }.getOrElse { LocalDate.now() }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(theme.canvas),
            ) {
                OperatingPickerSheet(
                    mode = OperatingPickerMode.DAY,
                    initial = initial,
                    theme = theme,
                    title = if (bound == "from") "Custom range · from" else "Custom range · to",
                    onDismiss = { pickingCustomBound = null },
                    onDay = { day ->
                        if (bound == "from") {
                            OperatingChromeState.periodFrom = day.toString()
                        } else {
                            OperatingChromeState.periodTo = day.toString()
                        }
                        OperatingChromeState.periodFilter = "custom"
                        OperatingChromeState.refresh += 1
                        pickingCustomBound = null
                    },
                )
            }
        }
        }
    }
}

/** Period = solid selected fill; Status = soft magenta fill (Own `2584:29250`). */
private enum class OperatingSheetChipStyle { Default, Period, Status }

@Composable
private fun OperatingSheetChipRow(
    options: List<String>,
    selected: String,
    theme: OperatingSheetTheme,
    style: OperatingSheetChipStyle = OperatingSheetChipStyle.Default,
    onSelect: (String) -> Unit,
) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            val on = option == selected
            val (fg, bg, stroke, weight) = when {
                style == OperatingSheetChipStyle.Period && on ->
                    ChipLook(Color.White, theme.accent, theme.accentSoft, FontWeight.Bold)
                style == OperatingSheetChipStyle.Period && !on ->
                    ChipLook(theme.body, theme.inset, theme.inset, FontWeight.Medium)
                style == OperatingSheetChipStyle.Status && on ->
                    ChipLook(
                        theme.accent,
                        theme.accent.copy(alpha = 0.18f),
                        theme.accent.copy(alpha = 0.6f),
                        FontWeight.SemiBold,
                    )
                style == OperatingSheetChipStyle.Status && !on ->
                    ChipLook(
                        Color.White.copy(alpha = 0.7f),
                        Color.White.copy(alpha = 0.04f),
                        Color.White.copy(alpha = 0.1f),
                        FontWeight.Medium,
                    )
                on -> ChipLook(theme.canvas, theme.accent, theme.accent, FontWeight.SemiBold)
                else -> ChipLook(theme.body, theme.inset, theme.border, FontWeight.SemiBold)
            }
            Text(
                option,
                color = fg,
                fontFamily = PlusJakartaSans,
                fontWeight = weight,
                fontSize = 12.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(bg)
                    .border(1.dp, stroke, RoundedCornerShape(999.dp))
                    .clickable { onSelect(option) }
                    .padding(
                        horizontal = if (style == OperatingSheetChipStyle.Default) 13.dp else 12.dp,
                        vertical = if (style == OperatingSheetChipStyle.Default) 7.dp else 6.dp,
                    ),
            )
        }
    }
}

private data class ChipLook(
    val fg: Color,
    val bg: Color,
    val stroke: Color,
    val weight: FontWeight,
)

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
