package com.example.momentra.ui.shell.group.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.GroupFinancePayloadDto
import com.example.momentra.data.api.GroupLifeBookingDto
import com.example.momentra.data.api.GroupLifePlanningItemDto
import com.example.momentra.data.api.GroupLifeUpdateDto
import com.example.momentra.data.api.GroupPollItemDto
import com.example.momentra.data.api.GroupPulsePayloadDto
import com.example.momentra.data.repository.GroupSliceRepository
import com.example.momentra.ui.theme.PlusJakartaSans
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Figma 575:14327 — Group Moments active tab (live API only). */
@Composable
fun GroupMomentsActiveContent(
    momentId: String?,
    momentTitle: String?,
    refreshToken: Long,
    onCreateMoment: () -> Unit = {},
    repository: GroupSliceRepository = remember { GroupSliceRepository() },
    modifier: Modifier = Modifier,
) {
    var loading by remember { mutableStateOf(true) }
    var pulse by remember { mutableStateOf<GroupPulsePayloadDto?>(null) }
    var finance by remember { mutableStateOf<GroupFinancePayloadDto?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var planningItems by remember { mutableStateOf<List<GroupLifePlanningItemDto>>(emptyList()) }
    var bookings by remember { mutableStateOf<List<GroupLifeBookingDto>>(emptyList()) }
    var updates by remember { mutableStateOf<List<GroupLifeUpdateDto>>(emptyList()) }
    var polls by remember { mutableStateOf<List<GroupPollItemDto>>(emptyList()) }
    var selectedPollId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(refreshToken, momentId) {
        if (momentId.isNullOrBlank()) {
            loading = false
            pulse = null
            finance = null
            planningItems = emptyList()
            bookings = emptyList()
            updates = emptyList()
            polls = emptyList()
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
        // Prefer dedicated list GETs (parity with iOS); fall back to Life facet embedding.
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
    }

    if (loading && pulse == null) {
        GroupActiveLoading(modifier.fillMaxSize())
        return
    }

    val budgetTotal = finance?.totals?.firstOrNull()?.budgetTotal
    val currency = finance?.totals?.firstOrNull()?.currencyCode ?: "INR"
    val peopleCount = pulse?.participantCount ?: 0
    val plansCount = pulse?.openTaskCount ?: planningItems.size
    val itineraryAccents = listOf(
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

        GroupHeroHeader(
            title = momentTitle ?: "Shared Moments",
            subtitle = "Plan, share, and relive together",
            meta = "PLANNING",
        )

        GroupSectionCard(title = "Shared Experience") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MomentsMetricCard(
                    label = "PEOPLE",
                    value = "$peopleCount",
                    glyph = "👥",
                    brush = Brush.linearGradient(listOf(Color(0xFF0F766E), Color(0xFF14B8A6))),
                    modifier = Modifier.weight(1f),
                )
                MomentsMetricCard(
                    label = "PLANS",
                    value = "$plansCount",
                    glyph = "🗺️",
                    brush = Brush.linearGradient(listOf(Color(0xFFE89574), GroupActiveTheme.Brand)),
                    modifier = Modifier.weight(1f),
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MomentsMetricCard(
                    label = "BUDGET",
                    value = GroupFinanceFormat.compactMoney(budgetTotal, currency),
                    glyph = "💰",
                    brush = Brush.linearGradient(listOf(Color(0xFF9A3412), GroupActiveTheme.AccentOrange)),
                    modifier = Modifier.weight(1f),
                )
                MomentsMetricCard(
                    label = "UPDATES",
                    value = "${updates.size}",
                    glyph = "✨",
                    brush = Brush.linearGradient(listOf(Color(0xFF6B21A8), Color(0xFFA855F7))),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        GroupSectionCard(title = "Itinerary") {
            if (planningItems.isEmpty()) {
                GroupEmptySection(
                    message = "No itinerary days yet",
                    detail = "Add a planning item from Quick Add — nothing is invented.",
                )
            } else {
                planningItems.forEachIndexed { index, item ->
                    MomentsListRow(
                        title = item.title ?: item.planningItemId.orEmpty(),
                        meta = formatTripInstant(item.dueAt) ?: item.status,
                        accent = itineraryAccents[index % itineraryAccents.size],
                        glyph = "📍",
                    )
                }
            }
        }

        GroupSectionCard(title = "Bookings") {
            if (bookings.isEmpty()) {
                GroupEmptySection(
                    message = "No bookings yet",
                    detail = "Add a booking from Quick Add when ready.",
                )
            } else {
                bookings.forEachIndexed { index, item ->
                    MomentsListRow(
                        title = item.title ?: item.bookingId.orEmpty(),
                        meta = item.status,
                        accent = itineraryAccents[(index + 1) % itineraryAccents.size],
                        glyph = "🏨",
                    )
                }
            }
        }

        GroupSectionCard(title = "Polls") {
            if (polls.isEmpty()) {
                GroupEmptySection(
                    message = "No polls yet",
                    detail = "Create a poll from Quick Add to decide together.",
                )
            } else {
                polls.forEachIndexed { index, item ->
                    MomentsListRow(
                        title = item.question ?: item.pollId.orEmpty(),
                        meta = item.status,
                        accent = itineraryAccents[(index + 3) % itineraryAccents.size],
                        glyph = "📊",
                        onClick = item.pollId?.let { pollId -> ({ selectedPollId = pollId }) },
                    )
                }
            }
        }

        GroupSectionCard(title = "Updates") {
            if (updates.isEmpty()) {
                GroupEmptySection(
                    message = "No updates yet",
                    detail = "Share a status update from Quick Add.",
                )
            } else {
                updates.take(8).forEachIndexed { index, item ->
                    MomentsListRow(
                        title = item.message ?: item.updateId.orEmpty(),
                        meta = formatTripInstant(item.createdAt),
                        accent = itineraryAccents[(index + 2) % itineraryAccents.size],
                        glyph = "✏️",
                    )
                }
            }
        }

        GroupSectionCard(
            title = "Shared Gallery",
            badge = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GroupComingSoonBadge()
                    GroupApiGapBadge()
                }
            },
        ) {
            GroupEmptySection(
                message = "Gallery empty",
                detail = "Shared media will appear here when group media API is live.",
            )
        }

        MomentsQuickAddCta(onClick = onCreateMoment)
    }

    selectedPollId?.let { pollId ->
        PollDetailSheet(
            pollId = pollId,
            visible = true,
            onDismiss = { selectedPollId = null },
            onSaved = { /* parent refreshToken handles reload */ },
            repository = repository,
        )
    }
}

@Composable
private fun MomentsMetricCard(
    label: String,
    value: String,
    glyph: String,
    brush: Brush,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(brush)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(glyph, fontSize = 18.sp)
        Text(label, color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
        Text(value, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans)
    }
}

@Composable
private fun MomentsListRow(
    title: String,
    meta: String?,
    accent: Color,
    glyph: String,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF181716))
            .border(1.dp, GroupActiveTheme.Border, RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(36.dp)
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
            Text(title, color = GroupActiveTheme.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
            meta?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = GroupActiveTheme.Secondary, fontSize = 11.sp, fontFamily = PlusJakartaSans)
            }
        }
    }
}

@Composable
private fun MomentsQuickAddCta(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(listOf(GroupActiveTheme.Brand, GroupActiveTheme.AccentOrange)))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "Create the next shared moment",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = PlusJakartaSans,
        )
        Text(
            "Plan, booking, poll, expense, memory or update.",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 12.sp,
            fontFamily = PlusJakartaSans,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "+ Open Quick Add",
                color = GroupActiveTheme.AccentOrange,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
        }
    }
}

private fun formatTripInstant(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    return runCatching {
        OffsetDateTime.parse(raw).format(DateTimeFormatter.ofPattern("MMM d · h:mm a", Locale.US))
    }.getOrElse {
        raw.take(16).replace('T', ' ')
    }
}
