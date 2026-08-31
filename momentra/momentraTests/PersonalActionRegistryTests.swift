import Foundation
import Testing
@testable import momentra

struct PersonalActionRegistryTests {
    @Test func v019CodesAreComplete() {
        let codes = Set(PersonalActionCode.allCases.map(\.rawValue))
        #expect(codes.contains("EXPENSE_CREATE"))
        #expect(codes.contains("LIFE_OBSERVATION_RECORD"))
        #expect(codes.contains("GOAL_CREATE"))
        #expect(codes.contains("MILESTONE_CREATE"))
        #expect(codes.contains("PROGRESS_RECORD"))
        #expect(codes.contains("OPPORTUNITY_CREATE"))
        #expect(codes.contains("PIVOT_RECORD"))
        #expect(codes.contains("LEARNING_ACTIVITY_CREATE"))
        #expect(codes.contains("LIFESTYLE_ACTIVITY_CREATE"))
        #expect(codes.contains("RELATIONSHIP_ACTIVITY_RECORD"))
        #expect(codes.contains("MOVEMENT_RECORD"))
        #expect(PersonalActionCode.allCases.count == 11)
    }

    @Test func relationshipsTilesUseRelationshipActivityCode() {
        let tiles = PersonalActionRegistry.tiles(for: .relationships, hasActiveMoment: true)
        #expect(!tiles.isEmpty)
        #expect(tiles.allSatisfy { $0.code == .relationshipActivityRecord })
        #expect(tiles.map(\.label).contains("Connection"))
        #expect(tiles.map(\.label).contains("Support"))
    }

    @Test func lifeOpsMoneyTilesEnabledWithActiveMoment() {
        let caps = ["EXPENSE_CREATE", "MOVEMENT_RECORD", "LIFE_OBSERVATION_RECORD"]
        let tiles = PersonalActionRegistry.tiles(
            for: .lifeOperations,
            hasActiveMoment: true,
            capabilityCodes: caps
        )
        let transfer = tiles.first { $0.label == "Transfer" }
        let savings = tiles.first { $0.label == "Savings" }
        #expect(transfer?.code == .movementRecord)
        #expect(transfer?.enabledWhenMomentActive == true)
        #expect(savings?.enabledWhenMomentActive == true)
        #expect(tiles.first { $0.label == "Income" }?.enabledWhenMomentActive == true)
    }

    @Test func emptyCapabilitiesFailClosed() {
        let tiles = PersonalActionRegistry.tiles(
            for: .lifeOperations,
            hasActiveMoment: true,
            capabilityCodes: []
        )
        #expect(tiles.isEmpty)
        #expect(!PersonalActionRegistry.isDestinationEnabled([], destination: .expense))
        #expect(!PersonalActionRegistry.isDestinationEnabled(nil, destination: .expense))
    }

    @Test func capabilityFilterKeepsMatchingCodesOnly() {
        let tiles = PersonalActionRegistry.tiles(
            for: .futureBuilding,
            hasActiveMoment: true,
            capabilityCodes: ["EXPENSE_CREATE", "MILESTONE_CREATE"]
        )
        #expect(tiles.map(\.label) == ["Milestone"])
    }

    @Test func futureFamilyDefaultCodesIncludeGoalAdjacentCaps() {
        let codes = Set(PersonalActionRegistry.defaultCodes(for: .futureBuilding).map(\.rawValue))
        #expect(codes.contains("MILESTONE_CREATE"))
        #expect(codes.contains("PROGRESS_RECORD"))
        #expect(codes.contains("LEARNING_ACTIVITY_CREATE"))
        // GOAL_CREATE is in V019 set; hub surfaces milestone/progress/etc for Future Building.
        #expect(PersonalActionCode.goalCreate.rawValue == "GOAL_CREATE")
    }
}
