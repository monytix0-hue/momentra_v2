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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.ui.theme.PlusJakartaSans
import com.example.momentra.ui.theme.RetailRose

/** Rose grab handle on slate sheets (`#1e293b`). */
@Composable
fun RoseGrabber(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().height(22.dp), horizontalArrangement = Arrangement.Center) {
        Box(
            Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(RetailRose.rose.copy(alpha = 0.55f)),
        )
    }
}

@Composable
fun RoseSheetHeader(
    title: String,
    subtitle: String?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                title,
                style = RetailRose.screenTitle().copy(fontSize = 20.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Box(
                Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Text("✕", style = RetailRose.rowValue(RetailRose.textBody).copy(fontSize = 12.sp))
            }
        }
        subtitle?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = RetailRose.rowCaption(), maxLines = 2)
        }
    }
}

/** Amber section label above fields. */
@Composable
fun RoseFieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = RetailRose.metricLabel().copy(color = RetailRose.amber, fontSize = 10.sp, letterSpacing = 0.4.sp),
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
) {
    val shape = RetailRose.rPill
    val enabled = locked == null
    val click = if (onClick != null && enabled) Modifier.clickable(onClick = onClick) else Modifier
    Text(
        text,
        style = RetailRose.chipLabel(selected).copy(
            color = when {
                locked != null -> RetailRose.textFaint
                selected -> Color.White
                else -> RetailRose.textBody
            },
        ),
        maxLines = 1,
        modifier = modifier
            .clip(shape)
            .background(if (selected) RetailRose.rose else RetailRose.chip)
            .border(
                1.dp,
                when {
                    locked != null -> RetailRose.chipStroke
                    selected -> RetailRose.rose
                    else -> RetailRose.roseBorder.copy(alpha = 0.35f)
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
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) {
            RoseTextField(value = query, placeholder = placeholder, onValueChange = onQueryChange)
        }
        Text(
            "Scan",
            style = RetailRose.chipLabel(true).copy(color = Color.White),
            maxLines = 1,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(RetailRose.rose)
                .clickable(onClick = onScan)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        )
    }
}

/** Section card with rose border on slate. */
@Composable
fun RoseSectionCard(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RoseFieldLabel(label)
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RetailRose.rCardTight)
                .background(RetailRose.cardSlate)
                .border(1.dp, RetailRose.rose.copy(alpha = 0.4f), RetailRose.rCardTight)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

@Composable
fun RoseLinesCard(
    title: String,
    lineCount: Int,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    RoseSectionCard(label = title, modifier = modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (lineCount == 0) "No lines yet" else "$lineCount line${if (lineCount == 1) "" else "s"}",
                style = RetailRose.rowCaption(),
                modifier = Modifier.weight(1f),
            )
        }
        content()
    }
}

@Composable
fun RoseTotalsCard(
    payableLabel: String,
    payable: String,
    tender: String?,
    modifier: Modifier = Modifier,
    rows: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RetailRose.rCardTight)
            .background(Color.Black.copy(alpha = 0.24f))
            .border(1.dp, RetailRose.rose.copy(alpha = 0.35f), RetailRose.rCardTight)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        rows()
        Box(Modifier.fillMaxWidth().height(1.dp).background(RetailRose.divider))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(payableLabel, style = RetailRose.rowCaption().copy(fontSize = 10.sp))
                Text(payable, style = RetailRose.metricValue().copy(fontSize = 26.sp), maxLines = 1)
            }
            tender?.let {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text("Tender", style = RetailRose.rowCaption().copy(fontSize = 10.sp))
                    Text(it, style = RetailRose.rowValue(RetailRose.amber), maxLines = 1)
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
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RetailRose.rInner)
            .background(RetailRose.field)
            .border(1.dp, RetailRose.roseBorder.copy(alpha = 0.3f), RetailRose.rInner)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = RetailRose.rowCaption(RetailRose.amber).copy(fontSize = 10.sp), maxLines = 1)
            Text(value, style = RetailRose.rowTitle(), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text("›", style = RetailRose.rowValue(RetailRose.textFaint), maxLines = 1)
    }
}

@Composable
fun RoseMoreDetails(
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RetailRose.rInner)
                .clickable(onClick = onToggle)
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (expanded) "Hide details" else "More details",
                style = RetailRose.viewAll(),
                modifier = Modifier.weight(1f),
            )
            Text(if (expanded) "▲" else "▼", style = RetailRose.rowCaption(RetailRose.textFaint))
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

@Composable
fun RoseStickyFooter(
    label: String,
    enabled: Boolean,
    caveat: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val shape = RetailRose.rRow
        Text(
            label,
            style = RetailRose.storyCtaTitle().copy(color = if (enabled) Color.White else RetailRose.textFaint),
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .height(51.dp)
                .shadow(
                    elevation = if (enabled) 10.dp else 0.dp,
                    shape = shape,
                    clip = false,
                    ambientColor = RetailRose.rose.copy(alpha = 0.45f),
                    spotColor = RetailRose.rose.copy(alpha = 0.45f),
                )
                .clip(shape)
                .background(if (enabled) RetailRose.rose else RetailRose.surfaceRow)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(vertical = 15.dp, horizontal = 12.dp),
        )
        Text(
            caveat,
            style = RetailRose.rowCaption(RetailRose.textFaint).copy(fontSize = 10.sp),
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
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RetailRose.rCardTight)
            .background(RetailRose.rose.copy(alpha = 0.12f))
            .border(1.dp, RetailRose.rose.copy(alpha = 0.45f), RetailRose.rCardTight)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, style = RetailRose.rowCaption(RetailRose.amber), maxLines = 1)
        Text(amount, style = RetailRose.metricValue().copy(fontSize = 28.sp), maxLines = 1)
        caption?.let { Text(it, style = RetailRose.rowCaption(), maxLines = 2) }
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
    onSelect: (String) -> Unit,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        label?.let { RoseFieldLabel(it) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { option ->
                RoseModeChip(option, selected = option == selected, onClick = { onSelect(option) })
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
