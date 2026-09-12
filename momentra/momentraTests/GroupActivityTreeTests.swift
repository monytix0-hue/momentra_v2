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
                incomeId: nil,
                activityId: nil,
                contributionId: nil,
                amount: amount,
                currencyCode: currency,
                lifestyleContext: nil,
                description: nil,
                merchantName: nil,
                categoryCode: nil,
                subcategoryCode: nil,
                financialAccountId: nil,
                paymentMethodCode: nil,
                participantId: nil,
                status: nil,
                wellbeingRating: nil,
                source: nil
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
