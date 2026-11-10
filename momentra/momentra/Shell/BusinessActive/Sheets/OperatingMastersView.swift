import SwiftUI

/// Six masters tabs for both verticals. Rows come from `business.operating_master` through the
/// company endpoint; the projection's `masters` list is only the offline fallback.
struct OperatingMastersView: View {
    let theme: OperatingTheme
    @ObservedObject var model: BusinessOperatingModel

    @Environment(\.dismiss) private var dismiss
    @State private var tab: OperatingMasterTab
    /// One sheet at a time: new master, edit master, or a price list.
    @State private var sheet: OperatingMastersSheet?

    /// Which tab opens first — a board row can say "open the catalogue" and land there — and
    /// whether the create form opens with it, which is what the setup's `+ Create SKU` row wants.
    init(
        theme: OperatingTheme,
        model: BusinessOperatingModel,
        initialTab: OperatingMasterTab = .principals,
        openCreateForm: Bool = false
    ) {
        self.theme = theme
        self.model = model
        _tab = State(initialValue: initialTab)
        _sheet = State(initialValue: openCreateForm ? .create : nil)
    }

    private var rows: [OperatingMasterPayload] {
        if tab == .priceLists { return [] }
        let kinds = tab.kinds
        let filtered = model.masters.filter { kinds.contains($0.kind.uppercased()) }
        return filtered.sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                Text("Masters")
                    .font(.plusJakarta(size: 24, weight: .semibold))
                    .foregroundStyle(theme.ink)
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(OperatingMasterTab.allCases) { item in
                            Button {
                                tab = item
                            } label: {
                                OperatingPill(theme: theme, label: item.title, accent: tab == item)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
                Text(tab.note)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(theme.body)

                if tab == .priceLists {
                    priceLists
                } else if tab == .invite {
                    inviteBlock
                } else {
                    masterRows
                }
            }
            .padding(18)
        }
        .background(theme.canvas.ignoresSafeArea())
        .task {
            await model.loadMasters()
            if tab == .priceLists { await model.loadPriceLists() }
        }
        .onChange(of: tab) { _, newTab in
            // Price lists are a separate company read, so they are fetched only when that tab
            // is actually opened.
            if newTab == .priceLists { Task { await model.loadPriceLists() } }
        }
        .sheet(item: $sheet) { route in
            Group {
                switch route {
                case .create:
                    OperatingMasterFormView(
                        theme: theme,
                        tab: tab,
                        existing: nil,
                        parents: parentCandidates,
                        onSave: { draft in
                            let created = await model.createMaster(draft)
                            let ok = model.writeError == nil
                            if ok, model.vertical.isMadeToOrder {
                                MtoToastHost.shared.show(
                                    MtoToastCatalog.forMasterCreated(
                                        kind: draft.kind,
                                        entity: draft.name.isEmpty ? "this record" : draft.name
                                    )
                                )
                            }
                            return created != nil && ok
                        }
                    )
                case let .edit(master):
                    OperatingMasterFormView(
                        theme: theme,
                        tab: tab,
                        existing: master,
                        parents: parentCandidates,
                        onSave: { draft in
                            let updated = await model.updateMaster(
                                id: master.id,
                                draft: draft,
                                expectedVersion: master.version ?? 1
                            )
                            let ok = model.writeError == nil
                            if ok, model.vertical.isMadeToOrder {
                                MtoToastHost.shared.show(
                                    MtoToastCatalog.forMasterCreated(
                                        kind: draft.kind,
                                        entity: draft.name.isEmpty ? "this record" : draft.name
                                    )
                                )
                            }
                            return updated != nil && ok
                        },
                        onArchive: {
                            let ok = await model.archiveMaster(
                                id: master.id,
                                expectedVersion: master.version ?? 1
                            )
                            if ok, model.vertical.isMadeToOrder {
                                MtoToastHost.shared.show(
                                    .deleted,
                                    entity: master.name.isEmpty ? "this record" : master.name
                                )
                            }
                            return ok
                        }
                    )
                case .priceList:
                    OperatingPriceListFormView(theme: theme, model: model)
                }
            }
            .presentationDetents([.large])
            .presentationDragIndicator(.visible)
            .presentationCornerRadius(24)
            .preferredColorScheme(.dark)
        }
    }

    private var parentCandidates: [OperatingMasterPayload] {
        model.masters.filter {
            let kind = $0.kind.uppercased()
            return kind == "PRINCIPAL" || kind == "WAREHOUSE" || kind == "LOCATION"
        }
    }

    private var masterRows: some View {
        Group {
            if rows.isEmpty {
                OperatingEmptyState(
                    theme: theme,
                    title: "No \(tab.singular) yet",
                    bodyText: "Create one to reference it by id from every record."
                )
            } else {
                VStack(spacing: 0) {
                    ForEach(rows) { master in
                        Button {
                            sheet = .edit(master)
                        } label: {
                            VStack(alignment: .leading, spacing: 4) {
                                HStack(spacing: 8) {
                                    Text(master.name)
                                        .font(.plusJakarta(size: 14, weight: .medium))
                                        .foregroundStyle(theme.ink)
                                    Spacer(minLength: 4)
                                    if master.source == "EVENT" {
                                        OperatingBadgeChip(theme: theme, badge: "Legacy")
                                    } else {
                                        OperatingBadgeChip(theme: theme, badge: master.kind.capitalized)
                                    }
                                }
                                let meta = [master.code, master.unit, master.gstRate.map { "GST \($0)%" }]
                                    .compactMap { $0 }
                                    .filter { !$0.isEmpty }
                                    .joined(separator: " · ")
                                if !meta.isEmpty {
                                    Text(meta)
                                        .font(.plusJakarta(size: 12))
                                        .foregroundStyle(theme.body)
                                }
                            }
                            .padding(.vertical, 9)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .contentShape(Rectangle())
                        }
                        .buttonStyle(.plain)
                        .disabled(master.source == "EVENT")
                        Divider().overlay(theme.border)
                    }
                }
            }
            Button {
                sheet = .create
            } label: {
                Text("＋ New \(tab.singular)")
                    .font(.plusJakarta(size: 14, weight: .semibold))
                    .foregroundStyle(theme.onAccent)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 13)
                    .background(theme.accent)
                    .clipShape(RoundedRectangle(cornerRadius: 13))
            }
            .buttonStyle(.plain)
        }
    }

    private var priceLists: some View {
        Group {
            if model.priceLists.isEmpty {
                OperatingEmptyState(
                    theme: theme,
                    title: "No price list yet",
                    bodyText: "A price list prices SKUs for a dated window. Create the SKUs first."
                )
            } else {
                VStack(spacing: 0) {
                    ForEach(model.priceLists) { list in
                        VStack(alignment: .leading, spacing: 4) {
                            Text(list.name)
                                .font(.plusJakarta(size: 14))
                                .foregroundStyle(theme.ink)
                            Text("\(list.validFrom)\(list.validTo.map { " – \($0)" } ?? "") · \(list.lines?.count ?? 0) lines")
                                .font(.plusJakarta(size: 12))
                                .foregroundStyle(theme.body)
                        }
                        .padding(.vertical, 9)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        Divider().overlay(theme.border)
                    }
                }
            }
            Button {
                sheet = .priceList
            } label: {
                Text("＋ New price list")
                    .font(.plusJakarta(size: 14, weight: .semibold))
                    .foregroundStyle(theme.onAccent)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 13)
                    .background(theme.accent)
                    .clipShape(RoundedRectangle(cornerRadius: 13))
            }
            .buttonStyle(.plain)
        }
    }

    private var inviteBlock: some View {
        OperatingCard(theme: theme) {
            Text("Invite a person into this company")
                .font(.plusJakarta(size: 15, weight: .semibold))
                .foregroundStyle(theme.ink)
            Text("An invite grants a role and its access. Joining is not approval, and approval is not payment.")
                .font(.plusJakarta(size: 12))
                .foregroundStyle(theme.body)
            Text("Use the company Invite sheet to issue a code or QR.")
                .font(.plusJakarta(size: 12))
                .foregroundStyle(theme.muted)
        }
    }
}

/// One sheet at a time from the masters tabs, so a create/edit/price-list never competes with
/// another `.sheet` modifier on the same view.
private enum OperatingMastersSheet: Identifiable {
    case create
    case edit(OperatingMasterPayload)
    case priceList

    var id: String {
        switch self {
        case .create: return "create"
        case let .edit(master): return "edit:\(master.id)"
        case .priceList: return "priceList"
        }
    }
}

// MARK: - Master form

/// Principal / dealer, warehouse / route, and SKU all share one form because they share one body.
/// Which fields are visible depends on the tab; the body sent is always complete.
struct OperatingMasterFormView: View {
    let theme: OperatingTheme
    let tab: OperatingMasterTab
    let existing: OperatingMasterPayload?
    let parents: [OperatingMasterPayload]
    /// Returns `true` when the write went through, so the sheet can dismiss itself.
    let onSave: (OperatingMasterDraft) async -> Bool
    /// Soft-archives the row. `nil` for a brand-new master, which has nothing to archive.
    var onArchive: (() async -> Bool)? = nil

    @Environment(\.dismiss) private var dismiss
    @State private var draft: OperatingMasterDraft
    @State private var saving = false
    @State private var statePickerOpen = false

    init(
        theme: OperatingTheme,
        tab: OperatingMasterTab,
        existing: OperatingMasterPayload?,
        parents: [OperatingMasterPayload],
        onSave: @escaping (OperatingMasterDraft) async -> Bool,
        onArchive: (() async -> Bool)? = nil
    ) {
        self.theme = theme
        self.tab = tab
        self.existing = existing
        self.parents = parents
        self.onSave = onSave
        self.onArchive = onArchive
        _draft = State(initialValue: OperatingMasterDraft(
            kind: existing?.kind.uppercased() ?? tab.defaultKind,
            name: existing?.name ?? "",
            code: existing?.code ?? "",
            unit: existing?.unit ?? "",
            sellPrice: existing?.sellPrice ?? "",
            purchasePrice: existing?.purchasePrice ?? "",
            mrp: existing?.mrp ?? "",
            hsn: existing?.hsn ?? "",
            gstRate: existing?.gstRate ?? "",
            gstin: existing?.gstin ?? "",
            billingState: existing?.billingState ?? "",
            parentMasterId: existing?.parentMasterId ?? ""
        ))
    }

    private var isSku: Bool { draft.kind == "SKU" || draft.kind == "PRODUCT" }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                Text(existing == nil ? "New \(tab.singular)" : "Edit \(existing?.name ?? "")")
                    .font(.plusJakarta(size: 22, weight: .semibold))
                    .foregroundStyle(theme.ink)
                field("Name", text: $draft.name)
                field("Code (optional)", text: $draft.code)
                if isSku {
                    field("Unit", text: $draft.unit)
                    field("Sell price", text: $draft.sellPrice, decimal: true)
                    field("Purchase price", text: $draft.purchasePrice, decimal: true)
                    field("MRP (optional)", text: $draft.mrp, decimal: true)
                    field("HSN", text: $draft.hsn)
                    field("GST rate %", text: $draft.gstRate, decimal: true)
                }
                if tab == .principals {
                    field("GSTIN (optional)", text: $draft.gstin)
                    pickerField(
                        "Billing state",
                        value: draft.billingState,
                        hint: IndiaStateCatalog.gstinStateHint(gstin: draft.gstin, state: draft.billingState)
                    ) { statePickerOpen = true }
                }
                if !parents.isEmpty {
                    Text("Attach to")
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(theme.body)
                    ForEach(parents) { parent in
                        Button {
                            draft.parentMasterId = draft.parentMasterId == parent.id ? "" : parent.id
                        } label: {
                            Text(parent.name)
                                .font(.plusJakarta(size: 13))
                                .foregroundStyle(draft.parentMasterId == parent.id ? theme.onAccent : theme.ink)
                                .padding(.horizontal, 10)
                                .padding(.vertical, 8)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .background(draft.parentMasterId == parent.id ? theme.accent : theme.inset)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 10)
                                        .stroke(draft.parentMasterId == parent.id ? theme.accent : theme.border, lineWidth: 1)
                                )
                                .clipShape(RoundedRectangle(cornerRadius: 10))
                        }
                        .buttonStyle(.plain)
                    }
                }
                if existing?.source == "EVENT" {
                    Text("This row was derived from a recorded event. Create a master to make it editable.")
                        .font(.plusJakarta(size: 11))
                        .foregroundStyle(theme.warning)
                }
                Button {
                    Task {
                        saving = true
                        let ok = await onSave(draft)
                        saving = false
                        if ok { dismiss() }
                    }
                } label: {
                    Text(saving ? "Saving…" : (existing == nil ? "Create" : "Save changes"))
                        .font(.plusJakarta(size: 14, weight: .semibold))
                        .foregroundStyle(theme.onAccent)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 13)
                        .background(draft.canSave ? theme.accent : theme.accent.opacity(0.4))
                        .clipShape(RoundedRectangle(cornerRadius: 13))
                }
                .buttonStyle(.plain)
                .disabled(saving || !draft.canSave)
                if let onArchive, let existing, existing.source != "EVENT" {
                    // Archive, never delete: a posted bill keeps its reference, so the row is
                    // retired rather than removed.
                    Button {
                        Task {
                            saving = true
                            let ok = await onArchive()
                            saving = false
                            if ok { dismiss() }
                        }
                    } label: {
                        Text("Archive \(tab.singular)")
                            .font(.plusJakarta(size: 13, weight: .semibold))
                            .foregroundStyle(theme.negative)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                            .overlay(
                                RoundedRectangle(cornerRadius: 12)
                                    .stroke(theme.negative.opacity(0.6), lineWidth: 1)
                            )
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                    .buttonStyle(.plain)
                    .disabled(saving)
                }
                OperatingWhyFootnote(
                    theme: theme,
                    text: "A master has one identity. Records reference it by id, so renaming it never breaks a posted bill."
                )
            }
            .padding(18)
        }
        .background(theme.canvas.ignoresSafeArea())
        .sheet(isPresented: $statePickerOpen) {
            OperatingStatePickerView(
                theme: theme,
                selected: draft.billingState
            ) { picked in
                draft.billingState = picked.name
                statePickerOpen = false
            } onCancel: {
                statePickerOpen = false
            }
        }
    }

    /// A form row that opens the state picker instead of a keyboard.
    private func pickerField(
        _ label: String,
        value: String,
        hint: String? = nil,
        onTap: @escaping () -> Void
    ) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.plusJakarta(size: 11))
                .foregroundStyle(theme.muted)
            Button(action: onTap) {
                HStack(spacing: 4) {
                    Text(value.isEmpty ? "Pick a state" : value)
                        .font(.plusJakarta(size: 14))
                        .foregroundStyle(value.isEmpty ? theme.muted : theme.ink)
                        .lineLimit(1)
                    Spacer(minLength: 4)
                    Text("›")
                        .font(.plusJakarta(size: 16))
                        .foregroundStyle(theme.muted)
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 11)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(theme.inset)
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(theme.border, lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            if let hint {
                Text(hint)
                    .font(.plusJakarta(size: 10))
                    .foregroundStyle(theme.muted)
            }
        }
    }

    private func field(_ label: String, text: Binding<String>, decimal: Bool = false) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.plusJakarta(size: 11))
                .foregroundStyle(theme.muted)
            TextField("", text: text)
                .keyboardType(decimal ? .decimalPad : .default)
                .textInputAutocapitalization(.words)
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
}

// MARK: - Price list form

/// `2566:32380` / `2558:29912` — a dated list with per-SKU prices.
struct OperatingPriceListFormView: View {
    let theme: OperatingTheme
    @ObservedObject var model: BusinessOperatingModel

    @Environment(\.dismiss) private var dismiss
    @State private var name = ""
    @State private var channel = ""
    @State private var validTo = ""
    @State private var gstRate = ""
    @State private var prices: [String: String] = [:]
    @State private var saving = false

    private var skus: [OperatingMasterPayload] {
        model.masters.filter { ["SKU", "PRODUCT"].contains($0.kind.uppercased()) }
    }

    private var validFrom: String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.string(from: Date())
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                Text("New price list")
                    .font(.plusJakarta(size: 22, weight: .semibold))
                    .foregroundStyle(theme.ink)
                field("Name", text: $name)
                field("Channel (optional)", text: $channel)
                field("Valid to (YYYY-MM-DD)", text: $validTo)
                field("GST rate %", text: $gstRate, decimal: true)
                if skus.isEmpty {
                    Text("Create a SKU first — a price list prices SKUs, it does not invent them.")
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(theme.warning)
                }
                ForEach(skus) { sku in
                    HStack(spacing: 10) {
                        Text(sku.name)
                            .font(.plusJakarta(size: 13))
                            .foregroundStyle(theme.ink)
                            .frame(maxWidth: .infinity, alignment: .leading)
                        TextField("Price", text: Binding(
                            get: { prices[sku.id] ?? "" },
                            set: { prices[sku.id] = $0 }
                        ))
                        .keyboardType(.decimalPad)
                        .font(.plusJakarta(size: 13))
                        .foregroundStyle(theme.ink)
                        .padding(8)
                        .frame(width: 110)
                        .background(theme.inset)
                        .overlay(
                            RoundedRectangle(cornerRadius: 9)
                                .stroke(theme.border, lineWidth: 1)
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 9))
                    }
                }
                Button {
                    Task { await save() }
                } label: {
                    Text(saving ? "Saving…" : "Create price list")
                        .font(.plusJakarta(size: 14, weight: .semibold))
                        .foregroundStyle(theme.onAccent)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 13)
                        .background(theme.accent)
                        .clipShape(RoundedRectangle(cornerRadius: 13))
                }
                .buttonStyle(.plain)
                .disabled(saving || name.trimmingCharacters(in: .whitespaces).isEmpty)
                if let error = model.writeError {
                    Text(error)
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(theme.negative)
                }
                OperatingWhyFootnote(
                    theme: theme,
                    text: "A price list is effective-dated. Two lists may overlap in name but never in a day."
                )
            }
            .padding(18)
        }
        .background(theme.canvas.ignoresSafeArea())
        .task { await model.loadMasters() }
    }

    private func save() async {
        saving = true
        defer { saving = false }
        let lines = prices.compactMap { key, value -> OperatingPriceListLineInput? in
            let trimmed = value.trimmingCharacters(in: .whitespaces)
            guard !trimmed.isEmpty else { return nil }
            return OperatingPriceListLineInput(masterId: key, price: trimmed, schemePct: nil)
        }
        let body = OperatingPriceListCreateBody(
            name: name.trimmingCharacters(in: .whitespaces),
            momentId: model.momentId,
            channel: channel.isEmpty ? nil : channel,
            gstRate: gstRate.isEmpty ? nil : gstRate,
            validFrom: validFrom,
            validTo: validTo.isEmpty ? nil : validTo,
            lines: lines
        )
        let created = await model.createPriceList(body)
        if created != nil { dismiss() }
    }

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
}
