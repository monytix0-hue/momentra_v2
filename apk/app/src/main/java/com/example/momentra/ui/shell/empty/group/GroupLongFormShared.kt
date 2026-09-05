package com.example.momentra.ui.shell.empty.group

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.momentra.ui.shell.maestro.MaestroIds
import androidx.compose.ui.platform.testTag
import com.example.momentra.ui.theme.PlusJakartaSans

internal const val GROUP_LOCAL_ONLY_NOTE =
    "Synced with your group — budget and reminders save on activate"

/** Compact Figma type strip (EXPERIENCE SETUPS / PURCHASE SETUPS / …). */
@Composable
fun GroupLongFormTypeChipStrip(
    title: String,
    types: List<GroupTypeOption>,
    selectedCode: String,
    onSelect: (GroupTypeOption) -> Unit,
    shortLabel: (GroupTypeOption) -> String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF161B26))
            .border(1.dp, Color(0xBF3A3842), RoundedCornerShape(18.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            title.uppercase(),
            color = Color(0xFF938EA1),
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = PlusJakartaSans,
            letterSpacing = 0.7.sp,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            types.forEach { opt ->
                val selected = opt.code == selectedCode
                val accent = opt.palette.accent
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) accent.copy(alpha = 0.16f) else Color(0xFF1E293B))
                        .border(
                            1.dp,
                            if (selected) accent.copy(alpha = 0.6f) else Color(0xCC3A3842),
                            RoundedCornerShape(14.dp),
                        )
                        .clickable { onSelect(opt) }
                        .padding(vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Image(
                        painterResource(opt.iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        colorFilter = ColorFilter.tint(if (selected) accent else GroupSetupTheme.TextSecondary),
                    )
                    Text(
                        shortLabel(opt),
                        color = if (selected) accent else GroupSetupTheme.TextSecondary,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = PlusJakartaSans,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
fun GroupLongFormDiamondDivider(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF3A3842), thickness = 1.dp)
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(Color(0xFFC9BFFF)),
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF3A3842), thickness = 1.dp)
    }
}

@Composable
fun GroupLongFormSectionCard(
    step: String,
    title: String,
    accent: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF3A3842))
            .border(1.dp, Color(0xFF938EA1).copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box {
                Text(
                    step,
                    color = accent.copy(alpha = 0.06f),
                    fontSize = 64.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = PlusJakartaSans,
                )
            }
            Text(
                title.uppercase(),
                color = accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
        }
        content()
    }
}

@Composable
fun GroupLongFormSubsectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        color = GroupSetupTheme.TextSecondary,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = PlusJakartaSans,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun GroupLongFormGroupTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        color = Color(0xFFE5E0EE),
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = PlusJakartaSans,
        modifier = modifier.fillMaxWidth(),
    )
}

/** Quick picks for shared-experience destination — user can also type any custom value. */
val groupDestinationSuggestions = listOf("Goa, India", "Jaipur", "Manali", "Singapore")

/** Label + hint + free-text destination with optional suggestion chips. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GroupLongFormDestinationField(
    label: String,
    hint: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "City, country or venue",
    suggestions: List<String> = groupDestinationSuggestions,
    modifier: Modifier = Modifier,
    testTag: String = MaestroIds.setupField("destination"),
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            label,
            color = GroupSetupTheme.TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = PlusJakartaSans,
        )
        Text(
            hint,
            color = GroupSetupTheme.TextSecondary.copy(alpha = 0.85f),
            fontSize = 12.sp,
            fontFamily = PlusJakartaSans,
        )
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = { if (it.length <= 200) onValueChange(it) },
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = Color(0xFFE5E0EE),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFFC9BFFF)),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF161B26))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .testTag(testTag),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        color = GroupSetupTheme.TextSecondary.copy(alpha = 0.7f),
                        fontSize = 16.sp,
                        fontFamily = PlusJakartaSans,
                    )
                }
                inner()
            },
        )
        if (suggestions.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                suggestions.forEach { suggestion ->
                    val selected = value.equals(suggestion, ignoreCase = true)
                    Text(
                        suggestion,
                        color = if (selected) Color(0xFFC9BFFF) else GroupSetupTheme.TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        fontFamily = PlusJakartaSans,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                if (selected) Color(0xFF2D2640) else Color(0xFF161B26),
                            )
                            .border(
                                1.dp,
                                if (selected) Color(0xFF7C5CFC) else Color(0xFF1E293B),
                                RoundedCornerShape(999.dp),
                            )
                            .clickable { onValueChange(suggestion) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("$testTag.suggestion.${suggestion.hashCode()}"),
                    )
                }
            }
        }
    }
}

/** Label + hint + trailing dropdown pill (Figma form-row). */
@Composable
fun GroupLongFormPrefRow(
    label: String,
    hint: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    editableGlyph: Boolean = false,
    testTag: String = MaestroIds.SETUP_DROPDOWN,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                label,
                color = GroupSetupTheme.TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = PlusJakartaSans,
            )
            Text(
                hint,
                color = GroupSetupTheme.TextSecondary.copy(alpha = 0.85f),
                fontSize = 12.sp,
                fontFamily = PlusJakartaSans,
            )
        }
        Spacer(Modifier.width(8.dp))
        Box {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF161B26))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                    .clickable { expanded = true }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("$testTag.trigger"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    value,
                    color = Color(0xFFE5E0EE),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (editableGlyph) "✎" else "▼",
                    color = GroupSetupTheme.TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.testTag("$testTag.menu"),
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                option,
                                color = if (option == value) Color(0xFFC9BFFF) else Color.White,
                                fontWeight = if (option == value) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = PlusJakartaSans,
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

/** Custom budget amount field shown when Budget preset is "Custom…". */
@Composable
fun GroupBudgetCustomField(
    value: String,
    onValueChange: (String) -> Unit,
    currencyCode: String,
    modifier: Modifier = Modifier,
    testTag: String = MaestroIds.setupField("budgetCustom"),
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            "Custom amount ($currencyCode)",
            color = GroupSetupTheme.TextSecondary,
            fontSize = 12.sp,
            fontFamily = PlusJakartaSans,
        )
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = { onValueChange(GroupBudgetUtils.formatCustomAmountInput(it)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = androidx.compose.ui.text.TextStyle(
                color = Color(0xFFE5E0EE),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF161B26))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .testTag(testTag),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        "₹84,000",
                        color = GroupSetupTheme.TextSecondary.copy(alpha = 0.6f),
                        fontSize = 16.sp,
                        fontFamily = PlusJakartaSans,
                    )
                }
                inner()
            },
        )
    }
}

@Composable
fun GroupLongFormToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                title,
                color = GroupSetupTheme.TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = PlusJakartaSans,
            )
            Text(
                subtitle,
                color = GroupSetupTheme.TextSecondary.copy(alpha = 0.85f),
                fontSize = 12.sp,
                fontFamily = PlusJakartaSans,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accent,
                uncheckedThumbColor = GroupSetupTheme.TextSecondary,
                uncheckedTrackColor = GroupSetupTheme.Border,
            ),
        )
    }
}

@Composable
fun GroupLongFormNamePills(
    primary: String,
    secondary: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(primary to true, secondary to false).forEach { (text, _) ->
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF161B26))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(accent),
                )
                Text(
                    text,
                    color = Color(0xFFE5E0EE),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = PlusJakartaSans,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text("▼", color = GroupSetupTheme.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun GroupLongFormLocalOnlyNote(modifier: Modifier = Modifier) {
    Text(
        GROUP_LOCAL_ONLY_NOTE,
        color = GroupSetupTheme.TextSecondary,
        fontSize = 11.sp,
        fontFamily = PlusJakartaSans,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun GroupLongFormReadyBanner(
    message: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFF10B981).copy(alpha = 0.15f))
            .border(1.dp, Color(0xFF10B981).copy(alpha = 0.35f), RoundedCornerShape(999.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("✓", color = Color(0xFF10B981), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(
            message,
            color = Color(0xFFE5E0EE),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
        )
    }
}

internal fun cycleOption(current: String, options: List<String>): String {
    if (options.isEmpty()) return current
    val idx = options.indexOf(current).let { if (it < 0) 0 else it }
    return options[(idx + 1) % options.size]
}

internal fun experienceChipLabel(opt: GroupTypeOption): String = when (opt.code) {
    "TRIP" -> "Trip"
    "WEDDING" -> "Wedding"
    "HOUSE_PARTY" -> "Celebration"
    "OFFICE_OUTING" -> "Office"
    else -> opt.label.take(8)
}

internal fun purchaseChipLabel(opt: GroupTypeOption): String = when (opt.code) {
    "GIFT_POOL" -> "Gift"
    "GROUP_PURCHASE" -> "Purchase"
    "SHARED_ASSET" -> "Asset"
    "COMMUNITY_PURCHASE", "CUSTOM" -> "Custom"
    else -> opt.label.take(8)
}

internal fun livingChipLabel(opt: GroupTypeOption): String = when (opt.code) {
    "FLATMATES" -> "Flatmates"
    "FAMILY_HOUSEHOLD" -> "Family"
    "CO_LIVING" -> "Co-living"
    "CUSTOM" -> "Custom"
    else -> opt.label.take(8)
}
