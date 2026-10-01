import Foundation

enum BusinessMomentFilter: String, Equatable {
    case all
    case revenue
    case expense
    case invoice
    case spend
    case issue
    case update
    case decision
    case approval
}

struct BusinessMomentChip: Equatable, Identifiable {
    var id: String { filter.rawValue }
    let filter: BusinessMomentFilter
    let label: String
}

enum BusinessMomentsContent: Equatable {
    case loading
    case error
    case feedEmpty
    case filterEmpty
    case timeline
}

struct BusinessMomentsEntry: Equatable, Identifiable {
    var id: String { card.id + card.occurredAt }
    let card: BusinessMomentCardModel
    let eyebrow: String?
    let timeLabel: String
    let actorLine: String?
}

struct BusinessMomentsGroup: Equatable, Identifiable {
    var id: String { label }
    let label: String
    let entries: [BusinessMomentsEntry]
}

struct BusinessMomentsPresentation: Equatable {
    static let context = "Company activity"
    static let feedEmptyTitle = "Nothing recorded yet"
    static let addLabel = "Add something"

    let familyTitle: String
    let contextLine: String
    let chips: [BusinessMomentChip]
    let selected: BusinessMomentFilter
    let groups: [BusinessMomentsGroup]
    let content: BusinessMomentsContent
    let emptyTitle: String?
    let emptyBody: String?
    let errorMessage: String?
}

struct BusinessMomentsFeed {
    var momentId: String? = nil
    var generation: UInt64 = 0
    var items: [APIClient.ActivityItemPayload] = []
    var nextCursor: String? = nil
    var error: String? = nil
    var loading: Bool = false

    func begin(_ momentId: String?) -> BusinessMomentsFeed {
        var next = self
        let switched = self.momentId != momentId
        next.momentId = momentId
        next.generation += 1
        if switched {
            next.items = []
            next.nextCursor = nil
        }
        if momentId == nil || momentId?.isEmpty == true {
            next.error = "Select a Business Moment."
            next.loading = false
        } else {
            next.error = nil
            next.loading = next.items.isEmpty
        }
        return next
    }

    func applyPage(
        momentId: String,
        generation: UInt64,
        pageItems: [APIClient.ActivityItemPayload],
        cursor: String?,
        append: Bool
    ) -> BusinessMomentsFeed {
        guard momentId == self.momentId, generation == self.generation else { return self }
        var next = self
        next.items = append ? items + pageItems : pageItems
        next.nextCursor = cursor
        next.error = nil
        next.loading = false
        return next
    }

    func applyFailure(momentId: String, generation: UInt64, message: String?) -> BusinessMomentsFeed {
        guard momentId == self.momentId, generation == self.generation else { return self }
        var next = self
        let text = message?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        next.error = text.isEmpty ? "Could not load activity." : text
        next.loading = false
        return next
    }
}

func businessMomentChips(_ family: BusinessMomentFamily) -> [BusinessMomentChip] {
    switch family {
    case .money:
        return [
            BusinessMomentChip(filter: .all, label: "All"),
            BusinessMomentChip(filter: .revenue, label: "Revenue"),
            BusinessMomentChip(filter: .expense, label: "Expenses"),
            BusinessMomentChip(filter: .invoice, label: "Invoices"),
        ]
    case .daily:
        return [
            BusinessMomentChip(filter: .all, label: "All"),
            BusinessMomentChip(filter: .spend, label: "Spend"),
            BusinessMomentChip(filter: .issue, label: "Issues"),
            BusinessMomentChip(filter: .update, label: "Updates"),
            BusinessMomentChip(filter: .approval, label: "Approvals"),
        ]
    case .team:
        return [
            BusinessMomentChip(filter: .all, label: "All"),
            BusinessMomentChip(filter: .update, label: "Updates"),
            BusinessMomentChip(filter: .decision, label: "Decisions"),
            BusinessMomentChip(filter: .approval, label: "Approvals"),
        ]
    }
}

func buildBusinessMomentsPresentation(
    family: BusinessMomentFamily,
    items: [APIClient.ActivityItemPayload],
    filter: BusinessMomentFilter,
    loading: Bool,
    error: String?,
    now: Date,
    timeZone: TimeZone
) -> BusinessMomentsPresentation {
    let chips = businessMomentChips(family)
    let selected = chips.contains { $0.filter == filter } ? filter : .all
    let filtered = items.filter { matchesBusinessMomentFilter(family, selected, $0.activityCode) }
    let groups = groupBusinessMoments(family, filtered.map { businessMomentRecord(from: $0) }, now: now, timeZone: timeZone)
    let failed = !(error?.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ?? true)
    let content: BusinessMomentsContent
    if loading && items.isEmpty {
        content = .loading
    } else if failed && items.isEmpty {
        content = .error
    } else if items.isEmpty {
        content = .feedEmpty
    } else if filtered.isEmpty {
        content = .filterEmpty
    } else {
        content = .timeline
    }
    let chipLabel = chips.first { $0.filter == selected }?.label ?? "All"
    return BusinessMomentsPresentation(
        familyTitle: BusinessMomentFamilyConfig.forFamily(family, smallShop: false).title,
        contextLine: BusinessMomentsPresentation.context,
        chips: chips,
        selected: selected,
        groups: content == .timeline ? groups : [],
        content: content,
        emptyTitle: content == .feedEmpty
            ? BusinessMomentsPresentation.feedEmptyTitle
            : (content == .filterEmpty ? filterEmptyTitle(chipLabel) : nil),
        emptyBody: content == .feedEmpty ? feedEmptyBody(family) : nil,
        errorMessage: failed ? error : nil
    )
}

func matchesBusinessMomentFilter(
    _ family: BusinessMomentFamily,
    _ filter: BusinessMomentFilter,
    _ activityCode: String
) -> Bool {
    if filter == .all { return true }
    let code = activityCode.uppercased()
    switch filter {
    case .revenue: return code == "BUSINESS_REVENUE"
    case .expense: return family == .money && code == "BUSINESS_EXPENSE"
    case .spend: return family == .daily && code == "BUSINESS_EXPENSE"
    case .invoice: return code == "BUSINESS_INVOICE"
    case .issue: return code == "ISSUE_REPORTED"
    case .update: return code == "BUSINESS_UPDATE" || code == "IMPROVEMENT_LOGGED" || code == "ACTIVITY_LOGGED"
    case .decision: return code == "DECISION_RECORDED"
    case .approval: return code == "APPROVAL_REQUESTED" || code == "APPROVAL_APPROVED" || code == "APPROVAL_REJECTED"
    case .all: return true
    }
}

func businessMomentRecord(from item: APIClient.ActivityItemPayload) -> BusinessMomentCardModel {
    let payload = item.activityPayload
    let sourceIds = [payload?.expenseId, payload?.revenueId, payload?.invoiceId, payload?.issueId, payload?.updateId, payload?.approvalRequestId, payload?.activityId].compactMap { $0 }
    let code = item.activityCode.uppercased()
    let rawAmount = code == "BUSINESS_INVOICE" ? payload?.totalAmount : payload?.amount
    let subtitle = code == "BUSINESS_EXPENSE" ? prettyToken(payload?.categoryCode) : nil
    let actor = item.actorDisplayName?.trimmingCharacters(in: .whitespacesAndNewlines)
    let title = item.title.trimmingCharacters(in: .whitespacesAndNewlines)
    return BusinessMomentCardModel(
        id: sourceIds.first ?? "\(item.activityCode)-\(item.occurredAt)-\(item.title)",
        title: title.isEmpty ? fallbackTitle(item.activityCode) : title,
        occurredAt: item.occurredAt,
        amountLabel: formatMomentAmount(rawAmount, payload?.currencyCode),
        activityCode: item.activityCode,
        sourceActivityIds: sourceIds,
        actorDisplayName: (actor?.isEmpty == false) ? actor : nil,
        subtitle: subtitle,
        statusLabel: prettyToken(payload?.status) ?? prettyToken(payload?.severity)
    )
}

func formatMomentAmount(_ raw: String?, _ currency: String?) -> String? {
    let text = raw?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
    if text.isEmpty { return nil }
    let shown: String
    if let number = Decimal(string: text) {
        let format = NumberFormatter()
        format.numberStyle = .decimal
        format.locale = Locale(identifier: "en_US_POSIX")
        format.maximumFractionDigits = 2
        format.minimumFractionDigits = 0
        shown = format.string(from: number as NSDecimalNumber) ?? text
    } else {
        shown = text
    }
    let code = currency?.trimmingCharacters(in: .whitespacesAndNewlines).uppercased() ?? ""
    if code.isEmpty { return shown }
    if code == "INR" { return "₹" + shown }
    if code == "USD" { return "$" + shown }
    return code + " " + shown
}

private func feedEmptyBody(_ family: BusinessMomentFamily) -> String {
    switch family {
    case .money: return "Revenue, expenses and other money activity will appear here."
    case .daily: return "Spend, vendors, issues and updates will appear here."
    case .team: return "Team updates, decisions and approvals will appear here."
    }
}

private func filterEmptyTitle(_ chipLabel: String) -> String {
    let word: String
    switch chipLabel {
    case "Expenses": word = "expenses"
    case "Invoices": word = "invoices"
    case "Approvals": word = "approvals"
    case "Updates": word = "updates"
    case "Decisions": word = "decisions"
    case "Issues": word = "issues"
    default: word = chipLabel.lowercased()
    }
    return "No \(word) yet"
}

private func prettyToken(_ raw: String?) -> String? {
    let text = raw?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
    if text.isEmpty { return nil }
    return text.split(separator: "_").map { word in
        let lower = word.lowercased()
        return lower.prefix(1).uppercased() + lower.dropFirst()
    }.joined(separator: " ")
}

private func fallbackTitle(_ activityCode: String) -> String {
    switch activityCode.uppercased() {
    case "BUSINESS_REVENUE": return "Revenue recorded"
    case "BUSINESS_EXPENSE": return "Expense"
    case "BUSINESS_INVOICE": return "Invoice created"
    case "ISSUE_REPORTED": return "Issue reported"
    case "BUSINESS_UPDATE", "ACTIVITY_LOGGED": return "Update"
    case "IMPROVEMENT_LOGGED": return "Improvement logged"
    case "DECISION_RECORDED": return "Decision recorded"
    case "APPROVAL_REQUESTED": return "Approval requested"
    case "APPROVAL_APPROVED": return "Approval completed"
    case "APPROVAL_REJECTED": return "Approval rejected"
    default: return "Activity"
    }
}

private func momentEyebrow(_ family: BusinessMomentFamily, _ activityCode: String) -> String? {
    switch activityCode.uppercased() {
    case "BUSINESS_REVENUE": return "Revenue"
    case "BUSINESS_EXPENSE": return family == .daily ? "Spend" : "Expense"
    case "BUSINESS_INVOICE": return "Invoice"
    case "ISSUE_REPORTED": return "Issue"
    case "BUSINESS_UPDATE", "IMPROVEMENT_LOGGED", "ACTIVITY_LOGGED": return "Update"
    case "DECISION_RECORDED": return "Decision"
    case "APPROVAL_REQUESTED", "APPROVAL_APPROVED", "APPROVAL_REJECTED": return "Approval"
    default: return nil
    }
}

private func groupBusinessMoments(
    _ family: BusinessMomentFamily,
    _ cards: [BusinessMomentCardModel],
    now: Date,
    timeZone: TimeZone
) -> [BusinessMomentsGroup] {
    var calendar = Calendar(identifier: .gregorian)
    calendar.timeZone = timeZone
    let timeFormat = DateFormatter()
    timeFormat.locale = Locale(identifier: "en_US_POSIX")
    timeFormat.timeZone = timeZone
    timeFormat.dateFormat = "h:mm a"
    let dayFormat = DateFormatter()
    dayFormat.locale = Locale(identifier: "en_US_POSIX")
    dayFormat.timeZone = timeZone
    dayFormat.dateFormat = "d MMM"
    var groups: [BusinessMomentsGroup] = []
    var currentLabel: String?
    var current: [BusinessMomentsEntry] = []
    func flush() {
        guard let label = currentLabel, !current.isEmpty else { return }
        groups.append(BusinessMomentsGroup(label: label, entries: current))
        current = []
    }
    for card in cards {
        let parsed = parseMomentInstant(card.occurredAt)
        let label: String
        if let parsed {
            let day = calendar.startOfDay(for: parsed)
            let today = calendar.startOfDay(for: now)
            if day == today {
                label = "TODAY"
            } else if let yesterday = calendar.date(byAdding: .day, value: -1, to: today), day == yesterday {
                label = "YESTERDAY"
            } else {
                label = dayFormat.string(from: parsed).uppercased()
            }
        } else {
            label = "Earlier"
        }
        if label != currentLabel {
            flush()
            currentLabel = label
        }
        current.append(BusinessMomentsEntry(
            card: card,
            eyebrow: momentEyebrow(family, card.activityCode),
            timeLabel: parsed.map { timeFormat.string(from: $0) } ?? card.occurredAt,
            actorLine: card.actorDisplayName.map { "by \($0)" }
        ))
    }
    flush()
    return groups
}

private func parseMomentInstant(_ raw: String) -> Date? {
    let withFraction = ISO8601DateFormatter()
    withFraction.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
    if let date = withFraction.date(from: raw) { return date }
    let plain = ISO8601DateFormatter()
    plain.formatOptions = [.withInternetDateTime]
    return plain.date(from: raw)
}
