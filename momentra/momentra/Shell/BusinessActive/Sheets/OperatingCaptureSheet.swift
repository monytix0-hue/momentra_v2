import SwiftUI

/// One capture sheet for every `TRD-QA-01..12` / `DST-QA-01..12` frame, plus `HOLD`,
/// `TRANSFER`, and `REPRINT`.
///
/// The layout is chosen from the capture mode; behaviour and validation stay here so a new
/// field never has to be added twice. Dates reuse `SetupDateTimeField` rather than a second
/// picker implementation.
struct OperatingCaptureSheet: View {
    let theme: OperatingTheme
    let mode: String
    @ObservedObject var model: BusinessOperatingModel

    @Environment(\.dismiss) private var dismiss
    @State private var draft = Draft()
    @State private var saving = false
    @State private var document: OperatingDocumentPayload?
    @State private var documentError: String?

    /// The twelve frames collect the same primitives; which ones are visible depends on the mode.
    struct Draft {
        var title = ""
        var amount = ""
        var note = ""
        var counterparty = ""
        var channel = ""
        var quantity = ""
        var unit = ""
        var sku = ""
        var masterId: String?
        var occurredAt: String? = ISO8601DateFormatter().string(from: Date())
        var dueAt: String?
        var paymentStatus = "UNPAID"
        var stockAction = "IN"
        var stockReason = ""
        var ownership = "Owned"
        var holdTarget = "Order"
        var holdChoice = OperatingCaptureLayout.holdChoices[0]
        var holdReason = ""
        var transferFromId: String?
        var transferToId: String?
        var reprintKind = "INVOICE"
        var reprintSourceId = ""
        var revisionKind = ""
        var itemName = ""
        var itemQuantity = ""
        var itemUnit = ""
        var itemPrice = ""
    }

    private var layout: OperatingCaptureLayout? {
        OperatingCaptureLayout.forMode(mode)
    }

    private var displayTitle: String {
        switch mode.uppercased() {
        case OperatingCaptureLayout.holdMode: return "Hold a record"
        case OperatingCaptureLayout.transferMode: return "Transfer stock"
        case OperatingCaptureLayout.reprintMode: return "Reprint a document"
        default: return layout?.label ?? mode.capitalized
        }
    }

    private var qaCode: String {
        switch mode.uppercased() {
        case OperatingCaptureLayout.holdMode: return "QA-HOLD"
        case OperatingCaptureLayout.transferMode: return "QA-TRANSFER"
        case OperatingCaptureLayout.reprintMode: return "QA-REPRINT"
        default: return layout?.qaCode ?? "QA"
        }
    }

    private var needsAmount: Bool {
        guard let layout else { return false }
        return [.sale, .purchase, .collection, .payment, .spend, .budget, .approval].contains(layout)
    }

    private var amountLabel: String {
        switch layout {
        case .collection: return "Collected"
        case .payment: return "Paid"
        case .spend: return "Spent"
        case .budget: return "Budget"
        case .sale, .saleReturn: return "Value"
        case .purchase: return "Cost"
        default: return "Amount"
        }
    }

    private var canSave: Bool {
        switch mode.uppercased() {
        case OperatingCaptureLayout.reprintMode: return !draft.reprintSourceId.isEmpty
        case OperatingCaptureLayout.holdMode:
            // A hold is a revision of a record that already exists — never a new one.
            return !draft.reprintSourceId.isEmpty
                && !draft.holdReason.trimmingCharacters(in: .whitespaces).isEmpty
        case OperatingCaptureLayout.transferMode:
            // Two ends are mandatory and they cannot be the same place: a transfer that does not
            // say where stock left and where it landed is not a movement.
            return !draft.transferFromId.isNilOrEmpty
                && !draft.transferToId.isNilOrEmpty
                && draft.transferFromId != draft.transferToId
                && !draft.quantity.trimmingCharacters(in: .whitespaces).isEmpty
        default: return !draft.title.trimmingCharacters(in: .whitespaces).isEmpty && (!needsAmount || !draft.amount.isEmpty)
        }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                HStack(spacing: 8) {
                    Text(displayTitle)
                        .font(.plusJakarta(size: 22, weight: .semibold))
                        .foregroundStyle(theme.ink)
                    Spacer()
                    OperatingPill(theme: theme, label: qaCode, accent: true)
                }

                if mode.uppercased() == OperatingCaptureLayout.reprintMode {
                    reprintBody
                } else if mode.uppercased() == OperatingCaptureLayout.transferMode {
                    transferBody
                } else if mode.uppercased() == OperatingCaptureLayout.holdMode {
                    holdBody
                } else {
                    standardBody
                }

                if let error = documentError ?? model.writeError {
                    Text(error)
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(theme.negative)
                }
                saveButton
                OperatingWhyFootnote(theme: theme, text: afterSaveNote)
            }
            .padding(18)
        }
        .background(theme.canvas.ignoresSafeArea())
    }

    // MARK: - Standard layouts

    @ViewBuilder
    private var standardBody: some View {
        field(titleLabel, text: $draft.title)
        if needsAmount {
            field(amountLabel, text: $draft.amount, decimal: true)
        }
        if layout != .budget {
            field(counterpartyLabel, text: $draft.counterparty)
        }
        if [.sale, .purchase, .stock, .saleReturn, .order, .shipment].contains(layout) {
            field("Channel (optional)", text: $draft.channel)
        }
        if [.sale, .purchase, .stock, .saleReturn, .order, .shipment].contains(layout) {
            skuPicker
            HStack(spacing: 10) {
                field("Quantity", text: $draft.quantity, decimal: true)
                field("Unit", text: $draft.unit)
            }
        }
        if layout == .stock {
            segmented("Stock action", options: ["IN", "OUT", "TRANSFER"], selection: $draft.stockAction)
            field("Reason", text: $draft.stockReason)
        }
        if layout == .collection || layout == .payment {
            segmented("Payment", options: ["PAID", "PARTIAL", "UNPAID"], selection: $draft.paymentStatus)
        }
        if layout == .update {
            field("What changed", text: $draft.revisionKind)
        }
        if layout == .order || layout == .shipment || layout == .collection || layout == .payment {
            setupDateField("Due", selection: $draft.dueAt)
        }
        setupDateField("Recorded at", selection: $draft.occurredAt)
        field("Note (optional)", text: $draft.note)
    }

    private var titleLabel: String {
        switch layout {
        case .sale: return "What sold"
        case .purchase: return "What was bought"
        case .collection: return "Collected from"
        case .payment: return "Paid to"
        case .spend: return "What was spent on"
        case .stock: return "Stock item"
        case .saleReturn: return "What came back"
        case .budget: return "Budget line"
        case .update: return "What is being updated"
        case .approval: return "What needs approval"
        case .order: return "Order"
        case .shipment: return "Delivery"
        case nil: return "What happened"
        }
    }

    private var counterpartyLabel: String {
        switch layout {
        case .purchase: return "Supplier"
        case .collection: return "Buyer"
        case .payment: return "Payee"
        case .order, .shipment: return model.vertical == .trading ? "Buyer" : "Dealer"
        default: return model.vertical == .trading ? "Buyer (optional)" : "Dealer (optional)"
        }
    }

    /// The display name behind a chosen master id, so the record reads in words while the
    /// contract still carries the id the server validates.
    private func masterName(_ id: String?) -> String? {
        guard let id, !id.isEmpty else { return nil }
        return model.masters.first { $0.id == id }?.name
    }

    private var skuPicker: some View {
        let skus = model.masters.filter { ["SKU", "PRODUCT"].contains($0.kind.uppercased()) }
        return Group {
            if !skus.isEmpty {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Product")
                        .font(.plusJakarta(size: 11))
                        .foregroundStyle(theme.muted)
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(skus) { sku in
                                Button {
                                    draft.masterId = sku.id
                                    draft.sku = sku.name
                                    if draft.unit.isEmpty { draft.unit = sku.unit ?? "" }
                                    if draft.itemPrice.isEmpty { draft.itemPrice = sku.sellPrice ?? "" }
                                } label: {
                                    OperatingPill(
                                        theme: theme,
                                        label: sku.name,
                                        accent: draft.masterId == sku.id
                                    )
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                }
            }
        }
    }

    // MARK: - HOLD

    @ViewBuilder
    private var holdBody: some View {
        segmented("Hold", options: ["Order", "Dispatch"], selection: $draft.holdTarget)
        segmented("State", options: OperatingCaptureLayout.holdChoices, selection: $draft.holdChoice)
        let openType = draft.holdTarget == "Dispatch" ? "SHIPMENT" : "ORDER"
        let open = (model.projection?.activity ?? []).filter { $0.eventType == openType }
        if open.isEmpty {
            Text("Nothing open to hold on this moment.")
                .font(.plusJakarta(size: 12))
                .foregroundStyle(theme.warning)
        } else {
            Text("Open \(openType.lowercased())s")
                .font(.plusJakarta(size: 12))
                .foregroundStyle(theme.body)
            ForEach(open.prefix(8)) { row in
                Button {
                    draft.reprintSourceId = row.id
                    draft.title = row.title
                } label: {
                    VStack(alignment: .leading, spacing: 3) {
                        Text(row.title)
                            .font(.plusJakarta(size: 14, weight: .medium))
                            .foregroundStyle(theme.ink)
                        Text([row.counterpartyName, row.status].compactMap { $0 }.joined(separator: " · "))
                            .font(.plusJakarta(size: 12))
                            .foregroundStyle(theme.body)
                    }
                    .padding(10)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(draft.reprintSourceId == row.id ? theme.accent.opacity(0.14) : theme.inset)
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(draft.reprintSourceId == row.id ? theme.accent : theme.border, lineWidth: 1)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                .buttonStyle(.plain)
            }
        }
        field("Hold reason", text: $draft.holdReason)
    }

    // MARK: - TRANSFER

    @ViewBuilder
    private var transferBody: some View {
        skuPicker
        field("Item", text: $draft.title)
        let locations = model.masters.filter {
            ["WAREHOUSE", "LOCATION", "ROUTE"].contains($0.kind.uppercased())
        }
        if locations.isEmpty {
            Text("Add a second warehouse or route in Masters before moving stock.")
                .font(.plusJakarta(size: 12))
                .foregroundStyle(theme.warning)
        } else {
            locationPicker(label: "From", locations: locations, selection: $draft.transferFromId)
            locationPicker(label: "To", locations: locations, selection: $draft.transferToId)
        }
        HStack(spacing: 10) {
            field("Quantity", text: $draft.quantity, decimal: true)
            field("Unit", text: $draft.unit)
        }
        segmented("Reason", options: ["Stocktake", "Replenishment", "Damage", "Correction"], selection: $draft.stockReason)
        segmented("Ownership", options: ["Owned", "Consignment"], selection: $draft.ownership)
        Text("A transfer moves stock between two places. Total on-hand does not change.")
            .font(.plusJakarta(size: 12))
            .foregroundStyle(theme.body)
    }

    /// Two places, one movement. The same master cannot be both ends, so the destination list
    /// hides whatever is already selected as the source.
    private func locationPicker(
        label: String,
        locations: [OperatingMasterPayload],
        selection: Binding<String?>
    ) -> some View {
        let other: String? = label == "From" ? draft.transferToId : draft.transferFromId
        let available = locations.filter { $0.id != other }
        return VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.plusJakarta(size: 11))
                .foregroundStyle(theme.muted)
            if available.isEmpty {
                Text("Add a second warehouse or route to move stock between them.")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(theme.warning)
            } else {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(available) { location in
                            Button {
                                selection.wrappedValue = selection.wrappedValue == location.id ? nil : location.id
                            } label: {
                                OperatingPill(
                                    theme: theme,
                                    label: location.name,
                                    accent: selection.wrappedValue == location.id
                                )
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
            }
        }
    }

    // MARK: - REPRINT

    @ViewBuilder
    private var reprintBody: some View {
        segmented("Document", options: ["Invoice", "Challan"], selection: $draft.reprintKind)
        let sources = (model.projection?.activity ?? []).filter {
            ["SALE", "PURCHASE", "SERVICE"].contains($0.eventType)
        }
        if sources.isEmpty {
            Text("Nothing recorded to reprint on this moment.")
                .font(.plusJakarta(size: 12))
                .foregroundStyle(theme.warning)
        } else {
            Text("Source record")
                .font(.plusJakarta(size: 12))
                .foregroundStyle(theme.body)
            ForEach(sources.prefix(10)) { row in
                Button {
                    draft.reprintSourceId = row.id
                    draft.title = row.title
                    Task {
                        documentError = nil
                        document = await model.document(
                            eventId: row.id,
                            kind: draft.reprintKind == "Challan" ? "CHALLAN" : "INVOICE"
                        )
                        if document == nil { documentError = model.writeError }
                    }
                } label: {
                    VStack(alignment: .leading, spacing: 3) {
                        Text(row.title)
                            .font(.plusJakarta(size: 14, weight: .medium))
                            .foregroundStyle(theme.ink)
                        Text(
                            [
                                row.counterpartyName,
                                row.amount.map { OperatingMoneyFormat.format(model.currency, $0) },
                                String(row.occurredAt.prefix(10)),
                            ].compactMap { $0 }.joined(separator: " · ")
                        )
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(theme.body)
                    }
                    .padding(10)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(draft.reprintSourceId == row.id ? theme.accent.opacity(0.14) : theme.inset)
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(draft.reprintSourceId == row.id ? theme.accent : theme.border, lineWidth: 1)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                .buttonStyle(.plain)
            }
        }
        if let document {
            OperatingCard(theme: theme) {
                Text(document.documentNumber.isEmpty ? document.kind.capitalized : document.documentNumber)
                    .font(.plusJakarta(size: 15, weight: .semibold))
                    .foregroundStyle(theme.ink)
                if let buyer = document.buyer?.name, !buyer.isEmpty {
                    Text("To \(buyer)")
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(theme.body)
                }
                let lines = document.lines ?? []
                if !lines.isEmpty {
                    Text(lines.count == 1 ? "1 line" : "\(lines.count) lines")
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(theme.body)
                }
                if let total = document.grandTotal, !total.isEmpty {
                    Text("Total \(OperatingMoneyFormat.format(document.currency, total))")
                        .font(.plusJakarta(size: 13, weight: .semibold))
                        .foregroundStyle(theme.accent)
                }
                Text("Reprint reads the record. Nothing is posted.")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(theme.muted)
            }
        }
    }

    // MARK: - Save

    private var saveButton: some View {
        Button {
            Task { await save() }
        } label: {
            Text(saveLabel)
                .font(.plusJakarta(size: 14, weight: .semibold))
                .foregroundStyle(theme.onAccent)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 13)
                .background(canSave ? theme.accent : theme.accent.opacity(0.4))
                .clipShape(RoundedRectangle(cornerRadius: 13))
        }
        .buttonStyle(.plain)
        .disabled(saving || !canSave)
    }

    private var saveLabel: String {
        if saving { return "Saving…" }
        switch mode.uppercased() {
        case OperatingCaptureLayout.reprintMode: return "Load document"
        case OperatingCaptureLayout.holdMode: return draft.holdChoice == "No hold" ? "Release hold" : "Hold record"
        default: return "Record"
        }
    }

    private var afterSaveNote: String {
        mode.uppercased() == OperatingCaptureLayout.reprintMode
            ? "A reprint is a read. The invoice or challan is drawn on device from recorded data."
            : "One capture posts one event. Nothing here rewrites an approved record."
    }

    private func save() async {
        switch mode.uppercased() {
        case OperatingCaptureLayout.reprintMode:
            guard !draft.reprintSourceId.isEmpty else { return }
            documentError = nil
            document = await model.document(
                eventId: draft.reprintSourceId,
                kind: draft.reprintKind == "Challan" ? "CHALLAN" : "INVOICE"
            )
            if document == nil { documentError = model.writeError }
            return
        case OperatingCaptureLayout.holdMode:
            guard !draft.reprintSourceId.isEmpty else { return }
            saving = true
            defer { saving = false }
            // A hold revises the record it was placed on, so the event type comes from that row
            // rather than the segmented control — the two cannot drift apart.
            let source = (model.projection?.activity ?? []).first { $0.id == draft.reprintSourceId }
            let ok = await model.record(
                OperatingEventInput(
                    momentId: model.momentId,
                    eventType: source?.eventType
                        ?? (draft.holdTarget == "Dispatch" ? "SHIPMENT" : "ORDER"),
                    title: source?.title ?? (draft.title.isEmpty ? "Hold" : draft.title),
                    contract: holdContract(),
                    revisesEventId: draft.reprintSourceId
                )
            )
            if ok { dismiss() }
            return
        default:
            saving = true
            defer { saving = false }
            let ok = await model.record(eventInput())
            if ok { dismiss() }
        }
    }

    private func holdContract() -> OperatingContractInput {
        var contract = OperatingContractInput()
        contract.holdStatus = OperatingContractVocabulary.holdStatus(draft.holdChoice)
        // A release carries no reason; a hold must, or the server rejects the write.
        let reason = draft.holdReason.trimmingCharacters(in: .whitespaces)
        contract.holdReason = reason.isEmpty ? nil : reason
        return contract
    }

    private func eventInput() -> OperatingEventInput {
        var contract = OperatingContractInput()
        var input = OperatingEventInput(
            momentId: model.momentId,
            eventType: eventType,
            title: draft.title.trimmingCharacters(in: .whitespaces),
            amount: draft.amount.isEmpty ? nil : draft.amount,
            note: draft.note.isEmpty ? nil : draft.note,
            channelName: draft.channel.isEmpty ? nil : draft.channel,
            counterpartyName: draft.counterparty.isEmpty ? nil : draft.counterparty,
            occurredAt: draft.occurredAt,
            quantity: draft.quantity.isEmpty ? nil : draft.quantity,
            unit: draft.unit.isEmpty ? nil : draft.unit,
            dueAt: draft.dueAt,
            masterKind: nil,
            masterId: draft.masterId
        )
        if layout == .collection || layout == .payment {
            input.paymentStatus = draft.paymentStatus
        }
        if layout == .stock {
            contract.stockAction = OperatingContractVocabulary.stockAction(draft.stockAction)
            contract.stockReason = OperatingContractVocabulary.stockReason(draft.stockReason)
            contract.ownership = OperatingContractVocabulary.ownership(draft.ownership)
        }
        if mode.uppercased() == OperatingCaptureLayout.transferMode {
            contract.stockAction = "TRANSFER"
            contract.transferFromMasterId = draft.transferFromId
            contract.transferToMasterId = draft.transferToId
            contract.transferFromName = masterName(draft.transferFromId)
            contract.transferToName = masterName(draft.transferToId)
            contract.stockReason = OperatingContractVocabulary.stockReason(draft.stockReason) ?? "TRANSFER"
            contract.ownership = OperatingContractVocabulary.ownership(draft.ownership)
        }
        if !draft.masterId.isNilOrEmpty {
            input.lineItems = [
                .init(
                    itemName: draft.sku.isEmpty ? draft.title : draft.sku,
                    quantity: draft.quantity.isEmpty ? nil : draft.quantity,
                    unit: draft.unit.isEmpty ? nil : draft.unit,
                    unitPrice: draft.itemPrice.isEmpty ? nil : draft.itemPrice,
                    masterId: draft.masterId
                ),
            ]
        }
        input.contract = contract
        return input
    }

    private var eventType: String {
        switch layout {
        case .sale: return "SALE"
        case .purchase: return "PURCHASE"
        case .collection: return "COLLECTION"
        case .payment: return "PAYMENT"
        case .spend: return "SPEND"
        case .stock: return "STOCK"
        case .saleReturn: return "RETURN"
        case .budget: return "BUDGET"
        case .update: return "UPDATE"
        case .approval: return "APPROVAL"
        case .order: return "ORDER"
        case .shipment: return "SHIPMENT"
        case nil: return "UPDATE"
        }
    }

    // MARK: - Field primitives

    private func field(_ label: String, text: Binding<String>, decimal: Bool = false) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.plusJakarta(size: 11))
                .foregroundStyle(theme.muted)
            TextField("", text: text)
                .keyboardType(decimal ? .decimalPad : .default)
                .font(.plusJakarta(size: 14))
                .foregroundStyle(theme.ink)
                .padding(.horizontal, 12)
                .padding(.vertical, 11)
                .background(theme.inset)
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(theme.border, lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: 12))
        }
    }

    private func segmented(_ label: String, options: [String], selection: Binding<String>) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.plusJakarta(size: 11))
                .foregroundStyle(theme.muted)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(options, id: \.self) { option in
                        Button {
                            selection.wrappedValue = option
                        } label: {
                            OperatingPill(
                                theme: theme,
                                label: option,
                                accent: selection.wrappedValue == option
                            )
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
    }

    private func setupDateField(_ label: String, selection: Binding<String?>) -> some View {
        SetupDateTimeField(label: label, isoValue: selection)
    }
}

private extension Optional where Wrapped == String {
    var isNilOrEmpty: Bool {
        guard let value = self else { return true }
        return value.isEmpty
    }
}
