import Foundation

enum PulseMetric: Equatable {
    case available(String)
    case zero
    case unavailable
}

struct PulseFact: Equatable {
    let label: String
    let metric: PulseMetric
    let companyWide: Bool
}

struct PulseAttentionItem: Equatable, Identifiable {
    let id: String
    let title: String
}

struct BusinessMomentCardModel: Equatable, Identifiable {
    let id: String
    let title: String
    let occurredAt: String
    let amountLabel: String?
    let activityCode: String
    let sourceActivityIds: [String]
    /// Profile name of the member who recorded the row. Nil when the profile has none.
    var actorDisplayName: String? = nil
    /// Supporting payload text, such as an expense category. Nil when the payload has none.
    var subtitle: String? = nil
    /// Payload status or issue severity. Nil when the payload has none.
    var statusLabel: String? = nil
}

struct BusinessPulsePresentation: Equatable {
    static let recentTitle = "Recent activity"
    static let companyTotals = "Company totals"
    static let attentionEmpty = "No open issues right now."

    let familyTitle: String
    let facts: [PulseFact]
    let today: [BusinessQuickAddKind]
    let attention: [PulseAttentionItem]
    let attentionEmptyLine: String?
    let recent: [BusinessMomentCardModel]

    func displayedFacts() -> [PulseFact] {
        Array(facts.filter {
            if case .unavailable = $0.metric { return false }
            return true
        }.prefix(3))
    }

    func showsCompanyTotals() -> Bool {
        displayedFacts().contains { $0.companyWide }
    }
}

struct BusinessPulseFactsInput {
    var family: BusinessMomentFamily
    var smallShop: Bool
    var capabilities: [String]?
    var momentTypeCode: String?
    var financeQuality: String?
    var revenueTotal: String?
    var expenseTotal: String?
    var invoiceOutstandingTotal: String?
    var runwayMonths: String?
    var availableCash: String?
    var lifeFailed: Bool = false
    var openIssueCount: Int?
    var activeVendorCount: Int?
    var monthlySpend: String?
    var slaCompliancePct: Int?
    var sectionQuality: [String: String] = [:]
    var needsAttention: [(id: String, title: String)] = []
    var rosterCount: Int?
    var approvalTitles: [(id: String, title: String)]?
    var issueTitles: [(id: String, title: String)]?
    var activities: [BusinessMomentCardModel] = []
}

func buildBusinessPulsePresentation(_ input: BusinessPulseFactsInput) -> BusinessPulsePresentation {
    let spec = BusinessMomentFamilyConfig.forFamily(input.family, smallShop: input.smallShop)
    let today = BusinessMomentFamilyConfig.visibleActions(
        spec.primary,
        capabilities: input.capabilities,
        momentTypeCode: input.momentTypeCode
    )
    let facts: [PulseFact]
    switch input.family {
    case .money: facts = moneyFacts(input)
    case .daily: facts = dailyFacts(input)
    case .team: facts = teamFacts(input)
    }
    let attention = Array(attentionItems(input).prefix(3))
    let emptyLine = attention.isEmpty && attentionSourceLoaded(input)
        ? BusinessPulsePresentation.attentionEmpty
        : nil
    return BusinessPulsePresentation(
        familyTitle: spec.title,
        facts: facts,
        today: today,
        attention: attention,
        attentionEmptyLine: emptyLine,
        recent: Array(input.activities.prefix(3))
    )
}

private func moneyFacts(_ input: BusinessPulseFactsInput) -> [PulseFact] {
    let known = !input.lifeFailed
    return [
        PulseFact(label: "Revenue", metric: financeAmount(input.financeQuality, input.revenueTotal), companyWide: true),
        PulseFact(label: "Outflow", metric: financeAmount(input.financeQuality, input.expenseTotal), companyWide: true),
        PulseFact(label: "Cash", metric: known ? textMetric(input.availableCash, real: true) : .unavailable, companyWide: true),
        PulseFact(label: "Runway", metric: known ? textMetric(input.runwayMonths, real: true) : .unavailable, companyWide: true),
    ]
}

private func dailyFacts(_ input: BusinessPulseFactsInput) -> [PulseFact] {
    let quality = input.sectionQuality
    return [
        PulseFact(label: "Open issues", metric: countedMetric(quality["needsAttention"], input.openIssueCount), companyWide: false),
        PulseFact(label: "Vendors", metric: countedMetric(quality["activeVendors"], input.activeVendorCount), companyWide: false),
        PulseFact(label: "Spend", metric: textMetric(input.monthlySpend, real: quality["monthlySpend"] == "REAL_DATA"), companyWide: false),
        PulseFact(label: "SLA", metric: countedMetric(quality["slaCompliance"], input.slaCompliancePct), companyWide: false),
    ]
}

private func teamFacts(_ input: BusinessPulseFactsInput) -> [PulseFact] {
    [
        PulseFact(label: "People", metric: countMetric(input.rosterCount), companyWide: false),
        PulseFact(label: "Approvals", metric: countMetric(input.approvalTitles?.count), companyWide: false),
        PulseFact(label: "Your updates", metric: countMetric(input.activities.count), companyWide: false),
    ]
}

func businessMomentCard(from item: APIClient.ActivityItemPayload) -> BusinessMomentCardModel {
    let payloadId = item.activityPayload?.activityId ?? item.activityPayload?.expenseId
    let amount = item.activityPayload?.amount
    let currency = item.activityPayload?.currencyCode
    let amountLabel: String? = {
        guard let amount, !amount.isEmpty else { return nil }
        if let currency, !currency.isEmpty { return "\(currency) \(amount)" }
        return amount
    }()
    let actor = item.actorDisplayName?.trimmingCharacters(in: .whitespacesAndNewlines)
    return BusinessMomentCardModel(
        id: payloadId ?? "\(item.activityCode)-\(item.occurredAt)-\(item.title)",
        title: item.title,
        occurredAt: item.occurredAt,
        amountLabel: amountLabel,
        activityCode: item.activityCode,
        sourceActivityIds: [payloadId].compactMap { $0 },
        actorDisplayName: (actor?.isEmpty == false) ? actor : nil
    )
}

private func attentionItems(_ input: BusinessPulseFactsInput) -> [PulseAttentionItem] {
    switch input.family {
    case .money:
        var rows = (input.approvalTitles ?? []).map { PulseAttentionItem(id: $0.id, title: $0.title) }
        if case .available(let value) = financeAmount(input.financeQuality, input.invoiceOutstandingTotal) {
            rows.append(PulseAttentionItem(id: "invoice-outstanding", title: "Invoices outstanding \(value)"))
        }
        return rows
    case .daily:
        if input.sectionQuality["needsAttention"] == "REAL_DATA" {
            return input.needsAttention.map { PulseAttentionItem(id: $0.id, title: $0.title) }
        }
        return (input.issueTitles ?? []).map { PulseAttentionItem(id: $0.id, title: $0.title) }
    case .team:
        let approvals = (input.approvalTitles ?? []).map { PulseAttentionItem(id: $0.id, title: $0.title) }
        let issues = (input.issueTitles ?? []).map { PulseAttentionItem(id: $0.id, title: $0.title) }
        return approvals + issues
    }
}

private func attentionSourceLoaded(_ input: BusinessPulseFactsInput) -> Bool {
    switch input.family {
    case .money:
        return input.approvalTitles != nil || financeIsReal(input.financeQuality)
    case .daily:
        return input.sectionQuality["needsAttention"] == "REAL_DATA" || input.issueTitles != nil
    case .team:
        return input.approvalTitles != nil || input.issueTitles != nil
    }
}

private func financeIsReal(_ quality: String?) -> Bool {
    let value = (quality ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
    return !value.isEmpty && value != "EMPTY" && value != "EMPTY_SUPPORTED"
}

private func financeAmount(_ quality: String?, _ raw: String?) -> PulseMetric {
    guard financeIsReal(quality) else { return .unavailable }
    return textMetric(raw, real: true)
}

private func countMetric(_ count: Int?) -> PulseMetric {
    guard let count else { return .unavailable }
    return count == 0 ? .zero : .available(String(count))
}

private func countedMetric(_ quality: String?, _ value: Int?) -> PulseMetric {
    guard quality == "REAL_DATA", let value else { return .unavailable }
    return value == 0 ? .zero : .available(String(value))
}

private func textMetric(_ raw: String?, real: Bool) -> PulseMetric {
    guard real else { return .unavailable }
    let text = (raw ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
    guard !text.isEmpty, text.lowercased() != "null" else { return .unavailable }
    let cleaned = text.replacingOccurrences(of: ",", with: "").replacingOccurrences(of: "₹", with: "").trimmingCharacters(in: .whitespaces)
    if let number = Double(cleaned) {
        return number == 0 ? .zero : .available(text)
    }
    return .available(text)
}
