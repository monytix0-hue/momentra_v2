import XCTest
@testable import momentra

final class GroupActivityTreeTests: XCTestCase {
    func testAmountLabelFormatsInr() {
        let item = activity(
            code: "GROUP_EXPENSE_RECORDED",
            amount: "250.0000",
            currency: "INR",
            expenseId: "e1"
        )
        XCTAssertEqual(GroupActivityPresentation.amountLabel(for: item), "₹250")
    }

    func testAmountLabelParsesCommaSeparated() {
        let item = activity(
            code: "GROUP_EXPENSE_RECORDED",
            amount: "1,234.50",
            currency: "INR",
            expenseId: "e1"
        )
        XCTAssertEqual(GroupActivityPresentation.amountLabel(for: item), "₹1234.50")
    }

    func testNestsVoidedUnderRecorded() {
        let recorded = activity(
            code: "GROUP_EXPENSE_RECORDED",
            title: "Lunch",
            occurredAt: "2026-09-10T10:00:00Z",
            amount: "100.0000",
            expenseId: "e1"
        )
        let voided = activity(
            code: "GROUP_EXPENSE_VOIDED",
            title: "Lunch",
            occurredAt: "2026-09-11T12:00:00Z",
            amount: "100.0000",
            expenseId: "e1",
            actor: "Sam"
        )
        let other = activity(
            code: "GROUP_POLL_CREATED",
            title: "Poll",
            occurredAt: "2026-09-12T09:00:00Z"
        )

        let tree = GroupActivityPresentation.activityTree(from: [voided, other, recorded])
        XCTAssertEqual(tree.count, 2)
        let expenseNode = try XCTUnwrap(tree.first { $0.item.activityPayload?.expenseId == "e1" })
        XCTAssertEqual(expenseNode.item.activityCode, "GROUP_EXPENSE_RECORDED")
        XCTAssertEqual(expenseNode.children.count, 1)
        XCTAssertEqual(expenseNode.children[0].activityCode, "GROUP_EXPENSE_VOIDED")
        XCTAssertTrue(GroupActivityPresentation.nodeHasVoidChild(expenseNode))
        XCTAssertEqual(
            GroupActivityPresentation.rowTitle(for: expenseNode.children[0], isChild: true),
            "Sam deleted this activity"
        )
        XCTAssertFalse(tree.contains { $0.item.activityCode == "GROUP_EXPENSE_VOIDED" })
    }

    func testSortsByLatestChildSoVoidFloatsAboveOlderPeers() {
        let recorded = activity(
            code: "GROUP_EXPENSE_RECORDED",
            title: "Lunch",
            occurredAt: "2026-09-10T10:00:00Z",
            amount: "100.0000",
            expenseId: "e1"
        )
        let voided = activity(
            code: "GROUP_EXPENSE_VOIDED",
            title: "Lunch",
            occurredAt: "2026-09-13T12:00:00Z",
            amount: "100.0000",
            expenseId: "e1",
            actor: "Sam"
        )
        let other = activity(
            code: "GROUP_POLL_CREATED",
            title: "Poll",
            occurredAt: "2026-09-12T09:00:00Z"
        )

        let tree = GroupActivityPresentation.activityTree(from: [recorded, other, voided])
        XCTAssertEqual(tree.first?.item.activityPayload?.expenseId, "e1")
        XCTAssertEqual(tree.first?.children.first?.activityCode, "GROUP_EXPENSE_VOIDED")
    }

    func testOrphanVoidUsesDeletedTitle() {
        let voided = activity(
            code: "GROUP_EXPENSE_VOIDED",
            title: "Lunch",
            occurredAt: "2026-09-11T12:00:00Z",
            amount: "100.0000",
            expenseId: "e1",
            actor: "Sam"
        )
        let tree = GroupActivityPresentation.activityTree(from: [voided])
        XCTAssertEqual(tree.count, 1)
        XCTAssertEqual(tree[0].item.activityCode, "GROUP_EXPENSE_VOIDED")
        XCTAssertEqual(
            GroupActivityPresentation.rowTitle(for: tree[0].item, isChild: false),
            "Sam deleted this activity"
        )
        XCTAssertTrue(GroupActivityPresentation.nodeHasVoidChild(tree[0]))
    }

    private func activity(
        code: String,
        title: String = "Item",
        occurredAt: String = "2026-09-10T10:00:00Z",
        amount: String? = nil,
        currency: String? = "INR",
        expenseId: String? = nil,
        actor: String? = nil
    ) -> APIClient.ActivityItemPayload {
        let payload: APIClient.ActivityItemPayload.ActivityPayload? =
            (amount != nil || expenseId != nil)
            ? APIClient.ActivityItemPayload.ActivityPayload(
                expenseId: expenseId,
                amount: amount,
                currencyCode: currency
            )
            : nil
        return APIClient.ActivityItemPayload(
            activityCode: code,
            title: title,
            occurredAt: occurredAt,
            activityPayload: payload,
            actorDisplayName: actor
        )
    }
}
