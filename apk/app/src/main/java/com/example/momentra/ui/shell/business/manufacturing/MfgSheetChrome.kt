package com.example.momentra.ui.shell.business.manufacturing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R
import com.example.momentra.ui.theme.PlusJakartaSans
import com.example.momentra.ui.theme.ManufacturingAmber

/** Zinc grab handle on MFG sheets — Figma `#71717A`, 40×4, radius 2. */
@Composable
fun MfgGrabber(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().height(22.dp), horizontalArrangement = Arrangement.Center) {
        Box(
            Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(ManufacturingAmber.textFaint),
        )
    }
}

@Composable
fun MfgSheetHeader(
    title: String,
    subtitle: String?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    showAltBadge: Boolean = false,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                title,
                style = ManufacturingAmber.screenTitle().copy(fontSize = 22.sp, lineHeight = 28.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (showAltBadge) {
                Text(
                    "ALT",
                    style = ManufacturingAmber.chipLabel(true).copy(color = ManufacturingAmber.onAccent),
                    maxLines = 1,
                    modifier = Modifier
                        .clip(ManufacturingAmber.rPill)
                        .background(ManufacturingAmber.accentSoft)
                        .border(1.dp, ManufacturingAmber.accent, ManufacturingAmber.rPill)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
            Box(
                Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ManufacturingAmber.cardSlate)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Text("✕", style = ManufacturingAmber.rowValue(ManufacturingAmber.textBody).copy(fontSize = 13.sp))
            }
        }
        subtitle?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = ManufacturingAmber.rowCaption(ManufacturingAmber.textBody).copy(fontSize = 12.sp), maxLines = 2)
        }
    }
}

/** Field label above inputs — Figma slate Medium 11 (`#CBD5E1`). */
@Composable
fun MfgFieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = ManufacturingAmber.metricLabel().copy(
            color = ManufacturingAmber.textBody,
            fontSize = 11.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        ),
        maxLines = 1,
        modifier = modifier,
    )
}

/** Amber section title (Cart / Keypad / Parked…). */
@Composable
fun MfgSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = ManufacturingAmber.chipLabel(true).copy(color = ManufacturingAmber.accentSoft, fontSize = 12.sp),
        maxLines = 1,
        modifier = modifier,
    )
}

@Composable
fun MfgModeChip(
    text: String,
    selected: Boolean,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    locked: String? = null,
    /** When true, selected fill is tender green (`#22C55E`) per Figma POS. */
    tender: Boolean = false,
) {
    val shape = ManufacturingAmber.rPill
    val enabled = locked == null
    val click = if (onClick != null && enabled) Modifier.clickable(onClick = onClick) else Modifier
    val fill = when {
        locked != null -> ManufacturingAmber.chip
        selected && tender -> ManufacturingAmber.positive
        selected -> ManufacturingAmber.accent
        else -> ManufacturingAmber.chip
    }
    Text(
        text,
        style = ManufacturingAmber.chipLabel(selected).copy(
            color = when {
                locked != null -> ManufacturingAmber.textFaint
                selected && tender -> Color.White
                selected -> ManufacturingAmber.onAccent
                else -> ManufacturingAmber.textBody
            },
        ),
        maxLines = 1,
        modifier = modifier
            .clip(shape)
            .background(fill)
            .border(
                1.dp,
                when {
                    locked != null -> ManufacturingAmber.chipStroke
                    selected -> ManufacturingAmber.accentSoft
                    else -> ManufacturingAmber.chipBorder
                },
                shape,
            )
            .then(click)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
fun MfgTextField(
    value: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = true,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(if (singleLine) 46.dp else 72.dp)
            .clip(ManufacturingAmber.rInner)
            .background(ManufacturingAmber.field)
            .border(1.dp, ManufacturingAmber.accentBorder.copy(alpha = 0.35f), ManufacturingAmber.rInner)
            .padding(horizontal = 12.dp, vertical = if (singleLine) 0.dp else 10.dp),
        contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = TextStyle(
                color = ManufacturingAmber.textPrimary,
                fontFamily = PlusJakartaSans,
                fontSize = 14.sp,
            ),
            cursorBrush = SolidColor(ManufacturingAmber.accent),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(placeholder, style = ManufacturingAmber.rowCaption(ManufacturingAmber.textFaint))
                }
                inner()
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun MfgScanField(
    query: String,
    placeholder: String,
    onQueryChange: (String) -> Unit,
    onScan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(ManufacturingAmber.rInner)
            .background(ManufacturingAmber.cardSlate)
            .border(1.5.dp, ManufacturingAmber.accent, ManufacturingAmber.rInner)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.qa_package),
            contentDescription = null,
            tint = ManufacturingAmber.textBody,
            modifier = Modifier.size(18.dp),
        )
        Box(Modifier.weight(1f)) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = ManufacturingAmber.textPrimary,
                    fontFamily = PlusJakartaSans,
                    fontSize = 13.sp,
                ),
                cursorBrush = SolidColor(ManufacturingAmber.accent),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text(placeholder, style = ManufacturingAmber.rowCaption(ManufacturingAmber.accentSoft).copy(fontSize = 13.sp))
                    }
                    inner()
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Text(
            "Scan",
            style = ManufacturingAmber.chipLabel(true).copy(color = Color.White, fontSize = 11.sp),
            maxLines = 1,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(ManufacturingAmber.accent)
                .clickable(onClick = onScan)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

/** Section card with rose border on slate. */
@Composable
fun MfgSectionCard(
    label: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(ManufacturingAmber.rCardTight)
            .background(ManufacturingAmber.cardSlate)
            .border(1.dp, ManufacturingAmber.accent.copy(alpha = 0.3f), ManufacturingAmber.rCardTight)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            MfgSectionLabel(label)
            Spacer(Modifier.weight(1f))
            trailing?.let {
                Text(it, style = ManufacturingAmber.rowCaption().copy(fontSize = 11.sp), maxLines = 1)
            }
        }
        content()
    }
}

@Composable
fun MfgLinesCard(
    title: String,
    lineCount: Int,
    modifier: Modifier = Modifier,
    units: Int? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val trailing = when {
        lineCount == 0 -> "No lines yet"
        units != null ->
            "$lineCount line${if (lineCount == 1) "" else "s"} · $units unit${if (units == 1) "" else "s"}"
        else -> "$lineCount line${if (lineCount == 1) "" else "s"}"
    }
    MfgSectionCard(label = title, trailing = trailing, modifier = modifier, content = content)
}

@Composable
fun MfgTotalsCard(
    payableLabel: String,
    payable: String,
    tender: String?,
    modifier: Modifier = Modifier,
    rows: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(ManufacturingAmber.rCardTight)
            .background(ManufacturingAmber.cardSlate)
            .border(1.5.dp, ManufacturingAmber.accent, ManufacturingAmber.rCardTight)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        rows()
        Box(Modifier.fillMaxWidth().height(1.dp).background(ManufacturingAmber.accentBorder))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(payableLabel, style = ManufacturingAmber.rowCaption().copy(fontSize = 11.sp))
                Text(payable, style = ManufacturingAmber.metricValue().copy(fontSize = 28.sp), maxLines = 1)
            }
            tender?.let {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Tender",
                        style = ManufacturingAmber.rowCaption(ManufacturingAmber.accentSoft).copy(
                            fontSize = 10.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        ),
                    )
                    Text(
                        it,
                        style = ManufacturingAmber.rowValue(ManufacturingAmber.positive).copy(fontSize = 13.sp),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
fun MfgTotalRow(
    label: String,
    value: String,
    valueColor: Color = ManufacturingAmber.textPrimary,
    caption: String? = null,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(label, style = ManufacturingAmber.rowCaption(ManufacturingAmber.textBody).copy(fontSize = 12.sp))
            caption?.let { Text(it, style = ManufacturingAmber.rowCaption(ManufacturingAmber.textFaint).copy(fontSize = 10.sp)) }
        }
        Text(value, style = ManufacturingAmber.rowValue(valueColor).copy(fontSize = 14.sp))
    }
}

@Composable
fun MfgPickerRow(
    label: String,
    value: String,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    /** When true, label sits above the field (Figma POS). When false, nested amber label. */
    externalLabel: Boolean = false,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (externalLabel) MfgFieldLabel(label)
        Row(
            Modifier
                .fillMaxWidth()
                .clip(ManufacturingAmber.rInner)
                .background(ManufacturingAmber.cardSlate)
                .border(1.dp, ManufacturingAmber.accentBorder, ManufacturingAmber.rInner)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (!externalLabel) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(label, style = ManufacturingAmber.rowCaption(ManufacturingAmber.accentSoft).copy(fontSize = 10.sp), maxLines = 1)
                    Text(value, style = ManufacturingAmber.rowTitle().copy(fontSize = 14.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            } else {
                Text(
                    value,
                    style = ManufacturingAmber.rowTitle().copy(fontSize = 14.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            Text("›", style = ManufacturingAmber.rowValue(ManufacturingAmber.textBody).copy(fontSize = 16.sp), maxLines = 1)
        }
    }
}

@Composable
fun MfgMoreDetails(
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(ManufacturingAmber.rInner)
                .border(1.dp, ManufacturingAmber.accent.copy(alpha = 0.4f), ManufacturingAmber.rInner)
                .clickable(onClick = onToggle)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                if (expanded) "Hide details" else "More details",
                style = ManufacturingAmber.viewAll().copy(fontSize = 13.sp),
            )
            Spacer(Modifier.weight(1f))
            hint?.takeIf { !expanded }?.let {
                Text(it, style = ManufacturingAmber.rowCaption().copy(fontSize = 11.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                if (expanded) "⌃" else "⌄",
                style = ManufacturingAmber.viewAll().copy(fontSize = 14.sp),
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
        }
    }
}

@Composable
fun MfgWhyBox(body: String, modifier: Modifier = Modifier, tinted: Boolean = true) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ManufacturingAmber.accent.copy(alpha = 0.12f))
            .border(1.dp, ManufacturingAmber.accent.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Why", style = ManufacturingAmber.rowCaption(ManufacturingAmber.accentSoft).copy(fontSize = 11.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
            Text(body, style = ManufacturingAmber.rowCaption(ManufacturingAmber.whyBody).copy(fontSize = 11.sp), modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun MfgStickyFooter(
    label: String,
    enabled: Boolean,
    caveat: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier.fillMaxWidth().padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val shape = ManufacturingAmber.rCta
        Box(
            Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (enabled) 16.dp else 0.dp,
                    shape = shape,
                    clip = false,
                    ambientColor = ManufacturingAmber.indigo.copy(alpha = 0.6f),
                    spotColor = ManufacturingAmber.indigo.copy(alpha = 0.6f),
                )
                .clip(shape)
                .background(if (enabled) ManufacturingAmber.accent else ManufacturingAmber.surfaceRow)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(vertical = 16.dp, horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                label,
                style = ManufacturingAmber.storyCtaTitle().copy(
                    color = if (enabled) ManufacturingAmber.onAccent else ManufacturingAmber.textFaint,
                    fontSize = 15.sp,
                ),
                maxLines = 1,
            )
        }
        Text(
            caveat,
            style = ManufacturingAmber.rowCaption(ManufacturingAmber.textBody).copy(fontSize = 10.sp),
            maxLines = 2,
        )
    }
}

@Composable
fun MfgAmountHero(
    label: String,
    amount: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    currencyBadge: String? = null,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(ManufacturingAmber.rCardTight)
            .background(ManufacturingAmber.cardSlate)
            .border(1.5.dp, ManufacturingAmber.accent, ManufacturingAmber.rCardTight)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = ManufacturingAmber.rowCaption().copy(fontSize = 12.sp), maxLines = 1)
            Spacer(Modifier.weight(1f))
            currencyBadge?.let {
                Text(
                    it,
                    style = ManufacturingAmber.rowCaption(ManufacturingAmber.accentSoft).copy(
                        fontSize = 11.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
            }
        }
        Text(amount, style = ManufacturingAmber.metricValue().copy(fontSize = 40.sp, lineHeight = 48.sp), maxLines = 1)
        caption?.let { Text(it, style = ManufacturingAmber.rowCaption().copy(fontSize = 11.sp), maxLines = 2) }
    }
}

/** Toast after master save — Figma `✓ Saved · sheet closes and returns back`. */
@Composable
fun MfgSavedToast(modifier: Modifier = Modifier, message: String = "✓ Saved · sheet closes and returns back") {
    Text(
        message,
        style = ManufacturingAmber.rowCaption(ManufacturingAmber.textPrimary).copy(fontSize = 12.sp),
        maxLines = 2,
        modifier = modifier
            .fillMaxWidth()
            .clip(ManufacturingAmber.rInner)
            .background(ManufacturingAmber.positive.copy(alpha = 0.16f))
            .border(1.dp, ManufacturingAmber.positive.copy(alpha = 0.45f), ManufacturingAmber.rInner)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    )
}

@Composable
fun MfgStepper(
    quantity: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        MfgStepperButton("−", onDecrement)
        Text(quantity.toString(), style = ManufacturingAmber.rowValue().copy(fontSize = 13.sp))
        MfgStepperButton("+", onIncrement)
    }
}

@Composable
private fun MfgStepperButton(glyph: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(ManufacturingAmber.chip)
            .border(1.dp, ManufacturingAmber.accentBorder.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, style = ManufacturingAmber.rowValue().copy(fontSize = 15.sp))
    }
}

@Composable
fun MfgMetaRows(
    rows: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(ManufacturingAmber.rInner)
            .background(ManufacturingAmber.field)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        rows.forEach { (label, value) ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, style = ManufacturingAmber.rowCaption(ManufacturingAmber.accentSoft).copy(fontSize = 10.sp), maxLines = 1)
                Text(value, style = ManufacturingAmber.rowValue(), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun MfgLockedChip(label: String, modifier: Modifier = Modifier) {
    MfgModeChip(text = label, selected = false, locked = "Unavailable", modifier = modifier)
}

/**
 * Products & BOM catalogue row — Figma `2545:29477`.
 * Dot · name/meta · price · GST badge · Edit (honest locked without hydrate).
 */
@Composable
fun MfgCatalogPartRow(
    name: String,
    meta: String,
    priceLabel: String,
    gstBadge: String?,
    editLocked: Boolean = true,
    onEdit: (() -> Unit)? = null,
    accentDot: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ManufacturingAmber.chip)
            .border(1.dp, ManufacturingAmber.chipBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (accentDot) ManufacturingAmber.accent else Color(0xFF22C55E)),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(name, style = ManufacturingAmber.rowTitle().copy(fontSize = 14.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(meta, style = ManufacturingAmber.rowCaption().copy(fontSize = 11.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(
            priceLabel,
            style = ManufacturingAmber.rowTitle().copy(fontSize = 14.sp, color = ManufacturingAmber.accentSoft),
            maxLines = 1,
        )
        gstBadge?.takeIf { it.isNotBlank() }?.let { badge ->
            Text(
                badge,
                style = ManufacturingAmber.rowCaption(ManufacturingAmber.accentSoft).copy(
                    fontSize = 10.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(ManufacturingAmber.chip)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                maxLines = 1,
            )
        }
        Box(
            Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(ManufacturingAmber.surface)
                .border(1.dp, ManufacturingAmber.chipBorder, RoundedCornerShape(10.dp))
                .then(
                    if (editLocked || onEdit == null) Modifier
                    else Modifier.clickable(onClick = onEdit),
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .then(if (editLocked) Modifier else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (editLocked) "Edit · locked" else "Edit",
                style = ManufacturingAmber.rowCaption(
                    if (editLocked) ManufacturingAmber.textMuted else ManufacturingAmber.textPrimary,
                ).copy(fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
            )
        }
    }
}

/** Figma denser browser CTA (`+ New stock item` / `+ New product`) under catalogue grids. */
@Composable
fun MfgDenseCreateCta(
    title: String,
    hint: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ManufacturingAmber.accent.copy(alpha = 0.08f))
            .border(1.dp, ManufacturingAmber.accent.copy(alpha = 0.40f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = ManufacturingAmber.chipLabel(false).copy(color = ManufacturingAmber.accent, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
        Spacer(Modifier.weight(1f))
        Text(hint, style = ManufacturingAmber.rowCaption(), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Create | Edit existing mode chips shared by master sheets (Figma mode row). */
@Composable
fun MfgCreateEditMode(
    isCreate: Boolean,
    onCreate: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    editLocked: Boolean = false,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        MfgFieldLabel("mode")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MfgModeChip("Create", selected = isCreate, onClick = onCreate)
            if (editLocked) {
                MfgLockedChip("Edit existing")
            } else {
                MfgModeChip("Edit existing", selected = !isCreate, onClick = onEdit)
            }
        }
    }
}

/** GST / tax total row that never invents a computed amount. */
@Composable
fun MfgUnavailableGstRow(
    label: String = "Input GST · CGST+SGST",
    caption: String = "Unavailable — stamped server-side on bill / GRN",
    modifier: Modifier = Modifier,
) {
    MfgTotalRow(
        label = label,
        value = "Unavailable",
        valueColor = ManufacturingAmber.warning,
        caption = caption,
        modifier = modifier,
    )
}

/**
 * On-sheet numeric keypad for Quick amount (Figma RTL-QA-01-ALT).
 * Posts the same amount string the text field would — no separate event path.
 */
@Composable
fun MfgNumericKeypad(
    value: String,
    modifier: Modifier = Modifier,
    maxLength: Int = 12,
    onValueChange: (String) -> Unit,
) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(".", "0", "⌫"),
    )
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        keys.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ManufacturingAmber.chip)
                            .border(1.dp, ManufacturingAmber.accentBorder.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .clickable {
                                onValueChange(mfgKeypadApply(value, key, maxLength))
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(key, style = ManufacturingAmber.rowValue().copy(fontSize = 18.sp))
                    }
                }
            }
        }
    }
}

internal fun mfgKeypadApply(current: String, key: String, maxLength: Int = 12): String {
    return when (key) {
        "⌫" -> current.dropLast(1)
        "." -> when {
            current.contains('.') -> current
            current.isEmpty() -> "0."
            else -> (current + ".").take(maxLength)
        }
        else -> {
            if (current == "0" && key != ".") key.take(maxLength)
            else (current + key).take(maxLength)
        }
    }
}

/** Chip row helper for labelled LOV selections. */
@Composable
fun MfgChipRow(
    label: String?,
    options: List<String>,
    selected: String,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        label?.let { MfgFieldLabel(it) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { option ->
                MfgModeChip(option, selected = option == selected, onClick = { onSelect(option) })
            }
        }
    }
}

/** Standard slate sheet column used by every retail quick-add surface. */
@Composable
internal fun MfgSheetBody(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(ManufacturingAmber.cardSlate)
            .padding(horizontal = ManufacturingAmber.gutter, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}


@Composable
fun MfgEmpty(title: String, caption: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(ManufacturingAmber.rInner)
            .background(ManufacturingAmber.chip)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, style = ManufacturingAmber.rowTitle())
        Text(caption, style = ManufacturingAmber.rowCaption())
    }
}
