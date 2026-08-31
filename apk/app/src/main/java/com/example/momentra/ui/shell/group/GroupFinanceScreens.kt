package com.example.momentra.ui.shell.group

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.momentra.data.api.GroupFinancePayloadDto
import com.example.momentra.data.api.GroupFinancePositionDto
import com.example.momentra.data.api.GroupParticipantDto
import com.example.momentra.data.repository.GroupSliceRepository
import com.example.momentra.data.security.BalanceMask
import com.example.momentra.data.security.SecurityPreferences
import com.example.momentra.ui.shell.group.wedding.WeddingActiveTheme
import com.example.momentra.ui.theme.PlusJakartaSans
import java.math.BigDecimal
import java.math.RoundingMode

/** Chrome tokens for Group Finance / Expense Splits (Figma 1257:9021 / 1257:8866). */
data class GroupFinanceChrome(
    val bg: Color,
    val card: Color,
    val border: Color,
    val text: Color,
    val secondary: Color,
    val accent: Color,
    val accentLight: Color,
    val accentSoft: Color,
    val green: Color = Color(0xFF4ADE80),
    val orange: Color = Color(0xFFFF7A3D),
    val heroGradient: Brush,
) {
    companion object {
        fun forWedding() = GroupFinanceChrome(
            bg = WeddingActiveTheme.Bg,
            card = WeddingActiveTheme.Card,
            border = WeddingActiveTheme.Border,
            text = WeddingActiveTheme.Text,
            secondary = WeddingActiveTheme.Secondary,
            accent = WeddingActiveTheme.Accent,
            accentLight = WeddingActiveTheme.AccentLight,
            accentSoft = WeddingActiveTheme.AccentSoft,
            heroGradient = WeddingActiveTheme.HeroGradient,
        )

        fun forGeneric() = GroupFinanceChrome(
            bg = TripSheetTokens.Bg,
            card = TripSheetTokens.Field,
            border = TripSheetTokens.Border,
            text = TripSheetTokens.Text,
            secondary = TripSheetTokens.Muted,
            accent = TripSheetTokens.Accent,
            accentLight = TripSheetTokens.AccentEnd,
            accentSoft = Color(0x33FF7A3D),
            heroGradient = Brush.horizontalGradient(listOf(TripSheetTokens.Accent, TripSheetTokens.AccentEnd)),
        )

        fun forHouseParty(): GroupFinanceChrome {
            val t = com.example.momentra.ui.shell.group.experience.ExperienceActiveTheme.HouseParty
            return GroupFinanceChrome(
                bg = t.bg,
                card = t.card,
                border = t.border,
                text = t.text,
                secondary = t.secondary,
                accent = t.accent,
                accentLight = t.accentLight,
                accentSoft = t.accentSoft,
                heroGradient = t.heroGradient,
            )
        }

        fun forOfficeOuting(): GroupFinanceChrome {
            val t = com.example.momentra.ui.shell.group.experience.ExperienceActiveTheme.OfficeOuting
            return GroupFinanceChrome(
                bg = t.bg,
                card = t.card,
                border = t.border,
                text = t.text,
                secondary = t.secondary,
                accent = t.accent,
                accentLight = t.accentLight,
                accentSoft = t.accentSoft,
                heroGradient = t.heroGradient,
            )
        }

        fun forPurchase(theme: com.example.momentra.ui.shell.group.purchase.PurchaseActiveTheme): GroupFinanceChrome =
            GroupFinanceChrome(
                bg = theme.bg,
                card = theme.card,
                border = theme.border,
                text = theme.text,
                secondary = theme.secondary,
                accent = theme.accent,
                accentLight = theme.accentLight,
                accentSoft = theme.accentSoft,
                heroGradient = theme.heroGradient,
            )

        fun forLiving(theme: com.example.momentra.ui.shell.group.living.LivingActiveTheme): GroupFinanceChrome =
            GroupFinanceChrome(
                bg = theme.bg,
                card = theme.card,
                border = theme.border,
                text = theme.text,
                secondary = theme.secondary,
                accent = theme.accent,
                accentLight = theme.accentLight,
                accentSoft = theme.accentSoft,
                heroGradient = theme.heroGradient,
            )

        fun forFamily(family: GroupExperienceFamily): GroupFinanceChrome = when (family) {
            GroupExperienceFamily.WEDDING -> forWedding()
            GroupExperienceFamily.HOUSE_PARTY -> forHouseParty()
            GroupExperienceFamily.OFFICE_OUTING -> forOfficeOuting()
            GroupExperienceFamily.GIFT_POOL,
            GroupExperienceFamily.GROUP_PURCHASE,
            GroupExperienceFamily.SHARED_ASSET,
            GroupExperienceFamily.CUSTOM_PURCHASE,
            -> forPurchase(
                com.example.momentra.ui.shell.group.purchase.PurchaseActiveTheme.forFamily(family),
            )
            GroupExperienceFamily.FLATMATES,
            GroupExperienceFamily.FAMILY_HOUSEHOLD,
            GroupExperienceFamily.CO_LIVING,
            GroupExperienceFamily.CUSTOM_LIVING,
            -> forLiving(
                com.example.momentra.ui.shell.group.living.LivingActiveTheme.forFamily(family),
            )
            GroupExperienceFamily.SHARED_GENERIC -> forGeneric()
        }
    }
}

enum class GroupFinanceDestination {
    FINANCE,
    SPLITS,
}

/** Figma 1257:9021 — Group Finance. */
@Composable
fun GroupFinanceDetailFlow(
    visible: Boolean,
    momentId: String?,
    momentTitle: String?,
    isWedding: Boolean = false,
    experienceFamily: GroupExperienceFamily = GroupExperienceFamily.SHARED_GENERIC,
    onDismiss: () -> Unit,
    onOpenSplits: () -> Unit = {},
    onSettle: () -> Unit = {},
    repository: GroupSliceRepository = remember { GroupSliceRepository() },
) {
    if (!visible || momentId.isNullOrBlank()) return
    val family = when {
        experienceFamily != GroupExperienceFamily.SHARED_GENERIC -> experienceFamily
        isWedding -> GroupExperienceFamily.WEDDING
        else -> GroupExperienceFamily.SHARED_GENERIC
    }
    val chrome = GroupFinanceChrome.forFamily(family)
    val isWeddingChrome = family == GroupExperienceFamily.WEDDING
    val financeScreenTitle = if (family.isThemedLiving()) {
        com.example.momentra.ui.shell.group.living.LivingActiveTheme.forFamily(family).financeTitle
    } else {
        "Group Finance"
    }
    val hide = SecurityPreferences(LocalContext.current).hideBalances()
    var loading by remember { mutableStateOf(true) }
    var finance by remember { mutableStateOf<GroupFinancePayloadDto?>(null) }
    var participants by remember { mutableStateOf<List<GroupParticipantDto>>(emptyList()) }
    var title by remember { mutableStateOf(momentTitle) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(momentId, visible) {
        if (!visible) return@LaunchedEffect
        error = null
        GroupTabDataCache.peekPulse(momentId)?.finance?.let {
            finance = it
            loading = false
        } ?: run { loading = true }
        val fin = repository.getFinance(momentId)
        val people = repository.getParticipants(momentId)
        fin.onSuccess {
            title = it.title ?: title
            finance = it.payload
            loading = false
        }.onFailure {
            error = it.message
            loading = false
        }
        people.onSuccess { participants = it.participants }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(chrome.bg)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            FinanceTopBar(
                title = financeScreenTitle,
                accent = chrome.accent,
                text = chrome.text,
                onBack = onDismiss,
            )
            val total = finance?.totals?.firstOrNull()
            val currency = total?.currencyCode ?: "INR"
            val peopleCount = participants.size.takeIf { it > 0 }
                ?: finance?.positions?.size
                ?: 0
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                error?.let { Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans) }
                Text(
                    "${if (isWeddingChrome) "💕" else if (family.isThemedExperience()) "🎉" else "🌴"} ${title ?: "Group Moment"} · $peopleCount members",
                    color = chrome.accent,
                    fontSize = 12.sp,
                    fontFamily = PlusJakartaSans,
                )

                if (loading && finance == null) {
                    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = chrome.accent) }
                } else {
                    FinanceSummaryCard(finance, total, currency, hide, chrome)
                    ParticipantPositionsSection(finance?.positions.orEmpty(), participants, currency, hide, chrome)
                }
            }
            SettleOutstandingCta(
                enabled = true,
                chrome = chrome,
                onClick = onSettle,
            )
        }
    }
}

/** Figma 1257:8866 — Expense Splits / View Splits. */
@Composable
fun GroupExpenseSplitsFlow(
    visible: Boolean,
    momentId: String?,
    momentTitle: String?,
    isWedding: Boolean = false,
    experienceFamily: GroupExperienceFamily = GroupExperienceFamily.SHARED_GENERIC,
    onDismiss: () -> Unit,
    onOpenFinance: () -> Unit = {},
    onSettle: (() -> Unit)? = null,
    repository: GroupSliceRepository = remember { GroupSliceRepository() },
) {
    if (!visible || momentId.isNullOrBlank()) return
    val family = when {
        experienceFamily != GroupExperienceFamily.SHARED_GENERIC -> experienceFamily
        isWedding -> GroupExperienceFamily.WEDDING
        else -> GroupExperienceFamily.SHARED_GENERIC
    }
    val chrome = GroupFinanceChrome.forFamily(family)
    val isWeddingChrome = family == GroupExperienceFamily.WEDDING
    val hide = SecurityPreferences(LocalContext.current).hideBalances()
    var loading by remember { mutableStateOf(true) }
    var finance by remember { mutableStateOf<GroupFinancePayloadDto?>(null) }
    var participants by remember { mutableStateOf<List<GroupParticipantDto>>(emptyList()) }
    var title by remember { mutableStateOf(momentTitle) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(momentId, visible) {
        if (!visible) return@LaunchedEffect
        error = null
        GroupTabDataCache.peekPulse(momentId)?.finance?.let {
            finance = it
            loading = false
        } ?: run { loading = true }
        val fin = repository.getFinance(momentId)
        val people = repository.getParticipants(momentId)
        fin.onSuccess {
            title = it.title ?: title
            finance = it.payload
            loading = false
        }.onFailure {
            error = it.message
            loading = false
        }
        people.onSuccess { participants = it.participants }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(chrome.bg)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            FinanceTopBar(
                title = "Expense Splits",
                accent = chrome.accent,
                text = chrome.text,
                onBack = onDismiss,
            )
            val total = finance?.totals?.firstOrNull()
            val currency = total?.currencyCode ?: "INR"
            val viewer = finance?.viewerPosition
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                error?.let { Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans) }
                if (loading && finance == null) {
                    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = chrome.accent) }
                } else {
                    SplitsSummaryCard(title, total, currency, hide, chrome, isWeddingChrome || family.isThemedExperience(), onOpenFinance)
                    ViewerBalanceCard(viewer, currency, hide, chrome, onSettle = onSettle)
                    WhoOwesWhomSection(finance?.positions.orEmpty(), participants, currency, hide, chrome)
                    ExpenseBreakdownSection(finance?.positions.orEmpty(), participants, currency, hide, chrome)
                }
            }
            if (onSettle != null) {
                SettleOutstandingCta(
                    enabled = true,
                    chrome = chrome,
                    onClick = onSettle,
                )
            }
        }
    }
}

@Composable
private fun FinanceTopBar(title: String, accent: Color, text: Color, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Icon(
            Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "Back",
            tint = accent,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(onClick = onBack)
                .padding(8.dp),
        )
        Text(title, color = text, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
        Icon(
            Icons.Outlined.Tune,
            contentDescription = null,
            tint = accent.copy(alpha = 0.45f),
            modifier = Modifier.size(40.dp).padding(10.dp),
        )
    }
}

@Composable
private fun FinanceSummaryCard(
    finance: GroupFinancePayloadDto?,
    total: com.example.momentra.data.api.GroupFinanceTotalDto?,
    currency: String,
    hide: Boolean,
    chrome: GroupFinanceChrome,
) {
    val utilization = GroupFinanceFormat.utilizationPercent(total?.expenseTotal, total?.budgetTotal)
    val expense = GroupFinanceFormat.parseAmount(total?.expenseTotal)
    val budget = GroupFinanceFormat.parseAmount(total?.budgetTotal)
    val left = (budget - expense).max(BigDecimal.ZERO)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TripSheetTokens.CardRadius))
            .background(chrome.card)
            .border(1.dp, chrome.border, RoundedCornerShape(TripSheetTokens.CardRadius))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Finance Summary", color = chrome.text, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans)
        Text("TOTAL EXPENSES", color = chrome.secondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
        Text(
            BalanceMask.mask(GroupFinanceFormat.formatMoney(total?.expenseTotal, currency), hide),
            color = chrome.text,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = PlusJakartaSans,
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryTile(
                "Budget",
                BalanceMask.mask(GroupFinanceFormat.formatMoney(total?.budgetTotal, currency), hide),
                "$utilization% Utilized",
                chrome.accent,
                chrome,
                Modifier.weight(1f),
            )
            SummaryTile(
                "Contributions",
                BalanceMask.mask(GroupFinanceFormat.formatMoney(total?.contributionTotal, currency), hide),
                "Collected",
                chrome.green,
                chrome,
                Modifier.weight(1f),
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryTile(
                "Settled",
                BalanceMask.mask(GroupFinanceFormat.formatMoney(total?.settledTotal, currency), hide),
                "Recorded",
                chrome.green,
                chrome,
                Modifier.weight(1f),
            )
            val outstanding = GroupFinanceFormat.parseAmount(total?.outstandingTotal)
            SummaryTile(
                "Outstanding",
                BalanceMask.mask(GroupFinanceFormat.formatMoney(total?.outstandingTotal, currency), hide),
                if (outstanding > BigDecimal.ZERO) "Action pending" else "No action needed",
                if (outstanding > BigDecimal.ZERO) chrome.orange else chrome.green,
                chrome,
                Modifier.weight(1f),
            )
        }
        GroupProgressBar(percent = utilization, fill = chrome.accent, track = Color(0xFF332E40))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "${BalanceMask.mask(GroupFinanceFormat.formatMoney(total?.expenseTotal, currency), hide)} spent",
                color = chrome.secondary,
                fontSize = 12.sp,
                fontFamily = PlusJakartaSans,
            )
            Text(
                "${BalanceMask.mask(GroupFinanceFormat.formatMoney(left.toPlainString(), currency), hide)} left",
                color = chrome.secondary,
                fontSize = 12.sp,
                fontFamily = PlusJakartaSans,
            )
        }
        if (finance == null || total == null) {
            Text("No finance data yet — add an expense to populate this view.", color = chrome.secondary, fontSize = 12.sp, fontFamily = PlusJakartaSans)
        }
    }
}

@Composable
private fun SummaryTile(
    label: String,
    value: String,
    hint: String,
    hintColor: Color,
    chrome: GroupFinanceChrome,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(chrome.bg)
            .border(1.dp, chrome.border, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, color = chrome.secondary, fontSize = 11.sp, fontFamily = PlusJakartaSans)
        Text(value, color = chrome.text, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
        Text(hint, color = hintColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
    }
}

@Composable
private fun ParticipantPositionsSection(
    positions: List<GroupFinancePositionDto>,
    participants: List<GroupParticipantDto>,
    currency: String,
    hide: Boolean,
    chrome: GroupFinanceChrome,
) {
    val nameById = participants.associate { it.participantId to (it.displayName ?: it.participantId.take(8)) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("👥 Participant Positions", color = chrome.text, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans)
            Text(
                "${positions.size}",
                color = chrome.accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(chrome.accentSoft)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
        if (positions.isEmpty()) {
            Text("Positions appear after shared expenses are recorded.", color = chrome.secondary, fontSize = 12.sp, fontFamily = PlusJakartaSans)
        } else {
            positions.forEach { pos ->
                PositionDetailCard(
                    name = nameById[pos.participantId] ?: pos.participantId.take(8),
                    position = pos,
                    currency = currency,
                    hide = hide,
                    chrome = chrome,
                )
            }
        }
    }
}

@Composable
private fun PositionDetailCard(
    name: String,
    position: GroupFinancePositionDto,
    currency: String,
    hide: Boolean,
    chrome: GroupFinanceChrome,
) {
    val net = GroupFinanceFormat.parseAmount(position.netPosition)
    val getsBack = net >= BigDecimal.ZERO
    val netLabel = if (getsBack) {
        "+${BalanceMask.mask(GroupFinanceFormat.formatMoney(position.netPosition, currency), hide)} Gets back"
    } else {
        "-${BalanceMask.mask(GroupFinanceFormat.formatMoney(net.abs().toPlainString(), currency), hide)} Owes"
    }
    val netColor = if (getsBack) chrome.green else chrome.orange
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(chrome.card)
            .border(1.dp, chrome.border, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AvatarBubble(name, highlight = getsBack, chrome = chrome)
            Column(modifier = Modifier.weight(1f)) {
                Text(name, color = chrome.text, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
            }
            Text(netLabel, color = netColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MetricCell("Paid", BalanceMask.mask(GroupFinanceFormat.formatMoney(position.paidTotal, currency), hide), chrome, Modifier.weight(1f))
            MetricCell("Allocated", BalanceMask.mask(GroupFinanceFormat.formatMoney(position.allocatedTotal, currency), hide), chrome, Modifier.weight(1f))
            MetricCell("Payable", BalanceMask.mask(GroupFinanceFormat.formatMoney(position.payableTotal, currency), hide), chrome, Modifier.weight(1f), tint = chrome.orange)
            MetricCell("Receivable", BalanceMask.mask(GroupFinanceFormat.formatMoney(position.receivableTotal, currency), hide), chrome, Modifier.weight(1f), tint = chrome.green)
        }
    }
}

@Composable
private fun MetricCell(
    label: String,
    value: String,
    chrome: GroupFinanceChrome,
    modifier: Modifier = Modifier,
    tint: Color = chrome.text,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = chrome.secondary, fontSize = 10.sp, fontFamily = PlusJakartaSans)
        Text(value, color = tint, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
    }
}

@Composable
private fun SettleOutstandingCta(
    enabled: Boolean,
    chrome: GroupFinanceChrome,
    onClick: () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(RoundedCornerShape(999.dp))
            .background(chrome.heroGradient)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Settle Outstanding", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, fontFamily = PlusJakartaSans)
            Text(
                if (enabled) "Record a settlement against open balances" else "Settlements unavailable",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontFamily = PlusJakartaSans,
            )
        }
    }
}

@Composable
private fun SplitsSummaryCard(
    title: String?,
    total: com.example.momentra.data.api.GroupFinanceTotalDto?,
    currency: String,
    hide: Boolean,
    chrome: GroupFinanceChrome,
    isWedding: Boolean,
    onOpenFinance: () -> Unit,
) {
    val utilization = GroupFinanceFormat.utilizationPercent(total?.expenseTotal, total?.budgetTotal)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TripSheetTokens.CardRadius))
            .background(chrome.card)
            .border(1.dp, chrome.border, RoundedCornerShape(TripSheetTokens.CardRadius))
            .clickable(onClick = onOpenFinance)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${if (isWedding) "💕" else "🌴"} ${title ?: "Group Moment"}",
                color = chrome.text,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier.weight(1f),
            )
            Text(
                BalanceMask.mask(GroupFinanceFormat.formatMoney(total?.expenseTotal, currency), hide),
                color = chrome.accent,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSans,
            )
        }
        Text("TOTAL EXPENSES", color = chrome.secondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
        GroupProgressBar(percent = utilization, fill = chrome.accent, track = Color(0xFF332E40))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "Budget ${BalanceMask.mask(GroupFinanceFormat.compactMoney(total?.budgetTotal, currency), hide)}",
                color = chrome.secondary,
                fontSize = 11.sp,
                fontFamily = PlusJakartaSans,
            )
            Text(
                "Outstanding ${BalanceMask.mask(GroupFinanceFormat.compactMoney(total?.outstandingTotal, currency), hide)}",
                color = chrome.secondary,
                fontSize = 11.sp,
                fontFamily = PlusJakartaSans,
            )
        }
        Text("Tap for Group Finance →", color = chrome.accentLight, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
    }
}

@Composable
private fun ViewerBalanceCard(
    viewer: GroupFinancePositionDto?,
    currency: String,
    hide: Boolean,
    chrome: GroupFinanceChrome,
    onSettle: (() -> Unit)? = null,
) {
    val net = GroupFinanceFormat.parseAmount(viewer?.netPosition)
    val headline = when {
        viewer == null -> "No balance yet"
        net < BigDecimal.ZERO -> "You owe ${BalanceMask.mask(GroupFinanceFormat.formatMoney(net.abs().toPlainString(), currency), hide)}"
        net > BigDecimal.ZERO -> "You get back ${BalanceMask.mask(GroupFinanceFormat.formatMoney(viewer.netPosition, currency), hide)}"
        else -> "You're settled up"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(chrome.heroGradient)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("YOUR BALANCE", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
        Text(headline, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Keep group coordination high by settling up.",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 12.sp,
                fontFamily = PlusJakartaSans,
                modifier = Modifier.weight(1f),
            )
            if (onSettle != null) {
                Text(
                    "Settle Up",
                    color = chrome.accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.9f))
                        .clickable(onClick = onSettle)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun WhoOwesWhomSection(
    positions: List<GroupFinancePositionDto>,
    participants: List<GroupParticipantDto>,
    currency: String,
    hide: Boolean,
    chrome: GroupFinanceChrome,
) {
    val nameById = participants.associate { it.participantId to (it.displayName ?: it.participantId.take(8)) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("🤝 Who Owes Whom", color = chrome.text, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans)
        Text(
            "Net positions from live finance — pairwise settlement graph is not available yet.",
            color = chrome.secondary,
            fontSize = 11.sp,
            fontFamily = PlusJakartaSans,
        )
        val actionable = positions.filter {
            GroupFinanceFormat.parseAmount(it.payableTotal) > BigDecimal.ZERO ||
                GroupFinanceFormat.parseAmount(it.receivableTotal) > BigDecimal.ZERO ||
                GroupFinanceFormat.parseAmount(it.netPosition).abs() > BigDecimal.ZERO
        }
        if (actionable.isEmpty()) {
            Text("No outstanding nets yet.", color = chrome.secondary, fontSize = 12.sp, fontFamily = PlusJakartaSans)
        } else {
            actionable.forEach { pos ->
                val name = nameById[pos.participantId] ?: pos.participantId.take(8)
                val net = GroupFinanceFormat.parseAmount(pos.netPosition)
                val owes = net < BigDecimal.ZERO
                val amount = BalanceMask.mask(
                    GroupFinanceFormat.formatMoney(net.abs().toPlainString(), currency),
                    hide,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(chrome.card)
                        .border(1.dp, chrome.border, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AvatarBubble(name, highlight = !owes, chrome = chrome)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (owes) "$name owes (net)" else "$name gets back (net)",
                            color = chrome.text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSans,
                        )
                        Text(
                            if (owes) "Pending settlement" else "Receivable outstanding",
                            color = chrome.secondary,
                            fontSize = 11.sp,
                            fontFamily = PlusJakartaSans,
                        )
                    }
                    Text(amount, color = if (owes) chrome.orange else chrome.green, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans)
                    Text(
                        if (owes) "Owe" else "Get",
                        color = if (owes) chrome.orange else chrome.green,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = PlusJakartaSans,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background((if (owes) chrome.orange else chrome.green).copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpenseBreakdownSection(
    positions: List<GroupFinancePositionDto>,
    participants: List<GroupParticipantDto>,
    currency: String,
    hide: Boolean,
    chrome: GroupFinanceChrome,
) {
    val nameById = participants.associate { it.participantId to (it.displayName ?: it.participantId.take(8)) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("📊 Expense Breakdown", color = chrome.text, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans)
        if (positions.isEmpty()) {
            Text("Breakdown appears after expenses are recorded.", color = chrome.secondary, fontSize = 12.sp, fontFamily = PlusJakartaSans)
        } else {
            positions.forEach { pos ->
                val name = nameById[pos.participantId] ?: pos.participantId.take(8)
                val paid = GroupFinanceFormat.parseAmount(pos.paidTotal)
                val share = GroupFinanceFormat.parseAmount(pos.allocatedTotal)
                val net = GroupFinanceFormat.parseAmount(pos.netPosition)
                val ratio = if (share > BigDecimal.ZERO) {
                    paid.multiply(BigDecimal(100)).divide(share, 0, RoundingMode.HALF_UP).toInt().coerceIn(0, 100)
                } else {
                    0
                }
                val positive = net >= BigDecimal.ZERO
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(chrome.card)
                        .border(1.dp, chrome.border, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AvatarBubble(name, highlight = positive, chrome = chrome)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(name, color = chrome.text, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                            Text(
                                "Paid ${BalanceMask.mask(GroupFinanceFormat.compactMoney(pos.paidTotal, currency), hide)} · Share ${BalanceMask.mask(GroupFinanceFormat.compactMoney(pos.allocatedTotal, currency), hide)}",
                                color = chrome.secondary,
                                fontSize = 11.sp,
                                fontFamily = PlusJakartaSans,
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GroupProgressBar(
                                percent = ratio,
                                fill = if (positive) chrome.green else chrome.orange,
                                track = Color(0xFF332E40),
                                modifier = Modifier.weight(1f),
                            )
                            val signed = if (positive) {
                                "+${BalanceMask.mask(GroupFinanceFormat.formatMoney(pos.netPosition, currency), hide)}"
                            } else {
                                "-${BalanceMask.mask(GroupFinanceFormat.formatMoney(net.abs().toPlainString(), currency), hide)}"
                            }
                            Text(signed, color = if (positive) chrome.green else chrome.orange, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AvatarBubble(name: String, highlight: Boolean, chrome: GroupFinanceChrome) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (highlight) chrome.accent else Color.White.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.take(1).uppercase(),
            color = if (highlight) Color.White else chrome.text,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = PlusJakartaSans,
        )
    }
}

@Composable
private fun GroupProgressBar(
    percent: Int,
    fill: Color,
    track: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(track),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(percent.coerceIn(0, 100) / 100f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(fill),
        )
    }
}
