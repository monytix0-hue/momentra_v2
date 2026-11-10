package com.example.momentra.ui.shell.business.manufacturing

import androidx.compose.runtime.Composable
import com.example.momentra.data.api.OperatingActivityDto
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.shell.business.shared.OperatingBoardSectionBody
import com.example.momentra.ui.shell.business.shared.OperatingBoardRowRow
import com.example.momentra.ui.shell.business.shared.OperatingPaymentRows
import com.example.momentra.ui.shell.business.shared.OperatingPulseSurface
import com.example.momentra.ui.shell.business.shared.OperatingPulseTiles
import com.example.momentra.ui.shell.business.shared.OperatingSheetChrome
import com.example.momentra.ui.shell.business.shared.OperatingSheetFrame
import com.example.momentra.ui.shell.business.shared.OperatingSheetSpec
import com.example.momentra.ui.shell.business.shared.OperatingSheetTheme
import com.example.momentra.ui.shell.business.shared.OperatingWhyFootnote
import com.example.momentra.ui.shell.business.shared.sectionByCode

/**
 * The eighteen `View all ›` surfaces of STANDARD_PRODUCTION / MADE_TO_ORDER.
 * Same pulse + board count as Trading, but Moments bind projection codes
 * `orders` / `production` / `stock` (not trading `network`).
 */
sealed interface ManufacturingSheetState {
    val spec: OperatingSheetSpec

    data object TargetBudget : ManufacturingSheetState {
        override val spec = OperatingSheetSpec(
            code = "TARGET_BUDGET",
            title = "Target & Budget",
            eyebrow = "What this period is aiming at",
            emptyTitle = "No target set",
            emptyBody = "Set a sales target and a period budget to measure the network against them.",
            pulseSurface = true,
        )
    }

    data object Finance : ManufacturingSheetState {
        override val spec = OperatingSheetSpec(
            code = "FINANCE",
            title = "Finance",
            eyebrow = "Recorded sales, money in, money still out",
            emptyTitle = "No finance recorded",
            emptyBody = "Plant sales, collections, and vendor payables appear once recorded.",
            pulseSurface = true,
        )
    }

    data object PaymentReadiness : ManufacturingSheetState {
        override val spec = OperatingSheetSpec(
            code = "PAYMENT_READINESS",
            title = "Payment Readiness",
            eyebrow = "What can be collected and what must be paid",
            emptyTitle = "Nothing awaiting settlement",
            emptyBody = "Open receivables and vendor payables appear here.",
            pulseSurface = true,
        )
    }

    data object NeedsAttention : ManufacturingSheetState {
        override val spec = OperatingSheetSpec(
            code = "ATTENTION",
            title = "Needs Attention",
            eyebrow = "Overdue, blocked, and waiting on a decision",
            emptyTitle = "Nothing is waiting",
            emptyBody = "Approvals, overdue items, and blockers will show here.",
            pulseSurface = true,
        )
    }

    data object Activity : ManufacturingSheetState {
        override val spec = OperatingSheetSpec(
            code = "ACTIVITY",
            title = "Activity",
            eyebrow = "Everything recorded in this moment",
            emptyTitle = "Nothing recorded yet",
            emptyBody = "Work orders, dispatches, and settlement will show here.",
            pulseSurface = true,
        )
    }

    data object Gst : ManufacturingSheetState {
        override val spec = OperatingSheetSpec(
            code = "GST",
            title = "GST Position",
            eyebrow = "Indicative tax on recorded records",
            emptyTitle = "No GST recorded",
            emptyBody = "GST appears on records once tracking is on and a rate is supplied.",
            pulseSurface = true,
        )
    }

    data object OrdersCommitments : ManufacturingSheetState {
        override val spec = OperatingSheetSpec(
            code = "orders",
            title = "Work Orders & Commitments",
            eyebrow = "Commitments are not revenue until billed",
            emptyTitle = "No work orders yet",
            emptyBody = "Work orders and their dispatches will show here.",
        )
    }

    data object StockProducts : ManufacturingSheetState {
        override val spec = OperatingSheetSpec(
            code = "stock",
            title = "Stock & Products",
            eyebrow = "Owned on-hand only — consignment stays out",
            emptyTitle = "No stock recorded",
            emptyBody = "Open stock, principal receipts, and dispatches build the on-hand position.",
        )
    }

    /** Projection `familyMomentSections` code — not trading `network`. */
    data object Production : ManufacturingSheetState {
        override val spec = OperatingSheetSpec(
            code = "production",
            title = "Production",
            eyebrow = "Recorded vs accepted plant output",
            emptyTitle = "No production yet",
            emptyBody = "Production batches and accepted output appear here.",
        )
    }

    data object OpenCommitments : ManufacturingSheetState {
        override val spec = OperatingSheetSpec(
            code = "M4",
            title = "Open Commitments",
            eyebrow = "What is still owed in either direction",
            emptyTitle = "No open commitments",
            emptyBody = "Open orders, unsettled money, and dispatches in transit appear here.",
        )
    }

    data object LifeCurrentBusiness : ManufacturingSheetState {
        override val spec = OperatingSheetSpec(
            code = "L1",
            title = "Current Production / Business",
            eyebrow = "As-of today · live plant position",
            emptyTitle = "Nothing recorded yet",
            emptyBody = "Board updates and the live position will show here.",
        )
    }

    data object LifeOrdersBuyers : ManufacturingSheetState {
        override val spec = OperatingSheetSpec(
            code = "L2",
            title = "Orders / Buyers",
            eyebrow = "Attributed plant sales by buyer and channel",
            emptyTitle = "No channels recorded",
            emptyBody = "Channel-attributed sales appear once a channel is chosen.",
        )
    }

    data object LifeMoneyMoving : ManufacturingSheetState {
        override val spec = OperatingSheetSpec(
            code = "L3",
            title = "Money Moving",
            eyebrow = "Collected, paid, and the pay lag between",
            emptyTitle = "No settlement yet",
            emptyBody = "Collections and vendor payments will show here.",
        )
    }

    data object LifePeopleActivity : ManufacturingSheetState {
        override val spec = OperatingSheetSpec(
            code = "L4",
            title = "Production Operations",
            eyebrow = "Plant execution · issues · stock signals",
            emptyTitle = "No activity yet",
            emptyBody = "Stock moves, issues, and updates will show here.",
        )
    }

    data object MonthlyMemory : ManufacturingSheetState {
        override val spec = OperatingSheetSpec(
            code = "ME1",
            title = "Monthly Memory",
            eyebrow = "Each month this plant has recorded",
            emptyTitle = "No month recorded",
            emptyBody = "Months appear once records exist in them.",
        )
    }

    data object MoneyMemory : ManufacturingSheetState {
        override val spec = OperatingSheetSpec(
            code = "ME2",
            title = "Money Memory",
            eyebrow = "Collected, paid, and returned",
            emptyTitle = "No money memory",
            emptyBody = "Settlements and returns will show here.",
        )
    }

    data object FulfilmentMemory : ManufacturingSheetState {
        override val spec = OperatingSheetSpec(
            code = "fulfilment",
            title = "Fulfilment Memory",
            eyebrow = "Delivered, received, and how late",
            emptyTitle = "No fulfilment recorded",
            emptyBody = "Dispatch and receipt status build this picture.",
        )
    }

    data object PeoplePatterns : ManufacturingSheetState {
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
val MANUFACTURING_SHEET_STATES: List<ManufacturingSheetState> = listOf(
    ManufacturingSheetState.TargetBudget,
    ManufacturingSheetState.Finance,
    ManufacturingSheetState.PaymentReadiness,
    ManufacturingSheetState.NeedsAttention,
    ManufacturingSheetState.Activity,
    ManufacturingSheetState.Gst,
    ManufacturingSheetState.OrdersCommitments,
    ManufacturingSheetState.StockProducts,
    ManufacturingSheetState.Production,
    ManufacturingSheetState.OpenCommitments,
    ManufacturingSheetState.LifeCurrentBusiness,
    ManufacturingSheetState.LifeOrdersBuyers,
    ManufacturingSheetState.LifeMoneyMoving,
    ManufacturingSheetState.LifePeopleActivity,
    ManufacturingSheetState.MonthlyMemory,
    ManufacturingSheetState.MoneyMemory,
    ManufacturingSheetState.FulfilmentMemory,
    ManufacturingSheetState.PeoplePatterns,
)

private fun attentionChip(severity: String?): String = when (severity?.uppercase()) {
    "CRITICAL" -> "Overdue"
    "HIGH" -> "Alert"
    "MEDIUM" -> "Unavailable"
    else -> "Open"
}

/** MTO (job-order) titles where Figma diverges from plant copy. */
fun ManufacturingSheetState.specFor(subtype: String?): OperatingSheetSpec {
    val mto = subtype.equals("MADE_TO_ORDER", ignoreCase = true)
    if (!mto) return spec
    return when (this) {
        ManufacturingSheetState.OrdersCommitments -> spec.copy(
            title = "Current Jobs",
            emptyTitle = "No jobs yet",
            emptyBody = "Confirmed jobs and their operations will show here.",
        )
        ManufacturingSheetState.Production -> spec.copy(
            title = "Job Output",
            eyebrow = "Recorded vs accepted against open jobs",
            emptyTitle = "No job output yet",
            emptyBody = "Accepted production against confirmed jobs appears here.",
        )
        ManufacturingSheetState.StockProducts -> spec.copy(
            title = "Job Operations",
            eyebrow = "WIP against open jobs",
            emptyTitle = "No job stock yet",
            emptyBody = "Materials issued to jobs and finished goods appear here.",
        )
        ManufacturingSheetState.OpenCommitments -> spec.copy(title = "Open Jobs")
        ManufacturingSheetState.LifeCurrentBusiness -> spec.copy(
            title = "Current Jobs Today",
            eyebrow = "As-of today · live job position",
        )
        ManufacturingSheetState.LifeOrdersBuyers -> spec.copy(
            title = "Jobs & Customers",
            eyebrow = "Attributed work by customer and channel",
        )
        ManufacturingSheetState.LifePeopleActivity -> spec.copy(
            title = "Production Operations",
            eyebrow = "Job execution · issues · stock signals",
        )
        else -> spec
    }
}

@Composable
fun ManufacturingBoardSheet(
    state: ManufacturingSheetState,
    model: OperatingProjectionDto,
    theme: OperatingSheetTheme,
    onDismiss: () -> Unit,
    onFooterAction: ((String) -> Unit)? = null,
) {
    val spec = state.specFor(model.subtype)
    val section = model.sectionByCode(spec.code)
    val attention = model.attention
    val payments = model.paymentReadiness
    val activity = model.activity
    val total = when (spec.code) {
        "ATTENTION" -> attention.size
        "PAYMENT_READINESS" -> payments.size
        "ACTIVITY" -> activity.size
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
                "ATTENTION" -> attention.count {
                    OperatingSheetChrome.matchesStatus(status, attentionChip(it.severity))
                }
                "PAYMENT_READINESS" -> payments.count {
                    OperatingSheetChrome.matchesStatus(status, it.dueBucket)
                }
                "ACTIVITY" -> activity.count {
                    OperatingSheetChrome.matchesStatus(status, it.badge)
                }
                else -> section?.rows?.count {
                    OperatingSheetChrome.matchesStatus(status, it.badge)
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
                "ATTENTION" -> attention
                    .filter { OperatingSheetChrome.matchesStatus(status, attentionChip(it.severity)) }
                    .forEach { item ->
                        OperatingBoardRowRow(
                            row = OperatingActivityDto(
                                id = item.id,
                                eventType = item.kind,
                                title = item.title,
                                subtitle = item.counterpartyName,
                                amount = item.amount,
                                badge = attentionChip(item.severity),
                            ),
                            model = model,
                            theme = theme,
                        )
                    }
                "PAYMENT_READINESS" -> OperatingPaymentRows(
                    rows = payments.filter {
                        OperatingSheetChrome.matchesStatus(status, it.dueBucket)
                    },
                    model = model,
                    theme = theme,
                )
                "ACTIVITY" -> activity
                    .filter { OperatingSheetChrome.matchesStatus(status, it.badge) }
                    .forEach { row -> OperatingBoardRowRow(row, model, theme) }
                else -> Unit
            }
        } else {
            val filtered = section?.copy(
                rows = section.rows.filter {
                    OperatingSheetChrome.matchesStatus(status, it.badge)
                },
            )
            OperatingBoardSectionBody(
                section = filtered,
                spec = spec,
                model = model,
                theme = theme,
            )
        }
    }
}
