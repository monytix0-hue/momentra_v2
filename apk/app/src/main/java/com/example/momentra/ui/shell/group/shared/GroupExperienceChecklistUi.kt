package com.example.momentra.ui.shell.group.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R
import com.example.momentra.data.api.GroupLifePlanningItemDto
import com.example.momentra.data.repository.GroupSliceRepository
import com.example.momentra.ui.shell.components.MomentraModalBottomSheet
import com.example.momentra.ui.shell.group.wedding.create.ChipRow
import com.example.momentra.ui.shell.group.wedding.create.FieldLabel
import com.example.momentra.ui.shell.group.wedding.create.PrimaryCta
import com.example.momentra.ui.shell.group.wedding.create.SheetAccent
import com.example.momentra.ui.shell.group.wedding.create.SheetField
import com.example.momentra.ui.shell.group.wedding.create.SheetHeader
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExperienceChecklistAddSheet(
    visible: Boolean,
    momentId: String?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    accent: SheetAccent,
    surface: Color = Color(0xFF1C1A24),
    repository: GroupSliceRepository = remember { GroupSliceRepository() },
) {
    if (!visible) return
    MomentraModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = surface,
        skipPartiallyExpanded = false,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 40.dp, height = 5.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
            )
        },
    ) {
        ExperienceChecklistSheetBody(
            momentId = momentId,
            repository = repository,
            onDismiss = onDismiss,
            onSaved = onSaved,
            accent = accent,
        )
    }
}

/** Quick Add / Moments sheet body: title + category chips + seed packing list. */
@Composable
fun ExperienceChecklistSheetBody(
    momentId: String?,
    repository: GroupSliceRepository,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    accent: SheetAccent,
) {
    var title by remember { mutableStateOf("") }
    var categoryCode by remember { mutableStateOf(GroupExperienceChecklistCatalog.defaultCode()) }
    var submitting by remember { mutableStateOf(false) }
    var seeding by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val categoryLabels = GroupExperienceChecklistCatalog.categories.map { it.label }
    val selectedLabel = GroupExperienceChecklistCatalog.labelForCode(categoryCode)

    SheetHeader(
        R.drawable.ic_group_qa_calendar,
        "Checklist",
        "Shared packing & essentials",
        accent = accent,
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Item")
        SheetField(title, { title = it }, "What to pack or prepare", minHeight = 42)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Category")
        ChipRow(categoryLabels, selectedLabel, accent) { label ->
            categoryCode = GroupExperienceChecklistCatalog.categories
                .firstOrNull { it.label == label }?.code
                ?: GroupExperienceChecklistCatalog.defaultCode()
        }
    }
    error?.let {
        Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans)
    }
    PrimaryCta(
        label = "Add",
        enabled = !momentId.isNullOrBlank() && title.isNotBlank() && !submitting && !seeding,
        accent = accent,
        loading = submitting,
        onClick = {
            val mid = momentId ?: return@PrimaryCta
            val trimmed = title.trim()
            if (trimmed.isEmpty()) return@PrimaryCta
            scope.launch {
                submitting = true
                error = null
                repository.createPlanningItem(
                    momentId = mid,
                    title = trimmed,
                    categoryCode = categoryCode,
                ).onSuccess {
                    submitting = false
                    onSaved()
                    onDismiss()
                }.onFailure { e ->
                    submitting = false
                    error = e.message ?: "Could not add checklist item"
                }
            }
        },
    )
    PrimaryCta(
        label = if (seeding) "Seeding…" else "Seed packing list",
        enabled = !momentId.isNullOrBlank() && !submitting && !seeding,
        accent = accent,
        loading = seeding,
        lightLabel = true,
        onClick = {
            val mid = momentId ?: return@PrimaryCta
            scope.launch {
                seeding = true
                error = null
                val existing = repository.listPlanningItems(mid).getOrNull()?.items.orEmpty()
                val existingKeys = existing
                    .filter { GroupExperienceChecklistCatalog.isChecklistCode(it.categoryCode) }
                    .mapNotNull { item ->
                        val t = item.title?.trim()?.lowercase(Locale.US) ?: return@mapNotNull null
                        val c = item.categoryCode?.trim()?.uppercase(Locale.US) ?: return@mapNotNull null
                        "$c|$t"
                    }
                    .toMutableSet()
                var created = 0
                var failed: String? = null
                for ((code, seedTitle) in GroupExperienceChecklistCatalog.seedPackingList) {
                    val key = "${code}|${seedTitle.trim().lowercase(Locale.US)}"
                    if (key in existingKeys) continue
                    val result = repository.createPlanningItem(
                        momentId = mid,
                        title = seedTitle,
                        categoryCode = code,
                    )
                    if (result.isSuccess) {
                        existingKeys.add(key)
                        created++
                    } else {
                        failed = result.exceptionOrNull()?.message
                        break
                    }
                }
                seeding = false
                if (failed != null) {
                    error = failed
                } else {
                    onSaved()
                    if (created > 0) onDismiss()
                    else error = "Packing list already seeded"
                }
            }
        },
    )
}

@Composable
fun MomentsChecklistSection(
    items: List<GroupLifePlanningItemDto>,
    momentId: String?,
    chrome: MomentsChrome,
    repository: GroupSliceRepository = remember { GroupSliceRepository() },
    onChanged: () -> Unit = {},
    onAdd: (() -> Unit)? = null,
) {
    val groups = remember(items) { GroupExperienceChecklistCatalog.groupedByCategory(items) }
    val scope = rememberCoroutineScope()
    var togglingId by remember { mutableStateOf<String?>(null) }

    MomentsSectionHeader("Checklist  ✅", chrome)
    if (groups.isEmpty()) {
        GroupEmptySection(
            "No checklist items yet",
            "Add essentials from Quick Add, or seed the packing list.",
        )
        if (onAdd != null) {
            Text(
                "Add checklist item",
                color = chrome.accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .clickable(onClick = onAdd)
                    .padding(vertical = 4.dp),
            )
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        groups.forEach { (category, groupItems) ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    category.label,
                    color = chrome.accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                )
                groupItems.forEach { item ->
                    val done = item.status?.equals("DONE", ignoreCase = true) == true
                    val id = item.planningItemId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(chrome.card)
                            .border(1.dp, chrome.border, RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Checkbox(
                            checked = done,
                            onCheckedChange = { checked ->
                                if (id.isNullOrBlank() || momentId.isNullOrBlank() || togglingId != null) return@Checkbox
                                val next = if (checked) "DONE" else "OPEN"
                                scope.launch {
                                    togglingId = id
                                    repository.updatePlanningItem(
                                        momentId = momentId,
                                        planningItemId = id,
                                        title = item.title ?: "Item",
                                        categoryCode = item.categoryCode,
                                        status = next,
                                    ).onSuccess { onChanged() }
                                    togglingId = null
                                }
                            },
                            enabled = !id.isNullOrBlank() && !momentId.isNullOrBlank() && togglingId != id,
                            colors = CheckboxDefaults.colors(
                                checkedColor = chrome.accent,
                                uncheckedColor = chrome.secondary,
                                checkmarkColor = Color.White,
                            ),
                        )
                        Text(
                            item.title ?: "Item",
                            color = if (done) chrome.secondary else chrome.text,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = PlusJakartaSans,
                            textDecoration = if (done) TextDecoration.LineThrough else null,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        if (onAdd != null) {
            Text(
                "Add item",
                color = chrome.accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .clickable(onClick = onAdd)
                    .padding(vertical = 2.dp),
            )
        }
    }
}
