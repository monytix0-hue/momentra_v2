package com.example.momentra.ui.shell.business.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.OperatingMasterDto
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.theme.PlusJakartaSans

/**
 * Parts / Materials browsers for MFG + MTO.
 *
 * Figma: `MTO-SHEET · Parts browser (Quote / Job)` `2706:37762`,
 * `MTO-SHEET · Materials browser (Job PO)` `2706:37822`,
 * `MFG-SHEET · Parts browser (SO / WO)` `2547:29629`.
 *
 * Lists catalogue masters already on the projection — no invented stock math.
 */

enum class OperatingBrowserKind {
    Parts,
    Materials,
}

data class OperatingBrowserItem(
    val id: String,
    val name: String,
    val meta: String,
    val priceLabel: String?,
    val category: String,
    val unit: String? = null,
    /** Raw catalogue price (no currency glyph) for capture prefills. */
    val unitPrice: String? = null,
    val masterKind: String? = null,
    val code: String? = null,
)

/** Selection handed from Parts/Materials browser into an open or soon-to-open capture. */
data class OperatingBrowserSelection(
    val id: String,
    val name: String,
    val unit: String? = null,
    val unitPrice: String? = null,
    val masterKind: String? = null,
    val code: String? = null,
    val browserKind: OperatingBrowserKind,
    /** Quick Add mode to open when no capture is already mounted. */
    val captureHint: String,
)

fun OperatingBrowserItem.toSelection(
    browserKind: OperatingBrowserKind,
    subtype: String?,
): OperatingBrowserSelection = OperatingBrowserSelection(
    id = id,
    name = name,
    unit = unit,
    unitPrice = unitPrice,
    masterKind = masterKind,
    code = code,
    browserKind = browserKind,
    captureHint = captureHintForBrowser(browserKind, subtype),
)

fun captureHintForBrowser(kind: OperatingBrowserKind, subtype: String?): String = when (kind) {
    OperatingBrowserKind.Materials -> "PURCHASE"
    OperatingBrowserKind.Parts -> when (subtype) {
        "TRADING_WHOLESALE", "DISTRIBUTION_DEALERSHIP" -> "SALE"
        else -> "ORDER"
    }
}

@Composable
fun OperatingPartsBrowserSheet(
    model: OperatingProjectionDto,
    kind: OperatingBrowserKind,
    theme: OperatingSheetTheme,
    contextLabel: String? = null,
    onAdd: (OperatingBrowserItem) -> Unit,
    onCreateNew: () -> Unit,
    onDismiss: () -> Unit,
) {
    val accent = theme.accent
    val surface = theme.surface
    val inset = theme.inset
    val border = theme.border
    val ink = theme.ink
    val body = theme.body

    val categories = when (kind) {
        OperatingBrowserKind.Parts -> listOf("Raw", "WIP", "Jobs", "Consumables", "Spares")
        OperatingBrowserKind.Materials -> listOf("Raw", "Consumables", "Finished", "Packing", "Spares")
    }
    var query by remember { mutableStateOf("") }
    var selectedCat by remember { mutableStateOf(categories.first()) }

    val items = remember(model, kind) { browserItemsFromProjection(model, kind) }
    val filtered = items.filter { item ->
        val q = query.trim()
        val matchesQuery = q.isEmpty() ||
            item.name.contains(q, ignoreCase = true) ||
            item.meta.contains(q, ignoreCase = true)
        val hasSelectedCat = items.any { it.category.equals(selectedCat, true) }
        val matchesCat = !hasSelectedCat ||
            selectedCat == categories.first() ||
            item.category.equals(selectedCat, ignoreCase = true)
        matchesQuery && matchesCat
    }

    val title = when (kind) {
        OperatingBrowserKind.Parts -> "Parts & products"
        OperatingBrowserKind.Materials -> "Materials"
    }
    val hint = when (kind) {
        OperatingBrowserKind.Parts -> "Tap Add · qty\nper quote / job line"
        OperatingBrowserKind.Materials -> contextLabel ?: "Job materials + on-hand"
    }
    val searchPlaceholder = when (kind) {
        OperatingBrowserKind.Parts -> "Search part, drawing or HSN"
        OperatingBrowserKind.Materials -> "Search material or HSN"
    }
    val createLabel = when (kind) {
        OperatingBrowserKind.Parts -> "+ New part / SKU"
        OperatingBrowserKind.Materials -> "+ New BOM"
    }
    val createHint = when (kind) {
        OperatingBrowserKind.Parts -> "Name · HSN · GST · UOM"
        OperatingBrowserKind.Materials -> "Components · qty · scrap %"
    }

    val sheetShape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(max = 640.dp)
            .clip(sheetShape)
            .background(surface)
            .border(1.dp, accent.copy(alpha = 0.40f), sheetShape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = ink, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(hint, color = body, fontFamily = PlusJakartaSans, fontSize = 11.sp)
            }
            Box(
                Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(inset)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Text("✕", color = body, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(inset)
                .border(1.dp, border, RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = TextStyle(
                    color = ink,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                ),
                cursorBrush = SolidColor(accent),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text(searchPlaceholder, color = body, fontFamily = PlusJakartaSans, fontSize = 13.sp)
                    }
                    inner()
                },
            )
            Text(
                "Scan",
                color = accent,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
            )
        }

        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            categories.forEach { cat ->
                val selected = cat == selectedCat
                Box(
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (selected) accent else inset)
                        .border(1.dp, if (selected) accent else border, RoundedCornerShape(999.dp))
                        .clickable { selectedCat = cat }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        cat,
                        color = if (selected) Color(0xFF16181F) else body,
                        fontFamily = PlusJakartaSans,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp,
                    )
                }
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (filtered.isEmpty()) {
                Text(
                    "No parts recorded yet — add a catalogue SKU, or create one below. Missing stock never shows as 0.",
                    color = body,
                    fontFamily = PlusJakartaSans,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            } else {
                filtered.forEach { item ->
                    BrowserRow(
                        item = item,
                        accent = accent,
                        inset = inset,
                        ink = ink,
                        body = body,
                        onAdd = { onAdd(item) },
                        onSelect = { onAdd(item) },
                    )
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.08f))
                .border(1.dp, accent.copy(alpha = 0.40f), RoundedCornerShape(12.dp))
                .clickable(onClick = onCreateNew)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(createLabel, color = accent, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(Modifier.weight(1f))
            Text(createHint, color = body, fontFamily = PlusJakartaSans, fontSize = 11.sp)
        }
    }
}

@Composable
private fun BrowserRow(
    item: OperatingBrowserItem,
    accent: Color,
    inset: Color,
    ink: Color,
    body: Color,
    onAdd: () -> Unit,
    onSelect: () -> Unit = onAdd,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(inset)
            .border(1.dp, accent.copy(alpha = 0.30f), RoundedCornerShape(14.dp))
            .clickable(onClick = onSelect)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                item.name,
                color = ink,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                item.meta,
                color = body,
                fontFamily = PlusJakartaSans,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!item.priceLabel.isNullOrBlank()) {
            Text(
                item.priceLabel,
                color = accent,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            )
        }
        Box(
            Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(accent)
                .clickable(onClick = onAdd)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text("Add", color = Color(0xFF16181F), fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

internal fun browserItemsFromProjection(
    model: OperatingProjectionDto,
    kind: OperatingBrowserKind,
): List<OperatingBrowserItem> {
    val masters = model.masters.orEmpty()
    val kinds = when (kind) {
        OperatingBrowserKind.Parts -> setOf("SKU", "PRODUCT", "BATCH", "PART", "BOM")
        OperatingBrowserKind.Materials -> setOf("SKU", "PRODUCT", "BATCH", "MATERIAL", "RAW")
    }
    return masters
        .filter { it.kind?.uppercase() in kinds || it.kind.isNullOrBlank() }
        .map { it.toBrowserItem() }
}

private fun OperatingMasterDto.toBrowserItem(): OperatingBrowserItem {
    val category = when {
        kind.equals("BATCH", true) -> "WIP"
        name.contains("wip", true) -> "WIP"
        name.contains("spare", true) -> "Spares"
        name.contains("pack", true) -> "Packing"
        name.contains("consum", true) -> "Consumables"
        else -> "Raw"
    }
    val rawPrice = listOfNotNull(sellPrice, purchasePrice, mrp)
        .firstOrNull { !it.isNullOrBlank() }
        ?.removePrefix("₹")
        ?.trim()
    val metaParts = listOfNotNull(
        unit?.takeIf { it.isNotBlank() },
        code?.takeIf { it.isNotBlank() },
        hsn?.takeIf { it.isNotBlank() }?.let { "HSN $it" },
        gstRate?.takeIf { it.isNotBlank() }?.let { if (it.endsWith("%")) it else "$it%" },
        // Honest FEFO: masters expose batch identity, not expiry math.
        when {
            kind.equals("BATCH", true) -> "FEFO · Unavailable"
            else -> null
        },
    )
    return OperatingBrowserItem(
        id = id,
        name = name.ifBlank { "Unnamed part" },
        meta = metaParts.joinToString(" · ").ifBlank { "Catalogue · details unavailable" },
        priceLabel = rawPrice?.let { if (it.startsWith("₹")) it else "₹$it" },
        category = category,
        unit = unit?.takeIf { it.isNotBlank() },
        unitPrice = rawPrice?.takeIf { it.isNotBlank() },
        masterKind = kind.takeIf { it.isNotBlank() },
        code = code?.takeIf { it.isNotBlank() },
    )
}

/** Projection FEFO line for a selected catalogue master — never invents an expiry date. */
fun fefoFactLabel(master: OperatingMasterDto?): String {
    if (master == null) return "Unavailable"
    val batch = master.kind.equals("BATCH", ignoreCase = true)
    val code = master.code?.takeIf { it.isNotBlank() }
    return when {
        batch && code != null -> "Batch $code · expiry Unavailable"
        batch -> "Batch · expiry Unavailable"
        code != null -> "SKU $code · FEFO Unavailable"
        else -> "Unavailable"
    }
}
