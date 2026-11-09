package com.example.momentra.ui.shell.empty.business

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.ui.theme.PlusJakartaSans

/**
 * The setup document's own palette. Both frames share one structure and differ only in
 * accent, so every colour the document paints is derived from the subtype here rather than
 * from a hardcoded palette.
 */
internal data class SetupDocTheme(
    val accent: Color,
    val canvas: Color,
    val card: Color,
    val inset: Color,
    val border: Color,
    val panelStroke: Color,
    val ink: Color,
    val muted: Color,
    val faint: Color,
    val positive: Color,
    val negative: Color,
)

/** `TRD-SETUP-v6` tokens — emerald on the trading canvas (`2566:32923`). */
internal val TradingSetupDocTheme = SetupDocTheme(
    accent = Color(0xFF10B981),
    canvas = Color(0xFF090D16),
    card = Color(0xFF151D2E),
    inset = Color(0xFF090D16),
    border = Color(0xFF1E293B),
    panelStroke = Color(0xFF334155),
    ink = Color(0xFFF8FAFC),
    muted = Color(0xFF94A3B8),
    faint = Color(0xFF64748B),
    positive = Color(0xFF22C55E),
    negative = Color(0xFFF87171),
)

/** `DST-SETUP-v6` tokens — orange on the distribution canvas (aligned to DST-PULSE-v12). */
internal val DistributionSetupDocTheme = SetupDocTheme(
    accent = Color(0xFFEA580C),
    canvas = Color(0xFF0F172A),
    card = Color(0xFF151D2E),
    inset = Color(0xFF0F172A),
    border = Color(0xFF1E293B),
    panelStroke = Color(0xFF334155),
    ink = Color(0xFFF8FAFC),
    muted = Color(0xFF94A3B8),
    faint = Color(0xFF64748B),
    positive = Color(0xFF22C55E),
    negative = Color(0xFFF87171),
)

/** `MFG-SETUP-v6` / `MTO-SETUP` — amber on the manufacturing zinc canvas. */
internal val ManufacturingSetupDocTheme = SetupDocTheme(
    accent = Color(0xFFF59E0B),
    canvas = Color(0xFF18181B),
    card = Color(0xFF27272A),
    inset = Color(0xFF18181B),
    border = Color(0xFF3F3F46),
    panelStroke = Color(0xFF52525B),
    ink = Color(0xFFF8FAFC),
    muted = Color(0xFFA1A1AA),
    faint = Color(0xFF71717A),
    positive = Color(0xFF22C55E),
    negative = Color(0xFFF87171),
)

/** `RTL-SETUP-v6` — rose on the retail canvas (`2510:29750`). */
internal val RetailSetupDocTheme = SetupDocTheme(
    accent = Color(0xFFF43F5E),
    canvas = Color(0xFF0B0A1F),
    card = Color(0xFF2A1A1F),
    inset = Color(0xFF170D11),
    border = Color(0xFF4C0519),
    panelStroke = Color(0xFF881337),
    ink = Color(0xFFF8FAFC),
    muted = Color(0xFF94A3B8),
    faint = Color(0xFF64748B),
    positive = Color(0xFF22C55E),
    negative = Color(0xFFF87171),
)

/** `RST-SETUP-v6` — ember on the restaurant stone canvas. */
internal val RestaurantSetupDocTheme = SetupDocTheme(
    accent = Color(0xFFEA580C),
    canvas = Color(0xFF1C1917),
    card = Color(0xFF292524),
    inset = Color(0xFF1C1917),
    border = Color(0xFF44403C),
    panelStroke = Color(0xFF57534E),
    ink = Color(0xFFFAFAF9),
    muted = Color(0xFFA8A29E),
    faint = Color(0xFF78716C),
    positive = Color(0xFF22C55E),
    negative = Color(0xFFF87171),
)

/** `SVC-SETUP-v6` — teal on the services canvas (`2535:29334`). */
internal val ServiceSetupDocTheme = SetupDocTheme(
    accent = Color(0xFF0D9488),
    canvas = Color(0xFF042F2E),
    card = Color(0xFF134E4A),
    inset = Color(0xFF042F2E),
    border = Color(0xFF115E59),
    panelStroke = Color(0xFF0F766E),
    ink = Color(0xFFF0FDFA),
    muted = Color(0xFF5EEAD4),
    faint = Color(0xFF64748B),
    positive = Color(0xFF22C55E),
    negative = Color(0xFFF87171),
)

internal fun setupDocTheme(subtypeCode: String): SetupDocTheme = when {
    SetupDocumentSpec.isManufacturing(subtypeCode) -> ManufacturingSetupDocTheme
    SetupDocumentSpec.isRestaurant(subtypeCode) -> RestaurantSetupDocTheme
    SetupDocumentSpec.isService(subtypeCode) -> ServiceSetupDocTheme
    SetupDocumentSpec.isRetail(subtypeCode) -> RetailSetupDocTheme
    SetupDocumentSpec.isDistribution(subtypeCode) -> DistributionSetupDocTheme
    else -> TradingSetupDocTheme
}

/**
 * One numbered section: accent `01`, 18sp bold title, 12sp subtitle, then a 20dp-radius card
 * whose 1dp stroke is the accent at 28%. Body content is 16dp padded with 14dp stacking.
 */
@Composable
internal fun SetupSection(
    spec: SetupSectionSpec,
    theme: SetupDocTheme,
    modifier: Modifier = Modifier,
    /** Marketing Figma: soft accent drop `0 8px 28px @ 0.18` under section cards. */
    accentShadow: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier
            .fillMaxWidth()
            .then(
                if (accentShadow) {
                    Modifier.shadow(
                        elevation = 14.dp,
                        shape = shape,
                        ambientColor = theme.accent.copy(alpha = 0.18f),
                        spotColor = theme.accent.copy(alpha = 0.18f),
                    )
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .background(theme.card)
            .border(1.dp, theme.accent.copy(alpha = 0.28f), shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                spec.number,
                color = theme.accent,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            )
            Text(
                spec.title,
                color = theme.ink,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
        if (spec.subtitle.isNotBlank()) {
            Text(
                spec.subtitle,
                color = theme.muted,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
            )
        }
        content()
    }
}

/** A `panel · Title` inset: 14dp radius, 12dp padding, 12dp stacking. */
@Composable
internal fun SetupPanel(
    title: String,
    theme: SetupDocTheme,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(theme.inset)
            .border(1.dp, theme.panelStroke, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            title,
            color = theme.accent,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
        )
        content()
    }
}

/** `Field · label` over an input, with the frame's optional 11sp helper below it. */
@Composable
internal fun SetupField(
    label: String,
    value: String,
    theme: SetupDocTheme,
    modifier: Modifier = Modifier,
    hint: String? = null,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    maxLines: Int = 1,
    onValueChange: (String) -> Unit,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = theme.muted, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Medium, fontSize = 11.sp)
        val empty = value.isBlank()
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(theme.inset)
                .border(1.dp, theme.panelStroke, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            if (empty && !placeholder.isNullOrBlank()) {
                Text(
                    placeholder,
                    color = theme.faint,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    maxLines = maxLines,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = maxLines == 1,
                maxLines = maxLines,
                textStyle = TextStyle(
                    color = theme.ink,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                ),
                cursorBrush = SolidColor(theme.accent),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (!hint.isNullOrBlank()) {
            Text(hint, color = theme.faint, fontFamily = PlusJakartaSans, fontSize = 11.sp)
        }
    }
}

/** A field that opens a picker instead of a keyboard (`Currency`, `Month`, dates). */
@Composable
internal fun SetupPickerField(
    label: String,
    value: String,
    theme: SetupDocTheme,
    modifier: Modifier = Modifier,
    hint: String? = null,
    placeholder: String = "Tap to pick",
    onClick: () -> Unit,
) {
    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label, color = theme.muted, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Medium, fontSize = 11.sp)
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(theme.inset)
                .border(1.dp, theme.panelStroke, RoundedCornerShape(10.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                value.ifBlank { placeholder },
                color = if (value.isBlank()) theme.faint else theme.ink,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text("›", color = theme.muted, fontFamily = PlusJakartaSans, fontSize = 16.sp)
        }
        if (!hint.isNullOrBlank()) {
            Text(hint, color = theme.faint, fontFamily = PlusJakartaSans, fontSize = 11.sp)
        }
    }
}

/** A row of pill chips that wraps; `multi` chips toggle, single-select chips swap. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SetupChipRow(
    options: List<String>,
    selected: Set<String>,
    theme: SetupDocTheme,
    modifier: Modifier = Modifier,
    label: String? = null,
    hint: String? = null,
    onSelect: (String) -> Unit,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!label.isNullOrBlank()) {
            Text(label, color = theme.muted, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Medium, fontSize = 12.sp)
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            options.forEach { option ->
                val on = option in selected
                Text(
                    option,
                    color = if (on) theme.inset else theme.muted,
                    fontFamily = PlusJakartaSans,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (on) theme.accent else theme.inset)
                        .border(
                            width = 1.dp,
                            color = if (on) theme.accent else theme.panelStroke,
                            shape = RoundedCornerShape(999.dp),
                        )
                        .clickable { onSelect(option) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
        if (!hint.isNullOrBlank()) {
            Text(hint, color = theme.faint, fontFamily = PlusJakartaSans, fontSize = 11.sp)
        }
    }
}

/** `why` block — accent-tinted card with a bold title and a muted explanation. */
@Composable
internal fun SetupWhy(
    title: String,
    body: String,
    theme: SetupDocTheme,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(theme.accent.copy(alpha = 0.10f))
            .border(1.dp, theme.accent.copy(alpha = 0.32f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, color = theme.accent, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text(body, color = theme.muted, fontFamily = PlusJakartaSans, fontSize = 11.sp)
    }
}

/** `status` pill — a filled dot plus a one-line verdict. */
@Composable
internal fun SetupStatusPill(
    text: String,
    theme: SetupDocTheme,
    modifier: Modifier = Modifier,
    tone: Color = theme.positive,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(999.dp))
            .background(tone.copy(alpha = 0.14f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("●", color = tone, fontFamily = PlusJakartaSans, fontSize = 9.sp)
        Text(text, color = tone, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}

/** Round avatar initials used by the §02 owner and team rows. */
@Composable
internal fun SetupAvatar(
    initials: String,
    theme: SetupDocTheme,
    size: Int = 36,
    tone: Color = theme.muted,
) {
    Box(
        Modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size / 2).dp))
            .background(theme.inset),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initials,
            color = tone,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = (size / 2 - 1).coerceAtLeast(10).sp,
        )
    }
}

/** A key/value line — `Company   Northstar Trading`. */
@Composable
internal fun SetupKeyValue(
    label: String,
    value: String,
    theme: SetupDocTheme,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            label,
            color = theme.muted,
            fontFamily = PlusJakartaSans,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f),
        )
        Text(
            value,
            color = theme.ink,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A §04 master row: icon tile, title/subtitle, chevron. */
@Composable
internal fun SetupMasterRow(
    glyph: String,
    title: String,
    subtitle: String,
    theme: SetupDocTheme,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(theme.inset)
            .border(1.dp, theme.panelStroke, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(theme.border),
            contentAlignment = Alignment.Center,
        ) {
            Text(glyph, color = theme.ink, fontFamily = PlusJakartaSans, fontSize = 14.sp)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title,
                color = theme.ink,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                color = theme.muted,
                fontFamily = PlusJakartaSans,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text("›", color = theme.muted, fontFamily = PlusJakartaSans, fontSize = 18.sp)
    }
}

/** The document's single filled call to action. */
@Composable
internal fun SetupCta(
    label: String,
    theme: SetupDocTheme,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accentShadow: Boolean = false,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier
            .fillMaxWidth()
            .then(
                if (accentShadow && enabled) {
                    Modifier.shadow(
                        elevation = 14.dp,
                        shape = shape,
                        ambientColor = theme.accent.copy(alpha = 0.18f),
                        spotColor = theme.accent.copy(alpha = 0.18f),
                    )
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .background(if (enabled) theme.accent else theme.accent.copy(alpha = 0.35f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = theme.ink,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
        )
    }
}

/** A muted footnote that closes a section. */
@Composable
internal fun SetupFootnote(body: String, theme: SetupDocTheme, modifier: Modifier = Modifier) {
    if (body.isBlank()) return
    Text(body, color = theme.muted, fontFamily = PlusJakartaSans, fontSize = 12.sp, modifier = modifier)
}

/**
 * A dashed, unfilled action — the `Share QR` / `WhatsApp` pair under §02's people card, copied
 * from the group setup card so a setup invite reads the same in both places.
 */
@Composable
internal fun SetupOutlineAction(
    title: String,
    theme: SetupDocTheme,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    /** A mint in flight: the row shows it rather than greying out with no reason. */
    busy: Boolean = false,
    onClick: () -> Unit,
) {
    val live = enabled && !busy
    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, theme.border, RoundedCornerShape(14.dp))
            .clickable(enabled = live, onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (busy) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = theme.accent,
                strokeWidth = 2.dp,
            )
        } else {
            Text(
                title,
                color = if (live) theme.ink else theme.muted,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
        }
    }
}

internal val SetupHairline = 1.dp
internal val SetupSectionGap = 14.dp

/** Keeps a fixed-width spacer available to sections that split a row in two. */
@Composable
internal fun SetupRowSplit(
    theme: SetupDocTheme,
    leftLabel: String,
    leftValue: String,
    leftPlaceholder: String,
    rightLabel: String,
    rightValue: String,
    rightPlaceholder: String,
    onLeft: () -> Unit,
    onRight: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SetupPickerField(
            label = leftLabel,
            value = leftValue,
            theme = theme,
            placeholder = leftPlaceholder,
            modifier = Modifier.weight(1f),
            onClick = onLeft,
        )
        SetupPickerField(
            label = rightLabel,
            value = rightValue,
            theme = theme,
            placeholder = rightPlaceholder,
            modifier = Modifier.weight(1f),
            onClick = onRight,
        )
    }
}

/** Small helper so callers do not need to import a divider shape of their own. */
@Composable
internal fun SetupDivider(theme: SetupDocTheme) {
    Spacer(
        Modifier
            .fillMaxWidth()
            .height(SetupHairline)
            .background(theme.border),
    )
}
