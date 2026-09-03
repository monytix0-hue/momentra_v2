package com.example.momentra.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.ui.shell.empty.BusinessFieldKind
import com.example.momentra.ui.shell.empty.BusinessSetupFieldSpec
import com.example.momentra.ui.shell.maestro.MaestroIds

@Composable
fun SetupPrefField(
    field: BusinessSetupFieldSpec,
    selections: MutableMap<String, Any>,
    modifier: Modifier = Modifier,
    selectedChipColor: Color = SetupTokens.BizAccent,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (field.kind) {
            BusinessFieldKind.CHIPS, BusinessFieldKind.DROPDOWN -> {
                SetupDropdownField(
                    label = field.label,
                    value = selections[field.key]?.toString().orEmpty(),
                    options = field.options,
                    onValueChange = { selections[field.key] = it },
                    accent = selectedChipColor,
                    testTag = MaestroIds.setupDropdown(field.key),
                )
            }
            else -> {
                Text(field.label, color = Color(0xFFE2E8F0), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
        when (field.kind) {
            BusinessFieldKind.CHIPS, BusinessFieldKind.DROPDOWN -> Unit
            BusinessFieldKind.TEXT, BusinessFieldKind.TOGGLE, BusinessFieldKind.DATE,
            BusinessFieldKind.DATETIME, BusinessFieldKind.TIME -> when (field.kind) {
            BusinessFieldKind.TEXT -> {
                BasicTextField(
                    value = selections[field.key]?.toString().orEmpty(),
                    onValueChange = { selections[field.key] = it },
                    textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                    cursorBrush = SolidColor(selectedChipColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                        .testTag("setup.text.${field.key}"),
                )
            }
            BusinessFieldKind.TOGGLE -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Enabled", color = SetupTokens.TextSecondary, fontSize = 13.sp)
                    Switch(
                        checked = selections[field.key] as? Boolean ?: false,
                        onCheckedChange = { selections[field.key] = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = selectedChipColor),
                    )
                }
            }
            BusinessFieldKind.DATE -> {
                SetupDateField(
                    label = field.label,
                    isoValue = selections[field.key]?.toString(),
                    onIsoChange = { selections[field.key] = it },
                    testTag = MaestroIds.setupDate(field.key),
                )
            }
            BusinessFieldKind.DATETIME -> {
                SetupDateTimeField(
                    label = field.label,
                    isoValue = selections[field.key]?.toString(),
                    onIsoChange = { selections[field.key] = it },
                    testTag = MaestroIds.setupDateTime(field.key),
                )
            }
            BusinessFieldKind.TIME -> {
                val iso = selections[field.key]?.toString()
                SetupDateTimeField(
                    label = field.label,
                    isoValue = iso?.let { "1970-01-01T$it" },
                    onIsoChange = { value ->
                        selections[field.key] = value.substringAfter('T', value)
                    },
                    testTag = MaestroIds.setupTime(field.key),
                )
            }
            else -> Unit
            }
        }
    }
}
