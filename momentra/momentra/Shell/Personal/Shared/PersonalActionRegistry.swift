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

struct PersonalHubSection: Identifiable {
    var id: String { title ?? "root" }
    let title: String?
    let tiles: [PersonalActionTile]
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

    /// Unified Personal catalog with section headers (Everyday, Future, Lifestyle, People).
    static func unifiedSections(
        presentFamilies: Set<PersonalPulseFamily>,
        hasActiveMoment: Bool,
        capabilityCodes: [String]? = nil
    ) -> [PersonalHubSection] {
        var sections: [PersonalHubSection] = []
        let order: [(PersonalPulseFamily, String)] = [
            (.lifeOperations, "Everyday"),
            (.futureBuilding, "Future"),
            (.lifestyle, "Lifestyle"),
            (.relationships, "People"),
        ]
        for (family, title) in order where presentFamilies.contains(family) {
            let caps = capabilityCodes ?? defaultCodes(for: family).map(\.rawValue)
            let tiles = catalogTiles(for: family).map { tile in
                let enabled = hasActiveMoment && tile.tappable && (
                    family != .lifeOperations
                        || isTileEnabled(tile, hasActiveMoment: true, capabilityCodes: caps.isEmpty ? defaultCodes(for: .lifeOperations).map(\.rawValue) : caps)
                )
                return PersonalActionTile(
                    code: tile.code,
                    label: tile.label,
                    icon: tile.icon,
                    colors: tile.colors,
                    enabledWhenMomentActive: family == .lifeOperations ? enabled : (hasActiveMoment && tile.tappable),
                    tappable: tile.tappable
                )
            }
            if !tiles.isEmpty {
                sections.append(PersonalHubSection(title: title, tiles: tiles))
            }
        }
        if sections.isEmpty && hasActiveMoment {
            sections.append(PersonalHubSection(title: "Everyday", tiles: Array(catalogTiles(for: .lifeOperations).prefix(3))))
        }
        return sections
    }

    /// Unified Personal catalog — Everyday first, then present family tiles (presence unlocks).
    static func unifiedTiles(
        presentFamilies: Set<PersonalPulseFamily>,
        hasActiveMoment: Bool,
        capabilityCodes: [String]? = nil
    ) -> [PersonalActionTile] {
        var out: [PersonalActionTile] = []
        let order: [PersonalPulseFamily] = [.lifeOperations, .futureBuilding, .lifestyle, .relationships]
        for family in order where presentFamilies.contains(family) {
            let caps = capabilityCodes ?? defaultCodes(for: family).map(\.rawValue)
            let familyTiles = catalogTiles(for: family).map { tile in
                // Presence unlocks; empty Life Ops caps alone must not hide Future tiles.
                let enabled = hasActiveMoment && tile.tappable && (
                    family != .lifeOperations
                        || isTileEnabled(tile, hasActiveMoment: true, capabilityCodes: caps.isEmpty ? defaultCodes(for: .lifeOperations).map(\.rawValue) : caps)
                )
                return PersonalActionTile(
                    code: tile.code,
                    label: tile.label,
                    icon: tile.icon,
                    colors: tile.colors,
                    enabledWhenMomentActive: family == .lifeOperations ? enabled : (hasActiveMoment && tile.tappable),
                    tappable: tile.tappable
                )
            }
            // Deduplicate Adjust across families — keep first (Everyday).
            for tile in familyTiles {
                if tile.label == "Adjust", out.contains(where: { $0.label == "Adjust" }) { continue }
                out.append(tile)
            }
        }
        if out.isEmpty && hasActiveMoment {
            out = catalogTiles(for: .lifeOperations).prefix(3).map {
                PersonalActionTile(
                    code: $0.code,
                    label: $0.label,
                    icon: $0.icon,
                    colors: $0.colors,
                    enabledWhenMomentActive: true,
                    tappable: $0.tappable
                )
            }
        }
        return out
    }

    /// Primary Today / Add row labels for a family (exactly 3).
    static func primaryLabels(for family: PersonalPulseFamily) -> [String] {
        family.theme.todayActionLabels
    }

    /// Family-first hub: primaries then secondaries for the selected family.
    static func familyFirstSections(
        selected: PersonalPulseFamily,
        presentFamilies: Set<PersonalPulseFamily>,
        hasActiveMoment: Bool,
        capabilityCodes: [String]? = nil,
        showOtherFamilies: Bool = false
    ) -> [PersonalHubSection] {
        let caps = capabilityCodes ?? defaultCodes(for: selected).map(\.rawValue)
        let all = tiles(for: selected, hasActiveMoment: hasActiveMoment, capabilityCodes: caps)
        let primaries = Set(primaryLabels(for: selected))
        // Map Connect → Connection, Shared → Shared Exp for registry labels.
        let primaryTiles = all.filter { tile in
            primaries.contains(tile.label)
                || (selected == .relationships && tile.label == "Connection" && primaries.contains("Connect"))
                || (selected == .relationships && (tile.label == "Shared Exp" || tile.label == "Shared") && primaries.contains("Shared"))
        }
        let orderedPrimary: [PersonalActionTile] = primaryLabels(for: selected).compactMap { label in
            primaryTiles.first {
                $0.label == label
                    || (label == "Connect" && $0.label == "Connection")
                    || (label == "Shared" && ($0.label == "Shared Exp" || $0.label == "Shared"))
            }
        }
        let secondary = all.filter { tile in !orderedPrimary.contains(where: { $0.id == tile.id }) }
        var sections: [PersonalHubSection] = [
            PersonalHubSection(title: nil, tiles: orderedPrimary + secondary),
        ]
        if showOtherFamilies {
            let others = PersonalPulseFamily.allCases.filter { $0 != selected && presentFamilies.contains($0) }
            for family in others {
                let familyCaps = capabilityCodes ?? defaultCodes(for: family).map(\.rawValue)
                let tiles = tiles(for: family, hasActiveMoment: hasActiveMoment, capabilityCodes: familyCaps)
                if !tiles.isEmpty {
                    sections.append(PersonalHubSection(title: family.switcherLabel, tiles: tiles))
                }
            }
        }
        return sections
    }

    /// Builds hub tiles for a family — always returns the full catalog; greys tiles when capability/moment inactive.
    /// When `simpleMode` is true on Life Ops, keeps Expense / Mood / Recovery only.
    static func tiles(
        for family: PersonalPulseFamily,
        hasActiveMoment: Bool,
        capabilityCodes: [String]? = nil,
        simpleMode: Bool = false
    ) -> [PersonalActionTile] {
        let effectiveCaps = capabilityCodes ?? defaultCodes(for: family).map(\.rawValue)
        var catalog = catalogTiles(for: family)
        if simpleMode && family == .lifeOperations {
            let keep = Set(["Spend", "Mood", "Recovery"])
            catalog = catalog.filter { keep.contains($0.label) }
        }
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
        case "Spend", "Expense", "Income", "Transfer", "Savings":
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
                tile(.progressRecord, "Progress", "QaTrending", "#10B981", "#047857"),
                tile(.learningActivityCreate, "Learning", "QaBook", "#6366F1", "#4338CA"),
                tile(.opportunityCreate, "Opportunity", "QaActivity", "#3B82F6", "#1D4ED8"),
                tile(.pivotRecord, "Pivot", "QaRefresh", "#06B6D4", "#0891B2"),
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
                tile(.relationshipActivityRecord, "Shared Exp", "QaCamera", "#EB3CAA", "#C82891"),
                tile(.relationshipActivityRecord, "Support", "QaHeart", "#C8238C", "#A51473"),
                tile(.relationshipActivityRecord, "Investment", "QaTrending", "#F578C8", "#E12A9E"),
                tile(.relationshipActivityRecord, "Adjust", "QaSliders", "#F064B9", "#D23296"),
            ]
        case .lifeOperations:
            return [
                tile(.expenseCreate, "Spend", "QaWallet", "#8B5CF6", "#6C4EF2"),
                tile(.lifeObservationRecord, "Mood", "QaSmile", "#06B6D4", "#0891B2"),
                tile(.lifeObservationRecord, "Recovery", "QaActivity", "#3B82F6", "#1D4ED8"),
                tile(.expenseCreate, "Income", "QaTrending", "#10B981", "#047857"),
                tile(.lifeObservationRecord, "Attention", "QaTarget", "#A78BFA", "#7C3AED"),
                tile(.movementRecord, "Transfer", "QaRefresh", "#1E40AF", "#0B2A8A"),
                tile(.movementRecord, "Savings", "QaTrending", "#10B981", "#047857"),
                tile(.lifeObservationRecord, "Adjust", "QaSliders", "#D946EF", "#86198F"),
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
