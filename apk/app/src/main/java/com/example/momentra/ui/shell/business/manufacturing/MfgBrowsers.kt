package com.example.momentra.ui.shell.business.manufacturing

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
internal fun MfgPartsBrowserSheet(
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
        mfgPartCategory(it) == cat &&
            (query.isBlank() || it.name.contains(query, true) || it.code.orEmpty().contains(query, true))
    }
    MfgSheetBody(Modifier.verticalScroll(rememberScrollState())) {
        MfgGrabber()
        MfgSheetHeader("Parts browser", "Add into SO / WO", onClose = onDismiss)
        MfgScanField(query, "Search or scan part", onQueryChange = { query = it }, onScan = {})
        MfgChipRow(null, listOf("Raw", "WIP", "Finished", "Consumables", "Spares"), cat) { cat = it }
        filtered.take(20).forEach { p ->
            MfgPickerRow(
                p.name,
                listOfNotNull(
                    p.code,
                    p.unit,
                    mfgPartCategory(p),
                    p.sellPrice?.let { MfgFormat.exact(model.currency, it) },
                ).joinToString(" · "),
                onClick = { onAdd(p) },
            )
        }
        if (filtered.isEmpty()) {
            MfgEmpty(
                if (parts.isEmpty()) "No parts" else "No $cat matches",
                if (parts.isEmpty()) "Create a part / SKU first." else "Switch chip or clear search.",
            )
        }
        MfgDenseCreateCta("+ New part / SKU", "Opens parts master", onClick = onNewPart)
        MfgWhyBox("Selecting a part prefills Order (SO / WO) — commitment only, no revenue or GST.")
        MfgStickyFooter(
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
internal fun MfgMaterialsBrowserSheet(
    model: OperatingProjectionDto,
    onDismiss: () -> Unit,
    onAdd: (OperatingMasterDto) -> Unit,
    onNewBom: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val mats = model.masters.orEmpty().filter {
        it.kind.equals("SKU", true) || it.kind.equals("PART", true) || it.kind.equals("MATERIAL", true)
    }
    MfgSheetBody(Modifier.verticalScroll(rememberScrollState())) {
        MfgGrabber()
        MfgSheetHeader("Materials browser", "On-hand for PO", onClose = onDismiss)
        MfgScanField(query, "Search materials", onQueryChange = { query = it }, onScan = {})
        mats.filter { query.isBlank() || it.name.contains(query, true) }.take(20).forEach { m ->
            MfgPickerRow(
                m.name,
                listOfNotNull(m.unit, "on-hand · Unavailable").joinToString(" · "),
                onClick = { onAdd(m) },
            )
        }
        if (mats.isEmpty()) MfgEmpty("No materials", "Add raw / consumable parts.")
        MfgDenseCreateCta("+ New BOM", "Opens BOM master", onClick = onNewBom)
        MfgWhyBox("On-hand qty stays Unavailable until a stock balance API exists. Selection still prefills PO.")
        MfgStickyFooter(
            label = "Done",
            enabled = true,
            caveat = "On-hand · Unavailable",
            onClick = onDismiss,
        )
        Spacer(Modifier.height(16.dp))
    }
}
