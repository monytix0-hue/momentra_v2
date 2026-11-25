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
    /** Setup §04 network row — QA-01 Business contacts (Supplier / Customer). */
    val networkLabel: String = "Business contacts",
)

object OperatingMomentCatalog {
    /** Corrections BS-01 / BS-02 — business-specific channel seeds. */
    val tradingChannels = listOf("Wholesale", "Retail", "B2B Direct", "Online", "Distributor", "Other")

    val distributionChannels = listOf(
        "General Trade",
        "Modern Trade",
        "Wholesale",
        "Institutional",
        "Direct Distribution",
        "Van Sales",
        "Other",
    )

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

    /** Corrections BS-03 — 18 marketing KPIs (+ client billing for client campaigns). */
    val marketingKpiMetrics = listOf(
        "LEADS",
        "QUALIFIED_LEADS",
        "ENQUIRIES",
        "REACH",
        "IMPRESSIONS",
        "ENGAGEMENTS",
        "ENGAGEMENT_RATE",
        "CLICKS",
        "CTR",
        "WEBSITE_VISITS",
        "CONVERSIONS",
        "CONVERSION_RATE",
        "ORDERS",
        "ATTRIBUTED_SALES",
        "ROAS",
        "REGISTRATIONS",
        "ATTENDANCE",
        "CUSTOM",
    )

    /**
     * Client / Own setup + invite labels — Figma MKT-MASTER Invite (`2569:29329`).
     * API has no `MEDIA_BUYER` role; Media buyer and Creative both mint as `CONTRIBUTOR`
     * (selection is by label, same as APK `MarketingClientInviteRoles`).
     */
    val marketingInviteRoles = listOf(
        OperatingInviteRole("FINANCE", "Owner"),
        OperatingInviteRole("APPROVER", "Manager"),
        OperatingInviteRole("OPERATIONS_LEAD", "Account lead"),
        OperatingInviteRole("CONTRIBUTOR", "Media buyer"),
        OperatingInviteRole("CONTRIBUTOR", "Creative"),
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

    // v5.1 AT-01: 8 shared Quick Adds; Client adds INVOICE+COLLECTION (=10).
    // QA-09 is a single UPDATE tile ("Team Update / Issue"); ISSUE writes stay allowed via UPDATE grant.
    private val marketingAdds = listOf(
        "ACTIVITY", "PROGRESS", "SPEND", "PAYMENT", "REVISION", "APPROVAL", "UPDATE", "RESULT",
    )

    val families: Map<BusinessSetupKind, List<OperatingSubtype>> = mapOf(
        BusinessSetupKind.MARKETING_CAMPAIGNS to listOf(
            OperatingSubtype(
                code = "CLIENT_CAMPAIGN",
                label = "Client Campaign",
                partyLabel = "Client",
                targetMetrics = marketingKpiMetrics + listOf("CLIENT_BILLING"),
                requiresChannel = false,
                quickAdds = marketingAdds + listOf("INVOICE", "COLLECTION"),
                channels = marketingChannels,
                inviteRoles = marketingInviteRoles,
                networkLabel = "Clients & providers",
            ),
            OperatingSubtype(
                code = "OWN_BUSINESS_CAMPAIGN",
                label = "Own Business Campaign",
                partyLabel = "Brand",
                targetMetrics = marketingKpiMetrics,
                requiresChannel = false,
                quickAdds = marketingAdds,
                channels = marketingChannels,
                inviteRoles = marketingInviteRoles,
                networkLabel = "Providers & creators",
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
                networkLabel = "Business contacts",
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
                networkLabel = "Suppliers & customers",
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
        "UPDATE" to "Team Update / Issue",
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
        "clientFee", "clientApprovedMediaBudget", "agencyOperatingBudget", "offering", "campaignPurpose", "creativeApprovalRequired", "reportingCadence",
        "conversionNumerator", "conversionDenominator", "conversionDefinitionVersion", "campaignStatus",
    )

    /** Corrections BS-04 — searchable unit master grouped by measurement category. */
    data class UnitMasterEntry(val code: String, val label: String, val category: String)

    val unitMaster: List<UnitMasterEntry> = listOf(
        UnitMasterEntry("PC", "Piece (pc)", "Count"),
        UnitMasterEntry("PAIR", "Pair", "Count"),
        UnitMasterEntry("SET", "Set", "Count"),
        UnitMasterEntry("PACK", "Pack", "Count"),
        UnitMasterEntry("BOX", "Box", "Count"),
        UnitMasterEntry("CARTON", "Carton", "Count"),
        UnitMasterEntry("DOZEN", "Dozen (12 pc)", "Count"),
        UnitMasterEntry("KG", "Kilogram (kg)", "Mass"),
        UnitMasterEntry("G", "Gram (g)", "Mass"),
        UnitMasterEntry("TONNE", "Tonne", "Mass"),
        UnitMasterEntry("L", "Litre (L)", "Volume"),
        UnitMasterEntry("ML", "Millilitre (mL)", "Volume"),
        UnitMasterEntry("M", "Metre (m)", "Length"),
        UnitMasterEntry("CM", "Centimetre (cm)", "Length"),
        UnitMasterEntry("FT", "Foot", "Length"),
        UnitMasterEntry("SQFT", "Square Foot (sq ft)", "Area"),
        UnitMasterEntry("SQM", "Square Metre (sq m)", "Area"),
    )

    val unitCategories: List<String> = listOf("Count", "Mass", "Volume", "Length", "Area")

    fun unitsForCategory(category: String): List<UnitMasterEntry> =
        unitMaster.filter { it.category.equals(category, ignoreCase = true) }
}
