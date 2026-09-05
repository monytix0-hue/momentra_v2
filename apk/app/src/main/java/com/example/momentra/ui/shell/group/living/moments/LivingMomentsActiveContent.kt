package com.example.momentra.ui.shell.group.living.moments

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
import com.example.momentra.data.api.GroupExpenseListItemDto
import com.example.momentra.data.api.GroupFinancePayloadDto
import com.example.momentra.data.api.GroupLifeBookingDto
import com.example.momentra.data.api.GroupLifePlanningItemDto
import com.example.momentra.data.api.GroupLifeUpdateDto
import com.example.momentra.data.api.GroupLivingRuleItemDto
import com.example.momentra.data.api.GroupMaintenanceRecordItemDto
import com.example.momentra.data.api.GroupMemoryItemDto
import com.example.momentra.data.api.GroupPollItemDto
import com.example.momentra.data.api.GroupPulsePayloadDto
import com.example.momentra.data.api.GroupResidentItemDto
import com.example.momentra.data.api.GroupSharedAssetItemDto
import com.example.momentra.data.repository.GroupSliceRepository
import kotlinx.coroutines.launch
import com.example.momentra.ui.shell.group.living.create.LivingActiveTheme
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
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

/** Shared Living Moments (G09–G12). Live APIs only; never invent highlights. */
@Composable
fun LivingMomentsActiveContent(
    theme: LivingActiveTheme,
    momentId: String?,
    momentTitle: String?,
    refreshToken: Long,
    momentTypeCode: String? = null,
    onOpenQuickAdd: () -> Unit = {},
    repository: GroupSliceRepository = remember { GroupSliceRepository() },
    modifier: Modifier = Modifier,
) {
    val chrome = MomentsChrome.living(theme)
    val label = theme.typeLabel
    val isFlatmates = label == LivingActiveTheme.Flatmates.typeLabel
    val isCoLiving = label == LivingActiveTheme.CoLiving.typeLabel
    val isFamily = label == LivingActiveTheme.FamilyHousehold.typeLabel
    val isCustom = label == LivingActiveTheme.CustomLiving.typeLabel
    val showUpcoming = isFlatmates || isFamily

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
    var residents by remember { mutableStateOf<List<GroupResidentItemDto>>(emptyList()) }
    var rules by remember { mutableStateOf<List<GroupLivingRuleItemDto>>(emptyList()) }
    var assets by remember { mutableStateOf<List<GroupSharedAssetItemDto>>(emptyList()) }
    var maintenance by remember { mutableStateOf<List<GroupMaintenanceRecordItemDto>>(emptyList()) }
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
        residents = repository.listResidents(momentId).getOrNull()?.items.orEmpty()
        rules = repository.listLivingRules(momentId).getOrNull()?.items.orEmpty()
        assets = repository.listSharedAssets(momentId).getOrNull()?.items.orEmpty()
        maintenance = repository.listMaintenanceRecords(momentId).getOrNull()?.items.orEmpty()
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
    val contributionTotal = finance?.totals?.firstOrNull()?.contributionTotal
    val expenseTotal = finance?.totals?.firstOrNull()?.expenseTotal
    val currency = finance?.totals?.firstOrNull()?.currencyCode ?: "INR"
    val peopleCount = residents.size.takeIf { it > 0 } ?: pulse?.participantCount ?: 0
    val openTasks = pulse?.openTaskCount ?: lifeOpenTasks
    val moments = if (memoryCount > 0) memoryCount else memoryItems.size
    val eventsCount = bookings.size + planningItems.count { !it.dueAt.isNullOrBlank() }
    val funded = LivingMomentsMath.fundedPercent(contributionTotal, budgetTotal)
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
    val heroStats = when {
        isFlatmates -> listOf(
            Triple("RESIDENTS", "$peopleCount", g0),
            Triple("COLLECTED", funded?.let { "$it%" } ?: "—", g1),
            Triple("RENT", GroupFinanceFormat.compactMoney(budgetTotal, currency), g2),
            Triple("MOMENTS", "$moments", g3),
        )
        isCoLiving -> listOf(
            Triple("RESIDENTS", "$peopleCount", g0),
            Triple("COLLECTED", funded?.let { "$it%" } ?: "—", g1),
            Triple("BUDGET", GroupFinanceFormat.compactMoney(budgetTotal, currency), g2),
            Triple("TASKS", "$openTasks", g3),
        )
        isFamily -> listOf(
            Triple("PEOPLE", "$peopleCount", g0),
            Triple("EVENTS", "$eventsCount", g1),
            Triple("BUDGET", GroupFinanceFormat.compactMoney(budgetTotal, currency), g2),
            Triple("MOMENTS", "$moments", g3),
        )
        else -> listOf(
            Triple("PEOPLE", "$peopleCount", g0),
            Triple("ASSETS", "${assets.size}", g1),
            Triple("BUDGET", GroupFinanceFormat.compactMoney(budgetTotal, currency), g2),
            Triple("MOMENTS", "$moments", g3),
        )
    }
    val highlights = memoryItems.filter { !it.title.isNullOrBlank() }.take(3)

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
            eyebrow = "SHARED LIVING",
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

        MomentsSectionHeader("Tasks / Planning", chrome, onViewAll = { scheduleOpen = true })
        if (dayGroups.isEmpty()) {
            GroupEmptySection("No tasks yet", "Add a task from Quick Add — nothing is invented.")
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

        MomentsSectionHeader("House Rules  📋", chrome)
        if (rules.isEmpty()) {
            GroupEmptySection("No house rules yet", "Add a rule from Quick Add when ready.")
        } else {
            rules.take(6).forEach { rule ->
                MomentsSimpleRowCard(
                    title = rule.title ?: "Rule",
                    chrome = chrome,
                    meta = rule.ruleText,
                    status = rule.status,
                )
            }
        }

        MomentsSectionHeader("Shared Assets  🔑", chrome)
        if (assets.isEmpty()) {
            GroupEmptySection("No shared assets yet", "Add a household asset from Quick Add.")
        } else {
            assets.take(6).forEach { asset ->
                MomentsSimpleRowCard(
                    title = asset.title ?: asset.sharedAssetId ?: "Asset",
                    chrome = chrome,
                    meta = asset.assetType,
                    status = asset.status,
                )
            }
        }

        MomentsSectionHeader("Maintenance  🔧", chrome)
        if (maintenance.isEmpty()) {
            GroupEmptySection("No maintenance records", "Log maintenance from Quick Add when something needs care.")
        } else {
            maintenance.take(6).forEach { record ->
                MomentsSimpleRowCard(
                    title = record.title ?: "Maintenance",
                    chrome = chrome,
                    meta = record.description,
                    status = record.status,
                )
            }
        }

        MomentsSectionHeader("Residents  🏠", chrome)
        if (residents.isEmpty()) {
            GroupEmptySection("No residents yet", "Add a resident from Quick Add — nothing is invented.")
        } else {
            residents.take(8).forEach { resident ->
                MomentsSimpleRowCard(
                    title = resident.name ?: resident.residentId ?: "Resident",
                    chrome = chrome,
                    meta = resident.roleCode,
                    status = resident.status,
                )
            }
        }

        if (highlights.isNotEmpty()) {
            MomentsSectionHeader("Highlights  ✨", chrome)
            highlights.forEach { memory ->
                MomentsSimpleRowCard(
                    title = memory.title ?: "Memory",
                    chrome = chrome,
                    meta = memory.occurredAt,
                    status = if (memory.mediaCount > 0) "${memory.mediaCount} media" else memory.status,
                )
            }
        }

        MomentsSectionHeader(
            if (isCustom || !theme.includesContribution) "Expenses & Budget  💸" else "Contributions & Expenses  💸",
            chrome,
        )
        MomentsExpensesCard(
            spent = expenseTotal,
            currency = currency,
            peopleCount = peopleCount,
            expenses = expenses,
            chrome = chrome,
        )

        if (showUpcoming) {
            MomentsSectionHeader("Upcoming Events  🗓", chrome)
            if (upcoming.isEmpty()) {
                GroupEmptySection("Nothing upcoming", "Near-term bookings and plans will show here.")
            } else {
                upcoming.forEachIndexed { index, event ->
                    MomentsUpcomingEventCard(event = event, highlight = index == 0, chrome = chrome)
                }
            }
        }

        MomentsQuickAddCta(
            chrome = chrome,
            onClick = onOpenQuickAdd,
            title = "Add to the ${theme.typeLabel.lowercase(Locale.US)} story",
            subtitle = "Add a resident, expense, task, asset or memory.",
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
        chrome = MomentsChrome.living(theme),
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

private object LivingMomentsMath {
    fun fundedPercent(contributionTotal: String?, budgetTotal: String?): Int? {
        val budget = parse(budgetTotal) ?: return null
        if (budget.compareTo(BigDecimal.ZERO) <= 0) return null
        val contrib = parse(contributionTotal) ?: BigDecimal.ZERO
        return contrib.multiply(BigDecimal(100))
            .divide(budget, 0, RoundingMode.HALF_UP)
            .toInt()
            .coerceIn(0, 100)
    }

    private fun parse(raw: String?): BigDecimal? {
        if (raw.isNullOrBlank()) return null
        return runCatching { BigDecimal(raw.trim()) }.getOrNull()
    }
}
