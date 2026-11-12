package com.example.momentra.ui.shell.business.mto

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
import com.example.momentra.ui.theme.MadeToOrderAmber

/** Zinc grab handle on MFG sheets — Figma `#71717A`, 40×4, radius 2. */
@Composable
fun MtoGrabber(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().height(22.dp), horizontalArrangement = Arrangement.Center) {
        Box(
            Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MadeToOrderAmber.textFaint),
        )
    }
}

@Composable
fun MtoSheetHeader(
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
                style = MadeToOrderAmber.screenTitle().copy(fontSize = 22.sp, lineHeight = 28.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (showAltBadge) {
                Text(
                    "ALT",
                    style = MadeToOrderAmber.chipLabel(true).copy(color = MadeToOrderAmber.onAccent),
                    maxLines = 1,
                    modifier = Modifier
                        .clip(MadeToOrderAmber.rPill)
                        .background(MadeToOrderAmber.accentSoft)
                        .border(1.dp, MadeToOrderAmber.accent, MadeToOrderAmber.rPill)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
            Box(
                Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MadeToOrderAmber.cardSlate)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Text("✕", style = MadeToOrderAmber.rowValue(MadeToOrderAmber.textBody).copy(fontSize = 13.sp))
            }
        }
        subtitle?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MadeToOrderAmber.rowCaption(MadeToOrderAmber.textBody).copy(fontSize = 12.sp), maxLines = 2)
        }
    }
}

/** Field label above inputs — Figma slate Medium 11 (`#CBD5E1`). */
@Composable
fun MtoFieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MadeToOrderAmber.metricLabel().copy(
            color = MadeToOrderAmber.textBody,
            fontSize = 11.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        ),
        maxLines = 1,
        modifier = modifier,
    )
}

/** Amber section title (Cart / Keypad / Parked…). */
@Composable
fun MtoSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MadeToOrderAmber.chipLabel(true).copy(color = MadeToOrderAmber.accentSoft, fontSize = 12.sp),
        maxLines = 1,
        modifier = modifier,
    )
}

@Composable
fun MtoModeChip(
    text: String,
    selected: Boolean,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    locked: String? = null,
    /** When true, selected fill is tender green (`#22C55E`) per Figma POS. */
    tender: Boolean = false,
) {
    val shape = MadeToOrderAmber.rPill
    val enabled = locked == null
    val click = if (onClick != null && enabled) Modifier.clickable(onClick = onClick) else Modifier
    val fill = when {
        locked != null -> MadeToOrderAmber.chip
        selected && tender -> MadeToOrderAmber.positive
        selected -> MadeToOrderAmber.accent
        else -> MadeToOrderAmber.chip
    }
    Text(
        text,
        style = MadeToOrderAmber.chipLabel(selected).copy(
            color = when {
                locked != null -> MadeToOrderAmber.textFaint
                selected && tender -> Color.White
                selected -> MadeToOrderAmber.onAccent
                else -> MadeToOrderAmber.textBody
            },
        ),
        maxLines = 1,
        modifier = modifier
            .clip(shape)
            .background(fill)
            .border(
                1.dp,
                when {
                    locked != null -> MadeToOrderAmber.chipStroke
                    selected -> MadeToOrderAmber.accentSoft
                    else -> MadeToOrderAmber.chipBorder
                },
                shape,
            )
            .then(click)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
fun MtoTextField(
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
            .clip(MadeToOrderAmber.rInner)
            .background(MadeToOrderAmber.field)
            .border(1.dp, MadeToOrderAmber.accentBorder.copy(alpha = 0.35f), MadeToOrderAmber.rInner)
            .padding(horizontal = 12.dp, vertical = if (singleLine) 0.dp else 10.dp),
        contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = TextStyle(
                color = MadeToOrderAmber.textPrimary,
                fontFamily = PlusJakartaSans,
                fontSize = 14.sp,
            ),
            cursorBrush = SolidColor(MadeToOrderAmber.accent),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(placeholder, style = MadeToOrderAmber.rowCaption(MadeToOrderAmber.textFaint))
                }
                inner()
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun MtoScanField(
    query: String,
    placeholder: String,
    onQueryChange: (String) -> Unit,
    onScan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(MadeToOrderAmber.rInner)
            .background(MadeToOrderAmber.cardSlate)
            .border(1.5.dp, MadeToOrderAmber.accent, MadeToOrderAmber.rInner)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.qa_package),
            contentDescription = null,
            tint = MadeToOrderAmber.textBody,
            modifier = Modifier.size(18.dp),
        )
        Box(Modifier.weight(1f)) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = MadeToOrderAmber.textPrimary,
                    fontFamily = PlusJakartaSans,
                    fontSize = 13.sp,
                ),
                cursorBrush = SolidColor(MadeToOrderAmber.accent),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text(placeholder, style = MadeToOrderAmber.rowCaption(MadeToOrderAmber.accentSoft).copy(fontSize = 13.sp))
                    }
                    inner()
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Text(
            "Scan",
            style = MadeToOrderAmber.chipLabel(true).copy(color = Color.White, fontSize = 11.sp),
            maxLines = 1,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MadeToOrderAmber.accent)
                .clickable(onClick = onScan)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

/** Section card with rose border on slate. */
@Composable
fun MtoSectionCard(
    label: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(MadeToOrderAmber.rCardTight)
            .background(MadeToOrderAmber.cardSlate)
            .border(1.dp, MadeToOrderAmber.accent.copy(alpha = 0.3f), MadeToOrderAmber.rCardTight)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            MtoSectionLabel(label)
            Spacer(Modifier.weight(1f))
            trailing?.let {
                Text(it, style = MadeToOrderAmber.rowCaption().copy(fontSize = 11.sp), maxLines = 1)
            }
        }
        content()
    }
}

@Composable
fun MtoLinesCard(
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
    MtoSectionCard(label = title, trailing = trailing, modifier = modifier, content = content)
}

@Composable
fun MtoTotalsCard(
    payableLabel: String,
    payable: String,
    tender: String?,
    modifier: Modifier = Modifier,
    rows: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(MadeToOrderAmber.rCardTight)
            .background(MadeToOrderAmber.cardSlate)
            .border(1.5.dp, MadeToOrderAmber.accent, MadeToOrderAmber.rCardTight)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        rows()
        Box(Modifier.fillMaxWidth().height(1.dp).background(MadeToOrderAmber.accentBorder))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(payableLabel, style = MadeToOrderAmber.rowCaption().copy(fontSize = 11.sp))
                Text(payable, style = MadeToOrderAmber.metricValue().copy(fontSize = 28.sp), maxLines = 1)
            }
            tender?.let {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Tender",
                        style = MadeToOrderAmber.rowCaption(MadeToOrderAmber.accentSoft).copy(
                            fontSize = 10.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        ),
                    )
                    Text(
                        it,
                        style = MadeToOrderAmber.rowValue(MadeToOrderAmber.positive).copy(fontSize = 13.sp),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
fun MtoTotalRow(
    label: String,
    value: String,
    valueColor: Color = MadeToOrderAmber.textPrimary,
    caption: String? = null,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(label, style = MadeToOrderAmber.rowCaption(MadeToOrderAmber.textBody).copy(fontSize = 12.sp))
            caption?.let { Text(it, style = MadeToOrderAmber.rowCaption(MadeToOrderAmber.textFaint).copy(fontSize = 10.sp)) }
        }
        Text(value, style = MadeToOrderAmber.rowValue(valueColor).copy(fontSize = 14.sp))
    }
}

@Composable
fun MtoPickerRow(
    label: String,
    value: String,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    /** When true, label sits above the field (Figma POS). When false, nested amber label. */
    externalLabel: Boolean = false,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (externalLabel) MtoFieldLabel(label)
        Row(
            Modifier
                .fillMaxWidth()
                .clip(MadeToOrderAmber.rInner)
                .background(MadeToOrderAmber.cardSlate)
                .border(1.dp, MadeToOrderAmber.accentBorder, MadeToOrderAmber.rInner)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (!externalLabel) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(label, style = MadeToOrderAmber.rowCaption(MadeToOrderAmber.accentSoft).copy(fontSize = 10.sp), maxLines = 1)
                    Text(value, style = MadeToOrderAmber.rowTitle().copy(fontSize = 14.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            } else {
                Text(
                    value,
                    style = MadeToOrderAmber.rowTitle().copy(fontSize = 14.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            Text("›", style = MadeToOrderAmber.rowValue(MadeToOrderAmber.textBody).copy(fontSize = 16.sp), maxLines = 1)
        }
    }
}

@Composable
fun MtoMoreDetails(
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
                .clip(MadeToOrderAmber.rInner)
                .border(1.dp, MadeToOrderAmber.accent.copy(alpha = 0.4f), MadeToOrderAmber.rInner)
                .clickable(onClick = onToggle)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                if (expanded) "Hide details" else "More details",
                style = MadeToOrderAmber.viewAll().copy(fontSize = 13.sp),
            )
            Spacer(Modifier.weight(1f))
            hint?.takeIf { !expanded }?.let {
                Text(it, style = MadeToOrderAmber.rowCaption().copy(fontSize = 11.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                if (expanded) "⌃" else "⌄",
                style = MadeToOrderAmber.viewAll().copy(fontSize = 14.sp),
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
        }
    }
}

@Composable
fun MtoWhyNote(body: String, modifier: Modifier = Modifier, tinted: Boolean = true) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MadeToOrderAmber.accent.copy(alpha = 0.12f))
            .border(1.dp, MadeToOrderAmber.accent.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Why", style = MadeToOrderAmber.rowCaption(MadeToOrderAmber.accentSoft).copy(fontSize = 11.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
            Text(body, style = MadeToOrderAmber.rowCaption(MadeToOrderAmber.whyBody).copy(fontSize = 11.sp), modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun MtoStickyFooter(
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
        val shape = MadeToOrderAmber.rCta
        Box(
            Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (enabled) 16.dp else 0.dp,
                    shape = shape,
                    clip = false,
                    ambientColor = MadeToOrderAmber.indigo.copy(alpha = 0.6f),
                    spotColor = MadeToOrderAmber.indigo.copy(alpha = 0.6f),
                )
                .clip(shape)
                .background(if (enabled) MadeToOrderAmber.accent else MadeToOrderAmber.surfaceRow)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(vertical = 16.dp, horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                label,
                style = MadeToOrderAmber.storyCtaTitle().copy(
                    color = if (enabled) MadeToOrderAmber.onAccent else MadeToOrderAmber.textFaint,
                    fontSize = 15.sp,
                ),
                maxLines = 1,
            )
        }
        Text(
            caveat,
            style = MadeToOrderAmber.rowCaption(MadeToOrderAmber.textBody).copy(fontSize = 10.sp),
            maxLines = 2,
        )
    }
}

@Composable
fun MtoAmountHero(
    label: String,
    amount: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    currencyBadge: String? = null,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(MadeToOrderAmber.rCardTight)
            .background(MadeToOrderAmber.cardSlate)
            .border(1.5.dp, MadeToOrderAmber.accent, MadeToOrderAmber.rCardTight)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MadeToOrderAmber.rowCaption().copy(fontSize = 12.sp), maxLines = 1)
            Spacer(Modifier.weight(1f))
            currencyBadge?.let {
                Text(
                    it,
                    style = MadeToOrderAmber.rowCaption(MadeToOrderAmber.accentSoft).copy(
                        fontSize = 11.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
            }
        }
        Text(amount, style = MadeToOrderAmber.metricValue().copy(fontSize = 40.sp, lineHeight = 48.sp), maxLines = 1)
        caption?.let { Text(it, style = MadeToOrderAmber.rowCaption().copy(fontSize = 11.sp), maxLines = 2) }
    }
}

/** Toast after master save — Figma `✓ Saved · sheet closes and returns back`. */
@Composable
fun MtoSavedToast(modifier: Modifier = Modifier, message: String = "✓ Saved · sheet closes and returns back") {
    Text(
        message,
        style = MadeToOrderAmber.rowCaption(MadeToOrderAmber.textPrimary).copy(fontSize = 12.sp),
        maxLines = 2,
        modifier = modifier
            .fillMaxWidth()
            .clip(MadeToOrderAmber.rInner)
            .background(MadeToOrderAmber.positive.copy(alpha = 0.16f))
            .border(1.dp, MadeToOrderAmber.positive.copy(alpha = 0.45f), MadeToOrderAmber.rInner)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    )
}

@Composable
fun MtoStepper(
    quantity: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        MtoStepperButton("−", onDecrement)
        Text(quantity.toString(), style = MadeToOrderAmber.rowValue().copy(fontSize = 13.sp))
        MtoStepperButton("+", onIncrement)
    }
}

@Composable
private fun MtoStepperButton(glyph: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MadeToOrderAmber.chip)
            .border(1.dp, MadeToOrderAmber.accentBorder.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, style = MadeToOrderAmber.rowValue().copy(fontSize = 15.sp))
    }
}

@Composable
fun MtoMetaRows(
    rows: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(MadeToOrderAmber.rInner)
            .background(MadeToOrderAmber.field)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        rows.forEach { (label, value) ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, style = MadeToOrderAmber.rowCaption(MadeToOrderAmber.accentSoft).copy(fontSize = 10.sp), maxLines = 1)
                Text(value, style = MadeToOrderAmber.rowValue(), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun MtoLockedChip(label: String, modifier: Modifier = Modifier) {
    MtoModeChip(text = label, selected = false, locked = "Unavailable", modifier = modifier)
}

/**
 * Products & BOM catalogue row — Figma `2545:29477`.
 * Dot · name/meta · price · GST badge · Edit (honest locked without hydrate).
 */
@Composable
fun MtoCatalogPartRow(
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
            .background(MadeToOrderAmber.chip)
            .border(1.dp, MadeToOrderAmber.chipBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (accentDot) MadeToOrderAmber.accent else Color(0xFF22C55E)),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(name, style = MadeToOrderAmber.rowTitle().copy(fontSize = 14.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(meta, style = MadeToOrderAmber.rowCaption().copy(fontSize = 11.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(
            priceLabel,
            style = MadeToOrderAmber.rowTitle().copy(fontSize = 14.sp, color = MadeToOrderAmber.accentSoft),
            maxLines = 1,
        )
        gstBadge?.takeIf { it.isNotBlank() }?.let { badge ->
            Text(
                badge,
                style = MadeToOrderAmber.rowCaption(MadeToOrderAmber.accentSoft).copy(
                    fontSize = 10.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(MadeToOrderAmber.chip)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                maxLines = 1,
            )
        }
        Box(
            Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(MadeToOrderAmber.surface)
                .border(1.dp, MadeToOrderAmber.chipBorder, RoundedCornerShape(10.dp))
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
                style = MadeToOrderAmber.rowCaption(
                    if (editLocked) MadeToOrderAmber.textMuted else MadeToOrderAmber.textPrimary,
                ).copy(fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
            )
        }
    }
}

/** Figma denser browser CTA (`+ New stock item` / `+ New product`) under catalogue grids. */
@Composable
fun MtoDenseCreateCta(
    title: String,
    hint: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MadeToOrderAmber.accent.copy(alpha = 0.08f))
            .border(1.dp, MadeToOrderAmber.accent.copy(alpha = 0.40f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MadeToOrderAmber.chipLabel(false).copy(color = MadeToOrderAmber.accent, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
        Spacer(Modifier.weight(1f))
        Text(hint, style = MadeToOrderAmber.rowCaption(), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Create | Edit existing mode chips shared by master sheets (Figma mode row). */
@Composable
fun MtoCreateEditMode(
    isCreate: Boolean,
    onCreate: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    editLocked: Boolean = false,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        MtoFieldLabel("mode")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MtoModeChip("Create", selected = isCreate, onClick = onCreate)
            if (editLocked) {
                MtoLockedChip("Edit existing")
            } else {
                MtoModeChip("Edit existing", selected = !isCreate, onClick = onEdit)
            }
        }
    }
}

/** GST / tax total row that never invents a computed amount. */
@Composable
fun MtoUnavailableGstRow(
    label: String = "Input GST · CGST+SGST",
    caption: String = "Unavailable — stamped server-side on bill / GRN",
    modifier: Modifier = Modifier,
) {
    MtoTotalRow(
        label = label,
        value = "Unavailable",
        valueColor = MadeToOrderAmber.warning,
        caption = caption,
        modifier = modifier,
    )
}

/**
 * On-sheet numeric keypad for Quick amount (Figma RTL-QA-01-ALT).
 * Posts the same amount string the text field would — no separate event path.
 */
@Composable
fun MtoNumericKeypad(
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
                            .background(MadeToOrderAmber.chip)
                            .border(1.dp, MadeToOrderAmber.accentBorder.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .clickable {
                                onValueChange(mtoKeypadApply(value, key, maxLength))
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(key, style = MadeToOrderAmber.rowValue().copy(fontSize = 18.sp))
                    }
                }
            }
        }
    }
}

internal fun mtoKeypadApply(current: String, key: String, maxLength: Int = 12): String {
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
fun MtoChipRow(
    label: String?,
    options: List<String>,
    selected: String,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        label?.let { MtoFieldLabel(it) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { option ->
                MtoModeChip(option, selected = option == selected, onClick = { onSelect(option) })
            }
        }
    }
}

/** Standard slate sheet column used by every retail quick-add surface. */
@Composable
internal fun MtoSheetBody(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(MadeToOrderAmber.cardSlate)
            .padding(horizontal = MadeToOrderAmber.gutter, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}


@Composable
fun MtoEmpty(title: String, caption: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(MadeToOrderAmber.rInner)
            .background(MadeToOrderAmber.chip)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, style = MadeToOrderAmber.rowTitle())
        Text(caption, style = MadeToOrderAmber.rowCaption())
    }
}
