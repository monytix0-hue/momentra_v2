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
    let icon: String
    let colors: [Color]
    /// Hub enables when moment is active and expense/movement capability allows money quick-add.
    let enabledWhenMomentActive: Bool
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

    /// Builds hub tiles for a family.
    /// - `nil` capabilityCodes → family default V019 codes (catalog / tests)
    /// - empty array → fail closed (no tiles)
    /// - non-empty → destination-level enablement
    static func tiles(
        for family: PersonalPulseFamily,
        hasActiveMoment: Bool,
        capabilityCodes: [String]? = nil
    ) -> [PersonalActionTile] {
        let effectiveCaps = capabilityCodes ?? defaultCodes(for: family).map(\.rawValue)
        let catalog = catalogTiles(for: family)
        return catalog.compactMap { tile in
            let dest = destination(for: tile.code)
            if !isDestinationEnabled(effectiveCaps, destination: dest) { return nil }
            return PersonalActionTile(
                code: tile.code,
                label: tile.label,
                icon: tile.icon,
                colors: tile.colors,
                enabledWhenMomentActive: tile.enabledWhenMomentActive && hasActiveMoment
            )
        }
    }

    /// Hub labels still drive sheet routing; codes are the governance source of truth.
    private static func catalogTiles(for family: PersonalPulseFamily) -> [PersonalActionTile] {
        switch family {
        case .futureBuilding:
            return [
                tile(.milestoneCreate, "Milestone", "scope", "#8B5CF6", "#6C4EF2"),
                tile(.opportunityCreate, "Opportunity", "waveform.path.ecg", "#3B82F6", "#1D4ED8"),
                tile(.pivotRecord, "Pivot", "arrow.triangle.2.circlepath", "#06B6D4", "#0891B2"),
                tile(.progressRecord, "Progress", "chart.line.uptrend.xyaxis", "#10B981", "#047857"),
                tile(.learningActivityCreate, "Learning", "book", "#6366F1", "#4338CA"),
            ]
        case .lifestyle:
            return [
                tile(.lifestyleActivityCreate, "Experience", "wallet.pass", "#EC4899", "#BE185D"),
                tile(.lifestyleActivityCreate, "Wellbeing", "waveform.path.ecg", "#A78BFA", "#7C3AED"),
                tile(.lifestyleActivityCreate, "Discovery", "face.smiling", "#F472B6", "#C026D3"),
                tile(.lifestyleActivityCreate, "Create", "scope", "#FB7185", "#F43F5E"),
                tile(.lifestyleActivityCreate, "Adjust", "arrow.triangle.2.circlepath", "#6366F1", "#4338CA"),
            ]
        case .relationships:
            return [
                tile(.relationshipActivityRecord, "Connection", "person.2", "#E12A9E", "#BE1882"),
                tile(.relationshipActivityRecord, "Support", "heart", "#C8238C", "#A51473"),
                tile(.relationshipActivityRecord, "Shared Exp", "camera", "#EB3CAA", "#C82891"),
                tile(.relationshipActivityRecord, "Investment", "chart.line.uptrend.xyaxis", "#F578C8", "#E12A9E"),
                tile(.relationshipActivityRecord, "Adjust", "slider.horizontal.3", "#F064B9", "#D23296"),
            ]
        case .lifeOperations:
            return [
                tile(.expenseCreate, "Income", "chart.line.uptrend.xyaxis", "#10B981", "#047857"),
                tile(.lifeObservationRecord, "Recovery", "waveform.path.ecg", "#3B82F6", "#1D4ED8"),
                tile(.lifeObservationRecord, "Mood", "face.smiling", "#06B6D4", "#0891B2"),
                tile(.lifeObservationRecord, "Attention", "scope", "#A78BFA", "#7C3AED"),
                tile(.movementRecord, "Transfer", "arrow.triangle.2.circlepath", "#1E40AF", "#0B2A8A"),
                tile(.movementRecord, "Savings", "chart.line.uptrend.xyaxis", "#10B981", "#047857"),
                tile(.lifeObservationRecord, "Adjust", "slider.horizontal.3", "#D946EF", "#86198F"),
                tile(.lifeObservationRecord, "Reflect", "book", "#6366F1", "#4338CA", enabled: false),
            ]
        }
    }

    private static func tile(
        _ code: PersonalActionCode,
        _ label: String,
        _ icon: String,
        _ start: String,
        _ end: String,
        enabled: Bool = true
    ) -> PersonalActionTile {
        PersonalActionTile(
            code: code,
            label: label,
            icon: icon,
            colors: [Color(hex: start), Color(hex: end)],
            enabledWhenMomentActive: enabled
        )
    }
}
