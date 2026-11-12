import SwiftUI

/// Figma-parity retail master create/edit sheets (supplier, location, product, stock item).
/// Uses `BusinessOperatingModel.createMaster` / `updateMaster` — no new APIs.
enum RetailMasterFormKind: String, Identifiable {
    case supplier
    case location
    case product
    case stockItem

    var id: String { rawValue }

    static func from(tab: OperatingMasterTab) -> RetailMasterFormKind? {
        switch tab {
        case .principals: return .supplier
        case .locations: return .location
        case .catalogue: return .product
        default: return nil
        }
    }

    var figmaNode: String {
        switch self {
        case .supplier: return "2506:29612"
        case .location: return "2506:29703"
        case .product: return "2506:29782"
        case .stockItem: return "2506:29907"
        }
    }

    var title: String {
        switch self {
        case .supplier: return "New supplier"
        case .location: return "Add location"
        case .product: return "Create product / SKU"
        case .stockItem: return "Create stock item"
        }
    }

    var cta: String {
        switch self {
        case .supplier: return "Save supplier"
        case .location: return "Save location"
        case .product: return "Save product"
        case .stockItem: return "Save stock item"
        }
    }

    var defaultMasterKind: String {
        switch self {
        case .supplier: return "SUPPLIER"
        case .location: return "LOCATION"
        case .product: return "PRODUCT"
        case .stockItem: return "MATERIAL"
        }
    }

    var subtitle: String {
        switch self {
        case .supplier: return "Supplier master · GSTIN drives input credit on Purchase / GRN"
        case .location: return "Stores, back rooms & counters · stock point"
        case .product: return "Master item · typed fields + GST before it appears in Sales Order"
        case .stockItem: return "Master stock item · unit, rate, HSN & GST before Purchase Order can add it"
        }
    }

    var why: String {
        switch self {
        case .supplier:
            return "Supplier is a master write. GSTIN state code decides CGST+SGST vs IGST on Purchase / GRN. PO save is commitment only."
        case .location:
            return "Locations are stock and sale points. Products pick a default location in Create Product; transfers move stock between them."
        case .product:
            return "Create Product is a master write. Sales Order and POS only pick saved SKUs; GST posts on the bill, not on save."
        case .stockItem:
            return "Create Stock Item is a master write. Purchase Order only references saved items; input GST posts on Purchase / GRN."
        }
    }

    var toastCaption: String {
        switch self {
        case .supplier: return "Supplier saved · available in Purchase Order"
        case .location: return "Location saved · stock point ready"
        case .product, .stockItem: return "Master saved · available in browser on next open"
        }
    }

    var editTitle: String {
        switch self {
        case .supplier: return "Edit supplier"
        case .location: return "Edit location"
        case .product: return "Edit product / SKU"
        case .stockItem: return "Edit stock item"
        }
    }
}

/// Rose chrome wrapper around master create/edit — used from `RetailMastersView` / `OperatingMastersView`.
struct RetailMasterFormSheet: View {
    let theme: OperatingTheme
    let kind: RetailMasterFormKind
    let model: BusinessOperatingModel
    var existing: OperatingMasterPayload? = nil
    var onSaved: () -> Void = {}
    /// Legacy callbacks kept for `OperatingMastersView` nesting.
    var onSave: ((OperatingMasterDraft) async -> Bool)? = nil
    var onUpdate: ((String, OperatingMasterDraft, Int) async -> Bool)? = nil

    @Environment(\.dismiss) private var dismiss
    @State private var draft: OperatingMasterDraft
    @State private var saving = false
    @State private var isCreate = true
    @State private var editingId: String?
    @State private var editingVersion: Int = 1
    @State private var showToast = false

    // UI-local / note-mapped Figma fields
    @State private var legalName = ""
    @State private var phone = ""
    @State private var email = ""
    @State private var gstRegistration = "Regular"
    @State private var category = ""
    @State private var paymentTerms = ""
    @State private var address = ""
    @State private var status = "Active"
    @State private var locationType = "Store"
    @State private var receiptPrinter = "Screen only"
    @State private var displayOrder = ""
    @State private var stocksCategory = "Staples"
    @State private var locationLead = ""
    @State private var variant = "Single"
    @State private var defaultLocation = "Main store"
    @State private var trackBatch = "Off · optional"
    @State private var tracked = "Track stock"
    @State private var parQty = ""
    @State private var preferredSupplier = ""
    @State private var contextChip = "Catalogue master"
    @State private var gstChip = "5%"

    init(
        theme: OperatingTheme,
        kind: RetailMasterFormKind,
        model: BusinessOperatingModel,
        existing: OperatingMasterPayload? = nil,
        onSaved: @escaping () -> Void = {},
        onSave: ((OperatingMasterDraft) async -> Bool)? = nil,
        onUpdate: ((String, OperatingMasterDraft, Int) async -> Bool)? = nil
    ) {
        self.theme = theme
        self.kind = kind
        self.model = model
        self.existing = existing
        self.onSaved = onSaved
        self.onSave = onSave
        self.onUpdate = onUpdate
        _draft = State(initialValue: OperatingMasterDraft(
            kind: kind.defaultMasterKind,
            name: existing?.name ?? ""
        ))
        _isCreate = State(initialValue: existing == nil)
        if let existing {
            _editingId = State(initialValue: existing.id)
            _editingVersion = State(initialValue: existing.version ?? 1)
        }
    }

    private var candidates: [OperatingMasterPayload] {
        let source = model.masters.isEmpty ? (model.projection?.masters ?? []) : model.masters
        return source.filter { matchesKind($0.kind) }
            .sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
    }

    private var locationNames: [String] {
        let source = model.masters.isEmpty ? (model.projection?.masters ?? []) : model.masters
        let fromMasters = source
            .filter { ["LOCATION", "STORE", "WAREHOUSE", "CHANNEL"].contains($0.kind.uppercased()) }
            .map(\.name)
        return Array(Set(fromMasters + ["Main store", "Back store", "Warehouse", "Counter"])).sorted()
    }

    private var supplierNames: [String] {
        let source = model.masters.isEmpty ? (model.projection?.masters ?? []) : model.masters
        return source.filter { $0.kind.uppercased() == "SUPPLIER" }.map(\.name)
    }

    var body: some View {
        ZStack(alignment: .top) {
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    RoseGrabber()
                    RoseSheetHeader(
                        theme: theme,
                        title: isCreate ? kind.title : kind.editTitle,
                        subtitle: kind.subtitle,
                        qaCode: kind.figmaNode,
                        onClose: { dismiss() }
                    )

                    RoseCreateEditMode(
                        theme: theme,
                        isCreate: $isCreate,
                        // Only lock edit when a create-only callback is wired without update/model fallback.
                        editLocked: onSave != nil && onUpdate == nil
                    ) {
                        // Edit mode entered — picker below
                    }
                    .onChange(of: isCreate) { _, create in
                        if create { clearForCreate() }
                    }

                    if !isCreate {
                        RoseSectionCard(theme: theme, label: "Edit existing") {
                            if candidates.isEmpty {
                                RoseEmptyCard(
                                    theme: theme,
                                    title: "No masters yet",
                                    bodyText: "Create one first, then edit from here."
                                )
                            } else {
                                ForEach(candidates.prefix(8), id: \.id) { master in
                                    RosePickerRow(theme: theme, label: master.kind, value: master.name) {
                                        hydrate(master)
                                    }
                                }
                            }
                        }
                    }

                    switch kind {
                    case .supplier: supplierFields
                    case .location: locationFields
                    case .product: productFields
                    case .stockItem: stockItemFields
                    }

                    RoseWhyBox(theme: theme, text: kind.why)

                    RoseStickyFooter(
                        theme: theme,
                        title: saving ? "Saving…" : kind.cta,
                        caption: kind.toastCaption,
                        disabled: draft.name.trimmingCharacters(in: .whitespaces).isEmpty
                            || (!isCreate && editingId == nil),
                        loading: saving,
                        formStyle: kind == .supplier,
                        action: { Task { await save() } }
                    )
                }
                .padding(16)
                .padding(.bottom, 24)
            }
            .background(theme.canvas)

            if showToast {
                RoseSavedToast()
                    .padding(.top, 12)
                    .transition(.move(edge: .top).combined(with: .opacity))
            }
        }
        .task {
            if let existing { hydrate(existing) }
            if model.masters.isEmpty { await model.loadMasters() }
        }
    }

    @ViewBuilder
    private var supplierFields: some View {
        RoseFieldLabel(text: "Supplier name")
        RoseTextField(theme: theme, placeholder: "e.g. Coastal Traders", text: $draft.name)
        RoseFieldLabel(text: "Legal / trade name")
        RoseTextField(theme: theme, placeholder: "As on GST certificate", text: $legalName)
        RoseFieldLabel(text: "GSTIN")
        RoseTextField(theme: theme, placeholder: "27AAACC1234D1Z7", text: $draft.gstin)
        Text("15 chars · state code from GSTIN · local check only")
            .font(.plusJakarta(size: 10))
            .foregroundStyle(OperatingV12.subtitle)
        RoseFieldLabel(text: "GST registration")
        RoseChipRow(theme: theme, options: ["Regular", "Composition", "Unregistered"], selection: $gstRegistration)
        RoseFieldLabel(text: "Phone")
        RoseTextField(theme: theme, placeholder: "+91 98200 12345", text: $phone, keyboard: .phonePad)
        RoseFieldLabel(text: "Email (optional)")
        RoseTextField(theme: theme, placeholder: "orders@example.in", text: $email, keyboard: .emailAddress)
        RoseFieldLabel(text: "Category")
        RoseChipRow(
            theme: theme,
            options: ["FMCG", "Staples", "Personal care", "Home care", "Beverages", "Packaging"],
            selection: $category
        )
        RoseFieldLabel(text: "Payment terms")
        RoseTextField(theme: theme, placeholder: "Net 7 days", text: $paymentTerms)
        RoseFieldLabel(text: "Address")
        RoseTextField(theme: theme, placeholder: "MIDC, Andheri East, Mumbai", text: $address)
        RoseFieldLabel(text: "Status")
        RoseChipRow(theme: theme, options: ["Active", "Preferred", "Inactive"], selection: $status)
    }

    @ViewBuilder
    private var locationFields: some View {
        RoseFieldLabel(text: "Location name")
        RoseTextField(theme: theme, placeholder: "e.g. Mezzanine store", text: $draft.name)
        RoseFieldLabel(text: "Type")
        RoseChipRow(theme: theme, options: ["Store", "Warehouse", "Counter"], selection: $locationType)
        RoseFieldLabel(text: "Receipt printer")
        RoseChipRow(
            theme: theme,
            options: ["Counter printer 1", "Label printer", "Screen only"],
            selection: $receiptPrinter
        )
        RoseFieldLabel(text: "Display order")
        RoseTextField(theme: theme, placeholder: "5", text: $displayOrder, keyboard: .numberPad)
        RoseFieldLabel(text: "Stocks categories")
        RoseChipRow(
            theme: theme,
            options: ["Staples", "Snacks", "Personal care", "Beverages", "Home care"],
            selection: $stocksCategory
        )
        RoseFieldLabel(text: "Location lead (optional)")
        RoseTextField(theme: theme, placeholder: "Imran Shaikh", text: $locationLead)
        RoseFieldLabel(text: "Status")
        RoseChipRow(theme: theme, options: ["Active", "Paused"], selection: $status)
    }

    @ViewBuilder
    private var productFields: some View {
        RoseChipRow(
            theme: theme,
            options: ["Harbor Mart POS", "Catalogue master", "GST-aware"],
            selection: $contextChip
        )
        RoseFieldLabel(text: "Product name")
        RoseTextField(theme: theme, placeholder: "e.g. Basmati rice 5kg", text: $draft.name)
        RoseFieldLabel(text: "Category")
        RoseChipRow(
            theme: theme,
            options: ["Staples", "Snacks", "Personal care", "Beverages", "Home care"],
            selection: $category
        )
        RoseFieldLabel(text: "Variants")
        RoseChipRow(theme: theme, options: ["Size / pack", "Single", "Colour set"], selection: $variant)
        RoseAmountHero(
            theme: theme,
            label: "Sell price",
            amountText: draft.sellPrice.isEmpty ? "—" : OperatingV12Money.amount(draft.sellPrice, currency: model.currency),
            caption: "Sell price ≤ MRP · per-variant override"
        )
        RoseFieldLabel(text: "Sell price amount")
        RoseTextField(theme: theme, placeholder: "649", text: $draft.sellPrice, keyboard: .decimalPad)
        RoseFieldLabel(text: "MRP")
        RoseTextField(theme: theme, placeholder: "699", text: $draft.mrp, keyboard: .decimalPad)
        RoseSectionCard(theme: theme, label: "GST on product") {
            RoseChipRow(theme: theme, options: ["5%", "18%", "Exempt"], selection: $gstChip)
            Text("Rate + HSN live on the master. Output GST posts on bill / POS Sale only — never on Sales Order save.")
                .font(.plusJakarta(size: 10))
                .foregroundStyle(OperatingV12.subtitle)
        }
        .onChange(of: gstChip) { _, chip in
            draft.gstRate = chip == "Exempt" ? "" : String(chip.dropLast())
        }
        RoseFieldLabel(text: "HSN / SAC")
        RoseTextField(theme: theme, placeholder: "1006 · Rice", text: $draft.hsn)
        HsnLookupHints(query: draft.hsn, accent: theme.accent) { code, rate in
            draft.hsn = code
            if let rate, !rate.isEmpty, rate != "0" {
                draft.gstRate = rate
                gstChip = "\(rate)%"
            }
        }
        RoseFieldLabel(text: "Default location")
        RoseChipRow(theme: theme, options: locationNames, selection: $defaultLocation)
        RoseFieldLabel(text: "Unit")
        RoseChipRow(theme: theme, options: ["pcs", "kg", "pack"], selection: Binding(
            get: { draft.unit.isEmpty ? "pcs" : draft.unit },
            set: { draft.unit = $0 }
        ))
        RoseFieldLabel(text: "Barcode / EAN")
        RoseTextField(theme: theme, placeholder: "8901234567890 · scan to fill", text: $draft.code)
        RosePickerRow(theme: theme, label: "Track batch / expiry", value: trackBatch) {
            trackBatch = trackBatch.hasPrefix("Off") ? "On · batch required" : "Off · optional"
        }
        RoseFieldLabel(text: "Status")
        RoseChipRow(theme: theme, options: ["Active", "Inactive"], selection: $status)
    }

    @ViewBuilder
    private var stockItemFields: some View {
        RoseChipRow(
            theme: theme,
            options: ["Harbor Mart POS", "Stock master", "GST-aware"],
            selection: $contextChip
        )
        RoseFieldLabel(text: "Item name")
        RoseTextField(theme: theme, placeholder: "e.g. Carry bags (L)", text: $draft.name)
        RoseFieldLabel(text: "Category")
        RoseChipRow(
            theme: theme,
            options: ["Packaging", "Consumables", "Cleaning", "Stationery", "Display", "Spares"],
            selection: Binding(
                get: { category.isEmpty ? "Packaging" : category },
                set: { category = $0 }
            )
        )
        RoseFieldLabel(text: "Unit of measure")
        RoseChipRow(theme: theme, options: ["kg", "L", "pcs", "bag", "can"], selection: Binding(
            get: { draft.unit.isEmpty ? "pcs" : draft.unit },
            set: { draft.unit = $0 }
        ))
        RoseAmountHero(
            theme: theme,
            label: "Purchase rate",
            amountText: draft.purchasePrice.isEmpty ? "—" : OperatingV12Money.amount(draft.purchasePrice, currency: model.currency),
            caption: "Default rate on PO lines · editable per order"
        )
        RoseFieldLabel(text: "Purchase rate amount")
        RoseTextField(theme: theme, placeholder: "480", text: $draft.purchasePrice, keyboard: .decimalPad)
        RoseSectionCard(theme: theme, label: "GST on purchase") {
            RoseChipRow(theme: theme, options: ["0%", "5%", "12%", "18%"], selection: $gstChip)
            Text("Input GST on Purchase / GRN. PO commitment can show estimate; it does not post tax.")
                .font(.plusJakarta(size: 10))
                .foregroundStyle(OperatingV12.subtitle)
        }
        .onChange(of: gstChip) { _, chip in
            draft.gstRate = String(chip.dropLast())
        }
        RoseFieldLabel(text: "HSN code")
        RoseTextField(theme: theme, placeholder: "HSN code", text: $draft.hsn)
        HsnLookupHints(query: draft.hsn, accent: theme.accent) { code, rate in
            draft.hsn = code
            if let rate, !rate.isEmpty {
                draft.gstRate = rate
                gstChip = rate == "0" ? "Exempt" : "\(rate)%"
            }
        }
        RosePickerRow(theme: theme, label: "Default location", value: defaultLocation) {
            if let idx = locationNames.firstIndex(of: defaultLocation) {
                defaultLocation = locationNames[(idx + 1) % locationNames.count]
            } else if let first = locationNames.first {
                defaultLocation = first
            }
        }
        RoseFieldLabel(text: "Par / reorder qty")
        RoseTextField(theme: theme, placeholder: "500 pcs", text: $parQty)
        RosePickerRow(
            theme: theme,
            label: "Preferred supplier",
            value: preferredSupplier.isEmpty ? (supplierNames.first ?? "Select…") : preferredSupplier
        ) {
            let names = supplierNames.isEmpty ? ["Select…"] : supplierNames
            if let idx = names.firstIndex(of: preferredSupplier) {
                preferredSupplier = names[(idx + 1) % names.count]
            } else {
                preferredSupplier = names[0]
            }
        }
        RoseChipRow(theme: theme, options: ["Track stock", "Non-tracked"], selection: $tracked)
        RoseFieldLabel(text: "Status")
        RoseChipRow(theme: theme, options: ["Active", "Inactive"], selection: $status)
    }

    private func matchesKind(_ raw: String) -> Bool {
        let k = raw.uppercased()
        switch kind {
        case .supplier: return k == "SUPPLIER"
        case .location: return ["LOCATION", "STORE", "WAREHOUSE", "CHANNEL"].contains(k)
        case .product: return ["PRODUCT", "SKU"].contains(k)
        case .stockItem: return ["MATERIAL", "CONSUMABLE", "ITEM"].contains(k)
        }
    }

    private func hydrate(_ master: OperatingMasterPayload) {
        editingId = master.id
        editingVersion = master.version ?? 1
        draft.kind = master.kind
        draft.name = master.name
        draft.code = master.code ?? ""
        draft.unit = master.unit ?? ""
        draft.sellPrice = master.sellPrice ?? ""
        draft.mrp = master.mrp ?? ""
        draft.purchasePrice = master.purchasePrice ?? ""
        draft.gstRate = master.gstRate ?? ""
        draft.hsn = master.hsn ?? ""
        draft.gstin = master.gstin ?? ""
        draft.billingState = master.billingState ?? ""
        draft.parentMasterId = master.parentMasterId ?? ""
        if kind == .supplier {
            legalName = master.code ?? ""
            gstRegistration = gstRegistrationLabel(master.gstRegistration)
        }
        if kind == .stockItem {
            tracked = (master.tracked == false) ? "Non-tracked" : "Track stock"
            if let parent = master.parentMasterId,
               let match = (model.masters.isEmpty ? (model.projection?.masters ?? []) : model.masters)
                .first(where: { $0.id == parent }) {
                preferredSupplier = match.name
            }
        }
        gstChip = draft.gstRate.isEmpty ? (kind == .product ? "Exempt" : "0%") : "\(draft.gstRate)%"
        status = (master.status?.uppercased() == "INACTIVE") ? "Inactive" : "Active"
        if kind == .location, master.status?.uppercased() == "INACTIVE" {
            status = "Paused"
        }
    }

    private func gstRegistrationLabel(_ raw: String?) -> String {
        switch raw?.uppercased() {
        case "COMPOSITION": return "Composition"
        case "UNREGISTERED": return "Unregistered"
        default: return "Regular"
        }
    }

    private func clearForCreate() {
        editingId = nil
        editingVersion = 1
        draft = OperatingMasterDraft(kind: kind.defaultMasterKind, name: "")
        legalName = ""; phone = ""; email = ""; category = ""; paymentTerms = ""; address = ""
        status = "Active"; gstRegistration = "Regular"; locationType = "Store"
        receiptPrinter = "Screen only"; displayOrder = ""; stocksCategory = "Staples"
        locationLead = ""; variant = "Single"; defaultLocation = "Main store"
        trackBatch = "Off · optional"; tracked = "Track stock"; parQty = ""
        preferredSupplier = ""; contextChip = kind == .stockItem ? "Stock master" : "Catalogue master"
        gstChip = kind == .stockItem ? "0%" : "5%"
        draft.gstRate = kind == .stockItem ? "0" : "5"
        draft.unit = "pcs"
    }

    private func applyMetaToKind() {
        switch kind {
        case .location:
            draft.kind = locationType == "Warehouse" ? "WAREHOUSE" : "LOCATION" // Store + Counter
            // D6 receipt printer + lead/order/category are UI-local; fold into code as short tag when blank.
            if draft.code.trimmingCharacters(in: .whitespaces).isEmpty {
                let tag = [locationType, receiptPrinter, stocksCategory, status]
                    .joined(separator: " · ")
                draft.code = String(tag.prefix(60))
            }
        case .stockItem:
            draft.kind = kind.defaultMasterKind
            if let match = (model.masters.isEmpty ? (model.projection?.masters ?? []) : model.masters)
                .first(where: { $0.name == preferredSupplier }) {
                draft.parentMasterId = match.id
            }
        case .supplier:
            draft.kind = "SUPPLIER"
            if draft.code.trimmingCharacters(in: .whitespaces).isEmpty {
                draft.code = legalName
            }
            // gstRegistration is UI-local on iOS CreateBody (no field); Android posts gstRegistration.
            _ = gstRegistration
        case .product:
            draft.kind = "PRODUCT"
        }
        // Digits-only HSN for contract validation.
        draft.hsn = String(draft.hsn.filter(\.isNumber).prefix(8))
        // Phone / email / address / payment / category / preferred / printer stay UI-local
        // (OperatingMasterDraft has no note/payload). Android create path stores them on event note.
        _ = [phone, email, paymentTerms, address, category, displayOrder, locationLead,
             variant, defaultLocation, trackBatch, tracked, parQty, contextChip, status]
    }

    private func save() async {
        saving = true
        defer { saving = false }
        applyMetaToKind()
        if kind == .product {
            draft.gstRate = gstChip == "Exempt" ? "" : String(gstChip.dropLast())
        }
        if kind == .stockItem {
            draft.gstRate = String(gstChip.dropLast())
        }
        let ok: Bool
        if !isCreate, let id = editingId {
            if let onUpdate {
                ok = await onUpdate(id, draft, editingVersion)
            } else {
                let updated = await model.updateMaster(
                    id: id,
                    draft: draft,
                    expectedVersion: editingVersion
                )
                ok = updated != nil && model.writeError == nil
            }
        } else if let onSave {
            ok = await onSave(draft)
        } else {
            let created = await model.createMaster(draft)
            ok = created != nil && model.writeError == nil
        }
        guard ok else { return }
        withAnimation { showToast = true }
        try? await Task.sleep(nanoseconds: 900_000_000)
        onSaved()
        dismiss()
    }
}
