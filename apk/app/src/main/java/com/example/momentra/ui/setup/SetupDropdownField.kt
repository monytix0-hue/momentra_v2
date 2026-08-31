package com.example.momentra.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.ui.shell.maestro.MaestroIds

@Composable
fun SetupDropdownField(
    label: String,
    hint: String? = null,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = SetupTokens.BrandPrimary,
    borderColor: Color = Color(0xFF1E293B),
    cardColor: Color = SetupTokens.BizCard,
    testTag: String = MaestroIds.SETUP_DROPDOWN,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag(testTag),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = SetupTokens.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            if (hint != null) {
                Text(hint, color = SetupTokens.TextSecondary, fontSize = 12.sp)
            }
        }
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(cardColor)
                    .border(1.dp, if (expanded) accent else borderColor, RoundedCornerShape(12.dp))
                    .clickable { expanded = true }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .testTag("$testTag.trigger"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = value.ifBlank { "Select" },
                    color = if (value.isBlank()) SetupTokens.TextSecondary else Color.White,
                    fontSize = 15.sp,
                )
                Text("▼", color = SetupTokens.TextSecondary, fontSize = 12.sp)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .background(SetupTokens.BizCard)
                    .testTag("$testTag.menu"),
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                option,
                                color = if (option == value) accent else Color.White,
                                fontWeight = if (option == value) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}
