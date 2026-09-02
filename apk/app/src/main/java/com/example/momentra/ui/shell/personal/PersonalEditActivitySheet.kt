package com.example.momentra.ui.shell.personal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.ActivityItemDto
import com.example.momentra.data.repository.PersonalSliceRepository
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val EditBg = Color(0xFF1C1926)
private val EditField = Color(0xFF3A3842)
private val EditText = Color(0xFFE5E0EE)
private val EditMuted = Color(0xFFC9C4D8)
private val EditDim = Color(0xFF64748B)
private val EditAccent = Color(0xFF7C5CFC)
private val EditPink = Color(0xFFE12A9E)
private val EditGreen = Color(0xFF10B981)
private val EditRed = Color(0xFFF87171)
private val EditBorder = Color(0xFF2A2538)

private val TagOptions = listOf("Joy", "Vitality", "Exploration", "Spend", "Ritual", "Social")
private val CategoryOptions = listOf(
    "Food", "Transport", "Shopping", "Health", "Entertainment", "Home", "Other",
)

private val SaveGradient = Brush.horizontalGradient(listOf(Color(0xFFEC4899), Color(0xFF7C5CFC)))

/** Figma edit activity form — expense or lifestyle update. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersonalEditActivitySheet(
    item: ActivityItemDto,
    momentId: String,
    onClose: () -> Unit,
    onSaved: () -> Unit,
    onDeleted: () -> Unit = {},
    repository: PersonalSliceRepository = remember { PersonalSliceRepository() },
    modifier: Modifier = Modifier,
) {
    val payload = item.activityPayload
    val isExpense = payload?.expenseId != null
    val activityId = payload?.activityId

    var name by remember(item) { mutableStateOf(item.title) }
    var amount by remember(item) { mutableStateOf(payload?.amount.orEmpty()) }
    var category by remember(item) {
        mutableStateOf(payload?.categoryCode?.replace('_', ' ')?.replaceFirstChar {
            it.titlecase(Locale.getDefault())
        }.orEmpty().ifBlank { CategoryOptions.first() })
    }
    var notes by remember(item) { mutableStateOf(payload?.description.orEmpty()) }
    var selectedTags by remember(item) { mutableStateOf(setOf<String>()) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val whenLabel = remember(item) { formatEditOccurredAt(item.occurredAt) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(EditBg)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
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
                Text("×", color = EditPink, fontSize = 22.sp)
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Edit Activity",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                )
                Text(
                    "Edit Entry Details",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontFamily = PlusJakartaSans,
                )
            }
            Spacer(Modifier.size(40.dp))
        }

        EditFieldBlock(label = "ACTIVITY NAME", value = name, onValueChange = { name = it })

        if (isExpense) {
            EditFieldBlock(
                label = "AMOUNT",
                value = amount,
                onValueChange = { amount = it },
                keyboardType = KeyboardType.Decimal,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "CATEGORY",
                color = EditDim,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CategoryOptions.forEach { option ->
                    val selected = category.equals(option, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (selected) EditAccent else EditField)
                            .border(
                                1.dp,
                                if (selected) EditAccent else Color(0xFF938EA1),
                                RoundedCornerShape(999.dp),
                            )
                            .clickable { category = option }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text(
                            option,
                            color = if (selected) Color.White else EditText,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                            fontFamily = PlusJakartaSans,
                        )
                    }
                }
            }
        }

        EditFieldBlock(
            label = "DATE & TIME",
            value = whenLabel,
            onValueChange = {},
            readOnly = true,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF14121B))
                .border(1.dp, EditBorder, RoundedCornerShape(14.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "NOTES",
                color = EditDim,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
            BasicTextField(
                value = notes.take(200),
                onValueChange = { notes = it.take(200) },
                textStyle = TextStyle(color = EditText, fontSize = 14.sp, fontFamily = PlusJakartaSans),
                cursorBrush = SolidColor(EditPink),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Max 200 characters", color = EditDim, fontSize = 11.sp, fontFamily = PlusJakartaSans)
                Text(
                    "${notes.length.coerceAtMost(200)}/200",
                    color = EditGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "TAGS",
                color = EditDim,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TagOptions.forEach { tag ->
                    val selected = tag in selectedTags
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (selected) EditPink.copy(alpha = 0.2f) else EditField)
                            .border(
                                1.dp,
                                if (selected) EditPink else Color(0xFF938EA1),
                                RoundedCornerShape(999.dp),
                            )
                            .clickable {
                                selectedTags = if (selected) selectedTags - tag else selectedTags + tag
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text(
                            tag,
                            color = if (selected) EditPink else EditText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = PlusJakartaSans,
                        )
                    }
                }
            }
        }

        error?.let {
            Text(it, color = EditRed, fontSize = 12.sp, fontFamily = PlusJakartaSans)
        }

        val canSave = name.isNotBlank() && !submitting && (isExpense || activityId != null)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (canSave) 1f else 0.6f)
                .clip(RoundedCornerShape(14.dp))
                .background(SaveGradient)
                .clickable(enabled = canSave) {
                    submitting = true
                    error = null
                    val trimmedNotes = notes.trim().ifBlank { null }
                    val tagSuffix = if (selectedTags.isEmpty()) null else selectedTags.joinToString(", ")
                    val description = listOfNotNull(trimmedNotes, tagSuffix?.let { "Tags: $it" })
                        .joinToString(" · ")
                        .ifBlank { null }
                    scope.launch {
                        val result = when {
                            isExpense -> {
                                repository.updateExpense(
                                    momentId = momentId,
                                    expenseId = payload!!.expenseId!!,
                                    amount = amount.trim().ifBlank { null },
                                    description = description ?: name.trim(),
                                    categoryCode = category.uppercase().replace(' ', '_'),
                                )
                            }
                            activityId != null -> {
                                repository.updateLifestyleActivity(
                                    momentId = momentId,
                                    activityId = activityId,
                                    title = name.trim(),
                                    description = description,
                                )
                            }
                            else -> Result.failure(IllegalStateException("This activity cannot be edited yet"))
                        }
                        submitting = false
                        result.fold(
                            onSuccess = { onSaved() },
                            onFailure = { e -> error = e.message ?: "Could not save" },
                        )
                    }
                }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (submitting) "Saving…" else "Save",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSans,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(EditRed.copy(alpha = 0.12f))
                .border(1.dp, EditRed.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                .clickable(enabled = !submitting && activityId != null) {
                    showDeleteConfirm = true
                }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Delete",
                color = EditRed,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSans,
            )
        }

        if (showDeleteConfirm) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(EditField)
                        .clickable { showDeleteConfirm = false }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("Cancel", color = EditMuted, fontFamily = PlusJakartaSans) }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(EditRed)
                        .clickable {
                            val id = activityId ?: return@clickable
                            submitting = true
                            scope.launch {
                                repository.voidLifestyleActivity(momentId, id).fold(
                                    onSuccess = {
                                        submitting = false
                                        onDeleted()
                                    },
                                    onFailure = { e ->
                                        submitting = false
                                        error = e.message ?: "Could not delete"
                                        showDeleteConfirm = false
                                    },
                                )
                            }
                        }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("Delete", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans) }
            }
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun EditFieldBlock(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    readOnly: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            label,
            color = EditDim,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            readOnly = readOnly,
            enabled = !readOnly,
            singleLine = label != "NOTES",
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            textStyle = TextStyle(color = EditText, fontSize = 14.sp, fontFamily = PlusJakartaSans),
            cursorBrush = SolidColor(EditPink),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(EditField)
                .border(1.dp, Color(0xFF938EA1), RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
        )
    }
}

private fun formatEditOccurredAt(iso: String): String = try {
    val instant = Instant.parse(iso)
    DateTimeFormatter.ofPattern("EEE, MMM d · h:mm a", Locale.getDefault())
        .withZone(ZoneId.systemDefault())
        .format(instant)
} catch (_: Exception) {
    iso
}
