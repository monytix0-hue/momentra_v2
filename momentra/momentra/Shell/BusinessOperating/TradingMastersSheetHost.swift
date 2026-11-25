import SwiftUI

/// Trading / Distribution masters host — APK `OperatingMastersSheet` parity.
/// List + New SKU / Edit; EVENT-sourced rows promote to a table master on save.
struct TradingMastersSheetHost: View {
    let momentId: String
    let projection: OperatingProjectionPayload?
    let initialTab: String
    var companyId: String? = nil
    var editingMasterId: String? = nil
    var accent: Color = Color(red: 0.06, green: 0.73, blue: 0.51)
    var onDismiss: () -> Void = {}
    var onSaved: () -> Void = {}

    @State private var tab: MasterTab = .catalogue
    @State private var remote: [OperatingMasterPayload] = []
    @State private var formOpen = false
    @State private var editing: OperatingMasterPayload? = nil
    @State private var name = ""
    @State private var unit = "pc"
    @State private var unitCategory = "Count"
    @State private var conversion = ""
    @State private var sellPrice = ""
    @State private var purchasePrice = ""
    @State private var saleIncludes = ""
    @State private var purchaseIncludes = ""
    @State private var hsn = ""
    @State private var gstRate = ""
    @State private var hsnHits: [HsnLookupResultPayload] = []
    @State private var hsnNote: String?
    @State private var error: String?
    @State private var saving = false
    @State private var archiveConfirm = false
    @State private var loaded = false

    enum MasterTab: String, CaseIterable, Identifiable {
        case principals = "Principals"
        case catalogue = "Catalogue"
        case locations = "Locations"
        case invite = "Invite"
        var id: String { rawValue }

        var sheetTitle: String {
            switch self {
            case .principals: return "Business contacts"
            case .catalogue: return "Catalogue & SKU"
            case .locations: return "Warehouses & routes"
            case .invite: return "Invite & QR"
            }
        }

        var singular: String {
            switch self {
            case .principals: return "supplier or customer"
            case .catalogue: return "SKU"
            case .locations: return "warehouse or route"
            case .invite: return "invite"
            }
        }

        var whyNote: String {
            switch self {
            case .principals:
                return "Identity is stable here: a contact row is referenced by id. One record can be Supplier, Customer, or both."
            case .catalogue:
                return "A SKU carries unit precision and standing prices. A posted bill never rewrites it."
            case .locations:
                return "A route belongs to a warehouse. Moving stock between them is a transfer, not a sale."
            case .invite:
                return "Invite grants a role and its access. Joining ≠ approval ≠ payment."
            }
        }

        var masterKind: String {
            switch self {
            case .principals: return "SUPPLIER"
            case .catalogue: return "PRODUCT"
            case .locations: return "WAREHOUSE"
            case .invite: return "PERSON"
            }
        }

        var kinds: Set<String> {
            switch self {
            case .principals: return ["PRINCIPAL", "DEALER", "SUPPLIER", "BUYER", "CUSTOMER", "CHANNEL"]
            case .catalogue: return ["SKU", "PRODUCT", "BATCH"]
            case .locations: return ["WAREHOUSE", "LOCATION", "ROUTE"]
            case .invite: return []
            }
        }
    }

    private var rows: [OperatingMasterPayload] {
        let kinds = tab.kinds
        guard !kinds.isEmpty else { return [] }
        let fromTable = remote.filter { kinds.contains($0.kind.uppercased()) }
        let seen = Set(fromTable.map { "\($0.kind.uppercased())|\($0.name.trimmingCharacters(in: .whitespacesAndNewlines).lowercased())" })
        let fromEvents = (projection?.masters ?? []).filter {
            kinds.contains($0.kind.uppercased())
                && !seen.contains("\($0.kind.uppercased())|\($0.name.trimmingCharacters(in: .whitespacesAndNewlines).lowercased())")
        }
        return (fromTable + fromEvents).sorted {
            $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending
        }
    }

    private var brand: String {
        projection?.title?.trimmingCharacters(in: .whitespacesAndNewlines).nilIfBlank ?? "this moment"
    }

    private var isDistribution: Bool {
        (projection?.subtype ?? "").caseInsensitiveCompare("DISTRIBUTION_DEALERSHIP") == .orderedSame
    }

    private var promoteFromEvent: Bool { (editing?.source ?? "") == "EVENT" }
    private var tableBacked: Bool { editing != nil && !promoteFromEvent }

    var body: some View {
        NavigationStack {
            Group {
                if formOpen {
                    formBody
                } else {
                    listBody
                }
            }
            .background(Color(red: 0.04, green: 0.05, blue: 0.09).ignoresSafeArea())
            .navigationTitle(formOpen ? formNavTitle : tab.sheetTitle)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(formOpen ? "Back" : "Close") {
                        if formOpen {
                            formOpen = false
                            editing = nil
                            clearForm()
                        } else {
                            onDismiss()
                        }
                    }
                    .foregroundStyle(accent)
                }
            }
            .task {
                resolveInitialTab()
                await reloadMasters()
                if !loaded, let id = editingMasterId, !id.isEmpty {
                    loaded = true
                    if let hit = rows.first(where: { $0.id == id })
                        ?? projection?.masters?.first(where: { $0.id == id }) {
                        openEdit(hit)
                    }
                }
            }
            .confirmationDialog(
                "Archive this master?",
                isPresented: $archiveConfirm,
                titleVisibility: .visible
            ) {
                Button("Archive", role: .destructive) {
                    Task { await archiveMaster() }
                }
                Button("Cancel", role: .cancel) {}
            } message: {
                Text("Archived masters leave pickers; posted history stays linked.")
            }
        }
        .presentationDetents([.medium, .large])
        .presentationDragIndicator(.visible)
    }

    private var formNavTitle: String {
        if promoteFromEvent { return "Save as \(tab.singular)" }
        if editing != nil { return "Edit \(editing?.name ?? tab.singular)" }
        return "New \(tab.singular)"
    }

    private var listBody: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                Text(tab.sheetTitle)
                    .font(.system(size: 24, weight: .semibold))
                    .foregroundStyle(.white)
                if tab != .invite {
                    Text("\(rows.count) record\(rows.count == 1 ? "" : "s") · \(brand)")
                        .font(.system(size: 12))
                        .foregroundStyle(Color.white.opacity(0.55))
                }

                if tab == .invite {
                    BusinessInviteQrView(
                        momentId: momentId,
                        title: brand,
                        viewerRole: projection?.role,
                        companyIdHint: companyId ?? projection?.companyId,
                        accent: accent,
                        distribution: isDistribution,
                        showIntroHeader: true,
                        onError: { error = $0 }
                    )
                } else if companyId == nil {
                    Text("Masters need a company. This moment is not attached to one yet.")
                        .font(.system(size: 13))
                        .foregroundStyle(Color.white.opacity(0.6))
                } else {
                    Button {
                        editing = nil
                        clearForm()
                        formOpen = true
                    } label: {
                        Text("New \(tab.singular)")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundStyle(.black)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(accent)
                            .clipShape(RoundedRectangle(cornerRadius: 14))
                    }
                    .buttonStyle(.plain)

                    if rows.isEmpty {
                        VStack(alignment: .leading, spacing: 6) {
                            Text("Nothing here yet")
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundStyle(.white)
                            Text("Add the first row and it becomes pickable on every capture sheet.")
                                .font(.system(size: 12))
                                .foregroundStyle(Color.white.opacity(0.55))
                        }
                        .padding(14)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(Color(red: 0.09, green: 0.11, blue: 0.15))
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                    }

                    ForEach(rows) { master in
                        Button {
                            openEdit(master)
                        } label: {
                            masterRow(master)
                        }
                        .buttonStyle(.plain)
                    }

                    Text("Why: \(tab.whyNote)")
                        .font(.system(size: 11))
                        .foregroundStyle(Color.white.opacity(0.45))
                        .padding(.top, 8)
                }

                if let error {
                    Text(error).font(.system(size: 12)).foregroundStyle(.red.opacity(0.9))
                }
            }
            .padding(16)
        }
    }

    private func masterRow(_ master: OperatingMasterPayload) -> some View {
        let meta = [
            master.unit.flatMap { $0.isEmpty ? nil : "per \($0)" },
            master.sellPrice.flatMap { $0.isEmpty ? nil : "sell \($0)" },
            master.purchasePrice.flatMap { $0.isEmpty ? nil : "buy \($0)" },
            master.gstRate.flatMap { $0.isEmpty ? nil : "GST \($0)%" },
            master.hsn.flatMap { $0.isEmpty ? nil : "HSN \($0)" },
        ].compactMap { $0 }.joined(separator: " · ")
        let fromEvent = (master.source ?? "") == "EVENT"
        return HStack(spacing: 10) {
            Text(String(master.kind.prefix(2)))
                .font(.system(size: 11, weight: .semibold))
                .foregroundStyle(accent)
                .frame(width: 30, height: 30)
                .background(accent.opacity(0.16))
                .clipShape(RoundedRectangle(cornerRadius: 9))
            VStack(alignment: .leading, spacing: 2) {
                Text(master.name.isEmpty ? "Unnamed \(master.kind.lowercased())" : master.name)
                    .font(.system(size: 14, weight: .medium))
                    .foregroundStyle(.white)
                    .lineLimit(1)
                Text(
                    meta.isEmpty
                        ? (fromEvent ? "From a posted record · tap to save as SKU" : "No price or GST set")
                        : meta
                )
                .font(.system(size: 11))
                .foregroundStyle(Color.white.opacity(0.55))
                .lineLimit(1)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            if fromEvent {
                Text("From record")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(Color.white.opacity(0.85))
                    .padding(.horizontal, 10)
                    .padding(.vertical, 5)
                    .overlay(
                        Capsule().stroke(Color.white.opacity(0.2), lineWidth: 1)
                    )
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .background(Color(red: 0.09, green: 0.11, blue: 0.15))
        .clipShape(RoundedRectangle(cornerRadius: 14))
        .overlay(
            RoundedRectangle(cornerRadius: 14)
                .stroke(Color.white.opacity(0.08), lineWidth: 1)
        )
    }

    private var formBody: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text(formNavTitle)
                    .font(.system(size: 22, weight: .semibold))
                    .foregroundStyle(.white)

                TextField("Name", text: $name)
                    .textFieldStyle(.plain)
                    .padding(12)
                    .background(Color(red: 0.09, green: 0.11, blue: 0.15))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .foregroundStyle(.white)

                if tab == .catalogue {
                    catalogueFields
                }

                if let error {
                    Text(error).font(.system(size: 12)).foregroundStyle(.red.opacity(0.9))
                }

                Button(saving ? "Saving…" : (promoteFromEvent || editing == nil ? "Create" : "Save changes")) {
                    Task { await saveMaster() }
                }
                .disabled(saving || name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(.black)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(accent)
                .clipShape(RoundedRectangle(cornerRadius: 14))

                if tableBacked, companyId != nil, editing?.version != nil {
                    Button("Archive") { archiveConfirm = true }
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(Color(red: 0.97, green: 0.44, blue: 0.44))
                }
            }
            .padding(16)
        }
    }

    private var catalogueFields: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Base unit")
                .font(.system(size: 12))
                .foregroundStyle(Color.white.opacity(0.55))
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(OperatingSeedUoms.categories, id: \.self) { cat in
                        Button {
                            unitCategory = cat
                            if let first = OperatingSeedUoms.entries(category: cat).first {
                                unit = first.code
                            }
                            if !OperatingSeedUoms.needsPackConversion(unit) {
                                conversion = ""
                            }
                        } label: {
                            Text(cat)
                                .font(.system(size: 12, weight: .semibold))
                                .foregroundStyle(unitCategory == cat ? Color.black : Color.white.opacity(0.85))
                                .padding(.horizontal, 12)
                                .padding(.vertical, 8)
                                .background(unitCategory == cat ? accent : Color(red: 0.09, green: 0.11, blue: 0.15))
                                .clipShape(Capsule())
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            LazyVGrid(columns: [GridItem(.adaptive(minimum: 88), spacing: 8)], spacing: 8) {
                ForEach(OperatingSeedUoms.entries(category: unitCategory)) { entry in
                    Button {
                        unit = entry.code
                        if !OperatingSeedUoms.needsPackConversion(entry.code) {
                            conversion = ""
                        }
                    } label: {
                        Text(entry.label)
                            .font(.system(size: 12, weight: .medium))
                            .foregroundStyle(unit == entry.code ? Color.black : Color.white.opacity(0.9))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 10)
                            .padding(.horizontal, 8)
                            .background(unit == entry.code ? accent : Color(red: 0.09, green: 0.11, blue: 0.15))
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                    }
                    .buttonStyle(.plain)
                }
            }
            if OperatingSeedUoms.needsPackConversion(unit) {
                TextField("Units of base (e.g. pc) per pack", text: $conversion)
                    .textFieldStyle(.plain)
                    .padding(12)
                    .background(Color(red: 0.09, green: 0.11, blue: 0.15))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .foregroundStyle(.white)
            }
            Text("Sale / purchase prices & GST (optional)")
                .font(.system(size: 12))
                .foregroundStyle(Color.white.opacity(0.55))
            TextField("Sale price", text: $sellPrice)
                .keyboardType(.decimalPad)
                .textFieldStyle(.plain)
                .padding(12)
                .background(Color(red: 0.09, green: 0.11, blue: 0.15))
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .foregroundStyle(.white)
            TextField("Purchase price", text: $purchasePrice)
                .keyboardType(.decimalPad)
                .textFieldStyle(.plain)
                .padding(12)
                .background(Color(red: 0.09, green: 0.11, blue: 0.15))
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .foregroundStyle(.white)
            includesChips("Sale GST", selection: $saleIncludes)
            includesChips("Purchase GST", selection: $purchaseIncludes)
            TextField("HSN (optional)", text: $hsn)
                .keyboardType(.numberPad)
                .textFieldStyle(.plain)
                .padding(12)
                .background(Color(red: 0.09, green: 0.11, blue: 0.15))
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .foregroundStyle(.white)
                .onChange(of: hsn) { _, value in
                    Task { await searchHsn(value) }
                }
            ForEach(hsnHits.prefix(6), id: \.code) { row in
                Button {
                    hsn = row.code
                    if let rate = row.gstRate, !rate.isEmpty { gstRate = rate }
                    hsnHits = []
                } label: {
                    Text([row.code, row.description, row.gstRate.map { "\($0)%" }].compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: " · "))
                        .font(.system(size: 12))
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(10)
                        .background(Color(red: 0.09, green: 0.11, blue: 0.15))
                        .clipShape(RoundedRectangle(cornerRadius: 10))
                }
                .buttonStyle(.plain)
            }
            if let hsnNote, !hsnNote.isEmpty {
                Text(hsnNote)
                    .font(.system(size: 11))
                    .foregroundStyle(Color.white.opacity(0.45))
            }
            TextField("GST rate % (optional)", text: $gstRate)
                .keyboardType(.decimalPad)
                .textFieldStyle(.plain)
                .padding(12)
                .background(Color(red: 0.09, green: 0.11, blue: 0.15))
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .foregroundStyle(.white)
        }
    }

    private func resolveInitialTab() {
        tab = MasterTab.allCases.first {
            initialTab.localizedCaseInsensitiveContains($0.rawValue)
                || ($0 == .catalogue && (
                    initialTab.localizedCaseInsensitiveContains("SKU")
                        || initialTab.localizedCaseInsensitiveContains("Catalogue")
                        || initialTab.localizedCaseInsensitiveContains("Product")
                ))
                || ($0 == .locations && (
                    initialTab.localizedCaseInsensitiveContains("Warehouse")
                        || initialTab.localizedCaseInsensitiveContains("Location")
                ))
                || ($0 == .invite && initialTab.localizedCaseInsensitiveContains("Invite"))
                || ($0 == .principals && (
                    initialTab.localizedCaseInsensitiveContains("Principal")
                        || initialTab.localizedCaseInsensitiveContains("Supplier")
                        || initialTab.localizedCaseInsensitiveContains("Contact")
                ))
        } ?? .catalogue
    }

    private func openEdit(_ master: OperatingMasterPayload) {
        editing = master
        name = master.name
        unit = master.unit ?? "pc"
        conversion = master.conversionToBase ?? ""
        sellPrice = master.sellPrice ?? ""
        purchasePrice = master.purchasePrice ?? ""
        hsn = master.hsn ?? ""
        gstRate = master.gstRate ?? ""
        if let sale = master.salePriceIncludesGst ?? master.priceIncludesGst {
            saleIncludes = sale == "YES" ? "Included" : (sale == "NO" ? "Added" : "")
        } else {
            saleIncludes = ""
        }
        if let purchase = master.purchasePriceIncludesGst {
            purchaseIncludes = purchase == "YES" ? "Included" : (purchase == "NO" ? "Added" : "")
        } else {
            purchaseIncludes = ""
        }
        formOpen = true
    }

    private func clearForm() {
        name = ""
        unit = "pc"
        unitCategory = "Count"
        conversion = ""
        sellPrice = ""
        purchasePrice = ""
        saleIncludes = ""
        purchaseIncludes = ""
        hsn = ""
        gstRate = ""
        hsnHits = []
        hsnNote = nil
        error = nil
    }

    private func reloadMasters() async {
        guard let companyId, !companyId.isEmpty else { return }
        do {
            var payload = try await APIClient.shared.listOperatingMasters(
                companyId: companyId,
                momentId: momentId
            )
            // Backfill setup DC location → WAREHOUSE when Locations is empty.
            if tab == .locations {
                let locKinds: Set<String> = ["WAREHOUSE", "LOCATION", "ROUTE"]
                let hasLoc = payload.items.contains { locKinds.contains($0.kind.uppercased()) }
                    || (projection?.masters ?? []).contains { locKinds.contains($0.kind.uppercased()) }
                let setupLoc = projection?.location?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
                if !hasLoc, !setupLoc.isEmpty {
                    var body = OperatingMasterCreateBody(kind: "WAREHOUSE", name: setupLoc)
                    body.momentId = momentId
                    _ = try? await APIClient.shared.createOperatingMaster(
                        companyId: companyId,
                        body: body,
                        idempotencyKey: "setup-loc-\(momentId)-\(setupLoc.lowercased())"
                    )
                    if let again = try? await APIClient.shared.listOperatingMasters(
                        companyId: companyId,
                        momentId: momentId
                    ) {
                        payload = again
                    }
                }
            }
            remote = payload.items
            error = nil
        } catch {
            // Keep projection-derived fallback rows when the company list fails.
            self.error = nil
        }
    }

    private func saveMaster() async {
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        guard let companyId else {
            error = "Masters need a company."
            return
        }
        if tab == .catalogue, OperatingSeedUoms.needsPackConversion(unit),
           conversion.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            error = "Units per pack is required for pack/box/carton/dozen"
            return
        }
        saving = true
        error = nil
        defer { saving = false }
        let conversionArg: String? = {
            guard tab == .catalogue, OperatingSeedUoms.needsPackConversion(unit) else { return nil }
            let t = conversion.trimmingCharacters(in: .whitespacesAndNewlines)
            return t.isEmpty ? nil : t
        }()
        do {
            if let existing = editing, !promoteFromEvent, let version = existing.version {
                var body = OperatingMasterUpdateBody(expectedVersion: version)
                body.name = trimmed
                if tab == .catalogue {
                    body.unit = unit
                    body.conversionToBase = conversionArg
                    body.sellPrice = BusinessSetupAmountFormatting.plainAmount(sellPrice).nilIfBlank
                    body.purchasePrice = BusinessSetupAmountFormatting.plainAmount(purchasePrice).nilIfBlank
                    body.hsn = hsn.nilIfBlank
                    body.gstRate = gstRate.nilIfBlank
                    body.gstApplicable = gstRate.isEmpty ? nil : "YES"
                }
                _ = try await APIClient.shared.updateOperatingMaster(
                    companyId: companyId,
                    masterId: existing.id,
                    body: body,
                    idempotencyKey: UUID().uuidString
                )
            } else {
                // New or EVENT promote → CREATE table master.
                var body = OperatingMasterCreateBody(kind: tab.masterKind, name: trimmed)
                body.momentId = momentId
                if tab == .catalogue {
                    body.unit = unit
                    body.conversionToBase = conversionArg
                    body.sellPrice = BusinessSetupAmountFormatting.plainAmount(sellPrice).nilIfBlank
                    body.purchasePrice = BusinessSetupAmountFormatting.plainAmount(purchasePrice).nilIfBlank
                    body.hsn = hsn.nilIfBlank
                    body.gstRate = gstRate.nilIfBlank
                    body.gstApplicable = gstRate.isEmpty ? nil : "YES"
                    body.salePriceIncludesGst = includesWire(saleIncludes)
                    body.purchasePriceIncludesGst = includesWire(purchaseIncludes)
                } else if tab == .principals {
                    body.kind = "SUPPLIER"
                }
                _ = try await APIClient.shared.createOperatingMaster(
                    companyId: companyId,
                    body: body,
                    idempotencyKey: UUID().uuidString
                )
            }
            await reloadMasters()
            formOpen = false
            editing = nil
            clearForm()
            onSaved()
        } catch {
            self.error = error.localizedDescription
        }
    }

    private func archiveMaster() async {
        guard let existing = editing, let companyId, let version = existing.version else {
            error = "Company or version missing for archive."
            return
        }
        saving = true
        error = nil
        defer { saving = false }
        do {
            _ = try await APIClient.shared.archiveOperatingMaster(
                companyId: companyId,
                masterId: existing.id,
                expectedVersion: version,
                idempotencyKey: UUID().uuidString
            )
            await reloadMasters()
            formOpen = false
            editing = nil
            clearForm()
            onSaved()
        } catch {
            self.error = error.localizedDescription
        }
    }

    private func includesWire(_ label: String) -> String? {
        switch label {
        case "Included": return "YES"
        case "Added": return "NO"
        default: return nil
        }
    }

    private func includesChips(_ title: String, selection: Binding<String>) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title).font(.system(size: 12)).foregroundStyle(Color.white.opacity(0.55))
            HStack(spacing: 8) {
                ForEach(["Default", "Included", "Added"], id: \.self) { label in
                    Button {
                        selection.wrappedValue = label == "Default" ? "" : label
                    } label: {
                        Text(label)
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundStyle((selection.wrappedValue == label || (label == "Default" && selection.wrappedValue.isEmpty)) ? Color.black : Color.white.opacity(0.85))
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background((selection.wrappedValue == label || (label == "Default" && selection.wrappedValue.isEmpty)) ? accent : Color(red: 0.09, green: 0.11, blue: 0.15))
                            .clipShape(Capsule())
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private func searchHsn(_ query: String) async {
        let q = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !q.isEmpty else {
            hsnHits = []
            hsnNote = nil
            return
        }
        do {
            let payload = try await APIClient.shared.searchHsn(query: q, limit: 8)
            hsnHits = payload.results ?? []
            hsnNote = payload.note
        } catch {
            hsnHits = []
            hsnNote = nil
        }
    }
}

private extension String {
    var nilIfBlank: String? {
        let t = trimmingCharacters(in: .whitespacesAndNewlines)
        return t.isEmpty ? nil : t
    }
}
