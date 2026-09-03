import Foundation

struct BusinessSetupTextFieldSpec {
    let key: String
    let label: String
}

struct BusinessSetupSectionSpec {
    let title: String
    let fields: [PersonalSetupFieldSpec]
    var textFields: [BusinessSetupTextFieldSpec] = []
}

struct BusinessSetupCatalogEntry {
    let defaultTitle: String
    let subtitle: String
    let momentTypeCode: String
    let activateLabel: String
    let footerTagline: String
    let sections: [BusinessSetupSectionSpec]
    let fields: [PersonalSetupFieldSpec]
    let defaultPreferences: [String: Any]
}

enum BusinessSetupCatalog {
    private static let teamOps = BusinessSetupCatalogEntry(
        defaultTitle: "Team Operations",
        subtitle: "Configure your team's operating system in one go.",
        momentTypeCode: "TEAM_OPERATIONS",
        activateLabel: "Activate Team Operations",
        footerTagline: "Run your team with clarity",
        sections: [
            BusinessSetupSectionSpec(
                title: "Team basics",
                fields: [
                    PersonalSetupFieldSpec(key: "teamName", label: "Team name", multiSelect: false, options: ["Growth & Product", "Operations", "Customer Success"]),
                    PersonalSetupFieldSpec(key: "size", label: "Team size", multiSelect: false, options: ["1-10 people", "11-25 people", "26-50 people", "50+ people"]),
                    PersonalSetupFieldSpec(key: "workMode", label: "Work mode", multiSelect: false, options: ["Remote", "Hybrid", "In-office"]),
                ]
            ),
            BusinessSetupSectionSpec(
                title: "Locale & finance",
                fields: [
                    PersonalSetupFieldSpec(key: "country", label: "Country", multiSelect: false, options: ["India", "United States", "United Kingdom", "Singapore"]),
                    PersonalSetupFieldSpec(key: "currency", label: "Currency", multiSelect: false, options: ["INR", "USD", "GBP", "EUR"]),
                    PersonalSetupFieldSpec(key: "timezone", label: "Timezone", multiSelect: false, options: ["IST (UTC+5:30)", "UTC", "EST (UTC-5)", "PST (UTC-8)"]),
                    PersonalSetupFieldSpec(key: "language", label: "Language", multiSelect: false, options: ["English", "Hindi", "Spanish", "French"]),
                    PersonalSetupFieldSpec(key: "financialYear", label: "Financial year", multiSelect: false, options: ["Apr - Mar", "Jan - Dec", "Jul - Jun"]),
                    PersonalSetupFieldSpec(key: "taxSystem", label: "Tax system", multiSelect: false, options: ["GST", "VAT", "Sales tax"]),
                ]
            ),
            BusinessSetupSectionSpec(
                title: "Governance",
                fields: [
                    PersonalSetupFieldSpec(key: "coordination", label: "Coordination style", multiSelect: false, options: ["Structured", "Flexible", "Async-first"]),
                    PersonalSetupFieldSpec(key: "reviewCycle", label: "Review cycle", multiSelect: false, options: ["Weekly", "Bi-weekly", "Monthly"]),
                    PersonalSetupFieldSpec(key: "monitoring", label: "Monitoring", multiSelect: false, options: ["Balanced", "Light", "Intensive"]),
                    PersonalSetupFieldSpec(key: "spendingApproval", label: "Spending approval", multiSelect: false, options: ["Required", "Optional", "Not required"]),
                ],
                textFields: [
                    BusinessSetupTextFieldSpec(key: "approvalThreshold", label: "Approval threshold"),
                ]
            ),
        ],
        fields: [],
        defaultPreferences: [
            "teamName": "Growth & Product",
            "size": "11-25 people",
            "workMode": "Hybrid",
            "country": "India",
            "currency": "INR",
            "timezone": "IST (UTC+5:30)",
            "language": "English",
            "financialYear": "Apr - Mar",
            "taxSystem": "GST",
            "coordination": "Structured",
            "reviewCycle": "Weekly",
            "monitoring": "Balanced",
            "spendingApproval": "Required",
            "approvalThreshold": "₹50,000",
        ]
    )

    private static let runway = BusinessSetupCatalogEntry(
        defaultTitle: "Business Runway",
        subtitle: "Configure your financial operating system on-the-go.",
        momentTypeCode: "BUSINESS_RUNWAY",
        activateLabel: "Activate Business Runway",
        footerTagline: "Know your runway",
        sections: [
            BusinessSetupSectionSpec(
                title: "Stage & horizon",
                fields: [
                    PersonalSetupFieldSpec(key: "businessStage", label: "Business stage", multiSelect: false, options: ["Early", "Scaling", "Mature", "Turnaround"]),
                    PersonalSetupFieldSpec(key: "goalHorizon", label: "Goal horizon", multiSelect: false, options: ["6-months goal", "12-months goal", "18-months goal", "24-months goal"]),
                    PersonalSetupFieldSpec(key: "multiCurrency", label: "Multi-currency", multiSelect: false, options: [], kind: .toggle),
                    PersonalSetupFieldSpec(key: "revenueStage", label: "Revenue stage", multiSelect: false, options: ["Pre-revenue", "Growing", "Stable", "Declining"]),
                    PersonalSetupFieldSpec(key: "revenueModel", label: "Revenue model", multiSelect: false, options: ["Recurring", "Project-based", "Mixed", "Marketplace"]),
                    PersonalSetupFieldSpec(key: "warningThreshold", label: "Runway warning threshold", multiSelect: false, options: ["3 months", "6 months", "9 months", "12 months"]),
                ]
            ),
            BusinessSetupSectionSpec(
                title: "Cash & revenue",
                fields: [
                    PersonalSetupFieldSpec(key: "fundingSource", label: "Funding source", multiSelect: false, options: ["Bootstrapped + revenue", "Venture-backed", "Revenue-only", "Grants"]),
                ],
                textFields: [
                    BusinessSetupTextFieldSpec(key: "availableCash", label: "Available cash"),
                    BusinessSetupTextFieldSpec(key: "monthlySpending", label: "Monthly spending"),
                    BusinessSetupTextFieldSpec(key: "monthlyRevenue", label: "Monthly revenue"),
                ]
            ),
        ],
        fields: [],
        defaultPreferences: [
            "businessStage": "Scaling",
            "goalHorizon": "18-months goal",
            "multiCurrency": true,
            "availableCash": "₹ 1,80,00,000",
            "monthlySpending": "₹ 12,50,000",
            "revenueStage": "Growing",
            "monthlyRevenue": "₹ 8,08,000",
            "revenueModel": "Recurring",
            "warningThreshold": "6 months",
            "fundingSource": "Bootstrapped + revenue",
        ]
    )

    private static let ops = BusinessSetupCatalogEntry(
        defaultTitle: "Business Operations",
        subtitle: "Configure operational capacity, monitoring and approvals.",
        momentTypeCode: "BUSINESS_OPERATIONS",
        activateLabel: "Activate Business Operations",
        footerTagline: "Operate with precision",
        sections: [
            BusinessSetupSectionSpec(
                title: "Operations focus",
                fields: [
                    PersonalSetupFieldSpec(key: "coreOps", label: "Core operations focus", multiSelect: false, options: ["Growth & Product", "Delivery", "Support", "Finance"]),
                    PersonalSetupFieldSpec(key: "scope", label: "Scope", multiSelect: false, options: ["Company-wide", "Department", "Team", "Project"]),
                    PersonalSetupFieldSpec(key: "model", label: "Operating model", multiSelect: false, options: ["Centralized", "Distributed", "Hybrid"]),
                    PersonalSetupFieldSpec(key: "cadence", label: "Planning cadence", multiSelect: false, options: ["Weekly", "Monthly", "Quarterly"]),
                    PersonalSetupFieldSpec(key: "monitoringStyle", label: "Monitoring style", multiSelect: false, options: ["Proactive", "Reactive", "Balanced"]),
                ]
            ),
            BusinessSetupSectionSpec(
                title: "Budget & approvals",
                fields: [
                    PersonalSetupFieldSpec(key: "allocationMethod", label: "Allocation method", multiSelect: false, options: ["Category-based", "Project-based", "Team-based"]),
                    PersonalSetupFieldSpec(key: "approvalModel", label: "Approval model", multiSelect: false, options: ["Threshold-based", "Manager sign-off", "Committee"]),
                ],
                textFields: [
                    BusinessSetupTextFieldSpec(key: "monthlyBudget", label: "Monthly budget"),
                    BusinessSetupTextFieldSpec(key: "approvalAlarm", label: "Approval alarm threshold"),
                ]
            ),
        ],
        fields: [],
        defaultPreferences: [
            "coreOps": "Growth & Product",
            "scope": "Company-wide",
            "model": "Centralized",
            "cadence": "Monthly",
            "monthlyBudget": "₹35,00,000",
            "allocationMethod": "Category-based",
            "monitoringStyle": "Proactive",
            "approvalModel": "Threshold-based",
            "approvalAlarm": "₹5,00,000",
        ]
    )

    static func forKind(_ kind: BusinessSetupKind) -> BusinessSetupCatalogEntry {
        switch kind {
        case .teamOperations: return teamOps
        case .businessRunway: return runway
        case .businessOperations: return ops
        }
    }
}
