package com.example.momentra.ui.shell.business.shared

import com.example.momentra.ui.shell.empty.business.BusinessSetupKind

/**
 * One row of the setup document's `Roles on invite` chip set.
 *
 * [label] is the operator word the frame prints (`Sales exec`, `Route lead`); [code] is the
 * nearest existing operating capability, because the backend accepts only `OPERATING_ROLES`
 * and drops anything else. The label is what a person sees; the code is what grants access.
 */
data class OperatingInviteRole(val code: String, val label: String)

data class OperatingSubtype(
    val code: String,
    val label: String,
    val partyLabel: String,
    val targetMetrics: List<String>,
    val requiresChannel: Boolean,
    val quickAdds: List<String>,
    /** Setup §03 `Channel` chips, in frame order. Empty means the family has no channel step. */
    val channels: List<String> = emptyList(),
    /** Setup §02 `Roles on invite`, in frame order. */
    val inviteRoles: List<OperatingInviteRole> = emptyList(),
    /** Setup §04 fourth row: `Principals & dealers` for trading, `Suppliers & buyers` for distribution. */
    val networkLabel: String = "Principals & dealers",
)

object OperatingMomentCatalog {
    /** The frame's five trading channels (`TRD-SETUP-v6` §03). */
    val tradingChannels = listOf("Retail buyers", "Wholesale", "Modern trade", "HoReCa", "Online")

    /** The frame's five distribution channels (`DST-SETUP-v6` §03). */
    val distributionChannels = listOf("Retail dealers", "Wholesale", "Modern trade", "HoReCa", "Van sales")

    /** Marketing campaign channels (`MKT-SETUP-v6` / contract §3.1 subset shown in setup chips). */
    val marketingChannels = listOf(
        "Paid Social",
        "Search Ads",
        "Display / Video Ads",
        "Organic Social",
        "Email",
        "Influencer",
        "SEO / Content",
        "SMS / Messaging",
    )

    val marketingInviteRoles = listOf(
        OperatingInviteRole("FINANCE", "Finance"),
        OperatingInviteRole("APPROVER", "Approver"),
        OperatingInviteRole("OPERATIONS_LEAD", "Campaign Lead"),
        OperatingInviteRole("CONTRIBUTOR", "Contributor"),
    )

    /**
     * §02 `Roles on invite`, in frame order. These are exactly the four codes
     * `POST /v1/company/invites` accepts (`OPERATING_INVITE_ROLES`), so every chip can actually
     * be minted — `OWNER_ADMIN`, `TEAM_MEMBER` and `OBSERVER` are not mintable and are gone.
     */
    val tradingInviteRoles = listOf(
        OperatingInviteRole("FINANCE", "Finance"),
        OperatingInviteRole("APPROVER", "Approver"),
        OperatingInviteRole("OPERATIONS_LEAD", "Operations"),
        OperatingInviteRole("CONTRIBUTOR", "Contributor"),
    )

    /** Dist labels follow DST-MASTER Invite & QR; codes stay mintable on `/v1/company/invites`. */
    val distributionInviteRoles = listOf(
        OperatingInviteRole("FINANCE", "Manager"),
        OperatingInviteRole("APPROVER", "Warehouse"),
        OperatingInviteRole("OPERATIONS_LEAD", "Route lead"),
        OperatingInviteRole("CONTRIBUTOR", "Salesman"),
    )

    fun isDistribution(subtypeCode: String): Boolean =
        subtypeCode.equals("DISTRIBUTION_DEALERSHIP", ignoreCase = true)

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
            OperatingSubtype(
                code = "CLIENT_CAMPAIGN",
                label = "Client Campaign",
                partyLabel = "Client",
                targetMetrics = listOf("LEADS", "CONVERSIONS", "REVENUE", "ORDERS", "REACH", "ENGAGEMENT", "CUSTOM_COUNT", "CLIENT_BILLING"),
                requiresChannel = false,
                quickAdds = marketingAdds + listOf("INVOICE", "COLLECTION"),
                channels = marketingChannels,
                inviteRoles = marketingInviteRoles,
                networkLabel = "Clients & vendors",
            ),
            OperatingSubtype(
                code = "OWN_BUSINESS_CAMPAIGN",
                label = "Own Business Campaign",
                partyLabel = "Brand",
                targetMetrics = listOf("LEADS", "CONVERSIONS", "REVENUE", "ORDERS", "REACH", "ENGAGEMENT", "CUSTOM_COUNT"),
                requiresChannel = false,
                quickAdds = marketingAdds,
                channels = marketingChannels,
                inviteRoles = marketingInviteRoles,
                networkLabel = "Vendors & creators",
            ),
        ),
        BusinessSetupKind.TRADING_DISTRIBUTION to listOf(
            OperatingSubtype(
                code = "TRADING_WHOLESALE",
                label = "Trading / Wholesale",
                partyLabel = "Business",
                targetMetrics = listOf("REVENUE", "ORDERS", "CUSTOM_COUNT"),
                requiresChannel = false,
                quickAdds = listOf("SALE", "PURCHASE", "SPEND", "PAYMENT", "COLLECTION", "BUDGET", "UPDATE", "ISSUE", "APPROVAL", "STOCK", "ORDER", "SHIPMENT"),
                channels = tradingChannels,
                inviteRoles = tradingInviteRoles,
                networkLabel = "Principals & dealers",
            ),
            OperatingSubtype(
                code = "DISTRIBUTION_DEALERSHIP",
                label = "Distribution / Dealership",
                partyLabel = "Principal",
                targetMetrics = listOf("REVENUE", "ORDERS", "CUSTOM_COUNT"),
                requiresChannel = false,
                quickAdds = listOf("SALE", "PURCHASE", "SPEND", "PAYMENT", "COLLECTION", "BUDGET", "UPDATE", "ISSUE", "APPROVAL", "STOCK", "ORDER", "SHIPMENT"),
                channels = distributionChannels,
                inviteRoles = distributionInviteRoles,
                networkLabel = "Suppliers & buyers",
            ),
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
        "gstTracking", "gstRegistration", "gstState", "gstStateCode", "gstin", "salePriceIncludesGst", "purchasePriceIncludesGst", "gstEffectiveFrom",
        "participants", "counterparties", "channels", "tradeReach", "monthlyBudget", "monthlyTarget", "targetMetric",
        "clientFee", "offering", "creativeApprovalRequired", "reportingCadence",
        "conversionNumerator", "conversionDenominator", "conversionDefinitionVersion", "campaignStatus",
    )
}
