package com.example.momentra.ui.shell.group.shared

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.GroupExpenseListItemDto
import com.example.momentra.data.api.GroupFinancePayloadDto
import com.example.momentra.data.api.GroupLifeBookingDto
import com.example.momentra.data.api.GroupLifePlanningItemDto
import com.example.momentra.data.api.GroupLifeUpdateDto
import com.example.momentra.data.api.GroupMemoryItemDto
import com.example.momentra.data.api.GroupPollItemDto
import com.example.momentra.data.api.GroupPulsePayloadDto
import com.example.momentra.data.repository.GroupSliceRepository
import kotlinx.coroutines.launch
import com.example.momentra.ui.theme.PlusJakartaSans
import java.util.Locale

/** Figma 575:14327 — Group Moments active tab (live API only). */
@Composable
fun GroupMomentsActiveContent(
    momentId: String?,
    momentTitle: String?,
    refreshToken: Long,
    momentTypeCode: String? = null,
    onCreateMoment: () -> Unit = {},
    repository: GroupSliceRepository = remember { GroupSliceRepository() },
    modifier: Modifier = Modifier,
) {
    val chrome = MomentsChrome.Trip
    var loading by remember { mutableStateOf(true) }
    var pulse by remember { mutableStateOf<GroupPulsePayloadDto?>(null) }
    var finance by remember { mutableStateOf<GroupFinancePayloadDto?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var planningItems by remember { mutableStateOf<List<GroupLifePlanningItemDto>>(emptyList()) }
    var bookings by remember { mutableStateOf<List<GroupLifeBookingDto>>(emptyList()) }
    var updates by remember { mutableStateOf<List<GroupLifeUpdateDto>>(emptyList()) }
    var polls by remember { mutableStateOf<List<GroupPollItemDto>>(emptyList()) }
    var memoryItems by remember { mutableStateOf<List<GroupMemoryItemDto>>(emptyList()) }
    var expenses by remember { mutableStateOf<List<GroupExpenseListItemDto>>(emptyList()) }
    var memoryCount by remember { mutableIntStateOf(0) }
    var selectedPollId by remember { mutableStateOf<String?>(null) }
    var pollsListOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var scheduleOpen by remember { mutableStateOf(false) }

    LaunchedEffect(refreshToken, momentId) {
        if (momentId.isNullOrBlank()) {
            loading = false
            pulse = null
            finance = null
            planningItems = emptyList()
            bookings = emptyList()
            updates = emptyList()
            polls = emptyList()
            memoryItems = emptyList()
            expenses = emptyList()
            memoryCount = 0
            return@LaunchedEffect
        }
        error = null
        GroupTabDataCache.peekPulse(momentId)?.let { cached ->
            pulse = cached.pulse
            finance = cached.finance
            loading = false
        } ?: run { loading = true }
        loadGroupPulseTab(repository, momentId).fold(
            onSuccess = { data ->
                pulse = data.pulse
                finance = data.finance
                loading = false
            },
            onFailure = { e ->
                error = e.message
                loading = false
            },
        )
        val plans = repository.listPlanningItems(momentId).getOrNull()?.items
        val books = repository.listBookings(momentId).getOrNull()?.items
        val upds = repository.listUpdates(momentId).getOrNull()?.items
        val pollList = repository.listPolls(momentId).getOrNull()?.items
        if (plans != null || books != null || upds != null || pollList != null) {
            planningItems = plans.orEmpty()
            bookings = books.orEmpty()
            updates = upds.orEmpty()
            polls = pollList.orEmpty()
        } else {
            repository.getLife(momentId).onSuccess { facet ->
                val life = facet.payload
                planningItems = life?.planningItems.orEmpty()
                bookings = life?.bookings.orEmpty()
                updates = life?.updates.orEmpty()
            }
        }
        repository.listMemories(momentId).onSuccess {
            memoryItems = it.items
            memoryCount = it.memoryCount.takeIf { c -> c > 0 } ?: it.items.size
        }.onFailure {
            repository.getMemory(momentId).onSuccess {
                memoryItems = it.payload?.items.orEmpty()
                memoryCount = it.payload?.memoryCount?.takeIf { c -> c > 0 } ?: memoryItems.size
            }
        }
        expenses = repository.listGroupExpenses(momentId, 10).getOrNull()?.items.orEmpty()
    }

    if (loading && pulse == null) {
        GroupActiveLoading(modifier.fillMaxSize())
        return
    }

    val budgetTotal = finance?.totals?.firstOrNull()?.budgetTotal
    val expenseTotal = finance?.totals?.firstOrNull()?.expenseTotal
    val currency = finance?.totals?.firstOrNull()?.currencyCode ?: "INR"
    val peopleCount = pulse?.participantCount ?: 0
    val plansPct = planningPlansPercent(planningItems)
    val momentsValue = if (memoryCount > 0) memoryCount else memoryItems.size
    val status = "PLANNING"
    val title = momentTitle ?: "Shared Moments"
    val dayGroups = itineraryDayGroups(planningItems)
    val upcoming = remember(bookings, planningItems, finance, peopleCount) {
        buildMomentsUpcomingEvents(bookings, planningItems, finance)
    }

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
            title = title,
            status = status,
            stats = listOf(
                Triple("PEOPLE", "$peopleCount", listOf(Color(0xFF14B8A6), Color(0xFF0F766E))),
                Triple("PLANS", "$plansPct%", listOf(Color(0xFFFF8E63), Color(0xFFE8744F))),
                Triple("BUDGET", GroupFinanceFormat.compactMoney(budgetTotal, currency), listOf(Color(0xFFE88A4F), Color(0xFFC2410C))),
                Triple("MOMENTS", "$momentsValue", listOf(Color(0xFFA855F7), Color(0xFF7C3AED))),
            ),
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
                MomentsItineraryDayCard(
                    dayIndex = index + 1,
                    day = day,
                    title = first?.title ?: "Plan",
                    timeLabel = formatPlanningTime(first?.dueAt) ?: "All day",
                    chrome = chrome,
                )
            }
        }

        MomentsSectionHeader("Updates / Feed  📱", chrome)
        if (updates.isEmpty()) {
            GroupEmptySection("No updates yet", "Share a status update from Quick Add.")
        } else {
            updates.take(3).forEachIndexed { index, item ->
                MomentsUpdateFeedRow(item = item, index = index, chrome = chrome)
            }
        }

        MomentsSectionHeader("Shared Gallery  📸", chrome)
        MemoryPhotoGalleryStrip(
            items = memoryItems,
            emptyMessage = "No photos yet",
            emptyDetail = "Add a memory with a photo from Quick Add.",
            text = chrome.text,
            muted = chrome.secondary,
            field = chrome.card,
            border = chrome.border,
            showMediaCountBadge = true,
        )

        MomentsSectionHeader("Bookings  🛎️", chrome)
        if (bookings.isEmpty()) {
            GroupEmptySection("No bookings yet", "Add a booking from Quick Add when ready.")
        } else {
            bookings.take(4).forEach { MomentsBookingCard(it, chrome) }
        }

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

        MomentsQuickAddCta(chrome = chrome, onClick = onCreateMoment)
    }

    PlanningScheduleSheet(
        items = planningItems,
        visible = scheduleOpen,
        onDismiss = { scheduleOpen = false },
        momentTypeCode = momentTypeCode,
        accent = Color(0xFF14B8A6),
        surface = chrome.bg,
        field = chrome.card,
        border = chrome.border,
        text = chrome.text,
        muted = chrome.secondary,
    )

    GroupPollsListSheet(
        visible = pollsListOpen,
        momentTitle = momentTitle,
        chrome = MomentsChrome.Trip,
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
