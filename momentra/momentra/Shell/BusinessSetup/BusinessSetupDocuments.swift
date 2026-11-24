import Foundation

/// Expandable module row title + optional Figma subtitle.
struct BusinessSetupModuleRow: Equatable, Hashable {
    let title: String
    let subtitle: String

    init(_ title: String, subtitle: String = "") {
        self.title = title
        self.subtitle = subtitle
    }
}

/// Pure chrome helpers for field error highlighting (unit-testable).
enum BusinessSetupFieldChrome {
    static func hasError(_ error: String?) -> Bool {
        guard let error else { return false }
        return !error.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }
}

/// Field / section copy for the nine Figma setup-v6 documents (rknf Obsidian).
/// Called from BusinessSetupShell via `BusinessSetupDocuments.spec(for:)`.
struct BusinessSetupFieldSpec: Identifiable, Equatable {
    enum Kind: Equatable {
        case text
        case choice([String])
        case multiChoice([String])
        case toggle
        case date
        case rowAction(String)
        case memberList([String])
        case moduleList([BusinessSetupModuleRow])
        case reviewSummary([String])
        case panelHeader(String)
    }

    let key: String
    let label: String
    let placeholder: String
    let kind: Kind
    let helper: String
    var id: String { key }

    init(key: String, label: String, placeholder: String, kind: Kind, helper: String = "") {
        self.key = key
        self.label = label
        self.placeholder = placeholder
        self.kind = kind
        self.helper = helper
    }
}

struct BusinessSetupSectionSpec: Identifiable, Equatable {
    let id: String
    let title: String
    let subtitle: String
    let fields: [BusinessSetupFieldSpec]
}

struct BusinessSetupDocumentSpec: Equatable {
    let typeCode: String
    let familyTitle: String
    let eyebrow: String
    let heroTitle: String
    let heroSubtitle: String
    let activateLabel: String
    let defaultTitleKey: String
    let sections: [BusinessSetupSectionSpec]

    var defaultPreferences: [String: String] {
        var prefs: [String: String] = [:]
        for section in sections {
            for field in section.fields {
                switch field.kind {
                case .text, .date, .rowAction:
                    prefs[field.key] = ""
                case .choice(let options):
                    prefs[field.key] = options.first ?? ""
                case .multiChoice(let options):
                    prefs[field.key] = options.first ?? ""
                case .toggle:
                    prefs[field.key] = "Yes"
                case .memberList(let members):
                    prefs[field.key] = members.joined(separator: "|")
                case .moduleList, .reviewSummary, .panelHeader:
                    prefs[field.key] = ""
                }
            }
        }
        return prefs
    }

    /// Ordered section titles for parity tests.
    var sectionTitles: [String] { sections.map(\.title) }
}

enum BusinessSetupDocuments {
    static let CURRENCY_OPTIONS = [
        "INR · Indian Rupee (₹)",
        "USD · US Dollar ($)",
        "EUR · Euro (€)",
        "GBP · British Pound (£)",
        "AED · UAE Dirham (د.إ)",
    ]

    static func spec(for typeCode: String) -> BusinessSetupDocumentSpec {
        switch typeCode.uppercased() {
        case "OWN_BUSINESS_CAMPAIGN": return ownMarketing
        case "TRADING_WHOLESALE": return trading
        case "DISTRIBUTION_DEALERSHIP": return distribution
        case "RETAIL_STORE": return retail
        case "RESTAURANT_FOOD": return restaurant
        case "CONSUMER_SERVICE": return service
        case "STANDARD_PRODUCTION": return manufacturing
        case "MADE_TO_ORDER": return madeToOrder
        default: return clientMarketing
        }
    }

    // MARK: - Marketing (5:33148 / 5:38325)

    /// Figma `5:33148` — five sections (Network/Catalogue omitted by product choice).
    static let clientMarketing = BusinessSetupDocumentSpec(
        typeCode: "CLIENT_CAMPAIGN",
        familyTitle: "Marketing",
        eyebrow: "Client campaign",
        heroTitle: "Brightline Setup",
        heroSubtitle: "Bring your business, people, and monthly plan together before you start.",
        activateLabel: "Start this business",
        defaultTitleKey: "campaignName",
        sections: [
            section("basics", "Business Basics", "A home for this client campaign.", [
                panelHeader("panelCampaign", "Campaign"),
                text("campaignName", "Campaign name", "Brightline Creative for Northstar Residences"),
                text("companyName", "Company", "Brightline Creative", helper: "Agency / firm name"),
                text("gstin", "GSTIN", "27AAKCM4321D1Z2", helper: "15-digit GSTIN, if you have one"),
                choice("currency", "Currency", CURRENCY_OPTIONS),
                date("startDate", "Start date", "12 October 2026"),
                date("endDate", "End date", "Still open"),
                panelHeader("panelClient", "Client"),
                text("clientName", "Client", "Northstar Residences"),
                text("clientContact", "Client contact (optional)", "Optional"),
                panelHeader("panelPurpose", "Purpose & structure"),
                choice(
                    "campaignPurpose",
                    "Campaign purpose",
                    ["Leads", "Launch", "Awareness", "Sales", "Event", "Traffic"]
                ),
                text("promotedBrand", "Promoted brand", "Optional"),
            ]),
            section(
                "people",
                "People & Roles",
                "Invite teammates and pick a role on Invite & QR. No separate Add person.",
                [
                    memberList("teamMembers", "Team", ["You (Owner)"]),
                    multiChoice(
                        "roles",
                        "Roles on invite",
                        ["Owner", "Admin", "Campaign Lead", "Finance", "Approver"],
                        helper: "Default Campaign Lead · owns delivery & reporting. Role applies when they join."
                    ),
                ]
            ),
            section("channels", "Channels & Monthly Plan", "Channels, client commercials, budget and targets.", [
                panelHeader("panelChannels", "Channels"),
                multiChoice(
                    "channels",
                    "Where do you run it?",
                    ["Paid social", "Search", "Print", "Influencer", "Events"],
                    helper: "These are the places you run the campaign. They are not a different kind of business."
                ),
                panelHeader("panelCommercials", "Client commercials"),
                text("clientFee", "Client fee", "₹1,50,000", helper: "Amount"),
                choice("billingBasis", "Billing basis", ["Retainer", "Deliverable", "Pass-through"]),
                panelHeader("panelBudget", "Budget"),
                date("planMonth", "Month", "October 2026"),
                text("monthlyBudget", "Monthly budget", "₹2,00,000", helper: "Amount"),
                text("campaignBudget", "Campaign budget", "₹6,00,000", helper: "Optional · aliases into monthly if empty"),
                text("clientApprovedMediaBudget", "Client-approved media", "₹4,00,000", helper: "Optional"),
                text("agencyOperatingBudget", "Agency operating", "₹50,000", helper: "Optional"),
                panelHeader("panelTargets", "Targets"),
                text("monthlyTarget", "Primary target", "120", helper: "Amount / count"),
                choice("targetMetric", "Target unit", ["Leads", "Reach", "Clicks", "Sales", "Client billing", "Custom"]),
            ]),
            section("gst", "GST & Tax Details", "Set up GST once to make buying and selling easier.", [
                panelHeader("panelTracking", "Tracking"),
                choice(
                    "gstTracking",
                    "Do you want to track GST in this Moment?",
                    ["Yes", "Not now"],
                    helper: "Skipping means tax tracking is disabled in Momentra. It does not mean the business is legally GST-exempt."
                ),
                panelHeader("panelRegistration", "Registration"),
                choice(
                    "gstRegistration",
                    "How is your business registered for GST?",
                    ["Regular", "Composition", "Unregistered", "Not sure"]
                ),
                text(
                    "gstState",
                    "Which state is your business registered in?",
                    "Maharashtra",
                    helper: "State"
                ),
                panelHeader("panelPricing", "Pricing"),
                choice(
                    "salePriceIncludesGst",
                    "Do prices include GST?",
                    ["Prices include GST", "GST is added separately"],
                    helper: "Your GSTIN stays the one entered above. It is stored as a reference only."
                ),
                date("gstEffectiveFrom", "Effective from (optional)", "YYYY-MM-DD"),
            ]),
            section("review", "Review your setup", "Confirm before you start.", [
                reviewSummary("review", "Summary", [
                    "clientName",
                    "gstin",
                    "channels",
                    "monthlyBudget",
                    "clientFee",
                    "monthlyTarget",
                    "targetMetric",
                ]),
            ]),
        ]
    )

    /// Figma `5:38325` MKT-OWN-SETUP-v6 — Network section omitted (five sections).
    static let ownMarketing = BusinessSetupDocumentSpec(
        typeCode: "OWN_BUSINESS_CAMPAIGN",
        familyTitle: "Marketing",
        eyebrow: "Own Business Campaign",
        heroTitle: "Harbor & Loom Setup",
        heroSubtitle: "Bring your business, people, and monthly plan together before you start.",
        activateLabel: "Start this business",
        defaultTitleKey: "campaignName",
        sections: [
            section("basics", "Business Basics", "Name the campaign and what it promotes.", [
                panelHeader("panelIdentity", "Identity"),
                text("campaignName", "Campaign name", "Harbor & Loom launch"),
                text("brandName", "Promoted brand", "Harbor & Loom", helper: "Your company — not a client"),
                text("gstin", "GSTIN", "29ABCDE4321T1Z5", helper: "15-digit GSTIN, if you have one"),
                choice("currency", "Currency", CURRENCY_OPTIONS),
                date("startDate", "Start date", "12 October 2026"),
                date("endDate", "End date", "Still open"),
                panelHeader("panelPurpose", "Purpose & structure"),
                multiChoice(
                    "campaignPurpose",
                    "Campaign purpose",
                    ["Awareness", "Leads", "Sales", "Traffic", "Retention"]
                ),
                choice("offerType", "Structure", ["One-time", "Ongoing"]),
                text("promotedProducts", "Promoted products", "Seasonal collection, hero SKU"),
                panelHeader("panelMore", "More"),
                text("description", "Description", "Everyday notes about this campaign"),
                text("location", "Location", "Add a region or outlet"),
            ]),
            section(
                "people",
                "People & Roles",
                "Invite teammates and pick a role on Invite & QR. No separate Add person.",
                [
                    memberList("teamMembers", "Team", ["You (Owner)"]),
                    multiChoice(
                        "roles",
                        "Roles on invite",
                        ["Owner", "Admin", "Campaign Lead", "Finance", "Approver"],
                        helper: "Default Campaign Lead · plan, launch, measure. Role applies when they join."
                    ),
                ]
            ),
            section("channels", "Channels & Monthly Plan", "Where you run it, budget, and targets.", [
                panelHeader("panelChannels", "Channels"),
                multiChoice(
                    "channels",
                    "Channels",
                    ["Paid social", "Search", "Print", "Influencer", "Events", "Store window"],
                    helper: "These are where you run creative — not separate businesses."
                ),
                text(
                    "marketingBudget",
                    "Own marketing budget",
                    "₹2,00,000",
                    helper: "Overall envelope for this campaign"
                ),
                date("planMonth", "Month", "October 2026"),
                text("monthlyBudget", "Monthly budget", "₹50,000", helper: "Amount"),
                text("conversionGoal", "Primary target", "Store visits / sales", helper: "What success looks like"),
                choice("targetMetric", "Target unit", [
                    "Leads", "Reach", "Clicks", "Attributed sales", "Engagements", "Custom",
                ]),
                text("supportingTarget", "Supporting target", "Optional secondary metric"),
            ]),
            section("gst", "GST & Tax Details", "Set up GST once to make buying and selling easier.", [
                panelHeader("panelTracking", "Tracking"),
                choice(
                    "gstTracking",
                    "Do you want to track GST in this Moment?",
                    ["Yes", "Not now"],
                    helper: "Skipping means tax tracking is disabled in Momentra. It does not mean the business is legally GST-exempt."
                ),
                panelHeader("panelRegistration", "Registration"),
                choice(
                    "gstRegistration",
                    "How is your business registered for GST?",
                    ["Regular", "Composition", "Unregistered", "Not sure"]
                ),
                text(
                    "gstState",
                    "Which state is your business registered in?",
                    "Maharashtra",
                    helper: "State"
                ),
                panelHeader("panelPricing", "Pricing"),
                choice(
                    "salePriceIncludesGst",
                    "Do prices include GST?",
                    ["Prices include GST", "GST is added separately"],
                    helper: "Your GSTIN stays the one entered above. It is stored as a reference only."
                ),
                date("gstEffectiveFrom", "Effective from (optional)", "YYYY-MM-DD"),
            ]),
            section("review", "Review your setup", "Confirm before you start.", [
                reviewSummary("review", "Summary", [
                    "campaignName",
                    "brandName",
                    "gstin",
                    "channels",
                    "monthlyBudget",
                    "conversionGoal",
                    "targetMetric",
                ]),
            ]),
        ]
    )

    // MARK: - Trading (5:18833 / 5:8637)

    /// Figma `5:18833` TRD-SETUP-v6 — Catalogue/Network section omitted (five sections).
    static let trading = BusinessSetupDocumentSpec(
        typeCode: "TRADING_WHOLESALE",
        familyTitle: "Trading",
        eyebrow: "Trading",
        heroTitle: "Northstar Trading Setup",
        heroSubtitle: "Bring your business, people, and monthly plan together before you start.",
        activateLabel: "Start this business",
        defaultTitleKey: "businessName",
        sections: [
            section("basics", "Business Basics", "A home for this trading business.", [
                panelHeader("panelIdentity", "Identity"),
                text("businessName", "Business name", "Northstar Trading"),
                text("companyName", "Company name", "Northstar Trading", helper: "Registered company name"),
                text("gstin", "GSTIN", "29ABCDE4321T1Z5", helper: "15-digit GSTIN, if you have one"),
                choice("currency", "Currency", CURRENCY_OPTIONS),
                date("startDate", "Start date", "12 October 2026"),
                date("endDate", "End date", "Still open"),
                panelHeader("panelDates", "Dates & more"),
                text("description", "Description", "Everyday notes about this business"),
                text("location", "Location", "Add your DC location"),
            ]),
            section(
                "people",
                "People & Roles",
                "Invite teammates and pick a role on Invite & QR. No separate Add person.",
                [
                    memberList("teamMembers", "Team", ["You (Owner)"]),
                    multiChoice(
                        "roles",
                        "Roles on invite",
                        ["Owner", "Manager", "Sales exec", "Warehouse", "Sales rep"],
                        helper: "Default Sales rep · own beat orders & collections. Role applies when they join."
                    ),
                ]
            ),
            section("channels", "Channels & Monthly Plan", "Where do you sell? Choose at least one.", [
                panelHeader("panelChannels", "Channels"),
                multiChoice(
                    "channels",
                    "Channels",
                    ["Retail buyers", "Wholesale", "Modern trade", "HoReCa", "Online"],
                    helper: "These are the places you sell or serve. They are not a different kind of business."
                ),
                date("planMonth", "Month", "October 2026"),
                text("monthlyBudget", "Monthly budget", "₹60,00,000", helper: "Amount"),
                text("monthlyTarget", "Monthly sales target", "₹90,00,000", helper: "Amount"),
                choice("targetMetric", "How the target is measured", ["Revenue", "Orders", "Count"]),
            ]),
            section("gst", "GST & Tax Details", "Set up GST once to make buying and selling easier.", [
                panelHeader("panelTracking", "Tracking"),
                choice(
                    "gstTracking",
                    "Do you want to track GST in this Moment?",
                    ["Yes", "Not now"],
                    helper: "Skipping means tax tracking is disabled in Momentra. It does not mean the business is legally GST-exempt."
                ),
                panelHeader("panelRegistration", "Registration"),
                choice(
                    "gstRegistration",
                    "How is your business registered for GST?",
                    ["Regular", "Composition", "Unregistered", "Not sure"]
                ),
                text(
                    "gstState",
                    "Which state is your business registered in?",
                    "Maharashtra",
                    helper: "State"
                ),
                panelHeader("panelPricing", "Pricing"),
                choice(
                    "salePriceIncludesGst",
                    "Do prices include GST?",
                    ["Prices include GST", "GST is added separately"],
                    helper: "Your GSTIN stays the one entered above. It is stored as a reference only."
                ),
                date("gstEffectiveFrom", "Effective from (optional)", "YYYY-MM-DD"),
            ]),
            section("review", "Review your setup", "Confirm before you start.", [
                reviewSummary("review", "Summary", [
                    "companyName",
                    "gstin",
                    "channels",
                    "monthlyBudget",
                    "monthlyTarget",
                    "targetMetric",
                ]),
            ]),
        ]
    )

    /// Figma `5:8637` — five sections (Catalogue/Network omitted by product choice).
    static let distribution = BusinessSetupDocumentSpec(
        typeCode: "DISTRIBUTION_DEALERSHIP",
        familyTitle: "Trading",
        eyebrow: "Distribution",
        heroTitle: "Metro Distribution Setup",
        heroSubtitle: "Bring your business, people, and monthly plan together before you start.",
        activateLabel: "Start this business",
        defaultTitleKey: "businessName",
        sections: [
            section("basics", "Business Basics", "A home for this distribution business.", [
                panelHeader("panelIdentity", "Identity"),
                text("businessName", "Business name", "Metro Distribution"),
                text("companyName", "Company name", "Metro Distribution", helper: "Registered company name"),
                text("gstin", "GSTIN", "27AAKCM4321D1Z2", helper: "15-digit GSTIN, if you have one"),
                choice("currency", "Currency", CURRENCY_OPTIONS),
                date("startDate", "Start date", "12 October 2026"),
                date("endDate", "End date", "Still open"),
                panelHeader("panelDates", "Dates & more"),
                text("description", "Description", "Everyday notes about this business"),
                text("location", "Location", "Add your DC location"),
            ]),
            section(
                "people",
                "People & Roles",
                "Invite teammates and pick a role on Invite & QR. No separate Add person.",
                [
                    memberList("teamMembers", "Team", ["You (Owner)"]),
                    multiChoice(
                        "roles",
                        "Roles on invite",
                        ["Owner", "Manager", "Route lead", "Warehouse", "Salesman"],
                        helper: "Default Salesman · own beat orders & collections. Role applies when they join."
                    ),
                ]
            ),
            section("channels", "Channels & Monthly Plan", "Where do you sell? Choose at least one.", [
                panelHeader("panelChannels", "Channels"),
                multiChoice(
                    "channels",
                    "Channels",
                    ["Retail dealers", "Wholesale", "Modern trade", "HoReCa", "Van sales"],
                    helper: "These are the places you sell or serve. They are not a different kind of business."
                ),
                date("planMonth", "Month", "October 2026"),
                text("monthlyBudget", "Monthly budget", "₹60,00,000", helper: "Amount"),
                text("monthlyTarget", "Monthly sales target", "₹90,00,000", helper: "Amount"),
                choice("targetMetric", "How the target is measured", ["Revenue", "Orders", "Count"]),
            ]),
            section("gst", "GST & Tax Details", "Set up GST once to make buying and selling easier.", [
                panelHeader("panelTracking", "Tracking"),
                choice(
                    "gstTracking",
                    "Do you want to track GST in this Moment?",
                    ["Yes", "Not now"],
                    helper: "Skipping means tax tracking is disabled in Momentra. It does not mean the business is legally GST-exempt."
                ),
                panelHeader("panelRegistration", "Registration"),
                choice(
                    "gstRegistration",
                    "How is your business registered for GST?",
                    ["Regular", "Composition", "Unregistered", "Not sure"]
                ),
                text(
                    "gstState",
                    "Which state is your business registered in?",
                    "Maharashtra",
                    helper: "State"
                ),
                panelHeader("panelPricing", "Pricing"),
                choice(
                    "salePriceIncludesGst",
                    "Do prices include GST?",
                    ["Prices include GST", "GST is added separately"],
                    helper: "Your GSTIN stays the one entered above. It is stored as a reference only."
                ),
                date("gstEffectiveFrom", "Effective from (optional)", "YYYY-MM-DD"),
            ]),
            section("review", "Review your setup", "Confirm before you start.", [
                reviewSummary("review", "Summary", [
                    "companyName",
                    "gstin",
                    "channels",
                    "monthlyBudget",
                    "monthlyTarget",
                    "targetMetric",
                ]),
            ]),
        ]
    )

    // MARK: - Retail (5:80797 / 5:90612 / 5:71162)

    /// Figma `5:80797` — five sections (Catalogue/Network omitted by product choice).
    static let retail = BusinessSetupDocumentSpec(
        typeCode: "RETAIL_STORE",
        familyTitle: "Retail",
        eyebrow: "Retail store",
        heroTitle: "Harbor Mart Setup",
        heroSubtitle: "Bring your business, people, and monthly plan together before you start.",
        activateLabel: "Start this store",
        defaultTitleKey: "storeName",
        sections: [
            section("basics", "Business Basics", "A home for this store.", [
                panelHeader("panelIdentity", "Identity"),
                text("storeName", "Store name", "Harbor Mart"),
                text("companyName", "Company name", "Harbor Mart", helper: "Registered company name"),
                text("gstin", "GSTIN", "27AAHCH4321M1Z3", helper: "15-digit GSTIN, if you have one"),
                choice("currency", "Currency", CURRENCY_OPTIONS),
                date("startDate", "Start date", "12 October 2026"),
                date("endDate", "End date", "Still open"),
                panelHeader("panelDates", "Dates & more"),
                text("description", "Description", "Everyday notes about this store"),
                text("location", "Location", "Add your store location"),
            ]),
            section(
                "people",
                "People & Roles",
                "Invite teammates and pick a role on Invite & QR. No separate Add person.",
                [
                    memberList("teamMembers", "Team", ["You (Owner)"]),
                    multiChoice(
                        "roles",
                        "Roles on invite",
                        ["Owner", "Manager", "Cashier", "Stock", "Floor"],
                        helper: "Default Cashier · POS, bills & returns. Role applies when they join."
                    ),
                ]
            ),
            section("channels", "Channels & Monthly Plan", "Where do you sell? Choose at least one.", [
                panelHeader("panelChannels", "Channels"),
                multiChoice(
                    "channels",
                    "Channels",
                    ["Counter", "Home", "Online", "B2B", "Market"],
                    helper: "These are the places you sell or serve. They are not a different kind of business."
                ),
                date("planMonth", "Month", "October 2026"),
                text("monthlyBudget", "Monthly budget", "₹8,40,000", helper: "Amount"),
                text("monthlyTarget", "Monthly sales target", "₹12,00,000", helper: "Amount"),
                choice("targetMetric", "How the target is measured", ["Revenue", "Orders", "Count"]),
            ]),
            section("gst", "GST & Tax Details", "Set up GST once to make buying and selling easier.", [
                panelHeader("panelTracking", "Tracking"),
                choice(
                    "gstTracking",
                    "Do you want to track GST in this Moment?",
                    ["Yes", "Not now"],
                    helper: "Skipping means tax tracking is disabled in Momentra. It does not mean the business is legally GST-exempt."
                ),
                panelHeader("panelRegistration", "Registration"),
                choice(
                    "gstRegistration",
                    "How is your business registered for GST?",
                    ["Regular", "Composition", "Unregistered", "Not sure"]
                ),
                text(
                    "gstState",
                    "Which state is your business registered in?",
                    "Maharashtra",
                    helper: "State"
                ),
                panelHeader("panelPricing", "Pricing"),
                choice(
                    "salePriceIncludesGst",
                    "Do prices include GST?",
                    ["Prices include GST", "GST is added separately"],
                    helper: "Your GSTIN stays the one entered above. It is stored as a reference only."
                ),
                date("gstEffectiveFrom", "Effective from (optional)", "YYYY-MM-DD"),
            ]),
            section("review", "Review your setup", "Confirm before you start.", [
                reviewSummary("review", "Summary", [
                    "companyName",
                    "gstin",
                    "channels",
                    "monthlyBudget",
                    "monthlyTarget",
                    "targetMetric",
                ]),
            ]),
        ]
    )

    /// Figma `5:90612` RST-SETUP-v6 — §04 modules are display-only (no masters sheets).
    static let restaurant = BusinessSetupDocumentSpec(
        typeCode: "RESTAURANT_FOOD",
        familyTitle: "Retail",
        eyebrow: "Restaurant",
        heroTitle: "Spice Route Setup",
        heroSubtitle: "Bring your business, people, and monthly plan together before you start.",
        activateLabel: "Start this restaurant",
        defaultTitleKey: "outletName",
        sections: [
            section("basics", "Business Basics", "A home for this restaurant.", [
                panelHeader("panelIdentity", "Identity"),
                text("outletName", "Restaurant name", "Spice Route Kitchen"),
                text("companyName", "Company name", "Spice Route Kitchen", helper: "Registered company name"),
                text("gstin", "GSTIN", "27AASCS4821K1Z3", helper: "15-digit GSTIN, if you have one"),
                choice("currency", "Currency", CURRENCY_OPTIONS),
                date("startDate", "Start date", "12 October 2026"),
                date("endDate", "End date", "Still open"),
                panelHeader("panelDates", "Dates & more"),
                text("description", "Description", "Everyday notes about this restaurant"),
                text("location", "Location", "Add your restaurant location"),
            ]),
            section(
                "people",
                "People & Roles",
                "Invite teammates and pick a role on Invite & QR. No separate Add person.",
                [
                    memberList("teamMembers", "Team", ["You (Owner)"]),
                    multiChoice(
                        "roles",
                        "Roles on invite",
                        ["Owner", "Manager", "Cashier", "Kitchen", "Waiter"],
                        helper: "Default Cashier · POS, bills & KOT view. Role applies when they join."
                    ),
                ]
            ),
            section("channels", "Channels & Monthly Plan", "Where do you sell? Choose at least one.", [
                panelHeader("panelChannels", "Channels"),
                multiChoice(
                    "channels",
                    "Channels",
                    ["Dine-in", "Direct", "Zomato", "Takeaway", "Swiggy"],
                    helper: "These are the places you sell or serve. They are not a different kind of business."
                ),
                date("planMonth", "Month", "October 2026"),
                text("monthlyBudget", "Monthly budget", "₹5,20,000", helper: "Amount"),
                text("monthlyTarget", "Monthly sales target", "₹6,50,000", helper: "Amount"),
                choice("targetMetric", "How the target is measured", ["Revenue", "Orders", "Count"]),
            ]),
            section(
                "network",
                "Kitchen, Menu & Network",
                "Modules your POS, KOT and purchases will use after start.",
                [
                    moduleListRows("modules", "Modules", [
                        BusinessSetupModuleRow("Menu", subtitle: "42 dishes · GST 5% restaurant"),
                        BusinessSetupModuleRow("Stations", subtitle: "Tandoor · Hot · Cold · Bar · Floor"),
                        BusinessSetupModuleRow("Vendors", subtitle: "3 suppliers · GSTIN on file"),
                        BusinessSetupModuleRow("Team invite & QR", subtitle: "Owner · Manager · Cashier · Kitchen · Waiter"),
                    ]),
                ]
            ),
            section("gst", "GST & Tax Details", "Set up GST once to make buying and selling easier.", [
                panelHeader("panelTracking", "Tracking"),
                choice(
                    "gstTracking",
                    "Do you want to track GST in this Moment?",
                    ["Yes", "Not now"],
                    helper: "Skipping means tax tracking is disabled in Momentra. It does not mean the business is legally GST-exempt."
                ),
                panelHeader("panelRegistration", "Registration"),
                choice(
                    "gstRegistration",
                    "How is your business registered for GST?",
                    ["Regular", "Composition", "Unregistered", "Not sure"]
                ),
                text(
                    "gstState",
                    "Which state is your business registered in?",
                    "Maharashtra",
                    helper: "State"
                ),
                panelHeader("panelPricing", "Pricing"),
                choice(
                    "salePriceIncludesGst",
                    "Do prices include GST?",
                    ["Prices include GST", "GST is added separately"],
                    helper: "Your GSTIN stays the one entered above. It is stored as a reference only."
                ),
                date("gstEffectiveFrom", "Effective from (optional)", "YYYY-MM-DD"),
            ]),
            section("review", "Review your setup", "Confirm before you start.", [
                reviewSummary("review", "Summary", [
                    "companyName",
                    "gstin",
                    "channels",
                    "monthlyBudget",
                    "monthlyTarget",
                    "targetMetric",
                ]),
            ]),
        ]
    )

    static let service = BusinessSetupDocumentSpec(
        typeCode: "CONSUMER_SERVICE",
        familyTitle: "Retail",
        eyebrow: "Services",
        heroTitle: "BrightCare Setup",
        heroSubtitle: "Bring your business, people, and monthly plan together before you start.",
        activateLabel: "Start this business",
        defaultTitleKey: "serviceName",
        sections: [
            section("basics", "Business Basics", "A home for this salon & spa.", [
                panelHeader("panelIdentity", "Identity"),
                text("serviceName", "Business name", "BrightCare"),
                text("companyName", "Company name", "BrightCare", helper: "Registered company name"),
                text("gstin", "GSTIN", "27AAHCB4321M1Z8", helper: "15-digit GSTIN, if you have one"),
                choice("currency", "Currency", CURRENCY_OPTIONS),
                date("startDate", "Start date", "12 October 2026"),
                date("endDate", "End date", "Still open"),
                panelHeader("panelDates", "Dates & more"),
                text("description", "Description", "Everyday notes about this business"),
                text("location", "Location", "Add your studio location"),
            ]),
            section(
                "people",
                "People & Roles",
                "Invite teammates and pick a role on Invite & QR. No separate Add person.",
                [
                    memberList("teamMembers", "Team", ["You (Owner)"]),
                    multiChoice(
                        "roles",
                        "Roles on invite",
                        ["Owner", "Manager", "Stylist", "Front desk", "Therapist"],
                        helper: "Default Stylist · own calendar, bookings & charge view. Role applies when they join."
                    ),
                ]
            ),
            section("channels", "Channels & Monthly Plan", "Where do you serve? Choose at least one.", [
                panelHeader("panelChannels", "Channels"),
                multiChoice(
                    "channels",
                    "Channels",
                    ["Studio", "Home", "Online", "Corporate", "Events"],
                    helper: "These are the places you sell or serve. They are not a different kind of business."
                ),
                date("planMonth", "Month", "October 2026"),
                text("monthlyBudget", "Monthly budget", "₹3,20,000", helper: "Amount"),
                text("monthlyChargeTarget", "Monthly charge target", "₹6,50,000", helper: "Amount"),
                choice("targetMetric", "How the target is measured", ["Revenue", "Appointments", "Count"]),
            ]),
            section("gst", "GST & Tax Details", "Set up GST once to make buying and selling easier.", [
                panelHeader("panelTracking", "Tracking"),
                choice(
                    "gstTracking",
                    "Do you want to track GST in this Moment?",
                    ["Yes", "Not now"],
                    helper: "Skipping means tax tracking is disabled in Momentra. It does not mean the business is legally GST-exempt."
                ),
                panelHeader("panelRegistration", "Registration"),
                choice(
                    "gstRegistration",
                    "How is your business registered for GST?",
                    ["Regular", "Composition", "Unregistered", "Not sure"]
                ),
                text(
                    "gstState",
                    "Which state is your business registered in?",
                    "Maharashtra",
                    helper: "State"
                ),
                panelHeader("panelPricing", "Pricing"),
                choice(
                    "salePriceIncludesGst",
                    "Do prices include GST?",
                    ["Prices include GST", "GST is added separately"],
                    helper: "Your GSTIN stays the one entered above. It is stored as a reference only."
                ),
                date("gstEffectiveFrom", "Effective from (optional)", "YYYY-MM-DD"),
            ]),
            section("review", "Review your setup", "Confirm before you start.", [
                reviewSummary("review", "Summary", [
                    "companyName",
                    "gstin",
                    "channels",
                    "monthlyBudget",
                    "monthlyChargeTarget",
                    "targetMetric",
                ]),
            ]),
        ]
    )

    // MARK: - Manufacturing (5:50728 / 5:55863)
    // Figma parity without §04 Products/BOM/Network (same omit pattern as Trading).

    /// Figma `5:50728` — Products/BOM/Network omitted.
    static let manufacturing = BusinessSetupDocumentSpec(
        typeCode: "STANDARD_PRODUCTION",
        familyTitle: "Manufacturing",
        eyebrow: "Manufacturing",
        heroTitle: "Forge & Form Setup",
        heroSubtitle: "Bring your business, people, and monthly plan together before you start.",
        activateLabel: "Start this business",
        defaultTitleKey: "businessName",
        sections: [
            section("basics", "Business Basics", "A home for this plant.", [
                panelHeader("panelIdentity", "Identity"),
                text("businessName", "Business name", "Forge & Form"),
                text("companyName", "Company name", "Forge & Form", helper: "Registered company name"),
                text("gstin", "GSTIN", "27AAFCF4321M1Z6", helper: "15-digit GSTIN, if you have one"),
                choice("currency", "Currency", CURRENCY_OPTIONS),
                date("startDate", "Start date", "12 October 2026"),
                date("endDate", "End date", "Still open"),
                panelHeader("panelDates", "Dates & more"),
                text("description", "Description", "Everyday notes about this plant"),
                text("location", "Location", "Add your plant location"),
            ]),
            section(
                "people",
                "People & Roles",
                "Invite teammates and pick a role on Invite & QR. No separate Add person.",
                [
                    memberList("teamMembers", "Team", ["You (Owner)"]),
                    multiChoice(
                        "roles",
                        "Roles on invite",
                        ["Owner", "Manager", "Supervisor", "Storekeeper", "Operator"],
                        helper: "Default Operator · own work orders & production entry. Role applies when they join."
                    ),
                ]
            ),
            section("channels", "Channels & Monthly Plan", "Where do you sell or make? Choose at least one.", [
                panelHeader("panelChannels", "Channels"),
                multiChoice(
                    "channels",
                    "Channels",
                    ["Direct B2B", "Dealers", "OEM", "Export", "Job work"],
                    helper: "These are the places you sell or serve. They are not a different kind of business."
                ),
                date("planMonth", "Month", "October 2026"),
                text("monthlyBudget", "Monthly budget", "₹42,00,000", helper: "Amount"),
                text("monthlyTarget", "Monthly sales target", "₹65,00,000", helper: "Amount"),
                choice("targetMetric", "How the target is measured", [
                    "Invoiced value (or accepted job units)",
                    "Production output",
                    "Delivery count",
                    "Orders",
                ]),
            ]),
            section("gst", "GST & Tax Details", "Set up GST once to make buying and selling easier.", [
                panelHeader("panelTracking", "Tracking"),
                choice(
                    "gstTracking",
                    "Do you want to track GST in this Moment?",
                    ["Yes", "Not now"],
                    helper: "Skipping means tax tracking is disabled in Momentra. It does not mean the business is legally GST-exempt."
                ),
                panelHeader("panelRegistration", "Registration"),
                choice(
                    "gstRegistration",
                    "How is your business registered for GST?",
                    ["Regular", "Composition", "Unregistered", "Not sure"]
                ),
                text(
                    "gstState",
                    "Which state is your business registered in?",
                    "Maharashtra",
                    helper: "State"
                ),
                panelHeader("panelPricing", "Pricing"),
                choice(
                    "salePriceIncludesGst",
                    "Do prices include GST?",
                    ["Prices include GST", "GST is added separately"],
                    helper: "Your GSTIN stays the one entered above. It is stored as a reference only."
                ),
                date("gstEffectiveFrom", "Effective from (optional)", "YYYY-MM-DD"),
            ]),
            section("review", "Review your setup", "Confirm before you start.", [
                reviewSummary("review", "Summary", [
                    "businessName",
                    "companyName",
                    "gstin",
                    "channels",
                    "monthlyBudget",
                    "monthlyTarget",
                    "targetMetric",
                ]),
            ]),
        ]
    )

    /// Figma `5:55863` — Quotes/Jobs/Network omitted. No Plan · Monthly section.
    static let madeToOrder = BusinessSetupDocumentSpec(
        typeCode: "MADE_TO_ORDER",
        familyTitle: "Manufacturing",
        eyebrow: "Job order",
        heroTitle: "Forge & Form Setup",
        heroSubtitle: "Bring your business, people, and monthly plan together before you start.",
        activateLabel: "Activate Moment",
        defaultTitleKey: "businessName",
        sections: [
            section("basics", "Business Basics", "A home for this job-order shop.", [
                text("businessName", "Business name", "Forge & Form"),
                text("companyName", "Company name", "Forge & Form", helper: "Registered company name"),
                text("gstin", "GSTIN", "27AAFCF4321M1Z6", helper: "15-digit GSTIN, if you have one"),
                choice("currency", "Currency", CURRENCY_OPTIONS),
                date("startDate", "Start date", "12 October 2026"),
                date("endDate", "End date", "Still open"),
                panelHeader("panelDates", "Dates & more"),
                text("description", "Description", "Everyday notes about this plant"),
                multiChoice("channels", "Channels", [
                    "Direct B2B", "Dealers", "OEM", "Export", "Job work",
                ], helper: "Channels are where you sell. They never change the Job order mode."),
                text("monthlyBudget", "Monthly budget", "₹9,00,000", helper: "Amount"),
                text("monthlyTarget", "Monthly target", "₹18,00,000", helper: "Amount"),
                choice("targetMetric", "How the target is measured", [
                    "Invoiced value (or accepted job units)",
                    "Production output",
                    "Delivery count",
                    "Orders",
                ], helper: "Posted invoices count toward the target. Quotes, confirmed orders and advances never do."),
            ]),
            section("people", "People & Roles", "Invite teammates and pick a role on Invite & QR. No separate Add person.", [
                memberList("teamMembers", "Team", ["You (Owner)"]),
                multiChoice("roles", "Roles on invite", [
                    "Owner", "Manager", "Supervisor", "Storekeeper", "Operator",
                ], helper: "Default Operator · own work orders & production entry. Role applies when they join."),
                rowAction("addPeople", "+ Invite & QR · pick role", "Invite"),
            ]),
            section("quotes", "Job order · Quotes & order approval", "Job order only. The customer-order picker activates when a quote is accepted.", [
                choice("quoteValidity", "Quote validity", [
                    "15 days · expired quotes revise, never invoice",
                    "30 days · expired quotes revise, never invoice",
                    "7 days · expired quotes revise, never invoice",
                ]),
                choice("specificationTemplate", "+ Specification template", [
                    "Drawing · material · tolerance · finish",
                    "Create / upload / link from library",
                ]),
                choice("orderApproval", "Order approval", [
                    "Discount >5% or value >₹5L → Owner approves",
                    "All confirmed orders → Owner approves",
                    "No approval required",
                ]),
                choice("jobOnConfirm", "Job per confirmed order", [
                    "Accepted order opens a job with target qty",
                    "Open job manually after confirm",
                ]),
                choice("productsBomNote", "Products & BOM (optional)", [
                    "Fully detailed · BOM with cost and lead time",
                    "Skip for now · never forced",
                ]),
            ]),
            section("gst", "GST & Tax Details", "Set up GST once to make buying and selling easier.", [
                panelHeader("panelTracking", "Tracking"),
                choice("gstRegistered", "Do you want to track GST in this Moment?", ["Yes", "Not now"],
                       helper: "Skipping means tax tracking is disabled in Momentra. It does not mean the business is legally GST-exempt."),
                panelHeader("panelRegistration", "Registration"),
                choice("gstType", "How is your business registered for GST?", [
                    "Regular", "Composition", "Unregistered", "Not sure",
                ]),
                text("gstState", "Which state is your business registered in?", "Maharashtra", helper: "State"),
                panelHeader("panelPricing", "Pricing"),
                choice("priceIncludesGst", "Do prices include GST?", [
                    "Prices include GST", "GST is added separately",
                ], helper: "Your GSTIN stays the one entered above. It is stored as a reference only."),
                date("gstEffectiveFrom", "Effective from (optional)", "YYYY-MM-DD"),
            ]),
            section("review", "Activate", "Confirm before you start.", [
                reviewSummary("review", "Summary", [
                    "businessName", "companyName", "gstin", "monthlyBudget", "monthlyTarget",
                    "targetMetric", "quoteValidity",
                ]),
            ]),
        ]
    )

    // MARK: - Helpers

    private static func section(
        _ id: String,
        _ title: String,
        _ subtitle: String,
        _ fields: [BusinessSetupFieldSpec]
    ) -> BusinessSetupSectionSpec {
        BusinessSetupSectionSpec(id: id, title: title, subtitle: subtitle, fields: fields)
    }

    private static func text(
        _ key: String,
        _ label: String,
        _ placeholder: String,
        helper: String = ""
    ) -> BusinessSetupFieldSpec {
        BusinessSetupFieldSpec(key: key, label: label, placeholder: placeholder, kind: .text, helper: helper)
    }

    private static func choice(
        _ key: String,
        _ label: String,
        _ options: [String],
        helper: String = ""
    ) -> BusinessSetupFieldSpec {
        BusinessSetupFieldSpec(key: key, label: label, placeholder: "", kind: .choice(options), helper: helper)
    }

    private static func multiChoice(
        _ key: String,
        _ label: String,
        _ options: [String],
        helper: String = ""
    ) -> BusinessSetupFieldSpec {
        BusinessSetupFieldSpec(key: key, label: label, placeholder: "", kind: .multiChoice(options), helper: helper)
    }

    private static func toggle(_ key: String, _ label: String, helper: String = "") -> BusinessSetupFieldSpec {
        BusinessSetupFieldSpec(key: key, label: label, placeholder: "", kind: .toggle, helper: helper)
    }

    private static func date(
        _ key: String,
        _ label: String,
        _ placeholder: String,
        helper: String = ""
    ) -> BusinessSetupFieldSpec {
        BusinessSetupFieldSpec(key: key, label: label, placeholder: placeholder, kind: .date, helper: helper)
    }

    private static func rowAction(_ key: String, _ label: String, _ action: String) -> BusinessSetupFieldSpec {
        BusinessSetupFieldSpec(key: key, label: label, placeholder: action, kind: .rowAction(action))
    }

    private static func memberList(_ key: String, _ label: String, _ members: [String]) -> BusinessSetupFieldSpec {
        BusinessSetupFieldSpec(key: key, label: label, placeholder: "", kind: .memberList(members))
    }

    private static func moduleList(_ key: String, _ label: String, _ modules: [String]) -> BusinessSetupFieldSpec {
        BusinessSetupFieldSpec(
            key: key,
            label: label,
            placeholder: "",
            kind: .moduleList(modules.map { BusinessSetupModuleRow($0) })
        )
    }

    private static func moduleListRows(
        _ key: String,
        _ label: String,
        _ modules: [BusinessSetupModuleRow],
        helper: String = ""
    ) -> BusinessSetupFieldSpec {
        BusinessSetupFieldSpec(key: key, label: label, placeholder: "", kind: .moduleList(modules), helper: helper)
    }

    private static func panelHeader(_ key: String, _ title: String) -> BusinessSetupFieldSpec {
        BusinessSetupFieldSpec(key: key, label: title, placeholder: "", kind: .panelHeader(title))
    }

    private static func reviewSummary(_ key: String, _ label: String, _ keys: [String]) -> BusinessSetupFieldSpec {
        BusinessSetupFieldSpec(key: key, label: label, placeholder: "", kind: .reviewSummary(keys))
    }
}
