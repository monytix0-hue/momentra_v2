package com.example.momentra.ui.shell.business.gap

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.momentra.data.api.ActivityItemDto
import com.example.momentra.data.api.BusinessFinanceTotalDto
import com.example.momentra.data.api.LocationItemDto
import com.example.momentra.data.repository.BusinessSliceRepository
import com.example.momentra.domain.MomentSummary
import com.example.momentra.domain.isActiveStatus
import com.example.momentra.domain.isCompletedStatus
import com.example.momentra.ui.theme.PlusJakartaSans

internal val GapBg = Color(0xFF0C0F15)
internal val GapCard = Color(0xFF161B26)
internal val GapBorder = Color(0xFF1E293B)
internal val GapAccent = Color(0xFF818CF8)
internal val GapText = Color(0xFFF1F5F9)
internal val GapMuted = Color(0xFF64748B)

data class PendingBusinessActivation(
    val momentId: String,
    val title: String,
    val momentTypeCode: String?,
    val status: String,
)

enum class BusinessGapPage {
    Moments,
    CompanyPulse,
    Finance,
    Vendor,
    MomentsSettings,
    Milestone,
    Visibility,
    LocationPicker,
    LocationDashboard,
    LocationConfig,
    Inheritance,
}

fun businessFamilyLabel(code: String?): String {
    val c = code.orEmpty().uppercase()
    return when {
        c.contains("TEAM") -> "Team & Work"
        c.contains("RUNWAY") -> "Money & Cash Flow"
        c.contains("OPERATIONS") -> "Daily Business"
        c.isBlank() -> "Business"
        else -> c.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() }
    }
}

private fun MomentSummary.upcoming(): Boolean {
    val s = status.uppercase()
    return s == "UPCOMING" || s == "PLANNED" || s == "SCHEDULED"
}

@Composable
internal fun GapScreen(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GapBg),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (onBack != null) {
                Icon(
                    Icons.Outlined.KeyboardArrowLeft,
                    contentDescription = "Back",
                    tint = GapText,
                    modifier = Modifier.clickable(onClick = onBack),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = GapText, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                if (!subtitle.isNullOrBlank()) {
                    Text(subtitle, color = GapMuted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
                }
            }
            trailing?.invoke()
        }
        content()
    }
}

@Composable
internal fun GapCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GapCard)
            .border(1.dp, GapBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = { content() },
    )
}

@Composable
fun BusinessMomentsDirectory(
    moments: List<MomentSummary>,
    companyName: String,
    onBack: () -> Unit,
    onSelectMoment: (MomentSummary) -> Unit,
    onCreateMoment: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCompanyPulse: () -> Unit,
) {
    var tab by remember { mutableStateOf("Active") }
    val filtered = moments.filter { moment ->
        when (tab) {
            "Upcoming" -> moment.upcoming()
            "Completed" -> moment.isCompletedStatus()
            else -> moment.isActiveStatus()
        }
    }
    GapScreen(
        title = "Business Moments",
        subtitle = companyName.ifBlank { null },
        onBack = onBack,
        trailing = {
            Icon(
                Icons.Outlined.Settings,
                contentDescription = "Moments settings",
                tint = GapMuted,
                modifier = Modifier.clickable(onClick = onOpenSettings),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Active", "Upcoming", "Completed").forEach { label ->
                    val selected = tab == label
                    Text(
                        label,
                        color = if (selected) GapBg else GapText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = PlusJakartaSans,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (selected) GapAccent else GapCard)
                            .clickable { tab = label }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
            Text(
                "Company pulse",
                color = GapAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier.clickable(onClick = onOpenCompanyPulse),
            )
            if (filtered.isEmpty()) {
                Text("No moments in this tab.", color = GapMuted, fontSize = 13.sp, fontFamily = PlusJakartaSans)
            }
            filtered.forEach { moment ->
                GapCard(modifier = Modifier.clickable { onSelectMoment(moment) }) {
                    Text(businessFamilyLabel(moment.momentTypeCode), color = GapAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                    Text(moment.title.ifBlank { "Untitled moment" }, color = GapText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
                    Text(moment.status.ifBlank { "—" }, color = GapMuted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
                }
            }
            Text(
                "+ Create Moment",
                color = GapBg,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(GapAccent)
                    .clickable(onClick = onCreateMoment)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }
}

private data class PulseLine(
    val moment: MomentSummary,
    val score: String?,
    val attention: Int,
    val runway: String?,
    val openIssues: Int?,
)

@Composable
fun BusinessCompanyPulseScreen(
    companyName: String,
    moments: List<MomentSummary>,
    onBack: () -> Unit,
    onOpenMoment: (MomentSummary) -> Unit,
    repository: BusinessSliceRepository = remember { BusinessSliceRepository() },
) {
    var loading by remember { mutableStateOf(true) }
    var lines by remember { mutableStateOf<List<PulseLine>>(emptyList()) }
    LaunchedEffect(moments.map { it.momentId }) {
        loading = true
        lines = moments.take(12).map { moment ->
            val pulse = repository.getPulse(moment.momentId).getOrNull()?.payload
            PulseLine(
                moment = moment,
                score = pulse?.financialHealthScore?.takeIf { it.isNotBlank() },
                attention = pulse?.attentionCount ?: 0,
                runway = pulse?.runwayMonths?.takeIf { it.isNotBlank() },
                openIssues = pulse?.operations?.openIssueCount,
            )
        }
        loading = false
    }
    val activeCount = moments.count { it.isActiveStatus() }
    val attention = lines.sumOf { it.attention }
    GapScreen(title = "Business Pulse", subtitle = companyName.ifBlank { null }, onBack = onBack) {
        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GapAccent)
            }
            return@GapScreen
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GapCard(Modifier.weight(1f)) {
                    Text("ACTIVE MOMENTS", color = GapMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                    Text(activeCount.toString(), color = GapText, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                }
                GapCard(Modifier.weight(1f)) {
                    Text("NEEDS ATTENTION", color = GapMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                    Text(if (lines.isEmpty()) "—" else attention.toString(), color = GapText, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                }
            }
            if (lines.isEmpty()) {
                Text("No moments to read yet.", color = GapMuted, fontSize = 13.sp, fontFamily = PlusJakartaSans)
            }
            lines.forEach { line ->
                GapCard(modifier = Modifier.clickable { onOpenMoment(line.moment) }) {
                    Text(businessFamilyLabel(line.moment.momentTypeCode), color = GapText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
                    Text(line.moment.title, color = GapMuted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
                    Text(
                        listOfNotNull(
                            line.score?.let { "Health $it" },
                            line.runway?.let { "Runway $it" },
                            line.openIssues?.let { "Open issues $it" },
                        ).joinToString(" · ").ifBlank { "—" },
                        color = GapText,
                        fontSize = 13.sp,
                        fontFamily = PlusJakartaSans,
                    )
                }
            }
            GapCard {
                Text("What Momentra Noticed", color = GapText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
                Text("No insights yet", color = GapMuted, fontSize = 13.sp, fontFamily = PlusJakartaSans)
            }
        }
    }
}

private data class FinanceRollup(
    val currency: String?,
    val expenses: String?,
    val revenue: String?,
    val outstanding: String?,
    val activity: List<ActivityItemDto>,
)

@Composable
fun BusinessFinanceScreen(
    companyName: String,
    moments: List<MomentSummary>,
    onBack: () -> Unit,
    repository: BusinessSliceRepository = remember { BusinessSliceRepository() },
) {
    var loading by remember { mutableStateOf(true) }
    var rollup by remember { mutableStateOf(FinanceRollup(null, null, null, null, emptyList())) }
    LaunchedEffect(moments.map { it.momentId }) {
        loading = true
        val totals = mutableListOf<BusinessFinanceTotalDto>()
        val activity = mutableListOf<ActivityItemDto>()
        moments.take(12).forEach { moment ->
            repository.getFinance(moment.momentId).getOrNull()?.payload?.totals?.let { totals += it }
            repository.getActivity(moment.momentId, limit = 5).getOrNull()?.items?.let { activity += it }
        }
        val currency = totals.firstOrNull()?.currencyCode
        val same = currency != null && totals.all { it.currencyCode == currency }
        fun sum(pick: (BusinessFinanceTotalDto) -> String): String? {
            if (!same) return if (totals.size == 1) pick(totals.first()) else null
            val values = totals.mapNotNull { pick(it).toDoubleOrNull() }
            if (values.isEmpty()) return null
            return values.sum().toString()
        }
        rollup = FinanceRollup(
            currency = if (same) currency else totals.firstOrNull()?.currencyCode,
            expenses = sum { it.expenseTotal },
            revenue = sum { it.revenueTotal },
            outstanding = sum { it.invoiceOutstandingTotal },
            activity = activity.take(8),
        )
        loading = false
    }
    GapScreen(title = "Finance", subtitle = companyName.ifBlank { null }, onBack = onBack) {
        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GapAccent)
            }
            return@GapScreen
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FinanceTile("TOTAL EXPENSES", money(rollup.currency, rollup.expenses), Modifier.weight(1f))
                FinanceTile("REVENUE", money(rollup.currency, rollup.revenue), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FinanceTile("OUTSTANDING", money(rollup.currency, rollup.outstanding), Modifier.weight(1f))
                FinanceTile("CASH BALANCE", "—", Modifier.weight(1f))
            }
            Text("RECENT ACTIVITY", color = GapMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
            if (rollup.activity.isEmpty()) {
                Text("No activity yet.", color = GapMuted, fontSize = 13.sp, fontFamily = PlusJakartaSans)
            }
            rollup.activity.forEach { item ->
                GapCard {
                    Text(item.title.ifBlank { item.activityCode }, color = GapText, fontSize = 14.sp, fontFamily = PlusJakartaSans)
                    Text(item.occurredAt, color = GapMuted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
                }
            }
        }
    }
}

@Composable
private fun FinanceTile(label: String, value: String, modifier: Modifier) {
    GapCard(modifier) {
        Text(label, color = GapMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
        Text(value, color = GapText, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
    }
}

private fun money(currency: String?, amount: String?): String {
    if (amount.isNullOrBlank()) return "—"
    return listOfNotNull(currency, amount).joinToString(" ")
}

@Composable
fun BusinessMomentsSettingsScreen(
    onBack: () -> Unit,
    onOpenMilestone: () -> Unit,
    onOpenVisibility: () -> Unit,
) {
    var track by remember { mutableStateOf(true) }
    var timeline by remember { mutableStateOf(true) }
    var decisions by remember { mutableStateOf(false) }
    var alerts by remember { mutableStateOf(true) }
    var reminders by remember { mutableStateOf(false) }
    var delivery by remember { mutableStateOf(true) }
    GapScreen(
        title = "Moments Settings",
        subtitle = "Configure automated timelines, notifications, and compliance settings.",
        onBack = onBack,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GapCard {
                Text("General", color = GapText, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
                ToggleRow("Auto-track milestones", track) { track = it }
                ToggleRow("Enable team timeline", timeline) { timeline = it }
                ToggleRow("Show decision log", decisions) { decisions = it }
            }
            GapCard {
                Text("Notifications", color = GapText, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
                ToggleRow("Milestone alerts", alerts) { alerts = it }
                ToggleRow("Decision reminders", reminders) { reminders = it }
                ToggleRow("Delivery updates", delivery) { delivery = it }
            }
            GapCard {
                Text("Data & Privacy", color = GapText, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
                Text("Export moments data", color = GapMuted, fontSize = 13.sp, fontFamily = PlusJakartaSans)
                Text("Clear history", color = GapMuted, fontSize = 13.sp, fontFamily = PlusJakartaSans)
                Text("Retention policy", color = GapMuted, fontSize = 13.sp, fontFamily = PlusJakartaSans)
            }
            Text("Milestone tracking", color = GapAccent, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans, modifier = Modifier.clickable(onClick = onOpenMilestone))
            Text("Visibility", color = GapAccent, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans, modifier = Modifier.clickable(onClick = onOpenVisibility))
        }
    }
}

@Composable
internal fun ToggleRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = GapText, fontSize = 14.sp, fontFamily = PlusJakartaSans, modifier = Modifier.weight(1f))
        Switch(checked = value, onCheckedChange = onChange)
    }
}

@Composable
fun BusinessMilestoneSettingsScreen(onBack: () -> Unit) {
    var mode by remember { mutableStateOf("Hybrid") }
    var categories by remember {
        mutableStateOf(listOf("Product Launch" to true, "Hiring" to false, "Revenue Target" to true, "Partnership" to false, "Compliance" to true))
    }
    var requireApproval by remember { mutableStateOf(true) }
    GapScreen(
        title = "Milestone Tracking",
        subtitle = "Manage tracking rules, active category tags, and approval levels.",
        onBack = onBack,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GapCard {
                Text("Tracking Mode", color = GapText, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
                listOf("Automatic", "Manual", "Hybrid").forEach { option ->
                    Text(
                        option,
                        color = if (mode == option) GapAccent else GapText,
                        fontFamily = PlusJakartaSans,
                        modifier = Modifier.clickable { mode = option }.padding(vertical = 4.dp),
                    )
                }
            }
            GapCard {
                Text("Categories", color = GapText, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
                categories.forEachIndexed { index, row ->
                    ToggleRow(row.first, row.second) { on ->
                        categories = categories.mapIndexed { i, item -> if (i == index) item.first to on else item }
                    }
                }
            }
            GapCard {
                Text("Approval Flow", color = GapText, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
                ToggleRow("Require approval before logging", requireApproval) { requireApproval = it }
            }
        }
    }
}

@Composable
fun BusinessVisibilitySettingsScreen(onBack: () -> Unit) {
    var owners by remember { mutableStateOf(true) }
    var team by remember { mutableStateOf(true) }
    var vendors by remember { mutableStateOf(false) }
    GapScreen(title = "Visibility", subtitle = "Who can see business moments.", onBack = onBack) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ToggleRow("Owners", owners) { owners = it }
            ToggleRow("Team", team) { team = it }
            ToggleRow("Vendors", vendors) { vendors = it }
        }
    }
}

@Composable
fun BusinessGapHost(
    page: BusinessGapPage?,
    companyId: String?,
    companyName: String,
    moments: List<MomentSummary>,
    location: LocationItemDto?,
    onLocation: (LocationItemDto) -> Unit,
    onPage: (BusinessGapPage?) -> Unit,
    onClose: () -> Unit,
    onSelectMoment: (MomentSummary) -> Unit,
    onCreateMoment: () -> Unit,
    onBackToSettings: () -> Unit,
) {
    if (page == null) return
    when (page) {
        BusinessGapPage.Moments -> BusinessMomentsDirectory(
            moments = moments,
            companyName = companyName,
            onBack = onClose,
            onSelectMoment = onSelectMoment,
            onCreateMoment = onCreateMoment,
            onOpenSettings = { onPage(BusinessGapPage.MomentsSettings) },
            onOpenCompanyPulse = { onPage(BusinessGapPage.CompanyPulse) },
        )
        BusinessGapPage.CompanyPulse -> BusinessCompanyPulseScreen(
            companyName = companyName,
            moments = moments,
            onBack = { onPage(BusinessGapPage.Moments) },
            onOpenMoment = onSelectMoment,
        )
        BusinessGapPage.Finance -> BusinessFinanceScreen(
            companyName = companyName,
            moments = moments,
            onBack = onClose,
        )
        BusinessGapPage.Vendor -> {
            if (companyId.isNullOrBlank()) {
                GapScreen(title = "Vendor Operations", onBack = onClose) {
                    Text("Select a company first.", color = GapMuted, modifier = Modifier.padding(16.dp), fontFamily = PlusJakartaSans)
                }
            } else {
                VendorOperationsScreen(companyId = companyId, onBack = onClose)
            }
        }
        BusinessGapPage.MomentsSettings -> BusinessMomentsSettingsScreen(
            onBack = { onPage(BusinessGapPage.Moments) },
            onOpenMilestone = { onPage(BusinessGapPage.Milestone) },
            onOpenVisibility = { onPage(BusinessGapPage.Visibility) },
        )
        BusinessGapPage.Milestone -> BusinessMilestoneSettingsScreen(onBack = { onPage(BusinessGapPage.MomentsSettings) })
        BusinessGapPage.Visibility -> BusinessVisibilitySettingsScreen(onBack = { onPage(BusinessGapPage.MomentsSettings) })
        BusinessGapPage.LocationPicker,
        BusinessGapPage.LocationDashboard,
        BusinessGapPage.LocationConfig,
        BusinessGapPage.Inheritance -> {
            if (companyId.isNullOrBlank()) {
                GapScreen(title = "Locations", onBack = onBackToSettings) {
                    Text("Select a company first.", color = GapMuted, modifier = Modifier.padding(16.dp), fontFamily = PlusJakartaSans)
                }
            } else {
                BusinessLocationFlow(
                    companyId = companyId,
                    page = page,
                    selected = location,
                    onSelect = onLocation,
                    onPage = { onPage(it) },
                    onBackToSettings = onBackToSettings,
                )
            }
        }
    }
}
