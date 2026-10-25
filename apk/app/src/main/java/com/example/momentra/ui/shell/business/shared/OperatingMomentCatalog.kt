package com.example.momentra.ui.shell.business.shared

import com.example.momentra.ui.shell.empty.business.BusinessSetupKind

data class OperatingSubtype(
    val code: String,
    val label: String,
    val partyLabel: String,
    val targetMetrics: List<String>,
    val requiresChannel: Boolean,
    val quickAdds: List<String>,
)

object OperatingMomentCatalog {
    val roles = listOf(
        "OWNER_ADMIN" to "Owner / Admin",
        "OPERATIONS_LEAD" to "Operations Lead",
        "FINANCE" to "Finance",
        "APPROVER" to "Approver",
        "CONTRIBUTOR" to "Contributor",
        "TEAM_MEMBER" to "Team Member",
        "OBSERVER" to "Observer",
    )

    private val marketingAdds = listOf(
        "ACTIVITY", "PROGRESS", "SPEND", "PAYMENT", "REVISION", "APPROVAL", "UPDATE", "ISSUE", "RESULT",
    )

    val families: Map<BusinessSetupKind, List<OperatingSubtype>> = mapOf(
        BusinessSetupKind.MARKETING_CAMPAIGNS to listOf(
            OperatingSubtype("CLIENT_CAMPAIGN", "Client Campaign", "Client", listOf("LEADS", "CONVERSIONS", "REVENUE", "ORDERS", "REACH", "ENGAGEMENT", "CUSTOM_COUNT", "CLIENT_BILLING"), false, marketingAdds + listOf("INVOICE", "COLLECTION")),
            OperatingSubtype("OWN_BUSINESS_CAMPAIGN", "Own Business Campaign", "Brand", listOf("LEADS", "CONVERSIONS", "REVENUE", "ORDERS", "REACH", "ENGAGEMENT", "CUSTOM_COUNT"), false, marketingAdds),
        ),
        BusinessSetupKind.TRADING_DISTRIBUTION to listOf(
            OperatingSubtype("TRADING_WHOLESALE", "Trading / Wholesale", "Business", listOf("REVENUE", "ORDERS", "CUSTOM_COUNT"), false, listOf("SALE", "PURCHASE", "SPEND", "PAYMENT", "COLLECTION", "BUDGET", "UPDATE", "ISSUE", "APPROVAL", "STOCK", "ORDER", "SHIPMENT")),
            OperatingSubtype("DISTRIBUTION_DEALERSHIP", "Distribution / Dealership", "Principal", listOf("REVENUE", "ORDERS", "CUSTOM_COUNT"), false, listOf("SALE", "PURCHASE", "SPEND", "PAYMENT", "COLLECTION", "BUDGET", "UPDATE", "ISSUE", "APPROVAL", "STOCK", "ORDER", "SHIPMENT")),
        ),
        BusinessSetupKind.RETAIL_CONSUMER to listOf(
            OperatingSubtype("RETAIL_STORE", "Retail Store", "Store", listOf("REVENUE", "ORDERS", "CUSTOM_COUNT"), true, listOf("SALE", "PURCHASE", "SPEND", "PAYMENT", "COLLECTION", "BUDGET", "TARGET", "UPDATE", "ISSUE", "APPROVAL", "STOCK", "ORDER")),
            OperatingSubtype("RESTAURANT_FOOD", "Restaurant / Food", "Outlet", listOf("REVENUE", "ORDERS", "CUSTOM_COUNT"), true, listOf("SALE", "PURCHASE", "SPEND", "PAYMENT", "COLLECTION", "BUDGET", "TARGET", "UPDATE", "ISSUE", "APPROVAL", "STOCK", "ORDER")),
            OperatingSubtype("CONSUMER_SERVICE", "Consumer Service", "Service", listOf("REVENUE", "ORDERS", "CUSTOM_COUNT"), true, listOf("SERVICE", "APPOINTMENT", "PURCHASE", "SPEND", "PAYMENT", "COLLECTION", "BUDGET", "TARGET", "UPDATE", "ISSUE", "APPROVAL", "STOCK")),
        ),
        BusinessSetupKind.MANUFACTURING_PRODUCTION to listOf(
            OperatingSubtype("STANDARD_PRODUCTION", "Standard Production", "Plant", listOf("REVENUE", "PRODUCTION_OUTPUT", "DELIVERY_COUNT", "ORDERS"), false, listOf("PRODUCTION", "PURCHASE", "SPEND", "SALE", "CUSTOMER_ORDER", "DELIVERY", "PAYMENT", "COLLECTION", "BUDGET", "TARGET", "UPDATE", "ISSUE", "APPROVAL", "STOCK")),
            OperatingSubtype("MADE_TO_ORDER", "Made-to-Order", "Workshop", listOf("REVENUE", "PRODUCTION_OUTPUT", "DELIVERY_COUNT", "ORDERS"), false, listOf("PRODUCTION", "PURCHASE", "SPEND", "SALE", "CUSTOMER_ORDER", "DELIVERY", "PAYMENT", "COLLECTION", "BUDGET", "TARGET", "UPDATE", "ISSUE", "APPROVAL", "STOCK")),
        ),
    )

    val quickAddLabels = mapOf(
        "SPEND" to "Add Spend",
        "RESULT" to "Add Result",
        "PAYMENT" to "Add Payment",
        "COLLECTION" to "Add Collection",
        "BUDGET" to "Revise Budget",
        "TARGET" to "Revise Target",
        "UPDATE" to "Add Update",
        "ISSUE" to "Add Issue",
        "APPROVAL" to "Request Approval",
        "DELIVERABLE" to "Add Deliverable",
        "VENDOR_COST" to "Add Vendor Cost",
        "COMPLETE" to "Mark Complete",
        "SALE" to "Record Sale",
        "PURCHASE" to "Record Purchase",
        "STOCK" to "Stock Update",
        "ORDER" to "Order Update",
        "SHIPMENT" to "Shipment Update",
        "SERVICE" to "Record Service",
        "APPOINTMENT" to "Appointment / Job",
        "PRODUCTION" to "Record Production",
        "CUSTOMER_ORDER" to "Customer Order",
        "DELIVERY" to "Record Delivery",
    )

    fun subtypes(kind: BusinessSetupKind): List<OperatingSubtype> = families[kind].orEmpty()

    fun kindForTypeCode(code: String?): BusinessSetupKind? {
        val c = code?.trim()?.uppercase().orEmpty()
        if (c.isEmpty()) return null
        BusinessSetupKind.entries.firstOrNull { it.familyCode == c }?.let { return it }
        return families.entries.firstOrNull { (_, subs) -> subs.any { it.code == c } }?.key
    }

    fun isRetired(code: String?): Boolean {
        val c = code?.trim()?.uppercase().orEmpty()
        if (c.isEmpty()) return false
        if (kindForTypeCode(c) != null) return false
        return c.contains("TEAM_OPERATIONS") || c.contains("BUSINESS_RUNWAY") || c.contains("RUNWAY") ||
            (c.contains("OPERATIONS") && !c.contains("TEAM"))
    }

    val preferenceKeys = setOf(
        "subtype", "identityName", "partyName", "description", "currency", "startDate", "endDate", "location", "activationPolicy",
        "gstTracking", "gstRegistration", "gstState", "gstin", "salePriceIncludesGst", "purchasePriceIncludesGst", "gstEffectiveFrom",
        "participants", "counterparties", "channels", "tradeReach", "monthlyBudget", "monthlyTarget", "targetMetric",
    )
}
