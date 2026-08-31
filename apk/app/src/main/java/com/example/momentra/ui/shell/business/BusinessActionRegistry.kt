package com.example.momentra.ui.shell.business

/**
 * Mapper from V019 Business capability / action codes to Quick Add destinations.
 * Empty capabilities fail closed until bootstrap fills V019 codes (parity with Personal).
 */
object BusinessActionRegistry {

    const val EXPENSE_CREATE = "EXPENSE_CREATE"
    const val REVENUE_RECORD = "REVENUE_RECORD"
    const val INVOICE_CREATE = "INVOICE_CREATE"
    const val MEMBER_MANAGE = "MEMBER_MANAGE"
    const val VENDOR_MANAGE = "VENDOR_MANAGE"
    const val ISSUE_CREATE = "ISSUE_CREATE"
    const val SLA_MANAGE = "SLA_MANAGE"

    enum class Destination {
        SPEND,
        VENDOR,
        ISSUE,
        SLA,
        REVENUE,
        INVOICE,
        MEMBERS,
    }

    fun destinationFor(capabilityCode: String): Destination? = when (capabilityCode.uppercase()) {
        EXPENSE_CREATE -> Destination.SPEND
        VENDOR_MANAGE -> Destination.VENDOR
        ISSUE_CREATE -> Destination.ISSUE
        SLA_MANAGE -> Destination.SLA
        REVENUE_RECORD -> Destination.REVENUE
        INVOICE_CREATE -> Destination.INVOICE
        MEMBER_MANAGE -> Destination.MEMBERS
        else -> null
    }

    /** Revenue/Invoice are V019-mapped to BUSINESS_RUNWAY only. */
    fun isRunwayFinanceEnabled(momentTypeCode: String?): Boolean =
        momentTypeCode?.uppercase() == "BUSINESS_RUNWAY"

    fun isDestinationEnabled(capabilities: List<String>, destination: Destination): Boolean {
        if (capabilities.isEmpty()) return false
        return capabilities.any { destinationFor(it) == destination }
    }

    fun enabledDestinations(capabilities: List<String>): Set<Destination> {
        if (capabilities.isEmpty()) return emptySet()
        return capabilities.mapNotNull { destinationFor(it) }.toSet()
    }
}

fun BusinessQuickAddKind.registryDestination(): BusinessActionRegistry.Destination? = when (this) {
    BusinessQuickAddKind.EXPENSE,
    BusinessQuickAddKind.SPEND_ENTRY,
    BusinessQuickAddKind.REQUEST_APPROVAL,
    BusinessQuickAddKind.BUDGET_REVIEW,
    -> BusinessActionRegistry.Destination.SPEND
    BusinessQuickAddKind.UPDATE_VENDOR -> BusinessActionRegistry.Destination.VENDOR
    BusinessQuickAddKind.REPORT_ISSUE,
    BusinessQuickAddKind.LOG_IMPROVEMENT,
    -> BusinessActionRegistry.Destination.ISSUE
    BusinessQuickAddKind.SLA_CHECK -> BusinessActionRegistry.Destination.SLA
    BusinessQuickAddKind.REVENUE -> BusinessActionRegistry.Destination.REVENUE
    BusinessQuickAddKind.INVOICE -> BusinessActionRegistry.Destination.INVOICE
    else -> null
}

/** Unmapped kinds (update/memory/chrome) stay available when moment is active. */
fun BusinessQuickAddKind.isCapabilityEnabled(capabilities: List<String>): Boolean {
    val dest = registryDestination() ?: return true
    return BusinessActionRegistry.isDestinationEnabled(capabilities, dest)
}
