package com.example.momentra.ui.shell.business.mto

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.momentra.data.api.OperatingMasterDto
import com.example.momentra.data.api.OperatingProjectionDto

/** Figma `2547:29629` Parts browser (SO / WO). */
@Composable
internal fun MtoPartsBrowserSheet(
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onAdd: (OperatingMasterDto) -> Unit,
    onNewPart: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf("Finished") }
    val parts = model.masters.orEmpty().filter {
        it.kind.equals("SKU", true) || it.kind.equals("PART", true) || it.kind.equals("PRODUCT", true)
    }
    val filtered = parts.filter {
        mtoPartCategory(it) == cat &&
            (query.isBlank() || it.name.contains(query, true) || it.code.orEmpty().contains(query, true))
    }
    MtoSheetBody(Modifier.verticalScroll(rememberScrollState())) {
        MtoGrabber()
        MtoSheetHeader("Parts browser", "Add into SO / WO", onClose = onDismiss)
        MtoScanField(query, "Search or scan part", onQueryChange = { query = it }, onScan = {})
        MtoChipRow(null, listOf("Raw", "WIP", "Finished", "Consumables", "Spares"), cat) { cat = it }
        filtered.take(20).forEach { p ->
            MtoPickerRow(
                p.name,
                listOfNotNull(
                    p.code,
                    p.unit,
                    mtoPartCategory(p),
                    p.sellPrice?.let { MtoFormat.exact(model.currency, it) },
                ).joinToString(" · "),
                onClick = { onAdd(p) },
            )
        }
        if (filtered.isEmpty()) {
            MtoEmpty(
                if (parts.isEmpty()) "No parts" else "No $cat matches",
                if (parts.isEmpty()) "Create a part / SKU first." else "Switch chip or clear search.",
            )
        }
        MtoDenseCreateCta("+ New part / SKU", "Opens parts master", onClick = onNewPart)
        MtoWhyNote("Selecting a part prefills Order (SO / WO) — commitment only, no revenue or GST.")
        MtoStickyFooter(
            label = "Done",
            enabled = true,
            caveat = "${filtered.size} of ${parts.size} · $cat",
            onClick = onDismiss,
        )
        Spacer(Modifier.height(16.dp))
    }
}

/** Figma `2547:29687` Materials browser (PO). */
@Composable
internal fun MtoMaterialsBrowserSheet(
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onAdd: (OperatingMasterDto) -> Unit,
    onNewBom: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val mats = model.masters.orEmpty().filter {
        it.kind.equals("SKU", true) || it.kind.equals("PART", true) || it.kind.equals("MATERIAL", true)
    }
    MtoSheetBody(Modifier.verticalScroll(rememberScrollState())) {
        MtoGrabber()
        MtoSheetHeader("Materials browser", "On-hand for PO", onClose = onDismiss)
        MtoScanField(query, "Search materials", onQueryChange = { query = it }, onScan = {})
        mats.filter { query.isBlank() || it.name.contains(query, true) }.take(20).forEach { m ->
            MtoPickerRow(
                m.name,
                listOfNotNull(m.unit, "on-hand · Unavailable").joinToString(" · "),
                onClick = { onAdd(m) },
            )
        }
        if (mats.isEmpty()) MtoEmpty("No materials", "Add raw / consumable parts.")
        MtoDenseCreateCta("+ New BOM", "Opens BOM master", onClick = onNewBom)
        MtoWhyNote("On-hand qty stays Unavailable until a stock balance API exists. Selection still prefills PO.")
        MtoStickyFooter(
            label = "Done",
            enabled = true,
            caveat = "On-hand · Unavailable",
            onClick = onDismiss,
        )
        Spacer(Modifier.height(16.dp))
    }
}
