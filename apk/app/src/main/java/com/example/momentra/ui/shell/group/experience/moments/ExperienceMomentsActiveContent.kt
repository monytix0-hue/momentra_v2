package com.example.momentra.ui.shell.group.experience.moments

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.GroupFinancePayloadDto
import com.example.momentra.data.api.GroupLifePayloadDto
import com.example.momentra.data.api.GroupLifePlanningItemDto
import com.example.momentra.data.api.GroupMemoryItemDto
import com.example.momentra.data.api.GroupPollItemDto
import com.example.momentra.data.api.GroupPulsePayloadDto
import com.example.momentra.data.repository.GroupSliceRepository
import com.example.momentra.ui.shell.group.shared.GroupActiveLoading
import com.example.momentra.ui.shell.group.shared.GroupFinanceFormat
import com.example.momentra.ui.shell.group.shared.GroupTabDataCache
import com.example.momentra.ui.shell.group.shared.MemoryPhotoGalleryStrip
import com.example.momentra.ui.shell.group.shared.MomentsPlanningHeader
import com.example.momentra.ui.shell.group.shared.MomentsPlanningRecentRow
import com.example.momentra.ui.shell.group.shared.MomentsUrgentUpdateRow
import com.example.momentra.ui.shell.group.shared.PlanningScheduleSheet
import com.example.momentra.ui.shell.group.shared.PollDetailSheet
import com.example.momentra.ui.shell.group.shared.loadGroupPulseTab
import com.example.momentra.ui.shell.group.shared.recentOpenPlanningItems
import com.example.momentra.ui.theme.PlusJakartaSans
import com.example.momentra.ui.shell.group.experience.create.ExperienceActiveTheme
import com.example.momentra.ui.shell.group.experience.create.ExperienceEmptyBlock
import com.example.momentra.ui.shell.group.experience.create.ExperienceSectionCard
import com.example.momentra.ui.shell.group.experience.create.ExperienceStatCard

/** Figma 584:15500 / 584:16218 — Experience Moments. Live APIs only. */
@Composable
fun ExperienceMomentsActiveContent(
    theme: ExperienceActiveTheme,
    momentId: String?,
    momentTitle: String?,
    refreshToken: Long,
    momentTypeCode: String? = null,
    onOpenQuickAdd: () -> Unit = {},
    repository: GroupSliceRepository = remember { GroupSliceRepository() },
    modifier: Modifier = Modifier,
) {
    var loading by remember { mutableStateOf(true) }
    var pulse by remember { mutableStateOf<GroupPulsePayloadDto?>(null) }
    var finance by remember { mutableStateOf<GroupFinancePayloadDto?>(null) }
    var life by remember { mutableStateOf<GroupLifePayloadDto?>(null) }
    var planningItems by remember { mutableStateOf<List<GroupLifePlanningItemDto>>(emptyList()) }
    var polls by remember { mutableStateOf<List<GroupPollItemDto>>(emptyList()) }
    var memoryItems by remember { mutableStateOf<List<GroupMemoryItemDto>>(emptyList()) }
    var selectedPollId by remember { mutableStateOf<String?>(null) }
    var scheduleOpen by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(refreshToken, momentId) {
        if (momentId.isNullOrBlank()) {
            loading = false
            return@LaunchedEffect
        }
        error = null
        GroupTabDataCache.peekPulse(momentId)?.let { cached ->
            title = cached.title
            pulse = cached.pulse
            finance = cached.finance
            loading = false
        } ?: run { loading = true }
        loadGroupPulseTab(repository, momentId).fold(
            onSuccess = { data ->
                title = data.title
                pulse = data.pulse
                finance = data.finance
                loading = false
            },
            onFailure = { e ->
                error = e.message
                loading = false
            },
        )
        repository.getLife(momentId).onSuccess { facet ->
            life = facet.payload
            GroupTabDataCache.putLife(momentId, facet.payload)
        }
        planningItems = repository.listPlanningItems(momentId).getOrNull()?.items
            ?: life?.planningItems.orEmpty()
        polls = repository.listPolls(momentId).getOrNull()?.items.orEmpty()
        repository.listMemories(momentId).onSuccess { memoryItems = it.items }
            .onFailure {
                repository.getMemory(momentId).onSuccess { memoryItems = it.payload?.items.orEmpty() }
            }
    }

    if (loading && pulse == null && finance == null) {
        GroupActiveLoading(modifier.fillMaxSize())
        return
    }

    val budgetTotal = finance?.totals?.firstOrNull()?.budgetTotal
    val currency = finance?.totals?.firstOrNull()?.currencyCode ?: "INR"
    val peopleCount = pulse?.participantCount ?: 0
    val openTasks = pulse?.openTaskCount ?: life?.openTaskCount ?: 0
    val displayTitle = momentTitle ?: title ?: "${theme.typeLabel} Moments"
    val allPlans = if (planningItems.isNotEmpty()) planningItems else life?.planningItems.orEmpty()
    val recentPlans = recentOpenPlanningItems(allPlans)
    val bookings = life?.bookings.orEmpty()
    val updates = life?.updates.orEmpty()

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
                .background(theme.card)
                .border(1.dp, theme.border, RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("SHARED EXPERIENCE", color = theme.secondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
            Text(displayTitle, color = theme.text, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                ExperienceStatCard("PEOPLE", "$peopleCount", theme.statGradients[0], Modifier.weight(1f))
                ExperienceStatCard("BUDGET", GroupFinanceFormat.compactMoney(budgetTotal, currency), theme.statGradients[1], Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                ExperienceStatCard("UPDATES", "${updates.size}", theme.statGradients[2], Modifier.weight(1f))
                ExperienceStatCard("TASKS", "$openTasks", theme.statGradients[0], Modifier.weight(1f))
            }
        }

        ExperienceSectionCard(theme, "Planning") {
            MomentsPlanningHeader(
                title = "Recent plans",
                text = theme.text,
                muted = theme.secondary,
                accent = theme.accent,
                onOpenSchedule = { scheduleOpen = true },
            )
            if (recentPlans.isEmpty()) {
                ExperienceEmptyBlock(theme, "No timeline items yet", "Add a planning item from Quick Add — nothing is invented.")
            } else {
                recentPlans.forEach { item ->
                    MomentsPlanningRecentRow(
                        item = item,
                        momentTypeCode = momentTypeCode,
                        text = theme.text,
                        muted = theme.secondary,
                        accent = theme.accent,
                        field = theme.bg,
                        border = theme.border,
                    )
                }
            }
        }

        ExperienceSectionCard(theme, "Bookings") {
            if (bookings.isEmpty()) {
                ExperienceEmptyBlock(theme, "No bookings yet", "Add a booking from Quick Add when ready.")
            } else {
                bookings.forEach { Text(it.title ?: it.bookingId.orEmpty(), color = theme.text, fontSize = 13.sp, fontFamily = PlusJakartaSans) }
            }
        }

        ExperienceSectionCard(theme, "Polls") {
            if (polls.isEmpty()) {
                ExperienceEmptyBlock(theme, "No polls yet", "Create a poll from Quick Add to decide together.")
            } else {
                polls.forEach { item ->
                    Text(
                        item.question ?: item.pollId.orEmpty(),
                        color = theme.text,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = PlusJakartaSans,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(theme.bg)
                            .border(1.dp, theme.border, RoundedCornerShape(12.dp))
                            .clickable { item.pollId?.let { selectedPollId = it } }
                            .padding(12.dp),
                    )
                }
            }
        }

        ExperienceSectionCard(theme, "Updates") {
            if (updates.isEmpty()) {
                ExperienceEmptyBlock(theme, "No updates yet", "Share a status update from Quick Add.")
            } else {
                updates.take(8).forEach { item ->
                    MomentsUrgentUpdateRow(
                        item = item,
                        text = theme.text,
                        muted = theme.secondary,
                        field = theme.bg,
                        border = theme.border,
                    )
                }
            }
        }

        ExperienceSectionCard(theme, "Shared Gallery") {
            MemoryPhotoGalleryStrip(
                items = memoryItems,
                emptyMessage = "No photos yet",
                emptyDetail = "Add a memory with a photo from Quick Add.",
                text = theme.text,
                muted = theme.secondary,
                field = theme.bg,
                border = theme.border,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(theme.heroGradient)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Add to the ${theme.typeLabel.lowercase()} story", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
            Text("Add a plan, expense, memory, poll or update.", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, fontFamily = PlusJakartaSans)
            Text(
                "+ Open Quick Add",
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

    PlanningScheduleSheet(
        items = allPlans,
        visible = scheduleOpen,
        onDismiss = { scheduleOpen = false },
        momentTypeCode = momentTypeCode,
        accent = theme.accent,
        surface = theme.card,
        field = theme.bg,
        border = theme.border,
        text = theme.text,
        muted = theme.secondary,
    )
    selectedPollId?.let { pollId ->
        PollDetailSheet(
            pollId = pollId,
            visible = true,
            onDismiss = { selectedPollId = null },
            onSaved = {},
            repository = repository,
        )
    }
}
