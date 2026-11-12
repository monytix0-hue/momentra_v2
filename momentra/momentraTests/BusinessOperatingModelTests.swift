import Foundation
import Testing
@testable import momentra

/// A gateway that answers from fixtures and records what it was asked to do, so the model can be
/// tested without a network or a running backend.
private final class RecordingGateway: BusinessOperatingGateway, @unchecked Sendable {
    var projection: OperatingProjectionPayload
    var masters: [OperatingMasterPayload] = []

    var projectionReads = 0
    var lastProjectionQuery: (period: String?, status: String?, from: String?, to: String?)?
    var lastRecordedEvent: OperatingEventInput?
    var lastDecision: (eventId: String, decision: String)?
    var lastCreatedMaster: OperatingMasterCreateBody?
    var lastUpdatedMaster: (id: String, body: OperatingMasterUpdateBody)?
    var lastArchivedMaster: (id: String, expectedVersion: Int)?
    var lastCreatedPriceList: OperatingPriceListCreateBody?
    var lastDocumentRequest: (eventId: String, kind: String)?

    /// When set, the next call throws this instead of succeeding.
    var failure: Error?

    init(projection: OperatingProjectionPayload) {
        self.projection = projection
    }

    func loadProjection(
        momentId: String,
        period: String?,
        status: String?,
        from: String?,
        to: String?
    ) async throws -> OperatingProjectionPayload {
        projectionReads += 1
        lastProjectionQuery = (period, status, from, to)
        if let failure { throw failure }
        return projection
    }

    func recordEvent(_ input: OperatingEventInput) async throws -> RecordOperatingEventResult {
        if let failure { throw failure }
        lastRecordedEvent = input
        return RecordOperatingEventResult(eventId: "event-1", approvalStatus: "RECORDED")
    }

    func decideEvent(momentId: String, eventId: String, decision: String) async throws -> RecordOperatingEventResult {
        if let failure { throw failure }
        lastDecision = (eventId, decision)
        return RecordOperatingEventResult(eventId: eventId, approvalStatus: decision)
    }

    func listMasters(companyId: String, kind: String?, momentId: String?) async throws -> [OperatingMasterPayload] {
        if let failure { throw failure }
        return masters
    }

    func createMaster(companyId: String, body: OperatingMasterCreateBody) async throws -> OperatingMasterPayload {
        if let failure { throw failure }
        lastCreatedMaster = body
        return Self.master(id: "master-1", kind: body.kind, name: body.name)
    }

    func updateMaster(companyId: String, masterId: String, body: OperatingMasterUpdateBody) async throws -> OperatingMasterPayload {
        if let failure { throw failure }
        lastUpdatedMaster = (masterId, body)
        return Self.master(id: masterId, kind: body.kind ?? "SKU", name: body.name ?? "")
    }

    func archiveMaster(companyId: String, masterId: String, expectedVersion: Int) async throws -> OperatingMasterPayload {
        if let failure { throw failure }
        lastArchivedMaster = (masterId, expectedVersion)
        return Self.master(id: masterId, kind: "SKU", name: "Archived")
    }

    func listPriceLists(companyId: String, momentId: String?) async throws -> [OperatingPriceListPayload] {
        if let failure { throw failure }
        return []
    }

    func createPriceList(companyId: String, body: OperatingPriceListCreateBody) async throws -> OperatingPriceListPayload {
        if let failure { throw failure }
        lastCreatedPriceList = body
        return try Self.decodePriceList(name: body.name)
    }

    func loadDocument(momentId: String, eventId: String, kind: String) async throws -> OperatingDocumentPayload {
        if let failure { throw failure }
        lastDocumentRequest = (eventId, kind)
        let json = """
        {
          "kind": "\(kind)",
          "eventId": "\(eventId)",
          "momentId": "\(momentId)",
          "documentNumber": "INV-1",
          "issuedAt": "2026-11-04T10:00:00.000Z",
          "currency": "INR",
          "reprint": true
        }
        """
        return try JSONDecoder().decode(OperatingDocumentPayload.self, from: Data(json.utf8))
    }

    static func decodePriceList(name: String) throws -> OperatingPriceListPayload {
        let json = """
        {
          "id": "list-1",
          "name": "\(name)",
          "validFrom": "2026-11-01"
        }
        """
        return try JSONDecoder().decode(OperatingPriceListPayload.self, from: Data(json.utf8))
    }

    /// A master row as the API returns it — decoded, so the fixture can never drift from the
    /// payload's required fields.
    static func master(id: String, kind: String, name: String) -> OperatingMasterPayload {
        let json = """
        {
          "id": "\(id)",
          "source": "MASTER",
          "kind": "\(kind)",
          "name": "\(name)",
          "status": "ACTIVE",
          "version": 1
        }
        """
        // A malformed fixture is a test-authoring bug, not a product bug, so it traps loudly.
        // swiftlint:disable:next force_try
        return try! JSONDecoder().decode(OperatingMasterPayload.self, from: Data(json.utf8))
    }
}

/// A stand-in for the server's projection body, decoded from the same JSON shape iOS receives.
private func makeQuietProjection(
    budgetConfigured: Bool = false,
    targetConfigured: Bool = false,
    gstTracking: String = "NO"
) throws -> OperatingProjectionPayload {
    let budget = budgetConfigured
        ? #"{ "amount": "500000", "spent": "0", "remaining": "500000", "configured": true }"#
        : #"{ "amount": "0", "spent": "0", "remaining": "0", "configured": false }"#
    let target = targetConfigured
        ? #"{ "metric": "REVENUE", "value": "1000000", "progress": "0", "configured": true }"#
        : #"{ "metric": "REVENUE", "value": "0", "progress": "0", "configured": false }"#
    let json = """
    {
      "retired": false,
      "familyCode": "TRADING",
      "subtype": "TRADING_WHOLESALE",
      "title": "skanda Trading",
      "financeVisible": true,
      "currency": "INR",
      "activity": [],
      "attention": [],
      "paymentReadiness": [],
      "budget": \(budget),
      "target": \(target),
      "gstTracking": "\(gstTracking)"
    }
    """
    return try JSONDecoder().decode(OperatingProjectionPayload.self, from: Data(json.utf8))
}

private func makeProjection(
    subtype: String = "TRADING_WHOLESALE",
    title: String = "Sharma Wholesale",
    financeVisible: Bool = true,
    retired: Bool = false
) throws -> OperatingProjectionPayload {
    let json = """
    {
      "retired": \(retired),
      "familyCode": "TRADING",
      "subtype": "\(subtype)",
      "subtypeLabel": "Trading",
      "title": "\(title)",
      "financeVisible": \(financeVisible),
      "currency": "INR",
      "startDate": "2026-11-01T00:00:00.000Z",
      "endDate": "2026-11-30T23:59:59.000Z",
      "finance": {
        "sales": "125000",
        "received": "90000",
        "outstanding": "35000",
        "payable": "12000",
        "eligibleCost": "80000",
        "cashPaid": "68000"
      },
      "pulseMetrics": [
        {
          "code": "FINANCE",
          "why": "Only recorded money counts.",
          "metrics": [
            { "key": "sales", "label": "Recorded sales", "value": "125000", "unit": "₹", "tone": "NORMAL" },
            { "key": "received", "label": "Collected", "value": "90000", "unit": "₹", "tone": "POSITIVE" }
          ]
        }
      ],
      "boards": {
        "moments": [
          {
            "code": "orders",
            "title": "Orders & Commitments",
            "detail": "3 open orders",
            "why": "Commitments are not revenue until billed.",
            "metrics": [
              { "key": "open", "label": "Open orders", "value": "3", "tone": "NORMAL" }
            ],
            "rows": [
              {
                "id": "event-1",
                "eventType": "ORDER",
                "title": "Order to Sharma Traders",
                "occurredAt": "2026-11-04T10:00:00.000Z",
                "amount": "48000",
                "badge": "Open",
                "subtitle": "2 items · due 12 Nov"
              }
            ]
          }
        ],
        "life": [
          { "code": "L1", "title": "Current Business", "detail": "Trading as recorded", "rows": [] }
        ],
        "memory": [
          { "code": "ME1", "title": "Monthly Memory", "detail": "November 2026", "rows": [] }
        ]
      },
      "quickAdds": [
        { "code": "SALE", "label": "Sale", "needsAmount": true },
        { "code": "PURCHASE", "label": "Purchase", "needsAmount": true }
      ]
    }
    """
    return try JSONDecoder().decode(OperatingProjectionPayload.self, from: Data(json.utf8))
}

@MainActor
struct BusinessOperatingModelTests {

    // MARK: Resolution

    @Test func verticalComesFromTheSubtype() throws {
        let trading = BusinessOperatingModel(
            momentId: "m1",
            companyId: "c1",
            subtype: "TRADING_WHOLESALE",
            repo: RecordingGateway(projection: try makeProjection())
        )
        #expect(trading.vertical == .trading)

        let distribution = BusinessOperatingModel(
            momentId: "m2",
            companyId: "c1",
            subtype: "DISTRIBUTION_DEALERSHIP",
            repo: RecordingGateway(projection: try makeProjection(subtype: "DISTRIBUTION_DEALERSHIP"))
        )
        #expect(distribution.vertical == .distribution)
    }

    @Test func anUnknownSubtypeStillRendersTheTradingShell() throws {
        // The host only mounts this for the two operating subtypes, so an unknown code is a bug in
        // the caller — it must not crash or blank the tab.
        let model = BusinessOperatingModel(
            momentId: "m1",
            companyId: "c1",
            subtype: "SOMETHING_ELSE",
            repo: RecordingGateway(projection: try makeProjection())
        )
        #expect(model.vertical == .trading)
    }

    @Test func titleFallsBackToTheVerticalWhenTheProjectionIsSilent() throws {
        let model = BusinessOperatingModel(
            momentId: "m1",
            companyId: nil,
            subtype: "DISTRIBUTION_DEALERSHIP",
            repo: RecordingGateway(projection: try makeProjection(subtype: "DISTRIBUTION_DEALERSHIP", title: ""))
        )
        #expect(model.title == "Distribution")
    }

    // MARK: Loading

    @Test func loadMovesToReadyAndReadsTheProjectionOnce() async throws {
        let gateway = RecordingGateway(projection: try makeProjection())
        let model = BusinessOperatingModel(momentId: "m1", companyId: "c1", subtype: "TRADING_WHOLESALE", repo: gateway)

        await model.load()

        #expect(model.phase == .ready)
        #expect(gateway.projectionReads == 1)
        #expect(model.currency == "INR")
        #expect(model.financeVisible)
        #expect(model.title == "Sharma Wholesale")
    }

    @Test func aRetiredMomentBecomesEmpty() async throws {
        let gateway = RecordingGateway(projection: try makeProjection(retired: true))
        let model = BusinessOperatingModel(momentId: "m1", companyId: "c1", subtype: "TRADING_WHOLESALE", repo: gateway)

        await model.load()

        #expect(model.phase == .empty)
    }

    @Test func freshSetupWithBudgetKeepsPulseChrome() throws {
        #expect(try makeQuietProjection().hasUsefulPulseChrome == false)
        #expect(try makeQuietProjection(budgetConfigured: true).hasUsefulPulseChrome == true)
        #expect(try makeQuietProjection(targetConfigured: true).hasUsefulPulseChrome == true)
        #expect(try makeQuietProjection(gstTracking: "YES").hasUsefulPulseChrome == true)
    }

    @Test func aFailedLoadSurfacesAReadableMessage() async throws {
        let gateway = RecordingGateway(projection: try makeProjection())
        gateway.failure = APIErrorKind.network("offline")
        let model = BusinessOperatingModel(momentId: "m1", companyId: "c1", subtype: "TRADING_WHOLESALE", repo: gateway)

        await model.load()

        guard case let .error(message) = model.phase else {
            Issue.record("expected an error phase, got \(model.phase)")
            return
        }
        #expect(message.lowercased().contains("offline"))
    }

    @Test func customSheetPeriodDefaultsFromAndToThenReloads() async throws {
        let gateway = RecordingGateway(projection: try makeProjection())
        let model = BusinessOperatingModel(momentId: "m1", companyId: "c1", subtype: "TRADING_WHOLESALE", repo: gateway)

        await model.applySheetPeriod("Custom range ›")

        #expect(model.periodFilter == "custom")
        #expect(model.periodFrom?.count == 10)
        #expect(model.periodTo?.count == 10)
        #expect(gateway.lastProjectionQuery?.period == "custom")
        #expect(gateway.lastProjectionQuery?.from == model.periodFrom)
        #expect(gateway.lastProjectionQuery?.to == model.periodTo)
        #expect(gateway.projectionReads == 1)
    }

    @Test func thisMonthClearsCustomBounds() async throws {
        let gateway = RecordingGateway(projection: try makeProjection())
        let model = BusinessOperatingModel(momentId: "m1", companyId: "c1", subtype: "TRADING_WHOLESALE", repo: gateway)
        await model.applySheetPeriod("Custom range ›")

        await model.applySheetPeriod("This month")

        #expect(model.periodFilter == "this_month")
        #expect(model.periodFrom == nil)
        #expect(model.periodTo == nil)
        #expect(gateway.lastProjectionQuery?.period == "this_month")
        #expect(gateway.lastProjectionQuery?.from == nil)
        #expect(gateway.lastProjectionQuery?.to == nil)
    }

    @Test func changingACustomBoundReloadsWithBothEnds() async throws {
        let gateway = RecordingGateway(projection: try makeProjection())
        let model = BusinessOperatingModel(momentId: "m1", companyId: "c1", subtype: "TRADING_WHOLESALE", repo: gateway)
        await model.applySheetPeriod("Custom range ›")
        let keptTo = model.periodTo

        await model.applyCustomPeriod(from: "2026-02-01")

        #expect(model.periodFilter == "custom")
        #expect(model.periodFrom == "2026-02-01")
        #expect(model.periodTo == keptTo)
        #expect(gateway.lastProjectionQuery?.from == "2026-02-01")
        #expect(gateway.lastProjectionQuery?.to == keptTo)
    }

    @Test func mastersFallBackToTheProjectionWhenTheReadFails() async throws {
        let gateway = RecordingGateway(projection: try makeProjection())
        let model = BusinessOperatingModel(momentId: "m1", companyId: "c1", subtype: "TRADING_WHOLESALE", repo: gateway)
        await model.load()

        gateway.failure = APIErrorKind.network("offline")
        await model.loadMasters()

        // No crash, and the projection's own list (empty here) is what remains.
        #expect(model.masters.isEmpty)
    }

    @Test func noCompanyMeansNoMasterRead() async throws {
        let gateway = RecordingGateway(projection: try makeProjection())
        gateway.masters = [RecordingGateway.master(id: "m1", kind: "SKU", name: "Rice")]
        let model = BusinessOperatingModel(momentId: "m1", companyId: nil, subtype: "TRADING_WHOLESALE", repo: gateway)

        await model.loadMasters()

        #expect(model.masters.isEmpty)
    }

    // MARK: Projection lookups

    @Test func aSectionIsFoundInAnyBoardGroup() async throws {
        let model = BusinessOperatingModel(
            momentId: "m1",
            companyId: "c1",
            subtype: "TRADING_WHOLESALE",
            repo: RecordingGateway(projection: try makeProjection())
        )
        await model.load()

        #expect(model.section(code: "orders")?.title == "Orders & Commitments")
        #expect(model.section(code: "L1")?.title == "Current Business")
        #expect(model.section(code: "ME1")?.title == "Monthly Memory")
        #expect(model.section(code: "GST") == nil)
    }

    @Test func aPulseGroupIsReadByItsCode() async throws {
        let model = BusinessOperatingModel(
            momentId: "m1",
            companyId: "c1",
            subtype: "TRADING_WHOLESALE",
            repo: RecordingGateway(projection: try makeProjection())
        )
        await model.load()

        let finance = model.pulseGroup(code: "FINANCE")
        #expect(finance?.metrics?.count == 2)
        #expect(finance?.why == "Only recorded money counts.")
        #expect(model.pulseGroup(code: "GST") == nil)
    }

    @Test func rowBadgesAndSubtitlesSurviveDecoding() async throws {
        let model = BusinessOperatingModel(
            momentId: "m1",
            companyId: "c1",
            subtype: "TRADING_WHOLESALE",
            repo: RecordingGateway(projection: try makeProjection())
        )
        await model.load()

        let row = model.section(code: "orders")?.rows?.first
        #expect(row?.badge == "Open")
        #expect(row?.subtitle == "2 items · due 12 Nov")
    }

    @Test func boardMetricsKeepTheirTone() async throws {
        let model = BusinessOperatingModel(
            momentId: "m1",
            companyId: "c1",
            subtype: "TRADING_WHOLESALE",
            repo: RecordingGateway(projection: try makeProjection())
        )
        await model.load()

        #expect(model.section(code: "orders")?.metrics?.first?.value == "3")
        #expect(model.section(code: "orders")?.why == "Commitments are not revenue until billed.")
        #expect(model.pulseGroup(code: "FINANCE")?.metrics?.last?.tone == "POSITIVE")
    }

    // MARK: Masters

    @Test func mastersGroupByKind() async throws {
        let gateway = RecordingGateway(projection: try makeProjection())
        gateway.masters = [
            RecordingGateway.master(id: "a", kind: "SKU", name: "Rice"),
            RecordingGateway.master(id: "b", kind: "SKU", name: "Sugar"),
            RecordingGateway.master(id: "c", kind: "DEALER", name: "Sharma Traders"),
        ]
        let model = BusinessOperatingModel(momentId: "m1", companyId: "c1", subtype: "TRADING_WHOLESALE", repo: gateway)
        await model.loadMasters()

        #expect(model.mastersByKind["SKU"]?.count == 2)
        #expect(model.mastersByKind["DEALER"]?.count == 1)
        #expect(model.masters(kinds: OperatingMasterTab.catalogue.kinds).count == 2)
        #expect(model.masters(kinds: OperatingMasterTab.principals.kinds).count == 1)
    }

    @Test func creatingAMasterCarriesTheMomentScopeAndReloads() async throws {
        let gateway = RecordingGateway(projection: try makeProjection())
        let model = BusinessOperatingModel(momentId: "m1", companyId: "c1", subtype: "TRADING_WHOLESALE", repo: gateway)

        var draft = OperatingMasterDraft(kind: "SKU", name: "Basmati 25kg")
        draft.sellPrice = "1450"
        draft.gstRate = "5"

        let created = await model.createMaster(draft)

        #expect(created?.id == "master-1")
        #expect(gateway.lastCreatedMaster?.momentId == "m1")
        #expect(gateway.lastCreatedMaster?.gstApplicable == "YES")
        #expect(gateway.lastCreatedMaster?.sellPrice == "1450")
        #expect(model.writeError == nil)
    }

    @Test func aNamelessMasterIsRejectedBeforeTheWire() async throws {
        let gateway = RecordingGateway(projection: try makeProjection())
        let model = BusinessOperatingModel(momentId: "m1", companyId: "c1", subtype: "TRADING_WHOLESALE", repo: gateway)

        let created = await model.createMaster(OperatingMasterDraft(kind: "SKU", name: "  "))

        #expect(created == nil)
        #expect(gateway.lastCreatedMaster == nil)
        #expect(model.writeError != nil)
    }

    @Test func updatingCarriesTheVersionStamp() async throws {
        let gateway = RecordingGateway(projection: try makeProjection())
        let model = BusinessOperatingModel(momentId: "m1", companyId: "c1", subtype: "TRADING_WHOLESALE", repo: gateway)

        var draft = OperatingMasterDraft(kind: "SKU", name: "Basmati 25kg")
        draft.mrp = "1600"

        _ = await model.updateMaster(id: "master-1", draft: draft, expectedVersion: 4)

        #expect(gateway.lastUpdatedMaster?.id == "master-1")
        #expect(gateway.lastUpdatedMaster?.body.expectedVersion == 4)
        #expect(gateway.lastUpdatedMaster?.body.mrp == "1600")
    }

    @Test func archivingSendsTheVersionItSaw() async throws {
        let gateway = RecordingGateway(projection: try makeProjection())
        let model = BusinessOperatingModel(momentId: "m1", companyId: "c1", subtype: "TRADING_WHOLESALE", repo: gateway)

        let archived = await model.archiveMaster(id: "master-9", expectedVersion: 12)

        #expect(archived)
        #expect(gateway.lastArchivedMaster?.id == "master-9")
        #expect(gateway.lastArchivedMaster?.expectedVersion == 12)
    }

    @Test func aConflictIsShownVerbatim() async throws {
        let gateway = RecordingGateway(projection: try makeProjection())
        gateway.failure = APIErrorKind.conflict("That master changed. Reload and try again.")
        let model = BusinessOperatingModel(momentId: "m1", companyId: "c1", subtype: "TRADING_WHOLESALE", repo: gateway)

        let created = await model.createMaster(OperatingMasterDraft(kind: "SKU", name: "Rice"))

        #expect(created == nil)
        #expect(model.writeError == "That master changed. Reload and try again.")
    }

    // MARK: Writes

    @Test func recordingAnEventStampsTheMomentAndReloads() async throws {
        let gateway = RecordingGateway(projection: try makeProjection())
        let model = BusinessOperatingModel(momentId: "m1", companyId: "c1", subtype: "TRADING_WHOLESALE", repo: gateway)

        var input = OperatingEventInput(momentId: "", eventType: "SALE", title: "Sale to Sharma Traders")
        input.amount = "48000"
        let ok = await model.record(input)

        #expect(ok)
        #expect(gateway.lastRecordedEvent?.momentId == "m1")
        #expect(gateway.lastRecordedEvent?.amount == "48000")
        // The projection is rebuilt after a write so the board cannot show a stale figure.
        #expect(gateway.projectionReads == 1)
    }

    @Test func aFailedWriteKeepsTheErrorAndDoesNotReload() async throws {
        let gateway = RecordingGateway(projection: try makeProjection())
        gateway.failure = APIErrorKind.validation("Quantity must be greater than zero.")
        let model = BusinessOperatingModel(momentId: "m1", companyId: "c1", subtype: "TRADING_WHOLESALE", repo: gateway)

        let ok = await model.record(OperatingEventInput(momentId: "", eventType: "SALE", title: "Sale"))

        #expect(!ok)
        #expect(model.writeError == "Quantity must be greater than zero.")
        #expect(gateway.projectionReads == 0)
    }

    @Test func aDecisionIsForwardedWithItsEventId() async throws {
        let gateway = RecordingGateway(projection: try makeProjection())
        let model = BusinessOperatingModel(momentId: "m1", companyId: "c1", subtype: "TRADING_WHOLESALE", repo: gateway)

        let ok = await model.decide(eventId: "event-1", decision: "APPROVED")

        #expect(ok)
        #expect(gateway.lastDecision?.eventId == "event-1")
        #expect(gateway.lastDecision?.decision == "APPROVED")
    }

    @Test func reprintRequestsTheKindItWasAsked() async throws {
        let gateway = RecordingGateway(projection: try makeProjection())
        let model = BusinessOperatingModel(momentId: "m1", companyId: "c1", subtype: "TRADING_WHOLESALE", repo: gateway)

        let document = await model.document(eventId: "event-1", kind: "CHALLAN")

        #expect(document?.kind == "CHALLAN")
        #expect(gateway.lastDocumentRequest?.kind == "CHALLAN")
    }

    @Test func aPriceListIsScopedToTheMomentAndReloaded() async throws {
        let gateway = RecordingGateway(projection: try makeProjection())
        let model = BusinessOperatingModel(momentId: "m1", companyId: "c1", subtype: "TRADING_WHOLESALE", repo: gateway)

        let created = await model.createPriceList(OperatingPriceListCreateBody(
            name: "Diwali scheme",
            validFrom: "2026-11-01",
            lines: [OperatingPriceListLineInput(masterId: "master-1", price: "1400", schemePct: "2")]
        ))

        #expect(created?.name == "Diwali scheme")
        #expect(gateway.lastCreatedPriceList?.momentId == "m1")
        #expect(gateway.lastCreatedPriceList?.lines?.first?.masterId == "master-1")
        #expect(model.writeError == nil)
    }

    @Test func aPriceListWithoutACompanyIsRejectedBeforeTheWire() async throws {
        let gateway = RecordingGateway(projection: try makeProjection())
        let model = BusinessOperatingModel(momentId: "m1", companyId: nil, subtype: "TRADING_WHOLESALE", repo: gateway)

        let created = await model.createPriceList(OperatingPriceListCreateBody(
            name: "Diwali scheme",
            validFrom: "2026-11-01"
        ))

        #expect(created == nil)
        #expect(gateway.lastCreatedPriceList == nil)
        #expect(model.writeError != nil)
    }
}

/// The six-section setup document is pure data behind `BusinessSetupDocSpec`, so these assert the
/// frame's structure and copy without rendering SwiftUI.
struct BusinessSetupDocSpecTests {

    // MARK: Sections

    @Test func tradingFamilyVerticalsCarryTheSameSixSectionsInOrder() {
        let verticals: [BusinessOperatingVertical] = [
            .trading, .distribution, .standardProduction, .madeToOrder,
        ]
        for vertical in verticals {
            let sections = BusinessSetupDocSpec.sections(vertical)
            #expect(sections.map(\.number) == ["01", "02", "03", "04", "05", "06"])
            #expect(sections[0].title == "Business Basics")
            #expect(sections[1].title == "People & Roles")
            #expect(sections[2].title == "Channels & Monthly Plan")
            #expect(sections[4].title == "GST & Tax Details")
            #expect(sections[5].title == "Review your setup")
        }
        #expect(BusinessSetupDocSpec.sections(.trading)[3].title == "Catalogue, Warehouses & Network")
        #expect(BusinessSetupDocSpec.sections(.standardProduction)[3].title == "Parts, Lines & Network")
        #expect(BusinessSetupDocSpec.sections(.madeToOrder)[3].title == "Parts, Lines & Network")
    }

    @Test func theBasicsSubtitleNamesTheVertical() {
        #expect(BusinessSetupDocSpec.sections(.trading)[0].subtitle == "A home for this trading business.")
        #expect(BusinessSetupDocSpec.sections(.distribution)[0].subtitle == "A home for this distribution business.")
        #expect(BusinessSetupDocSpec.sections(.standardProduction)[0].subtitle == "A home for this plant business.")
        #expect(BusinessSetupDocSpec.sections(.madeToOrder)[0].subtitle == "A home for this job order business.")
    }

    // MARK: Subtype

    @Test func theSubtypeResolvesBackToItsVertical() {
        #expect(BusinessSetupDocSpec.subtype(.trading) == "TRADING_WHOLESALE")
        #expect(BusinessSetupDocSpec.subtype(.distribution) == "DISTRIBUTION_DEALERSHIP")
        #expect(BusinessSetupDocSpec.subtype(.standardProduction) == "STANDARD_PRODUCTION")
        #expect(BusinessSetupDocSpec.subtype(.madeToOrder) == "MADE_TO_ORDER")
        #expect(BusinessSetupDocSpec.vertical(forSubtype: "TRADING_WHOLESALE") == .trading)
        #expect(BusinessSetupDocSpec.vertical(forSubtype: "DISTRIBUTION_DEALERSHIP") == .distribution)
        #expect(BusinessSetupDocSpec.vertical(forSubtype: "STANDARD_PRODUCTION") == .standardProduction)
        #expect(BusinessSetupDocSpec.vertical(forSubtype: "MADE_TO_ORDER") == .madeToOrder)
        #expect(BusinessSetupDocSpec.vertical(forSubtype: nil) == nil)
        #expect(BusinessSetupDocSpec.vertical(forSubtype: "SOMETHING_ELSE") == nil)
    }

    // MARK: Chips

    @Test func channelsDifferPerVerticalAndRolesAreTheFourMintableCodes() {
        let tradingChannels = BusinessSetupDocSpec.channels(.trading)
        let distributionChannels = BusinessSetupDocSpec.channels(.distribution)
        #expect(tradingChannels.first == "Wholesale")
        #expect(distributionChannels.first == "General Trade")
        #expect(tradingChannels != distributionChannels)

        let expected = ["FINANCE", "APPROVER", "OPERATIONS_LEAD", "CONTRIBUTOR"]
        #expect(BusinessSetupDocSpec.roles(.trading).map(\.code) == expected)
        #expect(BusinessSetupDocSpec.roles(.distribution).map(\.code) == expected)
        #expect(BusinessSetupDocSpec.roles(.trading).map(\.label) == ["Finance", "Approver", "Operations", "Contributor"])
        #expect(BusinessSetupDocSpec.roles(.distribution).map(\.label) == ["Manager", "Warehouse", "Route lead", "Salesman"])
        #expect(OperatingSheetCatalog.all.count == 30)
        #expect(OperatingSheetChrome.figmaId(for: "FINANCE") == "2561:29430")
        #expect(OperatingSheetChrome.figmaId(for: "production") == "2549:30941")
        #expect(OperatingSheetChrome.figmaId(for: "ME3") == "2513:30751")
        #expect(OperatingSheetChrome.figmaId(for: "M1") == "2574:29810")
        #expect(OperatingSheetChrome.figmaId(for: "sales") == "2513:30297")
        #expect(OperatingSheetChrome.figmaId(for: "FINANCE", vertical: "TRADING_WHOLESALE") == "2566:33689")
        #expect(OperatingSheetChrome.figmaId(for: "ATTENTION", vertical: "RETAIL_STORE") == "2513:29426")
        #expect(OperatingSheetChrome.figmaId(for: "production", vertical: "STANDARD_PRODUCTION") == "2549:30941")
        #expect(OperatingSheetChrome.footerCta(for: "ME4") == "Open Memory")
        #expect(OperatingSheetChrome.footerAction(for: "PAYMENT_READINESS") == "PAYMENT")
        #expect(OperatingSheetChrome.statusFilters(for: "ATTENTION") == ["Open", "Overdue", "Settled"])
        // Every chip carries a capability the backend actually accepts.
        #expect(BusinessSetupDocSpec.roles(.trading).allSatisfy { !$0.code.isEmpty })
    }

    @Test func roleHelpersRoundTripAndSplitMembership() {
        for role in BusinessSetupDocSpec.roles(.trading) {
            #expect(BusinessSetupDocSpec.roleCode(role.label) == role.code)
            #expect(BusinessSetupDocSpec.roleLabel(role.code) == role.label)
        }
        #expect(BusinessSetupDocSpec.roleCode("Not a role") == "OPERATIONS_LEAD")
        #expect(BusinessSetupDocSpec.membershipType(for: "FINANCE") == "ADMIN")
        #expect(BusinessSetupDocSpec.membershipType(for: "APPROVER") == "ADMIN")
        #expect(BusinessSetupDocSpec.membershipType(for: "OPERATIONS_LEAD") == "MEMBER")
        #expect(BusinessSetupDocSpec.membershipType(for: "CONTRIBUTOR") == "MEMBER")
        #expect(BusinessSetupDocSpec.inviteAccessNote("FINANCE") == "Access: money, payments and finance boards.")
    }

    @Test func switchingTheKindKeepsTypedFieldsButDropsForeignChannels() {
        var doc = BusinessSetupDocState(vertical: .trading)
        doc.businessName = "Sharma Wholesale"
        doc.toggleChannel("B2B Direct")
        doc.toggleChannel("Wholesale")

        doc.switchVertical(.distribution)

        #expect(doc.vertical == .distribution)
        #expect(doc.businessName == "Sharma Wholesale")
        // `B2B Direct` is trading-only; `Wholesale` is shared and survives the switch.
        #expect(doc.channels == Set(["Wholesale"]))
        #expect(doc.inviteRole == "OPERATIONS_LEAD")
    }

    // MARK: §03

    @Test func indianGroupingMatchesTheFramesMoneyFields() {
        #expect(BusinessSetupDocSpec.formatIndianAmount("1400") == "1,400")
        #expect(BusinessSetupDocSpec.formatIndianAmount("60000") == "60,000")
        #expect(BusinessSetupDocSpec.formatIndianAmount("6000000") == "60,00,000")
        #expect(BusinessSetupDocSpec.formatIndianAmount("123456789") == "12,34,56,789")
        #expect(BusinessSetupDocSpec.formatIndianAmount("12.3456") == "12.34")
        #expect(BusinessSetupDocSpec.formatIndianAmount("") == "")
        #expect(BusinessSetupDocSpec.formatIndianAmount("000") == "0")
    }

    @Test func amountsKeepOnlyDigitsAndOneDot() {
        #expect(BusinessSetupDocSpec.plainAmount("₹ 6,00,000") == "600000")
        #expect(BusinessSetupDocSpec.plainAmount("1.2.3") == "1.23")
        #expect(BusinessSetupDocSpec.plainAmount("abc") == "")
    }

    @Test func theMetricLabelNeverShowsARawEnum() {
        #expect(BusinessSetupDocSpec.metricLabel("REVENUE") == "Revenue")
        #expect(BusinessSetupDocSpec.metricLabel("ORDERS") == "Orders")
        #expect(BusinessSetupDocSpec.metricLabel("CUSTOM_COUNT") == "Count")
        #expect(BusinessSetupDocSpec.metricLabel("") == "Revenue")
        #expect(BusinessSetupDocSpec.metricCode("Orders") == "ORDERS")
        #expect(BusinessSetupDocSpec.metricCode("Count") == "CUSTOM_COUNT")
        #expect(BusinessSetupDocSpec.metricCode("Revenue") == "REVENUE")
    }

    // MARK: §04

    @Test func anEmptyCatalogueNeverPrintsAZero() {
        let empty = BusinessSetupMasterCounts()
        #expect(empty.isEmpty)
        #expect(BusinessSetupDocSpec.catalogueSubtitle(empty) == BusinessSetupDocSpec.catalogueInvite)
        #expect(BusinessSetupDocSpec.catalogueSubtitle(empty) == "Name, barcode, MRP, case size, HSN, GST")
        #expect(!BusinessSetupDocSpec.catalogueSubtitle(empty).contains("0"))
    }

    @Test func aFilledCatalogueCountsSkusAndPriceLists() {
        var counts = BusinessSetupMasterCounts()
        counts.skus = 24
        counts.priceLists = 2
        #expect(BusinessSetupDocSpec.catalogueSubtitle(counts) == "24 SKUs · 2 price lists · HSN + GST on each")

        var single = BusinessSetupMasterCounts()
        single.skus = 1
        single.priceLists = 1
        #expect(BusinessSetupDocSpec.catalogueSubtitle(single) == "1 SKU · 1 price list · HSN + GST on each")
    }

    @Test func warehousesAreNamedBeforeTheyAreCounted() {
        var counts = BusinessSetupMasterCounts()
        counts.warehouses = 2
        counts.routes = 1
        #expect(BusinessSetupDocSpec.warehouseSubtitle(names: ["Godown A", "Godown B"], counts: counts) == "Godown A · Godown B · 1 route")

        // With no names the row falls back to the count, not to an empty string.
        #expect(BusinessSetupDocSpec.warehouseSubtitle(names: [], counts: counts) == "2 warehouses · 1 route")
        #expect(BusinessSetupDocSpec.warehouseSubtitle(names: [], counts: BusinessSetupMasterCounts()) == BusinessSetupDocSpec.warehouseInvite)
    }

    @Test func theNetworkRowUsesTheVerticalsOwnNoun() {
        var counts = BusinessSetupMasterCounts()
        counts.principals = 3
        counts.dealers = 4
        #expect(BusinessSetupDocSpec.networkSubtitle(.trading, counts: counts) == "3 principals · 4 dealers · GSTIN on file")
        #expect(BusinessSetupDocSpec.networkSubtitle(.distribution, counts: counts) == "3 suppliers · 4 buyers · GSTIN on file")
        #expect(BusinessSetupDocSpec.networkSubtitle(.trading, counts: BusinessSetupMasterCounts()) == BusinessSetupDocSpec.networkInvite)
        #expect(BusinessSetupDocSpec.networkLabel(.trading) == "Business contacts")
        #expect(BusinessSetupDocSpec.networkLabel(.distribution) == "Suppliers & customers")
    }

    // MARK: §02 copy

    @Test func initialsTakeTheFirstAndLastWord() {
        #expect(BusinessSetupDocSpec.initials("Kiran Rao") == "KR")
        #expect(BusinessSetupDocSpec.initials("kiran rao") == "KR")
        #expect(BusinessSetupDocSpec.initials("Kiran") == "K")
        #expect(BusinessSetupDocSpec.initials("  Kiran   Rao  ") == "KR")
        #expect(BusinessSetupDocSpec.initials("Kiran Mohan Rao") == "KR")
        #expect(BusinessSetupDocSpec.initials("") == "")
        #expect(BusinessSetupDocSpec.initials("   ") == "")
    }

    @Test func teamMetaReadsAsAStatusSentence() {
        #expect(BusinessSetupDocSpec.teamMeta(status: "PENDING", role: "Operations") == "Invite sent · Operations")
        #expect(BusinessSetupDocSpec.teamMeta(status: "ACTIVE", role: "Finance") == "Added · Finance")
    }

    @Test func theDefaultRoleNoteNamesTheDefaultMintableRole() {
        #expect(BusinessSetupDocSpec.defaultRoleNote(.trading) == "Default Operations · sales, stock, purchases and orders. Role applies when they join.")
        #expect(BusinessSetupDocSpec.defaultRoleNote(.distribution) == BusinessSetupDocSpec.defaultRoleNote(.trading))
    }

    @Test func inviteesRideTheParticipantsPreferenceAfterTheOwner() {
        var doc = BusinessSetupDocState(vertical: .trading)
        doc.businessName = "Sharma Wholesale"
        doc.invitees = [
            BusinessSetupInvitee(id: "a", name: "Kiran Rao", roleCode: "OPERATIONS_LEAD"),
            BusinessSetupInvitee(id: "b", name: "  Asha Menon  ", roleCode: "FINANCE", phone: "+91 98765 43210"),
            BusinessSetupInvitee(id: "c", name: "   ", roleCode: "APPROVER"),
        ]
        let rows = doc.preferences(ownerName: "Owner Name")["participants"] as? [[String: String]] ?? []

        #expect(rows.count == 3)
        #expect(rows[0]["displayName"] == "Owner Name")
        #expect(rows[0]["role"] == "OWNER_ADMIN")
        #expect(rows[1]["displayName"] == "Kiran Rao")
        #expect(rows[1]["role"] == "OPERATIONS_LEAD")
        #expect(rows[2]["displayName"] == "Asha Menon")
        #expect(rows[2]["role"] == "FINANCE")
        #expect(rows.allSatisfy { !($0["displayName"] ?? "").isEmpty })
    }

    @Test func savedParticipantsRoundTripBackIntoInviteeRows() {
        var reopened = BusinessSetupDocState(vertical: .trading)
        reopened.apply(preferences: [
            "subtype": "TRADING_WHOLESALE",
            "participants": [
                ["displayName": "Owner Name", "role": "OWNER_ADMIN"],
                ["displayName": "Kiran Rao", "role": "OPERATIONS_LEAD"],
                ["displayName": "Asha Menon", "role": "FINANCE"],
            ],
        ])

        #expect(reopened.invitees.map(\.name) == ["Kiran Rao", "Asha Menon"])
        #expect(reopened.invitees.map(\.roleLabel) == ["Operations", "Finance"])
        #expect(reopened.inviteRole == "OPERATIONS_LEAD")
    }

    // MARK: §05 codes

    @Test func gstLabelsRoundTripThroughTheirCodes() {
        for label in BusinessSetupDocSpec.gstRegistrationLabels {
            let code = BusinessSetupDocSpec.gstRegistrationCode(label)
            #expect(code == label.uppercased().replacingOccurrences(of: " ", with: "_"))
        }
        #expect(BusinessSetupDocSpec.gstPricingCode("Prices include GST") == "YES")
        #expect(BusinessSetupDocSpec.gstPricingCode("GST is added separately") == "NO")
        #expect(BusinessSetupDocSpec.gstTrackingCode("Yes") == "YES")
        #expect(BusinessSetupDocSpec.gstTrackingCode("Not now") == "NOT_NOW")
    }

    @Test func theCurrencyLabelCarriesItsCode() {
        #expect(BusinessSetupDocSpec.currencyCode("INR · Indian Rupee (₹)") == "INR")
        #expect(BusinessSetupDocSpec.currencyCode("USD · US Dollar") == "USD")
        #expect(BusinessSetupDocSpec.currencyCode("") == "INR")
    }

    // MARK: §06 copy

    @Test func theMomentLineReadsLikeTheReviewCard() {
        #expect(
            BusinessSetupDocSpec.momentLine(vertical: .trading, currency: "INR", startDate: "2026-10-12", endDate: "")
                == "Trading · INR · From 12 October 2026 · Still open"
        )
        #expect(
            BusinessSetupDocSpec.momentLine(vertical: .distribution, currency: "INR", startDate: "2026-10-12", endDate: "2027-03-31")
                == "Distribution · INR · From 12 October 2026 · Until 31 March 2027"
        )
        #expect(
            BusinessSetupDocSpec.momentLine(vertical: .trading, currency: "", startDate: "", endDate: "")
                == "Trading · INR · From Today · Still open"
        )
    }

    @Test func datesAndMonthsPrintAsTheFrameDoes() {
        #expect(BusinessSetupDocSpec.dateLabel("2026-10-12") == "12 October 2026")
        #expect(BusinessSetupDocSpec.dateLabel("2026-01-01") == "1 January 2026")
        #expect(BusinessSetupDocSpec.dateLabel("not-a-date") == "not-a-date")
        #expect(BusinessSetupDocSpec.monthLabel("2026-10") == "October 2026")
        #expect(BusinessSetupDocSpec.monthLabel("") == "This month")
    }

    // MARK: Preferences

    @Test func theDocumentEmitsOnlyAllowedPreferenceKeys() {
        for kind in BusinessSetupKind.allCases {
            let catalog = BusinessSetupCatalog.forKind(kind)
            #expect(BusinessSetupDocSpec.preferenceKeys.isSubset(of: catalog.allowedKeys))
        }
    }

    @Test func thePayloadKeySetIsStable() {
        let payload = BusinessSetupDocSpec.preferences(vertical: .trading, identityName: "Sharma", partyName: "")
        #expect(Set(payload.keys) == BusinessSetupDocSpec.preferenceKeys)
    }

    @Test func switchingToDistributionChangesTheSubtypeOnTheWire() {
        var doc = BusinessSetupDocState(vertical: .trading, initialName: "Sharma Wholesale")
        doc.toggleChannel("Wholesale")
        doc.budget = "6,00,000"
        doc.target = "10,00,000"

        let trading = doc.preferences(ownerName: "Kiran Rao")
        #expect(trading["subtype"] as? String == "TRADING_WHOLESALE")
        #expect(trading["channels"] as? [String] == ["Wholesale"])

        doc.switchVertical(.distribution)
        let distribution = doc.preferences(ownerName: "Kiran Rao")
        #expect(distribution["subtype"] as? String == "DISTRIBUTION_DEALERSHIP")
        // `Wholesale` is a channel of both verticals, so the switch keeps it selected.
        #expect(distribution["channels"] as? [String] == ["Wholesale"])
        // The money typed in §03 is sent as digits, not the grouped display string.
        #expect(distribution["monthlyBudget"] as? String == "600000")
        #expect(distribution["monthlyTarget"] as? String == "1000000")
        #expect(distribution["targetMetric"] as? String == "REVENUE")
    }

    @Test func theOwnerIsCarriedAsTheFirstParticipant() {
        let doc = BusinessSetupDocState(vertical: .trading, initialName: "Sharma")
        let payload = doc.preferences(ownerName: "Kiran Rao")
        let participants = payload["participants"] as? [[String: String]]
        #expect(participants?.first?["displayName"] == "Kiran Rao")
        #expect(participants?.first?["role"] == "OWNER_ADMIN")

        // A blank owner name still has to name someone, or the backend drops the row.
        let anonymous = doc.preferences(ownerName: "  ")
        #expect((anonymous["participants"] as? [[String: String]])?.first?["displayName"] == "Owner")
    }

    @Test func aDraftRoundTripsThroughItsPreferences() {
        var saved = BusinessSetupDocState(vertical: .distribution, initialName: "Metro Distribution")
        saved.description = "Dealership for the western belt"
        saved.location = "Pune"
        saved.gstin = "27AABCM1234C1ZX"
        saved.gstState = "Maharashtra"
        saved.gstTracking = "Not now"
        saved.gstRegistration = "Composition"
        saved.gstPricing = "Prices include GST"
        saved.channels = ["General Trade"]
        saved.budget = "2,50,000"
        saved.target = "9,00,000"
        saved.month = "2026-11"
        saved.startDate = "2026-11-01"
        // The chip is transient; the durable record is the per-invitee role.
        saved.invitees = [BusinessSetupInvitee(id: "a", name: "Asha Menon", roleCode: "FINANCE")]

        var reopened = BusinessSetupDocState()
        reopened.apply(preferences: saved.preferences(ownerName: "Kiran Rao"))

        #expect(reopened.vertical == .distribution)
        #expect(reopened.businessName == "Metro Distribution")
        #expect(reopened.description == "Dealership for the western belt")
        #expect(reopened.location == "Pune")
        #expect(reopened.gstin == "27AABCM1234C1ZX")
        #expect(reopened.gstState == "Maharashtra")
        #expect(reopened.gstTracking == "Not now")
        #expect(reopened.gstRegistration == "Composition")
        #expect(reopened.gstPricing == "Prices include GST")
        #expect(reopened.channels == Set(["General Trade"]))
        #expect(reopened.budget == "2,50,000")
        #expect(reopened.target == "9,00,000")
        #expect(reopened.startDate == "2026-11-01")
        #expect(reopened.currency == "INR · Indian Rupee (₹)")
        #expect(reopened.inviteRole == "OPERATIONS_LEAD")
        #expect(reopened.invitees.map(\.name) == ["Asha Menon"])
        #expect(reopened.invitees.map(\.roleCode) == ["FINANCE"])
        #expect(reopened.invitees.map(\.roleLabel) == ["Finance"])
    }

    @Test func anInviteeRoleOutsideTheChipSetRoundTripsUnchanged() {
        // A saved draft can carry a role the chip set no longer offers; the row must survive it
        // rather than silently degrading to the default operations role.
        var reopened = BusinessSetupDocState(vertical: .trading)
        reopened.apply(preferences: [
            "participants": [
                ["displayName": "Owner Name", "role": "OWNER_ADMIN"],
                ["displayName": "Kiran Rao", "role": "OBSERVER"],
            ],
        ])

        #expect(reopened.invitees.map(\.roleCode) == ["OBSERVER"])
        let rows = reopened.preferences(ownerName: "Owner Name")["participants"] as? [[String: String]] ?? []
        #expect(rows.last?["role"] == "OBSERVER")
    }

    // MARK: Readiness

    @Test func startingNeedsANameAChannelABudgetAndATarget() {
        var doc = BusinessSetupDocState(vertical: .trading)
        #expect(!doc.canStart)

        doc.businessName = "Sharma Wholesale"
        #expect(!doc.canStart)

        doc.toggleChannel("Wholesale")
        #expect(!doc.canStart)

        doc.budget = "6,00,000"
        #expect(!doc.canStart)

        doc.target = "10,00,000"
        #expect(doc.canStart)
    }

    @Test func aBlankNameIsNotAName() {
        var doc = BusinessSetupDocState(vertical: .trading)
        doc.businessName = "   "
        doc.toggleChannel("Wholesale")
        doc.budget = "1000"
        doc.target = "2000"
        #expect(!doc.canStart)
    }

    // MARK: Kind catalogue

    @Test func onlyTradingAndDistributionCarryTheDocument() {
        #expect(BusinessSetupKind.trading.vertical == .trading)
        #expect(BusinessSetupKind.distribution.vertical == .distribution)
        #expect(BusinessSetupKind.marketing.vertical == nil)
        #expect(BusinessSetupKind.retail.vertical == nil)
        #expect(BusinessSetupKind.trading.familyCode == "TRADING_DISTRIBUTION")
        #expect(BusinessSetupKind.distribution.familyCode == "TRADING_DISTRIBUTION")
        #expect(BusinessSetupKind.trading.subtype == "TRADING_WHOLESALE")
        #expect(BusinessSetupKind.distribution.subtype == "DISTRIBUTION_DEALERSHIP")
    }

    @Test func theCatalogPassesTheDocumentItsOwnChips() {
        let trading = BusinessSetupCatalog.forKind(.trading)
        #expect(trading.activateLabel == "Start this business")
        #expect(trading.momentTypeCode == "TRADING_WHOLESALE")
        #expect(trading.defaultPreferences["subtype"] as? String == "TRADING_WHOLESALE")

        let distribution = BusinessSetupCatalog.forKind(.distribution)
        #expect(distribution.momentTypeCode == "DISTRIBUTION_DEALERSHIP")
        #expect(distribution.defaultPreferences["subtype"] as? String == "DISTRIBUTION_DEALERSHIP")

        // A kind without the document gets no chips, so nothing can be sent for it by accident.
        #expect(BusinessSetupKind.marketing.channels.isEmpty)
        #expect(BusinessSetupKind.marketing.inviteRoles.isEmpty)
    }

    // MARK: Status

    @Test func onlyADraftMayBeDiscardedOrPromoted() {
        #expect(BusinessSetupDocSpec.isDiscardable("DRAFT"))
        #expect(!BusinessSetupDocSpec.isDiscardable("ACTIVE"))
        #expect(BusinessSetupDocSpec.isPromotable("DRAFT"))
        #expect(!BusinessSetupDocSpec.isPromotable("ACTIVE"))
    }

    @Test func theActivateCopyFollowsTheStatus() {
        #expect(BusinessSetupDocSpec.activateLabel(status: "DRAFT") == "Start this business")
        #expect(BusinessSetupDocSpec.activateLabel(status: "ACTIVE") == "Save changes")
        #expect(BusinessSetupDocSpec.activatingLabel(status: "DRAFT") == "Starting…")
        #expect(BusinessSetupDocSpec.activatingLabel(status: "ACTIVE") == "Saving…")
        #expect(BusinessSetupDocSpec.statusPillLabel(status: "DRAFT") == "Setup in progress")
        #expect(BusinessSetupDocSpec.statusPillLabel(status: "ACTIVE") == "Business is live")
    }
}
