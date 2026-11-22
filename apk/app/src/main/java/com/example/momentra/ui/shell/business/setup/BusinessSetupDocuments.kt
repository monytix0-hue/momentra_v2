package com.example.momentra.ui.shell.business.setup

/** Expandable module row title + optional Figma subtitle. */
data class BusinessSetupModuleRow(
    val title: String,
    val subtitle: String = "",
)

/** Field / section copy for the nine Figma setup-v6 documents. Lockstep with iOS. */
data class BusinessSetupFieldSpec(
    val key: String,
    val label: String,
    val placeholder: String = "",
    val kind: Kind,
    val helper: String = "",
) {
    sealed class Kind {
        data object Text : Kind()
        data class Choice(val options: List<String>) : Kind()
        data class MultiChoice(val options: List<String>) : Kind()
        data object Toggle : Kind()
        data object Date : Kind()
        data class RowAction(val action: String) : Kind()
        data class MemberList(val members: List<String>) : Kind()
        data class ModuleList(val modules: List<BusinessSetupModuleRow>) : Kind()
        data class ReviewSummary(val keys: List<String>) : Kind()
        data class PanelHeader(val title: String) : Kind()
    }
}

/** Pure chrome helpers for field error highlighting (unit-testable). */
object BusinessSetupFieldChrome {
    fun hasError(error: String?): Boolean = !error.isNullOrBlank()
}

data class BusinessSetupSectionSpec(
    val id: String,
    val title: String,
    val subtitle: String,
    val fields: List<BusinessSetupFieldSpec>,
)

data class BusinessSetupDocumentSpec(
    val typeCode: String,
    val familyTitle: String,
    val eyebrow: String,
    val heroTitle: String,
    val heroSubtitle: String,
    val activateLabel: String,
    val defaultTitleKey: String,
    val sections: List<BusinessSetupSectionSpec>,
) {
    val sectionTitles: List<String> get() = sections.map { it.title }

    fun defaultPreferences(): MutableMap<String, String> {
        val prefs = mutableMapOf<String, String>()
        sections.forEach { section ->
            section.fields.forEach { field ->
                prefs[field.key] = when (val kind = field.kind) {
                    is BusinessSetupFieldSpec.Kind.Text,
                    is BusinessSetupFieldSpec.Kind.Date,
                    is BusinessSetupFieldSpec.Kind.RowAction,
                    -> ""
                    is BusinessSetupFieldSpec.Kind.Choice -> kind.options.firstOrNull().orEmpty()
                    is BusinessSetupFieldSpec.Kind.MultiChoice -> kind.options.firstOrNull().orEmpty()
                    is BusinessSetupFieldSpec.Kind.Toggle -> "Yes"
                    is BusinessSetupFieldSpec.Kind.MemberList -> kind.members.joinToString("|")
                    is BusinessSetupFieldSpec.Kind.ModuleList,
                    is BusinessSetupFieldSpec.Kind.ReviewSummary,
                    is BusinessSetupFieldSpec.Kind.PanelHeader,
                    -> ""
                }
            }
        }
        return prefs
    }
}

object BusinessSetupDocuments {
    fun spec(forTypeCode: String): BusinessSetupDocumentSpec =
        when (forTypeCode.uppercase()) {
            "OWN_BUSINESS_CAMPAIGN" -> ownMarketing
            "TRADING_WHOLESALE" -> trading
            "DISTRIBUTION_DEALERSHIP" -> distribution
            "RETAIL_STORE" -> retail
            "RESTAURANT_FOOD" -> restaurant
            "CONSUMER_SERVICE" -> service
            "STANDARD_PRODUCTION" -> manufacturing
            "MADE_TO_ORDER" -> madeToOrder
            else -> clientMarketing
        }

    private fun section(
        id: String,
        title: String,
        subtitle: String,
        fields: List<BusinessSetupFieldSpec>,
    ) = BusinessSetupSectionSpec(id, title, subtitle, fields)

    private fun text(key: String, label: String, placeholder: String, helper: String = "") =
        BusinessSetupFieldSpec(key, label, placeholder, BusinessSetupFieldSpec.Kind.Text, helper)

    private fun choice(key: String, label: String, options: List<String>, helper: String = "") =
        BusinessSetupFieldSpec(key, label, kind = BusinessSetupFieldSpec.Kind.Choice(options), helper = helper)

    private fun multiChoice(key: String, label: String, options: List<String>, helper: String = "") =
        BusinessSetupFieldSpec(key, label, kind = BusinessSetupFieldSpec.Kind.MultiChoice(options), helper = helper)

    private fun toggle(key: String, label: String, helper: String = "") =
        BusinessSetupFieldSpec(key, label, kind = BusinessSetupFieldSpec.Kind.Toggle, helper = helper)

    private fun date(key: String, label: String, placeholder: String, helper: String = "") =
        BusinessSetupFieldSpec(key, label, placeholder, BusinessSetupFieldSpec.Kind.Date, helper)

    private fun rowAction(key: String, label: String, action: String) =
        BusinessSetupFieldSpec(key, label, action, BusinessSetupFieldSpec.Kind.RowAction(action))

    private fun memberList(key: String, label: String, members: List<String>) =
        BusinessSetupFieldSpec(key, label, kind = BusinessSetupFieldSpec.Kind.MemberList(members))

    private fun moduleList(key: String, label: String, modules: List<String>) =
        BusinessSetupFieldSpec(
            key,
            label,
            kind = BusinessSetupFieldSpec.Kind.ModuleList(modules.map { BusinessSetupModuleRow(it) }),
        )

    private fun moduleListRows(
        key: String,
        label: String,
        modules: List<BusinessSetupModuleRow>,
        helper: String = "",
    ) = BusinessSetupFieldSpec(
        key,
        label,
        kind = BusinessSetupFieldSpec.Kind.ModuleList(modules),
        helper = helper,
    )

    private fun panelHeader(key: String, title: String) =
        BusinessSetupFieldSpec(key, title, kind = BusinessSetupFieldSpec.Kind.PanelHeader(title))

    private fun reviewSummary(key: String, label: String, keys: List<String>) =
        BusinessSetupFieldSpec(key, label, kind = BusinessSetupFieldSpec.Kind.ReviewSummary(keys))

    /** Figma `5:33148` — five sections (Network/Catalogue omitted by product choice). */
    val clientMarketing = BusinessSetupDocumentSpec(
        typeCode = "CLIENT_CAMPAIGN",
        familyTitle = "Marketing",
        eyebrow = "Client campaign",
        heroTitle = "Brightline Setup",
        heroSubtitle = "Bring your business, people, and monthly plan together before you start.",
        activateLabel = "Start this business",
        defaultTitleKey = "campaignName",
        sections = listOf(
            section(
                "basics", "Business Basics", "A home for this client campaign.",
                listOf(
                    panelHeader("panelCampaign", "Campaign"),
                    text("campaignName", "Campaign name", "Brightline Creative for Northstar Residences"),
                    text(
                        "companyName", "Company", "Brightline Creative",
                        helper = "Agency / firm name",
                    ),
                    text(
                        "gstin", "GSTIN", "27AAKCM4321D1Z2",
                        helper = "15-digit GSTIN, if you have one",
                    ),
                    text("currency", "Currency", "INR · Indian Rupee (₹)"),
                    date("startDate", "Start date", "12 October 2026"),
                    date("endDate", "End date", "Still open"),
                    panelHeader("panelClient", "Client"),
                    text("clientName", "Client", "Northstar Residences"),
                    text("clientContact", "Client contact (optional)", "Optional"),
                    panelHeader("panelPurpose", "Purpose & structure"),
                    choice(
                        "campaignPurpose", "Campaign purpose",
                        listOf("Leads", "Launch", "Awareness", "Sales", "Event", "Traffic"),
                    ),
                    text("promotedBrand", "Promoted brand", "Optional"),
                ),
            ),
            section(
                "people", "People & Roles",
                "Invite teammates and pick a role on Invite & QR. No separate Add person.",
                listOf(
                    memberList("teamMembers", "Team", listOf("You (Owner)")),
                    multiChoice(
                        "roles", "Roles on invite",
                        listOf("Owner", "Admin", "Campaign Lead", "Finance", "Approver"),
                        helper = "Default Campaign Lead · owns delivery & reporting. Role applies when they join.",
                    ),
                ),
            ),
            section(
                "channels", "Channels & Monthly Plan",
                "Channels, client commercials, budget and targets.",
                listOf(
                    panelHeader("panelChannels", "Channels"),
                    multiChoice(
                        "channels", "Where do you run it?",
                        listOf("Paid social", "Search", "Print", "Influencer", "Events"),
                        helper = "These are the places you run the campaign. They are not a different kind of business.",
                    ),
                    panelHeader("panelCommercials", "Client commercials"),
                    text("clientFee", "Client fee", "₹1,50,000", helper = "Amount"),
                    choice(
                        "billingBasis", "Billing basis",
                        listOf("Retainer", "Deliverable", "Pass-through"),
                    ),
                    panelHeader("panelBudget", "Budget"),
                    date("planMonth", "Month", "October 2026"),
                    text("monthlyBudget", "Monthly budget", "₹2,00,000", helper = "Amount"),
                    text("campaignBudget", "Campaign budget", "₹6,00,000", helper = "Optional · aliases into monthly if empty"),
                    text(
                        "clientApprovedMediaBudget", "Client-approved media", "₹4,00,000",
                        helper = "Optional",
                    ),
                    text(
                        "agencyOperatingBudget", "Agency operating", "₹50,000",
                        helper = "Optional",
                    ),
                    panelHeader("panelTargets", "Targets"),
                    text("monthlyTarget", "Primary target", "120", helper = "Amount / count"),
                    choice(
                        "targetMetric", "Target unit",
                        listOf("Leads", "Reach", "Clicks", "Sales"),
                    ),
                ),
            ),
            section(
                "gst", "GST & Tax Details", "Set up GST once to make buying and selling easier.",
                listOf(
                    panelHeader("panelTracking", "Tracking"),
                    choice(
                        "gstTracking", "Do you want to track GST in this Moment?",
                        listOf("Yes", "Not now"),
                        helper = "Skipping means tax tracking is disabled in Momentra. It does not mean the business is legally GST-exempt.",
                    ),
                    panelHeader("panelRegistration", "Registration"),
                    choice(
                        "gstRegistration", "How is your business registered for GST?",
                        listOf("Regular", "Composition", "Unregistered", "Not sure"),
                    ),
                    text(
                        "gstState", "Which state is your business registered in?", "Maharashtra",
                        helper = "State",
                    ),
                    panelHeader("panelPricing", "Pricing"),
                    choice(
                        "salePriceIncludesGst", "Do prices include GST?",
                        listOf("Prices include GST", "GST is added separately"),
                        helper = "Your GSTIN stays the one entered above. It is stored as a reference only.",
                    ),
                    date("gstEffectiveFrom", "Effective from (optional)", "YYYY-MM-DD"),
                ),
            ),
            section(
                "review", "Review your setup", "Confirm before you start.",
                listOf(
                    reviewSummary(
                        "review", "Summary",
                        listOf(
                            "clientName",
                            "gstin",
                            "channels",
                            "monthlyBudget",
                            "clientFee",
                            "monthlyTarget",
                            "targetMetric",
                        ),
                    ),
                ),
            ),
        ),
    )

    val ownMarketing = BusinessSetupDocumentSpec(
        typeCode = "OWN_BUSINESS_CAMPAIGN",
        familyTitle = "Marketing",
        eyebrow = "Own business",
        heroTitle = "Promote your own business",
        heroSubtitle = "Track ads, offers, and inbound interest for your brand — not a client.",
        activateLabel = "Start this business",
        defaultTitleKey = "campaignName",
        sections = listOf(
            section(
                "basics", "Business Basics", "What you're promoting.",
                listOf(
                    text("campaignName", "Campaign name", "Summer launch"),
                    text("brandName", "Brand / business", "Harbor Mart"),
                    choice("offerType", "Offer type", listOf("Promotion", "Launch", "Always-on")),
                    date("startDate", "Start date", "01 Nov 2024"),
                    text("currency", "Currency", "INR"),
                ),
            ),
            section(
                "people", "People & Roles", "Who owns the spend.",
                listOf(
                    memberList("teamMembers", "Team", listOf("You (Owner)")),
                    multiChoice("roles", "Roles", listOf("Owner", "Admin", "Marketer", "Viewer")),
                    rowAction("addPeople", "Add member", "Add"),
                ),
            ),
            section(
                "network", "Network / Counterparties", "Channels and tools you use.",
                listOf(
                    moduleList(
                        "modules", "Modules",
                        listOf("Ad platforms", "Landing pages", "CRM / leads", "Creative assets"),
                    ),
                ),
            ),
            section(
                "channels", "Channels & Monthly Plan", "Where your ads run.",
                listOf(
                    multiChoice("channels", "Channels", listOf("Meta", "Google", "WhatsApp", "Other")),
                    text("monthlyBudget", "Monthly budget", "₹50,000"),
                    text("conversionGoal", "Conversion goal", "Store visits / sales"),
                ),
            ),
            section(
                "gst", "GST & Tax Details", "Your business registration.",
                listOf(
                    toggle("gstRegistered", "Registered for GST?"),
                    choice("gstType", "Registration type", listOf("Regular", "Composition", "Unregistered")),
                    text("gstState", "State", "Maharashtra"),
                    text("gstin", "GSTIN", "Optional"),
                ),
            ),
            section(
                "review", "Review your setup", "Confirm before you start.",
                listOf(
                    reviewSummary(
                        "review", "Summary",
                        listOf("campaignName", "brandName", "offerType", "monthlyBudget", "conversionGoal"),
                    ),
                ),
            ),
        ),
    )

    /** Figma `5:18833` TRD-SETUP-v6 — Catalogue/Network section omitted (five sections). */
    val trading = BusinessSetupDocumentSpec(
        typeCode = "TRADING_WHOLESALE",
        familyTitle = "Trading",
        eyebrow = "Trading",
        heroTitle = "Northstar Trading Setup",
        heroSubtitle = "Bring your business, people, and monthly plan together before you start.",
        activateLabel = "Start this business",
        defaultTitleKey = "businessName",
        sections = listOf(
            section(
                "basics", "Business Basics", "A home for this trading business.",
                listOf(
                    panelHeader("panelIdentity", "Identity"),
                    text("businessName", "Business name", "Northstar Trading"),
                    text(
                        "companyName", "Company name", "Northstar Trading",
                        helper = "Registered company name",
                    ),
                    text(
                        "gstin", "GSTIN", "29ABCDE4321T1Z5",
                        helper = "15-digit GSTIN, if you have one",
                    ),
                    text("currency", "Currency", "INR · Indian Rupee (₹)"),
                    date("startDate", "Start date", "12 October 2026"),
                    date("endDate", "End date", "Still open"),
                    panelHeader("panelDates", "Dates & more"),
                    text("description", "Description", "Everyday notes about this business"),
                    text("location", "Location", "Add your DC location"),
                ),
            ),
            section(
                "people", "People & Roles",
                "Invite teammates and pick a role on Invite & QR. No separate Add person.",
                listOf(
                    memberList("teamMembers", "Team", listOf("You (Owner)")),
                    multiChoice(
                        "roles", "Roles on invite",
                        listOf("Owner", "Manager", "Sales exec", "Warehouse", "Sales rep"),
                        helper = "Default Sales rep · own beat orders & collections. Role applies when they join.",
                    ),
                ),
            ),
            section(
                "channels", "Channels & Monthly Plan", "Where do you sell? Choose at least one.",
                listOf(
                    panelHeader("panelChannels", "Channels"),
                    multiChoice(
                        "channels", "Channels",
                        listOf("Retail buyers", "Wholesale", "Modern trade", "HoReCa", "Online"),
                        helper = "These are the places you sell or serve. They are not a different kind of business.",
                    ),
                    date("planMonth", "Month", "October 2026"),
                    text("monthlyBudget", "Monthly budget", "₹60,00,000", helper = "Amount"),
                    text("monthlyTarget", "Monthly sales target", "₹90,00,000", helper = "Amount"),
                    text("targetMetric", "How the target is measured", "Revenue"),
                ),
            ),
            section(
                "gst", "GST & Tax Details", "Set up GST once to make buying and selling easier.",
                listOf(
                    panelHeader("panelTracking", "Tracking"),
                    choice(
                        "gstTracking", "Do you want to track GST in this Moment?",
                        listOf("Yes", "Not now"),
                        helper = "Skipping means tax tracking is disabled in Momentra. It does not mean the business is legally GST-exempt.",
                    ),
                    panelHeader("panelRegistration", "Registration"),
                    choice(
                        "gstRegistration", "How is your business registered for GST?",
                        listOf("Regular", "Composition", "Unregistered", "Not sure"),
                    ),
                    text(
                        "gstState", "Which state is your business registered in?", "Maharashtra",
                        helper = "State",
                    ),
                    panelHeader("panelPricing", "Pricing"),
                    choice(
                        "salePriceIncludesGst", "Do prices include GST?",
                        listOf("Prices include GST", "GST is added separately"),
                        helper = "Your GSTIN stays the one entered above. It is stored as a reference only.",
                    ),
                    date("gstEffectiveFrom", "Effective from (optional)", "YYYY-MM-DD"),
                ),
            ),
            section(
                "review", "Review your setup", "Confirm before you start.",
                listOf(
                    reviewSummary(
                        "review", "Summary",
                        listOf(
                            "companyName",
                            "gstin",
                            "channels",
                            "monthlyBudget",
                            "monthlyTarget",
                            "targetMetric",
                        ),
                    ),
                ),
            ),
        ),
    )

    /** Figma `5:8637` — five sections (Catalogue/Network omitted by product choice). */
    val distribution = BusinessSetupDocumentSpec(
        typeCode = "DISTRIBUTION_DEALERSHIP",
        familyTitle = "Trading",
        eyebrow = "Distribution",
        heroTitle = "Metro Distribution Setup",
        heroSubtitle = "Bring your business, people, and monthly plan together before you start.",
        activateLabel = "Start this business",
        defaultTitleKey = "businessName",
        sections = listOf(
            section(
                "basics", "Business Basics", "A home for this distribution business.",
                listOf(
                    panelHeader("panelIdentity", "Identity"),
                    text("businessName", "Business name", "Metro Distribution"),
                    text(
                        "companyName", "Company name", "Metro Distribution",
                        helper = "Registered company name",
                    ),
                    text(
                        "gstin", "GSTIN", "27AAKCM4321D1Z2",
                        helper = "15-digit GSTIN, if you have one",
                    ),
                    text("currency", "Currency", "INR · Indian Rupee (₹)"),
                    date("startDate", "Start date", "12 October 2026"),
                    date("endDate", "End date", "Still open"),
                    panelHeader("panelDates", "Dates & more"),
                    text("description", "Description", "Everyday notes about this business"),
                    text("location", "Location", "Add your DC location"),
                ),
            ),
            section(
                "people", "People & Roles",
                "Invite teammates and pick a role on Invite & QR. No separate Add person.",
                listOf(
                    memberList("teamMembers", "Team", listOf("You (Owner)")),
                    multiChoice(
                        "roles", "Roles on invite",
                        listOf("Owner", "Manager", "Route lead", "Warehouse", "Salesman"),
                        helper = "Default Salesman · own beat orders & collections. Role applies when they join.",
                    ),
                ),
            ),
            section(
                "channels", "Channels & Monthly Plan", "Where do you sell? Choose at least one.",
                listOf(
                    panelHeader("panelChannels", "Channels"),
                    multiChoice(
                        "channels", "Channels",
                        listOf("Retail dealers", "Wholesale", "Modern trade", "HoReCa", "Van sales"),
                        helper = "These are the places you sell or serve. They are not a different kind of business.",
                    ),
                    date("planMonth", "Month", "October 2026"),
                    text("monthlyBudget", "Monthly budget", "₹60,00,000", helper = "Amount"),
                    text("monthlyTarget", "Monthly sales target", "₹90,00,000", helper = "Amount"),
                    text("targetMetric", "How the target is measured", "Revenue"),
                ),
            ),
            section(
                "gst", "GST & Tax Details", "Set up GST once to make buying and selling easier.",
                listOf(
                    panelHeader("panelTracking", "Tracking"),
                    choice(
                        "gstTracking", "Do you want to track GST in this Moment?",
                        listOf("Yes", "Not now"),
                        helper = "Skipping means tax tracking is disabled in Momentra. It does not mean the business is legally GST-exempt.",
                    ),
                    panelHeader("panelRegistration", "Registration"),
                    choice(
                        "gstRegistration", "How is your business registered for GST?",
                        listOf("Regular", "Composition", "Unregistered", "Not sure"),
                    ),
                    text(
                        "gstState", "Which state is your business registered in?", "Maharashtra",
                        helper = "State",
                    ),
                    panelHeader("panelPricing", "Pricing"),
                    choice(
                        "salePriceIncludesGst", "Do prices include GST?",
                        listOf("Prices include GST", "GST is added separately"),
                        helper = "Your GSTIN stays the one entered above. It is stored as a reference only.",
                    ),
                    date("gstEffectiveFrom", "Effective from (optional)", "YYYY-MM-DD"),
                ),
            ),
            section(
                "review", "Review your setup", "Confirm before you start.",
                listOf(
                    reviewSummary(
                        "review", "Summary",
                        listOf(
                            "companyName",
                            "gstin",
                            "channels",
                            "monthlyBudget",
                            "monthlyTarget",
                            "targetMetric",
                        ),
                    ),
                ),
            ),
        ),
    )

    /** Figma `5:80797` — five sections (Catalogue/Network omitted by product choice). */
    val retail = BusinessSetupDocumentSpec(
        typeCode = "RETAIL_STORE",
        familyTitle = "Retail",
        eyebrow = "Retail store",
        heroTitle = "Harbor Mart Setup",
        heroSubtitle = "Bring your business, people, and monthly plan together before you start.",
        activateLabel = "Start this store",
        defaultTitleKey = "storeName",
        sections = listOf(
            section(
                "basics", "Business Basics", "A home for this store.",
                listOf(
                    panelHeader("panelIdentity", "Identity"),
                    text("storeName", "Store name", "Harbor Mart"),
                    text(
                        "companyName", "Company name", "Harbor Mart",
                        helper = "Registered company name",
                    ),
                    text(
                        "gstin", "GSTIN", "27AAHCH4321M1Z3",
                        helper = "15-digit GSTIN, if you have one",
                    ),
                    text("currency", "Currency", "INR · Indian Rupee (₹)"),
                    date("startDate", "Start date", "12 October 2026"),
                    date("endDate", "End date", "Still open"),
                    panelHeader("panelDates", "Dates & more"),
                    text("description", "Description", "Everyday notes about this store"),
                    text("location", "Location", "Add your store location"),
                ),
            ),
            section(
                "people", "People & Roles",
                "Invite teammates and pick a role on Invite & QR. No separate Add person.",
                listOf(
                    memberList("teamMembers", "Team", listOf("You (Owner)")),
                    multiChoice(
                        "roles", "Roles on invite",
                        listOf("Owner", "Manager", "Cashier", "Stock", "Floor"),
                        helper = "Default Cashier · POS, bills & returns. Role applies when they join.",
                    ),
                ),
            ),
            section(
                "channels", "Channels & Monthly Plan", "Where do you sell? Choose at least one.",
                listOf(
                    panelHeader("panelChannels", "Channels"),
                    multiChoice(
                        "channels", "Channels",
                        listOf("Counter", "Home", "Online", "B2B", "Market"),
                        helper = "These are the places you sell or serve. They are not a different kind of business.",
                    ),
                    date("planMonth", "Month", "October 2026"),
                    text("monthlyBudget", "Monthly budget", "₹8,40,000", helper = "Amount"),
                    text("monthlyTarget", "Monthly sales target", "₹12,00,000", helper = "Amount"),
                    text("targetMetric", "How the target is measured", "Revenue"),
                ),
            ),
            section(
                "gst", "GST & Tax Details", "Set up GST once to make buying and selling easier.",
                listOf(
                    panelHeader("panelTracking", "Tracking"),
                    choice(
                        "gstTracking", "Do you want to track GST in this Moment?",
                        listOf("Yes", "Not now"),
                        helper = "Skipping means tax tracking is disabled in Momentra. It does not mean the business is legally GST-exempt.",
                    ),
                    panelHeader("panelRegistration", "Registration"),
                    choice(
                        "gstRegistration", "How is your business registered for GST?",
                        listOf("Regular", "Composition", "Unregistered", "Not sure"),
                    ),
                    text(
                        "gstState", "Which state is your business registered in?", "Maharashtra",
                        helper = "State",
                    ),
                    panelHeader("panelPricing", "Pricing"),
                    choice(
                        "salePriceIncludesGst", "Do prices include GST?",
                        listOf("Prices include GST", "GST is added separately"),
                        helper = "Your GSTIN stays the one entered above. It is stored as a reference only.",
                    ),
                    date("gstEffectiveFrom", "Effective from (optional)", "YYYY-MM-DD"),
                ),
            ),
            section(
                "review", "Review your setup", "Confirm before you start.",
                listOf(
                    reviewSummary(
                        "review", "Summary",
                        listOf(
                            "companyName",
                            "gstin",
                            "channels",
                            "monthlyBudget",
                            "monthlyTarget",
                            "targetMetric",
                        ),
                    ),
                ),
            ),
        ),
    )

    /** Figma `5:90612` RST-SETUP-v6 — §04 modules are display-only (no masters sheets). */
    val restaurant = BusinessSetupDocumentSpec(
        typeCode = "RESTAURANT_FOOD",
        familyTitle = "Retail",
        eyebrow = "Restaurant",
        heroTitle = "Spice Route Setup",
        heroSubtitle = "Bring your business, people, and monthly plan together before you start.",
        activateLabel = "Start this restaurant",
        defaultTitleKey = "outletName",
        sections = listOf(
            section(
                "basics", "Business Basics", "A home for this restaurant.",
                listOf(
                    panelHeader("panelIdentity", "Identity"),
                    text("outletName", "Restaurant name", "Spice Route Kitchen"),
                    text(
                        "companyName", "Company name", "Spice Route Kitchen",
                        helper = "Registered company name",
                    ),
                    text(
                        "gstin", "GSTIN", "27AASCS4821K1Z3",
                        helper = "15-digit GSTIN, if you have one",
                    ),
                    text("currency", "Currency", "INR · Indian Rupee (₹)"),
                    date("startDate", "Start date", "12 October 2026"),
                    date("endDate", "End date", "Still open"),
                    panelHeader("panelDates", "Dates & more"),
                    text("description", "Description", "Everyday notes about this restaurant"),
                    text("location", "Location", "Add your restaurant location"),
                ),
            ),
            section(
                "people", "People & Roles",
                "Invite teammates and pick a role on Invite & QR. No separate Add person.",
                listOf(
                    memberList("teamMembers", "Team", listOf("You (Owner)")),
                    multiChoice(
                        "roles", "Roles on invite",
                        listOf("Owner", "Manager", "Cashier", "Kitchen", "Waiter"),
                        helper = "Default Cashier · POS, bills & KOT view. Role applies when they join.",
                    ),
                ),
            ),
            section(
                "channels", "Channels & Monthly Plan", "Where do you sell? Choose at least one.",
                listOf(
                    panelHeader("panelChannels", "Channels"),
                    multiChoice(
                        "channels", "Channels",
                        listOf("Dine-in", "Direct", "Zomato", "Takeaway", "Swiggy"),
                        helper = "These are the places you sell or serve. They are not a different kind of business.",
                    ),
                    date("planMonth", "Month", "October 2026"),
                    text("monthlyBudget", "Monthly budget", "₹5,20,000", helper = "Amount"),
                    text("monthlyTarget", "Monthly sales target", "₹6,50,000", helper = "Amount"),
                    text("targetMetric", "How the target is measured", "Revenue"),
                ),
            ),
            section(
                "network", "Kitchen, Menu & Network",
                "Modules your POS, KOT and purchases will use after start.",
                listOf(
                    moduleListRows(
                        "modules", "Modules",
                        listOf(
                            BusinessSetupModuleRow("Menu", "42 dishes · GST 5% restaurant"),
                            BusinessSetupModuleRow("Stations", "Tandoor · Hot · Cold · Bar · Floor"),
                            BusinessSetupModuleRow("Vendors", "3 suppliers · GSTIN on file"),
                            BusinessSetupModuleRow(
                                "Team invite & QR",
                                "Owner · Manager · Cashier · Kitchen · Waiter",
                            ),
                        ),
                    ),
                ),
            ),
            section(
                "gst", "GST & Tax Details", "Set up GST once to make buying and selling easier.",
                listOf(
                    panelHeader("panelTracking", "Tracking"),
                    choice(
                        "gstTracking", "Do you want to track GST in this Moment?",
                        listOf("Yes", "Not now"),
                        helper = "Skipping means tax tracking is disabled in Momentra. It does not mean the business is legally GST-exempt.",
                    ),
                    panelHeader("panelRegistration", "Registration"),
                    choice(
                        "gstRegistration", "How is your business registered for GST?",
                        listOf("Regular", "Composition", "Unregistered", "Not sure"),
                    ),
                    text(
                        "gstState", "Which state is your business registered in?", "Maharashtra",
                        helper = "State",
                    ),
                    panelHeader("panelPricing", "Pricing"),
                    choice(
                        "salePriceIncludesGst", "Do prices include GST?",
                        listOf("Prices include GST", "GST is added separately"),
                        helper = "Your GSTIN stays the one entered above. It is stored as a reference only.",
                    ),
                    date("gstEffectiveFrom", "Effective from (optional)", "YYYY-MM-DD"),
                ),
            ),
            section(
                "review", "Review your setup", "Confirm before you start.",
                listOf(
                    reviewSummary(
                        "review", "Summary",
                        listOf(
                            "companyName",
                            "gstin",
                            "channels",
                            "monthlyBudget",
                            "monthlyTarget",
                            "targetMetric",
                        ),
                    ),
                ),
            ),
        ),
    )

    val service = BusinessSetupDocumentSpec(
        typeCode = "CONSUMER_SERVICE",
        familyTitle = "Retail",
        eyebrow = "Services",
        heroTitle = "BrightCare Setup",
        heroSubtitle = "Bring your business, people, and monthly plan together before you start.",
        activateLabel = "Start this business",
        defaultTitleKey = "serviceName",
        sections = listOf(
            section(
                "basics", "Business Basics", "A home for this salon & spa.",
                listOf(
                    panelHeader("panelIdentity", "Identity"),
                    text("serviceName", "Business name", "BrightCare"),
                    text(
                        "companyName", "Company name", "BrightCare",
                        helper = "Registered company name",
                    ),
                    text(
                        "gstin", "GSTIN", "27AAHCB4321M1Z8",
                        helper = "15-digit GSTIN, if you have one",
                    ),
                    text("currency", "Currency", "INR · Indian Rupee (₹)"),
                    date("startDate", "Start date", "12 October 2026"),
                    date("endDate", "End date", "Still open"),
                    panelHeader("panelDates", "Dates & more"),
                    text("description", "Description", "Everyday notes about this business"),
                    text("location", "Location", "Add your studio location"),
                ),
            ),
            section(
                "people", "People & Roles",
                "Invite teammates and pick a role on Invite & QR. No separate Add person.",
                listOf(
                    memberList("teamMembers", "Team", listOf("You (Owner)")),
                    multiChoice(
                        "roles", "Roles on invite",
                        listOf("Owner", "Manager", "Stylist", "Front desk", "Therapist"),
                        helper = "Default Stylist · own calendar, bookings & charge view. Role applies when they join.",
                    ),
                ),
            ),
            section(
                "channels", "Channels & Monthly Plan", "Where do you serve? Choose at least one.",
                listOf(
                    panelHeader("panelChannels", "Channels"),
                    multiChoice(
                        "channels", "Channels",
                        listOf("Studio", "Home", "Online", "Corporate", "Events"),
                        helper = "These are the places you sell or serve. They are not a different kind of business.",
                    ),
                    date("planMonth", "Month", "October 2026"),
                    text("monthlyBudget", "Monthly budget", "₹3,20,000", helper = "Amount"),
                    text(
                        "monthlyChargeTarget", "Monthly charge target", "₹6,50,000",
                        helper = "Amount",
                    ),
                    text("targetMetric", "How the target is measured", "Revenue"),
                ),
            ),
            section(
                "gst", "GST & Tax Details", "Set up GST once to make buying and selling easier.",
                listOf(
                    panelHeader("panelTracking", "Tracking"),
                    choice(
                        "gstTracking", "Do you want to track GST in this Moment?",
                        listOf("Yes", "Not now"),
                        helper = "Skipping means tax tracking is disabled in Momentra. It does not mean the business is legally GST-exempt.",
                    ),
                    panelHeader("panelRegistration", "Registration"),
                    choice(
                        "gstRegistration", "How is your business registered for GST?",
                        listOf("Regular", "Composition", "Unregistered", "Not sure"),
                    ),
                    text("gstState", "Which state is your business registered in?", "Maharashtra", helper = "State"),
                    panelHeader("panelPricing", "Pricing"),
                    choice(
                        "salePriceIncludesGst", "Do prices include GST?",
                        listOf("Prices include GST", "GST is added separately"),
                        helper = "Your GSTIN stays the one entered above. It is stored as a reference only.",
                    ),
                    date("gstEffectiveFrom", "Effective from (optional)", "YYYY-MM-DD"),
                ),
            ),
            section(
                "review", "Review your setup", "Confirm before you start.",
                listOf(
                    reviewSummary(
                        "review", "Summary",
                        listOf(
                            "companyName",
                            "gstin",
                            "channels",
                            "monthlyBudget",
                            "monthlyChargeTarget",
                            "targetMetric",
                        ),
                    ),
                ),
            ),
        ),
    )

    val manufacturing = BusinessSetupDocumentSpec(
        typeCode = "STANDARD_PRODUCTION",
        familyTitle = "Manufacturing",
        eyebrow = "Manufacturing",
        heroTitle = "Set up production",
        heroSubtitle = "Batches, BOM, and finished goods for standard production.",
        activateLabel = "Start this business",
        defaultTitleKey = "plantName",
        sections = listOf(
            section(
                "basics", "Business Basics", "Your plant.",
                listOf(
                    text("plantName", "Plant / business", "Northline Works"),
                    text("productLine", "Product line", "What you make"),
                    choice("batchStyle", "Production", listOf("Batch", "Continuous", "Mixed")),
                    text("gstin", "GSTIN", "Optional"),
                ),
            ),
            section(
                "people", "People & Roles", "Floor and planning.",
                listOf(
                    memberList("teamMembers", "Team", listOf("You (Owner)")),
                    multiChoice("roles", "Roles", listOf("Owner", "Plant manager", "Supervisor", "Operator")),
                    rowAction("addPeople", "Add Member", "Add"),
                ),
            ),
            section(
                "channels", "Channels & Monthly Plan", "Output targets.",
                listOf(
                    text("monthlyOutput", "Monthly output target", "Units / value"),
                    text("wipNote", "WIP note", "Optional"),
                    multiChoice("channels", "Sales channels", listOf("Dealers", "Direct", "Export", "Other")),
                ),
            ),
            section(
                "network", "BOM, Warehouses & Network", "Modules that light up after start.",
                listOf(
                    moduleList(
                        "modules", "Modules",
                        listOf("BOM & recipes", "Raw materials", "Finished goods", "Suppliers & buyers"),
                    ),
                ),
            ),
            section(
                "gst", "GST & Tax Details", "Plant registration.",
                listOf(
                    toggle("gstRegistered", "Registered for GST?"),
                    choice("gstType", "Registration type", listOf("Regular", "Composition", "Unregistered")),
                    text("gstState", "State", "Maharashtra"),
                ),
            ),
            section(
                "review", "Review your setup", "Confirm before you start.",
                listOf(
                    reviewSummary(
                        "review", "Summary",
                        listOf("plantName", "productLine", "batchStyle", "monthlyOutput"),
                    ),
                ),
            ),
        ),
    )

    val madeToOrder = BusinessSetupDocumentSpec(
        typeCode = "MADE_TO_ORDER",
        familyTitle = "Manufacturing",
        eyebrow = "Made to order",
        heroTitle = "Set up job-order work",
        heroSubtitle = "Quotes, job orders, and invoices against custom work.",
        activateLabel = "Start this business",
        defaultTitleKey = "shopName",
        sections = listOf(
            section(
                "basics", "Business Basics", "What customers order.",
                listOf(
                    text("shopName", "Shop / business", "Precision Jobs"),
                    text("jobType", "Job type", "Fabrication / print / …"),
                    choice("quoteStyle", "Quotes", listOf("Formal quotes", "Verbal + invoice", "Both")),
                    text("gstin", "GSTIN", "Optional"),
                ),
            ),
            section(
                "people", "People & Roles", "Who owns the jobs.",
                listOf(
                    memberList("teamMembers", "Team", listOf("You (Owner)")),
                    multiChoice("roles", "Roles", listOf("Owner", "Estimator", "Shop floor", "Accounts")),
                    rowAction("addPeople", "Add Member", "Add"),
                ),
            ),
            section(
                "channels", "Channels & Monthly Plan", "How orders move.",
                listOf(
                    choice("leadTime", "Typical lead time", listOf("Same day", "2–7 days", "Longer")),
                    text("capacityNote", "Capacity note", "Optional"),
                    text("monthlyJobs", "Jobs / month target", "40"),
                ),
            ),
            section(
                "network", "Jobs & Network", "Modules that light up after start.",
                listOf(
                    moduleList(
                        "modules", "Modules",
                        listOf("Quotes", "Job orders", "Materials", "Invoices"),
                    ),
                ),
            ),
            section(
                "gst", "GST & Tax Details", "Shop registration.",
                listOf(
                    toggle("gstRegistered", "Registered for GST?"),
                    choice("gstType", "Registration type", listOf("Regular", "Composition", "Unregistered")),
                    text("gstState", "State", "Maharashtra"),
                ),
            ),
            section(
                "review", "Review your setup", "Confirm before you start.",
                listOf(
                    reviewSummary(
                        "review", "Summary",
                        listOf("shopName", "jobType", "quoteStyle", "leadTime"),
                    ),
                ),
            ),
        ),
    )
}
