package com.example.momentra.ui.shell.business.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.ActivityItemDto
import com.example.momentra.data.repository.BusinessSliceRepository
import com.example.momentra.ui.shell.components.MomentraModalBottomSheet
import com.example.momentra.ui.shell.empty.group.GeBg
import com.example.momentra.ui.shell.empty.group.GeSecondary
import com.example.momentra.ui.shell.empty.group.GeText
import com.example.momentra.ui.shell.group.shared.GroupActivityCategoryFilter
import com.example.momentra.ui.shell.group.shared.GroupActivityRow
import com.example.momentra.ui.shell.group.shared.groupActivityTree
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.launch

/** Shared business activity sheet for TeamOps / Runway / Ops history CTAs. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessRecentActivityFlow(
    momentId: String?,
    visible: Boolean,
    onDismiss: () -> Unit,
    accent: Color = Color(0xFF818CF8),
    subtitle: String = "Updates, spend, memories, and other events.",
    repository: BusinessSliceRepository = remember { BusinessSliceRepository() },
) {
    if (!visible || momentId.isNullOrBlank()) return

    var items by remember { mutableStateOf<List<ActivityItemDto>>(emptyList()) }
    var nextCursor by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var filter by remember { mutableStateOf(GroupActivityCategoryFilter.ALL_ID) }
    val scope = rememberCoroutineScope()
    val chips = remember { GroupActivityCategoryFilter.chips(null) }
    val filteredItems = remember(items, filter) {
        items.filter { GroupActivityCategoryFilter.matches(it, filter) }
    }
    val activityNodes = remember(filteredItems) { groupActivityTree(filteredItems) }

    LaunchedEffect(momentId, visible) {
        if (!visible) return@LaunchedEffect
        loading = true
        error = null
        nextCursor = null
        filter = GroupActivityCategoryFilter.ALL_ID
        repository.getActivity(momentId, limit = 20).fold(
            onSuccess = {
                items = it.items
                nextCursor = it.nextCursor
            },
            onFailure = {
                error = it.message
                items = emptyList()
            },
        )
        loading = false
    }

    MomentraModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = GeBg,
        skipPartiallyExpanded = false,
        dragHandle = null,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "All activity",
                color = GeText,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSans,
            )
            Text(
                subtitle,
                color = GeSecondary,
                fontSize = 12.sp,
                fontFamily = PlusJakartaSans,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            BusinessActivityFilterChips(chips = chips, filter = filter, accent = accent, onSelect = { filter = it })
            when {
                loading && items.isEmpty() -> {
                    CircularProgressIndicator(color = accent, modifier = Modifier.padding(16.dp))
                }
                error != null && items.isEmpty() -> {
                    Text(error.orEmpty(), color = Color(0xFFF87171), fontSize = 13.sp, fontFamily = PlusJakartaSans)
                }
                items.isEmpty() -> {
                    Text(
                        "No activity yet",
                        color = GeText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = PlusJakartaSans,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
                filteredItems.isEmpty() -> {
                    Text(
                        "No activity in this category",
                        color = GeText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = PlusJakartaSans,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
                else -> {
                    error?.let {
                        Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans)
                    }
                    activityNodes.forEach { node ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            GroupActivityRow(
                                item = node.item,
                                accent = accent,
                                textColor = GeText,
                                secondaryColor = GeSecondary,
                                showChevron = false,
                                compactPadding = false,
                            )
                            node.children.forEach { child ->
                                GroupActivityRow(
                                    item = child,
                                    accent = accent,
                                    textColor = GeText,
                                    secondaryColor = GeSecondary,
                                    showChevron = false,
                                    compactPadding = false,
                                    isChild = true,
                                )
                            }
                        }
                    }
                    if (nextCursor != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = !loadingMore) {
                                    val cursor = nextCursor ?: return@clickable
                                    scope.launch {
                                        loadingMore = true
                                        repository.getActivity(momentId, cursor = cursor, limit = 20).fold(
                                            onSuccess = {
                                                items = items + it.items
                                                nextCursor = it.nextCursor
                                            },
                                            onFailure = { error = it.message },
                                        )
                                        loadingMore = false
                                    }
                                }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (loadingMore) {
                                CircularProgressIndicator(color = accent, modifier = Modifier.size(22.dp))
                            } else {
                                Text(
                                    "Load more",
                                    color = accent,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = PlusJakartaSans,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BusinessActivityFilterChips(
    chips: List<GroupActivityCategoryFilter.FilterChip>,
    filter: String,
    accent: Color,
    onSelect: (String) -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        chips.forEach { chip ->
            val selected = filter == chip.id
            Text(
                "${chip.emoji} ${chip.label}",
                color = if (selected) Color.White else Color(0xFFC9C4D8),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) accent else Color.White.copy(alpha = 0.06f))
                    .border(
                        1.dp,
                        if (selected) accent else Color.White.copy(alpha = 0.08f),
                        RoundedCornerShape(50),
                    )
                    .clickable {
                        onSelect(
                            if (filter == chip.id) GroupActivityCategoryFilter.ALL_ID else chip.id,
                        )
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}
