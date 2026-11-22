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

    static let clientMarketing = BusinessSetupDocumentSpec(
        typeCode: "CLIENT_CAMPAIGN",
        familyTitle: "Marketing",
        eyebrow: "Client campaign",
        heroTitle: "Set up a client campaign",
        heroSubtitle: "Budgets, channels, retainers, and leads for one client engagement.",
        activateLabel: "Start this business",
        defaultTitleKey: "campaignName",
        sections: [
            section("basics", "Business Basics", "Name the engagement and client.", [
                text("campaignName", "Campaign name", "Brightline Q2 push"),
                text("clientName", "Client name", "Acme Co."),
                choice("billingModel", "Billing", ["Retainer", "Project", "Hybrid"]),
                date("startDate", "Start date", "01 Nov 2024"),
                date("endDate", "End date", "30 Nov 2024"),
                text("currency", "Currency", "INR"),
            ]),
            section("people", "People & Roles", "Who runs the work day to day.", [
                memberList("teamMembers", "Team", ["You (Owner)", "Account lead"]),
                multiChoice("roles", "Roles", ["Owner", "Admin", "Commercial", "Viewer", "Executive"]),
                rowAction("addPeople", "Add specialized people", "Add"),
            ]),
            section("network", "Network / Counterparties", "Partners and platforms on this engagement.", [
                moduleList("modules", "Modules", [
                    "Agency / partners",
                    "Media platforms",
                    "Creative vendors",
                    "Analytics tools",
                    "Client stakeholders",
                ]),
            ]),
            section("channels", "Channels & Monthly Plan", "Where spend and leads will land.", [
                multiChoice("channels", "Channels", ["Meta Ads", "Google", "LinkedIn", "Email", "Other"]),
                text("monthlyBudget", "Monthly budget", "₹2,00,000"),
                text("leadGoal", "Lead / revenue goal", "120 leads / month"),
                choice("billingFrequency", "Billing frequency", ["Monthly", "Quarterly", "Half yearly"]),
            ]),
            section("gst", "GST & Tax Details", "Registration for this engagement.", [
                toggle("gstRegistered", "Registered for GST?"),
                choice("gstType", "Registration type", ["Regular", "Composition", "Unregistered"]),
                text("gstState", "State", "Maharashtra"),
                text("gstin", "GSTIN / PAN", "Optional"),
            ]),
            section("review", "Review your setup", "Confirm before you start.", [
                reviewSummary("review", "Summary", [
                    "campaignName", "clientName", "billingModel", "monthlyBudget", "leadGoal",
                ]),
            ]),
        ]
    )

    static let ownMarketing = BusinessSetupDocumentSpec(
        typeCode: "OWN_BUSINESS_CAMPAIGN",
        familyTitle: "Marketing",
        eyebrow: "Own business",
        heroTitle: "Promote your own business",
        heroSubtitle: "Track ads, offers, and inbound interest for your brand — not a client.",
        activateLabel: "Start this business",
        defaultTitleKey: "campaignName",
        sections: [
            section("basics", "Business Basics", "What you're promoting.", [
                text("campaignName", "Campaign name", "Summer launch"),
                text("brandName", "Brand / business", "Harbor Mart"),
                choice("offerType", "Offer type", ["Promotion", "Launch", "Always-on"]),
                date("startDate", "Start date", "01 Nov 2024"),
                text("currency", "Currency", "INR"),
            ]),
            section("people", "People & Roles", "Who owns the spend.", [
                memberList("teamMembers", "Team", ["You (Owner)"]),
                multiChoice("roles", "Roles", ["Owner", "Admin", "Marketer", "Viewer"]),
                rowAction("addPeople", "Add member", "Add"),
            ]),
            section("network", "Network / Counterparties", "Channels and tools you use.", [
                moduleList("modules", "Modules", [
                    "Ad platforms",
                    "Landing pages",
                    "CRM / leads",
                    "Creative assets",
                ]),
            ]),
            section("channels", "Channels & Monthly Plan", "Where your ads run.", [
                multiChoice("channels", "Channels", ["Meta", "Google", "WhatsApp", "Other"]),
                text("monthlyBudget", "Monthly budget", "₹50,000"),
                text("conversionGoal", "Conversion goal", "Store visits / sales"),
            ]),
            section("gst", "GST & Tax Details", "Your business registration.", [
                toggle("gstRegistered", "Registered for GST?"),
                choice("gstType", "Registration type", ["Regular", "Composition", "Unregistered"]),
                text("gstState", "State", "Maharashtra"),
                text("gstin", "GSTIN", "Optional"),
            ]),
            section("review", "Review your setup", "Confirm before you start.", [
                reviewSummary("review", "Summary", [
                    "campaignName", "brandName", "offerType", "monthlyBudget", "conversionGoal",
                ]),
            ]),
        ]
    )

    // MARK: - Trading (5:18833 / 5:8637)

    static let trading = BusinessSetupDocumentSpec(
        typeCode: "TRADING_WHOLESALE",
        familyTitle: "Trading",
        eyebrow: "Wholesale trading",
        heroTitle: "Set up wholesale trading",
        heroSubtitle: "Bring your business, supply, and mobility plan together before you start.",
        activateLabel: "Start the business!",
        defaultTitleKey: "businessName",
        sections: [
            section("basics", "Business Basics", "Who you trade as.", [
                text("businessName", "Business name", "Northstar Trading"),
                text("companyName", "Company name", "Northstar Trading"),
                text("gstin", "GSTIN", "15-digit GSTIN"),
                text("industry", "Industry", "FMCG, Retail & Personal Care"),
                date("startedOn", "Started on", "12 October 2023"),
                text("headquarters", "Headquarters", "City / location"),
            ]),
            section("people", "People & Roles", "Who runs the trade.", [
                memberList("teamMembers", "Team", ["You (Administrator)"]),
                multiChoice("roles", "Roles", ["Owners", "Manager", "Delivery", "Warehouse", "Salesman"]),
                rowAction("addPeople", "Add Member / Invite Member", "Add"),
            ]),
            section("channels", "Channels & Monthly Plan", "How you sell and plan.", [
                multiChoice("channels", "Platforms", ["Business App", "WhatsApp", "Phone/Mobile", "Website", "Others"]),
                text("targetSales", "Target sales", "₹25,00,000"),
                text("monthlyExpense", "Monthly expense", "₹1,00,000"),
                text("marketingBudget", "Monthly marketing budget", "₹20,000"),
            ]),
            section("network", "Catalogue, Warehouses & Network", "Modules that light up after start.", [
                moduleList("modules", "Modules", [
                    "Catalogue & Bills",
                    "e-Commerce Store",
                    "Delivery & Logistics",
                    "Suppliers & Buyers",
                    "Team Tasks & Chat",
                ]),
            ]),
            section("gst", "GST & Tax Details", "Registration in your country.", [
                toggle("gstRegistered", "Are you registered for GST in the country?"),
                choice("gstType", "Registration type", ["Regular", "Composition", "Unregistered"]),
                text("gstState", "State", "Maharashtra"),
                choice("filingFrequency", "Filing frequency", ["Monthly", "Quarterly"]),
            ]),
            section("review", "Review your setup", "Confirm before you start.", [
                reviewSummary("review", "Summary", [
                    "businessName", "companyName", "gstin", "industry", "targetSales",
                ]),
            ]),
        ]
    )

    static let distribution = BusinessSetupDocumentSpec(
        typeCode: "DISTRIBUTION_DEALERSHIP",
        familyTitle: "Trading",
        eyebrow: "Distribution",
        heroTitle: "Set up distribution",
        heroSubtitle: "Routes, dealers, and collections for a distribution moment.",
        activateLabel: "Start the business!",
        defaultTitleKey: "businessName",
        sections: [
            section("basics", "Business Basics", "Who you distribute as.", [
                text("businessName", "Distributor name", "Coastal Distributors"),
                text("principalBrand", "Principal / brand", "Optional"),
                text("territory", "Territory", "West zone"),
                text("gstin", "GSTIN", "Optional"),
            ]),
            section("people", "People & Roles", "Field and office roles.", [
                memberList("teamMembers", "Team", ["You (Owner)"]),
                multiChoice("roles", "Roles", ["Owner", "Manager", "Delivery", "Salesman"]),
                rowAction("addPeople", "Add Member / Invite Member", "Add"),
            ]),
            section("channels", "Channels & Monthly Plan", "Coverage and collections.", [
                choice("routeStyle", "Coverage", ["Beat routes", "Dealer network", "Both"]),
                text("dealerCount", "Approx. dealers", "40"),
                text("collectionCycle", "Collection cycle", "Weekly"),
                text("targetSales", "Target sales", "₹10,00,000"),
            ]),
            section("network", "Catalogue, Warehouses & Network", "Modules that light up after start.", [
                moduleList("modules", "Modules", [
                    "Catalogue & Bills",
                    "Dealer network",
                    "Delivery & Logistics",
                    "Suppliers & Buyers",
                    "Team Tasks & Chat",
                ]),
            ]),
            section("gst", "GST & Tax Details", "Registration for distribution.", [
                toggle("gstRegistered", "Registered for GST?"),
                choice("gstType", "Registration type", ["Regular", "Composition", "Unregistered"]),
                text("gstState", "State", "Maharashtra"),
            ]),
            section("review", "Review your setup", "Confirm before you start.", [
                reviewSummary("review", "Summary", [
                    "businessName", "territory", "routeStyle", "dealerCount", "targetSales",
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
                text("currency", "Currency", "INR · Indian Rupee (₹)"),
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
                text("targetMetric", "How the target is measured", "Revenue"),
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
                text("currency", "Currency", "INR · Indian Rupee (₹)"),
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
                text("targetMetric", "How the target is measured", "Revenue"),
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
                text("currency", "Currency", "INR · Indian Rupee (₹)"),
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
                text("targetMetric", "How the target is measured", "Revenue"),
            ]),
            section(
                "network",
                "Services, Rooms & Clients",
                "Masters your bookings, charges and purchases read from. Each opens its own sheet.",
                [
                    moduleListRows("modules", "Modules", [
                        BusinessSetupModuleRow("Services catalogue", subtitle: "26 services · 4 packages · SAC + GST on each"),
                        BusinessSetupModuleRow("Rooms & infrastructure", subtitle: "Name, capacity, and bookable slots"),
                        BusinessSetupModuleRow("Team & Staff", subtitle: "Name, duration, price, SAC, GST, room"),
                        BusinessSetupModuleRow("Clients", subtitle: "312 clients · GSTIN only for B2B"),
                        BusinessSetupModuleRow("Team Tasks & Chat", subtitle: "Stats Manager active · Interface items"),
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
                    "monthlyChargeTarget",
                    "targetMetric",
                ]),
            ]),
        ]
    )

    // MARK: - Manufacturing (5:50728 / 5:55863)

    static let manufacturing = BusinessSetupDocumentSpec(
        typeCode: "STANDARD_PRODUCTION",
        familyTitle: "Manufacturing",
        eyebrow: "Manufacturing",
        heroTitle: "Set up production",
        heroSubtitle: "Batches, BOM, and finished goods for standard production.",
        activateLabel: "Start this business",
        defaultTitleKey: "plantName",
        sections: [
            section("basics", "Business Basics", "Your plant.", [
                text("plantName", "Plant / business", "Northline Works"),
                text("productLine", "Product line", "What you make"),
                choice("batchStyle", "Production", ["Batch", "Continuous", "Mixed"]),
                text("gstin", "GSTIN", "Optional"),
            ]),
            section("people", "People & Roles", "Floor and planning.", [
                memberList("teamMembers", "Team", ["You (Owner)"]),
                multiChoice("roles", "Roles", ["Owner", "Plant manager", "Supervisor", "Operator"]),
                rowAction("addPeople", "Add Member", "Add"),
            ]),
            section("channels", "Channels & Monthly Plan", "Output targets.", [
                text("monthlyOutput", "Monthly output target", "Units / value"),
                text("wipNote", "WIP note", "Optional"),
                multiChoice("channels", "Sales channels", ["Dealers", "Direct", "Export", "Other"]),
            ]),
            section("network", "BOM, Warehouses & Network", "Modules that light up after start.", [
                moduleList("modules", "Modules", [
                    "BOM & recipes",
                    "Raw materials",
                    "Finished goods",
                    "Suppliers & buyers",
                ]),
            ]),
            section("gst", "GST & Tax Details", "Plant registration.", [
                toggle("gstRegistered", "Registered for GST?"),
                choice("gstType", "Registration type", ["Regular", "Composition", "Unregistered"]),
                text("gstState", "State", "Maharashtra"),
            ]),
            section("review", "Review your setup", "Confirm before you start.", [
                reviewSummary("review", "Summary", [
                    "plantName", "productLine", "batchStyle", "monthlyOutput",
                ]),
            ]),
        ]
    )

    static let madeToOrder = BusinessSetupDocumentSpec(
        typeCode: "MADE_TO_ORDER",
        familyTitle: "Manufacturing",
        eyebrow: "Made to order",
        heroTitle: "Set up job-order work",
        heroSubtitle: "Quotes, job orders, and invoices against custom work.",
        activateLabel: "Start this business",
        defaultTitleKey: "shopName",
        sections: [
            section("basics", "Business Basics", "What customers order.", [
                text("shopName", "Shop / business", "Precision Jobs"),
                text("jobType", "Job type", "Fabrication / print / …"),
                choice("quoteStyle", "Quotes", ["Formal quotes", "Verbal + invoice", "Both"]),
                text("gstin", "GSTIN", "Optional"),
            ]),
            section("people", "People & Roles", "Who owns the jobs.", [
                memberList("teamMembers", "Team", ["You (Owner)"]),
                multiChoice("roles", "Roles", ["Owner", "Estimator", "Shop floor", "Accounts"]),
                rowAction("addPeople", "Add Member", "Add"),
            ]),
            section("channels", "Channels & Monthly Plan", "How orders move.", [
                choice("leadTime", "Typical lead time", ["Same day", "2–7 days", "Longer"]),
                text("capacityNote", "Capacity note", "Optional"),
                text("monthlyJobs", "Jobs / month target", "40"),
            ]),
            section("network", "Jobs & Network", "Modules that light up after start.", [
                moduleList("modules", "Modules", [
                    "Quotes",
                    "Job orders",
                    "Materials",
                    "Invoices",
                ]),
            ]),
            section("gst", "GST & Tax Details", "Shop registration.", [
                toggle("gstRegistered", "Registered for GST?"),
                choice("gstType", "Registration type", ["Regular", "Composition", "Unregistered"]),
                text("gstState", "State", "Maharashtra"),
            ]),
            section("review", "Review your setup", "Confirm before you start.", [
                reviewSummary("review", "Summary", [
                    "shopName", "jobType", "quoteStyle", "leadTime",
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
        _ modules: [BusinessSetupModuleRow]
    ) -> BusinessSetupFieldSpec {
        BusinessSetupFieldSpec(key: key, label: label, placeholder: "", kind: .moduleList(modules))
    }

    private static func panelHeader(_ key: String, _ title: String) -> BusinessSetupFieldSpec {
        BusinessSetupFieldSpec(key: key, label: title, placeholder: "", kind: .panelHeader(title))
    }

    private static func reviewSummary(_ key: String, _ label: String, _ keys: [String]) -> BusinessSetupFieldSpec {
        BusinessSetupFieldSpec(key: key, label: label, placeholder: "", kind: .reviewSummary(keys))
    }
}
