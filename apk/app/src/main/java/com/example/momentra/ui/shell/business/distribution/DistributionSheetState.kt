package com.example.momentra.ui.shell.business.distribution

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
 * The eighteen `View all ›` surfaces of DISTRIBUTION_DEALERSHIP, one per DST-SHEET
 * frame. Same skeleton as Trading, with dealer network copy.
 */
sealed interface DistributionSheetState {
    val spec: OperatingSheetSpec

    data object TargetBudget : DistributionSheetState {
        override val spec = OperatingSheetSpec(
            code = "TARGET_BUDGET",
            title = "Target & Budget",
            eyebrow = "What this period is aiming at",
            emptyTitle = "No target set",
            emptyBody = "Set a sales target and a period budget to measure the network against them.",
            pulseSurface = true,
        )
    }

    data object Finance : DistributionSheetState {
        override val spec = OperatingSheetSpec(
            code = "FINANCE",
            title = "Finance",
            eyebrow = "Recorded sales, money in, money still out",
            emptyTitle = "No finance recorded",
            emptyBody = "Dealer sales, collections, and principal payables appear once recorded.",
            pulseSurface = true,
        )
    }

    data object PaymentReadiness : DistributionSheetState {
        override val spec = OperatingSheetSpec(
            code = "PAYMENT_READINESS",
            title = "Payment Readiness",
            eyebrow = "What can be collected and what must be paid",
            emptyTitle = "Nothing awaiting settlement",
            emptyBody = "Open dealer receivables and principal payables appear here.",
            pulseSurface = true,
        )
    }

    data object NeedsAttention : DistributionSheetState {
        override val spec = OperatingSheetSpec(
            code = "ATTENTION",
            title = "Needs Attention",
            eyebrow = "Overdue, blocked, and waiting on a decision",
            emptyTitle = "Nothing is waiting",
            emptyBody = "Approvals, overdue items, and blockers will show here.",
            pulseSurface = true,
        )
    }

    data object Activity : DistributionSheetState {
        override val spec = OperatingSheetSpec(
            code = "ACTIVITY",
            title = "Activity",
            eyebrow = "Everything recorded in this moment",
            emptyTitle = "Nothing recorded yet",
            emptyBody = "Dealer orders, dispatches, and settlement will show here.",
            pulseSurface = true,
        )
    }

    data object Gst : DistributionSheetState {
        override val spec = OperatingSheetSpec(
            code = "GST",
            title = "GST Position",
            eyebrow = "Indicative tax on recorded records",
            emptyTitle = "No GST recorded",
            emptyBody = "GST appears on records once tracking is on and a rate is supplied.",
            pulseSurface = true,
        )
    }

    data object OrdersCommitments : DistributionSheetState {
        override val spec = OperatingSheetSpec(
            code = "orders",
            title = "Orders & Commitments",
            eyebrow = "Dealer commitments are not revenue until billed",
            emptyTitle = "No dealer orders yet",
            emptyBody = "Dealer orders and their dispatches will show here.",
        )
    }

    data object StockProducts : DistributionSheetState {
        override val spec = OperatingSheetSpec(
            code = "stock",
            title = "Stock & Products",
            eyebrow = "Owned on-hand only — consignment stays out",
            emptyTitle = "No stock recorded",
            emptyBody = "Open stock, principal receipts, and dispatches build the on-hand position.",
        )
    }

    data object PeopleNetwork : DistributionSheetState {
        override val spec = OperatingSheetSpec(
            code = "network",
            title = "Dealers, Principals & Routes",
            eyebrow = "Who the network runs through",
            emptyTitle = "No dealers yet",
            emptyBody = "Named dealers, principals, and routes appear as you record them.",
        )
    }

    data object OpenCommitments : DistributionSheetState {
        override val spec = OperatingSheetSpec(
            code = "M4",
            title = "Open Commitments",
            eyebrow = "What is still owed in either direction",
            emptyTitle = "No open commitments",
            emptyBody = "Open orders, unsettled money, and dispatches in transit appear here.",
        )
    }

    data object LifeCurrentBusiness : DistributionSheetState {
        override val spec = OperatingSheetSpec(
            code = "L1",
            title = "Current Business",
            eyebrow = "The network today",
            emptyTitle = "Nothing recorded yet",
            emptyBody = "Board updates and the live position will show here.",
        )
    }

    data object LifeDealersChannels : DistributionSheetState {
        override val spec = OperatingSheetSpec(
            code = "L2",
            title = "Dealers & Channels",
            eyebrow = "Attributed sales by dealer and channel",
            emptyTitle = "No channels recorded",
            emptyBody = "Dealer-attributed sales appear once a channel is chosen.",
        )
    }

    data object LifeMoneyMoving : DistributionSheetState {
        override val spec = OperatingSheetSpec(
            code = "L3",
            title = "Money Moving",
            eyebrow = "Collected, paid, and the pay lag between",
            emptyTitle = "No settlement yet",
            emptyBody = "Dealer collections and principal payments will show here.",
        )
    }

    data object LifePeopleActivity : DistributionSheetState {
        override val spec = OperatingSheetSpec(
            code = "L4",
            title = "People & Activity",
            eyebrow = "Who recorded what",
            emptyTitle = "No activity yet",
            emptyBody = "Stock moves, issues, and updates will show here.",
        )
    }

    data object MonthlyMemory : DistributionSheetState {
        override val spec = OperatingSheetSpec(
            code = "ME1",
            title = "Monthly Memory",
            eyebrow = "Each month this network has recorded",
            emptyTitle = "No month recorded",
            emptyBody = "Months appear once records exist in them.",
        )
    }

    data object MoneyMemory : DistributionSheetState {
        override val spec = OperatingSheetSpec(
            code = "ME2",
            title = "Money Memory",
            eyebrow = "Collected, paid, and returned",
            emptyTitle = "No money memory",
            emptyBody = "Settlements and returns will show here.",
        )
    }

    data object FulfilmentMemory : DistributionSheetState {
        override val spec = OperatingSheetSpec(
            code = "fulfilment",
            title = "Fulfilment Memory",
            eyebrow = "Delivered, received, and how late",
            emptyTitle = "No fulfilment recorded",
            emptyBody = "Dispatch and receipt status build this picture.",
        )
    }

    data object PeoplePatterns : DistributionSheetState {
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
val DISTRIBUTION_SHEET_STATES: List<DistributionSheetState> = listOf(
    DistributionSheetState.TargetBudget,
    DistributionSheetState.Finance,
    DistributionSheetState.PaymentReadiness,
    DistributionSheetState.NeedsAttention,
    DistributionSheetState.Activity,
    DistributionSheetState.Gst,
    DistributionSheetState.OrdersCommitments,
    DistributionSheetState.StockProducts,
    DistributionSheetState.PeopleNetwork,
    DistributionSheetState.OpenCommitments,
    DistributionSheetState.LifeCurrentBusiness,
    DistributionSheetState.LifeDealersChannels,
    DistributionSheetState.LifeMoneyMoving,
    DistributionSheetState.LifePeopleActivity,
    DistributionSheetState.MonthlyMemory,
    DistributionSheetState.MoneyMemory,
    DistributionSheetState.FulfilmentMemory,
    DistributionSheetState.PeoplePatterns,
)

@Composable
fun DistributionBoardSheet(
    state: DistributionSheetState,
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
