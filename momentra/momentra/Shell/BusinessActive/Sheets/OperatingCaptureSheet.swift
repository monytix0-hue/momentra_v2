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
    /// Master chosen from Parts/Materials browser — applied once on appear.
    var browserPrefill: OperatingBrowserItem? = nil
    /// Trading mode chips that leave this sheet (Return/Reprint/Transfer/Delivery/Approval/Hold DO/Reimburse).
    var onRouteToMode: ((String) -> Void)? = nil

    @Environment(\.dismiss) private var dismiss
    @State private var draft = Draft()
    @State private var saving = false
    @State private var document: OperatingDocumentPayload?
    @State private var documentError: String?
    @State private var didApplyBrowserPrefill = false
    @State private var moreDetailsOpen = false

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
        var parentEventId: String?
        var itemName = ""
        var itemQuantity = ""
        var itemUnit = ""
        var itemPrice = ""
        // QA-02/03/04 — Callers: OperatingQuickAddHubView / RetailFamily sheets present this
        // capture for SALE/ORDER/COLLECTION. Existing file; schemas: OperatingContractInput
        // (quotationStatus, paymentState, goodsStatus, orderSide, entryMode) + Draft locals.
        // Instruction: implement Pass 2 QA-02–05 and Pass 3 CF invariants across platforms.
        var documentType = "Sales order"
        var entryMode = "Quick Sale"
        var paymentChoice = "Fully Paid"
        var amountPaidNow = ""
        var goodsStatus = "Deliver Later"
        var quoteStatus = "Sent"
    }

    private var layout: OperatingCaptureLayout? {
        OperatingCaptureLayout.forMode(mode)
    }

    private var displayTitle: String {
        if theme.vertical == .consumerService, ServiceCaptureCopy.serviceModes.contains(mode.uppercased()) {
            return ServiceCaptureCopy.sheetTitle(mode)
        }
        // Retail money QAs normally open `RetailCaptureSheet`; keep copy parity if this host is hit.
        if theme.vertical == .retailStore, RetailCaptureCopy.moneyModes.contains(mode.uppercased()) {
            return RetailCaptureCopy.sheetTitle(mode)
        }
        switch mode.uppercased() {
        case OperatingCaptureLayout.holdMode: return "Hold a record"
        case OperatingCaptureLayout.transferMode: return "Transfer stock"
        case OperatingCaptureLayout.reprintMode: return "Reprint a document"
        case "SERVICE": return "Finalize Charge"
        case "APPOINTMENT", "BOOKING": return "Booking"
        case "STATUS": return "Status / Assignment"
        case "CONFIRM": return "Accept / Confirm Order"
        case "QUOTE_REVISE": return "Quote revise / expire"
        case "OPEN_JOB": return "Open Production Job"
        case "JOB_STATUS": return "Job status change"
        case "REWORK": return "Rework / Scrap / Job Work"
        case "PRODUCTION": return "Record Production"
        case "ORDER": return model.vertical.isMadeToOrder ? "Create Quotation" : (layout?.label ?? "Order")
        default: return layout?.label ?? mode.capitalized
        }
    }

    private var tradingContextLine: String? {
        guard theme.isOpsQaCapture else { return nil }
        if theme.isDistributionCapture {
            return DistributionQuickAddCopy.context(mode: mode, brand: model.title)
        }
        return TradingQuickAddCopy.context(mode: mode, brand: model.title)
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
        if theme.vertical == .consumerService, ServiceCaptureCopy.serviceModes.contains(mode.uppercased()) {
            return ServiceCaptureCopy.amountLabel(mode)
        }
        if theme.vertical == .retailStore, RetailCaptureCopy.moneyModes.contains(mode.uppercased()) {
            return RetailCaptureCopy.amountLabel(mode)
        }
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
        case "CONFIRM":
            return !draft.counterparty.trimmingCharacters(in: .whitespaces).isEmpty
                && !draft.quantity.trimmingCharacters(in: .whitespaces).isEmpty
        case "QUOTE_REVISE":
            return !(draft.parentEventId ?? "").isEmpty
                && !draft.counterparty.trimmingCharacters(in: .whitespaces).isEmpty
        case "OPEN_JOB", "JOB_STATUS":
            return !(draft.parentEventId ?? "").isEmpty
                && !draft.title.trimmingCharacters(in: .whitespaces).isEmpty
        case "REWORK":
            return !draft.quantity.trimmingCharacters(in: .whitespaces).isEmpty
                && !draft.title.trimmingCharacters(in: .whitespaces).isEmpty
        default:
            if theme.isOpsQaCapture {
                return tradingCanSave
            }
            if theme.vertical == .consumerService {
                return serviceCanSave
            }
            return !draft.title.trimmingCharacters(in: .whitespaces).isEmpty && (!needsAmount || !draft.amount.isEmpty)
        }
    }

    /// SVC amount-first sheets: gate on the Figma-primary field, not a trading-style title.
    private var serviceCanSave: Bool {
        switch mode.uppercased() {
        case "SERVICE", "PURCHASE", "VENDOR", "COLLECTION", "PAYMENT", "REIMBURSE", "SPEND",
             "APPOINTMENT", "BOOKING", "BUDGET", "TARGET":
            return !draft.amount.trimmingCharacters(in: .whitespaces).isEmpty
        case "STOCK":
            return !draft.quantity.trimmingCharacters(in: .whitespaces).isEmpty
                && !draft.title.trimmingCharacters(in: .whitespaces).isEmpty
        case "STATUS":
            return !draft.title.trimmingCharacters(in: .whitespaces).isEmpty
        case "ISSUE", "UPDATE", "APPROVAL":
            return !draft.title.trimmingCharacters(in: .whitespaces).isEmpty
        default:
            return !draft.title.trimmingCharacters(in: .whitespaces).isEmpty && (!needsAmount || !draft.amount.isEmpty)
        }
    }

    /// Trading TRD sheets derive title from counterparty / SKU / description — amount or qty is the gate.
    private var tradingCanSave: Bool {
        switch layout {
        case .sale, .purchase, .collection, .payment, .spend, .budget, .saleReturn:
            return !draft.amount.trimmingCharacters(in: .whitespaces).isEmpty
        case .stock, .shipment:
            return !draft.quantity.trimmingCharacters(in: .whitespaces).isEmpty
        case .update:
            return !draft.title.trimmingCharacters(in: .whitespaces).isEmpty
        case .approval:
            return !draft.title.trimmingCharacters(in: .whitespaces).isEmpty
        case .order:
            return !draft.counterparty.trimmingCharacters(in: .whitespaces).isEmpty
        case nil:
            return !draft.title.trimmingCharacters(in: .whitespaces).isEmpty
        }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                HStack(spacing: 8) {
                    Text(displayTitle)
                        .font(.plusJakarta(size: 22, weight: theme.isOpsQaCapture ? .bold : .semibold))
                        .foregroundStyle(theme.ink)
                    Spacer()
                    if theme.isOpsQaCapture {
                        Button { dismiss() } label: {
                            Text("✕")
                                .font(.plusJakarta(size: 14, weight: .bold))
                                .foregroundStyle(Color(hex: "#C9D2E3"))
                                .frame(width: 32, height: 32)
                                .background(theme.captureField)
                                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                        }
                        .buttonStyle(.plain)
                    } else {
                        OperatingPill(theme: theme, label: qaCode, accent: true)
                    }
                }

                if let tradingContextLine {
                    Text(tradingContextLine)
                        .font(.plusJakarta(size: 13, weight: .medium))
                        .foregroundStyle(theme.cyan)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }

                if theme.vertical == .consumerService {
                    serviceModeChips
                }

                if theme.vertical == .consumerService,
                   ServiceCaptureCopy.serviceModes.contains(mode.uppercased()),
                   !["HOLD", "TRANSFER", "REPRINT"].contains(mode.uppercased()) {
                    ServiceCaptureBody(
                        theme: theme,
                        mode: mode,
                        model: model,
                        draft: $draft,
                        moreDetailsOpen: $moreDetailsOpen
                    )
                } else if mode.uppercased() == OperatingCaptureLayout.reprintMode {
                    reprintBody
                } else if mode.uppercased() == OperatingCaptureLayout.transferMode {
                    transferBody
                } else if mode.uppercased() == OperatingCaptureLayout.holdMode {
                    holdBody
                } else if ["CONFIRM", "QUOTE_REVISE", "OPEN_JOB", "JOB_STATUS", "REWORK"].contains(mode.uppercased()) {
                    mtoPlantSpecialBody
                } else if theme.isOpsQaCapture {
                    tradingBody
                } else {
                    standardBody
                }

                if let error = documentError ?? model.writeError {
                    Text(error)
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(theme.negative)
                }
                if theme.isOpsQaCapture {
                    Text(tradingStickyMeta)
                        .font(.plusJakarta(size: 12, weight: .medium))
                        .foregroundStyle(theme.captureHint)
                }
                if theme.vertical == .consumerService,
                   ServiceCaptureCopy.serviceModes.contains(mode.uppercased()),
                   !["HOLD", "TRANSFER", "REPRINT"].contains(mode.uppercased()) {
                    serviceSaveFooter
                } else {
                    saveButton
                    OperatingWhyFootnote(theme: theme, text: afterSaveNote)
                }
            }
            .padding(18)
        }
        .background((theme.isOpsQaCapture ? theme.captureSheet : theme.canvas).ignoresSafeArea())
        .onAppear {
            if theme.vertical == .consumerService {
                applyServiceDefaults()
            }
            if theme.isOpsQaCapture {
                switch mode.uppercased() {
                case "SALE":
                    if draft.entryMode == "Quick Sale" { draft.entryMode = "Quick amount" }
                    if draft.paymentChoice == "Fully Paid" { draft.paymentChoice = "Received" }
                case "PURCHASE":
                    if draft.entryMode == "Quick Sale" || draft.entryMode == "Quick amount" { draft.entryMode = "Quick cost" }
                    if draft.paymentChoice == "Fully Paid" || draft.paymentChoice == "Received" { draft.paymentChoice = "Pay later" }
                    if draft.goodsStatus == "Deliver Later" { draft.goodsStatus = "Received now" }
                case "SPEND":
                    if draft.entryMode == "Quick Sale" || draft.entryMode == "Quick amount" { draft.entryMode = "Quick" }
                    if draft.paymentChoice == "Fully Paid" || draft.paymentChoice == "Received" { draft.paymentChoice = "Paid" }
                case "STOCK":
                    if draft.stockAction == "IN" { draft.stockAction = "Set current" }
                case "ORDER":
                    if draft.documentType == "Sales order" { draft.documentType = "Customer order" }
                    if draft.documentType == "Purchase order" { draft.documentType = "Supplier order" }
                case "RETURN":
                    if draft.goodsStatus == "Deliver Later" { draft.goodsStatus = "Returned to stock" }
                case "SHIPMENT", "DELIVERY":
                    if draft.goodsStatus == "Deliver Later" { draft.goodsStatus = "Outgoing" }
                case "APPROVAL":
                    if draft.paymentChoice == "Fully Paid" || draft.paymentChoice == "Received" { draft.paymentChoice = "Approve" }
                case "BUDGET", "TARGET":
                    if draft.revisionKind.isEmpty { draft.revisionKind = "Budget" }
                default: break
                }
            }
            applyBrowserPrefillIfNeeded()
        }
    }

    // MARK: - Trading TRD-QA body (Figma 01…12)

    @ViewBuilder
    private var tradingBody: some View {
        switch layout {
        case .sale:
            tradingSaleBody
        case .purchase:
            tradingPurchaseBody
        case .collection:
            TradingAmountHero(theme: theme, label: "Collection amount (M)", currency: model.currency, text: $draft.amount, hint: "≤ outstanding")
            field("Customer (M)", text: $draft.counterparty)
            collectionContinuityBlock
            setupDateField("Collection date (M)", selection: $draft.occurredAt)
            field("Method (O)", text: $draft.channel)
            TradingMoreDetails(theme: theme, open: $moreDetailsOpen, note: $draft.note)
        case .payment:
            TradingAmountHero(theme: theme, label: "Payment amount (M)", currency: model.currency, text: $draft.amount, hint: "≤ outstanding")
            field("Supplier / payee (M)", text: $draft.counterparty)
            field("Eligible payable (M)", text: $draft.title)
            setupDateField("Payment date (M)", selection: $draft.occurredAt)
            field("Method (O)", text: $draft.channel)
            TradingMoreDetails(theme: theme, open: $moreDetailsOpen, note: $draft.note)
        case .spend:
            segmented("", options: ["Quick", "Detailed"], selection: $draft.entryMode)
            TradingAmountHero(theme: theme, label: "Amount (M)", currency: model.currency, text: $draft.amount, hint: ">0")
            field("Expense type (M)", text: $draft.title)
            setupDateField("Date (M)", selection: $draft.occurredAt)
            segmented("Payment state (M)", options: ["Paid", "Part paid", "Pay later"], selection: $draft.paymentChoice)
            if draft.paymentChoice != "Paid" {
                field("Payee (C · if payable)", text: $draft.counterparty)
                setupDateField("Due date (C)", selection: $draft.dueAt, danger: true)
            }
            if draft.entryMode.localizedCaseInsensitiveContains("Detail") {
                field("Purpose", text: $draft.revisionKind)
            }
            TradingMoreDetails(theme: theme, open: $moreDetailsOpen, note: $draft.note)
        case .stock:
            tradingStockBody
        case .saleReturn:
            segmented("", options: ["Customer return", "Supplier return"], selection: $draft.revisionKind)
            TradingAmountHero(theme: theme, label: "Credit amount (M)", currency: model.currency, text: $draft.amount, hint: "Within eligible")
            field(draft.revisionKind.localizedCaseInsensitiveContains("Supplier") ? "Source purchase (M)" : "Source sale (M)", text: $draft.title)
            field("Item / qty (C · itemized)", text: $draft.itemName)
            HStack(spacing: 10) {
                field("Quantity", text: $draft.quantity, decimal: true)
                field("Unit", text: $draft.unit)
            }
            segmented("Goods movement (M)", options: ["Returned to stock", "Pending inspection", "Not returned"], selection: $draft.goodsStatus)
            field("Reason (M)", text: $draft.stockReason)
            setupDateField("Return date (M)", selection: $draft.occurredAt)
            TradingMoreDetails(theme: theme, open: $moreDetailsOpen, note: $draft.note)
        case .budget:
            segmented("", options: ["Budget", "Sales target"], selection: $draft.revisionKind)
            TradingAmountHero(theme: theme, label: "New amount (M)", currency: model.currency, text: $draft.amount, hint: "≥ 0")
            field("Period (M)", text: $draft.title)
            setupDateField("Effective date (M)", selection: $draft.occurredAt)
            TradingMoreDetails(theme: theme, open: $moreDetailsOpen, note: $draft.note)
        case .update:
            tradingUpdateBody
        case .approval:
            tradingApprovalBody
        case .order:
            tradingOrderBody
        case .shipment:
            tradingShipmentBody
        case nil:
            standardBody
        }
    }

    @ViewBuilder
    private var tradingSaleBody: some View {
        segmented(
            "",
            options: ["Quick amount", "Itemized", "Return", "Reprint"],
            selection: Binding(
                get: { draft.entryMode },
                set: { picked in
                    if picked == "Return" { routeTradingChip(to: "RETURN"); return }
                    if picked == "Reprint" { routeTradingChip(to: "REPRINT"); return }
                    draft.entryMode = picked
                }
            )
        )
        let itemized = draft.entryMode.localizedCaseInsensitiveContains("Item")
        if !itemized {
            TradingAmountHero(theme: theme, label: "Sale amount (M)", currency: model.currency, text: $draft.amount, hint: ">0")
        } else {
            skuPicker
            HStack(spacing: 10) {
                field("Quantity", text: $draft.quantity, decimal: true)
                field("Unit", text: $draft.unit)
            }
            field("Unit price", text: $draft.itemPrice, decimal: true)
        }
        setupDateField("Sale date (M)", selection: $draft.occurredAt)
        segmented("Payment state (M)", options: ["Received", "Part paid", "Pay later"], selection: $draft.paymentChoice)
        if draft.paymentChoice == "Part paid" || draft.paymentChoice == "Pay later" {
            field(
                draft.paymentChoice == "Part paid" ? "Customer (C · part paid)" : "Customer (C · pay later)",
                text: $draft.counterparty
            )
        }
        if draft.paymentChoice == "Part paid" {
            field("Received now (C)", text: $draft.amountPaidNow, decimal: true)
        }
        if draft.paymentChoice != "Received" {
            setupDateField("Due date (C)", selection: $draft.dueAt, danger: true)
        }
        field("Channel (C · if enabled)", text: $draft.channel)
        TradingMoreDetails(theme: theme, open: $moreDetailsOpen, note: $draft.note)
    }

    @ViewBuilder
    private var tradingPurchaseBody: some View {
        segmented("", options: ["Quick cost", "Itemized"], selection: $draft.entryMode)
        let itemized = draft.entryMode.localizedCaseInsensitiveContains("Item")
        if !itemized {
            TradingAmountHero(theme: theme, label: "Purchase amount (M)", currency: model.currency, text: $draft.amount, hint: ">0")
        } else {
            skuPicker
            HStack(spacing: 10) {
                field("Quantity", text: $draft.quantity, decimal: true)
                field("Unit", text: $draft.unit)
            }
        }
        field("Supplier / supplier (C/M)", text: $draft.counterparty)
        setupDateField("Purchase date (M)", selection: $draft.occurredAt)
        segmented("Payment state (M)", options: ["Paid", "Part paid", "Pay later"], selection: $draft.paymentChoice)
        if draft.paymentChoice == "Part paid" {
            field("Paid now (C)", text: $draft.amountPaidNow, decimal: true)
        }
        segmented("Goods (M)", options: ["Received now", "Receive later"], selection: $draft.goodsStatus)
        TradingMoreDetails(theme: theme, open: $moreDetailsOpen, note: $draft.note)
    }

    @ViewBuilder
    private var tradingStockBody: some View {
        segmented(
            "",
            options: ["Set current", "Add", "Reduce", "Transfer"],
            selection: Binding(
                get: { draft.stockAction },
                set: { picked in
                    if picked == "Transfer" { routeTradingChip(to: "TRANSFER"); return }
                    draft.stockAction = picked
                }
            )
        )
        TradingAmountHero(
            theme: theme,
            label: "Quantity (M)",
            currency: "Qty · \(draft.unit.isEmpty ? "pc" : draft.unit)",
            text: $draft.quantity,
            hint: "Shows prior → delta → result",
            isQuantity: true
        )
        field("Item / SKU (M)", text: $draft.title)
        field("UOM (M)", text: $draft.unit)
        field("Location (C)", text: $draft.channel)
        field("Reason (M)", text: $draft.stockReason)
        setupDateField("Count date (M)", selection: $draft.occurredAt)
        TradingMoreDetails(theme: theme, open: $moreDetailsOpen, note: $draft.note)
    }

    @ViewBuilder
    private var tradingUpdateBody: some View {
        segmented(
            "",
            options: ["Update", "Issue", "Approval"],
            selection: Binding(
                get: { draft.revisionKind.isEmpty ? "Update" : draft.revisionKind },
                set: { picked in
                    if picked == "Approval" { routeTradingChip(to: "APPROVAL"); return }
                    draft.revisionKind = picked
                }
            )
        )
        field("Description (M)", text: $draft.title)
        parentPicker(
            label: "Linked source (O)",
            types: ["SALE", "PURCHASE", "ORDER", "CUSTOMER_ORDER", "SHIPMENT", "DELIVERY", "COLLECTION", "PAYMENT", "SPEND", "STOCK", "RETURN", "BUDGET"]
        )
        if draft.revisionKind == "Issue" {
            field("Owner (C · Issue)", text: $draft.counterparty)
            setupDateField("Due date (C · assigned)", selection: $draft.dueAt, danger: true)
        } else {
            setupDateField("Date", selection: $draft.occurredAt)
        }
        TradingMoreDetails(theme: theme, open: $moreDetailsOpen, note: $draft.note)
    }

    @ViewBuilder
    private var tradingApprovalBody: some View {
        segmented(
            "",
            options: ["Approve", "Reject", "Reimburse"],
            selection: Binding(
                get: { draft.paymentChoice },
                set: { picked in
                    if picked == "Reimburse" { routeTradingChip(to: "PAYMENT"); return }
                    draft.paymentChoice = picked
                }
            )
        )
        field("Linked request (M)", text: $draft.title)
        if draft.paymentChoice == "Reject" {
            field("Reason (C · reject)", text: $draft.note)
        }
        setupDateField("Decide by (C)", selection: $draft.dueAt, danger: true)
        field("Acting role (H)", text: $draft.counterparty)
        TradingMoreDetails(theme: theme, open: $moreDetailsOpen, note: $draft.revisionKind)
    }

    @ViewBuilder
    private var tradingOrderBody: some View {
        segmented(
            "",
            options: ["Customer order", "Supplier order", "Delivery"],
            selection: Binding(
                get: { draft.documentType },
                set: { picked in
                    if picked == "Delivery" { routeTradingChip(to: "SHIPMENT"); return }
                    draft.documentType = picked
                }
            )
        )
        field(
            draft.documentType.localizedCaseInsensitiveContains("Supplier") ? "Supplier (M · purchase order)" : "Customer (M · sales order)",
            text: $draft.counterparty
        )
        setupDateField("Order date (M)", selection: $draft.occurredAt)
        skuPicker
        HStack(spacing: 10) {
            field("Quantity", text: $draft.quantity, decimal: true)
            field("Unit", text: $draft.unit)
        }
        field("Unit price", text: $draft.itemPrice, decimal: true)
        setupDateField("Expected delivery", selection: $draft.dueAt)
        field("Status (H)", text: $draft.quoteStatus)
        field("Commitment amount", text: $draft.amount, decimal: true)
        TradingMoreDetails(theme: theme, open: $moreDetailsOpen, note: $draft.note)
    }

    @ViewBuilder
    private var tradingShipmentBody: some View {
        segmented(
            "",
            options: ["Outgoing", "Incoming", "Hold DO"],
            selection: Binding(
                get: { draft.goodsStatus },
                set: { picked in
                    if picked == "Hold DO" { routeTradingChip(to: "HOLD"); return }
                    draft.goodsStatus = picked
                }
            )
        )
        TradingAmountHero(
            theme: theme,
            label: "Qty delivered (M)",
            currency: "Qty · \(draft.unit.isEmpty ? "pc" : draft.unit)",
            text: $draft.quantity,
            hint: "≤ remaining",
            isQuantity: true
        )
        field(
            draft.goodsStatus.localizedCaseInsensitiveContains("Incoming") ? "Source purchase (M · incoming)" : "Source sale (M · outgoing)",
            text: $draft.title
        )
        field("Prefill remaining", text: $draft.itemName)
        setupDateField("Delivery date (M)", selection: $draft.occurredAt)
        field("Carrier / ref (O)", text: $draft.channel)
        TradingMoreDetails(theme: theme, open: $moreDetailsOpen, note: $draft.note)
    }

    private func routeTradingChip(to target: String) {
        guard theme.isOpsQaCapture else { return }
        if let onRouteToMode {
            onRouteToMode(target.uppercased())
        } else {
            dismiss()
        }
    }

    private var tradingStickyMeta: String {
        let currency = model.currency.isEmpty ? "INR" : model.currency
        let money: (String) -> String = { raw in
            guard !raw.isEmpty else { return "—" }
            return OperatingMoneyFormat.format(currency, raw, visible: true)
        }
        switch layout {
        case .sale:
            switch draft.paymentChoice {
            case "Received": return "Received · no open AR"
            case "Part paid":
                if let total = Double(draft.amount), let paid = Double(draft.amountPaidNow), total > paid {
                    return "Part paid · \(money(String(total - paid))) stays in AR"
                }
                return "Part paid · remaining stays in AR"
            default: return "Pay later · full amount stays in AR"
            }
        case .purchase:
            let goods = draft.goodsStatus.localizedCaseInsensitiveContains("later") ? "receive later" : "received now"
            return "\(money(draft.amount)) payable · goods \(goods)"
        case .collection:
            let shown = money(draft.amount)
            return "Allocated \(shown) of \(shown)"
        case .payment:
            let shown = money(draft.amount)
            return "Allocated \(shown) of \(shown)"
        case .spend: return draft.paymentChoice == "Paid" ? "Paid · posts expense + disbursement" : "Pay later · needs payee"
        case .stock:
            let qty = draft.quantity.isEmpty ? "—" : draft.quantity
            let unit = draft.unit.isEmpty ? "pc" : draft.unit
            return "Prior — → \(qty) → result \(unit)"
        case .saleReturn:
            let qty = draft.quantity.isEmpty ? "0" : draft.quantity
            return "Credit \(money(draft.amount)) · +\(qty) pc to stock"
        case .budget: return "v3 replaces v2 · prior kept"
        case .update: return "Adds to Needs Attention"
        case .approval: return draft.paymentChoice == "Reimburse" ? "Reimburse posts a separate payment" : "Decision only · money is separate"
        case .order: return "Commitment \(money(draft.amount)) · nothing posted"
        case .shipment:
            let qty = draft.quantity.isEmpty ? "—" : draft.quantity
            return "−\(qty) pc stock · money stays on Collection"
        case nil: return "Draft · Not yet saved"
        }
    }

    // MARK: - MTO / MFG special QAs

    @ViewBuilder
    private var mtoPlantSpecialBody: some View {
        let upper = mode.uppercased()
        switch upper {
        case "CONFIRM", "QUOTE_REVISE":
            field("Customer", text: $draft.counterparty)
            field("Quote / order value", text: $draft.amount, decimal: true)
            HStack(spacing: 10) {
                field("Quantity", text: $draft.quantity, decimal: true)
                field("Unit", text: $draft.unit)
            }
            if upper == "QUOTE_REVISE" {
                segmented(
                    "Status",
                    options: ["Draft", "Sent", "Accepted", "Rejected", "Expired"],
                    selection: $draft.revisionKind
                )
            } else {
                Text("Acceptance confirms the order. It does not post revenue, a receivable, or GST.")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(theme.muted)
            }
            parentPicker(label: upper == "CONFIRM" ? "Accepted quote" : "Quote to revise", types: ["CUSTOMER_ORDER", "ORDER"])
            field("Note (optional)", text: $draft.note)
        case "OPEN_JOB":
            parentPicker(label: "Confirmed order", types: ["CUSTOMER_ORDER", "ORDER"])
            field("Job ID", text: $draft.title)
            HStack(spacing: 10) {
                field("Job target qty", text: $draft.quantity, decimal: true)
                field("Unit", text: $draft.unit)
            }
            field("BOM / notes (optional)", text: $draft.note)
            Text("A job carries the confirmed order target. Opening it posts no output, stock or sale.")
                .font(.plusJakarta(size: 12))
                .foregroundStyle(theme.muted)
        case "JOB_STATUS":
            parentPicker(label: "Open job / order", types: ["CUSTOMER_ORDER", "ORDER", "UPDATE"])
            segmented(
                "Job status",
                options: ["Open", "In progress", "On hold", "Complete", "Cancelled"],
                selection: $draft.revisionKind
            )
            field("Status note", text: $draft.title)
            field("Note (optional)", text: $draft.note)
        case "REWORK":
            segmented(
                "Mode",
                options: ["Rework", "Scrap", "Job work out"],
                selection: Binding(
                    get: { draft.revisionKind.isEmpty ? "Rework" : draft.revisionKind },
                    set: { draft.revisionKind = $0 }
                )
            )
            field("Action", text: $draft.title)
            parentPicker(label: "Source batch / job", types: ["PRODUCTION", "CUSTOMER_ORDER", "ORDER"])
            HStack(spacing: 10) {
                field("Qty", text: $draft.quantity, decimal: true)
                field("Unit", text: $draft.unit)
            }
            field("Vendor (optional)", text: $draft.counterparty)
            field("Notes (optional)", text: $draft.note)
            Text("Distinct canonical events — scrap never becomes finished stock.")
                .font(.plusJakarta(size: 12))
                .foregroundStyle(theme.muted)
        default:
            standardBody
        }
    }

    @ViewBuilder
    private func parentPicker(label: String, types: [String]) -> some View {
        let rows = (model.projection?.activity ?? []).filter { types.contains($0.eventType.uppercased()) }
        VStack(alignment: .leading, spacing: 6) {
            Text(label)
                .font(.plusJakarta(size: theme.isOpsQaCapture ? 12 : 11, weight: theme.isOpsQaCapture ? .medium : .regular))
                .foregroundStyle(theme.isOpsQaCapture ? theme.captureLabel : theme.muted)
            if rows.isEmpty {
                Text(theme.isOpsQaCapture ? "Search · optional — nothing recorded yet" : "Nothing recorded yet to link.")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(theme.muted)
            } else {
                ForEach(rows.prefix(8)) { row in
                    Button {
                        draft.parentEventId = row.id
                        if theme.isOpsQaCapture {
                            draft.itemName = row.title
                        }
                        if draft.counterparty.isEmpty {
                            draft.counterparty = row.counterpartyName ?? ""
                        }
                        if draft.title.isEmpty { draft.title = row.title }
                    } label: {
                        Text(draft.parentEventId == row.id ? "Selected \(row.title)" : row.title)
                            .font(.plusJakarta(size: 13, weight: .medium))
                            .foregroundStyle(theme.ink)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(10)
                            .background(draft.parentEventId == row.id ? theme.accent.opacity(0.16) : theme.inset)
                            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    // MARK: - Standard layouts

    @ViewBuilder
    private var standardBody: some View {
        if layout == .order && (model.vertical == .trading || model.vertical == .distribution) {
            // QA-02 / CF-05 — Quotation ≠ confirmed sales/purchase order.
            segmented("Document type", options: ["Quotation", "Sales order", "Purchase order"], selection: $draft.documentType)
            if draft.documentType == "Quotation" {
                segmented("Quote status", options: ["Draft", "Sent", "Accepted", "Rejected", "Expired"], selection: $draft.quoteStatus)
            }
        }
        if layout == .sale && (model.vertical == .trading || model.vertical == .distribution || model.vertical == .retailStore) {
            // TRD-QA-01: Quick amount · Itemized. Distribution/Retail keep Quick Sale labels.
            if model.vertical == .trading {
                segmented("", options: ["Quick amount", "Itemized"], selection: $draft.entryMode)
            } else {
                segmented("Entry", options: ["Quick Sale", "Itemised Sale"], selection: $draft.entryMode)
            }
        }
        field(titleLabel, text: $draft.title)
        let itemizedSale = draft.entryMode.localizedCaseInsensitiveContains("Itemis")
            || draft.entryMode.caseInsensitiveCompare("Itemized") == .orderedSame
        if needsAmount && layout != .collection && !(layout == .sale && itemizedSale) {
            field(theme.isOpsQaCapture && layout == .sale ? "Sale amount (M)" : amountLabel, text: $draft.amount, decimal: true)
        }
        if layout != .budget {
            field(counterpartyLabel, text: $draft.counterparty)
        }
        if [.sale, .purchase, .stock, .saleReturn, .order, .shipment].contains(layout) {
            field(theme.isOpsQaCapture ? "Channel (C · if enabled)" : "Channel (optional)", text: $draft.channel)
        }
        if [.sale, .purchase, .stock, .saleReturn, .order, .shipment].contains(layout) {
            if layout != .sale || itemizedSale || !theme.isOpsQaCapture {
                skuPicker
            }
            if layout != .sale || itemizedSale {
                HStack(spacing: 10) {
                    field("Quantity", text: $draft.quantity, decimal: true)
                    field("Unit", text: $draft.unit)
                }
            }
        }
        if layout == .sale {
            if model.vertical == .trading {
                segmented("Payment state (M)", options: ["Received", "Part paid", "Pay later"], selection: $draft.paymentChoice)
                if draft.paymentChoice == "Part paid" {
                    field("Received now (C)", text: $draft.amountPaidNow, decimal: true)
                }
                if draft.paymentChoice != "Received" {
                    setupDateField("Due date (C)", selection: $draft.dueAt, danger: true)
                }
            } else {
                segmented("Payment status", options: ["Fully Paid", "Partially Paid", "Pay Later"], selection: $draft.paymentChoice)
                if draft.paymentChoice == "Partially Paid" {
                    field("Amount paid now", text: $draft.amountPaidNow, decimal: true)
                }
                if draft.paymentChoice != "Fully Paid" {
                    setupDateField("Payment due date", selection: $draft.dueAt)
                }
                if draft.entryMode == "Itemised Sale" {
                    segmented("Delivery status", options: ["Fully Delivered", "Partially Delivered", "Deliver Later"], selection: $draft.goodsStatus)
                }
            }
        }
        if layout == .stock {
            segmented("Stock action", options: ["IN", "OUT", "TRANSFER"], selection: $draft.stockAction)
            field("Reason", text: $draft.stockReason)
            // FEFO binds projection batch identity only — never invents an expiry date.
            VStack(alignment: .leading, spacing: 4) {
                Text("FEFO / nearest expiry")
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(theme.muted)
                Text(fefoFactLabel(master: selectedMaster))
                    .font(.plusJakarta(size: 13, weight: .medium))
                    .foregroundStyle(theme.ink)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(10)
            .background(theme.inset)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        }
        if layout == .collection {
            // QA-05 — outstanding sale continuity.
            collectionContinuityBlock
            field(amountLabel, text: $draft.amount, decimal: true)
        } else if layout == .payment {
            segmented("Payment", options: ["PAID", "PARTIAL", "UNPAID"], selection: $draft.paymentStatus)
        }
        if layout == .update {
            field("What changed", text: $draft.revisionKind)
        }
        if layout == .order || layout == .shipment || layout == .payment {
            setupDateField("Due", selection: $draft.dueAt)
        }
        if layout == .shipment {
            Text("Delivery moves goods against the referenced sale. It does not create another sale.")
                .font(.plusJakarta(size: 12))
                .foregroundStyle(theme.muted)
        }
        setupDateField("Recorded at", selection: $draft.occurredAt)
        field("Note (optional)", text: $draft.note)
        if layout == .order && draft.documentType == "Quotation" {
            Text("A quotation is not a confirmed sales order and does not post revenue.")
                .font(.plusJakarta(size: 12))
                .foregroundStyle(theme.muted)
        }
    }

    @ViewBuilder
    private var collectionContinuityBlock: some View {
        let open = (model.projection?.paymentReadiness ?? []).filter { $0.relevance.uppercased() == "RECEIVABLE" }
        VStack(alignment: .leading, spacing: 6) {
            Text("Reference sale")
                .font(.plusJakarta(size: 11))
                .foregroundStyle(theme.muted)
            if open.isEmpty {
                Text("No outstanding sales to collect against.")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(theme.warning)
            } else {
                ForEach(open.prefix(8)) { row in
                    Button {
                        draft.parentEventId = row.id
                        if draft.counterparty.isEmpty {
                            draft.counterparty = row.counterpartyName ?? ""
                        }
                        if draft.amount.isEmpty { draft.amount = row.amount }
                        if draft.title.isEmpty { draft.title = row.title }
                    } label: {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(draft.parentEventId == row.id ? "Selected \(row.title)" : row.title)
                                .font(.plusJakarta(size: 13, weight: .medium))
                                .foregroundStyle(theme.ink)
                            let original = row.total ?? row.amount
                            Text("Outstanding \(row.amount) · Original \(original)")
                                .font(.plusJakarta(size: 11))
                                .foregroundStyle(theme.muted)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(10)
                        .background(draft.parentEventId == row.id ? theme.accent.opacity(0.16) : theme.inset)
                        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                    }
                    .buttonStyle(.plain)
                }
            }
            Text("Collection updates the sale outstanding. It does not create another sale.")
                .font(.plusJakarta(size: 12))
                .foregroundStyle(theme.muted)
        }
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
        case .collection: return "Customer"
        case .payment: return "Payee"
        case .order:
            return draft.documentType.hasPrefix("Purchase") ? "Supplier" : "Customer"
        case .shipment: return "Customer"
        default: return "Customer (optional)"
        }
    }

    /// The display name behind a chosen master id, so the record reads in words while the
    /// contract still carries the id the server validates.
    private func masterName(_ id: String?) -> String? {
        guard let id, !id.isEmpty else { return nil }
        return model.masters.first { $0.id == id }?.name
    }

    private var selectedMaster: OperatingMasterPayload? {
        guard let id = draft.masterId, !id.isEmpty else { return nil }
        return model.masters.first { $0.id == id }
    }

    private var skuPicker: some View {
        let kinds: Set<String> = ["SKU", "PRODUCT", "BATCH", "PART", "MATERIAL", "RAW", "BOM"]
        let skus = model.masters.filter { kinds.contains($0.kind.uppercased()) || $0.kind.isEmpty }
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
                                    draft.itemName = sku.name
                                    if draft.title.isEmpty { draft.title = sku.name }
                                    if draft.quantity.isEmpty { draft.quantity = "1" }
                                    if draft.unit.isEmpty { draft.unit = sku.unit ?? "" }
                                    if draft.itemPrice.isEmpty {
                                        draft.itemPrice = sku.sellPrice ?? sku.purchasePrice ?? ""
                                    }
                                } label: {
                                    VStack(alignment: .leading, spacing: 2) {
                                        OperatingPill(
                                            theme: theme,
                                            label: sku.name,
                                            accent: draft.masterId == sku.id
                                        )
                                        if sku.kind.uppercased() == "BATCH" {
                                            Text(fefoFactLabel(master: sku))
                                                .font(.plusJakarta(size: 10))
                                                .foregroundStyle(theme.warning)
                                        }
                                    }
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                }
            }
        }
    }

    private func applyBrowserPrefillIfNeeded() {
        guard !didApplyBrowserPrefill, let item = browserPrefill else { return }
        didApplyBrowserPrefill = true
        draft.masterId = item.id
        draft.sku = item.name
        draft.itemName = item.name
        if draft.title.isEmpty { draft.title = item.name }
        if draft.quantity.isEmpty { draft.quantity = "1" }
        if draft.unit.isEmpty, let unit = item.unit, !unit.isEmpty { draft.unit = unit }
        if draft.itemPrice.isEmpty, let price = item.unitPrice, !price.isEmpty {
            draft.itemPrice = price
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

    // MARK: - Service (SVC) chrome

    @ViewBuilder
    private var serviceModeChips: some View {
        let chips = ServiceCaptureCopy.modeChips(mode)
        if !chips.isEmpty {
            let selected = serviceSelectedChip(from: chips)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 6) {
                    ForEach(chips, id: \.self) { chip in
                        TealModeChip(theme: theme, text: chip, selected: chip == selected) {
                            applyServiceModeChip(chip)
                        }
                    }
                }
            }
        }
    }

    private func serviceSelectedChip(from chips: [String]) -> String {
        let upper = mode.uppercased()
        switch upper {
        case "SERVICE":
            if draft.entryMode.localizedCaseInsensitiveContains("Reprint") { return "Reprint" }
            if draft.entryMode.localizedCaseInsensitiveContains("Walk") { return "Walk-in charge" }
            return chips.first ?? "From booking"
        case "PURCHASE", "VENDOR":
            return draft.entryMode.localizedCaseInsensitiveContains("Equip") ? "Equipment" : "Consumables"
        case "STOCK":
            if draft.stockAction.localizedCaseInsensitiveContains("Add") { return "Add" }
            if draft.stockAction.localizedCaseInsensitiveContains("Reduce") { return "Reduce" }
            return "Set current"
        case "APPOINTMENT", "BOOKING":
            return draft.entryMode.localizedCaseInsensitiveContains("Walk") ? "Walk-in" : "Appointment"
        case "STATUS":
            if draft.paymentChoice.localizedCaseInsensitiveContains("Complete") { return "Complete" }
            if draft.paymentChoice.localizedCaseInsensitiveContains("Reschedule") { return "Reschedule" }
            return "Start"
        case "BUDGET", "TARGET":
            return draft.paymentChoice.localizedCaseInsensitiveContains("Session") ? "Sessions" : "Charges"
        case "ISSUE", "UPDATE", "APPROVAL":
            if draft.paymentChoice.localizedCaseInsensitiveContains("Approval") { return "Approval" }
            if draft.paymentChoice.localizedCaseInsensitiveContains("Update") { return "Update" }
            return "Issue"
        default:
            return chips.first ?? ""
        }
    }

    private func applyServiceModeChip(_ chip: String) {
        switch mode.uppercased() {
        case "SERVICE":
            if chip == "Reprint" {
                onRouteToMode?("REPRINT")
                return
            }
            draft.entryMode = chip
        case "PURCHASE", "VENDOR", "APPOINTMENT", "BOOKING":
            draft.entryMode = chip
        case "STOCK":
            draft.stockAction = chip == "Set current" ? "Set current stock" : chip
        case "STATUS", "BUDGET", "TARGET", "ISSUE", "UPDATE", "APPROVAL":
            draft.paymentChoice = chip
        default:
            break
        }
    }

    private func applyServiceDefaults() {
        switch mode.uppercased() {
        case "SERVICE":
            if draft.entryMode == "Quick Sale" || draft.entryMode.isEmpty { draft.entryMode = "From booking" }
            if draft.paymentChoice == "Fully Paid" || draft.paymentChoice.isEmpty { draft.paymentChoice = "Collect now" }
        case "PURCHASE", "VENDOR":
            if draft.entryMode == "Quick Sale" || draft.entryMode.isEmpty { draft.entryMode = "Consumables" }
            if draft.paymentChoice == "Fully Paid" || draft.paymentChoice.isEmpty { draft.paymentChoice = "Pay later" }
        case "SPEND":
            if draft.paymentChoice == "Fully Paid" || draft.paymentChoice.isEmpty { draft.paymentChoice = "Paid now" }
        case "STOCK":
            if draft.stockAction == "IN" || draft.stockAction.isEmpty { draft.stockAction = "Set current stock" }
            if draft.stockReason.isEmpty { draft.stockReason = "Usage / count" }
        case "APPOINTMENT", "BOOKING":
            if draft.entryMode == "Quick Sale" || draft.entryMode.isEmpty { draft.entryMode = "Appointment" }
            if draft.paymentChoice == "Fully Paid" || draft.paymentChoice.isEmpty { draft.paymentChoice = "Booked" }
        case "STATUS":
            if draft.paymentChoice == "Fully Paid" || draft.paymentChoice.isEmpty { draft.paymentChoice = "Start" }
        case "BUDGET", "TARGET":
            if draft.paymentChoice == "Fully Paid" || draft.paymentChoice.isEmpty { draft.paymentChoice = "Charges" }
        case "ISSUE", "UPDATE", "APPROVAL":
            if draft.paymentChoice == "Fully Paid" || draft.paymentChoice.isEmpty { draft.paymentChoice = "Issue" }
        default:
            break
        }
    }

    private var serviceSaveFooter: some View {
        VStack(spacing: 8) {
            Button {
                Task { await save() }
            } label: {
                Text(saveLabel)
                    .font(.plusJakarta(size: 15, weight: .bold))
                    .foregroundStyle(ServiceCaptureCopy.inkOnAccent)
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .background(
                        LinearGradient(
                            colors: canSave
                                ? [theme.accent, TealSheetChrome.lime]
                                : [theme.accent.opacity(0.35), TealSheetChrome.lime.opacity(0.35)],
                            startPoint: .leading,
                            endPoint: .trailing
                        )
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            }
            .buttonStyle(.plain)
            .disabled(saving || !canSave)
            Text(ServiceCaptureCopy.draftFooter)
                .font(.plusJakarta(size: 11, weight: .medium))
                .foregroundStyle(OperatingV12.subtitle)
                .frame(maxWidth: .infinity)
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
        if theme.vertical == .consumerService, ServiceCaptureCopy.serviceModes.contains(mode.uppercased()) {
            return ServiceCaptureCopy.cta(mode)
        }
        if theme.vertical == .retailStore, RetailCaptureCopy.moneyModes.contains(mode.uppercased()) {
            let money = draft.amount.trimmingCharacters(in: .whitespaces)
            return money.isEmpty ? RetailCaptureCopy.cta(mode) : RetailCaptureCopy.cta(mode, amount: "₹\(money)")
        }
        if theme.isOpsQaCapture {
            switch mode.uppercased() {
            case OperatingCaptureLayout.reprintMode:
                return theme.isDistributionCapture ? DistributionQuickAddCopy.cta(mode: mode) : "Load document"
            case OperatingCaptureLayout.holdMode:
                if theme.isDistributionCapture { return DistributionQuickAddCopy.cta(mode: mode) }
                return draft.holdChoice == "No hold" ? "Release hold" : "Hold record"
            case OperatingCaptureLayout.transferMode:
                return theme.isDistributionCapture ? DistributionQuickAddCopy.cta(mode: mode) : "Save transfer"
            case "SALE", "PURCHASE", "COLLECTION", "PAYMENT", "SPEND", "STOCK", "RETURN",
                 "BUDGET", "TARGET", "UPDATE", "ISSUE", "APPROVAL", "ORDER", "SHIPMENT", "DELIVERY":
                return theme.isDistributionCapture
                    ? DistributionQuickAddCopy.cta(mode: mode)
                    : TradingQuickAddCopy.cta(mode: mode)
            default: return "Save"
            }
        }
        switch mode.uppercased() {
        case OperatingCaptureLayout.reprintMode: return "Load document"
        case OperatingCaptureLayout.holdMode: return draft.holdChoice == "No hold" ? "Release hold" : "Hold record"
        case "CONFIRM": return "Confirm order"
        case "QUOTE_REVISE": return "Save quote revise"
        case "OPEN_JOB": return "Open job"
        case "JOB_STATUS": return "Save job status"
        case "REWORK": return "Post action"
        case "ORDER" where model.vertical.isMadeToOrder: return "Send quotation"
        default: return "Record"
        }
    }

    private var afterSaveNote: String {
        if theme.vertical == .consumerService, ServiceCaptureCopy.serviceModes.contains(mode.uppercased()) {
            return ServiceCaptureCopy.why(mode)
        }
        if theme.vertical == .retailStore, RetailCaptureCopy.moneyModes.contains(mode.uppercased()) {
            return RetailCaptureCopy.why(mode)
        }
        if mode.uppercased() == OperatingCaptureLayout.reprintMode {
            return "A reprint is a read. The invoice or challan is drawn on device from recorded data."
        }
        if theme.isOpsQaCapture {
            return theme.isDistributionCapture
                ? DistributionQuickAddCopy.why(mode: mode)
                : TradingQuickAddCopy.why(mode: mode)
        }
        return "One capture posts one event. Nothing here rewrites an approved record."
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
            if ok {
                if model.vertical.isMadeToOrder {
                    let entity = draft.title.isEmpty
                        ? (draft.counterparty.isEmpty ? model.title : draft.counterparty)
                        : draft.title
                    MtoToastHost.shared.show(
                        MtoToastCatalog.forCaptureSuccess(
                            mode: mode,
                            eventType: eventType,
                            entity: entity.isEmpty ? "this job" : entity
                        )
                    )
                }
                dismiss()
            }
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
        let trimmedTitle = draft.title.trimmingCharacters(in: .whitespaces)
        let tradingFallbackTitle: String = {
            guard theme.isOpsQaCapture, trimmedTitle.isEmpty else { return trimmedTitle }
            let party = draft.counterparty.trimmingCharacters(in: .whitespaces)
            if !party.isEmpty { return party }
            let sku = draft.itemName.trimmingCharacters(in: .whitespaces)
            if !sku.isEmpty { return sku }
            switch layout {
            case .sale: return "Sale"
            case .purchase: return "Purchase"
            case .collection: return "Collection"
            case .payment: return "Payment"
            case .spend: return "Expense"
            case .stock: return "Stock update"
            case .saleReturn: return "Return"
            case .budget: return "Budget revision"
            case .shipment: return "Delivery"
            case .order: return draft.documentType
            default: return displayTitle
            }
        }()
        var input = OperatingEventInput(
            momentId: model.momentId,
            eventType: eventType,
            title: tradingFallbackTitle,
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
            input.parentEventId = draft.parentEventId
        }
        let upper = mode.uppercased()
        // REWORK owns its own stock/update mapping — do not inherit generic STOCK IN defaults.
        if layout == .stock && upper != "REWORK" {
            contract.stockAction = OperatingContractVocabulary.stockAction(draft.stockAction)
            contract.stockReason = OperatingContractVocabulary.stockReason(draft.stockReason)
            contract.ownership = OperatingContractVocabulary.ownership(draft.ownership)
        }
        if layout == .sale {
            let itemized = draft.entryMode.localizedCaseInsensitiveContains("Itemis")
                || draft.entryMode.caseInsensitiveCompare("Itemized") == .orderedSame
            contract.entryMode = itemized ? "ITEMIZED" : "QUICK"
            switch draft.paymentChoice {
            case "Fully Paid", "Received":
                contract.paymentState = "RECEIVED"
                contract.receivedAmount = draft.amount.isEmpty ? nil : draft.amount
            case "Partially Paid", "Part paid":
                contract.paymentState = "PART_PAID"
                contract.receivedAmount = draft.amountPaidNow.isEmpty ? nil : draft.amountPaidNow
            default:
                contract.paymentState = "PAY_LATER"
            }
            if itemized {
                switch draft.goodsStatus {
                case "Fully Delivered": contract.goodsStatus = "DELIVERED"
                case "Partially Delivered": contract.goodsStatus = "PARTIAL"
                default: contract.goodsStatus = "LATER"
                }
            }
        }
        if layout == .order && (model.vertical == .trading || model.vertical == .distribution) {
            // TRD-QA-11 chips: Customer order · Supplier order · Delivery → CF-05 SALES/PURCHASE.
            if draft.documentType.localizedCaseInsensitiveContains("Supplier")
                || draft.documentType.hasPrefix("Purchase") {
                contract.orderSide = "PURCHASE"
                contract.quotationStatus = "ACCEPTED"
                contract.orderStatus = "PLACED"
            } else if draft.documentType == "Quotation" {
                contract.orderSide = "SALES"
                switch draft.quoteStatus {
                case "Draft": contract.quotationStatus = "DRAFT"
                case "Accepted": contract.quotationStatus = "ACCEPTED"
                case "Rejected": contract.quotationStatus = "REJECTED"
                case "Expired": contract.quotationStatus = "EXPIRED"
                default: contract.quotationStatus = "SENT"
                }
                if contract.quotationStatus == "ACCEPTED" { contract.orderStatus = "PLACED" }
            } else if draft.documentType != "Delivery" {
                contract.orderSide = "SALES"
                contract.quotationStatus = "ACCEPTED"
                contract.orderStatus = "PLACED"
            }
            contract.estimatedAmount = draft.amount.isEmpty ? nil : draft.amount
        }
        if layout == .purchase && theme.isOpsQaCapture {
            let itemized = draft.entryMode.localizedCaseInsensitiveContains("Item")
            contract.entryMode = itemized ? "ITEMIZED" : "QUICK"
            switch draft.paymentChoice {
            case "Paid":
                contract.paymentState = "PAID"
                contract.receivedAmount = draft.amount.isEmpty ? nil : draft.amount
            case "Part paid":
                contract.paymentState = "PART_PAID"
                contract.receivedAmount = draft.amountPaidNow.isEmpty ? nil : draft.amountPaidNow
            default:
                contract.paymentState = "PAY_LATER"
            }
            contract.goodsStatus = draft.goodsStatus.localizedCaseInsensitiveContains("later") ? "LATER" : "RECEIVED"
        }
        if layout == .spend && theme.isOpsQaCapture {
            switch draft.paymentChoice {
            case "Paid": contract.paymentState = "PAID"
            case "Part paid": contract.paymentState = "PART_PAID"
            default: contract.paymentState = "PAY_LATER"
            }
        }
        if upper == "CONFIRM" || upper == "QUOTE_REVISE" || (upper == "ORDER" && model.vertical.isManufacturing) {
            let status: String = {
                if upper == "CONFIRM" { return "ACCEPTED" }
                switch draft.revisionKind {
                case "Draft": return "DRAFT"
                case "Sent": return "SENT"
                case "Rejected": return "REJECTED"
                case "Expired": return "EXPIRED"
                case "Accepted": return "ACCEPTED"
                default: return upper == "QUOTE_REVISE" ? "SENT" : "DRAFT"
                }
            }()
            contract.quotationStatus = status
            if status == "ACCEPTED" { contract.orderStatus = "PLACED" }
            contract.estimatedAmount = draft.amount.isEmpty ? nil : draft.amount
            input.parentEventId = draft.parentEventId
            if upper == "CONFIRM" || upper == "QUOTE_REVISE" {
                input.revisesEventId = draft.parentEventId
            }
            if input.title.isEmpty {
                input.title = draft.counterparty.isEmpty ? "Customer order" : draft.counterparty
            }
        }
        if upper == "OPEN_JOB" || upper == "JOB_STATUS" {
            input.parentEventId = draft.parentEventId
            contract.productionStatus = {
                switch draft.revisionKind {
                case "Open": return "PLANNED"
                case "Complete", "Cancelled": return "COMPLETED"
                default: return "IN_PROGRESS"
                }
            }()
        }
        if upper == "REWORK" {
            input.parentEventId = draft.parentEventId
            // Scrap / rework qty is the rejectedQuantity the manufacturing projection keys on.
            let qty = draft.quantity.trimmingCharacters(in: .whitespaces)
            if !qty.isEmpty { input.rejectedQuantity = qty }
            if draft.revisionKind == "Scrap" {
                contract.stockAction = "REDUCE"
                contract.stockReason = "waste"
                contract.ownership = "OWNED"
            }
            if input.note == nil {
                input.note = "Rework/scrap — scrap never becomes finished stock"
            }
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
        if theme.vertical == .consumerService {
            if input.title.isEmpty {
                let fallback = draft.itemName.isEmpty ? draft.counterparty : draft.itemName
                if !fallback.trimmingCharacters(in: .whitespaces).isEmpty {
                    input.title = fallback
                }
            }
            if input.counterpartyName == nil || input.counterpartyName?.isEmpty == true {
                let party = draft.counterparty.trimmingCharacters(in: .whitespaces)
                if !party.isEmpty { input.counterpartyName = party }
            }
            if (input.lineItems ?? []).isEmpty, !draft.itemName.trimmingCharacters(in: .whitespaces).isEmpty {
                input.lineItems = [
                    .init(
                        itemName: draft.itemName,
                        quantity: draft.quantity.isEmpty ? "1" : draft.quantity,
                        unit: draft.unit.isEmpty ? nil : draft.unit,
                        unitPrice: draft.amount.isEmpty ? nil : draft.amount,
                        masterId: draft.masterId
                    ),
                ]
            }
        }
        input.contract = contract
        return input
    }

    private var eventType: String {
        switch mode.uppercased() {
        case "SERVICE": return "SERVICE"
        case "APPOINTMENT", "BOOKING": return "APPOINTMENT"
        case "STATUS":
            // Service status rides on appointments; retail / restaurant status rides on orders.
            return theme.vertical == .consumerService ? "APPOINTMENT" : "ORDER"
        case "CONFIRM", "QUOTE_REVISE":
            return "CUSTOMER_ORDER"
        case "ORDER" where model.vertical.isManufacturing:
            return "CUSTOMER_ORDER"
        case "OPEN_JOB", "JOB_STATUS":
            return "UPDATE"
        case "REWORK":
            return draft.revisionKind == "Scrap" ? "STOCK" : "UPDATE"
        case "PRODUCTION":
            return "PRODUCTION"
        default: break
        }
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
        case .order: return model.vertical.isManufacturing ? "CUSTOMER_ORDER" : "ORDER"
        case .shipment: return "SHIPMENT"
        case nil: return "UPDATE"
        }
    }

    // MARK: - Field primitives

    private func field(_ label: String, text: Binding<String>, decimal: Bool = false) -> some View {
        VStack(alignment: .leading, spacing: theme.isOpsQaCapture ? 6 : 4) {
            Text(label)
                .font(.plusJakarta(size: theme.isOpsQaCapture ? 12 : 11, weight: .medium))
                .foregroundStyle(theme.captureLabel)
            TextField("", text: text)
                .keyboardType(decimal ? .decimalPad : .default)
                .font(.plusJakarta(size: theme.isOpsQaCapture ? 15 : 14))
                .foregroundStyle(theme.captureValue)
                .padding(.horizontal, theme.isOpsQaCapture ? 14 : 12)
                .padding(.vertical, theme.isOpsQaCapture ? 12 : 11)
                .background(theme.captureField)
                .overlay(
                    RoundedRectangle(cornerRadius: theme.isOpsQaCapture ? 14 : 12)
                        .stroke(theme.isOpsQaCapture ? Color.white.opacity(0.08) : theme.border, lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: theme.isOpsQaCapture ? 14 : 12))
        }
    }

    private func segmented(_ label: String, options: [String], selection: Binding<String>) -> some View {
        VStack(alignment: .leading, spacing: theme.isOpsQaCapture ? 8 : 4) {
            if !label.isEmpty {
                Text(label)
                    .font(.plusJakarta(size: theme.isOpsQaCapture ? 12 : 11, weight: .medium))
                    .foregroundStyle(theme.captureLabel)
            }
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(options, id: \.self) { option in
                        Button {
                            selection.wrappedValue = option
                        } label: {
                            if theme.isOpsQaCapture {
                                Text(option)
                                    .font(.plusJakarta(size: 13, weight: selection.wrappedValue == option ? .bold : .medium))
                                    .foregroundStyle(selection.wrappedValue == option ? Color.white : Color(hex: "#C9D2E3"))
                                    .padding(.horizontal, 14)
                                    .padding(.vertical, 9)
                                    .background(selection.wrappedValue == option ? theme.accent : theme.captureField)
                                    .overlay(
                                        Capsule().stroke(
                                            selection.wrappedValue == option ? theme.accent : Color.white.opacity(0.12),
                                            lineWidth: 1
                                        )
                                    )
                                    .clipShape(Capsule())
                            } else {
                                OperatingPill(
                                    theme: theme,
                                    label: option,
                                    accent: selection.wrappedValue == option
                                )
                            }
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
    }

    private func setupDateField(_ label: String, selection: Binding<String?>, danger: Bool = false) -> some View {
        SetupDateTimeField(
            label: label,
            isoValue: selection,
            labelColor: (theme.isOpsQaCapture && danger) ? Color(hex: "#EF4444") : nil
        )
    }
}

private extension Optional where Wrapped == String {
    var isNilOrEmpty: Bool {
        guard let value = self else { return true }
        return value.isEmpty
    }
}
