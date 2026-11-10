import Combine
import Foundation

/// Owns the projection for one Trading or Distribution moment, plus the company's masters.
///
/// The projection is reloaded on `momentId` change and whenever the shell bumps
/// `businessTabRefreshToken`, so a write made anywhere else shows up here without a manual pull.
@MainActor
final class BusinessOperatingModel: ObservableObject {
    enum Phase: Equatable {
        case loading
        case ready
        case empty
        case error(String)
    }

    @Published private(set) var phase: Phase = .loading
    @Published private(set) var projection: OperatingProjectionPayload?
    @Published private(set) var masters: [OperatingMasterPayload] = []
    @Published private(set) var priceLists: [OperatingPriceListPayload] = []
    @Published private(set) var writeError: String?
    @Published private(set) var writing = false

    let momentId: String
    let companyId: String?
    let vertical: BusinessOperatingVertical

    private let repo: BusinessOperatingGateway
    private var loadTask: Task<Void, Never>?

    init(
        momentId: String,
        companyId: String?,
        subtype: String?,
        repo: BusinessOperatingGateway = BusinessOperatingRepository()
    ) {
        self.momentId = momentId
        self.companyId = companyId
        self.vertical = BusinessOperatingVertical.forSubtype(subtype) ?? .trading
        self.repo = repo
    }

    var currency: String { projection?.currency ?? "INR" }
    var financeVisible: Bool { projection?.financeVisible ?? false }
    var title: String {
        if let title = projection?.title, !title.isEmpty { return title }
        switch vertical {
        case .trading: return "Trading"
        case .distribution: return "Distribution"
        case .standardProduction: return "Manufacturing"
        case .madeToOrder: return "Make to order"
        case .clientCampaign: return "Client Campaign"
        case .ownBusinessCampaign: return "Own Business Campaign"
        case .retailStore: return "Retail"
        case .restaurantFood: return "Restaurant"
        case .consumerService: return "Service"
        }
    }

    /// Masters grouped by kind, so a tab lists only what it owns.
    var mastersByKind: [String: [OperatingMasterPayload]] {
        Dictionary(grouping: masters) { $0.kind.uppercased() }
    }

    func masters(kinds: Set<String>) -> [OperatingMasterPayload] {
        masters.filter { kinds.contains($0.kind.uppercased()) }
    }

    /// Resolve a board section anywhere in the projection by its code.
    func section(code: String) -> OperatingBoardSectionPayload? {
        let groups = [projection?.boards?.moments, projection?.boards?.life, projection?.boards?.memory]
        for group in groups {
            if let hit = group?.first(where: { $0.code == code }) { return hit }
        }
        return nil
    }

    /// The six Pulse surfaces read a metric group, not a board section.
    func pulseGroup(code: String) -> OperatingPulseMetricGroupPayload? {
        projection?.pulseMetrics?.first { $0.code == code }
    }

    // MARK: - Loading

    /// Optional view-all period / status filters (`DST-SHEET` chips) re-project from the API.
    var periodFilter: String?
    var statusFilter: String?
    /// ISO `yyyy-MM-dd` bounds for `period=custom` (`from` / `to` query params).
    var periodFrom: String?
    var periodTo: String?

    func load() async {
        loadTask?.cancel()
        if projection == nil { phase = .loading }
        do {
            let custom = periodFilter == "custom"
            let payload = try await repo.loadProjection(
                momentId: momentId,
                period: periodFilter,
                status: statusFilter,
                from: custom ? periodFrom : nil,
                to: custom ? periodTo : nil
            )
            projection = payload
            phase = payload.retired == true ? .empty : .ready
        } catch is CancellationError {
            return
        } catch {
            phase = .error(Self.describe(error))
        }
        await loadMasters()
    }

    /// Remap Figma period chip labels onto the API `period` query.
    func applySheetPeriod(_ label: String) async {
        switch label {
        case "Last month":
            periodFilter = "last_month"
            periodFrom = nil
            periodTo = nil
        case "Custom range ›":
            periodFilter = "custom"
            if (periodFrom ?? "").isEmpty {
                periodFrom = Self.isoDay(Self.startOfYear())
            }
            if (periodTo ?? "").isEmpty {
                periodTo = Self.isoDay(Date())
            }
        default:
            periodFilter = "this_month"
            periodFrom = nil
            periodTo = nil
        }
        // Period bounds are not @Published; notify so From/To chips refresh before reload.
        objectWillChange.send()
        await load()
    }

    /// Update one custom-range bound and reload with `period=custom&from&to`.
    func applyCustomPeriod(from: String? = nil, to: String? = nil) async {
        if let from { periodFrom = from }
        if let to { periodTo = to }
        periodFilter = "custom"
        objectWillChange.send()
        await load()
    }

    func applySheetStatus(_ label: String) async {
        switch label.lowercased() {
        case "open": statusFilter = "open"
        case "done": statusFilter = "done"
        case "overdue": statusFilter = "overdue"
        case "settled": statusFilter = "settled"
        default: statusFilter = "all"
        }
        await load()
    }

    /// Masters are company-scoped and optional: a failed master read must not blank the tab.
    func loadMasters() async {
        guard let companyId, !companyId.isEmpty else { return }
        do {
            masters = try await repo.listMasters(companyId: companyId, kind: nil, momentId: momentId)
        } catch {
            masters = projection?.masters ?? []
        }
    }

    func loadPriceLists() async {
        guard let companyId, !companyId.isEmpty else { return }
        do {
            priceLists = try await repo.listPriceLists(companyId: companyId, momentId: momentId)
        } catch {
            priceLists = []
        }
    }

    // MARK: - Writes

    @discardableResult
    func record(_ input: OperatingEventInput) async -> Bool {
        var body = input
        body.momentId = momentId
        writing = true
        writeError = nil
        defer { writing = false }
        do {
            _ = try await repo.recordEvent(body)
            await load()
            return true
        } catch {
            writeError = Self.describe(error)
            return false
        }
    }

    @discardableResult
    func decide(eventId: String, decision: String) async -> Bool {
        writing = true
        writeError = nil
        defer { writing = false }
        do {
            _ = try await repo.decideEvent(momentId: momentId, eventId: eventId, decision: decision)
            await load()
            return true
        } catch {
            writeError = Self.describe(error)
            return false
        }
    }

    @discardableResult
    func createMaster(_ draft: OperatingMasterDraft) async -> OperatingMasterPayload? {
        guard let companyId, !companyId.isEmpty, draft.canSave else {
            writeError = "A master needs a name and a company."
            return nil
        }
        writing = true
        writeError = nil
        defer { writing = false }
        do {
            var scoped = draft
            scoped.momentId = momentId
            let created = try await repo.createMaster(companyId: companyId, body: scoped.toCreateBody())
            await loadMasters()
            await load()
            return created
        } catch {
            writeError = Self.describe(error)
            return nil
        }
    }

    @discardableResult
    func updateMaster(id: String, draft: OperatingMasterDraft, expectedVersion: Int) async -> OperatingMasterPayload? {
        guard let companyId, !companyId.isEmpty, draft.canSave else {
            writeError = "A master needs a name and a company."
            return nil
        }
        writing = true
        writeError = nil
        defer { writing = false }
        do {
            let updated = try await repo.updateMaster(
                companyId: companyId,
                masterId: id,
                body: draft.toUpdateBody(expectedVersion: expectedVersion)
            )
            await loadMasters()
            return updated
        } catch {
            writeError = Self.describe(error)
            return nil
        }
    }

    @discardableResult
    func archiveMaster(id: String, expectedVersion: Int) async -> Bool {
        guard let companyId, !companyId.isEmpty else { return false }
        writing = true
        writeError = nil
        defer { writing = false }
        do {
            _ = try await repo.archiveMaster(companyId: companyId, masterId: id, expectedVersion: expectedVersion)
            await loadMasters()
            await load()
            return true
        } catch {
            writeError = Self.describe(error)
            return false
        }
    }

    @discardableResult
    func createPriceList(_ body: OperatingPriceListCreateBody) async -> OperatingPriceListPayload? {
        guard let companyId, !companyId.isEmpty else {
            writeError = "A price list belongs to a company."
            return nil
        }
        writing = true
        writeError = nil
        defer { writing = false }
        do {
            var scoped = body
            if scoped.momentId == nil { scoped.momentId = momentId }
            let created = try await repo.createPriceList(companyId: companyId, body: scoped)
            await loadPriceLists()
            return created
        } catch {
            writeError = Self.describe(error)
            return nil
        }
    }

    func document(eventId: String, kind: String) async -> OperatingDocumentPayload? {
        do {
            return try await repo.loadDocument(momentId: momentId, eventId: eventId, kind: kind)
        } catch {
            writeError = Self.describe(error)
            return nil
        }
    }

    // MARK: - Errors

    /// A failure is shown as the server's message when it has one, never as a raw NSError
    /// description the operator cannot act on.
    private static func describe(_ error: Error) -> String {
        if let api = error as? APIErrorKind {
            switch api {
            case .network: return "You appear to be offline. The record was not posted."
            case .unauthenticated: return "Sign in again to continue."
            case let .validation(message), let .conflict(message), let .forbidden(message),
                 let .notFound(message), let .rateLimited(message), let .server(message),
                 let .unknown(message):
                return message.isEmpty ? "That did not go through." : message
            }
        }
        let text = error.localizedDescription
        return text.isEmpty ? "That did not go through." : text
    }

    private static func isoDay(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = .current
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.string(from: date)
    }

    private static func startOfYear(_ date: Date = Date()) -> Date {
        let calendar = Calendar(identifier: .gregorian)
        let year = calendar.component(.year, from: date)
        return calendar.date(from: DateComponents(year: year, month: 1, day: 1)) ?? date
    }
}

extension OperatingProjectionPayload {
    /// True when Pulse has setup or live signal worth showing (budget / target / GST /
    /// feeds) instead of the Obsidian "No pulse yet" takeover. Fresh ACTIVE moments with
    /// setup prefs must keep the normal Pulse chrome even before the first sale.
    var hasUsefulPulseChrome: Bool {
        if !(activity ?? []).isEmpty || !(attention ?? []).isEmpty { return true }
        if !(paymentReadiness ?? []).isEmpty { return true }
        if budget?.configured == true { return true }
        if target?.configured == true { return true }
        if gstTracking?.caseInsensitiveCompare("YES") == .orderedSame { return true }
        return false
    }
}
