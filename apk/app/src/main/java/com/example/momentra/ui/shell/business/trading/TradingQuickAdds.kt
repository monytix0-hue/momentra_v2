package com.example.momentra.ui.shell.business.trading

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.shell.business.mto.MtoCaptureCopy
import com.example.momentra.ui.shell.business.mto.MtoToastHost
import com.example.momentra.ui.shell.business.mto.MtoToastKind
import com.example.momentra.ui.shell.business.mto.MtoToastSpec
import com.example.momentra.ui.shell.business.shared.BusinessMomentFamilyConfig
import com.example.momentra.ui.shell.business.shared.OperatingCaptureDraft
import com.example.momentra.ui.shell.business.shared.OperatingCaptureSheet
import com.example.momentra.ui.shell.business.shared.OperatingChromeState
import com.example.momentra.ui.shell.business.shared.OperatingSheetScrim
import com.example.momentra.ui.shell.business.shared.OperatingTab
import com.example.momentra.ui.shell.business.shared.OperatingV12HubScreen
import com.example.momentra.ui.shell.business.shared.OperatingV12Theme
import com.example.momentra.ui.theme.PlusJakartaSans

data class TradingEventDraft(
    val eventType: String,
    val title: String,
    val amount: String?,
    val note: String?,
    val channelName: String?,
    val counterpartyName: String?,
    val paymentStatus: String?,
    val occurredAt: String? = null,
    val quantity: String? = null,
    val unit: String? = null,
    val unitPrice: String? = null,
    val itemName: String? = null,
    val parentEventId: String? = null,
    val revisesEventId: String? = null,
    val masterKind: String? = null,
    val conversionToBase: String? = null,
    val dueAt: String? = null,
    val contract: com.example.momentra.data.api.OperatingContractBody? = null,
    val masterId: String? = null,
    val masterGstApplicable: String? = null,
    val masterGstClassification: String? = null,
    val masterGstRate: String? = null,
    val masterPriceIncludesGst: String? = null,
    val masterSalePriceIncludesGst: String? = null,
    val masterPurchasePriceIncludesGst: String? = null,
    val masterPurchasePrice: String? = null,
    val masterGstEffectiveFrom: String? = null,
    val masterHsn: String? = null,
    val masterGstRegistration: String? = null,
    val masterBusinessState: String? = null,
    val masterBillingState: String? = null,
    val masterDeliveryState: String? = null,
    val masterGstin: String? = null,
    val lines: List<com.example.momentra.data.api.OperatingLineBody>? = null,
)

/** Titles match Figma TRD-QA sheet headers. */
private enum class TradingSheet(
    val title: String,
    val eventType: String,
) {
    SALE("Record Sale", "SALE"),
    PURCHASE("Record Purchase", "PURCHASE"),
    SPEND("Record Expense", "SPEND"),
    PAYMENT("Record Payment", "PAYMENT"),
    COLLECTION("Record Collection", "COLLECTION"),
    BUDGET("Budget / Target Revision", "BUDGET"),
    TARGET("Budget / Target Revision", "TARGET"),
    UPDATE("Post Update / Issue", "UPDATE"),
    ISSUE("Post Update / Issue", "ISSUE"),
    STOCK("Stock Update", "STOCK"),
    ORDER("Order", "ORDER"),
    SHIPMENT("Delivery", "SHIPMENT"),
    DELIVERY("Delivery", "DELIVERY"),
    RETURN("Return / Credit Note", "RETURN"),
    REIMBURSE("Approval / Reimbursement", "PAYMENT"),
    PERSON("Add or invite person", "UPDATE"),
    ROLE("Add or invite person", "UPDATE"),
    INVITE("Add or invite person", "UPDATE"),
    SUPPLIER("Add Supplier", "UPDATE"),
    HOLD("Hold a record", "ORDER"),
    TRANSFER("Transfer stock", "STOCK"),
    REPRINT("Reprint a document", "SALE"),
}

@Composable
fun TradingQuickAddHub(
    model: OperatingProjectionDto,
    onRecord: suspend (TradingEventDraft) -> Unit,
    onDecide: suspend (String, String) -> Unit,
    @Suppress("UNUSED_PARAMETER") companyId: String? = null,
    momentId: String? = null,
) {
    val allowed = model.quickAdds.map { it.code }.toSet()
    var sheet by remember { mutableStateOf<TradingSheet?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var approvalOpen by remember { mutableStateOf(false) }
    val theme = remember { OperatingV12Theme.tradingForScreen(OperatingTab.QUICK_ADD) }

    fun resolveSheet(code: String): TradingSheet? {
        val hint = code.uppercase()
        return TradingSheet.entries.firstOrNull { it.captureMode == hint || it.name == hint }
            ?: when (hint) {
                "SHIPMENT", "DELIVERY" -> TradingSheet.SHIPMENT
                "STOCK" -> TradingSheet.STOCK
                "BUDGET", "TARGET" -> TradingSheet.BUDGET
                "SALE" -> TradingSheet.SALE
                "PURCHASE" -> TradingSheet.PURCHASE
                "COLLECTION" -> TradingSheet.COLLECTION
                "PAYMENT" -> TradingSheet.PAYMENT
                "SPEND", "EXPENSE" -> TradingSheet.SPEND
                "ORDER" -> TradingSheet.ORDER
                "UPDATE", "ISSUE" -> TradingSheet.UPDATE
                "RETURN" -> TradingSheet.RETURN
                "REPRINT" -> TradingSheet.REPRINT
                "HOLD" -> TradingSheet.HOLD
                "TRANSFER" -> TradingSheet.TRANSFER
                "APPROVAL", "REIMBURSE" -> null // handled via approvalOpen
                else -> null
            }
    }

    fun openFromCode(code: String) {
        if (code.equals("APPROVAL", ignoreCase = true) || code.equals("REIMBURSE", ignoreCase = true)) {
            approvalOpen = true
        } else {
            sheet = resolveSheet(code) ?: TradingSheet.UPDATE
        }
    }

    LaunchedEffect(momentId) {
        val pending = momentId?.let { BusinessMomentFamilyConfig.consumePendingQuickAdd(it) }
        if (!pending.isNullOrBlank()) openFromCode(pending)
    }
    LaunchedEffect(OperatingChromeState.quickAddHint) {
        val hint = OperatingChromeState.quickAddHint ?: return@LaunchedEffect
        OperatingChromeState.quickAddHint = null
        openFromCode(hint)
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OperatingV12HubScreen(
                model = model,
                theme = theme,
                onCapture = { code -> openFromCode(code) },
                onOpenMasters = { tabName -> OperatingChromeState.mastersOpen = tabName },
            )
            if (error != null) {
                Text(error ?: "", color = Color(0xFFF87171), fontFamily = PlusJakartaSans, fontSize = 13.sp)
            }
            Text(
                "Review approvals",
                color = TradeAccent,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { approvalOpen = true }.padding(vertical = 4.dp),
            )
        }

        val createMaster: suspend (String, String, String?, String?) -> Unit = { kind, name, unit, conversion ->
            onRecord(
                TradingEventDraft(
                    eventType = "UPDATE",
                    title = name,
                    amount = null,
                    note = null,
                    channelName = null,
                    counterpartyName = null,
                    paymentStatus = null,
                    unit = unit,
                    itemName = name,
                    masterKind = kind,
                    conversionToBase = conversion,
                ),
            )
        }

        suspend fun submitDraft(draft: TradingEventDraft, dismiss: () -> Unit) {
            runCatching { onRecord(draft) }
                .onSuccess {
                    MtoToastHost.show(
                        MtoToastSpec(
                            kind = MtoToastKind.Saved,
                            title = MtoCaptureCopy.SAVED_TOAST,
                            body = "Changes recorded · ${model.title ?: "trading"} updated",
                            actionLabel = "Done",
                            accent = TradeBrand,
                        ),
                    )
                    dismiss()
                }
                .onFailure { error = it.message ?: "Could not save" }
        }

        val open = sheet
        if (open != null) {
            OperatingSheetScrim(
                onDismiss = { sheet = null },
                accent = TradeBrand,
                surface = TradeSheetSurface,
                border = Color.White.copy(alpha = 0.08f),
                expandFully = true,
            ) {
                OperatingCaptureSheet(
                    title = open.title,
                    icon = open.hubIcon,
                    eventType = open.eventType,
                    mode = open.captureMode,
                    model = model,
                    channelLabel = when (open.captureMode) {
                        "SALE" -> "Channel (C · if enabled)"
                        "COLLECTION" -> "Customer"
                        else -> "Channel"
                    },
                    variant = "trading",
                    onDismiss = { sheet = null },
                    onSubmit = { draft ->
                        submitDraft(draft.toTrading()) { sheet = null }
                    },
                    onDecide = onDecide,
                    onCreateMaster = if ("UPDATE" in allowed) createMaster else null,
                )
            }
        }
        if (approvalOpen) {
            OperatingSheetScrim(
                onDismiss = { approvalOpen = false },
                accent = TradeBrand,
                surface = TradeSheetSurface,
                border = Color.White.copy(alpha = 0.08f),
                expandFully = true,
            ) {
                OperatingCaptureSheet(
                    title = "Approval / Reimbursement",
                    icon = R.drawable.qa_shield,
                    eventType = "APPROVAL",
                    mode = "APPROVAL",
                    model = model,
                    variant = "trading",
                    onDismiss = { approvalOpen = false },
                    onSubmit = { draft ->
                        submitDraft(draft.toTrading()) { approvalOpen = false }
                    },
                    onDecide = onDecide,
                )
            }
        }
    }
}

private val TradingSheet.hubIcon: Int
    get() = when (this) {
        TradingSheet.SALE, TradingSheet.SPEND, TradingSheet.REPRINT -> R.drawable.qa_receipt
        TradingSheet.PURCHASE -> R.drawable.qa_wallet
        TradingSheet.PAYMENT, TradingSheet.REIMBURSE -> R.drawable.qa_credit_card
        TradingSheet.COLLECTION, TradingSheet.RETURN -> R.drawable.qa_arrow_left_right
        TradingSheet.BUDGET, TradingSheet.TARGET -> R.drawable.qa_budget
        TradingSheet.UPDATE -> R.drawable.qa_message
        TradingSheet.ISSUE -> R.drawable.qa_alert
        TradingSheet.STOCK, TradingSheet.TRANSFER -> R.drawable.qa_package
        TradingSheet.ORDER, TradingSheet.HOLD -> R.drawable.qa_clipboard
        TradingSheet.SHIPMENT, TradingSheet.DELIVERY -> R.drawable.qa_shipment
        TradingSheet.PERSON -> R.drawable.qa_user_plus
        TradingSheet.ROLE -> R.drawable.qa_users
        TradingSheet.INVITE -> R.drawable.qa_qr
        TradingSheet.SUPPLIER -> R.drawable.qa_network
    }

private val TradingSheet.captureMode: String
    get() = when (this) {
        TradingSheet.SALE -> "SALE"
        TradingSheet.PURCHASE -> "PURCHASE"
        TradingSheet.SPEND -> "SPEND"
        TradingSheet.PAYMENT -> "PAYMENT"
        TradingSheet.COLLECTION -> "COLLECTION"
        TradingSheet.BUDGET -> "BUDGET"
        TradingSheet.TARGET -> "TARGET"
        TradingSheet.UPDATE -> "UPDATE"
        TradingSheet.ISSUE -> "ISSUE"
        TradingSheet.STOCK -> "STOCK"
        TradingSheet.ORDER -> "ORDER"
        TradingSheet.SHIPMENT -> "SHIPMENT"
        TradingSheet.DELIVERY -> "DELIVERY"
        TradingSheet.RETURN -> "RETURN"
        TradingSheet.REIMBURSE -> "REIMBURSE"
        TradingSheet.PERSON -> "PERSON"
        TradingSheet.ROLE -> "ROLE"
        TradingSheet.INVITE -> "INVITE"
        TradingSheet.SUPPLIER -> "SUPPLIER"
        TradingSheet.HOLD -> "HOLD"
        TradingSheet.TRANSFER -> "TRANSFER"
        TradingSheet.REPRINT -> "REPRINT"
    }

private fun OperatingCaptureDraft.toTrading() = TradingEventDraft(
    eventType = eventType,
    title = title,
    amount = amount,
    note = note,
    channelName = channelName,
    counterpartyName = counterpartyName,
    paymentStatus = paymentStatus,
    occurredAt = occurredAt,
    quantity = quantity,
    unit = unit,
    unitPrice = unitPrice,
    itemName = itemName,
    parentEventId = parentEventId,
    revisesEventId = revisesEventId,
    masterKind = masterKind,
    conversionToBase = conversionToBase,
    dueAt = dueAt,
    contract = contract,
    masterId = masterId,
    masterGstApplicable = masterGstApplicable,
    masterGstClassification = masterGstClassification,
    masterGstRate = masterGstRate,
    masterPriceIncludesGst = masterPriceIncludesGst,
    masterSalePriceIncludesGst = masterSalePriceIncludesGst,
    masterPurchasePriceIncludesGst = masterPurchasePriceIncludesGst,
    masterPurchasePrice = masterPurchasePrice,
    masterGstEffectiveFrom = masterGstEffectiveFrom,
    masterHsn = masterHsn,
    masterGstRegistration = masterGstRegistration,
    masterBusinessState = masterBusinessState,
    masterBillingState = masterBillingState,
    masterDeliveryState = masterDeliveryState,
    masterGstin = masterGstin,
    lines = lines,
)
