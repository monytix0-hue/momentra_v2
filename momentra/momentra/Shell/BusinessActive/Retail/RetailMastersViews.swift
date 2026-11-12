import SwiftUI

/// RTL masters lists — Figma Suppliers / Invite & QR / Catalogue / Locations (`2506:*`).
/// Structural port of `ServiceMastersViews` with rose Harbor Mart chrome.
enum RetailMastersTab: String, CaseIterable, Identifiable {
    case suppliers
    case catalogue
    case locations
    case invite

    var id: String { rawValue }

    var title: String {
        switch self {
        case .suppliers: return "Suppliers"
        case .catalogue: return "Catalogue"
        case .locations: return "Locations"
        case .invite: return "Invite & QR"
        }
    }

    var operatingTab: OperatingMasterTab {
        switch self {
        case .suppliers: return .principals
        case .catalogue: return .catalogue
        case .locations: return .locations
        case .invite: return .invite
        }
    }

    static func from(_ tab: OperatingMasterTab) -> RetailMastersTab {
        switch tab {
        case .principals, .priceLists: return .suppliers
        case .catalogue: return .catalogue
        case .locations: return .locations
        case .invite: return .invite
        }
    }

    var kinds: Set<String> {
        switch self {
        case .suppliers: return ["SUPPLIER"]
        case .catalogue: return ["PRODUCT", "SKU", "ITEM", "MATERIAL", "CONSUMABLE"]
        case .locations: return ["LOCATION", "STORE", "WAREHOUSE", "CHANNEL", "COUNTER"]
        case .invite: return []
        }
    }

    var why: String {
        switch self {
        case .invite:
            return "Invite grants a role and its access. Joining ≠ approval ≠ payment."
        case .suppliers:
            return "Supplier is a master write. GSTIN drives input tax credit on Purchase / GRN only — a Purchase Order never posts GST."
        case .catalogue:
            return "Catalogue lists saved SKUs. GST badge is the product rate from its HSN — output GST posts on bill / POS Sale, never on Sales Order save."
        case .locations:
            return "Locations are stock and sale points. Set the receipt logo under Store branding (JPEG only). Stock moves only on GRN, Sale or Transfer — never on order save."
        }
    }

    var subtitle: String {
        switch self {
        case .invite: return "invite grants a role"
        case .suppliers: return "Supplier master · GSTIN drives input credit on Purchase / GRN"
        case .catalogue: return "Catalogue master · products with price + GST before they reach SO / POS"
        case .locations: return "Stores, back rooms & counters · stock and sale points · receipt logo"
        }
    }
}

private let retailCatalogueCategories = ["Staples", "Snacks", "Personal care", "Beverages", "Home care"]

struct RetailMastersView: View {
    let theme: OperatingTheme
    @ObservedObject var model: BusinessOperatingModel
    var initialTab: OperatingMasterTab = .principals

    @Environment(\.dismiss) private var dismiss
    @State private var tab: RetailMastersTab
    @State private var formKind: RetailMasterFormKind?
    @State private var editing: OperatingMasterPayload?
    @State private var category = retailCatalogueCategories[0]
    @State private var supplierQuery = ""
    @State private var showToast = false
    @State private var inviteError: String?
    @State private var brandingMessage: String?

    init(theme: OperatingTheme, model: BusinessOperatingModel, initialTab: OperatingMasterTab = .principals) {
        self.theme = theme
        self.model = model
        self.initialTab = initialTab
        _tab = State(initialValue: RetailMastersTab.from(initialTab))
    }

    private var brand: String {
        let title = model.title.trimmingCharacters(in: .whitespacesAndNewlines)
        return title.isEmpty ? "Harbor Mart" : title
    }

    private var rows: [OperatingMasterPayload] {
        let source = model.masters.isEmpty ? (model.projection?.masters ?? []) : model.masters
        return source
            .filter { tab.kinds.contains($0.kind.uppercased()) }
            .sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
    }

    var body: some View {
        ZStack(alignment: .top) {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    RoseGrabber()
                    RoseSheetHeader(
                        theme: theme,
                        title: tab == .invite ? "Invite team · \(brand)" : tab.title,
                        subtitle: tab == .invite
                            ? "\(brand) · \(ownerLine) · \(tab.subtitle)"
                            : tab.subtitle,
                        onClose: { dismiss() }
                    )

                    tabChips

                    switch tab {
                    case .invite:
                        inviteBlock
                    case .suppliers:
                        suppliersList
                    case .catalogue:
                        catalogueList
                    case .locations:
                        locationsList
                    }

                    RoseWhyBox(theme: theme, text: tab.why)
                    RoseStickyFooter(
                        theme: theme,
                        title: footerTitle,
                        caption: footerCaption,
                        action: {
                            showSavedToast()
                            dismiss()
                        }
                    )
                }
                .padding(16)
                .padding(.bottom, 28)
            }
            .background(theme.canvas.ignoresSafeArea())

            if showToast {
                RoseSavedToast()
                    .padding(.top, 12)
                    .transition(.move(edge: .top).combined(with: .opacity))
            }
        }
        .task { if model.masters.isEmpty { await model.loadMasters() } }
        .sheet(item: $formKind) { kind in
            RetailMasterFormSheet(
                theme: theme,
                kind: kind,
                model: model,
                existing: editing,
                onSaved: {
                    editing = nil
                    formKind = nil
                    showSavedToast()
                    Task { await model.loadMasters() }
                }
            )
            .presentationDetents([.large])
            .presentationDragIndicator(.visible)
            .preferredColorScheme(.dark)
        }
    }

    private var tabChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 6) {
                ForEach(RetailMastersTab.allCases) { item in
                    RoseModeChip(theme: theme, text: item.title, selected: tab == item) {
                        tab = item
                    }
                }
            }
        }
    }

    private var ownerLine: String {
        model.projection?.role.nilIfBlank.map { roleLabel($0) } ?? "Kiran Rao (Owner)"
    }

    /// Figma `2506:29333` — live mint / contacts / QR via shared `BusinessInviteQr` + rose accents.
    private var inviteBlock: some View {
        VStack(alignment: .leading, spacing: 12) {
            BusinessInviteQr(
                companyId: model.companyId,
                title: brand,
                role: model.projection?.role,
                theme: theme,
                roles: RetailInviteRoles.all,
                roleAccess: RetailInviteRoles.accessNote(for:),
                defaultRoleLabel: "Cashier",
                noun: "store",
                accent: .rose,
                onError: { inviteError = $0 }
            )
            if let inviteError, !inviteError.isEmpty {
                Text(inviteError)
                    .font(.plusJakarta(size: 12, weight: .medium))
                    .foregroundStyle(theme.negative)
            }
        }
    }

    private var footerTitle: String {
        switch tab {
        case .invite, .catalogue: return "Done"
        case .suppliers: return "Save supplier"
        case .locations: return "Save locations"
        }
    }

    private var footerCaption: String {
        switch tab {
        case .invite: return "3 invites pending · roles apply on join"
        case .catalogue:
            let shown = max(filteredCatalogue.count, rows.isEmpty ? 4 : min(rows.count, 4))
            return "\(shown) of \(max(shown, 38)) products · \(category)"
        case .suppliers: return "Supplier saved · available in Purchase Order"
        case .locations: return "Locations saved · stock points update"
        }
    }

    private var suppliersList: some View {
        VStack(alignment: .leading, spacing: 12) {
            VStack(alignment: .leading, spacing: 6) {
                RoseFieldLabel(text: "Search")
                RoseTextField(
                    theme: theme,
                    placeholder: "Search suppliers, GSTIN or phone",
                    text: $supplierQuery
                )
            }
            if filteredSuppliers.isEmpty {
                RoseEmptyCard(
                    theme: theme,
                    title: "No suppliers yet",
                    bodyText: "Supplier is a master write. GSTIN drives input credit on Purchase / GRN."
                )
            } else {
                ForEach(filteredSuppliers) { row in
                    Button {
                        editing = row
                        formKind = .supplier
                    } label: {
                        supplierCard(row, preferred: isPreferred(row))
                    }
                    .buttonStyle(.plain)
                }
            }
            Button {
                editing = nil
                formKind = .supplier
            } label: {
                outlinePill("+ New supplier")
            }
            .buttonStyle(.plain)
        }
    }

    private var catalogueList: some View {
        VStack(alignment: .leading, spacing: 12) {
            RoseChipRow(
                theme: theme,
                options: retailCatalogueCategories,
                selection: $category,
                style: .ticket
            )
            if filteredCatalogue.isEmpty {
                RoseEmptyCard(
                    theme: theme,
                    title: "No products yet",
                    bodyText: "Create a product — GST posts on bill / POS Sale, never on Sales Order save."
                )
            } else {
                ForEach(filteredCatalogue) { row in
                    Button {
                        editing = row
                        formKind = stockKinds.contains(row.kind.uppercased()) ? .stockItem : .product
                    } label: {
                        catalogueRow(row)
                    }
                    .buttonStyle(.plain)
                }
            }
            Text(
                "Catalogue lists saved SKUs. GST badge is the product rate from its HSN — output GST posts on bill / POS Sale, never on Sales Order save."
            )
            .font(.plusJakarta(size: 12, weight: .medium))
            .foregroundStyle(OperatingV12.title)
            .padding(12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(RoseSheetChrome.chip)
            .overlay(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .strokeBorder(RoseSheetChrome.label.opacity(0.40), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))

            HStack(spacing: 8) {
                Button {
                    editing = nil
                    formKind = .product
                } label: {
                    outlinePill("+ New product")
                }
                .buttonStyle(.plain)
                Button {
                    editing = nil
                    formKind = .stockItem
                } label: {
                    outlinePill("+ New stock item")
                }
                .buttonStyle(.plain)
            }
        }
    }

    private var locationsList: some View {
        VStack(alignment: .leading, spacing: 12) {
            RetailStoreBrandingCard(
                theme: theme,
                companyId: model.companyId,
                momentId: model.momentId,
                onMessage: { brandingMessage = $0 }
            )
            if let brandingMessage, !brandingMessage.isEmpty {
                Text(brandingMessage)
                    .font(.plusJakarta(size: 12, weight: .medium))
                    .foregroundStyle(
                        brandingMessage.hasPrefix("JPEG only") || brandingMessage.hasPrefix("Could not")
                            ? theme.warning
                            : RoseSheetChrome.label
                    )
            }
            if rows.isEmpty {
                RoseEmptyCard(
                    theme: theme,
                    title: "No locations yet",
                    bodyText: "Locations are stock and sale points. Stock moves only on GRN, Sale or Transfer."
                )
            } else {
                ForEach(rows) { row in
                    Button {
                        editing = row
                        formKind = .location
                    } label: {
                        locationCard(row, highlighted: isBusy(row))
                    }
                    .buttonStyle(.plain)
                }
            }
            Button {
                editing = nil
                formKind = .location
            } label: {
                outlinePill("+ Add location")
            }
            .buttonStyle(.plain)
        }
    }

    private var stockKinds: Set<String> { ["MATERIAL", "CONSUMABLE", "ITEM"] }

    private var filteredSuppliers: [OperatingMasterPayload] {
        let q = supplierQuery.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard !q.isEmpty else { return rows }
        return rows.filter {
            $0.name.lowercased().contains(q)
                || ($0.gstin ?? "").lowercased().contains(q)
                || ($0.code ?? "").lowercased().contains(q)
        }
    }

    private var filteredCatalogue: [OperatingMasterPayload] {
        let products = rows.filter { !stockKinds.contains($0.kind.uppercased()) }
        let pool = products.isEmpty ? rows : products
        return pool.filter { row in
            let hay = "\(row.name) \(row.dimension ?? "") \(row.unit ?? "") \(row.code ?? "")".lowercased()
            return hay.contains(category.lowercased()) || pool.count <= 6
        }
    }

    private func supplierCard(_ row: OperatingMasterPayload, preferred: Bool) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 8) {
                Text(row.name)
                    .font(.plusJakarta(size: 14, weight: .bold))
                    .foregroundStyle(OperatingV12.title)
                    .lineLimit(1)
                Text(supplierCategory(row))
                    .font(.plusJakarta(size: 10, weight: .bold))
                    .foregroundStyle(OperatingV12.title)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(RoseSheetChrome.chipBorder)
                    .clipShape(Capsule())
                Spacer(minLength: 4)
                if preferred {
                    HStack(spacing: 4) {
                        Image(systemName: "star.fill")
                            .font(.system(size: 9, weight: .bold))
                        Text("Preferred")
                            .font(.plusJakarta(size: 10, weight: .bold))
                    }
                    .foregroundStyle(RoseSheetChrome.inkOnChip)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(RoseSheetChrome.label)
                    .clipShape(Capsule())
                } else {
                    editPill
                }
            }
            Text(supplierMeta(row))
                .font(.plusJakarta(size: 11, weight: .medium))
                .foregroundStyle(RoseSheetChrome.textMuted)
                .lineLimit(1)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(RoseSheetChrome.chip)
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .strokeBorder(
                    preferred ? theme.accent : RoseSheetChrome.chipBorder,
                    lineWidth: 1
                )
        )
        .shadow(color: preferred ? theme.accent.opacity(0.25) : .clear, radius: 10, y: 0)
    }

    private func catalogueRow(_ row: OperatingMasterPayload) -> some View {
        HStack(spacing: 10) {
            RoundedRectangle(cornerRadius: 2, style: .continuous)
                .fill(stockDot(row))
                .frame(width: 10, height: 10)
            VStack(alignment: .leading, spacing: 2) {
                Text(row.name)
                    .font(.plusJakarta(size: 14, weight: .semibold))
                    .foregroundStyle(OperatingV12.title)
                    .lineLimit(1)
                Text(catalogueMeta(row))
                    .font(.plusJakarta(size: 11, weight: .medium))
                    .foregroundStyle(RoseSheetChrome.textMuted)
                    .lineLimit(1)
            }
            Spacer(minLength: 4)
            Text(priceLabel(row))
                .font(.plusJakarta(size: 14, weight: .bold))
                .foregroundStyle(RoseSheetChrome.label)
            Text(gstBadge(row))
                .font(.plusJakarta(size: 10, weight: .bold))
                .foregroundStyle(RoseSheetChrome.label)
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
                .background(RoseSheetChrome.deepRose)
                .clipShape(Capsule())
            editPill
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(RoseSheetChrome.chip)
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .strokeBorder(RoseSheetChrome.chipBorder, lineWidth: 1)
        )
    }

    private func locationCard(_ row: OperatingMasterPayload, highlighted: Bool) -> some View {
        HStack(spacing: 8) {
            VStack(alignment: .leading, spacing: 2) {
                Text(row.name)
                    .font(.plusJakarta(size: 15, weight: .bold))
                    .foregroundStyle(OperatingV12.title)
                Text(locationMeta(row))
                    .font(.plusJakarta(size: 11, weight: .medium))
                    .foregroundStyle(RoseSheetChrome.textMuted)
            }
            Spacer(minLength: 4)
            let busy = isBusy(row)
            Text(busy ? "Busy" : "Open")
                .font(.plusJakarta(size: 10, weight: .bold))
                .foregroundStyle(busy ? Color.white : RoseSheetChrome.inkOnChip)
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
                .background(busy ? theme.accent : RoseSheetChrome.label)
                .clipShape(Capsule())
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(RoseSheetChrome.chip)
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .strokeBorder(
                    highlighted ? theme.accent : RoseSheetChrome.chipBorder,
                    lineWidth: 1
                )
        )
        .shadow(color: highlighted ? theme.accent.opacity(0.25) : .clear, radius: 10, y: 0)
    }

    private var editPill: some View {
        Text("Edit")
            .font(.plusJakarta(size: 12, weight: .bold))
            .foregroundStyle(OperatingV12.title)
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(theme.surface)
            .overlay(
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .strokeBorder(RoseSheetChrome.chipBorder, lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
    }

    private func outlinePill(_ title: String) -> some View {
        Text(title)
            .font(.plusJakarta(size: 12, weight: .bold))
            .foregroundStyle(RoseSheetChrome.label)
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(theme.surface)
            .overlay(
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .strokeBorder(theme.accent, lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
    }

    private func isPreferred(_ row: OperatingMasterPayload) -> Bool {
        let blob = "\(row.status ?? "") \(row.code ?? "") \(row.unit ?? "")".uppercased()
        return blob.contains("PREFERRED")
    }

    private func isBusy(_ row: OperatingMasterPayload) -> Bool {
        let blob = "\(row.status ?? "") \(row.code ?? "")".uppercased()
        return blob.contains("BUSY")
    }

    private func supplierCategory(_ row: OperatingMasterPayload) -> String {
        let code = (row.code ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
        if !code.isEmpty, code.count <= 16, !code.contains("@") { return code }
        return row.kind.capitalized
    }

    private func supplierMeta(_ row: OperatingMasterPayload) -> String {
        let gst = row.gstin.nilIfBlank.map { "GSTIN \($0)" } ?? "GSTIN —"
        let phone = row.code.nilIfBlank.flatMap { code in
            code.hasPrefix("+") || code.first?.isNumber == true ? code : nil
        } ?? "Phone —"
        return "\(gst) · \(phone)"
    }

    private func catalogueMeta(_ row: OperatingMasterPayload) -> String {
        let cat = row.dimension.nilIfBlank
            ?? row.unit.nilIfBlank
            ?? category
        let aisle = row.code.nilIfBlank.map { "Aisle \($0)" } ?? "Aisle —"
        return "\(cat) · \(aisle)"
    }

    private func locationMeta(_ row: OperatingMasterPayload) -> String {
        let type: String = {
            switch row.kind.uppercased() {
            case "WAREHOUSE": return "Warehouse"
            case "CHANNEL", "COUNTER": return "Counter"
            case "STORE": return "Store"
            default: return "Store"
            }
        }()
        let open = row.conversionToBase.nilIfBlank ?? "—"
        return "\(type) · \(open) open bills"
    }

    private func priceLabel(_ row: OperatingMasterPayload) -> String {
        if let sell = row.sellPrice.nilIfBlank {
            return OperatingMoneyFormat.format(model.currency, sell)
        }
        if let purchase = row.purchasePrice.nilIfBlank {
            return OperatingMoneyFormat.format(model.currency, purchase)
        }
        return "—"
    }

    private func gstBadge(_ row: OperatingMasterPayload) -> String {
        if let rate = row.gstRate.nilIfBlank { return "GST \(rate)%" }
        return "GST —"
    }

    private func stockDot(_ row: OperatingMasterPayload) -> Color {
        let status = (row.status ?? "").uppercased()
        if status.contains("INACTIVE") || status.contains("HOLD") {
            return theme.accent
        }
        return Color(hex: "#22C55E")
    }

    private func roleLabel(_ raw: String) -> String {
        switch raw.uppercased() {
        case "OWNER_ADMIN", "OWNER": return "Owner"
        default: return raw.replacingOccurrences(of: "_", with: " ").capitalized
        }
    }

    private func showSavedToast() {
        withAnimation { showToast = true }
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.4) {
            withAnimation { showToast = false }
        }
    }
}

/// Retail invite chips — Figma `2506:29333` Owner / Manager / Cashier / Stock / Floor.
enum RetailInviteRoles {
    static let all: [BusinessInviteRole] = [
        .init(code: "FINANCE", label: "Owner"),
        .init(code: "APPROVER", label: "Manager"),
        .init(code: "OPERATIONS_LEAD", label: "Cashier"),
        .init(code: "CONTRIBUTOR", label: "Stock"),
        .init(code: "CONTRIBUTOR", label: "Floor"),
    ]

    static func accessNote(for roleCode: String) -> String {
        switch roleCode {
        case "FINANCE":
            return "Owner → full access · reports, payouts and invites. The role you pick sets their access when they join"
        case "APPROVER":
            return "Manager → POS oversight, stock & approvals · no payouts. The role you pick sets their access when they join"
        case "OPERATIONS_LEAD":
            return "Cashier → POS, bills & returns · no reports or payouts. The role you pick sets their access when they join"
        default:
            return "Stock / Floor → stock moves & floor ops · no reports or payouts. The role you pick sets their access when they join"
        }
    }
}

private extension String {
    var nilIfBlank: String? {
        let trimmed = trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : trimmed
    }
}

private extension Optional where Wrapped == String {
    var nilIfBlank: String? {
        self?.nilIfBlank
    }
}
