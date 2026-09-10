import XCTest
@testable import momentra

final class PlanningDayKeyTests: XCTestCase {
    private var istCalendar: Calendar {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = TimeZone(identifier: "Asia/Kolkata")!
        return cal
    }

    func testUtcInstantMapsToLocalIstDayNotUtcPrefix() {
        // Local midnight 10 Sep 2026 IST == 2026-09-09T18:30:00.000Z
        let iso = "2026-09-09T18:30:00.000Z"
        let day = planningDayKey(fromDueAt: iso, calendar: istCalendar)
        XCTAssertNotNil(day)

        let comps = istCalendar.dateComponents([.year, .month, .day], from: day!)
        XCTAssertEqual(comps.year, 2026)
        XCTAssertEqual(comps.month, 9)
        XCTAssertEqual(comps.day, 10, "Must use local IST day, not UTC calendar prefix 09")
    }

    func testOffsetInstantMapsToLocalDay() {
        let iso = "2026-09-10T00:00:00+05:30"
        let day = planningDayKey(fromDueAt: iso, calendar: istCalendar)
        XCTAssertNotNil(day)
        let comps = istCalendar.dateComponents([.year, .month, .day], from: day!)
        XCTAssertEqual(comps.day, 10)
        XCTAssertEqual(comps.month, 9)
    }

    func testDateOnlyUsesLocalCalendarDay() {
        let iso = "2026-09-10"
        let day = planningDayKey(fromDueAt: iso, calendar: Calendar.current)
        XCTAssertNotNil(day)
        let comps = Calendar.current.dateComponents([.year, .month, .day], from: day!)
        XCTAssertEqual(comps.year, 2026)
        XCTAssertEqual(comps.month, 9)
        XCTAssertEqual(comps.day, 10)
    }

    func testNilAndUnparseableDueAtYieldNilKey() {
        XCTAssertNil(planningDayKey(fromDueAt: nil))
        XCTAssertNil(planningDayKey(fromDueAt: ""))
        XCTAssertNil(planningDayKey(fromDueAt: "not-a-date"))
        XCTAssertNil(parsePlanningInstant("2026-09-09T18:30:00.000Zjunk"))
    }

    func testDoesNotTreatDatetimePrefixAsLocalDate() {
        // If parse failed and took prefix(10), this would wrongly land on Sep 9 local.
        // Successful parse of the full instant must win.
        let parsed = parsePlanningInstant("2026-09-09T18:30:00.000Z")
        XCTAssertNotNil(parsed)
        var utc = Calendar(identifier: .gregorian)
        utc.timeZone = TimeZone(secondsFromGMT: 0)!
        XCTAssertEqual(utc.component(.day, from: parsed!), 9)
        XCTAssertEqual(istCalendar.component(.day, from: parsed!), 10)
    }

    func testDefaultScheduleDaySkipsEmptyToday() {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = TimeZone(secondsFromGMT: 0)!
        let today = cal.startOfDay(for: Date(timeIntervalSince1970: 1_778_025_600)) // 2026-05-05 UTC
        let tripDay = cal.date(byAdding: .day, value: 3, to: today)!
        let chosen = defaultPlanningScheduleDay(today: today, itemDays: [tripDay], calendar: cal)
        XCTAssertTrue(cal.isDate(chosen, inSameDayAs: tripDay))
    }

    func testDefaultScheduleDayPrefersTodayWhenItHasItems() {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = TimeZone(secondsFromGMT: 0)!
        let today = cal.startOfDay(for: Date(timeIntervalSince1970: 1_778_025_600))
        let later = cal.date(byAdding: .day, value: 2, to: today)!
        let chosen = defaultPlanningScheduleDay(today: today, itemDays: [later, today], calendar: cal)
        XCTAssertTrue(cal.isDate(chosen, inSameDayAs: today))
    }
}
