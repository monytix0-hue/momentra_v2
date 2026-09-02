package com.example.momentra.ui.shell.group

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.GroupFinancePayloadDto
import com.example.momentra.data.api.GroupMemoryItemDto
import com.example.momentra.data.api.GroupMemoryPayloadDto
import com.example.momentra.data.api.GroupPulsePayloadDto
import com.example.momentra.data.repository.GroupSliceRepository
import com.example.momentra.data.security.BalanceMask
import com.example.momentra.data.security.SecurityPreferences
import com.example.momentra.ui.theme.PlusJakartaSans
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Figma 575:14470 — Group Memory active tab (live API only). */
@Composable
fun GroupMemoryActiveContent(
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
        } ?: run {
            if (pulse == null && finance == null) loading = true
        }
        loadGroupMemoryTab(repository, momentId).fold(
            onSuccess = { data ->
                memory = data.memory
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

    if (loading && memory == null && pulse == null && finance == null) {
        GroupActiveLoading(modifier.fillMaxSize())
        return
    }

    val total = finance?.totals?.firstOrNull()
    val currency = total?.currencyCode ?: "INR"
    val items = memory?.items.orEmpty()
    val peopleCount = pulse?.participantCount ?: 0
    val timelineAccents = listOf(
        Color(0xFF14B8A6),
        Color(0xFFB45309),
        Color(0xFFA855F7),
        GroupActiveTheme.AccentOrange,
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GroupActiveTheme.Bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .padding(bottom = 56.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        error?.let { Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans) }

        MemoryHeroCard(
            title = momentTitle ?: "Group Memory",
            peopleCount = peopleCount,
            memoryCount = items.size.coerceAtLeast(memory?.memoryCount ?: 0),
        )

        GroupSectionCard(title = "Memory Timeline") {
            if (items.isEmpty()) {
                GroupEmptySection(
                    message = "Timeline empty",
                    detail = "Shared memories will appear here — nothing is invented.",
                )
            } else {
                items.forEachIndexed { index, item ->
                    MemoryTimelineRow(
                        item = item,
                        accent = timelineAccents[index % timelineAccents.size],
                        glyph = memoryGlyph(index),
                    )
                }
            }
        }

        GroupSectionCard(title = "Milestone Wall", badge = { GroupApiGapBadge() }) {
            GroupEmptySection(message = "No milestones yet", detail = "Milestone capture is not live for groups.")
        }

        GroupSectionCard(
            title = "Gallery",
            badge = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GroupComingSoonBadge()
                    GroupApiGapBadge()
                }
            },
        ) {
            GroupEmptySection(message = "No photos yet", detail = "Shared gallery requires group media API.")
        }

        GroupSectionCard(title = "People Impact") {
            Text(
                if (peopleCount > 0) "$peopleCount people shaped this shared story"
                else "No participants yet",
                color = GroupActiveTheme.Text,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
            GroupMetricTile("Participants", "$peopleCount")
        }

        GroupSectionCard(title = "Budget Reflection") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFFE8621A), GroupActiveTheme.Brand)))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Planned", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontFamily = PlusJakartaSans)
                        Text(
                            BalanceMask.mask(GroupFinanceFormat.formatMoney(total?.budgetTotal, currency), hide),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSans,
                        )
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Actual", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontFamily = PlusJakartaSans)
                        Text(
                            BalanceMask.mask(GroupFinanceFormat.formatMoney(total?.expenseTotal, currency), hide),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSans,
                        )
                    }
                }
                val util = GroupFinanceFormat.utilizationPercent(total?.expenseTotal, total?.budgetTotal)
                GroupProgressBar(percent = util)
                Text(
                    if (util > 0) "$util% of planned budget used"
                    else "Budget utilization appears when planned and actual amounts exist",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    fontFamily = PlusJakartaSans,
                )
            }
        }

        GroupSectionCard(title = "Memory Intelligence", badge = { GroupComingSoonBadge() }) {
            GroupEmptySection(
                message = "Insights coming soon",
                detail = "AI memory intelligence for groups is on the roadmap.",
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.horizontalGradient(listOf(GroupActiveTheme.Brand, GroupActiveTheme.AccentOrange)))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Preserve this moment",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSans,
            )
            Text(
                "Capture a photo, caption, or milestone for the group timeline.",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp,
                fontFamily = PlusJakartaSans,
            )
            GroupCtaButton(label = "Preserve this moment", enabled = true, onClick = onOpenQuickAdd)
        }
    }
}

@Composable
private fun MemoryHeroCard(
    title: String,
    peopleCount: Int,
    memoryCount: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFE8621A), GroupActiveTheme.Brand, Color(0xFF3D2A24))))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Text("📷", fontSize = 24.sp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans)
                Text(
                    "What you'll remember from this trip",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MemoryHeroChip("$peopleCount people")
            MemoryHeroChip(
                if (memoryCount > 0) "$memoryCount memories captured" else "No memories yet",
            )
        }
    }
}

@Composable
private fun MemoryHeroChip(label: String) {
    Text(
        label,
        color = Color.White,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = PlusJakartaSans,
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(Color.White.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
private fun MemoryTimelineRow(
    item: GroupMemoryItemDto,
    accent: Color,
    glyph: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF181716))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(accent),
        )
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(glyph, fontSize = 16.sp)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                item.title ?: item.memoryId.orEmpty(),
                color = GroupActiveTheme.Text,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
            formatMemoryInstant(item.occurredAt)?.let {
                Text(it, color = GroupActiveTheme.Secondary, fontSize = 11.sp, fontFamily = PlusJakartaSans)
            }
        }
    }
}

private fun memoryGlyph(index: Int): String = when (index % 4) {
    0 -> "🌱"
    1 -> "🗺️"
    2 -> "💰"
    else -> "🌅"
}

private fun formatMemoryInstant(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    return runCatching {
        OffsetDateTime.parse(raw).format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.US))
    }.getOrElse {
        raw.take(10)
    }
}
