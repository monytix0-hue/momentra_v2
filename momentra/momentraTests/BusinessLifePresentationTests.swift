import Foundation
import Testing
@testable import momentra

struct BusinessLifePresentationTests {
    private let zone = TimeZone(identifier: "Asia/Kolkata")!
    private var now: Date { ISO8601DateFormatter().date(from: "2026-09-30T12:00:00Z")! }

    @Test func lensesShareCompanyStateAndOnlyTheMovementTabChanges() {
        let state = buildBusinessLifeCompanyState(facts: sample(), now: now, timeZone: zone)
        #expect(state == buildBusinessLifeCompanyState(facts: sample(), now: now, timeZone: zone))
        #expect(movementTab(for: .overview) == .activity)
        #expect(movementTab(for: .daily) == .activity)
        #expect(movementTab(for: .team) == .activity)
        #expect(movementTab(for: .money) == .money)
        #expect(businessLifeLens(momentTypeCode: "BUSINESS_RUNWAY") == .money)
        #expect(businessLifeLens(momentTypeCode: "BUSINESS_OPERATIONS") == .daily)
        #expect(businessLifeLens(momentTypeCode: "TEAM_OPERATIONS") == .team)
        #expect(businessLifeLens(momentTypeCode: nil) == .overview)
        #expect(state.overview[0].state == "Needs attention")
        #expect(state.overview[1].state == "Watch")
        #expect(state.overview[2].state == "Not set up")
    }

    @Test func thisWeekCountsOnlyTheRecentWindow() {
        let state = buildBusinessLifeCompanyState(facts: sample(), now: now, timeZone: zone)
        #expect(state.thisWeek == "1 recent company event is from this week.")
        #expect(state.activity.count == 2)
    }

    @Test func zeroCashStaysAndMissingRevenueIsOmitted() {
        var facts = sample()
        facts.runway = ["availableCash": "0", "monthlyRevenue": ""]
        facts.runwayStatus = nil
        facts.runwayMonths = nil
        facts.signals = []
        facts.sections["runway"] = "REAL_DATA"
        let state = buildBusinessLifeCompanyState(facts: facts, now: now, timeZone: zone)
        #expect(state.financialPosition == [BusinessLifeValueLine(label: "Available cash", value: "0")])
        #expect(!state.financialPosition.contains { $0.label == "Revenue" })
        #expect(businessLifeFinanceScope == "Company totals")
        #expect(state.needsAttention.isEmpty)
        #expect(state.overview[0].state == "Recorded")
    }

    @Test func attentionAndWorkingStayCompanyWide() {
        let state = buildBusinessLifeCompanyState(facts: sample(), now: now, timeZone: zone)
        #expect(state.needsAttention == ["Cash is tight", "Spend is high"])
        #expect(state.working == ["Invoice collected"])
    }

    private func sample() -> BusinessLifeFacts {
        BusinessLifeFacts(
            sections: [
                "runway": "REAL_DATA",
                "businessOperations": "REAL_DATA",
                "teamOperations": "EMPTY_SUPPORTED",
            ],
            runway: [
                "statusLabel": "Scaling",
                "availableCash": "50000",
                "revenueTotal": "8000",
                "expenseTotal": "10000",
            ],
            daily: ["statusLabel": "Daily Business", "monthlyBudget": "80000"],
            team: [:],
            runwayStatus: "Scaling",
            dailyStatus: "Daily Business",
            runwayMonths: "5.0",
            signals: [
                BusinessLifeSignalFact(title: "Cash is tight", family: "RUNWAY", statusLabel: "Action"),
                BusinessLifeSignalFact(title: "Spend is high", family: "OPERATIONS", statusLabel: "Watch"),
                BusinessLifeSignalFact(title: "Invoice collected", family: "RUNWAY", statusLabel: "Healthy"),
            ],
            activity: [
                BusinessLifeActivityFact(title: "Till sale", occurredAt: "2026-09-30T05:12:00Z", activityCode: "BUSINESS_REVENUE"),
                BusinessLifeActivityFact(title: "Old expense", occurredAt: "2026-09-22T05:12:00Z", activityCode: "BUSINESS_EXPENSE"),
            ]
        )
    }
}
