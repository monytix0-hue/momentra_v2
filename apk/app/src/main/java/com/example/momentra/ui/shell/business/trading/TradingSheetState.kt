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
    onFooterAction: ((String) -> Unit)? = null,
) {
    val spec = state.spec
    val section = model.sectionByCode(spec.code)
    val total = when (spec.code) {
        "ATTENTION" -> model.attention.size
        "PAYMENT_READINESS" -> model.paymentReadiness.size
        "ACTIVITY" -> model.activity.size
        else -> section?.rows?.size
            ?: model.pulseMetrics?.firstOrNull { it.code == spec.code }?.metrics?.size
            ?: 0
    }
    OperatingSheetFrame(
        title = spec.title,
        eyebrow = spec.eyebrow,
        model = model,
        theme = theme,
        onDismiss = onDismiss,
        sheetCode = spec.code,
        totalCount = total,
        visibleCount = { status ->
            when (spec.code) {
                "ATTENTION" -> model.attention.count {
                    val chip = when (it.severity?.uppercase()) {
                        "CRITICAL" -> "Overdue"
                        "HIGH" -> "Alert"
                        "MEDIUM" -> "Unavailable"
                        else -> "Open"
                    }
                    com.example.momentra.ui.shell.business.shared.OperatingSheetChrome.matchesStatus(status, chip)
                }
                "PAYMENT_READINESS" -> model.paymentReadiness.count {
                    com.example.momentra.ui.shell.business.shared.OperatingSheetChrome.matchesStatus(status, it.dueBucket)
                }
                "ACTIVITY" -> model.activity.count {
                    com.example.momentra.ui.shell.business.shared.OperatingSheetChrome.matchesStatus(status, it.badge)
                }
                else -> section?.rows?.count {
                    com.example.momentra.ui.shell.business.shared.OperatingSheetChrome.matchesStatus(status, it.badge)
                } ?: total
            }
        },
        onFooterAction = onFooterAction,
    ) { status ->
        if (spec.pulseSurface) {
            val surface = runCatching { OperatingPulseSurface.valueOf(spec.code) }.getOrNull()
            OperatingPulseTiles(surface ?: OperatingPulseSurface.ACTIVITY, model, theme)
            val group = model.pulseMetrics?.firstOrNull { it.code == spec.code }
            OperatingWhyFootnote(group?.why, theme)
            when (spec.code) {
                "ATTENTION" -> model.attention
                    .filter {
                        val chip = when (it.severity?.uppercase()) {
                            "CRITICAL" -> "Overdue"
                            "HIGH" -> "Alert"
                            "MEDIUM" -> "Unavailable"
                            else -> "Open"
                        }
                        com.example.momentra.ui.shell.business.shared.OperatingSheetChrome.matchesStatus(status, chip)
                    }
                    .forEach { item ->
                        com.example.momentra.ui.shell.business.shared.OperatingBoardRowRow(
                            row = com.example.momentra.data.api.OperatingActivityDto(
                                id = item.id,
                                eventType = item.kind,
                                title = item.title,
                                subtitle = item.counterpartyName,
                                amount = item.amount,
                                badge = when (item.severity?.uppercase()) {
                                    "CRITICAL" -> "Overdue"
                                    "HIGH" -> "Alert"
                                    "MEDIUM" -> "Unavailable"
                                    else -> "Open"
                                },
                            ),
                            model = model,
                            theme = theme,
                        )
                    }
                "PAYMENT_READINESS" -> com.example.momentra.ui.shell.business.shared.OperatingPaymentRows(
                    rows = model.paymentReadiness.filter {
                        com.example.momentra.ui.shell.business.shared.OperatingSheetChrome.matchesStatus(status, it.dueBucket)
                    },
                    model = model,
                    theme = theme,
                )
                "ACTIVITY" -> model.activity
                    .filter {
                        com.example.momentra.ui.shell.business.shared.OperatingSheetChrome.matchesStatus(status, it.badge)
                    }
                    .forEach { row ->
                        com.example.momentra.ui.shell.business.shared.OperatingBoardRowRow(row, model, theme)
                    }
                else -> Unit
            }
        } else {
            OperatingBoardSectionBody(
                section = section?.copy(
                    rows = section.rows.filter {
                        com.example.momentra.ui.shell.business.shared.OperatingSheetChrome.matchesStatus(status, it.badge)
                    },
                ),
                spec = spec,
                model = model,
                theme = theme,
            )
        }
    }
}
