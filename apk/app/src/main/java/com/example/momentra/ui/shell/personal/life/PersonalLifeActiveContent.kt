package com.example.momentra.ui.shell.personal.life

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.PersonalLifeDto
import com.example.momentra.data.repository.PersonalSliceRepository
import com.example.momentra.ui.shell.personal.shared.PersonalLifeAllocation
import com.example.momentra.ui.shell.personal.shared.PersonalLifeAllocationMode
import com.example.momentra.ui.shell.personal.shared.PersonalLifeFamilyStatus
import com.example.momentra.ui.shell.personal.shared.PersonalLifeInsight
import com.example.momentra.ui.shell.personal.shared.PersonalLifeMoneySnapshot
import com.example.momentra.ui.shell.personal.shared.PersonalLifeSummaryModel
import com.example.momentra.ui.shell.personal.shared.PersonalLifeWeekFamilyCount
import com.example.momentra.ui.shell.personal.shared.PersonalLifeWeekSummary
import com.example.momentra.ui.shell.personal.shared.PersonalLifeWeekTier
import com.example.momentra.ui.theme.PlusJakartaSans

private val LifeBg = Color(0xFF14121B)
private val LifeCard = Color(0xFF1C1B2E)
private val LifeCardAlt = Color(0xFF161B26)
private val LifeText = Color(0xFFE5E0EE)
private val LifeMuted = Color(0xFFC9C4D8)
private val LifeDim = Color(0xFF8C8C9E)
private val LifePurple = Color(0xFF7C5CFC)
private val LifeGreen = Color(0xFF10B981)
private val LifeRed = Color(0xFFEF4444)
private val LifeAmber = Color(0xFFF59E0B)
private val LifeBlue = Color(0xFF3B82F6)
private val LifePink = Color(0xFFE12A9E)
private val BorderSoft = Color.White.copy(alpha = 0.08f)

/** M3 Life — overall state only (five honest blocks via PersonalLifeSummaryModel). */
@Composable
fun PersonalLifeActiveContent(
    refreshToken: Long,
    onLogRecovery: () -> Unit,
    onLogSpend: () -> Unit,
    onOpenAdd: () -> Unit,
    repository: PersonalSliceRepository = remember { PersonalSliceRepository() },
    modifier: Modifier = Modifier,
) {
    var loading by remember { mutableStateOf(true) }
    var life by remember { mutableStateOf<PersonalLifeDto?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    /** null = All; filters This week only. */
    var selectedFamilyFilter by remember { mutableStateOf<String?>(null) }
    var allocationMode by remember { mutableStateOf(PersonalLifeAllocationMode.ACTIVITY) }

    LaunchedEffect(refreshToken) {
        if (life != null) loading = false else loading = true
        error = null
        repository.getLife().fold(
            onSuccess = { life = it; loading = false },
            onFailure = { error = it.message; loading = false },
        )
    }

    if (loading && life == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = LifePurple)
        }
        return
    }

    val data = life
    if (data == null) {
        Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                error ?: "Life projection unavailable",
                color = LifeMuted,
                fontSize = 14.sp,
                fontFamily = PlusJakartaSans,
            )
        }
        return
    }

    val summary = remember(data, selectedFamilyFilter) {
        PersonalLifeSummaryModel.from(data, selectedFamilyFilter)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LifeBg)
            .verticalScroll(rememberScrollState()),
    ) {
        LifeChipRow(
            selectedFamilyCode = selectedFamilyFilter,
            onSelectFamilyCode = { selectedFamilyFilter = it },
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            error?.let {
                Text(it, color = LifeRed, fontSize = 12.sp, fontFamily = PlusJakartaSans)
            }
            LifeOverviewBlock(summary)
            LifeThisWeekBlock(summary.weekSummary)
            LifeWhereWentBlock(
                summary = summary,
                mode = allocationMode,
                onModeChange = { allocationMode = it },
            )
            summary.slipping?.let { insight ->
                LifeInsightBlock(
                    chromeTitle = "Something slipping",
                    insight = insight,
                    accent = LifeRed,
                    tintBg = Color(0xFF2A1520),
                    onLogRecovery = onLogRecovery,
                    onLogSpend = onLogSpend,
                    onOpenAdd = onOpenAdd,
                )
            }
            summary.working?.let { insight ->
                LifeInsightBlock(
                    chromeTitle = "What’s working",
                    insight = insight,
                    accent = LifeGreen,
                    tintBg = LifeCard,
                    onLogRecovery = onLogRecovery,
                    onLogSpend = onLogSpend,
                    onOpenAdd = onOpenAdd,
                )
            }
            summary.moneySnapshot?.let { LifeMoneyBlock(it) }
            summary.globalScore?.let { score ->
                Text(
                    "Overall score · $score/${summary.scoreMax}",
                    color = LifeDim,
                    fontSize = 11.sp,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LifeChipRow(
    selectedFamilyCode: String?,
    onSelectFamilyCode: (String?) -> Unit,
) {
    val chips = listOf(
        LifeFamilyChip("All", null, LifeMuted),
        LifeFamilyChip("Everyday", "LIFE_OPERATIONS", LifePurple),
        LifeFamilyChip("Future", "FUTURE_BUILDING", LifeBlue),
        LifeFamilyChip("Lifestyle", "LIFESTYLE", LifeAmber),
        LifeFamilyChip("People", "RELATIONSHIPS", LifePink),
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0C0F15)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            chips.forEach { chip ->
                val active = selectedFamilyCode == chip.familyCode
                val accent = chip.dot
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (active) accent.copy(alpha = 0.12f) else LifeCardAlt)
                        .border(
                            width = if (active) 1.5.dp else 1.dp,
                            color = if (active) accent.copy(alpha = 0.5f) else BorderSoft,
                            shape = RoundedCornerShape(20.dp),
                        )
                        .clickable { onSelectFamilyCode(chip.familyCode) }
                        .padding(
                            horizontal = if (active) 12.dp else 10.dp,
                            vertical = 6.dp,
                        ),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(accent),
                    )
                    Text(
                        chip.label,
                        color = if (active) Color.White else LifeDim,
                        fontSize = if (active) 12.sp else 11.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                        fontFamily = PlusJakartaSans,
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFF1E293B).copy(alpha = 0.3f)),
        )
    }
}

@Composable
private fun LifeOverviewBlock(summary: PersonalLifeSummaryModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LifeCard)
            .border(1.dp, BorderSoft, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Life overview",
            color = LifePurple,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            summary.familyStates.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    row.forEach { state ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(LifeCardAlt)
                                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                state.label,
                                color = LifeDim,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = PlusJakartaSans,
                            )
                            Text(
                                state.status.label,
                                color = statusColor(state.status),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = PlusJakartaSans,
                            )
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun LifeThisWeekBlock(week: PersonalLifeWeekSummary) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LifeCard)
            .border(1.dp, BorderSoft, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "This week",
            color = LifePurple,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
        Text(
            week.filterLabel?.let { "This week · $it" }
                ?: "Across Everyday, Future, Lifestyle, and People",
            color = LifeDim,
            fontSize = 11.sp,
            fontFamily = PlusJakartaSans,
        )
        when (week.tier) {
            PersonalLifeWeekTier.EMPTY -> {
                Text(
                    "Nothing logged this week yet",
                    color = LifeMuted,
                    fontSize = 13.sp,
                    fontFamily = PlusJakartaSans,
                )
            }
            PersonalLifeWeekTier.THIN -> {
                FamilyCountLines(week.familyCounts)
            }
            PersonalLifeWeekTier.PARTIAL, PersonalLifeWeekTier.RICH -> {
                week.sentence?.let {
                    Text(
                        it,
                        color = LifeText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = PlusJakartaSans,
                    )
                }
                FamilyCountLines(week.familyCounts)
            }
        }
    }
}

@Composable
private fun FamilyCountLines(counts: List<PersonalLifeWeekFamilyCount>) {
    counts.filter { it.periodLogs > 0 }.forEach { row ->
        val unit = if (row.periodLogs == 1) "activity" else "activities"
        Text(
            "${row.label} · ${row.periodLogs} $unit",
            color = LifeMuted,
            fontSize = 13.sp,
            fontFamily = PlusJakartaSans,
        )
    }
}

@Composable
private fun LifeWhereWentBlock(
    summary: PersonalLifeSummaryModel,
    mode: PersonalLifeAllocationMode,
    onModeChange: (PersonalLifeAllocationMode) -> Unit,
) {
    val allocation: PersonalLifeAllocation = if (mode == PersonalLifeAllocationMode.ACTIVITY) {
        summary.activityAllocation
    } else {
        summary.moneyAllocation
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LifeCard)
            .border(1.dp, BorderSoft, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Where your life went",
            color = LifePurple,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(LifeCardAlt)
                .padding(3.dp),
        ) {
            AllocationToggle(
                label = "Activity",
                active = mode == PersonalLifeAllocationMode.ACTIVITY,
                onClick = { onModeChange(PersonalLifeAllocationMode.ACTIVITY) },
                modifier = Modifier.weight(1f),
            )
            AllocationToggle(
                label = "Money",
                active = mode == PersonalLifeAllocationMode.MONEY,
                onClick = { onModeChange(PersonalLifeAllocationMode.MONEY) },
                modifier = Modifier.weight(1f),
            )
        }
        if (!allocation.hasData) {
            Text(
                if (mode == PersonalLifeAllocationMode.ACTIVITY) {
                    "No activity to allocate this week"
                } else {
                    "No spend to allocate this week"
                },
                color = LifeMuted,
                fontSize = 13.sp,
                fontFamily = PlusJakartaSans,
            )
        } else {
            allocation.slices.forEach { slice ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            slice.label,
                            color = LifeText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = PlusJakartaSans,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "${slice.percent}%",
                            color = LifeMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = PlusJakartaSans,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.White.copy(alpha = 0.06f)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(slice.percent / 100f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(familyAccent(slice.familyCode)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AllocationToggle(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        label,
        color = if (active) LifeText else LifeDim,
        fontSize = 12.sp,
        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
        fontFamily = PlusJakartaSans,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) LifePurple.copy(alpha = 0.25f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun LifeInsightBlock(
    chromeTitle: String,
    insight: PersonalLifeInsight,
    accent: Color,
    tintBg: Color,
    onLogRecovery: () -> Unit,
    onLogSpend: () -> Unit,
    onOpenAdd: () -> Unit,
) {
    val ctaClick = resolveLifeCtaHandler(
        insight.ctaAction,
        onLogRecovery,
        onLogSpend,
        onOpenAdd,
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(tintBg)
            .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            chromeTitle,
            color = accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
        Text(
            insight.headline,
            color = LifeText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
        Text(
            insight.body,
            color = LifeMuted,
            fontSize = 13.sp,
            fontFamily = PlusJakartaSans,
        )
        if (insight.ctaLabel != null && ctaClick != null) {
            Text(
                insight.ctaLabel,
                color = accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.12f))
                    .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .clickable(onClick = ctaClick)
                    .padding(vertical = 10.dp),
                    textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun LifeMoneyBlock(money: PersonalLifeMoneySnapshot) {
    val hideBalances = com.example.momentra.data.security.SecurityPreferences(LocalContext.current).hideBalances()
    fun moneyText(amount: Double): String =
        com.example.momentra.data.security.BalanceMask.mask(
            PersonalLifeSummaryModel.formatMoney(amount, money.currencyCode),
            hideBalances,
        )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LifeCard)
            .border(1.dp, BorderSoft, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "Money supporting your life",
            color = LifePurple,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
        if (money.incomeTotal > 0) {
            MoneyRow("Income", moneyText(money.incomeTotal))
        }
        if (money.expenseTotal > 0) {
            MoneyRow("Spent", moneyText(money.expenseTotal))
        }
        money.available?.let {
            MoneyRow("Available", moneyText(it))
        }
        if (money.byFamilySpend.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.06f)),
            )
            Spacer(Modifier.height(4.dp))
            money.byFamilySpend.forEach { slice ->
                MoneyRow(
                    slice.label,
                    moneyText(slice.value),
                )
            }
        }
    }
}

@Composable
private fun MoneyRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            color = LifeMuted,
            fontSize = 13.sp,
            fontFamily = PlusJakartaSans,
            modifier = Modifier.weight(1f),
        )
        Text(
            value,
            color = LifeText,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
        )
    }
}

private fun statusColor(status: PersonalLifeFamilyStatus): Color = when (status) {
    PersonalLifeFamilyStatus.STRONG -> LifeGreen
    PersonalLifeFamilyStatus.GROWING -> LifeBlue
    PersonalLifeFamilyStatus.STEADY -> LifeMuted
    PersonalLifeFamilyStatus.QUIET -> LifeDim
    PersonalLifeFamilyStatus.NEEDS_ATTENTION -> LifeAmber
}

private fun familyAccent(code: String): Color = when (code.uppercase()) {
    "LIFE_OPERATIONS" -> LifePurple
    "FUTURE_BUILDING" -> LifeGreen
    "LIFESTYLE" -> LifeAmber
    "RELATIONSHIPS" -> LifePink
    else -> LifeBlue
}

private data class LifeFamilyChip(
    val label: String,
    val familyCode: String?,
    val dot: Color,
)

private fun resolveLifeCtaHandler(
    ctaAction: String?,
    onLogRecovery: () -> Unit,
    onLogSpend: () -> Unit,
    onOpenAdd: () -> Unit,
): (() -> Unit)? = when (ctaAction?.uppercase()) {
    "LOG_RECOVERY" -> onLogRecovery
    "LOG_SPEND" -> onLogSpend
    "OPEN_ADD" -> onOpenAdd
    else -> null
}
