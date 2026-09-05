package com.example.momentra.ui.shell.group.experience.moments

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.GroupAttendanceItemDto
import com.example.momentra.data.api.GroupExpenseListItemDto
import com.example.momentra.data.api.GroupFinancePayloadDto
import com.example.momentra.data.api.GroupLifeBookingDto
import com.example.momentra.data.api.GroupLifePlanningItemDto
import com.example.momentra.data.api.GroupLifeUpdateDto
import com.example.momentra.data.api.GroupMemoryItemDto
import com.example.momentra.data.api.GroupPollItemDto
import com.example.momentra.data.api.GroupPulsePayloadDto
import com.example.momentra.data.api.GroupVendorItemDto
import com.example.momentra.data.repository.GroupSliceRepository
import kotlinx.coroutines.launch
import com.example.momentra.ui.shell.group.experience.create.ExperienceActiveTheme
import com.example.momentra.ui.shell.group.shared.GroupActiveLoading
import com.example.momentra.ui.shell.group.shared.GroupEmptySection
import com.example.momentra.ui.shell.group.shared.GroupFinanceFormat
import com.example.momentra.ui.shell.group.shared.GroupTabDataCache
import com.example.momentra.ui.shell.group.shared.MemoryPhotoGalleryStrip
import com.example.momentra.ui.shell.group.shared.MomentsChrome
import com.example.momentra.ui.shell.group.shared.MomentsExpensesCard
import com.example.momentra.ui.shell.group.shared.MomentsHeroHeader
import com.example.momentra.ui.shell.group.shared.MomentsItineraryDayCard
import com.example.momentra.ui.shell.group.shared.MomentsPollPreviewCard
import com.example.momentra.ui.shell.group.shared.MomentsQuickAddCta
import com.example.momentra.ui.shell.group.shared.MomentsSectionHeader
import com.example.momentra.ui.shell.group.shared.MomentsSimpleRowCard
import com.example.momentra.ui.shell.group.shared.MomentsUpdateFeedRow
import com.example.momentra.ui.shell.group.shared.MomentsUpcomingEventCard
import com.example.momentra.ui.shell.group.shared.PlanningScheduleSheet
import com.example.momentra.ui.shell.group.shared.GroupPollsListSheet
import com.example.momentra.ui.shell.group.shared.PollDetailSheet
import com.example.momentra.ui.shell.group.shared.buildMomentsUpcomingEvents
import com.example.momentra.ui.shell.group.shared.formatPlanningTime
import com.example.momentra.ui.shell.group.shared.itineraryDayGroups
import com.example.momentra.ui.shell.group.shared.loadGroupPulseTab
import com.example.momentra.ui.theme.PlusJakartaSans
import java.util.Locale

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
    val chrome = MomentsChrome.experience(theme)
    val isOfficeOuting = theme.typeLabel == ExperienceActiveTheme.OfficeOuting.typeLabel
    var loading by remember { mutableStateOf(true) }
    var pulse by remember { mutableStateOf<GroupPulsePayloadDto?>(null) }
    var finance by remember { mutableStateOf<GroupFinancePayloadDto?>(null) }
    var facetStatus by remember { mutableStateOf<String?>(null) }
    var lifeOpenTasks by remember { mutableIntStateOf(0) }
    var planningItems by remember { mutableStateOf<List<GroupLifePlanningItemDto>>(emptyList()) }
    var bookings by remember { mutableStateOf<List<GroupLifeBookingDto>>(emptyList()) }
    var updates by remember { mutableStateOf<List<GroupLifeUpdateDto>>(emptyList()) }
    var polls by remember { mutableStateOf<List<GroupPollItemDto>>(emptyList()) }
    var memoryItems by remember { mutableStateOf<List<GroupMemoryItemDto>>(emptyList()) }
    var vendors by remember { mutableStateOf<List<GroupVendorItemDto>>(emptyList()) }
    var attendance by remember { mutableStateOf<List<GroupAttendanceItemDto>>(emptyList()) }
    var expenses by remember { mutableStateOf<List<GroupExpenseListItemDto>>(emptyList()) }
    var memoryCount by remember { mutableIntStateOf(0) }
    var selectedPollId by remember { mutableStateOf<String?>(null) }
    var pollsListOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
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
            facetStatus = facet.status
            GroupTabDataCache.putLife(momentId, facet.payload)
            val life = facet.payload
            lifeOpenTasks = life?.openTaskCount ?: 0
            planningItems = repository.listPlanningItems(momentId).getOrNull()?.items
                ?: life?.planningItems.orEmpty()
            bookings = repository.listBookings(momentId).getOrNull()?.items
                ?: life?.bookings.orEmpty()
            updates = repository.listUpdates(momentId).getOrNull()?.items
                ?: life?.updates.orEmpty()
        }.onFailure {
            planningItems = repository.listPlanningItems(momentId).getOrNull()?.items.orEmpty()
            bookings = repository.listBookings(momentId).getOrNull()?.items.orEmpty()
            updates = repository.listUpdates(momentId).getOrNull()?.items.orEmpty()
        }
        polls = repository.listPolls(momentId).getOrNull()?.items.orEmpty()
        vendors = repository.listGroupVendors(momentId).getOrNull()?.items.orEmpty()
        attendance = repository.listAttendance(momentId).getOrNull()?.items.orEmpty()
        expenses = repository.listGroupExpenses(momentId, 10).getOrNull()?.items.orEmpty()
        repository.listMemories(momentId).onSuccess {
            memoryItems = it.items
            memoryCount = it.memoryCount.takeIf { c -> c > 0 } ?: it.items.size
        }.onFailure {
            repository.getMemory(momentId).onSuccess {
                memoryItems = it.payload?.items.orEmpty()
                memoryCount = it.payload?.memoryCount?.takeIf { c -> c > 0 } ?: memoryItems.size
            }
        }
    }

    if (loading && pulse == null && finance == null) {
        GroupActiveLoading(modifier.fillMaxSize())
        return
    }

    val budgetTotal = finance?.totals?.firstOrNull()?.budgetTotal
    val expenseTotal = finance?.totals?.firstOrNull()?.expenseTotal
    val currency = finance?.totals?.firstOrNull()?.currencyCode ?: "INR"
    val peopleCount = pulse?.participantCount ?: 0
    val openTasks = pulse?.openTaskCount ?: lifeOpenTasks
    val confirmed = attendance.count {
        val s = (it.attendanceStatus ?: "").uppercase(Locale.US)
        s == "CONFIRMED" || s == "ATTENDING" || s == "YES"
    }
    val moments = if (memoryCount > 0) memoryCount else memoryItems.size
    val displayTitle = momentTitle ?: title ?: "${theme.typeLabel} Moments"
    val status = (facetStatus ?: "PLANNING").uppercase(Locale.US)
    val dayGroups = itineraryDayGroups(planningItems)
    val upcoming = remember(bookings, planningItems, finance) {
        buildMomentsUpcomingEvents(bookings, planningItems, finance)
    }
    val g0 = listOf(theme.accentSolid, theme.accent)
    val g1 = listOf(theme.accent, theme.accentLight)
    val g2 = listOf(theme.accentLight, theme.accentSolid)
    val g3 = listOf(theme.accent, theme.accentSolid)
    val heroStats = if (isOfficeOuting) {
        listOf(
            Triple("GUESTS", "$peopleCount", g0),
            Triple("CONFIRMED", "$confirmed", g1),
            Triple("BUDGET", GroupFinanceFormat.compactMoney(budgetTotal, currency), g2),
            Triple("TASKS", "$openTasks", g3),
        )
    } else {
        listOf(
            Triple("GUESTS", "$peopleCount", g0),
            Triple("BUDGET", GroupFinanceFormat.compactMoney(budgetTotal, currency), g1),
            Triple("MOMENTS", "$moments", g2),
            Triple("TASKS", "$openTasks", g3),
        )
    }
    val showVendors = !isOfficeOuting || vendors.isNotEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(chrome.bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .padding(bottom = 56.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        error?.let { Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans) }

        MomentsHeroHeader(
            eyebrow = "SHARED EXPERIENCE",
            title = displayTitle,
            status = status,
            stats = heroStats,
            chrome = chrome,
        )

        MomentsSectionHeader("Polls  🗳️", chrome, onViewAll = { pollsListOpen = true })
        if (polls.isEmpty()) {
            GroupEmptySection("No polls yet", "Create a poll from Quick Add to decide together.")
        } else {
            polls.take(2).forEach { poll ->
                MomentsPollPreviewCard(poll = poll, chrome = chrome, onClick = { poll.pollId?.let { selectedPollId = it } })
            }
        }

        MomentsSectionHeader("Itinerary", chrome, onViewAll = { scheduleOpen = true })
        if (dayGroups.isEmpty()) {
            GroupEmptySection("No itinerary days yet", "Add a planning item from Quick Add — nothing is invented.")
        } else {
            dayGroups.forEachIndexed { index, (day, items) ->
                val first = items.firstOrNull()
                val timeLabel = listOfNotNull(
                    formatPlanningTime(first?.dueAt),
                    "${items.size} item${if (items.size == 1) "" else "s"}",
                ).joinToString(" · ")
                MomentsItineraryDayCard(
                    dayIndex = index + 1,
                    day = day,
                    title = first?.title ?: "Plans",
                    timeLabel = timeLabel.ifBlank { "All day" },
                    chrome = chrome,
                )
            }
        }

        MomentsSectionHeader("Updates / Feed  📱", chrome)
        if (updates.isEmpty()) {
            GroupEmptySection("No updates yet", "Share a status update from Quick Add.")
        } else {
            updates.take(5).forEachIndexed { index, item ->
                MomentsUpdateFeedRow(item = item, index = index, chrome = chrome)
            }
        }

        if (showVendors) {
            MomentsSectionHeader("Vendors  🏪", chrome)
            if (vendors.isEmpty()) {
                GroupEmptySection("No vendors yet", "Add a vendor from Quick Add when ready.")
            } else {
                vendors.take(5).forEach { vendor ->
                    MomentsSimpleRowCard(
                        title = vendor.vendorName ?: "Vendor",
                        chrome = chrome,
                        meta = vendor.vendorType,
                        status = vendor.status,
                    )
                }
            }
        }

        MomentsSectionHeader("Attendance  ✅", chrome)
        if (attendance.isEmpty()) {
            GroupEmptySection(
                if (isOfficeOuting) "No attendance yet" else "No RSVPs yet",
                "Record attendance from Quick Add.",
            )
        } else {
            attendance.take(8).forEach { row ->
                MomentsSimpleRowCard(
                    title = row.displayName ?: "Guest",
                    chrome = chrome,
                    meta = row.note,
                    status = row.attendanceStatus,
                )
            }
        }

        MomentsSectionHeader("Shared Gallery  📸", chrome)
        MemoryPhotoGalleryStrip(
            items = memoryItems,
            emptyMessage = "Gallery empty",
            emptyDetail = "Add a memory with a photo from Quick Add.",
            text = chrome.text,
            muted = chrome.secondary,
            field = chrome.card,
            border = chrome.border,
            showMediaCountBadge = true,
        )

        MomentsSectionHeader("Upcoming Events  🗓", chrome)
        if (upcoming.isEmpty()) {
            GroupEmptySection("Nothing upcoming", "Near-term bookings and plans will show here.")
        } else {
            upcoming.forEachIndexed { index, event ->
                MomentsUpcomingEventCard(event = event, highlight = index == 0, chrome = chrome)
            }
        }

        MomentsSectionHeader("Expenses & Budget  💸", chrome)
        MomentsExpensesCard(
            spent = expenseTotal,
            currency = currency,
            peopleCount = peopleCount,
            expenses = expenses,
            chrome = chrome,
        )

        MomentsQuickAddCta(
            chrome = chrome,
            onClick = onOpenQuickAdd,
            title = "Add to the ${theme.typeLabel.lowercase(Locale.US)} story",
            subtitle = "Add a plan, expense, memory, poll or update.",
        )
    }

    PlanningScheduleSheet(
        items = planningItems,
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
    GroupPollsListSheet(
        visible = pollsListOpen,
        momentTitle = momentTitle ?: title,
        chrome = MomentsChrome.experience(theme),
        polls = polls,
        onDismiss = { pollsListOpen = false },
        onChanged = {
            if (!momentId.isNullOrBlank()) {
                scope.launch {
                    polls = repository.listPolls(momentId).getOrNull()?.items.orEmpty()
                }
            }
        },
    )

    selectedPollId?.let { pollId ->
        PollDetailSheet(
            pollId = pollId,
            visible = true,
            onDismiss = { selectedPollId = null },
            onSaved = {
                if (!momentId.isNullOrBlank()) {
                    scope.launch {
                        polls = repository.listPolls(momentId).getOrNull()?.items.orEmpty()
                    }
                }
            },
            repository = repository,
        )
    }
}
