import Foundation
import Testing
@testable import momentra

struct ShellInvariantTests {
    @Test func healsUnsupportedContext() {
        let result = ShellStateInvariants.heal(
            ShellInvariantInput(
                supportedContexts: [.personal, .group],
                selectedContext: .business,
                selectedCompanyId: "c1",
                companies: [],
                moments: [],
                selectedMomentId: nil,
                selectedTabByContext: [:]
            )
        )
        #expect(result.selectedContext == .personal)
        #expect(result.selectedCompanyId == nil)
        #expect(result.healed)
    }

    @Test func businessScopesMomentsToCompany() {
        let result = ShellStateInvariants.heal(
            ShellInvariantInput(
                supportedContexts: [.personal, .business],
                selectedContext: .business,
                selectedCompanyId: "c1",
                companies: [
                    CompanySummary(companyId: "c1", displayName: "A"),
                    CompanySummary(companyId: "c2", displayName: "B"),
                ],
                moments: [
                    MomentSummary(momentId: "m1", title: "A1", status: "ACTIVE", companyId: "c1"),
                    MomentSummary(momentId: "m2", title: "B1", status: "ACTIVE", companyId: "c2"),
                ],
                selectedMomentId: "m2",
                selectedTabByContext: [.business: .memory]
            )
        )
        #expect(result.selectedCompanyId == "c1")
        #expect(result.moments.map(\.momentId) == ["m1"])
        #expect(result.selectedMomentId == "m1")
        #expect(result.selectedTabByContext[.business] == .memory)
    }

    @Test func momentThemeUsesPrimaryNotContextAccent() {
        let life = MomentThemes.personal("LIFE_OPERATIONS")
        let ctx = ContextTheme.of(.personal)
        #expect(life.primary != ctx.contextAccent || life.type == "LIFE_OPERATIONS")
        let future = MomentThemes.personal("FUTURE_BUILDING")
        #expect(future.type == "FUTURE_BUILDING")
        #expect(PersonalPulseFamily.forTypeCode("FUTURE_BUILDING").theme.heroTitle == "FUTURE SCORE")
    }

    @Test func personalPulseFamilyMapsV018SubtypeCodes() {
        #expect(PersonalPulseFamily.forTypeCode("FUTURE_MILESTONE") == .futureBuilding)
        #expect(PersonalPulseFamily.forTypeCode("LIFESTYLE_EXPERIENCE") == .lifestyle)
        #expect(PersonalPulseFamily.forTypeCode("RELATIONSHIP_SUPPORT") == .relationships)
        #expect(PersonalPulseFamily.forTypeCode("LIFE_RECOVERY") == .lifeOperations)
    }

    @Test func personalActionRegistryUsesDestinationLevelGating() {
        let tiles = PersonalActionRegistry.tiles(
            for: .futureBuilding,
            hasActiveMoment: true,
            capabilityCodes: ["MILESTONE_CREATE"]
        )
        #expect(tiles.contains { $0.label == "Milestone" })
        #expect(!tiles.contains { $0.label == "Opportunity" })
    }

    @Test func businessRevenueInvoiceGatedToRunwayMomentType() {
        let caps = [BusinessActionCode.revenueRecord.rawValue, BusinessActionCode.invoiceCreate.rawValue]
        #expect(!BusinessActionRegistry.isKindEnabled(.revenue, capabilities: caps, momentTypeCode: "TEAM_OPERATIONS"))
        #expect(!BusinessActionRegistry.isKindEnabled(.invoice, capabilities: caps, momentTypeCode: "BUSINESS_OPERATIONS"))
        #expect(BusinessActionRegistry.isKindEnabled(.revenue, capabilities: caps, momentTypeCode: "BUSINESS_RUNWAY"))
        #expect(BusinessActionRegistry.isKindEnabled(.invoice, capabilities: caps, momentTypeCode: "BUSINESS_RUNWAY"))
    }
}
