package com.example.momentra.ui.shell.group.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.ActivityItemDto
import com.example.momentra.data.repository.GroupSliceRepository
import com.example.momentra.domain.AppContext
import com.example.momentra.ui.shell.empty.group.GeBg
import com.example.momentra.ui.shell.empty.group.GeBorder
import com.example.momentra.ui.shell.empty.group.GeSecondary
import com.example.momentra.ui.shell.empty.group.GeText
import com.example.momentra.ui.theme.PlusJakartaSans
import com.example.momentra.ui.theme.shell.MomentThemes
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val RowSurface = Color(0xFF201E28)

/** Full Group activity list with cursor pagination + expense edit/void. */
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
    var error by remember { mutableStateOf<String?>(null) }
    var editingExpenseId by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val accent = MomentThemes.resolve(AppContext.GROUP, momentTypeCode).primary
    val isWedding = groupExperienceFamilyFor(momentTypeCode) == GroupExperienceFamily.WEDDING

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

    LaunchedEffect(momentId, visible) {
        if (!visible) return@LaunchedEffect
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = GeBg,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                "All activity",
                color = GeText,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSans,
            )
            Text(
                "Tap an expense to edit or void it. Settlements and other events are view-only.",
                color = GeSecondary,
                fontSize = 12.sp,
                fontFamily = PlusJakartaSans,
                modifier = Modifier.padding(bottom = 8.dp),
            )
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
                else -> {
                    error?.let {
                        Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans)
                    }
                    items.forEach { item ->
                        val expenseId = item.activityPayload?.expenseId
                        val canEdit = !expenseId.isNullOrBlank() &&
                            (item.activityCode.contains("EXPENSE", ignoreCase = true) || expenseId != null)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(RowSurface)
                                .border(1.dp, GeBorder, RoundedCornerShape(12.dp))
                                .then(
                                    if (canEdit) Modifier.clickable { editingExpenseId = expenseId }
                                    else Modifier,
                                )
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x33FFB598))
                                    .border(1.dp, GeBorder, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(groupActivityGlyph(item.activityCode), fontSize = 14.sp)
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    item.title,
                                    color = GeText,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = PlusJakartaSans,
                                )
                                Text(
                                    formatGroupActivityOccurredAt(item.occurredAt),
                                    color = GeSecondary,
                                    fontSize = 11.sp,
                                    fontFamily = PlusJakartaSans,
                                )
                            }
                            if (canEdit) {
                                Text("›", color = GeSecondary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
}

internal fun groupActivityGlyph(code: String): String = when {
    code.contains("EXPENSE", ignoreCase = true) -> "💸"
    code.contains("SETTLE", ignoreCase = true) -> "✅"
    code.contains("CONTRIB", ignoreCase = true) -> "🤝"
    else -> "📌"
}

internal fun formatGroupActivityOccurredAt(raw: String): String = try {
    val instant = Instant.parse(raw)
    DateTimeFormatter.ofPattern("d MMM · HH:mm", Locale.getDefault())
        .withZone(ZoneId.systemDefault())
        .format(instant)
} catch (_: Exception) {
    raw
}
