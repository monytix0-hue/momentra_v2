package com.example.momentra.ui.shell.group.living

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.GroupFinancePayloadDto
import com.example.momentra.data.api.GroupMemoryPayloadDto
import com.example.momentra.data.api.GroupPulsePayloadDto
import com.example.momentra.data.repository.GroupSliceRepository
import com.example.momentra.data.security.BalanceMask
import com.example.momentra.data.security.SecurityPreferences
import com.example.momentra.ui.shell.group.GroupActiveLoading
import com.example.momentra.ui.shell.group.GroupFinanceFormat
import com.example.momentra.ui.shell.group.GroupProgressBar
import com.example.momentra.ui.shell.group.GroupTabDataCache
import com.example.momentra.ui.shell.group.loadGroupMemoryTab
import com.example.momentra.ui.theme.PlusJakartaSans

/** Shared Living Memory. Live APIs only. */
@Composable
fun LivingMemoryActiveContent(
    theme: LivingActiveTheme,
    momentId: String?,
    momentTitle: String?,
    refreshToken: Long,
    onOpenQuickAdd: () -> Unit = {},
    repository: GroupSliceRepository = remember { GroupSliceRepository() },
    modifier: Modifier = Modifier,
) {
    var loading by remember { mutableStateOf(true) }
    var memory by remember { mutableStateOf<GroupMemoryPayloadDto?>(null) }
    var finance by remember { mutableStateOf<GroupFinancePayloadDto?>(null) }
    var pulse by remember { mutableStateOf<GroupPulsePayloadDto?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val hide = SecurityPreferences(LocalContext.current).hideBalances()

    LaunchedEffect(refreshToken, momentId) {
        if (momentId.isNullOrBlank()) {
            loading = false
            return@LaunchedEffect
        }
        error = null
        GroupTabDataCache.peekPulse(momentId)?.let { cached ->
            finance = cached.finance
            pulse = cached.pulse
            loading = false
        }
        GroupTabDataCache.peekMemory(momentId)?.let { cached ->
            memory = cached.memory
            finance = cached.finance ?: finance
            pulse = cached.pulse ?: pulse
            loading = false
        }
        if (pulse == null && finance == null) loading = true
        loadGroupMemoryTab(repository, momentId).fold(
            onSuccess = { data ->
                memory = data.memory
                finance = data.finance ?: finance
                pulse = data.pulse ?: pulse
                loading = false
            },
            onFailure = { e ->
                error = e.message
                loading = false
            },
        )
    }

    if (loading && pulse == null && finance == null && memory == null) {
        GroupActiveLoading(modifier.fillMaxSize())
        return
    }

    val total = finance?.totals?.firstOrNull()
    val currency = total?.currencyCode ?: "INR"
    val items = memory?.items.orEmpty()
    val memoryCount = maxOf(items.size, memory?.memoryCount ?: 0)
    val people = pulse?.participantCount ?: 0
    val displayTitle = momentTitle ?: "${theme.typeLabel} Memory"
    val funded = LivingMemoryMath.fundedPercent(total?.contributionTotal, total?.budgetTotal)
    val positions = finance?.positions.orEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(theme.bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .padding(bottom = 56.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        error?.let { Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(theme.heroGradient)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("GROUP MEMORY", color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
            Text(displayTitle, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans)
            Text("$people people shaped this household", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, fontFamily = PlusJakartaSans)
            Text(
                if (memoryCount > 0) "$memoryCount memories captured" else "No memories yet",
                color = theme.darkText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.9f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }

        LivingSectionCard(theme, "Memory Timeline") {
            if (items.isEmpty()) {
                LivingEmptyBlock(theme, "Timeline empty", "Shared memories will appear here — nothing is invented.")
            } else {
                items.forEach { item ->
                    Text(
                        item.title ?: "Memory",
                        color = theme.text,
                        fontSize = 13.sp,
                        fontFamily = PlusJakartaSans,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(theme.accentSoft)
                            .border(1.dp, theme.accent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                    )
                }
            }
        }

        LivingSectionCard(theme, "Memory Gallery") {
            LivingEmptyBlock(theme, "No photos yet", "Shared gallery requires group media API.")
        }

        LivingSectionCard(theme, "People Impact") {
            Text("Participants: $people", color = theme.text, fontSize = 13.sp, fontFamily = PlusJakartaSans)
            if (positions.isEmpty()) {
                LivingEmptyBlock(theme, "No contributors yet", "People appear after contributions and activity are recorded.")
            } else {
                positions.take(3).forEach { pos ->
                    val amount = pos.contributionTotal.takeIf { it.isNotBlank() && it != "0" } ?: pos.netPosition
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(pos.participantId.take(8) + "…", color = theme.text, fontSize = 12.sp, fontFamily = PlusJakartaSans)
                        Text(
                            BalanceMask.mask(GroupFinanceFormat.formatMoney(amount, pos.currencyCode), hide),
                            color = theme.secondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = PlusJakartaSans,
                        )
                    }
                }
            }
        }

        LivingSectionCard(theme, theme.budgetTitle) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Goal ${BalanceMask.mask(GroupFinanceFormat.formatMoney(total?.budgetTotal, currency), hide)}",
                    color = theme.text,
                    fontSize = 12.sp,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    if (theme.includesContribution) {
                        "Collected ${BalanceMask.mask(GroupFinanceFormat.formatMoney(total?.contributionTotal, currency), hide)}"
                    } else {
                        "Spent ${BalanceMask.mask(GroupFinanceFormat.formatMoney(total?.expenseTotal, currency), hide)}"
                    },
                    color = theme.text,
                    fontSize = 12.sp,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier.weight(1f),
                )
            }
            if (funded != null) {
                Text("$funded% funded", color = theme.accentLight, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
                GroupProgressBar(percent = funded)
            } else {
                Text("Set a budget to track household progress.", color = theme.secondary, fontSize = 12.sp, fontFamily = PlusJakartaSans)
            }
        }

        LivingSectionCard(theme, "Memory Intelligence") {
            LivingEmptyBlock(theme, "Insights coming soon", "AI memory intelligence for groups is on the roadmap — no invented copy.")
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(theme.heroGradient)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Preserve this household", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
            Text(
                "Capture a memory, photo, or update for the ${theme.typeLabel.lowercase()} story.",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp,
                fontFamily = PlusJakartaSans,
            )
            Text(
                "+ Capture Memory",
                color = theme.darkText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .clickable(onClick = onOpenQuickAdd)
                    .padding(vertical = 14.dp),
            )
        }
    }
}

private object LivingMemoryMath {
    fun fundedPercent(contributionTotal: String?, budgetTotal: String?): Int? {
        val budget = runCatching { java.math.BigDecimal(budgetTotal?.trim().orEmpty()) }.getOrNull() ?: return null
        if (budget.compareTo(java.math.BigDecimal.ZERO) <= 0) return null
        val contrib = runCatching { java.math.BigDecimal(contributionTotal?.trim().orEmpty()) }.getOrNull()
            ?: java.math.BigDecimal.ZERO
        return contrib.multiply(java.math.BigDecimal(100))
            .divide(budget, 0, java.math.RoundingMode.HALF_UP)
            .toInt()
            .coerceIn(0, 100)
    }
}
