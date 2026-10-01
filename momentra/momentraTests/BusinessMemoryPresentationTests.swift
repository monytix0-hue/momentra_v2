import Foundation
import Testing
@testable import momentra

struct BusinessMemoryPresentationTests {
    private let zone = TimeZone(identifier: "Asia/Kolkata")!
    private var now: Date { ISO8601DateFormatter().date(from: "2026-09-30T12:00:00Z")! }

    @Test func mapperIgnoresLensAndChipsFilterOnlyWorthRemembering() {
        let items = [
            fact("Cash risk", family: "BUSINESS_RUNWAY", at: "2026-09-28T04:00:00Z", type: "DECISION"),
            fact("Vendor issue", family: "BUSINESS_OPERATIONS", at: "2026-09-30T05:12:00Z", type: "NOTE"),
            fact("Shift plan", family: "TEAM_OPERATIONS", at: "2026-09-30T05:12:00Z", type: "NOTE"),
        ]
        let company = buildBusinessMemoryPresentation(items: items, now: now, zone: zone)
        let again = buildBusinessMemoryPresentation(items: items, now: now, zone: zone)
        let money = filterWorthRemembering(company.worthRemembering, lens: .money)
        let daily = filterWorthRemembering(company.worthRemembering, lens: .daily)
        #expect(money.map(\.title) == ["Cash risk"])
        #expect(daily.map(\.title) == ["Vendor issue"])
        #expect(filterWorthRemembering(company.worthRemembering, lens: .overview).count == 3)
        #expect(company.heroPeriod == again.heroPeriod)
        #expect(company.heroSentence == again.heroSentence)
        #expect(company.pattern == again.pattern)
        #expect(company.worked == again.worked)
        #expect(company.didnt == again.didnt)
        #expect(company.thenNow == again.thenNow)
        #expect(company.pattern?.title == "2 memories mention risk")
        #expect(company.worked == ["Shift plan"])
        #expect(company.didnt.count == 2)
    }

    @Test func delayAndOverdueAreNotRisk() {
        #expect(businessMemoryIsRisk(title: "Shipment delay", body: "overdue follow-up") == false)
        #expect(businessMemoryIsRisk(title: "Payment overdue", body: nil) == false)
        #expect(businessMemoryIsRisk(title: "Margin risk", body: nil) == true)
        #expect(businessMemoryIsRisk(title: "Open issue", body: nil) == true)
        #expect(businessMemoryIsRisk(title: "Safety incident", body: nil) == true)
        let built = buildBusinessMemoryPresentation(
            items: [
                fact("Shipment delay", body: "overdue follow-up"),
                fact("Payment overdue"),
            ],
            now: now,
            zone: zone
        )
        #expect(built.worked == ["Shipment delay", "Payment overdue"])
        #expect(built.didnt.isEmpty)
        #expect(built.pattern == nil)
    }

    @Test func repeatedTypeIsTheOnePatternAndASingleMemoryIsNot() {
        let repeated = buildBusinessMemoryPresentation(
            items: [
                fact("First close", type: "DECISION"),
                fact("Second close", type: "DECISION"),
            ],
            now: now,
            zone: zone
        )
        #expect(repeated.pattern?.title == "2 Decision memories")
        #expect(repeated.pattern?.evidence == ["First close", "Second close"])
        let single = buildBusinessMemoryPresentation(items: [fact("Only one", type: "DECISION")], now: now, zone: zone)
        #expect(single.pattern == nil)
        let tie = buildBusinessMemoryPresentation(
            items: [
                fact("Cash risk", type: "DECISION"),
                fact("Tax risk", type: "DECISION"),
            ],
            now: now,
            zone: zone
        )
        #expect(tie.pattern?.title == "2 memories mention risk")
    }

    @Test func thenNowUsesTwoDifferentLocalDates() {
        let built = buildBusinessMemoryPresentation(
            items: [
                fact("Opened the books", at: "2026-09-28T04:00:00Z"),
                fact("Closed the week", at: "2026-09-30T05:12:00Z"),
            ],
            now: now,
            zone: zone
        )
        #expect(built.heroPeriod == "28 Sep 2026 – 30 Sep 2026")
        #expect(built.thenNow?.thenTitle == "Opened the books")
        #expect(built.thenNow?.thenDate == "28 Sep 2026")
        #expect(built.thenNow?.nowTitle == "Closed the week")
        #expect(built.thenNow?.nowDate == "30 Sep 2026")
    }

    @Test func sameKolkataDayAcrossUtcMidnightOmitsThenNow() {
        let built = buildBusinessMemoryPresentation(
            items: [
                fact("After midnight", at: "2026-09-29T20:30:00Z"),
                fact("Morning", at: "2026-09-30T02:00:00Z"),
            ],
            now: now,
            zone: zone
        )
        #expect(built.heroPeriod == "30 Sep 2026")
        #expect(built.worthRemembering[0].dateLabel == "30 Sep 2026")
        #expect(built.worthRemembering[1].dateLabel == "30 Sep 2026")
        #expect(built.thenNow == nil)
    }

    @Test func emptyCompanyOmitsPatternAndThenNowAndEmptyBodyIsNotALearning() {
        let empty = buildBusinessMemoryPresentation(items: [], now: now, zone: zone)
        #expect(empty.pattern == nil)
        #expect(empty.thenNow == nil)
        #expect(empty.heroSentence == "Nothing saved yet.")
        #expect(empty.heroPeriod == "Company memory")
        let blank = buildBusinessMemoryPresentation(items: [fact("Cash note", body: "")], now: now, zone: zone)
        #expect(blank.pattern == nil)
        #expect(blank.thenNow == nil)
        #expect(blank.heroSentence == "1 saved memories.")
        #expect(blank.worked == ["Cash note"])
        #expect(blank.worthRemembering.first?.title == "Cash note")
        #expect(blank.heroSentence.contains("Cash note") == false)
    }

    private func fact(
        _ title: String,
        body: String? = nil,
        family: String? = nil,
        at: String? = nil,
        type: String? = nil
    ) -> BusinessMemoryItemFact {
        BusinessMemoryItemFact(
            title: title,
            body: body,
            occurredAt: at,
            memoryType: type,
            businessFamily: family
        )
    }
}
