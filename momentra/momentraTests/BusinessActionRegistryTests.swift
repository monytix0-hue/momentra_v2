import Foundation
import Testing
@testable import momentra

struct BusinessActionRegistryTests {
    @Test func emptyCapabilitiesFailOpen() {
        #expect(BusinessActionRegistry.isDestinationEnabled([], destination: .spend))
        #expect(BusinessActionRegistry.isDestinationEnabled(nil, destination: .revenue))
        #expect(BusinessActionRegistry.isKindEnabled(.revenue, capabilities: [], momentTypeCode: "BUSINESS_RUNWAY"))
        #expect(!BusinessActionRegistry.isKindEnabled(.revenue, capabilities: [], momentTypeCode: "TEAM_OPERATIONS"))
    }

    @Test func growingMoneyPrimary() {
        let spec = BusinessMomentFamilyConfig.forFamily(.money, smallShop: false)
        #expect(spec.primary == [.revenue, .expense, .invoice])
    }

    @Test func shopMoneyPrimary() {
        let spec = BusinessMomentFamilyConfig.forFamily(.money, smallShop: true)
        #expect(spec.primary == [.khata, .revenue, .expense])
        #expect(BusinessQuickAddKind.expense.label(smallShop: true) == "Shop expense")
    }

    @Test func dailyPrimary() {
        let growing = BusinessMomentFamilyConfig.forFamily(.daily, smallShop: false)
        let shop = BusinessMomentFamilyConfig.forFamily(.daily, smallShop: true)
        #expect(growing.primary == [.spendEntry, .updateVendor, .reportIssue])
        #expect(shop.primary == growing.primary)
        #expect(!shop.secondary.contains(.slaCheck))
    }

    @Test func teamPrimaryAndShopOmitsDecision() {
        let growing = BusinessMomentFamilyConfig.forFamily(.team, smallShop: false)
        #expect(growing.primary == [.teamUpdate, .approval, .decision])
        let shop = BusinessMomentFamilyConfig.forFamily(.team, smallShop: true)
        #expect(!shop.primary.contains(.decision))
        #expect(!shop.secondary.contains(.decision))
    }

    @Test func capabilityFilterDoesNotBackfill() {
        let spec = BusinessMomentFamilyConfig.forFamily(.money, smallShop: false)
        let visible = BusinessMomentFamilyConfig.visibleActions(
            spec.primary,
            capabilities: ["EXPENSE_CREATE", "REVENUE_RECORD"],
            momentTypeCode: "BUSINESS_RUNWAY"
        )
        #expect(visible == [.revenue, .expense])
        #expect(!visible.contains(.taxEntry))
    }

    @Test func eachFamilyQuickAddPrimaryDiffers() {
        #expect(BusinessMomentFamilyConfig.quickAddPrimary("BUSINESS_RUNWAY", smallShop: false, capabilities: []) == [.revenue, .expense, .invoice])
        #expect(BusinessMomentFamilyConfig.quickAddPrimary("BUSINESS_RUNWAY", smallShop: true, capabilities: []) == [.khata, .revenue, .expense])
        #expect(BusinessMomentFamilyConfig.quickAddPrimary("BUSINESS_OPERATIONS", smallShop: false, capabilities: []) == [.spendEntry, .updateVendor, .reportIssue])
        #expect(BusinessMomentFamilyConfig.quickAddPrimary("TEAM_OPERATIONS", smallShop: false, capabilities: []) == [.teamUpdate, .approval, .decision])
        #expect(BusinessMomentFamilyConfig.quickAddPrimary("TEAM_OPERATIONS", smallShop: true, capabilities: []) == [.khata, .teamUpdate, .approval])
        #expect(!BusinessMomentFamilyConfig.quickAddPrimary("BUSINESS_OPERATIONS", smallShop: false, capabilities: []).contains(.revenue))
        #expect(BusinessMomentFamilyConfig.quickAddPrimary(nil, smallShop: false, capabilities: []).isEmpty)
    }

    @Test func switchingMomentsChangesQuickAddWithoutRestart() {
        let moments = [
            MomentSummary(momentId: "money", title: "Money & Cash Flow", status: "ACTIVE", momentTypeCode: "BUSINESS_RUNWAY"),
            MomentSummary(momentId: "daily", title: "Daily Business", status: "ACTIVE", momentTypeCode: "BUSINESS_OPERATIONS"),
            MomentSummary(momentId: "team", title: "Team & Work", status: "ACTIVE", momentTypeCode: "TEAM_OPERATIONS"),
            MomentSummary(momentId: "blank", title: "Untitled", status: "ACTIVE", momentTypeCode: nil),
        ]
        func primary(_ id: String) -> [BusinessQuickAddKind] {
            BusinessMomentFamilyConfig.quickAddPrimary(
                BusinessMomentFamilyConfig.selectedQuickAddTypeCode(
                    selectedMomentId: id,
                    moments: moments,
                    selectedMomentTypeCode: "TEAM_OPERATIONS"
                ),
                smallShop: false,
                capabilities: []
            )
        }
        let money = primary("money")
        #expect(money == [.revenue, .expense, .invoice])
        #expect(primary("daily") == [.spendEntry, .updateVendor, .reportIssue])
        #expect(primary("team") == [.teamUpdate, .approval, .decision])
        #expect(primary("money") == money)
        #expect(BusinessMomentFamilyConfig.selectedQuickAddTypeCode(
            selectedMomentId: "blank",
            moments: moments,
            selectedMomentTypeCode: "TEAM_OPERATIONS"
        ) == nil)
        #expect(primary("blank").isEmpty)
    }

}
