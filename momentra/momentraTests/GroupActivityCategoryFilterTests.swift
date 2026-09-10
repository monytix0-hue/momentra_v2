import XCTest
@testable import momentra

final class GroupActivityCategoryFilterTests: XCTestCase {
    func testTripChipsStartWithAllAndIncludeHubNames() {
        let chips = GroupActivityCategoryFilter.chips(for: "GROUP_TRIP")
        XCTAssertEqual(chips.first?.id, GroupActivityCategoryFilter.allId)
        let ids = chips.map(\.id)
        XCTAssertTrue(ids.contains("expense"))
        XCTAssertTrue(ids.contains("planning"))
        XCTAssertTrue(ids.contains("booking"))
        XCTAssertEqual(chips.first(where: { $0.id == "expense" })?.label, "Expense")
        XCTAssertEqual(chips.first(where: { $0.id == "planning" })?.label, "Planning")
    }

    func testWeddingChipsUseShortHubNames() {
        let chips = GroupActivityCategoryFilter.chips(for: "WEDDING")
        XCTAssertEqual(chips.first?.id, "All")
        XCTAssertTrue(chips.contains(where: { $0.id == "planning" && $0.label == "Planning" }))
        XCTAssertTrue(chips.contains(where: { $0.id == "participant" && $0.label == "Invite" }))
        XCTAssertTrue(chips.contains(where: { $0.id == "settle" }))
    }

    func testExpenseCodeMatchesExpenseNotPlanning() {
        XCTAssertTrue(
            GroupActivityCategoryFilter.matches(activityCode: "EXPENSE_RECORDED", chipId: "expense")
        )
        XCTAssertFalse(
            GroupActivityCategoryFilter.matches(activityCode: "EXPENSE_RECORDED", chipId: "planning")
        )
    }

    func testAllMatchesEverything() {
        XCTAssertTrue(GroupActivityCategoryFilter.matches(activityCode: "POLL_CREATED", chipId: "All"))
        XCTAssertTrue(GroupActivityCategoryFilter.matches(activityCode: "MEMORY_ADDED", chipId: "All"))
        XCTAssertTrue(GroupActivityCategoryFilter.matches(activityCode: "UNKNOWN_CODE", chipId: "All"))
    }

    func testPlanningAndPollMatching() {
        XCTAssertTrue(
            GroupActivityCategoryFilter.matches(activityCode: "PLANNING_ITEM_CREATE", chipId: "planning")
        )
        XCTAssertTrue(GroupActivityCategoryFilter.matches(activityCode: "POLL_CREATED", chipId: "poll"))
        XCTAssertFalse(GroupActivityCategoryFilter.matches(activityCode: "POLL_CREATED", chipId: "memory"))
    }
}
