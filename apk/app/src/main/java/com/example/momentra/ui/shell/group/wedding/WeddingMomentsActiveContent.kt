package com.example.momentra.ui.shell.group.wedding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.GroupFinancePayloadDto
import com.example.momentra.data.api.GroupLifePayloadDto
import com.example.momentra.data.api.GroupPulsePayloadDto
import com.example.momentra.data.repository.GroupSliceRepository
import com.example.momentra.ui.shell.group.GroupActiveLoading
import com.example.momentra.ui.shell.group.GroupFinanceFormat
import com.example.momentra.ui.shell.group.GroupTabDataCache
import com.example.momentra.ui.shell.group.loadGroupPulseTab
import com.example.momentra.ui.theme.PlusJakartaSans

/** Figma 575:14768 — Wedding Moments. Live APIs only; no demo seeds. */
@Composable
fun WeddingMomentsActiveContent(
    momentId: String?,
    momentTitle: String?,
    refreshToken: Long,
    onOpenQuickAdd: () -> Unit = {},
    repository: GroupSliceRepository = remember { GroupSliceRepository() },
    modifier: Modifier = Modifier,
) {
    var loading by remember { mutableStateOf(true) }
    var pulse by remember { mutableStateOf<GroupPulsePayloadDto?>(null) }
    var finance by remember { mutableStateOf<GroupFinancePayloadDto?>(null) }
    var life by remember { mutableStateOf<GroupLifePayloadDto?>(null) }
    var title by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(refreshToken, momentId) {
        if (momentId.isNullOrBlank()) {
            loading = false
            pulse = null
            finance = null
            life = null
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
    }

    if (loading && pulse == null && finance == null) {
        GroupActiveLoading(modifier.fillMaxSize())
        return
    }

    val budgetTotal = finance?.totals?.firstOrNull()?.budgetTotal
    val currency = finance?.totals?.firstOrNull()?.currencyCode ?: "INR"
    val peopleCount = pulse?.participantCount ?: 0
    val openTasks = pulse?.openTaskCount ?: life?.openTaskCount ?: 0
    val displayTitle = momentTitle ?: title ?: "Shared Moments"
    val budgetLabel = if (budgetTotal != null) GroupFinanceFormat.compactMoney(budgetTotal, currency) else "—"
    val planningItems = life?.planningItems.orEmpty()
    val bookings = life?.bookings.orEmpty()
    val updates = life?.updates.orEmpty()

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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(WeddingActiveTheme.SectionRadius))
                    .background(WeddingActiveTheme.Card)
                    .border(1.dp, WeddingActiveTheme.Border, RoundedCornerShape(WeddingActiveTheme.SectionRadius))
                    .padding(WeddingActiveTheme.SectionPad),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("SHARED EXPERIENCE", color = WeddingActiveTheme.Secondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
                        Text(displayTitle, color = WeddingActiveTheme.Text, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans)
                    }
                    Text(
                        "PLANNING",
                        color = WeddingActiveTheme.DarkText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(WeddingActiveTheme.Accent)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    WeddingStatCard("GUESTS", "$peopleCount", WeddingActiveTheme.StatGradientA, Modifier.weight(1f), icon = "👥")
                    WeddingStatCard("BUDGET", budgetLabel, WeddingActiveTheme.StatGradientC, Modifier.weight(1f), icon = "💰")
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    WeddingStatCard("UPDATES", "${updates.size}", WeddingActiveTheme.StatGradientB, Modifier.weight(1f), icon = "✏️")
                    WeddingStatCard("TASKS", "$openTasks", WeddingActiveTheme.StatGradientD, Modifier.weight(1f), icon = "✅")
                }
            }

            WeddingSectionCard(title = "📅  Wedding Timeline") {
                if (planningItems.isEmpty()) {
                    WeddingEmptyBlock(
                        message = "No timeline items yet",
                        detail = "Add a planning item from Quick Add — nothing is invented.",
                    )
                } else {
                    planningItems.forEach { item ->
                        Text(
                            item.title ?: item.planningItemId.orEmpty(),
                            color = WeddingActiveTheme.Text,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = PlusJakartaSans,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(WeddingActiveTheme.Bg)
                                .border(1.dp, WeddingActiveTheme.Border, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                        )
                    }
                }
            }

            WeddingSectionCard(title = "🏨  Bookings") {
                if (bookings.isEmpty()) {
                    WeddingEmptyBlock(
                        message = "No bookings yet",
                        detail = "Add a booking from Quick Add when ready.",
                    )
                } else {
                    bookings.forEach { item ->
                        Text(
                            item.title ?: item.bookingId.orEmpty(),
                            color = WeddingActiveTheme.Text,
                            fontSize = 13.sp,
                            fontFamily = PlusJakartaSans,
                        )
                    }
                }
            }

            WeddingSectionCard(title = "✏️  Updates") {
                if (updates.isEmpty()) {
                    WeddingEmptyBlock(
                        message = "No updates yet",
                        detail = "Share a status update from Quick Add.",
                    )
                } else {
                    updates.take(8).forEach { item ->
                        Text(
                            item.message ?: item.updateId.orEmpty(),
                            color = WeddingActiveTheme.Text,
                            fontSize = 13.sp,
                            fontFamily = PlusJakartaSans,
                        )
                    }
                }
            }

            WeddingSectionCard(title = "📸  Shared Gallery") {
                WeddingEmptyBlock(
                    message = "Gallery empty",
                    detail = "Shared media will appear when group media API is live.",
                )
            }

            WeddingPinkCta(
                title = "Add to the wedding story",
                subtitle = "Add a plan, expense, memory, poll or update.",
                buttonLabel = "Open Quick Add",
                enabled = true,
                onClick = onOpenQuickAdd,
                outlinedButton = true,
            )
        }
    }
}
