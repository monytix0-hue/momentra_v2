import Foundation
import SwiftUI

/// V019 Group Quick Add + Figma 575:14655 tile catalog. Mirrors Android `GroupActionRegistry`.
enum GroupActionCode: String, CaseIterable, Equatable {
    case expenseCreate = "EXPENSE_CREATE"
    case contributionRecord = "CONTRIBUTION_RECORD"
    case settlementRecord = "SETTLEMENT_RECORD"
    case participantManage = "PARTICIPANT_MANAGE"
    case planningItemCreate = "PLANNING_ITEM_CREATE"
    case bookingCreate = "BOOKING_CREATE"
    case pollCreate = "POLL_CREATE"
    case updateCreate = "UPDATE_CREATE"
    case memoryCreate = "MEMORY_CREATE"
    case purchaseItemCreate = "PURCHASE_ITEM_CREATE"
    case residentManage = "RESIDENT_MANAGE"
}

enum GroupActionDestination: Equatable {
    case expense
    case contribution
    case settlement
    case participants
    case budget
    case planning
    case booking
    case poll
    case memory
    case update
    case invite
    case purchaseItem
    case resident
}

struct GroupActionTile: Identifiable {
    var id: String { "\(code?.rawValue ?? tileId)-\(label)" }
    let code: GroupActionCode?
    let tileId: String
    let label: String
    let subtitle: String
    let icon: String
    let colors: [Color]
    let destination: GroupActionDestination
    let enabledWhenMomentActive: Bool
    let apiGap: Bool
}

enum GroupActionRegistry {
    static func destination(for capabilityCode: String) -> GroupActionDestination? {
        switch capabilityCode.uppercased() {
        case GroupActionCode.expenseCreate.rawValue: return .expense
        case GroupActionCode.contributionRecord.rawValue: return .contribution
        case GroupActionCode.settlementRecord.rawValue: return .settlement
        case GroupActionCode.participantManage.rawValue: return .participants
        case GroupActionCode.planningItemCreate.rawValue: return .planning
        case GroupActionCode.bookingCreate.rawValue: return .booking
        case GroupActionCode.pollCreate.rawValue: return .poll
        case GroupActionCode.updateCreate.rawValue: return .update
        case GroupActionCode.memoryCreate.rawValue: return .memory
        case GroupActionCode.purchaseItemCreate.rawValue: return .purchaseItem
        case GroupActionCode.residentManage.rawValue: return .resident
        default: return nil
        }
    }

    static func figmaHubTiles(
        hasActiveMoment: Bool,
        capabilityCodes: [String]? = nil
    ) -> [GroupActionTile] {
        catalogTiles().map { tile in
            let enabled = hubTileEnabled(hasActiveMoment: hasActiveMoment, capabilityCodes: capabilityCodes, tile: tile)
            return GroupActionTile(
                code: tile.code,
                tileId: tile.tileId,
                label: tile.label,
                subtitle: tile.subtitle,
                icon: tile.icon,
                colors: tile.colors,
                destination: tile.destination,
                enabledWhenMomentActive: enabled,
                apiGap: tile.apiGap
            )
        }
    }

    /// Legacy API for tests.
    static func tiles(hasActiveMoment: Bool, capabilityCodes: [String]? = nil) -> [GroupActionTile] {
        figmaHubTiles(hasActiveMoment: hasActiveMoment, capabilityCodes: capabilityCodes)
    }

    private static func hubTileEnabled(
        hasActiveMoment: Bool,
        capabilityCodes: [String]?,
        tile: GroupActionTile
    ) -> Bool {
        if !hasActiveMoment { return false }
        if tile.apiGap { return false }
        // Empty capabilities must NOT enable everything — fail closed until bootstrap fills V019.
        guard let codes = capabilityCodes, !codes.isEmpty else {
            return tile.code == nil
        }
        guard let code = tile.code?.rawValue else { return true }
        return codes.map { $0.uppercased() }.contains(code.uppercased())
    }

    private static func catalogTiles() -> [GroupActionTile] {
        [
            tile("expense", .expenseCreate, "Expense", "Split a cost", "creditcard.fill", "#33C759", "#0F766E", .expense),
            tile("planning", .planningItemCreate, "Planning", "Itinerary & tasks", "calendar", "#14B8A6", "#0F766E", .planning),
            tile("budget", nil, "Budget", "Edit planned total", "chart.bar.fill", "#FFB598", "#E8621A", .budget),
            tile("booking", .bookingCreate, "Booking", "Reservations", "ticket.fill", "#FF7A3D", "#E85940", .booking),
            tile("poll", .pollCreate, "Poll", "Group decisions", "checklist", "#A855F7", "#7C3AED", .poll),
            tile("memory", .memoryCreate, "Memory", "Capture a moment", "camera.fill", "#FF8E63", "#E8744F", .memory),
            tile("update", .updateCreate, "Update", "Share status", "megaphone.fill", "#3B82F6", "#1D4ED8", .update),
            tile("contribution", .contributionRecord, "Contribution", "Record money in", "hand.raised.fill", "#10B981", "#047857", .contribution),
            tile("invite", .participantManage, "Invite", "Add people", "person.badge.plus", "#FFB598", "#E8621A", .invite),
            tile("settle", .settlementRecord, "Settle", "Pay down balances", "scalemass", "#059669", "#10B981", .settlement),
            tile("purchase", .purchaseItemCreate, "Purchase item", "Track a buy", "cart.fill", "#F59E0B", "#D97706", .purchaseItem),
            tile("resident", .residentManage, "Resident", "Add a housemate", "house.fill", "#6366F1", "#4338CA", .resident),
        ]
    }

    private static func tile(
        _ tileId: String,
        _ code: GroupActionCode?,
        _ label: String,
        _ subtitle: String,
        _ icon: String,
        _ start: String,
        _ end: String,
        _ destination: GroupActionDestination,
        apiGap: Bool = false
    ) -> GroupActionTile {
        GroupActionTile(
            code: code,
            tileId: tileId,
            label: label,
            subtitle: subtitle,
            icon: icon,
            colors: [Color(hex: start), Color(hex: end)],
            destination: destination,
            enabledWhenMomentActive: !apiGap,
            apiGap: apiGap
        )
    }

    static func equalSplitInputs(participantIds: [String]) -> [APIClient.GroupSplitInput] {
        participantIds.map { APIClient.GroupSplitInput(participantId: $0) }
    }

    static func defaultCodes() -> [GroupActionCode] {
        GroupActionCode.allCases
    }
}
