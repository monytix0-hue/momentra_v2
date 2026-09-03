package com.example.momentra.ui.shell.personal.relationships.create

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.repository.PersonalSliceRepository
import com.example.momentra.ui.theme.PlusJakartaSans

private val SheetBg = Color(0xFF14121B)
private val SheetElevated = Color(0xFF1C1926)
private val SheetField = Color(0xFF3A3842)
private val SheetText = Color(0xFFE5E0EE)
private val SheetMuted = Color(0xFFC9C4D8)
private val SheetDim = Color(0xFF64748B)
private val SheetPink = Color(0xFFE12A9E)
private val SheetGreen = Color(0xFF10B981)
private val SheetRed = Color(0xFFF87171)
private val SheetOrange = Color(0xFFFF7A3D)
private val SheetBorder = Color(0xFF2A2538)

private val RelFilters = listOf("All", "Partner", "Family", "Friends", "Self")
private val RelMoodChips = listOf("Romantic", "Supportive", "Playful", "Vulnerable", "Intentional")
private val RelDropdown = listOf("Partner", "Family", "Friends", "Self")

/**
 * Hosts Recent Activity (`1036:7697`) + Edit Activity (`1036:7727`) for Relationships Pulse.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalRelationshipsActivityFlow(
    momentId: String?,
    visible: Boolean,
    onDismiss: () -> Unit,
    onChanged: () -> Unit,
    repository: PersonalSliceRepository = remember { PersonalSliceRepository() },
) {
    if (!visible) return

    var items by remember { mutableStateOf<List<RelationshipsActivityItem>>(emptyList()) }
    var filter by remember { mutableStateOf("All") }
    var editing by remember { mutableStateOf<RelationshipsActivityItem?>(null) }
    val recentSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(momentId, visible) {
        if (!visible) return@LaunchedEffect
        repository.getActivity(momentId = momentId, limit = 30).fold(
            onSuccess = { items = mapActivityDtosToRelationships(it.items) },
            onFailure = { items = emptyList() },
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = recentSheetState,
        containerColor = SheetBg,
        dragHandle = null,
    ) {
        RelationshipsRecentActivityBody(
            items = items,
            filter = filter,
            onFilter = { filter = it },
            onClose = onDismiss,
            onEdit = { editing = it },
            onDelete = { id ->
                items = items.filterNot { it.id == id }
                onChanged()
            },
        )
    }

    editing?.let { item ->
        ModalBottomSheet(
            onDismissRequest = { editing = null },
            sheetState = editSheetState,
            containerColor = SheetElevated,
            dragHandle = null,
        ) {
            RelationshipsEditActivityBody(
                item = item,
                onClose = { editing = null },
                onSave = { updated ->
                    items = items.map { if (it.id == updated.id) updated else it }
                    editing = null
                    onChanged()
                },
                onDelete = {
                    items = items.filterNot { it.id == item.id }
                    editing = null
                    onChanged()
                },
            )
        }
    }
}

@Composable
private fun RelationshipsRecentActivityBody(
    items: List<RelationshipsActivityItem>,
    filter: String,
    onFilter: (String) -> Unit,
    onClose: () -> Unit,
    onEdit: (RelationshipsActivityItem) -> Unit,
    onDelete: (String) -> Unit,
) {
    val filtered = if (filter == "All") items else items.filter { it.filter.equals(filter, ignoreCase = true) }
    Column(
        modifier = Modifier
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
                    Text("‹", color = SheetText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    "Recent Activity",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                )
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center,
            ) {
                Text("🔔", fontSize = 16.sp)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RelFilters.forEach { label ->
                val active = filter == label
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFF201E28))
                        .border(
                            width = 1.dp,
                            color = if (active) SheetPink else Color(0xFFC9BFFF).copy(alpha = 0.4f),
                            shape = RoundedCornerShape(999.dp),
                        )
                        .clickable { onFilter(label) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        label,
                        color = if (active) SheetPink else SheetText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = PlusJakartaSans,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            filtered.forEach { item ->
                RelationshipsActivityManageRow(
                    item = item,
                    onEdit = { onEdit(item) },
                    onDelete = { onDelete(item.id) },
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RelationshipsActivityManageRow(
    item: RelationshipsActivityItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SheetPink),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(item.emoji, fontSize = 14.sp)
                }
                Text(
                    item.title,
                    color = SheetText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                )
            }
            Text(item.whenLabel, color = SheetMuted, fontSize = 11.sp, fontFamily = PlusJakartaSans)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(SheetPink)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                item.impact,
                color = SheetBg,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSans,
            )
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF2A2834))
                .clickable(onClick = onEdit),
            contentAlignment = Alignment.Center,
        ) {
            Text("✏️", fontSize = 12.sp)
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF3C1E1E))
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center,
        ) {
            Text("🗑️", fontSize = 12.sp)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RelationshipsEditActivityBody(
    item: RelationshipsActivityItem,
    onClose: () -> Unit,
    onSave: (RelationshipsActivityItem) -> Unit,
    onDelete: () -> Unit,
) {
    var name by remember(item.id) { mutableStateOf(item.title) }
    var impact by remember(item.id) { mutableStateOf(item.impact) }
    var relationship by remember(item.id) { mutableStateOf(item.relationship) }
    var whenLabel by remember(item.id) { mutableStateOf(item.whenLabel) }
    var notes by remember(item.id) { mutableStateOf(item.notes.ifBlank { "Spent the evening cooking dinner together and watching a movie. Really felt present and connected. Need to do this more often." }) }
    var selectedTags by remember(item.id) { mutableStateOf(item.tags.ifEmpty { listOf("Romantic") }.toSet()) }
    var relMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .size(width = 36.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF3A3842)),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Text("×", color = SheetOrange, fontSize = 22.sp)
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Edit Activity", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
                Text("Edit Entry Details", color = Color(0xFF94A3B8), fontSize = 12.sp, fontFamily = PlusJakartaSans)
            }
            Spacer(Modifier.size(40.dp))
        }

        PinkField(label = "ACTIVITY NAME", value = name, onValueChange = { name = it }, focused = true)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                PinkField(label = "IMPACT", value = impact, onValueChange = { impact = it }, focused = true)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("RELATIONSHIP", color = SheetDim, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SheetField)
                            .border(1.dp, Color(0xFF938EA1), RoundedCornerShape(14.dp))
                            .clickable { relMenu = true }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(relationship, color = Color.White, fontSize = 14.sp, fontFamily = PlusJakartaSans)
                        Text("▼", color = Color(0xFF94A3B8), fontSize = 10.sp)
                    }
                    DropdownMenu(expanded = relMenu, onDismissRequest = { relMenu = false }) {
                        RelDropdown.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = {
                                    relationship = opt
                                    relMenu = false
                                },
                            )
                        }
                    }
                }
            }
        }
        PinkField(label = "DATE & TIME", value = whenLabel, onValueChange = { whenLabel = it }, focused = false)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SheetBg)
                .border(1.dp, SheetBorder, RoundedCornerShape(14.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("NOTES", color = SheetDim, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
            BasicTextField(
                value = notes.take(200),
                onValueChange = { notes = it.take(200) },
                textStyle = TextStyle(color = SheetText, fontSize = 14.sp, fontFamily = PlusJakartaSans),
                cursorBrush = SolidColor(SheetPink),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Max 200 characters", color = SheetDim, fontSize = 11.sp, fontFamily = PlusJakartaSans)
                Text(
                    "${notes.length.coerceAtMost(200)}/200",
                    color = SheetGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                )
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RelMoodChips.forEach { chip ->
                val selected = chip in selectedTags
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (selected) SheetPink.copy(alpha = 0.15f) else Color(0xFF2A2538))
                        .border(
                            1.dp,
                            if (selected) SheetPink.copy(alpha = 0.25f) else Color.Transparent,
                            RoundedCornerShape(20.dp),
                        )
                        .clickable {
                            selectedTags = if (selected) selectedTags - chip else selectedTags + chip
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        chip,
                        color = if (selected) SheetPink else Color(0xFF9CA3AF),
                        fontSize = 12.sp,
                        fontFamily = PlusJakartaSans,
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(999.dp))
                .background(SheetGreen)
                .clickable {
                    onSave(
                        item.copy(
                            title = name,
                            impact = impact,
                            relationship = relationship,
                            filter = relationship,
                            whenLabel = whenLabel,
                            notes = notes,
                            tags = selectedTags.toList(),
                        ),
                    )
                }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Save Changes", color = SheetBg, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }

        Text(
            "Delete Activity",
            color = SheetRed,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
            modifier = Modifier
                .clickable(onClick = onDelete)
                .padding(8.dp),
        )
    }
}

@Composable
private fun PinkField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    focused: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label, color = SheetDim, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = SheetText, fontSize = 14.sp, fontFamily = PlusJakartaSans),
            cursorBrush = SolidColor(SheetPink),
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (focused) {
                        Modifier.drawBehind {
                            drawRoundRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(SheetPink.copy(alpha = 0.35f), Color.Transparent),
                                ),
                            )
                        }
                    } else Modifier,
                )
                .clip(RoundedCornerShape(14.dp))
                .background(SheetField)
                .border(
                    1.dp,
                    if (focused) SheetPink else Color(0xFF938EA1),
                    RoundedCornerShape(14.dp),
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
        )
    }
}
