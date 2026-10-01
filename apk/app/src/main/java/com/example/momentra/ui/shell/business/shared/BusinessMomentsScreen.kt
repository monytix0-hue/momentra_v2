package com.example.momentra.ui.shell.business.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.momentra.data.repository.BusinessSliceRepository
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

private val ErrorRed = Color(0xFFF87171)
private const val PAGE_LIMIT = 20

/**
 * One Moments body for Money, Daily Business, and Team.
 * The timeline is the company-authorized activity feed for the selected moment.
 */
@Composable
fun BusinessMomentsScreen(
    family: BusinessMomentFamilyConfig.Family,
    momentId: String?,
    momentTitle: String?,
    refreshToken: Long,
    onOpenQuickAdd: () -> Unit = {},
    repository: BusinessSliceRepository = remember { BusinessSliceRepository() },
    modifier: Modifier = Modifier,
) {
    val typeCode = when (family) {
        BusinessMomentFamilyConfig.Family.MONEY -> "BUSINESS_RUNWAY"
        BusinessMomentFamilyConfig.Family.DAILY -> "BUSINESS_OPERATIONS"
        BusinessMomentFamilyConfig.Family.TEAM -> "TEAM_OPERATIONS"
    }
    val theme = BusinessActiveTheme.forTypeCode(typeCode)
    var feed by remember { mutableStateOf(BusinessMomentsFeed()) }
    var filter by remember(momentId) { mutableStateOf(BusinessMomentFilter.ALL) }
    var retry by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val zone = remember { ZoneId.systemDefault() }

    LaunchedEffect(momentId, refreshToken, retry) {
        val started = feed.begin(momentId)
        feed = started
        if (momentId.isNullOrBlank()) return@LaunchedEffect
        repository.getActivity(momentId, limit = PAGE_LIMIT).fold(
            onSuccess = {
                feed = feed.applyPage(momentId, started.generation, it.items, it.nextCursor, append = false)
            },
            onFailure = {
                feed = feed.applyFailure(momentId, started.generation, it.message)
            },
        )
    }

    val presentation = buildBusinessMomentsPresentation(
        family = family,
        items = feed.items,
        filter = filter,
        loading = feed.loading,
        error = feed.error,
        now = Instant.now(),
        zone = zone,
    )

    if (presentation.content == BusinessMomentsContent.LOADING) {
        Box(modifier.fillMaxSize().background(theme.bg), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = theme.accent)
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(theme.bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .padding(bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!momentTitle.isNullOrBlank()) {
            Text(
                momentTitle,
                color = theme.secondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
        }
        Text(
            presentation.familyTitle,
            color = theme.text,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = PlusJakartaSans,
        )
        Text(
            presentation.contextLine,
            color = theme.secondary,
            fontSize = 12.sp,
            fontFamily = PlusJakartaSans,
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            presentation.chips.forEach { chip ->
                val selected = chip.filter == presentation.selected
                Text(
                    chip.label,
                    color = if (selected) Color.White else theme.text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (selected) theme.accent else theme.card)
                        .border(1.dp, if (selected) theme.accent else theme.border, RoundedCornerShape(20.dp))
                        .clickable { filter = chip.filter }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
        presentation.errorMessage?.let { message ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(message, color = ErrorRed, fontSize = 12.sp, fontFamily = PlusJakartaSans)
                Text(
                    "Try again",
                    color = theme.accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier.clickable { retry += 1 },
                )
            }
        }
        when (presentation.content) {
            BusinessMomentsContent.ERROR -> Unit
            BusinessMomentsContent.FEED_EMPTY -> EmptyBlock(
                title = presentation.emptyTitle.orEmpty(),
                body = presentation.emptyBody,
                action = BusinessMomentsPresentation.ADD_LABEL,
                onAction = onOpenQuickAdd,
                theme = theme,
            )
            BusinessMomentsContent.FILTER_EMPTY -> EmptyBlock(
                title = presentation.emptyTitle.orEmpty(),
                body = null,
                action = "Show all",
                onAction = { filter = BusinessMomentFilter.ALL },
                theme = theme,
            )
            BusinessMomentsContent.TIMELINE -> {
                presentation.groups.forEach { group ->
                    Text(
                        group.label,
                        color = theme.secondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                    )
                    group.entries.forEach { entry -> MomentEntryCard(entry, theme) }
                }
                if (feed.nextCursor != null && !momentId.isNullOrBlank()) {
                    Text(
                        "Load more",
                        color = theme.accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                        modifier = Modifier.clickable {
                            val cursor = feed.nextCursor ?: return@clickable
                            val id = feed.momentId ?: return@clickable
                            val generation = feed.generation
                            scope.launch {
                                repository.getActivity(id, cursor = cursor, limit = PAGE_LIMIT).fold(
                                    onSuccess = {
                                        feed = feed.applyPage(id, generation, it.items, it.nextCursor, append = true)
                                    },
                                    onFailure = {
                                        feed = feed.applyFailure(id, generation, it.message)
                                    },
                                )
                            }
                        },
                    )
                }
            }
            BusinessMomentsContent.LOADING -> Unit
        }
    }
}

@Composable
private fun EmptyBlock(
    title: String,
    body: String?,
    action: String,
    onAction: () -> Unit,
    theme: BusinessActiveTheme,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(theme.card)
            .border(1.dp, theme.border, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, color = theme.text, fontWeight = FontWeight.Bold, fontSize = 15.sp, fontFamily = PlusJakartaSans)
        if (!body.isNullOrBlank()) {
            Text(body, color = theme.secondary, fontSize = 13.sp, fontFamily = PlusJakartaSans)
        }
        Text(
            action,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = PlusJakartaSans,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(theme.accent)
                .clickable(onClick = onAction)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun MomentEntryCard(entry: BusinessMomentsEntry, theme: BusinessActiveTheme) {
    val card = entry.card
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(theme.card)
            .border(1.dp, theme.border, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        entry.eyebrow?.let {
            Text(it, color = theme.secondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
        }
        Text(card.title, color = theme.text, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
        val value = listOfNotNull(card.amountLabel, card.subtitle).joinToString(" · ")
        if (value.isNotEmpty()) {
            Text(value, color = theme.text, fontSize = 13.sp, fontFamily = PlusJakartaSans)
        }
        card.statusLabel?.let {
            Text(it, color = theme.secondary, fontSize = 12.sp, fontFamily = PlusJakartaSans)
        }
        val meta = listOfNotNull(entry.actorLine, entry.timeLabel).joinToString(" · ")
        if (meta.isNotEmpty()) {
            Text(meta, color = theme.secondary, fontSize = 12.sp, fontFamily = PlusJakartaSans)
        }
    }
}
