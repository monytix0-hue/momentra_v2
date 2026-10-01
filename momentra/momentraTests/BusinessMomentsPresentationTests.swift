import Foundation
import Testing
@testable import momentra

struct BusinessMomentsPresentationTests {
    private let zone = TimeZone(identifier: "Asia/Kolkata")!
    private var now: Date { ISO8601DateFormatter().date(from: "2026-09-30T12:00:00Z")! }

    @Test func moneyMapsRevenueExpenseAndInvoice() {
        let model = present(.money, [
            item("BUSINESS_REVENUE", "Till sale", "2026-09-30T05:12:00Z", amount: "12500.0000", currency: "INR", revenueId: "rev-1", status: "POSTED", actor: "Santosh"),
            item("BUSINESS_EXPENSE", "Shop expense", "2026-09-30T04:00:00Z", amount: "3250.0000", currency: "INR", expenseId: "exp-1", category: "RENT", actor: "  "),
            item("BUSINESS_INVOICE", "Invoice INV-9", "2026-09-29T03:48:00Z", total: "18000.0000", invoiceId: "inv-1", merchant: "ABC Traders"),
        ])
        let cards = model.groups.flatMap { $0.entries }.map { $0.card }
        #expect(cards[0].amountLabel == "₹12,500")
        #expect(model.groups[0].entries[0].actorLine == "by Santosh")
        #expect(cards[1].amountLabel == "₹3,250")
        #expect(cards[1].subtitle == "Rent")
        #expect(cards[1].actorDisplayName == nil)
        #expect(cards[2].amountLabel == "18,000")
        #expect(cards[2].subtitle == nil)
        #expect(!cards[2].title.contains("ABC"))
        #expect(!model.chips.contains { $0.label.contains("Vendor") || $0.label.contains("Khata") })
    }

    @Test func dailyMapsIssueAndUpdateAndDoesNotInventVendor() {
        let model = present(.daily, [
            item("ISSUE_REPORTED", "Delivery delayed", "2026-09-30T05:12:00Z", issueId: "iss-1", severity: "HIGH", actor: "Priya"),
            item("BUSINESS_UPDATE", "Shift note", "2026-09-30T04:00:00Z", updateId: "upd-1"),
            item("VENDOR_UPDATED", "", "2026-09-30T03:00:00Z", merchant: "ABC Packaging"),
        ])
        let entries = model.groups.flatMap { $0.entries }
        #expect(entries[0].eyebrow == "Issue")
        #expect(entries[0].card.statusLabel == "High")
        #expect(entries[0].card.amountLabel == nil)
        #expect(entries[0].actorLine == "by Priya")
        #expect(entries[1].eyebrow == "Update")
        #expect(entries[2].card.title == "Activity")
        #expect(entries[2].card.subtitle == nil)
        #expect(!model.chips.contains { $0.label == "Vendors" || $0.label == "Vendor" })
    }

    @Test func teamMapsUpdateDecisionAndApproval() {
        let model = present(.team, [
            item("BUSINESS_UPDATE", "Standup note", "2026-09-30T05:12:00Z", updateId: "u1"),
            item("DECISION_RECORDED", "Launch moved to Friday", "2026-09-30T04:00:00Z", actor: "Santosh"),
            item("APPROVAL_APPROVED", "Purchase request", "2026-09-29T03:00:00Z", approvalId: "a1", actor: "Priya"),
        ])
        let entries = model.groups.flatMap { $0.entries }
        #expect(entries.map { $0.eyebrow } == ["Update", "Decision", "Approval"])
        #expect(entries[1].card.title == "Launch moved to Friday")
        #expect(entries[2].actorLine == "by Priya")
        #expect(!model.chips.contains { $0.label.contains("Milestone") })
    }

    @Test func filtersUseActivityCodes() {
        let items = [
            item("BUSINESS_REVENUE", "Sale", "2026-09-30T05:00:00Z", amount: "1", currency: "INR"),
            item("BUSINESS_EXPENSE", "Rent", "2026-09-30T04:00:00Z", amount: "2", currency: "INR"),
            item("ISSUE_REPORTED", "Delay", "2026-09-30T03:00:00Z"),
            item("BUSINESS_UPDATE", "Note", "2026-09-30T02:00:00Z"),
            item("DECISION_RECORDED", "Hold", "2026-09-30T01:00:00Z"),
            item("APPROVAL_REQUESTED", "Buy", "2026-09-29T01:00:00Z"),
        ]
        #expect(present(.money, items).groups.flatMap { $0.entries }.count == 6)
        #expect(present(.money, items, .expense).groups.flatMap { $0.entries }.map { $0.card.title } == ["Rent"])
        #expect(present(.daily, items, .issue).groups.flatMap { $0.entries }.map { $0.card.title } == ["Delay"])
        #expect(present(.team, items, .decision).groups.flatMap { $0.entries }.map { $0.card.title } == ["Hold"])
    }

    @Test func keepsServerOrderIncludingEqualTimestamps() {
        let model = present(.money, [
            item("BUSINESS_REVENUE", "First", "2026-09-30T05:12:00Z"),
            item("BUSINESS_EXPENSE", "Second", "2026-09-30T05:12:00Z"),
        ])
        #expect(model.groups[0].entries.map { $0.card.title } == ["First", "Second"])
    }

    @Test func groupsTodayYesterdayAndOlder() {
        let model = present(.money, [
            item("BUSINESS_REVENUE", "Today row", "2026-09-30T05:12:00Z"),
            item("BUSINESS_EXPENSE", "Yesterday row", "2026-09-29T03:48:00Z"),
            item("BUSINESS_INVOICE", "Older row", "2026-09-28T04:00:00Z", total: "1"),
        ])
        #expect(model.groups.map { $0.label } == ["TODAY", "YESTERDAY", "28 SEP"])
        #expect(model.groups[0].entries[0].timeLabel == "10:42 AM")
    }

    @Test func emptyFilterAndErrorStayDistinct() {
        let empty = present(.money, [])
        #expect(empty.content == .feedEmpty)
        #expect(empty.emptyTitle == "Nothing recorded yet")
        #expect(empty.emptyBody == "Revenue, expenses and other money activity will appear here.")
        #expect(present(.daily, []).emptyBody?.contains("issues") == true)
        let filtered = present(.money, [item("BUSINESS_REVENUE", "Sale", "2026-09-30T05:00:00Z")], .expense)
        #expect(filtered.content == .filterEmpty)
        #expect(filtered.emptyTitle == "No expenses yet")
        #expect(filtered.emptyBody == nil)
        let failed = buildBusinessMomentsPresentation(
            family: .money,
            items: [],
            filter: .all,
            loading: false,
            error: "Network",
            now: now,
            timeZone: zone
        )
        #expect(failed.content == .error)
        #expect(failed.errorMessage == "Network")
        #expect(failed.emptyTitle == nil)
    }

    @Test func switchingClearsThePreviousMomentAndDropsStalePages() {
        let money = item("BUSINESS_REVENUE", "Money row", "2026-09-30T05:00:00Z")
        let daily = item("ISSUE_REPORTED", "Daily row", "2026-09-30T05:00:00Z")
        var feed = BusinessMomentsFeed()
        feed = feed.begin("money")
        let moneyGen = feed.generation
        feed = feed.applyPage(momentId: "money", generation: moneyGen, pageItems: [money], cursor: nil, append: false)
        #expect(present(.money, feed.items).familyTitle == "Money & Cash Flow")
        feed = feed.begin("daily")
        #expect(feed.items.isEmpty)
        #expect(present(.daily, feed.items).familyTitle == "Daily Business")
        #expect(present(.daily, feed.items).chips.map { $0.label } == ["All", "Spend", "Issues", "Updates", "Approvals"])
        feed = feed.applyPage(momentId: "money", generation: moneyGen, pageItems: [money], cursor: nil, append: false)
        #expect(feed.items.isEmpty)
        feed = feed.applyPage(momentId: "daily", generation: feed.generation, pageItems: [daily], cursor: nil, append: false)
        #expect(feed.items.map { $0.title } == ["Daily row"])
        feed = feed.begin("company-b")
        #expect(feed.items.isEmpty)
        feed = feed.applyPage(momentId: "money", generation: moneyGen, pageItems: [money], cursor: nil, append: false)
        #expect(feed.items.isEmpty)
    }

    @Test func otherMemberAppearsWhenTheRefreshedFeedIncludesThem() {
        #expect(present(.money, []).content == .feedEmpty)
        let after = present(.money, [
            item("BUSINESS_EXPENSE", "Member expense", "2026-09-30T05:12:00Z", amount: "10", currency: "INR", expenseId: "exp-b", actor: "Member B"),
        ])
        let entry = after.groups[0].entries[0]
        #expect(entry.card.title == "Member expense")
        #expect(entry.actorLine == "by Member B")
    }

    private func present(
        _ family: BusinessMomentFamily,
        _ items: [APIClient.ActivityItemPayload],
        _ filter: BusinessMomentFilter = .all
    ) -> BusinessMomentsPresentation {
        buildBusinessMomentsPresentation(
            family: family,
            items: items,
            filter: filter,
            loading: false,
            error: nil,
            now: now,
            timeZone: zone
        )
    }

    private func item(
        _ code: String,
        _ title: String,
        _ occurredAt: String,
        amount: String? = nil,
        currency: String? = nil,
        expenseId: String? = nil,
        revenueId: String? = nil,
        invoiceId: String? = nil,
        total: String? = nil,
        issueId: String? = nil,
        severity: String? = nil,
        updateId: String? = nil,
        approvalId: String? = nil,
        category: String? = nil,
        status: String? = nil,
        merchant: String? = nil,
        actor: String? = nil
    ) -> APIClient.ActivityItemPayload {
        APIClient.ActivityItemPayload(
            activityCode: code,
            title: title,
            occurredAt: occurredAt,
            activityPayload: APIClient.ActivityItemPayload.ActivityPayload(
                expenseId: expenseId,
                categoryCode: category,
                merchantName: merchant,
                status: status,
                revenueId: revenueId,
                invoiceId: invoiceId,
                totalAmount: total,
                issueId: issueId,
                severity: severity,
                updateId: updateId,
                approvalRequestId: approvalId,
                amount: amount,
                currencyCode: currency
            ),
            actorDisplayName: actor
        )
    }
}
