package com.example.momentra.ui.shell.personal.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.ActivityItemDto
import com.example.momentra.data.api.PersonalPulseDto
import com.example.momentra.data.repository.PersonalSliceRepository
import com.example.momentra.ui.theme.PlusJakartaSans

private val MomentsBg = Color(0xFF14121B)
private val MomentsText = Color(0xFFE5E0EE)
private val MomentsMuted = Color(0xFFC9C4D8)

/** Shared Personal Moments body — browse what happened (no Pulse scores / Life analytics). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalMomentsActiveContent(
    refreshToken: Long,
    momentId: String?,
    momentTitle: String?,
    momentTypeCode: String?,
    onOpenQuickAdd: () -> Unit = {},
    onActivityChanged: () -> Unit = {},
    repository: PersonalSliceRepository = remember { PersonalSliceRepository() },
    modifier: Modifier = Modifier,
) {
    val family = personalPulseFamilyFor(momentTypeCode)
    val theme = family.theme()
    var loading by remember { mutableStateOf(true) }
    var pulse by remember { mutableStateOf<PersonalPulseDto?>(null) }
    var activities by remember { mutableStateOf<List<ActivityItemDto>>(emptyList()) }
    var mayHaveMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<ActivityItemDto?>(null) }
    val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(refreshToken, momentId) {
        error = null
        loading = activities.isEmpty()
        loadPersonalMomentsTab(repository, momentId).fold(
            onSuccess = { page ->
                pulse = page.pulse
                activities = page.activities
                mayHaveMore = page.mayHaveMore
                loading = false
            },
            onFailure = { e ->
                error = e.message
                loading = false
            },
        )
    }

    if (loading && activities.isEmpty() && pulse == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = theme.accent)
        }
        return
    }

    val cards = MomentClustering.cards(activities, family)
    val spend = (pulse?.widgetPayload?.get("spendByCurrency") as? Map<*, *>)
        ?.entries
        ?.mapNotNull { e ->
            val code = e.key?.toString() ?: return@mapNotNull null
            val amount = e.value?.toString() ?: return@mapNotNull null
            code to amount
        }
        .orEmpty()
    val moneyLine = PersonalMomentsMoneyLine.line(family, spend)
    val highlight = PersonalMomentsHighlight.select(family, activities)
    val sections = MomentStreamGrouping.sections(cards)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MomentsBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        error?.let {
            Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans)
        }
        if (!momentTitle.isNullOrBlank()) {
            Text(momentTitle, color = MomentsMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(listOf(theme.heroStart.copy(alpha = 0.35f), theme.heroEnd.copy(alpha = 0.2f))))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(PersonalMomentsHeaderCopy.periodLabel(), color = MomentsText, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans)
            Text(PersonalMomentsHeaderCopy.subtitle(cards.size), color = MomentsMuted, fontSize = 14.sp, fontFamily = PlusJakartaSans)
            Text(family.switcherLabel(), color = theme.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
        }

        moneyLine?.let { line ->
            Text(
                line,
                color = MomentsMuted,
                fontSize = 13.sp,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
            )
        }

        highlight?.let { item ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(theme.accent.copy(alpha = 0.1f))
                    .border(1.dp, theme.accent.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(PersonalMomentsHighlight.sectionTitle(family), color = theme.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                Text(item.title, color = MomentsText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
                Text(item.detail, color = MomentsMuted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (sections.isEmpty()) {
                Text("No moments yet. Capture one from Add.", color = MomentsMuted, fontSize = 13.sp, fontFamily = PlusJakartaSans)
            } else {
                sections.forEach { section ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(section.header, color = MomentsMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                        section.cards.forEach { card ->
                            MomentCard(
                                model = card,
                                accent = theme.accent,
                                onTap = {
                                    if (card.tapDestination == MomentCardTapDestination.ACTIVITY_DETAIL) {
                                        editing = resolveActivity(activities, card)
                                    }
                                },
                            )
                            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                        }
                    }
                }
            }
        }

        if (mayHaveMore) {
            Text("Showing recent moments — more history may exist.", color = MomentsMuted, fontSize = 11.sp, fontFamily = PlusJakartaSans)
        }

        Text(
            "Capture another moment",
            color = theme.accent,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(theme.accent.copy(alpha = 0.12f))
                .clickable(onClick = onOpenQuickAdd)
                .padding(vertical = 12.dp),
        )
    }

    val editItem = editing
    val mid = momentId
    if (editItem != null && !mid.isNullOrBlank()) {
        ModalBottomSheet(
            onDismissRequest = { editing = null },
            sheetState = editSheetState,
            containerColor = Color(0xFF191622),
            dragHandle = null,
        ) {
            if (PersonalActivityTimelineDerived.isExpense(editItem)) {
                PersonalEditTransactionSheet(
                    item = editItem,
                    momentId = mid,
                    onClose = { editing = null },
                    onSaved = {
                        editing = null
                        onActivityChanged()
                    },
                    onDeleted = {
                        editing = null
                        onActivityChanged()
                    },
                )
            } else {
                PersonalEditActivitySheet(
                    item = editItem,
                    momentId = mid,
                    onClose = { editing = null },
                    onSaved = {
                        editing = null
                        onActivityChanged()
                    },
                    onDeleted = {
                        editing = null
                        onActivityChanged()
                    },
                )
            }
        }
    }
}

private fun resolveActivity(activities: List<ActivityItemDto>, card: MomentCardModel): ActivityItemDto? {
    for (sid in card.sourceActivityIds) {
        activities.firstOrNull { activity ->
            val id = activity.activityPayload?.activityId
                ?: activity.activityPayload?.expenseId
                ?: activity.activityPayload?.incomeId
                ?: activity.activityPayload?.contributionId
                ?: "${activity.occurredAt}|${activity.title}|${activity.activityCode}"
            id == sid
        }?.let { return it }
    }
    return activities.firstOrNull {
        it.occurredAt == card.occurredAt && it.activityCode == card.activityCode
    }
}
