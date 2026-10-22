package com.example.momentra.ui.shell.business.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class OperatingLovOption(
    val id: String,
    val name: String,
    val kind: String,
    val unit: String? = null,
    val subtitle: String? = null,
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchableOperatingLovSheet(
    title: String,
    kind: String,
    options: List<OperatingLovOption>,
    canAdd: Boolean,
    accent: Color = Color(0xFF6DDCFF),
    onSelect: (OperatingLovOption) -> Unit,
    /** name, base unit, optional conversionToBase */
    onAdd: ((String, String?, String?) -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    var adding by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newUnit by remember { mutableStateOf("") }
    var newConversion by remember { mutableStateOf("") }
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0E1620),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, color = Color(0xFFE8EEF7), fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search ${title.lowercase()}") },
                singleLine = true,
            )
            if (filtered.isEmpty()) {
                Text("No matching ${title.lowercase()}", color = Color(0xFF9AA3B2), fontSize = 13.sp)
                if (canAdd) {
                    TextButton(onClick = {
                        newName = query
                        adding = true
                    }) {
                        Text("+ Add $title", color = accent, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Text("Ask an admin to add this.", color = Color(0xFF9AA3B2), fontSize = 12.sp)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filtered, key = { it.id }) { option ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF15202B), RoundedCornerShape(12.dp))
                                .clickable {
                                    onSelect(option)
                                    onDismiss()
                                }
                                .padding(12.dp),
                        ) {
                            Text(option.name, color = Color(0xFFE8EEF7), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            option.subtitle?.takeIf { it.isNotBlank() }?.let {
                                Text(it, color = Color(0xFF9AA3B2), fontSize = 12.sp)
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
                    Text("+ Add $title", color = accent, fontWeight = FontWeight.SemiBold)
                }
            }
            if (adding) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF15202B), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Add $title", color = Color(0xFFE8EEF7), fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Name") },
                        singleLine = true,
                    )
                    if (kind.equals("PRODUCT", ignoreCase = true) || kind.equals("UOM", ignoreCase = true)) {
                        OutlinedTextField(
                            value = newUnit,
                            onValueChange = { newUnit = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(
                                    if (kind.equals("UOM", ignoreCase = true)) "Code / unit (required)"
                                    else "Base UOM (required)",
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
                            placeholder = { Text("Units per pack/box (optional)") },
                            singleLine = true,
                        )
                    }
                    exactMatch?.let { match ->
                        Text(
                            "Matching existing: ${match.name}. Choose existing or confirm a distinct name.",
                            color = Color(0xFFF0C674),
                            fontSize = 11.sp,
                        )
                        TextButton(onClick = {
                            onSelect(match)
                            onDismiss()
                        }) {
                            Text("Use existing", color = accent)
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { adding = false }) { Text("Cancel") }
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
                            Text("Save", color = accent, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
