import Foundation
import Testing
@testable import momentra

struct GroupActionRegistryTests {
    @Test func v019CodesAreComplete() {
        let codes = Set(GroupActionCode.allCases.map(\.rawValue))
        #expect(codes.contains("EXPENSE_CREATE"))
        #expect(codes.contains("CONTRIBUTION_RECORD"))
        #expect(codes.contains("SETTLEMENT_RECORD"))
        #expect(codes.contains("PARTICIPANT_MANAGE"))
        #expect(codes.contains("PLANNING_ITEM_CREATE"))
        #expect(codes.contains("BOOKING_CREATE"))
        #expect(codes.contains("POLL_CREATE"))
        #expect(codes.contains("UPDATE_CREATE"))
        #expect(codes.contains("MEMORY_CREATE"))
        #expect(GroupActionCode.allCases.count == 9)
    }

    @Test func destinationMapsV019Codes() {
        #expect(GroupActionRegistry.destination(for: "EXPENSE_CREATE") == .expense)
        #expect(GroupActionRegistry.destination(for: "contribution_record") == .contribution)
        #expect(GroupActionRegistry.destination(for: "SETTLEMENT_RECORD") == .settlement)
        #expect(GroupActionRegistry.destination(for: "PARTICIPANT_MANAGE") == .participants)
        #expect(GroupActionRegistry.destination(for: "UNKNOWN") == nil)
    }

    @Test func collabTilesLiveWithoutApiGap() {
        let tiles = GroupActionRegistry.tiles(hasActiveMoment: true)
        let planning = tiles.first { $0.tileId == "planning" }
        #expect(planning != nil)
        #expect(planning?.apiGap == false)
        #expect(planning?.enabledWhenMomentActive == true)
        #expect(tiles.first { $0.tileId == "booking" }?.apiGap == false)
        #expect(tiles.first { $0.tileId == "poll" }?.apiGap == false)
        #expect(tiles.first { $0.tileId == "update" }?.apiGap == false)
        #expect(tiles.first { $0.tileId == "memory" }?.apiGap == false)
    }

    @Test func settlementTileEnabledWhenMomentActive() {
        let tiles = GroupActionRegistry.tiles(hasActiveMoment: true)
        let settle = tiles.first { $0.code == .settlementRecord }
        #expect(settle != nil)
        #expect(settle?.enabledWhenMomentActive == true)
        #expect(settle?.destination == .settlement)
        #expect(tiles.first { $0.label == "Expense" }?.enabledWhenMomentActive == true)
    }

    @Test func capabilityFilterEnablesMatchingCodesOnly() {
        let tiles = GroupActionRegistry.tiles(
            hasActiveMoment: true,
            capabilityCodes: ["EXPENSE_CREATE", "CONTRIBUTION_RECORD"]
        )
        #expect(tiles.first { $0.code == .expenseCreate }?.enabledWhenMomentActive == true)
        #expect(tiles.first { $0.code == .contributionRecord }?.enabledWhenMomentActive == true)
        #expect(tiles.first { $0.code == .settlementRecord }?.enabledWhenMomentActive == false)
        #expect(tiles.first { $0.code == .participantManage }?.enabledWhenMomentActive == false)
    }

    @Test func equalSplitInputsOmitAmounts() {
        let ids = ["b-id", "a-id"]
        let inputs = GroupActionRegistry.equalSplitInputs(participantIds: ids)
        #expect(inputs.count == 2)
        #expect(inputs.allSatisfy { $0.amount == nil && $0.percent == nil && $0.shares == nil })
        #expect(inputs.map(\.participantId) == ids)
    }

    @Test func defaultCodesMatchV019Set() {
        let codes = Set(GroupActionRegistry.defaultCodes().map(\.rawValue))
        #expect(codes == Set(GroupActionCode.allCases.map(\.rawValue)))
    }
}
