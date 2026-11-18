package com.example.momentra.ui.shell.business.mto

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.momentra.ui.shell.business.shared.OperatingToastHost
import com.example.momentra.ui.shell.business.shared.OperatingToastOverlay
import com.example.momentra.ui.shell.business.shared.OperatingToastPanel

/**
 * MTO-TOAST · * — 13 Figma frames (`2712:29832`…`29887`, `2713:29928`…`30043`,
 * `2761:38075`, `2761:38100`).
 *
 * Thin host API: call [MtoToastHost.show] from capture success paths without rewriting the hub.
 */

private val ToastEmerald = Color(0xFF10B981)
private val ToastAmber = Color(0xFFEA580C)

enum class MtoToastKind {
    SetupActivated,
    Saved,
    Deleted,
    QuoteSent,
    OrderAccepted,
    InvoicePosted,
    SavedConfirmOrder,
    SavedBack,
    SavedCustomers,
    SavedLines,
    SavedProducts,
    Paused,
    Finished,
}

data class MtoToastSpec(
    val kind: MtoToastKind,
    val title: String,
    val body: String,
    val actionLabel: String,
    val accent: Color,
    /** Optional navigation hint for the CTA — hubs can react without coupling to toast UI. */
    val action: MtoToastAction = MtoToastAction.Dismiss,
)

enum class MtoToastAction {
    Dismiss,
    OpenPulse,
    ConfirmOrder,
    GoBack,
    OpenCustomers,
    OpenLines,
    OpenProducts,
}

object MtoToastCatalog {
    fun of(
        kind: MtoToastKind,
        entity: String = "this job",
        businessLabel: String = "Make to order",
    ): MtoToastSpec = when (kind) {
        MtoToastKind.SetupActivated -> MtoToastSpec(
            kind,
            "Setup activated",
            "$businessLabel · Make to order is live · opening Pulse",
            "Pulse ›",
            ToastAmber,
            MtoToastAction.OpenPulse,
        )
        MtoToastKind.Saved -> MtoToastSpec(
            kind, "Saved", "Changes recorded · $entity updated", "Done", ToastEmerald,
        )
        MtoToastKind.Deleted -> MtoToastSpec(
            kind, "Deleted", "$entity removed · nothing else changed", "Done", ToastEmerald,
        )
        MtoToastKind.QuoteSent -> MtoToastSpec(
            kind, "Quote sent", "$entity · quotation shared with customer", "Done", ToastEmerald,
        )
        MtoToastKind.OrderAccepted -> MtoToastSpec(
            kind, "Order accepted", "Job confirmed · production unlocked · not revenue", "Done", ToastEmerald,
        )
        MtoToastKind.InvoicePosted -> MtoToastSpec(
            kind, "Invoice posted", "AR recorded · not revenue until collected", "Done", ToastEmerald,
        )
        MtoToastKind.SavedConfirmOrder -> MtoToastSpec(
            kind, "Saved", "Changes recorded · $entity updated", "Confirm ›", ToastEmerald,
            MtoToastAction.ConfirmOrder,
        )
        MtoToastKind.SavedBack -> MtoToastSpec(
            kind, "Saved", "Changes recorded · $entity updated", "Back ›", ToastEmerald,
            MtoToastAction.GoBack,
        )
        MtoToastKind.SavedCustomers -> MtoToastSpec(
            kind, "Saved", "Changes recorded · $entity updated", "Customers ›", ToastEmerald,
            MtoToastAction.OpenCustomers,
        )
        MtoToastKind.SavedLines -> MtoToastSpec(
            kind, "Saved", "Changes recorded · $entity updated", "Lines ›", ToastEmerald,
            MtoToastAction.OpenLines,
        )
        MtoToastKind.SavedProducts -> MtoToastSpec(
            kind, "Saved", "Changes recorded · $entity updated", "Products ›", ToastEmerald,
            MtoToastAction.OpenProducts,
        )
        MtoToastKind.Paused -> MtoToastSpec(
            kind, "Paused", "Moment paused · Resume anytime from Manage", "Done", ToastEmerald,
        )
        MtoToastKind.Finished -> MtoToastSpec(
            kind, "Finished", "Moment finished · Memory stays available", "Done", ToastEmerald,
        )
    }

    /** Map a capture mode / event to the primary success toast. */
    fun forCaptureSuccess(
        captureMode: String?,
        eventType: String?,
        entity: String,
    ): MtoToastSpec {
        val mode = captureMode?.uppercase().orEmpty()
        val type = eventType?.uppercase().orEmpty()
        val kind = when {
            mode == "ORDER" || mode == "QUOTATION" || mode == "QUOTE" -> MtoToastKind.QuoteSent
            mode == "CONFIRM" || mode == "CONFIRM_ORDER" || mode == "ACCEPT" -> MtoToastKind.OrderAccepted
            // Mode wins over eventType so REPRINT (eventType SALE) stays Saved, not Invoice posted.
            mode == "SALE" || mode == "INVOICE" -> MtoToastKind.InvoicePosted
            mode == "QUOTE_REVISE" || mode == "REVISE" -> MtoToastKind.SavedConfirmOrder
            mode == "HOLD" -> MtoToastKind.Paused
            mode == "JOB_STATUS" && (type.contains("CLOSE") || type.contains("FINISH") || type.contains("COMPLETE")) ->
                MtoToastKind.Finished
            else -> MtoToastKind.Saved
        }
        return of(kind, entity = entity)
    }

    /** Master create success → Saved → Customers / Lines / Products CTA variants. */
    fun forMasterCreated(kind: String?, entity: String): MtoToastSpec {
        val k = kind?.uppercase().orEmpty()
        val toast = when {
            k in setOf("PRINCIPAL", "CUSTOMER", "VENDOR", "BUYER", "SUPPLIER", "PARTY") ->
                MtoToastKind.SavedCustomers
            k in setOf("LOCATION", "LINE", "STORE", "STATION", "FLOOR") ->
                MtoToastKind.SavedLines
            k in setOf("SKU", "PRODUCT", "PART", "BOM", "MATERIAL", "CATALOGUE", "CATALOG") ->
                MtoToastKind.SavedProducts
            else -> MtoToastKind.Saved
        }
        return of(toast, entity = entity)
    }
}

/** Process-wide toast host — delegates to [OperatingToastHost]. */
object MtoToastHost {
    val current: MtoToastSpec?
        get() = OperatingToastHost.current

    var onAction: ((MtoToastAction) -> Unit)?
        get() = OperatingToastHost.onAction
        set(value) {
            OperatingToastHost.onAction = value
        }

    fun show(spec: MtoToastSpec) {
        OperatingToastHost.show(spec)
    }

    fun show(kind: MtoToastKind, entity: String = "this job", businessLabel: String = "Make to order") {
        OperatingToastHost.show(kind, entity, businessLabel)
    }

    fun dismiss() {
        OperatingToastHost.dismiss()
    }
}

@Composable
fun MtoToastOverlay(
    modifier: Modifier = Modifier,
    autoDismissMs: Long = 4200L,
) {
    OperatingToastOverlay(modifier = modifier, autoDismissMs = autoDismissMs)
}

@Composable
fun MtoToastPanel(
    spec: MtoToastSpec,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OperatingToastPanel(spec = spec, onAction = onAction, modifier = modifier)
}