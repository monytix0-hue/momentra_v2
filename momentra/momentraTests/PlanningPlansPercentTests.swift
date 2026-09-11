import XCTest
@testable import momentra

final class PlanningPlansPercentTests: XCTestCase {
    private func item(status: String?, category: String?) -> GroupPlanningItem {
        GroupPlanningItem(
            planningItemId: UUID().uuidString,
            title: "Item",
            dueAt: nil,
            status: status,
            createdAt: nil,
            categoryCode: category,
            location: nil,
            priorityCode: nil,
            description: nil
        )
    }

    func testEmptyIsZero() {
        XCTAssertEqual(planningPlansPercent([]), 0)
    }

    func testChecklistPreferredOverItinerary() {
        let items = [
            item(status: "OPEN", category: "ITINERARY"),
            item(status: "OPEN", category: "SCHEDULE"),
            item(status: "DONE", category: "CLOTHING"),
            item(status: "OPEN", category: "CLOTHING"),
        ]
        // Checklist only: 1/2 = 50% (itinerary ignored when checklist exists)
        XCTAssertEqual(planningPlansPercent(items), 50)
    }

    func testItineraryFallbackWhenNoChecklist() {
        let items = [
            item(status: "DONE", category: "ITINERARY"),
            item(status: "OPEN", category: "SCHEDULE"),
            item(status: "DRAFT", category: "SCHEDULE"),
            item(status: "CANCELLED", category: "SCHEDULE"),
        ]
        // DRAFT + CANCELLED excluded → 1/2 = 50%
        XCTAssertEqual(planningPlansPercent(items), 50)
    }

    func testCompletedAndClosedCountAsDone() {
        let items = [
            item(status: "COMPLETED", category: "TRAVEL_ESSENTIALS"),
            item(status: "CLOSED", category: "DOCUMENTS_MONEY"),
            item(status: "OPEN", category: "MEDICINES_HEALTH"),
        ]
        // 2/3 ≈ 67%
        XCTAssertEqual(planningPlansPercent(items), 67)
    }
}
