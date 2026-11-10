package com.example.momentra.ui.shell.business.mto

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.delay

/**
 * MTO-TOAST · * — 13 Figma frames (`2712:29832`…`29887`, `2713:29928`…`30043`,
 * `2761:38075`, `2761:38100`).
 *
 * Thin host API: call [MtoToastHost.show] from capture success paths without rewriting the hub.
 */

private val ToastPanel = Color(0xFF16181F)
private val ToastInk = Color(0xFFF8F9FD)
private val ToastMuted = Color(0xFF9496A8)
private val ToastBtn = Color(0xFF1C1F28)
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

/** Process-wide toast host — safe for capture sheets to fire after save. */
object MtoToastHost {
    var current by mutableStateOf<MtoToastSpec?>(null)
        private set

    var onAction: ((MtoToastAction) -> Unit)? = null

    fun show(spec: MtoToastSpec) {
        current = spec
    }

    fun show(kind: MtoToastKind, entity: String = "this job", businessLabel: String = "Make to order") {
        show(MtoToastCatalog.of(kind, entity, businessLabel))
    }

    fun dismiss() {
        current = null
    }
}

@Composable
fun MtoToastOverlay(
    modifier: Modifier = Modifier,
    autoDismissMs: Long = 4200L,
) {
    val spec = MtoToastHost.current
    AnimatedVisibility(
        visible = spec != null,
        enter = fadeIn() + slideInVertically { -it / 2 },
        exit = fadeOut() + slideOutVertically { -it / 2 },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        val toast = spec ?: return@AnimatedVisibility
        LaunchedEffect(toast) {
            delay(autoDismissMs)
            if (MtoToastHost.current == toast) MtoToastHost.dismiss()
        }
        MtoToastPanel(
            spec = toast,
            onAction = {
                val action = toast.action
                MtoToastHost.dismiss()
                if (action != MtoToastAction.Dismiss) {
                    MtoToastHost.onAction?.invoke(action)
                }
            },
        )
    }
}

@Composable
fun MtoToastPanel(
    spec: MtoToastSpec,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .shadow(12.dp, shape, ambientColor = spec.accent.copy(alpha = 0.12f), spotColor = spec.accent.copy(alpha = 0.12f))
                .clip(shape)
                .background(ToastPanel)
                .border(1.dp, Color.White.copy(alpha = 0.08f), shape)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(spec.accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Text("✓", color = spec.accent, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(spec.title, color = ToastInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(spec.body, color = ToastMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(ToastBtn)
                    .clickable(onClick = onAction)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    spec.actionLabel,
                    color = spec.accent,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                )
            }
        }
        Box(
            Modifier
                .fillMaxWidth(0.9f)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(spec.accent.copy(alpha = 0.6f)),
        )
    }
}
