package com.example.momentra.ui.shell.business.retail

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R
import com.example.momentra.ui.theme.PlusJakartaSans
import com.example.momentra.ui.theme.RetailRose

/** Rose grab handle on slate sheets — Figma `#FB7185`, 40×4, radius 2. */
@Composable
fun RoseGrabber(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().height(22.dp), horizontalArrangement = Arrangement.Center) {
        Box(
            Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(RetailRose.grab),
        )
    }
}

@Composable
fun RoseSheetHeader(
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
                style = RetailRose.screenTitle().copy(fontSize = 22.sp, lineHeight = 28.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (showAltBadge) {
                Text(
                    "ALT",
                    style = RetailRose.chipLabel(true).copy(color = Color.White),
                    maxLines = 1,
                    modifier = Modifier
                        .clip(RetailRose.rPill)
                        .background(RetailRose.rosePill)
                        .border(1.dp, RetailRose.rose, RetailRose.rPill)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
            Box(
                Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(RetailRose.cardSlate)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Text("✕", style = RetailRose.rowValue(RetailRose.textBody).copy(fontSize = 13.sp))
            }
        }
        subtitle?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = RetailRose.rowCaption(RetailRose.textBody).copy(fontSize = 12.sp), maxLines = 2)
        }
    }
}

/** Field label above inputs — Figma slate Medium 11 (`#CBD5E1`). */
@Composable
fun RoseFieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = RetailRose.metricLabel().copy(
            color = RetailRose.textBody,
            fontSize = 11.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        ),
        maxLines = 1,
        modifier = modifier,
    )
}

/** Amber section title (Cart / Keypad / Parked…). */
@Composable
fun RoseSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = RetailRose.chipLabel(true).copy(color = RetailRose.amber, fontSize = 12.sp),
        maxLines = 1,
        modifier = modifier,
    )
}

@Composable
fun RoseModeChip(
    text: String,
    selected: Boolean,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    locked: String? = null,
    /** When true, selected fill is tender green (`#22C55E`) per Figma POS. */
    tender: Boolean = false,
    /**
     * Catalogue / stock browser category chips (Figma SO/PO): selected = rose fill,
     * amber border, dark ink on rose.
     */
    category: Boolean = false,
) {
    val shape = RetailRose.rPill
    val enabled = locked == null
    val click = if (onClick != null && enabled) Modifier.clickable(onClick = onClick) else Modifier
    val fill = when {
        locked != null -> RetailRose.chip
        selected && tender -> RetailRose.positive
        selected -> RetailRose.rose
        category -> Color(0xFF334155)
        else -> RetailRose.chip
    }
    Text(
        text,
        style = RetailRose.chipLabel(selected).copy(
            color = when {
                locked != null -> RetailRose.textFaint
                selected && category -> RetailRose.field
                selected -> Color.White
                category -> RetailRose.textFaint
                else -> RetailRose.textBody
            },
        ),
        maxLines = 1,
        modifier = modifier
            .clip(shape)
            .background(fill)
            .border(
                1.dp,
                when {
                    locked != null -> RetailRose.chipStroke
                    selected && category -> RetailRose.amber
                    selected -> RetailRose.rose
                    category -> Color(0xFF475569)
                    else -> RetailRose.roseBorder
                },
                shape,
            )
            .then(click)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
fun RoseTextField(
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
            .clip(RetailRose.rInner)
            .background(RetailRose.field)
            .border(1.dp, RetailRose.roseBorder.copy(alpha = 0.35f), RetailRose.rInner)
            .padding(horizontal = 12.dp, vertical = if (singleLine) 0.dp else 10.dp),
        contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = TextStyle(
                color = RetailRose.textPrimary,
                fontFamily = PlusJakartaSans,
                fontSize = 14.sp,
            ),
            cursorBrush = SolidColor(RetailRose.rose),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(placeholder, style = RetailRose.rowCaption(RetailRose.textFaint))
                }
                inner()
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun RoseScanField(
    query: String,
    placeholder: String,
    onQueryChange: (String) -> Unit,
    onScan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RetailRose.rInner)
            .background(RetailRose.cardSlate)
            .border(1.5.dp, RetailRose.rose, RetailRose.rInner)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_retail_barcode),
            contentDescription = null,
            tint = RetailRose.textBody,
            modifier = Modifier.size(18.dp),
        )
        Box(Modifier.weight(1f)) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = RetailRose.textPrimary,
                    fontFamily = PlusJakartaSans,
                    fontSize = 13.sp,
                ),
                cursorBrush = SolidColor(RetailRose.rose),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text(placeholder, style = RetailRose.rowCaption(RetailRose.textRoseHint).copy(fontSize = 13.sp))
                    }
                    inner()
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Text(
            "Scan",
            style = RetailRose.chipLabel(true).copy(color = Color.White, fontSize = 11.sp),
            maxLines = 1,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(RetailRose.rose)
                .clickable(onClick = onScan)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

/** Section card with rose border on slate. */
@Composable
fun RoseSectionCard(
    label: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RetailRose.rCardTight)
            .background(RetailRose.cardSlate)
            .border(1.dp, RetailRose.rose.copy(alpha = 0.3f), RetailRose.rCardTight)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            RoseSectionLabel(label)
            Spacer(Modifier.weight(1f))
            trailing?.let {
                Text(it, style = RetailRose.rowCaption().copy(fontSize = 11.sp), maxLines = 1)
            }
        }
        content()
    }
}

@Composable
fun RoseLinesCard(
    title: String,
    lineCount: Int,
    modifier: Modifier = Modifier,
    units: Int? = null,
    /** When set, replaces the default "N lines · …" trailing caption (Figma allocate / receive). */
    trailingOverride: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val trailing = trailingOverride ?: when {
        lineCount == 0 -> "No lines yet"
        units != null ->
            "$lineCount line${if (lineCount == 1) "" else "s"} · $units unit${if (units == 1) "" else "s"}"
        else -> "$lineCount line${if (lineCount == 1) "" else "s"}"
    }
    RoseSectionCard(label = title, trailing = trailing, modifier = modifier, content = content)
}

@Composable
fun RoseTotalsCard(
    payableLabel: String,
    payable: String,
    tender: String?,
    modifier: Modifier = Modifier,
    /** Figma side label: `Tender` (POS), `Method` (Collection/Payment), or `Pay later` (Purchase). */
    sideLabel: String = "Tender",
    sideValueColor: Color = RetailRose.positive,
    sideLabelColor: Color = RetailRose.amber,
    rows: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RetailRose.rCardTight)
            .background(RetailRose.cardSlate)
            .border(1.5.dp, RetailRose.rose, RetailRose.rCardTight)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        rows()
        Box(Modifier.fillMaxWidth().height(1.dp).background(RetailRose.roseBorder))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(payableLabel, style = RetailRose.rowCaption().copy(fontSize = 11.sp))
                Text(payable, style = RetailRose.metricValue().copy(fontSize = 28.sp), maxLines = 1)
            }
            tender?.let {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        sideLabel,
                        style = RetailRose.rowCaption(sideLabelColor).copy(
                            fontSize = 10.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        ),
                    )
                    Text(
                        it,
                        style = RetailRose.rowValue(sideValueColor).copy(fontSize = 13.sp),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
fun RoseTotalRow(
    label: String,
    value: String,
    valueColor: Color = RetailRose.textPrimary,
    caption: String? = null,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(label, style = RetailRose.rowCaption(RetailRose.textBody).copy(fontSize = 12.sp))
            caption?.let { Text(it, style = RetailRose.rowCaption(RetailRose.textFaint).copy(fontSize = 10.sp)) }
        }
        Text(value, style = RetailRose.rowValue(valueColor).copy(fontSize = 14.sp))
    }
}

@Composable
fun RosePickerRow(
    label: String,
    value: String,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    /** When true, label sits above the field (Figma POS). When false, nested amber label. */
    externalLabel: Boolean = false,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (externalLabel) RoseFieldLabel(label)
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RetailRose.rInner)
                .background(RetailRose.cardSlate)
                .border(1.dp, RetailRose.roseBorder, RetailRose.rInner)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (!externalLabel) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(label, style = RetailRose.rowCaption(RetailRose.amber).copy(fontSize = 10.sp), maxLines = 1)
                    Text(value, style = RetailRose.rowTitle().copy(fontSize = 14.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            } else {
                Text(
                    value,
                    style = RetailRose.rowTitle().copy(fontSize = 14.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            Text("›", style = RetailRose.rowValue(RetailRose.textBody).copy(fontSize = 16.sp), maxLines = 1)
        }
    }
}

@Composable
fun RoseMoreDetails(
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
                .clip(RetailRose.rInner)
                .border(1.dp, RetailRose.rose.copy(alpha = 0.4f), RetailRose.rInner)
                .clickable(onClick = onToggle)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                if (expanded) "Hide details" else "More details",
                style = RetailRose.viewAll().copy(fontSize = 13.sp),
            )
            Spacer(Modifier.weight(1f))
            hint?.takeIf { !expanded }?.let {
                Text(it, style = RetailRose.rowCaption().copy(fontSize = 11.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                if (expanded) "⌃" else "⌄",
                style = RetailRose.viewAll().copy(fontSize = 14.sp),
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
        }
    }
}

@Composable
fun RoseWhyBox(body: String, modifier: Modifier = Modifier, tinted: Boolean = true) {
    WhyNote(body = body, modifier = modifier, tinted = tinted)
}

/**
 * Sticky rose CTA + caption.
 *
 * Figma QA family (POS / money / ops / HTR / masters lists) uses **white** CTA ink and
 * slate `#CBD5E1` caveat. Master create/edit forms (`2506:29612`+) use dark ink on rose
 * (`#0F172A` / [RetailRose.inkOnRose]) and amber caveat — pass [formStyle] = true there.
 */
@Composable
fun RoseStickyFooter(
    label: String,
    enabled: Boolean,
    caveat: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** Master-form chrome: dark CTA ink + amber caveat (RTL-FORM frames). */
    formStyle: Boolean = false,
) {
    Column(
        modifier.fillMaxWidth().padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val shape = RetailRose.rCta
        val labelColor = when {
            !enabled -> RetailRose.textFaint
            formStyle -> RetailRose.inkOnRose
            else -> Color.White
        }
        val caveatColor = if (formStyle) RetailRose.amber else RetailRose.textBody
        Box(
            Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (enabled) 16.dp else 0.dp,
                    shape = shape,
                    clip = false,
                    ambientColor = RetailRose.indigo.copy(alpha = 0.6f),
                    spotColor = RetailRose.indigo.copy(alpha = 0.6f),
                )
                .clip(shape)
                .background(if (enabled) RetailRose.rose else RetailRose.surfaceRow)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(vertical = 16.dp, horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                label,
                style = RetailRose.storyCtaTitle().copy(
                    color = labelColor,
                    fontSize = 15.sp,
                ),
                maxLines = 1,
            )
        }
        Text(
            caveat,
            style = RetailRose.rowCaption(caveatColor).copy(fontSize = 10.sp),
            maxLines = 2,
        )
    }
}

@Composable
fun RoseAmountHero(
    label: String,
    amount: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    currencyBadge: String? = null,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RetailRose.rCardTight)
            .background(RetailRose.cardSlate)
            .border(1.5.dp, RetailRose.rose, RetailRose.rCardTight)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = RetailRose.rowCaption().copy(fontSize = 12.sp), maxLines = 1)
            Spacer(Modifier.weight(1f))
            currencyBadge?.let {
                Text(
                    it,
                    style = RetailRose.rowCaption(RetailRose.amber).copy(
                        fontSize = 11.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
            }
        }
        Text(amount, style = RetailRose.metricValue().copy(fontSize = 40.sp, lineHeight = 48.sp), maxLines = 1)
        caption?.let { Text(it, style = RetailRose.rowCaption().copy(fontSize = 11.sp), maxLines = 2) }
    }
}

/** Toast after master save — Figma `✓ Saved · sheet closes and returns back`. */
@Composable
fun RoseSavedToast(modifier: Modifier = Modifier, message: String = "✓ Saved · sheet closes and returns back") {
    Row(
        modifier
            .clip(RetailRose.rPill)
            .background(RetailRose.cardSlate)
            .border(1.dp, RetailRose.amber, RetailRose.rPill)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("✓", style = RetailRose.rowCaption(RetailRose.amber).copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
        Text(
            message.removePrefix("✓ ").trim(),
            style = RetailRose.rowCaption(RetailRose.textPrimary).copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
            maxLines = 1,
        )
    }
}

@Composable
fun RoseStepper(
    quantity: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        RoseStepperButton("−", onDecrement)
        Text(quantity.toString(), style = RetailRose.rowValue().copy(fontSize = 13.sp))
        RoseStepperButton("+", onIncrement)
    }
}

@Composable
private fun RoseStepperButton(glyph: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(RetailRose.chip)
            .border(1.dp, RetailRose.roseBorder.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, style = RetailRose.rowValue().copy(fontSize = 15.sp))
    }
}

@Composable
fun RoseMetaRows(
    rows: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RetailRose.rInner)
            .background(RetailRose.field)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        rows.forEach { (label, value) ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, style = RetailRose.rowCaption(RetailRose.amber).copy(fontSize = 10.sp), maxLines = 1)
                Text(value, style = RetailRose.rowValue(), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun RoseLockedChip(label: String, modifier: Modifier = Modifier) {
    RoseModeChip(text = label, selected = false, locked = "Unavailable", modifier = modifier)
}

/** Figma denser browser CTA (`+ New stock item` / `+ New product`) under catalogue grids. */
@Composable
fun RoseDenseCreateCta(
    title: String,
    hint: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(RetailRose.rose.copy(alpha = 0.08f))
            .border(1.dp, RetailRose.rose.copy(alpha = 0.40f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = RetailRose.chipLabel(false).copy(
                color = RetailRose.amber,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            ),
        )
        Spacer(Modifier.weight(1f))
        Text(hint, style = RetailRose.rowCaption(RetailRose.textFaint), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Create | Edit existing mode chips shared by master sheets (Figma mode row). */
@Composable
fun RoseCreateEditMode(
    isCreate: Boolean,
    onCreate: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    editLocked: Boolean = false,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        RoseFieldLabel("mode")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            RoseModeChip("Create", selected = isCreate, onClick = onCreate)
            if (editLocked) {
                RoseLockedChip("Edit existing")
            } else {
                RoseModeChip("Edit existing", selected = !isCreate, onClick = onEdit)
            }
        }
    }
}

/** GST / tax total row that never invents a computed amount. */
@Composable
fun RoseUnavailableGstRow(
    label: String = "Input GST · CGST+SGST",
    caption: String = "Unavailable — stamped server-side on bill / GRN",
    modifier: Modifier = Modifier,
) {
    RoseTotalRow(
        label = label,
        value = "Unavailable",
        valueColor = RetailRose.warning,
        caption = caption,
        modifier = modifier,
    )
}

/**
 * GST estimate row — shows derived amount when rates+amounts exist; else Unavailable.
 * Estimate is UI-only; GST posts only when Charge / Purchase posts with real tax config.
 */
@Composable
fun RoseGstEstimateRow(
    label: String,
    estimate: java.math.BigDecimal?,
    currency: String,
    captionWhenKnown: String = "Estimate only — GST posts on Charge / Purchase",
    captionWhenMissing: String = "Unavailable — rate missing on line / master",
    modifier: Modifier = Modifier,
) {
    if (estimate != null) {
        RoseTotalRow(
            label = label,
            value = RetailFormat.exact(currency, estimate.stripTrailingZeros().toPlainString()),
            valueColor = RetailRose.amber,
            caption = captionWhenKnown,
            modifier = modifier,
        )
    } else {
        RoseUnavailableGstRow(
            label = label,
            caption = captionWhenMissing,
            modifier = modifier,
        )
    }
}

/**
 * On-sheet numeric keypad for Quick amount (Figma RTL-QA-01-ALT).
 * Posts the same amount string the text field would — no separate event path.
 */
@Composable
fun RoseNumericKeypad(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    maxLength: Int = 12,
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
                            .background(RetailRose.chip)
                            .border(1.dp, RetailRose.roseBorder.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .clickable {
                                onValueChange(roseKeypadApply(value, key, maxLength))
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(key, style = RetailRose.rowValue().copy(fontSize = 18.sp))
                    }
                }
            }
        }
    }
}

internal fun roseKeypadApply(current: String, key: String, maxLength: Int = 12): String {
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
fun RoseChipRow(
    label: String?,
    options: List<String>,
    selected: String,
    modifier: Modifier = Modifier,
    /** When true, selected chip fills tender green (`#22C55E`) per Figma money/method chips. */
    tenderSelection: Boolean = false,
    onSelect: (String) -> Unit,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        label?.let { RoseFieldLabel(it) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { option ->
                RoseModeChip(
                    option,
                    selected = option == selected,
                    tender = tenderSelection,
                    onClick = { onSelect(option) },
                )
            }
        }
    }
}

/** Standard slate sheet column used by every retail quick-add surface. */
@Composable
internal fun RetailSheetBody(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(RetailRose.cardSlate)
            .padding(horizontal = RetailRose.gutter, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}
