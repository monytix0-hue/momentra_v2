package com.example.momentra.ui.shell.personal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.ActivityItemDto
import com.example.momentra.data.repository.PersonalSliceRepository
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.launch

private val TimelineBg = Color(0xFF14121B)
private val TimelineText = Color(0xFFE5E0EE)
private val TimelineMuted = Color(0xFFC9C4D8)
private val TimelinePurple = Color(0xFF7C5CFC)
private val TimelineBorder = Color.White.copy(alpha = 0.08f)
private val TimelineSurface = Color(0xFF201E28)

/** Hosts Activity Timeline + edit sheets for Life Ops / Lifestyle / Future Pulse. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalRecentActivityFlow(
    momentId: String?,
    visible: Boolean,
    onDismiss: () -> Unit,
    onChanged: () -> Unit,
    repository: PersonalSliceRepository = remember { PersonalSliceRepository() },
) {
    if (!visible) return

    var items by remember { mutableStateOf<List<ActivityItemDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var filter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<ActivityItemDto?>(null) }
    val recentSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    LaunchedEffect(momentId, visible) {
        if (!visible) return@LaunchedEffect
        loading = true
        repository.getActivity(momentId = momentId, limit = 50).fold(
            onSuccess = { items = it.items },
            onFailure = { items = emptyList() },
        )
        loading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = recentSheetState,
        containerColor = TimelineBg,
        dragHandle = null,
    ) {
        PersonalRecentActivityScreen(
            items = items,
            loading = loading,
            filter = filter,
            searchQuery = searchQuery,
            onFilter = { filter = it },
            onSearch = { searchQuery = it },
            onClose = onDismiss,
            onEdit = { editing = it },
        )
    }

    editing?.let { item ->
        if (momentId != null) {
            ModalBottomSheet(
                onDismissRequest = { editing = null },
                sheetState = editSheetState,
                containerColor = Color(0xFF191622),
                dragHandle = null,
            ) {
                if (PersonalActivityTimelineDerived.isExpense(item)) {
                    PersonalEditTransactionSheet(
                        item = item,
                        momentId = momentId,
                        onClose = { editing = null },
                        onSaved = {
                            editing = null
                            onChanged()
                            scope.launch {
                                repository.getActivity(momentId = momentId, limit = 50).fold(
                                    onSuccess = { items = it.items },
                                    onFailure = { },
                                )
                            }
                        },
                        onDeleted = {
                            editing = null
                            onChanged()
                            scope.launch {
                                repository.getActivity(momentId = momentId, limit = 50).fold(
                                    onSuccess = { items = it.items },
                                    onFailure = { },
                                )
                            }
                        },
                    )
                } else {
                    PersonalEditActivitySheet(
                        item = item,
                        momentId = momentId,
                        onClose = { editing = null },
                        onSaved = {
                            editing = null
                            onChanged()
                            scope.launch {
                                repository.getActivity(momentId = momentId, limit = 50).fold(
                                    onSuccess = { items = it.items },
                                    onFailure = { },
                                )
                            }
                        },
                        repository = repository,
                    )
                }
            }
        }
    }
}

@Composable
fun PersonalRecentActivityScreen(
    items: List<ActivityItemDto>,
    loading: Boolean,
    filter: String,
    searchQuery: String,
    onFilter: (String) -> Unit,
    onSearch: (String) -> Unit,
    onClose: () -> Unit,
    onEdit: (ActivityItemDto) -> Unit,
    modifier: Modifier = Modifier,
) {
    val filtered = items
        .filter { PersonalActivityTimelineDerived.isVisible(it) }
        .filter { PersonalActivityTimelineDerived.matchesFilter(it, filter) }
        .filter { PersonalActivityTimelineDerived.matchesSearch(it, searchQuery) }
    val stats = PersonalActivityTimelineDerived.computeStats(items)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.92f)
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onClose),
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("‹", color = TimelineText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text(
                        "Activity",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                    )
                    Text(
                        "Your daily rhythm & money",
                        color = TimelineMuted,
                        fontSize = 12.sp,
                        fontFamily = PlusJakartaSans,
                    )
                }
            }
        }

        BasicTextField(
            value = searchQuery,
            onValueChange = onSearch,
            singleLine = true,
            textStyle = TextStyle(color = TimelineText, fontSize = 14.sp, fontFamily = PlusJakartaSans),
            cursorBrush = SolidColor(TimelinePurple),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(TimelineSurface)
                .border(1.dp, TimelineBorder, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            decorationBox = { inner ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("🔍", fontSize = 14.sp)
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                "Search activity…",
                                color = TimelineMuted,
                                fontSize = 14.sp,
                                fontFamily = PlusJakartaSans,
                            )
                        }
                        inner()
                    }
                }
            },
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PersonalActivityTimelineDerived.primaryFilters.forEach { chip ->
                TimelineFilterChip(
                    label = chip.label,
                    emoji = chip.emoji,
                    selected = filter == chip.id,
                    onClick = { onFilter(if (filter == chip.id) "All" else chip.id) },
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PersonalActivityTimelineDerived.categoryFilters.forEach { chip ->
                TimelineFilterChip(
                    label = chip.label,
                    emoji = chip.emoji,
                    selected = filter == chip.id,
                    onClick = { onFilter(if (filter == chip.id) "All" else chip.id) },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TimelineStatCard("TOTAL LOGS", stats.totalLogs.toString(), Modifier.weight(1f))
            TimelineStatCard("THIS MONTH", stats.thisMonth.toString(), Modifier.weight(1f))
            TimelineStatCard("TOTAL AMOUNT", stats.totalAmountLabel, Modifier.weight(1f))
        }

        Spacer(Modifier.height(12.dp))

        when {
            loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = TimelinePurple)
                }
            }
            filtered.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (searchQuery.isNotBlank() || filter != "All") {
                            "No activity matches this filter."
                        } else {
                            "No activity yet — log your first entry from Pulse."
                        },
                        color = TimelineMuted,
                        fontSize = 14.sp,
                        fontFamily = PlusJakartaSans,
                    )
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    filtered.forEach { item ->
                        TimelineActivityRow(
                            item = item,
                            onEdit = { onEdit(item) },
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun TimelineFilterChip(
    label: String,
    emoji: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) TimelinePurple else Color.White.copy(alpha = 0.06f))
            .border(
                1.dp,
                if (selected) TimelinePurple else TimelineBorder,
                RoundedCornerShape(999.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            "$emoji $label",
            color = if (selected) Color.White else TimelineMuted,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
        )
    }
}

@Composable
private fun TimelineStatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(TimelineSurface)
            .border(1.dp, TimelineBorder, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            label,
            color = TimelineMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
        )
        Text(
            value,
            color = TimelineText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
    }
}

@Composable
private fun TimelineActivityRow(
    item: ActivityItemDto,
    onEdit: () -> Unit,
) {
    val visual = PersonalActivityTimelineDerived.rowVisual(item)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, TimelineBorder, RoundedCornerShape(14.dp)),
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(visual.accent),
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(visual.emoji, fontSize = 22.sp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    item.title,
                    color = TimelineText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                )
                Text(
                    visual.metadata,
                    color = TimelineMuted,
                    fontSize = 11.sp,
                    fontFamily = PlusJakartaSans,
                )
                Text(
                    visual.timeLabel,
                    color = TimelineMuted.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                )
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(TimelinePurple.copy(alpha = 0.15f))
                    .clickable(onClick = onEdit),
                contentAlignment = Alignment.Center,
            ) {
                Text("✏️", fontSize = 14.sp)
            }
        }
    }
}
