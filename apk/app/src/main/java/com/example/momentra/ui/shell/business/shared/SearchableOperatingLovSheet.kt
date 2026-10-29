package com.example.momentra.ui.shell.business.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.momentra.ui.theme.PlusJakartaSans

data class OperatingLovOption(
    val id: String,
    val name: String,
    val kind: String,
    val unit: String? = null,
    val subtitle: String? = null,
    val conversionToBase: String? = null,
    val detailAmount: String? = null,
)

object OperatingSeedUoms {
    val all: List<OperatingLovOption> = listOf(
        "pc" to "piece (pc)",
        "pair" to "pair",
        "set" to "set",
        "pack" to "pack",
        "box" to "box",
        "carton" to "carton",
        "dozen" to "dozen (12 pc)",
        "kg" to "kg",
        "g" to "g",
        "tonne" to "tonne",
        "L" to "litre (L)",
        "mL" to "mL",
        "m" to "metre (m)",
        "cm" to "cm",
        "foot" to "foot",
        "sq ft" to "sq ft",
        "sq m" to "sq m",
    ).map { (code, label) ->
        OperatingLovOption(id = "seed-uom-$code", name = code, kind = "UOM", unit = code, subtitle = label)
    }

    fun canonical(code: String?): String {
        val raw = code?.trim().orEmpty()
        if (raw.isEmpty()) return ""
        return when (raw.lowercase()) {
            "piece", "pieces", "count", "counts", "pcs" -> "pc"
            "litre", "liter", "litres", "liters" -> "L"
            else -> all.firstOrNull { it.name.equals(raw, ignoreCase = true) || it.subtitle.equals(raw, ignoreCase = true) }?.name ?: raw
        }
    }

    fun dimension(code: String): String? = when (canonical(code).lowercase()) {
        "pc", "pair", "set", "pack", "box", "carton", "dozen" -> "count"
        "kg", "g", "tonne" -> "mass"
        "l", "ml" -> "volume"
        "m", "cm", "foot" -> "length"
        "sq ft", "sq m" -> "area"
        else -> null
    }

    fun compatible(code: String, base: String, conversion: String?): Boolean {
        val left = dimension(code) ?: return true
        val right = dimension(base) ?: return true
        if (left != right) return false
        if (code.trim().lowercase() in setOf("pack", "box", "carton")) {
            return (conversion?.toDoubleOrNull() ?: 0.0) > 0
        }
        return true
    }
}

@Composable
fun SearchableOperatingLovSheet(
    title: String,
    kind: String,
    options: List<OperatingLovOption>,
    canAdd: Boolean,
    accent: Color = Color(0xFF6DDCFF),
    unitChoices: List<OperatingLovOption> = emptyList(),
    pageSize: Int? = null,
    onSelect: (OperatingLovOption) -> Unit,
    /** name, base unit, optional conversionToBase */
    onAdd: ((String, String?, String?) -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var adding by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newUnit by remember { mutableStateOf("") }
    var newConversion by remember { mutableStateOf("") }
    var page by remember { mutableIntStateOf(1) }
    LaunchedEffect(query) { page = 1 }
    val filtered = remember(query, options, kind) {
        val q = query.trim().lowercase()
        val base = options.filter { it.kind.equals(kind, ignoreCase = true) }
        if (q.isEmpty()) base else base.filter {
            it.name.lowercase().contains(q) || (it.subtitle?.lowercase()?.contains(q) == true)
        }
    }
    val exactMatch = remember(query, filtered) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) null else filtered.firstOrNull { it.name.equals(q, ignoreCase = true) }
    }
    val visible = if (pageSize == null || pageSize <= 0) filtered else filtered.take(page * pageSize)

    // In-sheet Dialog overlay (matches Group's TripSheetFormComponents / MemoryMedia picker).
    // Previously this was a second ModalBottomSheet stacked on the capture sheet, which added a
    // duplicate scrim + drag handle and could pop the parent when dismissed.
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val shape = RoundedCornerShape(20.dp)
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .shadow(24.dp, shape, ambientColor = accent.copy(alpha = 0.4f), spotColor = accent.copy(alpha = 0.4f))
                .clip(shape)
                .background(BusinessSheetTokens.bg)
                .border(1.dp, BusinessSheetTokens.border, shape)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title,
                    color = BusinessSheetTokens.text,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BusinessSheetTokens.field)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("×", color = BusinessSheetTokens.muted, fontFamily = PlusJakartaSans, fontSize = 16.sp)
                }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = BusinessSheetTokens.text, fontFamily = PlusJakartaSans, fontSize = 14.sp),
                placeholder = {
                    Text(
                        "Search ${title.lowercase()}",
                        color = BusinessSheetTokens.muted,
                        fontFamily = PlusJakartaSans,
                        fontSize = 14.sp,
                    )
                },
                singleLine = true,
            )
            if (filtered.isEmpty()) {
                Text(
                    "No matching ${title.lowercase()}",
                    color = BusinessSheetTokens.muted,
                    fontFamily = PlusJakartaSans,
                    fontSize = 13.sp,
                )
                if (canAdd) {
                    TextButton(onClick = {
                        newName = query
                        adding = true
                    }) {
                        Text("+ Add $title", color = accent, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Text(
                        "Ask an admin to add this.",
                        color = BusinessSheetTokens.muted,
                        fontFamily = PlusJakartaSans,
                        fontSize = 12.sp,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(visible, key = { index, _ -> index }) { _, option ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BusinessSheetTokens.field, RoundedCornerShape(12.dp))
                                .clickable {
                                    onSelect(option)
                                    onDismiss()
                                }
                                .padding(12.dp),
                        ) {
                            Text(
                                option.name,
                                color = BusinessSheetTokens.text,
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                            )
                            option.subtitle?.takeIf { it.isNotBlank() }?.let {
                                Text(it, color = BusinessSheetTokens.muted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
                            }
                        }
                    }
                    if (visible.size < filtered.size) {
                        item {
                            TextButton(onClick = { page += 1 }) {
                                Text("Show more", color = accent, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
            if (canAdd && !adding) {
                TextButton(onClick = {
                    newName = query
                    adding = true
                }) {
                    Text("+ Add $title", color = accent, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold)
                }
            }
            if (adding) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BusinessSheetTokens.field, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "Add $title",
                        color = BusinessSheetTokens.text,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                    )
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(color = BusinessSheetTokens.text, fontFamily = PlusJakartaSans, fontSize = 14.sp),
                        placeholder = { Text("Name", color = BusinessSheetTokens.muted, fontFamily = PlusJakartaSans) },
                        singleLine = true,
                    )
                    if (kind.equals("PRODUCT", ignoreCase = true) && unitChoices.isNotEmpty()) {
                        Text("Base unit", color = BusinessSheetTokens.muted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
                        unitChoices.chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                row.forEach { option ->
                                    Text(
                                        option.subtitle ?: option.name,
                                        color = if (newUnit == option.name) BusinessSheetTokens.bg else BusinessSheetTokens.text,
                                        fontFamily = PlusJakartaSans,
                                        fontSize = 12.sp,
                                        modifier = Modifier
                                            .background(
                                                if (newUnit == option.name) accent else BusinessSheetTokens.field,
                                                RoundedCornerShape(8.dp),
                                            )
                                            .clickable { newUnit = option.name }
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                    )
                                }
                            }
                        }
                    } else if (kind.equals("PRODUCT", ignoreCase = true) || kind.equals("UOM", ignoreCase = true)) {
                        OutlinedTextField(
                            value = newUnit,
                            onValueChange = { newUnit = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(color = BusinessSheetTokens.text, fontFamily = PlusJakartaSans, fontSize = 14.sp),
                            placeholder = {
                                Text(
                                    if (kind.equals("UOM", ignoreCase = true)) "Code / unit (required)"
                                    else "Base UOM (required)",
                                    color = BusinessSheetTokens.muted,
                                    fontFamily = PlusJakartaSans,
                                )
                            },
                            singleLine = true,
                        )
                    }
                    if (kind.equals("PRODUCT", ignoreCase = true)) {
                        OutlinedTextField(
                            value = newConversion,
                            onValueChange = { newConversion = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(color = BusinessSheetTokens.text, fontFamily = PlusJakartaSans, fontSize = 14.sp),
                            placeholder = {
                                Text(
                                    "Units per pack/box (optional)",
                                    color = BusinessSheetTokens.muted,
                                    fontFamily = PlusJakartaSans,
                                )
                            },
                            singleLine = true,
                        )
                    }
                    exactMatch?.let { match ->
                        Text(
                            "Matching existing: ${match.name}. Choose existing or confirm a distinct name.",
                            color = Color(0xFFF0C674),
                            fontFamily = PlusJakartaSans,
                            fontSize = 11.sp,
                        )
                        TextButton(onClick = {
                            onSelect(match)
                            onDismiss()
                        }) {
                            Text("Use existing", color = accent, fontFamily = PlusJakartaSans)
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { adding = false }) {
                            Text("Cancel", color = BusinessSheetTokens.muted, fontFamily = PlusJakartaSans)
                        }
                        TextButton(onClick = {
                            val name = newName.trim()
                            val unitValue = newUnit.trim()
                            if (name.isEmpty()) return@TextButton
                            if ((kind.equals("PRODUCT", ignoreCase = true) || kind.equals("UOM", ignoreCase = true)) &&
                                unitValue.isEmpty()
                            ) {
                                return@TextButton
                            }
                            val match = exactMatch
                            if (match != null) {
                                onSelect(match)
                            } else {
                                onAdd?.invoke(name, unitValue.ifEmpty { null }, newConversion.trim().ifEmpty { null })
                            }
                            onDismiss()
                        }) {
                            Text("Save", color = accent, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
