import Foundation

/// Everything the Trading and Distribution view layer reads and writes. The projection is
/// the same contract Android consumes; masters and price lists are company-scoped rows, not
/// event-derived names, so a write can reference a master by id.
protocol BusinessOperatingGateway {
    func loadProjection(momentId: String) async throws -> OperatingProjectionPayload
    func recordEvent(_ input: OperatingEventInput) async throws -> RecordOperatingEventResult
    func decideEvent(momentId: String, eventId: String, decision: String) async throws -> RecordOperatingEventResult
    func listMasters(companyId: String, kind: String?, momentId: String?) async throws -> [OperatingMasterPayload]
    func createMaster(companyId: String, body: OperatingMasterCreateBody) async throws -> OperatingMasterPayload
    func updateMaster(companyId: String, masterId: String, body: OperatingMasterUpdateBody) async throws -> OperatingMasterPayload
    func archiveMaster(companyId: String, masterId: String, expectedVersion: Int) async throws -> OperatingMasterPayload
    func listPriceLists(companyId: String, momentId: String?) async throws -> [OperatingPriceListPayload]
    func createPriceList(companyId: String, body: OperatingPriceListCreateBody) async throws -> OperatingPriceListPayload
    func loadDocument(momentId: String, eventId: String, kind: String) async throws -> OperatingDocumentPayload
}

/// What a capture sheet collects before it becomes an `operating_event`. Kept as plain data so
/// the mapping into the wire body can be unit tested without a composition.
struct OperatingEventInput {
    var momentId: String
    var eventType: String
    var title: String
    var amount: String? = nil
    var note: String? = nil
    var channelName: String? = nil
    var counterpartyName: String? = nil
    var occurredAt: String? = nil
    var parentEventId: String? = nil
    var quantity: String? = nil
    var unit: String? = nil
    var dueAt: String? = nil
    var masterKind: String? = nil
    /// Links the line to an existing `operating_master` instead of re-declaring the name.
    var masterId: String? = nil
    var lineItems: [OperatingLineItem]? = nil
    var contract: OperatingContractInput? = nil
    var paymentStatus: String? = nil
    var revisesEventId: String? = nil

    struct OperatingLineItem {
        var itemName: String
        var quantity: String? = nil
        var unit: String? = nil
        var unitPrice: String? = nil
        var masterId: String? = nil
    }
}

final class BusinessOperatingRepository: BusinessOperatingGateway {
    private let client: APIClient
    private let idempotency: IdempotencyKeyStore

    init(client: APIClient = .shared, idempotency: IdempotencyKeyStore = IdempotencyKeyStore()) {
        self.client = client
        self.idempotency = idempotency
    }

    func loadProjection(momentId: String) async throws -> OperatingProjectionPayload {
        try await client.getOperatingMoment(momentId: momentId)
    }

    func recordEvent(_ input: OperatingEventInput) async throws -> RecordOperatingEventResult {
        // The draft key is the event type plus the record it revises: retrying the same
        // capture reuses the key, a genuinely new capture gets a fresh one.
        let draftKey = "operating:\(input.eventType):\(input.revisesEventId ?? UUID().uuidString)"
        let key = idempotency.keyFor(draftKey: draftKey)
        let result = try await client.recordOperatingEvent(
            momentId: input.momentId,
            eventType: input.eventType,
            title: input.title,
            amount: input.amount,
            note: input.note,
            channelName: input.channelName,
            counterpartyName: input.counterpartyName,
            paymentStatus: input.paymentStatus,
            occurredAt: input.occurredAt,
            parentEventId: input.parentEventId,
            quantity: input.quantity,
            unit: input.unit,
            itemName: nil,
            unitPrice: nil,
            masterKind: input.masterKind,
            masterId: input.masterId,
            revisesEventId: input.revisesEventId,
            dueAt: input.dueAt,
            contract: input.contract,
            lineItems: input.lineItems?.map {
                (itemName: $0.itemName, quantity: $0.quantity, unit: $0.unit, unitPrice: $0.unitPrice, masterId: $0.masterId)
            },
            idempotencyKey: key
        )
        idempotency.clear(draftKey: draftKey)
        return RecordOperatingEventResult(eventId: result.eventId, approvalStatus: result.approvalStatus)
    }

    func decideEvent(momentId: String, eventId: String, decision: String) async throws -> RecordOperatingEventResult {
        try await client.decideOperatingEvent(momentId: momentId, eventId: eventId, decision: decision)
    }

    func listMasters(companyId: String, kind: String? = nil, momentId: String? = nil) async throws -> [OperatingMasterPayload] {
        try await client.listOperatingMasters(companyId: companyId, kind: kind, momentId: momentId).items
    }

    func createMaster(companyId: String, body: OperatingMasterCreateBody) async throws -> OperatingMasterPayload {
        let draftKey = "master-create:\(companyId):\(body.kind):\(body.name.lowercased())"
        let key = idempotency.keyFor(draftKey: draftKey)
        let created = try await client.createOperatingMaster(companyId: companyId, body: body, idempotencyKey: key)
        idempotency.clear(draftKey: draftKey)
        return created
    }

    func updateMaster(companyId: String, masterId: String, body: OperatingMasterUpdateBody) async throws -> OperatingMasterPayload {
        let key = idempotency.keyFor(draftKey: "master-update:\(masterId):\(body.expectedVersion)")
        let updated = try await client.updateOperatingMaster(
            companyId: companyId,
            masterId: masterId,
            body: body,
            idempotencyKey: key
        )
        idempotency.clear(draftKey: "master-update:\(masterId):\(body.expectedVersion)")
        return updated
    }

    func archiveMaster(companyId: String, masterId: String, expectedVersion: Int) async throws -> OperatingMasterPayload {
        let draftKey = "master-archive:\(masterId):\(expectedVersion)"
        let key = idempotency.keyFor(draftKey: draftKey)
        let archived = try await client.archiveOperatingMaster(
            companyId: companyId,
            masterId: masterId,
            expectedVersion: expectedVersion,
            idempotencyKey: key
        )
        idempotency.clear(draftKey: draftKey)
        return archived
    }

    func listPriceLists(companyId: String, momentId: String? = nil) async throws -> [OperatingPriceListPayload] {
        try await client.listOperatingPriceLists(companyId: companyId, momentId: momentId).items
    }

    func createPriceList(companyId: String, body: OperatingPriceListCreateBody) async throws -> OperatingPriceListPayload {
        let draftKey = "price-list-create:\(companyId):\(body.name.lowercased()):\(body.validFrom)"
        let key = idempotency.keyFor(draftKey: draftKey)
        let created = try await client.createOperatingPriceList(companyId: companyId, body: body, idempotencyKey: key)
        idempotency.clear(draftKey: draftKey)
        return created
    }

    func loadDocument(momentId: String, eventId: String, kind: String) async throws -> OperatingDocumentPayload {
        try await client.getOperatingDocument(momentId: momentId, eventId: eventId, kind: kind)
    }
}
