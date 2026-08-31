package com.example.momentra.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class SetupChipOption(
    val value: String,
    val label: String,
    val emoji: String? = null,
)

@Composable
fun SetupChipGrid(
    options: List<SetupChipOption>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    columns: Int = 2,
    selectedColor: Color = SetupTokens.ChipSelected,
    unselectedColor: Color = SetupTokens.ChipUnselected,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.chunked(columns).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { option ->
                    SetupChipCell(
                        option = option,
                        isSelected = option.value in selected,
                        onToggle = onToggle,
                        selectedColor = selectedColor,
                        unselectedColor = unselectedColor,
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(columns - row.size) {
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SetupChipCell(
    option: SetupChipOption,
    isSelected: Boolean,
    onToggle: (String) -> Unit,
    selectedColor: Color,
    unselectedColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) selectedColor else unselectedColor)
            .border(
                width = 1.dp,
                color = if (isSelected) selectedColor else SetupTokens.BorderSubtle,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable { onToggle(option.value) }
            .padding(horizontal = 12.dp, vertical = 14.dp)
            .semantics {
                role = Role.Checkbox
                contentDescription = "${option.label}, ${if (isSelected) "selected" else "not selected"}"
            },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (option.emoji != null) {
            Text(option.emoji, fontSize = 18.sp)
        }
        Text(
            option.label,
            color = SetupTokens.TextPrimary,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
fun SetupChipRow(
    options: List<SetupChipOption>,
    selected: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            val isSelected = option.value == selected
            Text(
                text = buildString {
                    if (option.emoji != null) append("${option.emoji} ")
                    append(option.label)
                },
                color = SetupTokens.TextPrimary,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) SetupTokens.ChipSelected else SetupTokens.ChipUnselected)
                    .border(
                        1.dp,
                        if (isSelected) SetupTokens.ChipSelected else SetupTokens.BorderSubtle,
                        RoundedCornerShape(12.dp),
                    )
                    .clickable { onSelect(option.value) }
                    .padding(vertical = 12.dp, horizontal = 8.dp)
                    .semantics {
                        role = Role.RadioButton
                        contentDescription = option.label
                    },
            )
        }
    }
}
