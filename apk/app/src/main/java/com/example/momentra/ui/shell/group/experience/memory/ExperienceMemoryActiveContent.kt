package com.example.momentra.ui.shell.group.experience.memory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.example.momentra.data.api.GroupParticipantDto
import com.example.momentra.data.api.GroupPulsePayloadDto
import com.example.momentra.data.repository.GroupSliceRepository
import com.example.momentra.data.security.BalanceMask
import com.example.momentra.data.security.SecurityPreferences
import com.example.momentra.ui.shell.group.experience.create.ExperienceActiveTheme
import com.example.momentra.ui.shell.group.shared.GroupActiveLoading
import com.example.momentra.ui.shell.group.shared.GroupActiveTheme
import com.example.momentra.ui.shell.group.shared.GroupApiGapBadge
import com.example.momentra.ui.shell.group.shared.GroupComingSoonBadge
import com.example.momentra.ui.shell.group.shared.GroupCtaButton
import com.example.momentra.ui.shell.group.shared.GroupEmptySection
import com.example.momentra.ui.shell.group.shared.GroupFinanceFormat
import com.example.momentra.ui.shell.group.shared.GroupMetricTile
import com.example.momentra.ui.shell.group.shared.GroupProgressBar
import com.example.momentra.ui.shell.group.shared.GroupSectionCard
import com.example.momentra.ui.shell.group.shared.GroupTabDataCache
import com.example.momentra.ui.shell.group.shared.loadGroupMemoryTab
import com.example.momentra.ui.shell.group.shared.MemoryPhotoGalleryStrip
import com.example.momentra.ui.shell.group.shared.RemoteMemoryImage
import com.example.momentra.ui.shell.group.shared.primaryDownloadUrl
import com.example.momentra.ui.theme.PlusJakartaSans
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Figma 575:14470 — Experience Memory. Live APIs only. */
@Composable
fun ExperienceMemoryActiveContent(
    theme: ExperienceActiveTheme,
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
    var participants by remember { mutableStateOf<List<GroupParticipantDto>>(emptyList()) }
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
            participants = cached.participants
            loading = false
        }
        if (pulse == null && finance == null) loading = true
        loadGroupMemoryTab(repository, momentId).fold(
            onSuccess = { data ->
                memory = data.memory
                finance = data.finance ?: finance
                pulse = data.pulse ?: pulse
                participants = data.participants
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
    val utilization = GroupFinanceFormat.utilizationPercent(total?.expenseTotal, total?.budgetTotal)
    val positions = finance?.positions.orEmpty()
    val timelineAccents = listOf(
        Color(0xFF14B8A6),
        Color(0xFFB45309),
        Color(0xFFA855F7),
        GroupActiveTheme.AccentOrange,
    )
    val nameById = remember(participants) {
        participants.associate { it.participantId to (it.displayName ?: it.participantId.take(8)) }
    }

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

        ExperienceMemoryHeroCard(
            title = displayTitle,
            peopleCount = people,
            memoryCount = memoryCount,
            theme = theme,
        )

        GroupSectionCard(title = "Memory Timeline") {
            if (items.isEmpty()) {
                GroupEmptySection(
                    message = "Timeline empty",
                    detail = "Shared memories will appear here — nothing is invented.",
                )
            } else {
                items.forEachIndexed { index, item ->
                    ExperienceMemoryTimelineRow(
                        item = item,
                        accent = timelineAccents[index % timelineAccents.size],
                        glyph = experienceMemoryGlyph(index),
                    )
                }
            }
        }

        GroupSectionCard(title = "Milestone Wall", badge = { GroupApiGapBadge() }) {
            GroupEmptySection(
                message = "No milestones yet",
                detail = "Milestone capture is not live for groups.",
            )
        }

        GroupSectionCard(title = "Memory Gallery") {
            MemoryPhotoGalleryStrip(
                items = items,
                emptyMessage = "No photos yet",
                emptyDetail = "Add a memory with a photo from Quick Add.",
                text = GroupActiveTheme.Text,
                muted = GroupActiveTheme.Secondary,
                field = GroupActiveTheme.Card,
                border = GroupActiveTheme.Border,
            )
        }

        GroupSectionCard(title = "People Impact") {
            Text(
                if (people > 0) "$people people shaped this shared story" else "No participants yet",
                color = GroupActiveTheme.Text,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
            GroupMetricTile("Participants", "$people")
            if (positions.isNotEmpty()) {
                positions.take(3).forEach { pos ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            nameById[pos.participantId] ?: pos.participantId.take(8),
                            color = GroupActiveTheme.Text,
                            fontSize = 12.sp,
                            fontFamily = PlusJakartaSans,
                        )
                        Text(
                            BalanceMask.mask(GroupFinanceFormat.formatMoney(pos.netPosition, pos.currencyCode), hide),
                            color = GroupActiveTheme.Secondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = PlusJakartaSans,
                        )
                    }
                }
            }
        }

        GroupSectionCard(title = theme.budgetTitle) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.horizontalGradient(listOf(theme.accentSolid, theme.accentLight)))
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
                GroupProgressBar(percent = utilization)
                Text(
                    if (utilization > 0) "$utilization% of planned budget used"
                    else "Set a budget to track planned vs actual.",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    fontFamily = PlusJakartaSans,
                )
            }
        }

        GroupSectionCard(title = "Memory Intelligence", badge = { GroupComingSoonBadge() }) {
            GroupEmptySection(
                message = "Insights coming soon",
                detail = "AI memory intelligence for groups is on the roadmap — no invented copy.",
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
                "Capture a memory, photo, or update for the ${theme.typeLabel.lowercase()} story.",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp,
                fontFamily = PlusJakartaSans,
            )
            GroupCtaButton(label = "+ Capture Memory", enabled = true, onClick = onOpenQuickAdd)
        }
    }
}

@Composable
private fun ExperienceMemoryHeroCard(
    title: String,
    peopleCount: Int,
    memoryCount: Int,
    theme: ExperienceActiveTheme,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(theme.accentSolid, theme.accentLight, Color(0xFF3D2A24)),
                ),
            )
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
                Text(theme.heroEmoji, fontSize = 24.sp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans)
                Text(
                    "What you'll remember from this ${theme.typeLabel.lowercase()}",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExperienceMemoryHeroChip("$peopleCount people")
            ExperienceMemoryHeroChip(
                if (memoryCount > 0) "$memoryCount memories captured" else "No memories yet",
            )
        }
    }
}

@Composable
private fun ExperienceMemoryHeroChip(label: String) {
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
private fun ExperienceMemoryTimelineRow(
    item: GroupMemoryItemDto,
    accent: Color,
    glyph: String,
) {
    val thumbUrl = item.primaryDownloadUrl()
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
                .size(width = 3.dp, height = 40.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(accent),
        )
        if (thumbUrl != null) {
            RemoteMemoryImage(
                url = thumbUrl,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(glyph, fontSize = 16.sp)
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                item.title ?: item.memoryId.orEmpty(),
                color = GroupActiveTheme.Text,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
            formatExperienceMemoryInstant(item.occurredAt)?.let {
                Text(it, color = GroupActiveTheme.Secondary, fontSize = 11.sp, fontFamily = PlusJakartaSans)
            }
        }
    }
}

private fun experienceMemoryGlyph(index: Int): String = when (index % 4) {
    0 -> "📷"
    1 -> "🎉"
    2 -> "🌅"
    else -> "✨"
}

private fun formatExperienceMemoryInstant(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    return runCatching {
        OffsetDateTime.parse(raw).format(DateTimeFormatter.ofPattern("d MMM · h:mm a", Locale.US))
    }.getOrNull()
}
