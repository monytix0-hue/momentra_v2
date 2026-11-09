package com.example.momentra.ui.shell.business.trading

import androidx.compose.runtime.Composable
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.shell.business.shared.OperatingBoardSectionBody
import com.example.momentra.ui.shell.business.shared.OperatingPulseSurface
import com.example.momentra.ui.shell.business.shared.OperatingPulseTiles
import com.example.momentra.ui.shell.business.shared.OperatingSheetFrame
import com.example.momentra.ui.shell.business.shared.OperatingSheetSpec
import com.example.momentra.ui.shell.business.shared.OperatingSheetTheme
import com.example.momentra.ui.shell.business.shared.OperatingWhyFootnote
import com.example.momentra.ui.shell.business.shared.sectionByCode

/**
 * The eighteen `View all ›` surfaces of TRADING_WHOLESALE, one per Figma frame.
 * Each maps to a board section code or a Pulse surface; the sheet renders tiles plus
 * rows from the projection, never the prose `detail` sentence.
 */
sealed interface TradingSheetState {
    val spec: OperatingSheetSpec

    data object TargetBudget : TradingSheetState {
        override val spec = OperatingSheetSpec(
            code = "TARGET_BUDGET",
            title = "Target & Budget",
            eyebrow = "What this period is aiming at",
            emptyTitle = "No target set",
            emptyBody = "Set a sales target and a period budget to measure the trade against them.",
            pulseSurface = true,
        )
    }

    data object Finance : TradingSheetState {
        override val spec = OperatingSheetSpec(
            code = "FINANCE",
            title = "Finance",
            eyebrow = "Recorded sales, money in, money still out",
            emptyTitle = "No finance recorded",
            emptyBody = "Sales, collections, and payables appear once recorded.",
            pulseSurface = true,
        )
    }

    data object PaymentReadiness : TradingSheetState {
        override val spec = OperatingSheetSpec(
            code = "PAYMENT_READINESS",
            title = "Payment Readiness",
            eyebrow = "What can be collected and what must be paid",
            emptyTitle = "Nothing awaiting settlement",
            emptyBody = "Open receivables and payables appear here as they are recorded.",
            pulseSurface = true,
        )
    }

    data object NeedsAttention : TradingSheetState {
        override val spec = OperatingSheetSpec(
            code = "ATTENTION",
            title = "Needs Attention",
            eyebrow = "Overdue, blocked, and waiting on a decision",
            emptyTitle = "Nothing is waiting",
            emptyBody = "Approvals, overdue items, and blockers will show here.",
            pulseSurface = true,
        )
    }

    data object Activity : TradingSheetState {
        override val spec = OperatingSheetSpec(
            code = "ACTIVITY",
            title = "Activity",
            eyebrow = "Everything recorded in this moment",
            emptyTitle = "Nothing recorded yet",
            emptyBody = "Sales, purchases, and money collection will show here.",
            pulseSurface = true,
        )
    }

    data object Gst : TradingSheetState {
        override val spec = OperatingSheetSpec(
            code = "GST",
            title = "GST Position",
            eyebrow = "Indicative tax on recorded records",
            emptyTitle = "No GST recorded",
            emptyBody = "GST appears on records once tracking is on and a rate is supplied.",
            pulseSurface = true,
        )
    }

    data object OrdersCommitments : TradingSheetState {
        override val spec = OperatingSheetSpec(
            code = "orders",
            title = "Orders & Commitments",
            eyebrow = "Commitments are not revenue until billed",
            emptyTitle = "No orders yet",
            emptyBody = "Buyer orders and their dispatches will show here.",
        )
    }

    data object StockProducts : TradingSheetState {
        override val spec = OperatingSheetSpec(
            code = "stock",
            title = "Stock & Products",
            eyebrow = "Owned on-hand only — consignment stays out",
            emptyTitle = "No stock recorded",
            emptyBody = "Open stock, purchases, and dispatches build the on-hand position.",
        )
    }

    data object PeopleNetwork : TradingSheetState {
        override val spec = OperatingSheetSpec(
            code = "network",
            title = "Buyers, Suppliers & Channels",
            eyebrow = "Who the trade runs through",
            emptyTitle = "No people yet",
            emptyBody = "Named buyers, suppliers, and channels appear as you record them.",
        )
    }

    data object OpenCommitments : TradingSheetState {
        override val spec = OperatingSheetSpec(
            code = "M4",
            title = "Open Commitments",
            eyebrow = "What is still owed in either direction",
            emptyTitle = "No open commitments",
            emptyBody = "Open orders, unsettled money, and dispatches in transit appear here.",
        )
    }

    data object LifeCurrentBusiness : TradingSheetState {
        override val spec = OperatingSheetSpec(
            code = "L1",
            title = "Current Business",
            eyebrow = "The trade today",
            emptyTitle = "Nothing recorded yet",
            emptyBody = "Board updates and the live position will show here.",
        )
    }

    data object LifeDealersChannels : TradingSheetState {
        override val spec = OperatingSheetSpec(
            code = "L2",
            title = "Buyers & Channels",
            eyebrow = "Attributed sales by channel",
            emptyTitle = "No channels recorded",
            emptyBody = "Channel-attributed sales appear once a channel is chosen.",
        )
    }

    data object LifeMoneyMoving : TradingSheetState {
        override val spec = OperatingSheetSpec(
            code = "L3",
            title = "Money Moving",
            eyebrow = "Collected, paid, and the pay lag between",
            emptyTitle = "No settlement yet",
            emptyBody = "Collections and payments will show here.",
        )
    }

    data object LifePeopleActivity : TradingSheetState {
        override val spec = OperatingSheetSpec(
            code = "L4",
            title = "People & Activity",
            eyebrow = "Who recorded what",
            emptyTitle = "No activity yet",
            emptyBody = "Stock moves, issues, and updates will show here.",
        )
    }

    data object MonthlyMemory : TradingSheetState {
        override val spec = OperatingSheetSpec(
            code = "ME1",
            title = "Monthly Memory",
            eyebrow = "Each month this trade has recorded",
            emptyTitle = "No month recorded",
            emptyBody = "Months appear once records exist in them.",
        )
    }

    data object MoneyMemory : TradingSheetState {
        override val spec = OperatingSheetSpec(
            code = "ME2",
            title = "Money Memory",
            eyebrow = "Collected, paid, and returned",
            emptyTitle = "No money memory",
            emptyBody = "Settlements and returns will show here.",
        )
    }

    data object FulfilmentMemory : TradingSheetState {
        override val spec = OperatingSheetSpec(
            code = "fulfilment",
            title = "Fulfilment Memory",
            eyebrow = "Delivered, received, and how late",
            emptyTitle = "No fulfilment recorded",
            emptyBody = "Dispatch and receipt status build this picture.",
        )
    }

    data object PeoplePatterns : TradingSheetState {
        override val spec = OperatingSheetSpec(
            code = "ME4",
            title = "Patterns & Decisions",
            eyebrow = "What repeats and what is waiting on approval",
            emptyTitle = "No pattern yet",
            emptyBody = "Repeated issues and pending approvals will show here.",
        )
    }
}

/** All eighteen surfaces in Figma order, used by tests and the sheet router. */
val TRADING_SHEET_STATES: List<TradingSheetState> = listOf(
    TradingSheetState.TargetBudget,
    TradingSheetState.Finance,
    TradingSheetState.PaymentReadiness,
    TradingSheetState.NeedsAttention,
    TradingSheetState.Activity,
    TradingSheetState.Gst,
    TradingSheetState.OrdersCommitments,
    TradingSheetState.StockProducts,
    TradingSheetState.PeopleNetwork,
    TradingSheetState.OpenCommitments,
    TradingSheetState.LifeCurrentBusiness,
    TradingSheetState.LifeDealersChannels,
    TradingSheetState.LifeMoneyMoving,
    TradingSheetState.LifePeopleActivity,
    TradingSheetState.MonthlyMemory,
    TradingSheetState.MoneyMemory,
    TradingSheetState.FulfilmentMemory,
    TradingSheetState.PeoplePatterns,
)

/** Board section codes the trading surfaces read. */
internal val TRADING_PULSE_CODES = setOf(
    "TARGET_BUDGET",
    "FINANCE",
    "PAYMENT_READINESS",
    "ATTENTION",
    "ACTIVITY",
    "GST",
)

@Composable
fun TradingBoardSheet(
    state: TradingSheetState,
    model: OperatingProjectionDto,
    theme: OperatingSheetTheme,
    onDismiss: () -> Unit,
) {
    val spec = state.spec
    OperatingSheetFrame(
        title = spec.title,
        eyebrow = spec.eyebrow,
        model = model,
        theme = theme,
        onDismiss = onDismiss,
    ) {
        if (spec.pulseSurface) {
            val surface = runCatching { OperatingPulseSurface.valueOf(spec.code) }.getOrNull()
            OperatingPulseTiles(surface ?: OperatingPulseSurface.ACTIVITY, model, theme)
            val group = model.pulseMetrics?.firstOrNull { it.code == spec.code }
            OperatingWhyFootnote(group?.why, theme)
        } else {
            OperatingBoardSectionBody(
                section = model.sectionByCode(spec.code),
                spec = spec,
                model = model,
                theme = theme,
            )
        }
    }
}
