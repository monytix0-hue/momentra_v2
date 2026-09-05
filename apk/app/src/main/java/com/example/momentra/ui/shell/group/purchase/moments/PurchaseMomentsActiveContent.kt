package com.example.momentra.ui.shell.group.purchase.moments

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.GroupExpenseListItemDto
import com.example.momentra.data.api.GroupFinancePayloadDto
import com.example.momentra.data.api.GroupFinancePositionDto
import com.example.momentra.data.api.GroupLifeBookingDto
import com.example.momentra.data.api.GroupLifePlanningItemDto
import com.example.momentra.data.api.GroupLifeUpdateDto
import com.example.momentra.data.api.GroupMemoryItemDto
import com.example.momentra.data.api.GroupOwnershipItemDto
import com.example.momentra.data.api.GroupParticipantDto
import com.example.momentra.data.api.GroupPollItemDto
import com.example.momentra.data.api.GroupPulsePayloadDto
import com.example.momentra.data.api.GroupPurchaseItemDto
import com.example.momentra.data.api.GroupVendorItemDto
import com.example.momentra.data.repository.GroupSliceRepository
import kotlinx.coroutines.launch
import com.example.momentra.ui.shell.group.purchase.create.PurchaseActiveTheme
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

/** Figma 601:* / 605:* / 617:* — Purchase Moments. Live APIs only; no invented pinned notes. */
@Composable
fun PurchaseMomentsActiveContent(
    theme: PurchaseActiveTheme,
    momentId: String?,
    momentTitle: String?,
    refreshToken: Long,
    momentTypeCode: String? = null,
    onOpenQuickAdd: () -> Unit = {},
    repository: GroupSliceRepository = remember { GroupSliceRepository() },
    modifier: Modifier = Modifier,
) {
    val chrome = MomentsChrome.purchase(theme)
    val label = theme.typeLabel
    val isGiftPool = label == PurchaseActiveTheme.GiftPool.typeLabel
    val isGroupPurchase = label == PurchaseActiveTheme.GroupPurchase.typeLabel
    val isSharedAsset = label == PurchaseActiveTheme.SharedAsset.typeLabel
    val isCustom = label == PurchaseActiveTheme.CustomPurchase.typeLabel

    val showContributions = isGiftPool || isGroupPurchase || isSharedAsset || isCustom
    val showOwnershipFull = isSharedAsset || isCustom
    val showOwnershipLight = isGroupPurchase
    val showVendors = isGroupPurchase || isCustom
    val showUpcoming = isGiftPool || isGroupPurchase
    val showGallery = !isCustom
    val galleryOnlyIfMedia = isSharedAsset

    var loading by remember { mutableStateOf(true) }
    var pulse by remember { mutableStateOf<GroupPulsePayloadDto?>(null) }
    var finance by remember { mutableStateOf<GroupFinancePayloadDto?>(null) }
    var facetStatus by remember { mutableStateOf<String?>(null) }
    var planningItems by remember { mutableStateOf<List<GroupLifePlanningItemDto>>(emptyList()) }
    var bookings by remember { mutableStateOf<List<GroupLifeBookingDto>>(emptyList()) }
    var updates by remember { mutableStateOf<List<GroupLifeUpdateDto>>(emptyList()) }
    var polls by remember { mutableStateOf<List<GroupPollItemDto>>(emptyList()) }
    var memoryItems by remember { mutableStateOf<List<GroupMemoryItemDto>>(emptyList()) }
    var purchaseItems by remember { mutableStateOf<List<GroupPurchaseItemDto>>(emptyList()) }
    var ownership by remember { mutableStateOf<List<GroupOwnershipItemDto>>(emptyList()) }
    var vendors by remember { mutableStateOf<List<GroupVendorItemDto>>(emptyList()) }
    var expenses by remember { mutableStateOf<List<GroupExpenseListItemDto>>(emptyList()) }
    var participants by remember { mutableStateOf<List<GroupParticipantDto>>(emptyList()) }
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
        purchaseItems = repository.listPurchaseItems(momentId).getOrNull()?.items.orEmpty()
        if (showOwnershipFull || showOwnershipLight) {
            ownership = repository.listOwnershipRecords(momentId).getOrNull()?.items.orEmpty()
        }
        if (showVendors) {
            vendors = repository.listGroupVendors(momentId).getOrNull()?.items.orEmpty()
        }
        expenses = repository.listGroupExpenses(momentId, 10).getOrNull()?.items.orEmpty()
        participants = repository.getParticipants(momentId).getOrNull()?.participants.orEmpty()
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
    val peopleCount = pulse?.participantCount ?: 0
    val moments = if (memoryCount > 0) memoryCount else memoryItems.size
    val funded = PurchaseMomentsMath.fundedPercent(contributionTotal, budgetTotal)
    val displayTitle = momentTitle ?: title ?: "${theme.typeLabel} Moments"
    val status = when {
        isSharedAsset -> "ACTIVE"
        isCustom -> "CURRENT"
        else -> (facetStatus ?: "PLANNING").uppercase(Locale.US)
    }
    val dayGroups = itineraryDayGroups(planningItems)
    val upcoming = remember(bookings, planningItems, finance) {
        buildMomentsUpcomingEvents(bookings, planningItems, finance)
    }
    val positions = finance?.positions.orEmpty()
    val nameById = participants.associate { it.participantId to (it.displayName ?: it.participantId.take(8)) }
    val g0 = listOf(theme.accentSolid, theme.accent)
    val g1 = listOf(theme.accent, theme.accentLight)
    val g2 = listOf(theme.accentLight, theme.accentSolid)
    val g3 = listOf(theme.accent, theme.accentSolid)
    val heroStats = when {
        isGiftPool -> listOf(
            Triple("PEOPLE", "$peopleCount", g0),
            Triple("FUNDED", funded?.let { "$it%" } ?: "—", g1),
            Triple("BUDGET", GroupFinanceFormat.compactMoney(budgetTotal, currency), g2),
            Triple("MOMENTS", "$moments", g3),
        )
        isGroupPurchase -> listOf(
            Triple("PEOPLE", "$peopleCount", g0),
            Triple("BUDGET", GroupFinanceFormat.compactMoney(budgetTotal, currency), g1),
            Triple("FUNDED", funded?.let { "$it%" } ?: "—", g2),
            Triple("ITEMS", if (purchaseItems.isNotEmpty()) "${purchaseItems.size}" else "—", g3),
        )
        isSharedAsset -> listOf(
            Triple("PEOPLE", "$peopleCount", g0),
            Triple("BUDGET", GroupFinanceFormat.compactMoney(budgetTotal, currency), g1),
            Triple("FUNDED", funded?.let { "$it%" } ?: "—", g2),
            Triple("OWNERS", if (ownership.isNotEmpty()) "${ownership.size}" else "—", g3),
        )
        else -> listOf(
            Triple("PEOPLE", "$peopleCount", g0),
            Triple("BUDGET", GroupFinanceFormat.compactMoney(budgetTotal, currency), g1),
            Triple("ITEMS", if (purchaseItems.isNotEmpty()) "${purchaseItems.size}" else "—", g2),
            Triple("MOMENTS", "$moments", g3),
        )
    }
    val galleryItems = if (galleryOnlyIfMedia) {
        memoryItems.filter { it.media.isNotEmpty() || it.mediaCount > 0 }
    } else {
        memoryItems
    }
    val showGallerySection = when {
        isCustom -> false
        isSharedAsset -> galleryItems.isNotEmpty()
        else -> showGallery
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
            eyebrow = "SHARED PURCHASE",
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

        MomentsSectionHeader(
            if (isSharedAsset) "Timeline" else "Itinerary",
            chrome,
            onViewAll = { scheduleOpen = true },
        )
        if (dayGroups.isEmpty()) {
            GroupEmptySection(
                if (isSharedAsset) "No timeline yet" else "No itinerary days yet",
                "Add a planning item from Quick Add — nothing is invented.",
            )
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

        if (showGallerySection) {
            MomentsSectionHeader("Shared Gallery  📸", chrome)
            MemoryPhotoGalleryStrip(
                items = if (galleryOnlyIfMedia) galleryItems else memoryItems,
                emptyMessage = "Gallery empty",
                emptyDetail = "Add a memory with a photo from Quick Add.",
                text = chrome.text,
                muted = chrome.secondary,
                field = chrome.card,
                border = chrome.border,
                showMediaCountBadge = true,
            )
        }

        if (showContributions) {
            MomentsSectionHeader(
                if (isCustom) "Contributions  💰" else theme.contributionsTitle,
                chrome,
            )
            PurchaseContributionsCard(
                contributionTotal = contributionTotal,
                budgetTotal = budgetTotal,
                currency = currency,
                funded = funded,
                positions = positions,
                nameById = nameById,
                chrome = chrome,
                light = isCustom,
            )
        }

        MomentsSectionHeader("Purchases  🛒", chrome)
        if (purchaseItems.isEmpty()) {
            GroupEmptySection("No purchase items yet", "Add an item from Quick Add — nothing is invented.")
        } else {
            purchaseItems.take(8).forEach { item ->
                MomentsSimpleRowCard(
                    title = item.label ?: item.purchaseItemId ?: "Item",
                    chrome = chrome,
                    meta = item.amount?.let { GroupFinanceFormat.formatMoney(it, currency) },
                    status = item.status,
                )
            }
        }

        if (showOwnershipFull || (showOwnershipLight && ownership.isNotEmpty())) {
            MomentsSectionHeader("Ownership  🔑", chrome)
            if (ownership.isEmpty()) {
                GroupEmptySection("No ownership records yet", "Add ownership from Quick Add when ready.")
            } else {
                ownership.take(if (showOwnershipLight) 3 else 8).forEach { row ->
                    MomentsSimpleRowCard(
                        title = row.displayName ?: "Owner",
                        chrome = chrome,
                        meta = listOfNotNull(row.ownershipShare, row.ownershipNote).joinToString(" · ").ifBlank { null },
                        status = row.status,
                    )
                }
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

        MomentsSectionHeader("Expenses & Budget  💸", chrome)
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
            subtitle = "Add an item, contribution, memory, poll or update.",
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
        chrome = MomentsChrome.purchase(theme),
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

@Composable
private fun PurchaseContributionsCard(
    contributionTotal: String?,
    budgetTotal: String?,
    currency: String,
    funded: Int?,
    positions: List<GroupFinancePositionDto>,
    nameById: Map<String, String>,
    chrome: MomentsChrome,
    light: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(chrome.card)
            .border(1.dp, chrome.border, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "Collected ${GroupFinanceFormat.compactMoney(contributionTotal, currency)}",
                color = chrome.text,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
            Text(
                funded?.let { "$it% funded" } ?: "—",
                color = chrome.accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
        }
        val fraction = ((funded ?: 0).coerceIn(0, 100) / 100f).coerceIn(0f, 1f)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFF252332)),
        ) {
            if (fraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(chrome.accent),
                )
            }
        }
        Text(
            "of ${GroupFinanceFormat.compactMoney(budgetTotal, currency)} budget",
            color = chrome.secondary,
            fontSize = 11.sp,
            fontFamily = PlusJakartaSans,
        )
        if (positions.isEmpty()) {
            GroupEmptySection("No contributions yet", "Record a contribution from Quick Add — nothing is invented.")
        } else {
            positions.take(if (light) 3 else 6).forEach { pos ->
                val amount = pos.contributionTotal.takeIf { it.isNotBlank() && it != "0" }
                    ?: pos.paidTotal.takeIf { it.isNotBlank() && it != "0" }
                MomentsSimpleRowCard(
                    title = nameById[pos.participantId] ?: pos.participantId.take(8),
                    chrome = chrome,
                    meta = amount?.let { GroupFinanceFormat.formatMoney(it, pos.currencyCode.ifBlank { currency }) },
                )
            }
        }
    }
}

private object PurchaseMomentsMath {
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
