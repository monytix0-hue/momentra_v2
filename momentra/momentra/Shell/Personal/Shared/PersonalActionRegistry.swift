import Foundation
import SwiftUI

/// V019 Personal Quick Add capability codes (S2 G6). Mirrors Android `PersonalActionRegistry`.
enum PersonalActionCode: String, CaseIterable, Equatable {
    case expenseCreate = "EXPENSE_CREATE"
    case lifeObservationRecord = "LIFE_OBSERVATION_RECORD"
    case goalCreate = "GOAL_CREATE"
    case milestoneCreate = "MILESTONE_CREATE"
    case progressRecord = "PROGRESS_RECORD"
    case opportunityCreate = "OPPORTUNITY_CREATE"
    case pivotRecord = "PIVOT_RECORD"
    case learningActivityCreate = "LEARNING_ACTIVITY_CREATE"
    case lifestyleActivityCreate = "LIFESTYLE_ACTIVITY_CREATE"
    case relationshipActivityRecord = "RELATIONSHIP_ACTIVITY_RECORD"
    case movementRecord = "MOVEMENT_RECORD"
}

struct PersonalActionTile: Identifiable {
    var id: String { "\(code.rawValue)-\(label)" }
    let code: PersonalActionCode
    let label: String
    /// Asset catalog name under `PersonalEmpty` (e.g. `QaWallet`).
    let icon: String
    let colors: [Color]
    /// Hub enables when moment is active and capability allows this tile.
    let enabledWhenMomentActive: Bool
    /// When false the tile stays visible but non-tappable (e.g. Reflect).
    let tappable: Bool
}

enum PersonalActionRegistry {
    enum Destination {
        case expense
        case lifeOps
        case future
        case lifestyle
        case relationships
        case movement
    }

    static func destination(for code: PersonalActionCode) -> Destination {
        switch code {
        case .expenseCreate: return .expense
        case .lifeObservationRecord: return .lifeOps
        case .goalCreate, .milestoneCreate, .progressRecord, .opportunityCreate, .pivotRecord, .learningActivityCreate:
            return .future
        case .lifestyleActivityCreate: return .lifestyle
        case .relationshipActivityRecord: return .relationships
        case .movementRecord: return .movement
        }
    }

    /// Empty capabilities fail closed (match Group / Android). `nil` also fails closed until bootstrap fills V019.
    static func isDestinationEnabled(_ capabilities: [String]?, destination target: Destination) -> Bool {
        guard let capabilities, !capabilities.isEmpty else {
            return false
        }
        let enabled = Set(capabilities.map { $0.uppercased() }.compactMap { cap -> Destination? in
            guard let code = PersonalActionCode(rawValue: cap) else { return nil }
            return destination(for: code)
        })
        return enabled.contains(target)
    }

    static func isMoneyQuickAddEnabled(_ capabilities: [String]?) -> Bool {
        isDestinationEnabled(capabilities, destination: .expense)
            || isDestinationEnabled(capabilities, destination: .movement)
    }

    /// Unique V019 codes available for a personal moment family (hub still expands to labeled tiles).
    static func defaultCodes(for family: PersonalPulseFamily) -> [PersonalActionCode] {
        switch family {
        case .lifeOperations:
            return [.expenseCreate, .lifeObservationRecord, .movementRecord]
        case .futureBuilding:
            return [
                .milestoneCreate,
                .opportunityCreate,
                .pivotRecord,
                .progressRecord,
                .learningActivityCreate,
            ]
        case .lifestyle:
            return [.lifestyleActivityCreate]
        case .relationships:
            return [.relationshipActivityRecord]
        }
    }

    /// Builds hub tiles for a family — always returns the full catalog; greys tiles when capability/moment inactive.
    static func tiles(
        for family: PersonalPulseFamily,
        hasActiveMoment: Bool,
        capabilityCodes: [String]? = nil
    ) -> [PersonalActionTile] {
        let effectiveCaps = capabilityCodes ?? defaultCodes(for: family).map(\.rawValue)
        let catalog = catalogTiles(for: family)
        return catalog.map { tile in
            let enabled = isTileEnabled(
                tile,
                hasActiveMoment: hasActiveMoment,
                capabilityCodes: effectiveCaps
            )
            return PersonalActionTile(
                code: tile.code,
                label: tile.label,
                icon: tile.icon,
                colors: tile.colors,
                enabledWhenMomentActive: enabled,
                tappable: tile.tappable
            )
        }
    }

    private static func isTileEnabled(
        _ tile: PersonalActionTile,
        hasActiveMoment: Bool,
        capabilityCodes: [String]
    ) -> Bool {
        guard hasActiveMoment, tile.tappable else { return false }
        guard !capabilityCodes.isEmpty else { return false }

        switch tile.label {
        case "Expense", "Transfer", "Savings":
            return isMoneyQuickAddEnabled(capabilityCodes)
        default:
            return isDestinationEnabled(capabilityCodes, destination: destination(for: tile.code))
        }
    }

    /// Hub labels still drive sheet routing; codes are the governance source of truth.
    private static func catalogTiles(for family: PersonalPulseFamily) -> [PersonalActionTile] {
        switch family {
        case .futureBuilding:
            return [
                tile(.milestoneCreate, "Milestone", "QaTarget", "#8B5CF6", "#6C4EF2"),
                tile(.opportunityCreate, "Opportunity", "QaActivity", "#3B82F6", "#1D4ED8"),
                tile(.pivotRecord, "Pivot", "QaRefresh", "#06B6D4", "#0891B2"),
                tile(.progressRecord, "Progress", "QaTrending", "#10B981", "#047857"),
                tile(.learningActivityCreate, "Learning", "QaBook", "#6366F1", "#4338CA"),
            ]
        case .lifestyle:
            return [
                tile(.lifestyleActivityCreate, "Experience", "QaWallet", "#EC4899", "#BE185D"),
                tile(.lifestyleActivityCreate, "Wellbeing", "QaActivity", "#A78BFA", "#7C3AED"),
                tile(.lifestyleActivityCreate, "Discovery", "QaSmile", "#F472B6", "#C026D3"),
                tile(.lifestyleActivityCreate, "Create", "QaTarget", "#FB7185", "#F43F5E"),
                tile(.lifestyleActivityCreate, "Adjust", "QaRefresh", "#6366F1", "#4338CA"),
            ]
        case .relationships:
            return [
                tile(.relationshipActivityRecord, "Connection", "QaUsers", "#E12A9E", "#BE1882"),
                tile(.relationshipActivityRecord, "Support", "QaHeart", "#C8238C", "#A51473"),
                tile(.relationshipActivityRecord, "Shared Exp", "QaCamera", "#EB3CAA", "#C82891"),
                tile(.relationshipActivityRecord, "Investment", "QaTrending", "#F578C8", "#E12A9E"),
                tile(.relationshipActivityRecord, "Adjust", "QaSliders", "#F064B9", "#D23296"),
            ]
        case .lifeOperations:
            return [
                tile(.expenseCreate, "Expense", "QaWallet", "#8B5CF6", "#6C4EF2"),
                tile(.lifeObservationRecord, "Recovery", "QaActivity", "#3B82F6", "#1D4ED8"),
                tile(.lifeObservationRecord, "Mood", "QaSmile", "#06B6D4", "#0891B2"),
                tile(.lifeObservationRecord, "Attention", "QaTarget", "#A78BFA", "#7C3AED"),
                tile(.movementRecord, "Transfer", "QaRefresh", "#1E40AF", "#0B2A8A"),
                tile(.movementRecord, "Savings", "QaTrending", "#10B981", "#047857"),
                tile(.lifeObservationRecord, "Adjust", "QaSliders", "#D946EF", "#86198F"),
                tile(.lifeObservationRecord, "Reflect", "QaBook", "#6366F1", "#4338CA", tappable: false),
            ]
        }
    }

    private static func tile(
        _ code: PersonalActionCode,
        _ label: String,
        _ icon: String,
        _ start: String,
        _ end: String,
        tappable: Bool = true
    ) -> PersonalActionTile {
        PersonalActionTile(
            code: code,
            label: label,
            icon: icon,
            colors: [Color(hex: start), Color(hex: end)],
            enabledWhenMomentActive: tappable,
            tappable: tappable
        )
    }
}
