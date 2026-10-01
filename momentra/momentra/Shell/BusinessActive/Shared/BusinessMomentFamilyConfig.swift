import Foundation

enum BusinessMomentFamily: String, Equatable {
    case money
    case daily
    case team
}

struct BusinessMomentFamilySpec: Equatable {
    let family: BusinessMomentFamily
    let title: String
    let primary: [BusinessQuickAddKind]
    let secondary: [BusinessQuickAddKind]
}

enum BusinessMomentFamilyConfig {
    static func forTypeCode(_ momentTypeCode: String?, smallShop: Bool) -> BusinessMomentFamilySpec {
        forFamily(familyOrNil(momentTypeCode) ?? .team, smallShop: smallShop)
    }

    /// Selected moment only. A blank type on that moment is not replaced by another moment's code.
    static func selectedQuickAddTypeCode(
        selectedMomentId: String?,
        moments: [MomentSummary],
        selectedMomentTypeCode: String?
    ) -> String? {
        if let moment = moments.first(where: { $0.momentId == selectedMomentId }) {
            let code = moment.momentTypeCode?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
            return code.isEmpty ? nil : code
        }
        let fallback = selectedMomentTypeCode?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        return fallback.isEmpty ? nil : fallback
    }

    /// Nil when the code is blank or not a Business family. Does not default to Team.
    static func familyOrNil(_ momentTypeCode: String?) -> BusinessMomentFamily? {
        let code = (momentTypeCode ?? "").trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        if code.isEmpty { return nil }
        if code.contains("RUNWAY") { return .money }
        if code.contains("OPERATIONS") && !code.contains("TEAM") { return .daily }
        if code.contains("TEAM") { return .team }
        return nil
    }

    static func quickAddSpec(_ momentTypeCode: String?, smallShop: Bool) -> BusinessMomentFamilySpec? {
        guard let family = familyOrNil(momentTypeCode) else { return nil }
        return forFamily(family, smallShop: smallShop)
    }

    static func quickAddPrimary(
        _ momentTypeCode: String?,
        smallShop: Bool,
        capabilities: [String]?
    ) -> [BusinessQuickAddKind] {
        guard let spec = quickAddSpec(momentTypeCode, smallShop: smallShop) else { return [] }
        return visibleActions(spec.primary, capabilities: capabilities, momentTypeCode: momentTypeCode)
    }

    static func forFamily(_ family: BusinessMomentFamily, smallShop: Bool) -> BusinessMomentFamilySpec {
        switch family {
        case .money: return smallShop ? shopMoney : growingMoney
        case .daily: return smallShop ? shopDaily : growingDaily
        case .team: return smallShop ? shopTeam : growingTeam
        }
    }

    /// Intersects the family's configured ids with capability availability.
    /// Empty capabilities fail open to the default codes, then that set filters this list.
    /// The registry does not supply a replacement catalog.
    static func visibleActions(
        _ kinds: [BusinessQuickAddKind],
        capabilities: [String]?,
        momentTypeCode: String?
    ) -> [BusinessQuickAddKind] {
        kinds.filter { kind in
            kind.isLive && BusinessActionRegistry.isKindEnabled(
                kind,
                capabilities: capabilities,
                momentTypeCode: momentTypeCode
            )
        }
    }

    private static let growingMoney = BusinessMomentFamilySpec(
        family: .money,
        title: "Money & Cash Flow",
        primary: [.revenue, .expense, .invoice],
        secondary: [.taxEntry, .investorUpdate, .budgetAlert, .forecastUpdate, .generalUpdate, .memory]
    )
    private static let shopMoney = BusinessMomentFamilySpec(
        family: .money,
        title: "Money & Cash Flow",
        primary: [.khata, .revenue, .expense],
        secondary: [.invoice, .memory]
    )
    private static let growingDaily = BusinessMomentFamilySpec(
        family: .daily,
        title: "Daily Business",
        primary: [.spendEntry, .updateVendor, .reportIssue],
        secondary: [.requestApproval, .logImprovement, .budgetReview, .slaCheck, .generalUpdate, .memory]
    )
    private static let shopDaily = BusinessMomentFamilySpec(
        family: .daily,
        title: "Daily Business",
        primary: [.spendEntry, .updateVendor, .reportIssue],
        secondary: [.khata, .memory]
    )
    private static let growingTeam = BusinessMomentFamilySpec(
        family: .team,
        title: "Team & Work",
        primary: [.teamUpdate, .approval, .decision],
        secondary: [.blocker, .meeting, .recognition, .milestone, .retrospective, .riskFlag, .activityLog, .poll, .expense, .memory]
    )
    private static let shopTeam = BusinessMomentFamilySpec(
        family: .team,
        title: "Team & Work",
        primary: [.khata, .teamUpdate, .approval],
        secondary: [.expense, .memory]
    )
}
