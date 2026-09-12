package com.example.momentra.ui.shell.group.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.example.momentra.data.api.GroupContributionItemDto
import com.example.momentra.data.repository.GroupSliceRepository
import com.example.momentra.domain.AppContext
import com.example.momentra.ui.shell.components.MomentraModalBottomSheet
import com.example.momentra.ui.shell.empty.group.GeBg
import com.example.momentra.ui.shell.empty.group.GeSecondary
import com.example.momentra.ui.shell.empty.group.GeText
import com.example.momentra.ui.theme.PlusJakartaSans
import com.example.momentra.ui.theme.shell.MomentThemes
import kotlinx.coroutines.launch
import java.util.Locale

/** Full Group activity list with cursor pagination + expense/contribution edit/void. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupRecentActivityFlow(
    momentId: String?,
    visible: Boolean,
    onDismiss: () -> Unit,
    onChanged: () -> Unit,
    momentTypeCode: String? = null,
    repository: GroupSliceRepository = remember { GroupSliceRepository() },
) {
    if (!visible || momentId.isNullOrBlank()) return

    var items by remember { mutableStateOf<List<ActivityItemDto>>(emptyList()) }
    var nextCursor by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var resolvingContribution by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var filter by remember(momentTypeCode) { mutableStateOf(GroupActivityCategoryFilter.ALL_ID) }
    var editingExpenseId by remember { mutableStateOf<String?>(null) }
    var editingContribution by remember { mutableStateOf<GroupContributionItemDto?>(null) }
    val scope = rememberCoroutineScope()
    val accent = MomentThemes.resolve(AppContext.GROUP, momentTypeCode).primary
    val isWedding = groupExperienceFamilyFor(momentTypeCode) == GroupExperienceFamily.WEDDING
    val chips = remember(momentTypeCode) { GroupActivityCategoryFilter.chips(momentTypeCode) }
    val filteredItems = remember(items, filter) {
        items.filter { GroupActivityCategoryFilter.matches(it, filter) }
    }
    val activityNodes = remember(filteredItems) { groupActivityTree(filteredItems) }

    fun reload() {
        scope.launch {
            loading = true
            error = null
            nextCursor = null
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
    }

    fun openContributionEdit(contributionId: String) {
        scope.launch {
            resolvingContribution = true
            error = null
            repository.listContributions(momentId, limit = 100).fold(
                onSuccess = { dto ->
                    val found = dto.items.firstOrNull { it.contributionId == contributionId }
                    if (found != null) {
                        editingContribution = found
                    } else {
                        error = "Contribution not found"
                    }
                },
                onFailure = { error = it.message },
            )
            resolvingContribution = false
        }
    }

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
                "Tap an expense or contribution to edit or void it. Settlements and other events are view-only.",
                color = GeSecondary,
                fontSize = 12.sp,
                fontFamily = PlusJakartaSans,
                modifier = Modifier.padding(bottom = 4.dp),
            )

            Row(
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
                                filter = if (filter == chip.id) {
                                    GroupActivityCategoryFilter.ALL_ID
                                } else {
                                    chip.id
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }

            when {
                loading && items.isEmpty() -> {
                    CircularProgressIndicator(color = accent, modifier = Modifier.padding(16.dp))
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
                    Text(
                        "Expenses, settlements, and updates will show here.",
                        color = GeSecondary,
                        fontSize = 13.sp,
                        fontFamily = PlusJakartaSans,
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
                    Text(
                        "Try another hub filter, or load more if the list is paginated.",
                        color = GeSecondary,
                        fontSize = 13.sp,
                        fontFamily = PlusJakartaSans,
                    )
                }
                else -> {
                    error?.let {
                        Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans)
                    }
                    if (resolvingContribution) {
                        CircularProgressIndicator(color = accent, modifier = Modifier.padding(8.dp))
                    }
                    activityNodes.forEach { node ->
                        val item = node.item
                        val expenseId = item.activityPayload?.expenseId
                        val contributionId = item.activityPayload?.contributionId
                        val canEditExpense = !expenseId.isNullOrBlank() &&
                            (item.activityCode.contains("EXPENSE", ignoreCase = true) || expenseId != null)
                        val canEditContribution = !contributionId.isNullOrBlank() && isContributionActivity(item)
                        val canEdit = !groupActivityNodeHasVoidChild(node) &&
                            (canEditExpense || canEditContribution)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            GroupActivityRow(
                                item = item,
                                accent = accent,
                                textColor = GeText,
                                secondaryColor = GeSecondary,
                                showChevron = canEdit,
                                compactPadding = false,
                                onClick = if (canEdit) {
                                    {
                                        when {
                                            canEditExpense -> editingExpenseId = expenseId
                                            canEditContribution && contributionId != null ->
                                                openContributionEdit(contributionId)
                                        }
                                    }
                                } else {
                                    null
                                },
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

    val editId = editingExpenseId
    if (editId != null) {
        GroupExpenseSheet(
            momentId = momentId,
            visible = true,
            onDismiss = { editingExpenseId = null },
            onSaved = {
                editingExpenseId = null
                onChanged()
                reload()
            },
            onDeleted = {
                editingExpenseId = null
                onChanged()
                reload()
            },
            expenseId = editId,
            isWedding = isWedding,
            momentTypeCode = momentTypeCode,
            repository = repository,
        )
    }

    val editContribution = editingContribution
    if (editContribution != null) {
        GroupContributionSheet(
            momentId = momentId,
            visible = true,
            onDismiss = { editingContribution = null },
            onSaved = {
                editingContribution = null
                onChanged()
                reload()
            },
            onDeleted = {
                editingContribution = null
                onChanged()
                reload()
            },
            isWedding = isWedding,
            editingContribution = editContribution,
            repository = repository,
        )
    }
}

private fun isContributionActivity(item: ActivityItemDto): Boolean {
    if (!item.activityPayload?.contributionId.isNullOrBlank()) return true
    val upper = item.activityCode.uppercase(Locale.US)
    return upper.contains("CONTRIBUTION") ||
        (upper.contains("CONTRIB") && !upper.contains("CONTRIBUTOR"))
}
