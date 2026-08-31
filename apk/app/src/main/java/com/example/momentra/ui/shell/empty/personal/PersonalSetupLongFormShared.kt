package com.example.momentra.ui.shell.empty.personal

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.ui.setup.SetupDropdownField
import com.example.momentra.ui.shell.maestro.MaestroIds
import com.example.momentra.ui.theme.PlusJakartaSans

object PersonalSetupLongFormTokens {
    val Bg = Color(0xFF14121B)
    val Card = Color(0xFF161B26)
    val Text = Color(0xFFE5E0EE)
    val Secondary = Color(0xFFC9C4D8)
    val Border = Color(0xFF1E293B)
    val SurfaceDeep = Color(0xFF0F172A)
    val Purple = Color(0xFF7C5CFC)
    val Teal = Color(0xFF10B981)
    val Blue = Color(0xFF3B82F6)
    val Indigo = Color(0xFF818CF8)
    val Amber = Color(0xFFF59E0B)
    val Orange = Color(0xFFFF7A3D)
    val Pink = Color(0xFFE12A9E)
    val Green = Color(0xFF10B981)
}

@Composable
fun PersonalSetupCloseRow(
    onBack: () -> Unit,
    enabled: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(enabled = enabled, onClick = onBack)
                .padding(vertical = 4.dp, horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("×", color = PersonalSetupLongFormTokens.Secondary, fontSize = 16.sp)
            Text(
                "Close",
                color = PersonalSetupLongFormTokens.Secondary,
                fontSize = 14.sp,
                fontFamily = PlusJakartaSans,
            )
        }
        Text(
            "PERSONAL MODE",
            color = PersonalSetupLongFormTokens.Secondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
        )
    }
}

@Composable
fun PersonalSetupHero(
    emoji: String,
    title: String,
    subtitle: String,
    accent: Color,
    @DrawableRes heroIcon: Int? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp, bottom = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        // Figma glow-container: ~280dp radial glow behind 112dp icon circle
        Box(
            modifier = Modifier
                .size(280.dp)
                .drawBehind {
                    val radius = size.minDimension * 0.48f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accent.copy(alpha = 0.40f),
                                accent.copy(alpha = 0.18f),
                                accent.copy(alpha = 0.06f),
                                Color.Transparent,
                            ),
                            center = center,
                            radius = radius,
                        ),
                        radius = radius,
                        center = center,
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(RoundedCornerShape(56.dp))
                    .background(PersonalSetupLongFormTokens.Card)
                    .border(1.dp, accent.copy(alpha = 0.2f), RoundedCornerShape(56.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (heroIcon != null) {
                    Image(
                        painter = painterResource(heroIcon),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        colorFilter = ColorFilter.tint(accent),
                    )
                } else {
                    Text(emoji, fontSize = 36.sp)
                }
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                title,
                color = PersonalSetupLongFormTokens.Text,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
                textAlign = TextAlign.Center,
            )
            Text(
                subtitle,
                color = PersonalSetupLongFormTokens.Secondary,
                fontSize = 14.sp,
                fontFamily = PlusJakartaSans,
                textAlign = TextAlign.Center,
                lineHeight = 21.sp,
            )
        }
    }
}

@Composable
fun PersonalSetupSectionCard(
    number: String,
    title: String,
    accent: Color,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // Shape via background/border only — avoid clip so title glow isn't cut off
            .background(PersonalSetupLongFormTokens.Card, RoundedCornerShape(16.dp))
            .border(1.dp, PersonalSetupLongFormTokens.Border, RoundedCornerShape(16.dp))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // Figma: watermark "01" + title side-by-side (gap 12), title has purple glow — not overlay
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                number,
                color = accent.copy(alpha = 0.08f),
                fontSize = 64.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSans,
            )
            Text(
                title.uppercase(),
                color = accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
                style = TextStyle(
                    shadow = Shadow(
                        color = accent.copy(alpha = 0.95f),
                        offset = Offset.Zero,
                        blurRadius = 18f,
                    ),
                ),
            )
        }
        content()
    }
}

@Composable
fun PersonalSetupBorderedGroup(
    title: String,
    border: Color,
    glyph: String = "●",
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PersonalSetupLongFormTokens.SurfaceDeep)
            .border(1.dp, border, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(border),
                contentAlignment = Alignment.Center,
            ) {
                Text(glyph, color = Color.White, fontSize = 12.sp)
            }
            Text(
                title,
                color = PersonalSetupLongFormTokens.Text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
        }
        content()
    }
}

@Composable
fun PersonalSetupCategoryTabs(
    tabs: List<Pair<String, Color>>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PersonalSetupLongFormTokens.Card)
            .border(1.dp, PersonalSetupLongFormTokens.Border, RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tabs.forEach { (label, accent) ->
            val isSelected = selected == label
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) accent else Color.Transparent)
                    .clickable { onSelect(label) }
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (isSelected) Color.White else PersonalSetupLongFormTokens.Secondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * Live status for setup footers.
 * @return (sectionsConfigured, answersSaved)
 */
fun setupStatusCounts(
    defaults: Map<String, Any>,
    selections: Map<String, Any>,
    sectionKeys: List<Set<String>>,
): Pair<Int, Int> {
    fun isAnswered(key: String): Boolean {
        val value = selections[key] ?: return false
        val default = defaults[key]
        if (value != default) return true
        return when (value) {
            is String -> value.isNotBlank()
            is Collection<*> -> value.isNotEmpty()
            is Boolean -> true
            else -> value.toString().isNotBlank()
        }
    }

    val answerKeys = (defaults.keys + selections.keys)
    val answersSaved = answerKeys.count { isAnswered(it) }
    val sectionsConfigured = sectionKeys.count { keys -> keys.any { isAnswered(it) } }
    return sectionsConfigured to answersSaved
}

@Composable
fun PersonalSetupFieldLabel(text: String, small: Boolean = false) {
    Text(
        text,
        color = PersonalSetupLongFormTokens.Secondary,
        fontSize = if (small) 10.sp else 12.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = PlusJakartaSans,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
fun PersonalSetupInlineDropdown(
    label: String,
    hint: String,
    key: String,
    options: List<String>,
    selections: SnapshotStateMap<String, Any>,
    accent: Color,
    border: Color = PersonalSetupLongFormTokens.Border,
) {
    SetupDropdownField(
        label = label,
        hint = hint,
        value = selections[key]?.toString().orEmpty(),
        options = options,
        onValueChange = { selections[key] = it },
        accent = accent,
        borderColor = border,
        cardColor = PersonalSetupLongFormTokens.Card,
        testTag = MaestroIds.setupDropdown(key),
    )
}

@Composable
fun PersonalSetupMultiSelect(
    label: String,
    hint: String,
    key: String,
    options: List<String>,
    selections: SnapshotStateMap<String, Any>,
    accent: Color,
) {
    val selected = remember(selections[key]) {
        when (val v = selections[key]) {
            is List<*> -> v.filterIsInstance<String>().toSet()
            is String -> setOf(v)
            else -> emptySet()
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = PersonalSetupLongFormTokens.Secondary, fontSize = 14.sp, fontFamily = PlusJakartaSans)
            Text(hint, color = PersonalSetupLongFormTokens.Secondary, fontSize = 12.sp, fontFamily = PlusJakartaSans)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Wrap via nested columns for wrap-like behavior without FlowRow dependency issues
        }
        options.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { option ->
                    val on = option in selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(PersonalSetupLongFormTokens.Card)
                            .border(
                                1.dp,
                                if (on) accent else PersonalSetupLongFormTokens.Border,
                                RoundedCornerShape(20.dp),
                            )
                            .clickable {
                                val next = selected.toMutableSet()
                                if (on) next.remove(option) else next.add(option)
                                selections[key] = next.toList()
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Text(
                            option,
                            color = PersonalSetupLongFormTokens.Text,
                            fontSize = 13.sp,
                            fontFamily = PlusJakartaSans,
                        )
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun PersonalSetupSummaryRow(label: String, value: String, big: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = PersonalSetupLongFormTokens.Secondary, fontSize = 14.sp, fontFamily = PlusJakartaSans)
        Text(
            value,
            color = PersonalSetupLongFormTokens.Text,
            fontSize = if (big) 20.sp else 16.sp,
            fontWeight = if (big) FontWeight.Bold else FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
        )
    }
}

@Composable
fun PersonalSetupActivateFooter(
    statusLine: String,
    readyLine: String,
    ctaLabel: String,
    footerTagline: String,
    ctaBrush: Brush,
    submitting: Boolean,
    error: String?,
    onActivate: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            statusLine,
            color = PersonalSetupLongFormTokens.Secondary,
            fontSize = 14.sp,
            fontFamily = PlusJakartaSans,
            textAlign = TextAlign.Center,
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(PersonalSetupLongFormTokens.Green.copy(alpha = 0.08f))
                .border(1.dp, PersonalSetupLongFormTokens.Green.copy(alpha = 0.2f), RoundedCornerShape(999.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("✓", color = PersonalSetupLongFormTokens.Green, fontSize = 12.sp)
            Text(
                readyLine,
                color = PersonalSetupLongFormTokens.Secondary,
                fontSize = 14.sp,
                fontFamily = PlusJakartaSans,
            )
        }
        error?.let {
            Text(it, color = Color(0xFFFF8A80), fontSize = 13.sp, fontFamily = PlusJakartaSans)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(ctaBrush)
                .clickable(enabled = !submitting, onClick = onActivate),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (submitting) "Activating…" else ctaLabel,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
        }
        Text(
            footerTagline,
            color = PersonalSetupLongFormTokens.Secondary,
            fontSize = 11.sp,
            fontFamily = PlusJakartaSans,
        )
        Spacer(Modifier.height(16.dp))
    }
}

fun SnapshotStateMap<String, Any>.putCatalogDefaults(defaults: Map<String, Any>) {
    clear()
    putAll(defaults)
}

fun selectionList(selections: SnapshotStateMap<String, Any>, key: String): List<String> =
    when (val v = selections[key]) {
        is List<*> -> v.filterIsInstance<String>()
        is String -> listOf(v)
        else -> emptyList()
    }

fun selectionString(selections: SnapshotStateMap<String, Any>, key: String): String =
    selections[key]?.toString().orEmpty()

fun cycleSelection(selections: SnapshotStateMap<String, Any>, key: String, options: List<String>) {
    val cur = selectionString(selections, key)
    val idx = options.indexOf(cur).let { if (it < 0) 0 else (it + 1) % options.size }
    selections[key] = options[idx]
}

@Composable
fun PersonalSetupStageHeader(
    label: String,
    key: String,
    options: List<String>,
    selections: SnapshotStateMap<String, Any>,
    accent: Color = PersonalSetupLongFormTokens.Purple,
) {
    SetupDropdownField(
        label = label,
        value = selectionString(selections, key),
        options = options,
        onValueChange = { selections[key] = it },
        accent = accent,
        cardColor = PersonalSetupLongFormTokens.Card,
        testTag = MaestroIds.setupDropdown(key),
    )
}

@Composable
fun PersonalSetupDualPills(
    label: String,
    key: String,
    options: List<String>,
    selections: SnapshotStateMap<String, Any>,
    accent: Color,
) {
    SetupDropdownField(
        label = label,
        value = selectionString(selections, key),
        options = options,
        onValueChange = { selections[key] = it },
        accent = accent,
        cardColor = PersonalSetupLongFormTokens.Card,
        testTag = MaestroIds.setupDropdown(key),
    )
}

@Composable
fun PersonalSetupAddRow(label: String, accent: Color, onClick: () -> Unit = {}) {
    Text(
        label,
        color = accent,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = PlusJakartaSans,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
    )
}

@Composable
fun PersonalSetupDiamondDivider(accent: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(PersonalSetupLongFormTokens.Border),
        )
        Spacer(
            Modifier
                .size(8.dp)
                .background(accent),
        )
        Spacer(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(PersonalSetupLongFormTokens.Border),
        )
    }
}

@Composable
fun PersonalSetupSummaryProgress(accent: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF1E293B)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.62f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(accent),
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("TODAY", color = PersonalSetupLongFormTokens.Secondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
            Text("FORWARD", color = PersonalSetupLongFormTokens.Secondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
        }
    }
}

@Composable
fun PersonalSetupToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accent: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = PersonalSetupLongFormTokens.Secondary, fontSize = 14.sp, fontFamily = PlusJakartaSans)
            Text(subtitle, color = PersonalSetupLongFormTokens.Secondary, fontSize = 12.sp, fontFamily = PlusJakartaSans)
        }
        androidx.compose.material3.Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accent,
            ),
        )
    }
}
