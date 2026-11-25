package com.example.momentra.ui.shell.business.shared

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.momentra.R
import com.example.momentra.data.api.OperatingActivityDto
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.data.repository.BusinessSliceRepository
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.launch

/**
 * Shared chrome host for Trading / Distribution Active kits — masters + Memory story +
 * Activity row Edit/Delete (revise / reverse).
 */
@Composable
fun OperatingChromeSheets(
    model: OperatingProjectionDto,
    companyId: String? = null,
) {
    val theme = remember(OperatingChromeState.distributionChrome) {
        if (OperatingChromeState.distributionChrome) {
            // DST-QA / DST-STORY sheet — Dist orange on `#13203A` (parity with DistSheetTheme).
            OperatingSheetTheme(
                accent = Color(0xFFEA580C),
                accentSoft = Color(0xFFFFB598),
                canvas = Color(0xFF13203A),
                surface = Color(0xFF13203A),
                inset = Color(0xFF24365A),
                border = Color(0xFF2A3D63),
                ink = Color.White,
                body = Color(0xFF94A4B8),
                warning = Color(0xFFF59E0B),
                negative = Color(0xFFF87171),
            )
        } else {
            OperatingChromeState.storyTheme()
        }
    }
    val scope = rememberCoroutineScope()
    val repo = remember { BusinessSliceRepository() }
    var actionError by remember { mutableStateOf<String?>(null) }

    val mastersTab = OperatingChromeState.mastersOpen
    if (!mastersTab.isNullOrBlank()) {
        val tab = masterTabForLabel(mastersTab)
        val editId = OperatingChromeState.mastersEditId
        OperatingSheetScrim(
            onDismiss = {
                OperatingChromeState.mastersOpen = null
                OperatingChromeState.mastersEditId = null
            },
            accent = theme.accent,
            surface = theme.surface,
            border = theme.border,
        ) {
            OperatingMastersSheet(
                tab = tab,
                model = model,
                context = OperatingMasterContext(
                    companyId = companyId,
                    momentId = OperatingChromeState.momentId,
                    onChanged = { OperatingChromeState.refresh += 1 },
                ),
                theme = theme,
                onClose = {
                    OperatingChromeState.mastersOpen = null
                    OperatingChromeState.mastersEditId = null
                },
                initialForm = if (mastersTab.contains("SKU", ignoreCase = true) ||
                    mastersTab.contains("Create", ignoreCase = true)
                ) {
                    OperatingMasterTab.Catalogue
                } else {
                    null
                },
                initialEditingId = editId,
            )
        }
    }

    if (OperatingChromeState.storyConfigOpen) {
        val momentId = OperatingChromeState.momentId
        if (!momentId.isNullOrBlank()) {
            OperatingSheetScrim(
                onDismiss = { OperatingChromeState.storyConfigOpen = false },
                accent = theme.accent,
                surface = theme.surface,
                border = theme.border,
            ) {
                OperatingStoryConfigSheet(
                    model = model,
                    momentId = momentId,
                    theme = theme,
                    noun = if (OperatingChromeState.distributionChrome) "distribution" else "trading",
                    chapters = if (OperatingChromeState.distributionChrome) {
                        DISTRIBUTION_STORY_PAGES
                    } else {
                        TRADING_STORY_PAGES
                    },
                    onDismiss = { OperatingChromeState.storyConfigOpen = false },
                )
            }
        } else {
            OperatingChromeState.storyConfigOpen = false
        }
    }

    OperatingChromeState.partsBrowserOpen?.let { kindName ->
        val kind = when (kindName.lowercase()) {
            "materials" -> OperatingBrowserKind.Materials
            "services" -> OperatingBrowserKind.Services
            else -> OperatingBrowserKind.Parts
        }
        OperatingSheetScrim(
            onDismiss = { OperatingChromeState.partsBrowserOpen = null },
            accent = theme.accent,
            surface = theme.surface,
            border = theme.border,
        ) {
            OperatingPartsBrowserSheet(
                model = model,
                kind = kind,
                theme = theme,
                onAdd = { item ->
                    OperatingChromeState.browserSelection = item.toSelection(kind, model.subtype)
                    OperatingChromeState.partsBrowserOpen = null
                    if (!OperatingChromeState.captureOpen) {
                        OperatingChromeState.quickAddHint =
                            OperatingChromeState.browserSelection?.captureHint
                    }
                },
                onCreateNew = {
                    OperatingChromeState.partsBrowserOpen = null
                    OperatingChromeState.mastersOpen = "Catalogue"
                },
                onDismiss = { OperatingChromeState.partsBrowserOpen = null },
            )
        }
    }

    val editingRow = OperatingChromeState.editing
    if (editingRow != null &&
        OperatingChromeState.reviseCapture == null &&
        OperatingChromeState.reverseConfirm == null
    ) {
        AlertDialog(
            onDismissRequest = { OperatingChromeState.editing = null },
            title = {
                Text(
                    editingRow.title.ifBlank { editingRow.eventType },
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            text = {
                Text(
                    listOfNotNull(
                        editingRow.eventType.takeIf { it.isNotBlank() },
                        editingRow.amount?.takeIf { it.isNotBlank() }?.let { "₹$it" },
                        editingRow.counterpartyName?.takeIf { it.isNotBlank() },
                    ).joinToString(" · ").ifBlank { "Edit or reverse this activity row." },
                    fontFamily = PlusJakartaSans,
                    fontSize = 13.sp,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val row = OperatingChromeState.editing ?: return@TextButton
                        OperatingChromeState.editing = null
                        when {
                            isMasterLinkedActivity(row, model) -> openMasterEdit(row, model)
                            isRevisableActivity(row) -> OperatingChromeState.reviseCapture = row
                            else -> actionError = "This row cannot be edited here."
                        }
                    },
                ) {
                    Text("Edit", color = theme.accent, fontFamily = PlusJakartaSans)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val row = OperatingChromeState.editing ?: return@TextButton
                        OperatingChromeState.editing = null
                        when {
                            isMasterLinkedActivity(row, model) -> openMasterEdit(row, model)
                            isRevisableActivity(row) -> OperatingChromeState.reverseConfirm = row
                            else -> actionError = "This row cannot be reversed."
                        }
                    },
                ) {
                    Text("Delete", color = theme.negative, fontFamily = PlusJakartaSans)
                }
            },
        )
    }

    val reverseRow = OperatingChromeState.reverseConfirm
    if (reverseRow != null) {
        AlertDialog(
            onDismissRequest = { OperatingChromeState.reverseConfirm = null },
            title = {
                Text("Reverse this record?", fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold)
            },
            text = {
                Text(
                    "“${reverseRow.title}” stays in the timeline as reversed. Money and stock effects are undone — this cannot hard-delete the original.",
                    fontFamily = PlusJakartaSans,
                    fontSize = 13.sp,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val row = OperatingChromeState.reverseConfirm ?: return@TextButton
                        val momentId = OperatingChromeState.momentId
                        if (momentId.isNullOrBlank()) {
                            actionError = "Moment missing."
                            return@TextButton
                        }
                        OperatingChromeState.reverseConfirm = null
                        scope.launch {
                            repo.recordOperatingEvent(
                                momentId = momentId,
                                eventType = row.eventType.ifBlank { "SALE" },
                                title = row.title.ifBlank { "Reversed" },
                                amount = "0",
                                note = "Reversed from activity",
                                channelName = row.channelName,
                                counterpartyName = row.counterpartyName,
                                quantity = "0",
                                unit = row.unit,
                                parentEventId = row.parentEventId,
                                revisesEventId = row.id,
                                activityStatus = "CANCELLED",
                            ).fold(
                                onSuccess = {
                                    OperatingProjectionCache.invalidate(momentId)
                                    OperatingChromeState.refresh += 1
                                },
                                onFailure = { actionError = it.message ?: "Could not reverse" },
                            )
                        }
                    },
                ) {
                    Text("Reverse", color = theme.negative, fontFamily = PlusJakartaSans)
                }
            },
            dismissButton = {
                TextButton(onClick = { OperatingChromeState.reverseConfirm = null }) {
                    Text("Cancel", color = theme.body, fontFamily = PlusJakartaSans)
                }
            },
        )
    }

    val reviseRow = OperatingChromeState.reviseCapture
    if (reviseRow != null) {
        val mode = captureModeForActivity(reviseRow)
        val variant = when {
            OperatingChromeState.distributionChrome -> "distribution"
            else -> "trading"
        }
        OperatingSheetScrim(
            onDismiss = { OperatingChromeState.reviseCapture = null },
            accent = theme.accent,
            surface = theme.surface,
            border = theme.border,
            expandFully = true,
        ) {
            OperatingCaptureSheet(
                title = "Edit ${reviseRow.eventType.lowercase().replaceFirstChar { it.titlecase() }}",
                icon = R.drawable.qa_receipt,
                eventType = reviseRow.eventType.ifBlank { mode },
                mode = mode,
                model = model,
                variant = variant,
                seed = reviseRow,
                onDismiss = { OperatingChromeState.reviseCapture = null },
                onSubmit = { draft ->
                    val momentId = OperatingChromeState.momentId
                        ?: throw IllegalStateException("Moment missing")
                    val result = repo.recordOperatingEvent(
                        momentId = momentId,
                        eventType = draft.eventType,
                        title = draft.title,
                        amount = draft.amount,
                        note = draft.note,
                        channelName = draft.channelName,
                        counterpartyName = draft.counterpartyName,
                        paymentStatus = draft.paymentStatus,
                        occurredAt = draft.occurredAt,
                        quantity = draft.quantity,
                        unit = draft.unit,
                        parentEventId = draft.parentEventId,
                        revisesEventId = draft.revisesEventId ?: reviseRow.id,
                        dueAt = draft.dueAt,
                        contract = draft.contract,
                        lines = draft.lines,
                        activityStatus = draft.activityStatus,
                    )
                    result.getOrThrow()
                    OperatingProjectionCache.invalidate(momentId)
                    OperatingChromeState.refresh += 1
                    OperatingChromeState.reviseCapture = null
                },
            )
        }
    }

    actionError?.let { msg ->
        AlertDialog(
            onDismissRequest = { actionError = null },
            title = { Text("Couldn’t update", fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold) },
            text = { Text(msg, fontFamily = PlusJakartaSans, fontSize = 13.sp) },
            confirmButton = {
                TextButton(onClick = { actionError = null }) {
                    Text("OK", color = theme.accent, fontFamily = PlusJakartaSans)
                }
            },
        )
    }
}

private fun isRevisableActivity(row: OperatingActivityDto): Boolean {
    val type = row.eventType.uppercase()
    if (type == "REVERSAL" || type == "CORRECTION") return false
    return type in setOf(
        "SALE", "PURCHASE", "SPEND", "PAYMENT", "COLLECTION", "STOCK",
        "ORDER", "SHIPMENT", "DELIVERY", "RETURN", "UPDATE", "ISSUE",
        "SERVICE", "APPOINTMENT", "BUDGET", "TARGET",
    )
}

private fun isMasterLinkedActivity(row: OperatingActivityDto, model: OperatingProjectionDto): Boolean {
    val type = row.eventType.uppercase()
    if (type !in setOf("UPDATE", "SKU", "PRODUCT", "MASTERS")) return false
    val name = row.title.trim().lowercase()
    if (name.isEmpty()) return false
    return model.masters.orEmpty().any {
        it.name.trim().equals(row.title.trim(), ignoreCase = true) &&
            it.kind.uppercase() in setOf("PRODUCT", "SKU", "PART", "MATERIAL", "SERVICE")
    }
}

private fun openMasterEdit(row: OperatingActivityDto, model: OperatingProjectionDto) {
    val master = model.masters.orEmpty().firstOrNull {
        it.name.trim().equals(row.title.trim(), ignoreCase = true) &&
            it.kind.uppercase() in setOf("PRODUCT", "SKU", "PART", "MATERIAL", "SERVICE")
    }
    OperatingChromeState.mastersEditId = master?.id
    OperatingChromeState.mastersOpen = "Catalogue"
}

private fun captureModeForActivity(row: OperatingActivityDto): String {
    val type = row.eventType.uppercase()
    return when (type) {
        "SPEND" -> "SPEND"
        "PAYMENT" -> "PAYMENT"
        "COLLECTION" -> "COLLECTION"
        "PURCHASE", "VENDOR" -> "PURCHASE"
        "STOCK", "TRANSFER" -> "STOCK"
        "ORDER" -> "ORDER"
        "SHIPMENT", "DELIVERY" -> "SHIPMENT"
        "RETURN" -> "RETURN"
        "ISSUE" -> "ISSUE"
        "SERVICE" -> "SERVICE"
        "APPOINTMENT" -> "APPOINTMENT"
        "BUDGET" -> "BUDGET"
        "TARGET" -> "TARGET"
        else -> "SALE"
    }
}

private fun masterTabForLabel(label: String): OperatingMasterTab {
    val key = label.trim().lowercase()
    return when {
        key.contains("invite") || key.contains("qr") -> OperatingMasterTab.Invite
        key.contains("price") -> OperatingMasterTab.PriceLists
        key.contains("warehouse") || key.contains("location") || key.contains("route") ->
            OperatingMasterTab.Locations
        key.contains("catalogue") || key.contains("sku") || key.contains("product") ->
            OperatingMasterTab.Catalogue
        else -> OperatingMasterTab.Principals
    }
}
