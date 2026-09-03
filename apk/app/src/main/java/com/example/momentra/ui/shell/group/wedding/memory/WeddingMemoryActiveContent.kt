package com.example.momentra.ui.shell.group.wedding.memory

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R
import com.example.momentra.data.api.GroupFinancePayloadDto
import com.example.momentra.data.api.GroupMemoryPayloadDto
import com.example.momentra.data.api.GroupPulsePayloadDto
import com.example.momentra.data.repository.GroupSliceRepository
import com.example.momentra.data.security.BalanceMask
import com.example.momentra.data.security.SecurityPreferences
import com.example.momentra.ui.shell.group.shared.GroupActiveLoading
import com.example.momentra.ui.shell.group.shared.GroupFinanceFormat
import com.example.momentra.ui.shell.group.shared.GroupProgressBar
import com.example.momentra.ui.shell.group.shared.GroupTabDataCache
import com.example.momentra.ui.shell.group.shared.loadGroupMemoryTab
import com.example.momentra.ui.theme.PlusJakartaSans
import com.example.momentra.ui.shell.group.wedding.create.WeddingActiveTheme
import com.example.momentra.ui.shell.group.wedding.create.WeddingComingSoonBlock
import com.example.momentra.ui.shell.group.wedding.create.WeddingEmptyBlock
import com.example.momentra.ui.shell.group.wedding.create.WeddingFadeIn
import com.example.momentra.ui.shell.group.wedding.create.WeddingPinkCta
import com.example.momentra.ui.shell.group.wedding.create.WeddingSectionCard

/** Figma 575:15203 — Wedding Memory. Live APIs only; no demo seeds. */
@Composable
fun WeddingMemoryActiveContent(
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
            memory = null
            finance = null
            pulse = null
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
                var merged = data.memory
                repository.listMemories(momentId).getOrNull()?.let { listed ->
                    if (listed.items.isNotEmpty()) {
                        merged = (merged ?: GroupMemoryPayloadDto()).copy(
                            items = listed.items,
                            memoryCount = listed.memoryCount.takeIf { it > 0 } ?: listed.items.size,
                        )
                    }
                }
                memory = merged
                finance = data.finance
                pulse = data.pulse
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
    val memoryCount = memory?.memoryCount ?: items.size
    val people = pulse?.participantCount ?: 0
    val displayTitle = momentTitle ?: "Group Memory"
    val utilization = GroupFinanceFormat.utilizationPercent(total?.expenseTotal, total?.budgetTotal)
    val positions = finance?.positions.orEmpty()

    WeddingFadeIn {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(WeddingActiveTheme.Bg)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .padding(bottom = 56.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            error?.let { Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(WeddingActiveTheme.HeroRadius))
                    .background(WeddingActiveTheme.HeroGradient)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(R.drawable.wedding_hub_cake),
                    contentDescription = null,
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop,
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("GROUP MEMORY", color = WeddingActiveTheme.DarkText.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                    Text(displayTitle, color = WeddingActiveTheme.DarkText, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans)
                    Text(
                        "$people people shaped this moment",
                        color = WeddingActiveTheme.DarkText.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontFamily = PlusJakartaSans,
                    )
                    MemoryPill(if (memoryCount > 0) "$memoryCount memories" else "No memories yet")
                }
            }

            WeddingSectionCard(title = "📖  Memory Timeline") {
                if (items.isEmpty()) {
                    WeddingEmptyBlock(
                        message = "Timeline empty",
                        detail = "Shared memories will appear here — nothing is invented.",
                    )
                } else {
                    items.forEach { item ->
                        Text(
                            item.title ?: item.memoryId.orEmpty().ifEmpty { "Memory" },
                            color = WeddingActiveTheme.Text,
                            fontSize = 13.sp,
                            fontFamily = PlusJakartaSans,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(WeddingActiveTheme.AccentSoft)
                                .border(1.dp, WeddingActiveTheme.Accent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                        )
                    }
                }
            }

            WeddingSectionCard(title = "🖼  Memory Gallery") {
                WeddingEmptyBlock(
                    message = "No photos yet",
                    detail = "Shared gallery requires group media API.",
                )
            }

            WeddingSectionCard(title = "👥  People Impact") {
                Text(
                    "$people people shaped this wedding story",
                    color = WeddingActiveTheme.Text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                )
                if (positions.isEmpty()) {
                    WeddingEmptyBlock(
                        message = "No contributors yet",
                        detail = "People appear after expenses and activity are recorded.",
                    )
                } else {
                    positions.take(3).forEach { pos ->
                        Text(
                            "${pos.participantId.take(8)} · ${BalanceMask.mask(GroupFinanceFormat.formatMoney(pos.netPosition, pos.currencyCode), hide)}",
                            color = WeddingActiveTheme.Secondary,
                            fontSize = 12.sp,
                            fontFamily = PlusJakartaSans,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(WeddingActiveTheme.MagentaGradient)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("WEDDING BUDGET", color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Planned", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontFamily = PlusJakartaSans)
                        Text(
                            BalanceMask.mask(GroupFinanceFormat.formatMoney(total?.budgetTotal, currency), hide).ifBlank { "—" },
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSans,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Actual", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontFamily = PlusJakartaSans)
                        Text(
                            BalanceMask.mask(GroupFinanceFormat.formatMoney(total?.expenseTotal, currency), hide).ifBlank { "—" },
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSans,
                        )
                    }
                }
                if (total?.budgetTotal != null) {
                    Text(
                        "$utilization% of planned",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = PlusJakartaSans,
                    )
                    GroupProgressBar(percent = utilization)
                } else {
                    Text(
                        "Set a budget to track planned vs actual.",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        fontFamily = PlusJakartaSans,
                    )
                }
            }

            WeddingSectionCard(title = "⚡  Memory Intelligence") {
                WeddingComingSoonBlock(
                    message = "Insights coming soon",
                    detail = "AI memory intelligence for groups is on the roadmap — no invented copy.",
                )
            }

            WeddingPinkCta(
                title = "Preserve this moment",
                subtitle = "Capture a photo, milestone, lesson or shared reflection.",
                buttonLabel = "Capture Memory",
                enabled = true,
                onClick = onOpenQuickAdd,
                outlinedButton = true,
            )
        }
    }
}

@Composable
private fun MemoryPill(label: String) {
    Text(
        label,
        color = WeddingActiveTheme.DarkText,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = PlusJakartaSans,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.75f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}
