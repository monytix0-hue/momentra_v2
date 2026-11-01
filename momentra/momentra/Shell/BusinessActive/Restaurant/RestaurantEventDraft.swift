import Foundation

/// Restaurant-side capture draft.
///
/// Carries exactly the fields the shared `OperatingCaptureSheet` produces plus the
/// itemized `lines[]`, so `POST …/operating-events` keeps its existing body shape.
/// Declared once here and used by the hub, the 10 capture sheets, POS, KOT and masters.
struct RestaurantEventDraft {
    var eventType: String
    var title: String
    var amount: String?
    var note: String?
    var channelName: String?
    var counterpartyName: String?
    var paymentStatus: String?
    var occurredAt: String? = nil
    var quantity: String? = nil
    var unit: String? = nil
    var unitPrice: String? = nil
    var itemName: String? = nil
    var parentEventId: String? = nil
    var revisesEventId: String? = nil
    var masterKind: String? = nil
    var dueAt: String? = nil
    var contract: OperatingContractInput? = nil
    var lineItems: [(itemName: String, quantity: String?, unit: String?, unitPrice: String?, masterId: String?)]? = nil

    init(
        eventType: String,
        title: String,
        amount: String?,
        note: String? = nil,
        channelName: String? = nil,
        counterpartyName: String? = nil,
        paymentStatus: String? = nil,
        occurredAt: String? = nil,
        quantity: String? = nil,
        unit: String? = nil,
        unitPrice: String? = nil,
        itemName: String? = nil,
        parentEventId: String? = nil,
        revisesEventId: String? = nil,
        masterKind: String? = nil,
        dueAt: String? = nil,
        contract: OperatingContractInput? = nil,
        lineItems: [(itemName: String, quantity: String?, unit: String?, unitPrice: String?, masterId: String?)]? = nil
    ) {
        self.eventType = eventType
        self.title = title
        self.amount = amount
        self.note = note
        self.channelName = channelName
        self.counterpartyName = counterpartyName
        self.paymentStatus = paymentStatus
        self.occurredAt = occurredAt
        self.quantity = quantity
        self.unit = unit
        self.unitPrice = unitPrice
        self.itemName = itemName
        self.parentEventId = parentEventId
        self.revisesEventId = revisesEventId
        self.masterKind = masterKind
        self.dueAt = dueAt
        self.contract = contract
        self.lineItems = lineItems
    }

    /// Lifts the shared capture sheet's draft across the module boundary unchanged.
    init(_ draft: OperatingCaptureDraft) {
        self.init(
            eventType: draft.eventType,
            title: draft.title,
            amount: draft.amount,
            note: draft.note,
            channelName: draft.channelName,
            counterpartyName: draft.counterpartyName,
            paymentStatus: draft.paymentStatus,
            occurredAt: draft.occurredAt,
            quantity: draft.quantity,
            unit: draft.unit,
            unitPrice: draft.unitPrice,
            itemName: draft.itemName,
            parentEventId: draft.parentEventId,
            revisesEventId: draft.revisesEventId,
            masterKind: draft.masterKind,
            dueAt: draft.dueAt,
            contract: draft.contract,
            lineItems: draft.lineItems
        )
    }
}
