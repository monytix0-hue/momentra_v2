package com.example.momentra.ui.shell.group.trip.moments

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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.momentra.data.api.GroupContributionItemDto
import com.example.momentra.data.api.GroupExpenseListItemDto
import com.example.momentra.data.api.GroupFinancePayloadDto
import com.example.momentra.data.api.GroupLifeBookingDto
import com.example.momentra.data.api.GroupLifePlanningItemDto
import com.example.momentra.data.api.GroupLifeUpdateDto
import com.example.momentra.data.api.GroupMemoryItemDto
import com.example.momentra.data.api.GroupPollItemDto
import com.example.momentra.data.api.GroupPulsePayloadDto
import com.example.momentra.data.repository.GroupSliceRepository
import com.example.momentra.ui.shell.group.shared.ActiveTabScrollScaffold
import com.example.momentra.ui.shell.group.shared.ContributionsListSheet
import com.example.momentra.ui.shell.group.shared.ExperienceChecklistAddSheet
import com.example.momentra.ui.shell.group.shared.ExpensesListSheet
import com.example.momentra.ui.shell.group.shared.GroupActiveLoading
import com.example.momentra.ui.shell.group.shared.GroupContributionSheet
import com.example.momentra.ui.shell.group.shared.GroupEmptySection
import com.example.momentra.ui.shell.group.shared.GroupExperienceChecklistCatalog
import com.example.momentra.ui.shell.group.shared.GroupFinanceFormat
import com.example.momentra.ui.shell.group.shared.GroupPollsListSheet
import com.example.momentra.ui.shell.group.shared.GroupTabDataCache
import com.example.momentra.ui.shell.group.shared.MemoryPhotoGalleryStrip
import com.example.momentra.ui.shell.group.shared.MomentsBookingCard
import com.example.momentra.ui.shell.group.shared.MomentsChecklistSection
import com.example.momentra.ui.shell.group.shared.MomentsChrome
import com.example.momentra.ui.shell.group.shared.MomentsContributionDetailsSection
import com.example.momentra.ui.shell.group.shared.MomentsExpensesCard
import com.example.momentra.ui.shell.group.shared.MomentsHeroHeader
import com.example.momentra.ui.shell.group.shared.MomentsItineraryDayCard
import com.example.momentra.ui.shell.group.shared.MomentsPollPreviewCard
import com.example.momentra.ui.shell.group.shared.MomentsQuickAddCta
import com.example.momentra.ui.shell.group.shared.MomentsSectionHeader
import com.example.momentra.ui.shell.group.shared.MomentsUpcomingEventCard
import com.example.momentra.ui.shell.group.shared.MomentsUpdateFeedRow
import com.example.momentra.ui.shell.group.shared.PlanningScheduleSheet
import com.example.momentra.ui.shell.group.shared.PollDetailSheet
import com.example.momentra.ui.shell.group.shared.buildMomentsUpcomingEvents
import com.example.momentra.ui.shell.group.shared.formatPlanningTime
import com.example.momentra.ui.shell.group.shared.itineraryDayGroups
import com.example.momentra.ui.shell.group.shared.loadGroupPulseTab
import com.example.momentra.ui.shell.group.shared.planningPlansPercent
import com.example.momentra.ui.shell.group.wedding.create.SheetAccent
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
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
    var contributions by remember { mutableStateOf<List<GroupContributionItemDto>>(emptyList()) }
    var memoryCount by remember { mutableIntStateOf(0) }
    var selectedPollId by remember { mutableStateOf<String?>(null) }
    var pollsListOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var scheduleOpen by remember { mutableStateOf(false) }
    var checklistSheetOpen by remember { mutableStateOf(false) }
    var contributionsOpen by remember { mutableStateOf(false) }
    var expensesOpen by remember { mutableStateOf(false) }
    var editingContribution by remember { mutableStateOf<GroupContributionItemDto?>(null) }

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
            contributions = emptyList()
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
        // Progressive: list facets in parallel after pulse paint.
        coroutineScope {
            val plansDeferred = async { repository.listPlanningItems(momentId).getOrNull()?.items }
            val booksDeferred = async { repository.listBookings(momentId).getOrNull()?.items }
            val updsDeferred = async { repository.listUpdates(momentId).getOrNull()?.items }
            val pollsDeferred = async { repository.listPolls(momentId).getOrNull()?.items }
            val memsDeferred = async { repository.listMemories(momentId) }
            val expensesDeferred = async {
                repository.listGroupExpenses(momentId, 50).getOrNull()?.items.orEmpty()
            }
            val contributionsDeferred = async {
                repository.listContributions(momentId, 50).getOrNull()?.items.orEmpty()
            }
            val plans = plansDeferred.await()
            val books = booksDeferred.await()
            val upds = updsDeferred.await()
            val pollList = pollsDeferred.await()
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
            memsDeferred.await().onSuccess {
                memoryItems = it.items
                memoryCount = it.memoryCount.takeIf { c -> c > 0 } ?: it.items.size
            }.onFailure {
                repository.getMemory(momentId).onSuccess {
                    memoryItems = it.payload?.items.orEmpty()
                    memoryCount = it.payload?.memoryCount?.takeIf { c -> c > 0 } ?: memoryItems.size
                }
            }
            expenses = expensesDeferred.await()
            contributions = contributionsDeferred.await()
        }
    }

    if (loading && pulse == null) {
        GroupActiveLoading(modifier.fillMaxSize())
        return
    }

    val allTotals = finance?.totals.orEmpty()
    val primaryTotal = GroupFinanceFormat.resolvePrimaryTotal(
        allTotals,
        preferredCurrency = finance?.viewerPosition?.currencyCode,
    )
    val currency = primaryTotal?.currencyCode ?: "INR"
    val budgetTotal = primaryTotal?.budgetTotal
    val spentLine = GroupFinanceFormat.expensePartitionLine(allTotals)
    val yourShareLine = GroupFinanceFormat.viewerAllocatedPartitionLine(
        finance?.viewerPosition,
        finance?.positions.orEmpty(),
    )
    val peopleCount = pulse?.participantCount ?: 0
    val plansPct = planningPlansPercent(planningItems)
    val momentsValue = if (memoryCount > 0) memoryCount else memoryItems.size
    val status = "PLANNING"
    val title = momentTitle ?: "Shared Moments"
    val dayGroups = itineraryDayGroups(planningItems)
    val upcoming = remember(bookings, planningItems, finance, peopleCount) {
        buildMomentsUpcomingEvents(bookings, planningItems, finance)
    }

    ActiveTabScrollScaffold(
        background = chrome.bg,
        modifier = modifier,
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

        if (polls.isNotEmpty()) {
            MomentsSectionHeader("Polls  🗳️", chrome, onViewAll = { pollsListOpen = true })
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

        MomentsChecklistSection(
            items = planningItems,
            momentId = momentId,
            chrome = chrome,
            repository = repository,
            onChanged = {
                if (!momentId.isNullOrBlank()) {
                    scope.launch {
                        planningItems = repository.listPlanningItems(momentId).getOrNull()?.items.orEmpty()
                    }
                }
            },
            onAdd = { checklistSheetOpen = true },
        )

        if (updates.isNotEmpty()) {
            MomentsSectionHeader("Updates / Feed  📱", chrome)
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

        if (bookings.isNotEmpty()) {
            MomentsSectionHeader("Bookings  🛎️", chrome)
            bookings.take(3).forEach { MomentsBookingCard(it, chrome) }
        }

        if (upcoming.isNotEmpty()) {
            MomentsSectionHeader("Upcoming Events  🗓", chrome)
            upcoming.forEachIndexed { index, event ->
                MomentsUpcomingEventCard(event = event, highlight = index == 0, chrome = chrome)
            }
        }

        MomentsContributionDetailsSection(
            items = contributions,
            chrome = chrome,
            momentId = momentId,
            onViewAll = { contributionsOpen = true },
            onEdit = { editingContribution = it },
        )

        MomentsSectionHeader("Expenses & Budget  💸", chrome, onViewAll = { expensesOpen = true })
        MomentsExpensesCard(
            totals = allTotals,
            yourAllocatedLine = yourShareLine,
            expenses = expenses,
            chrome = chrome,
            fallbackCurrency = currency,
        )

        MomentsQuickAddCta(chrome = chrome, onClick = onCreateMoment)
    }

    ExpensesListSheet(
        items = expenses,
        visible = expensesOpen,
        onDismiss = { expensesOpen = false },
        chrome = chrome,
        fallbackCurrency = currency,
    )

    PlanningScheduleSheet(
        items = GroupExperienceChecklistCatalog.nonChecklistItems(planningItems),
        visible = scheduleOpen,
        onDismiss = { scheduleOpen = false },
        momentId = momentId,
        momentTypeCode = momentTypeCode,
        onSaved = {
            if (!momentId.isNullOrBlank()) {
                scope.launch {
                    planningItems = repository.listPlanningItems(momentId).getOrNull()?.items.orEmpty()
                }
            }
        },
        accent = Color(0xFF14B8A6),
        surface = chrome.bg,
        field = chrome.card,
        border = chrome.border,
        text = chrome.text,
        muted = chrome.secondary,
        repository = repository,
    )

    ExperienceChecklistAddSheet(
        visible = checklistSheetOpen,
        momentId = momentId,
        onDismiss = { checklistSheetOpen = false },
        onSaved = {
            if (!momentId.isNullOrBlank()) {
                scope.launch {
                    planningItems = repository.listPlanningItems(momentId).getOrNull()?.items.orEmpty()
                }
            }
        },
        accent = SheetAccent(
            accent = Color(0xFF14B8A6),
            accentEnd = Color(0xFF0F766E),
            soft = Color(0xFF14B8A6).copy(alpha = 0.2f),
        ),
        surface = chrome.bg,
        repository = repository,
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

    ContributionsListSheet(
        items = contributions,
        visible = contributionsOpen,
        onDismiss = { contributionsOpen = false },
        chrome = MomentsChrome.Trip,
        momentId = momentId,
        onEdit = {
            contributionsOpen = false
            editingContribution = it
        },
    )

    val editItem = editingContribution
    if (editItem != null && !momentId.isNullOrBlank()) {
        GroupContributionSheet(
            momentId = momentId,
            visible = true,
            onDismiss = { editingContribution = null },
            onSaved = {
                editingContribution = null
                scope.launch {
                    contributions = repository.listContributions(momentId, 50).getOrNull()?.items.orEmpty()
                }
            },
            onDeleted = {
                editingContribution = null
                scope.launch {
                    contributions = repository.listContributions(momentId, 50).getOrNull()?.items.orEmpty()
                }
            },
            editingContribution = editItem,
            repository = repository,
        )
    }

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
