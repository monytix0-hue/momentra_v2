import SwiftUI

/// SVC masters lists — Figma Invite & QR / Services / Clients / Rooms & Staff.
/// Structural port of Android `ServiceMastersSheet` with teal chrome.
enum ServiceMastersTab: String, CaseIterable, Identifiable {
    case clients
    case services
    case roomsStaff
    case invite

    var id: String { rawValue }

    var title: String {
        switch self {
        case .clients: return "Clients"
        case .services: return "Services"
        case .roomsStaff: return "Rooms & Staff"
        case .invite: return "Invite & QR"
        }
    }

    var operatingTab: OperatingMasterTab {
        switch self {
        case .clients: return .principals
        case .services: return .catalogue
        case .roomsStaff: return .locations
        case .invite: return .invite
        }
    }

    static func from(_ tab: OperatingMasterTab) -> ServiceMastersTab {
        switch tab {
        case .principals: return .clients
        case .catalogue: return .services
        case .locations: return .roomsStaff
        case .invite, .priceLists: return .invite
        }
    }

    var kinds: Set<String> { operatingTab.kinds }

    var why: String {
        switch self {
        case .invite:
            return "Invite grants a role and its access. Joining ≠ approval ≠ payment."
        case .services:
            return "Catalogue lists saved offerings. GST badge is the SAC rate — output GST posts on Finalize Charge, never on booking save."
        case .clients:
            return "Client is a master write. Booking picks a saved client; GST posts on Finalize Charge, never on booking save."
        case .roomsStaff:
            return "Rooms and staff are capacity. Booking holds a slot; only Finalize Charge creates revenue."
        }
    }

    var subtitle: String {
        switch self {
        case .invite: return "invite grants a role"
        case .services: return "Services catalogue · offerings & packages with price, duration + GST"
        case .clients: return "Client master · guests & business clients · GSTIN only for B2B invoices"
        case .roomsStaff: return "Rooms, chairs & staff · where and by whom services are delivered"
        }
    }
}

private let serviceCategories = ["Hair", "Skin", "Spa", "Wellness", "Nails"]

struct ServiceMastersView: View {
    let theme: OperatingTheme
    @ObservedObject var model: BusinessOperatingModel
    var initialTab: OperatingMasterTab = .principals

    @Environment(\.dismiss) private var dismiss
    @State private var tab: ServiceMastersTab
    @State private var formKind: ServiceMasterFormKind?
    @State private var editing: OperatingMasterPayload?
    @State private var category = serviceCategories[0]
    @State private var clientQuery = ""
    @State private var showToast = false
    @State private var inviteError: String?

    init(theme: OperatingTheme, model: BusinessOperatingModel, initialTab: OperatingMasterTab = .principals) {
        self.theme = theme
        self.model = model
        self.initialTab = initialTab
        _tab = State(initialValue: ServiceMastersTab.from(initialTab))
    }

    private var brand: String {
        let title = model.title.trimmingCharacters(in: .whitespacesAndNewlines)
        return title.isEmpty ? "BrightCare" : title
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
                    TealGrabber()
                    TealSheetHeader(
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
                    case .services:
                        servicesCatalogue
                    case .clients:
                        clientsList
                    case .roomsStaff:
                        roomsStaffList
                    }

                    TealWhyBox(theme: theme, text: tab.why)
                    TealStickyFooter(
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
                TealSavedToast()
                    .padding(.top, 12)
                    .transition(.move(edge: .top).combined(with: .opacity))
            }
        }
        .task { if model.masters.isEmpty { await model.loadMasters() } }
        .sheet(item: $formKind) { kind in
            ServiceMasterFormSheet(
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
                ForEach(ServiceMastersTab.allCases) { item in
                    TealModeChip(theme: theme, text: item.title, selected: tab == item) {
                        tab = item
                    }
                }
            }
        }
    }

    private var ownerLine: String {
        model.projection?.role.nilIfBlank ?? "Owner"
    }

    /// Figma `2530:29334` — live mint / contacts / QR via shared `BusinessInviteQr`.
    private var inviteBlock: some View {
        VStack(alignment: .leading, spacing: 12) {
            BusinessInviteQr(
                companyId: model.companyId,
                title: brand,
                role: model.projection?.role,
                theme: theme,
                roles: ServiceInviteRoles.all,
                roleAccess: ServiceInviteRoles.accessNote(for:),
                defaultRoleLabel: "Stylist",
                noun: "service",
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
        case .invite, .services: return "Done"
        case .clients: return "Save client"
        case .roomsStaff: return "Save rooms"
        }
    }

    private var footerCaption: String {
        switch tab {
        case .invite: return "3 invites pending · roles apply on join"
        case .services:
            let shown = max(filteredServices.count, rows.isEmpty ? 4 : rows.count)
            return "\(shown) of \(max(shown, 26)) services · \(category)"
        case .clients: return "Client saved · available in Booking"
        case .roomsStaff: return "Rooms saved · calendar slots update"
        }
    }

    private var servicesCatalogue: some View {
        VStack(alignment: .leading, spacing: 12) {
            TealChipRow(theme: theme, options: serviceCategories, selection: $category, style: .lime)
            HStack(spacing: 8) {
                Button {
                    editing = nil
                    formKind = .service
                } label: {
                    outlinePill("+ New service")
                }
                .buttonStyle(.plain)
                Button {
                    editing = nil
                    formKind = .package
                } label: {
                    outlinePill("+ New package")
                }
                .buttonStyle(.plain)
            }
            if filteredServices.isEmpty {
                TealEmptyCard(
                    theme: theme,
                    title: "No offerings yet",
                    bodyText: "Create a service or package — GST posts on Finalize Charge, never on save."
                )
            } else {
                ForEach(filteredServices) { row in
                    Button {
                        editing = row
                        formKind = row.kind.uppercased() == "PACKAGE" ? .package : .service
                    } label: {
                        masterRow(
                            title: row.name,
                            meta: serviceMeta(row),
                            badge: gstBadge(row)
                        )
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private var clientsList: some View {
        VStack(alignment: .leading, spacing: 12) {
            VStack(alignment: .leading, spacing: 6) {
                TealFieldLabel(text: "Search")
                TealTextField(theme: theme, placeholder: "Search clients, phone or GSTIN", text: $clientQuery)
            }
            Button {
                editing = nil
                formKind = .client
            } label: {
                outlinePill("+ New client")
            }
            .buttonStyle(.plain)
            if filteredClients.isEmpty {
                TealEmptyCard(
                    theme: theme,
                    title: "No clients yet",
                    bodyText: "Client is a master write. Booking picks a saved client."
                )
            } else {
                ForEach(filteredClients) { row in
                    Button {
                        editing = row
                        formKind = .client
                    } label: {
                        masterRow(
                            title: row.name,
                            meta: row.gstin.nilIfBlank ?? "Guest · walk-in OK",
                            badge: clientBadge(row)
                        )
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private var roomsStaffList: some View {
        VStack(alignment: .leading, spacing: 12) {
            Button {
                editing = nil
                formKind = .roomStaff
            } label: {
                outlinePill("+ Add room / staff")
            }
            .buttonStyle(.plain)
            if rows.isEmpty {
                TealEmptyCard(
                    theme: theme,
                    title: "No capacity yet",
                    bodyText: "Rooms and staff are capacity. Booking holds a slot; only Finalize Charge creates revenue."
                )
            } else {
                ForEach(rows) { row in
                    Button {
                        editing = row
                        formKind = .roomStaff
                    } label: {
                        masterRow(
                            title: row.name,
                            meta: row.kind.capitalized,
                            badge: capacityBadge(row)
                        )
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private var filteredServices: [OperatingMasterPayload] {
        rows.filter { row in
            let hay = "\(row.name) \(row.dimension ?? "") \(row.unit ?? "") \(row.code ?? "")".lowercased()
            return hay.contains(category.lowercased()) || rows.count <= 6
        }
    }

    private var filteredClients: [OperatingMasterPayload] {
        let q = clientQuery.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard !q.isEmpty else { return rows }
        return rows.filter { $0.name.lowercased().contains(q) }
    }

    /// Figma outline action (`+ New client` / rooms) — lime border, cream ink.
    private func outlinePill(_ title: String) -> some View {
        Text(title)
            .font(.plusJakarta(size: 12, weight: .bold))
            .foregroundStyle(Color(hex: "#FEF3C7"))
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .overlay(
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .strokeBorder(TealSheetChrome.lime, lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
    }

    private func masterRow(title: String, meta: String, badge: String) -> some View {
        HStack(alignment: .top, spacing: 10) {
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.plusJakarta(size: 14, weight: .bold))
                    .foregroundStyle(OperatingV12.title)
                Text(meta)
                    .font(.plusJakarta(size: 11, weight: .medium))
                    .foregroundStyle(OperatingV12.subtitle)
            }
            Spacer(minLength: 8)
            Text(badge)
                .font(.plusJakarta(size: 10, weight: .bold))
                .foregroundStyle(TealSheetChrome.inkOnAccent)
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
                .background(theme.accent)
                .clipShape(Capsule())
        }
        .padding(12)
        .background(theme.surface)
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .strokeBorder(theme.accent.opacity(0.28), lineWidth: 1)
        )
    }

    private func serviceMeta(_ row: OperatingMasterPayload) -> String {
        let price = row.sellPrice.map { OperatingMoneyFormat.format(model.currency, $0) } ?? "—"
        let duration = row.unit.nilIfBlank ?? "45 min"
        let sac = row.hsn.nilIfBlank.map { "SAC \($0)" } ?? "SAC —"
        return "\(duration) · \(price) · \(sac)"
    }

    private func gstBadge(_ row: OperatingMasterPayload) -> String {
        if let rate = row.gstRate.nilIfBlank { return "\(rate)%" }
        return row.kind.uppercased() == "PACKAGE" ? "Pkg" : "GST"
    }

    private func clientBadge(_ row: OperatingMasterPayload) -> String {
        if (row.code ?? "").uppercased().contains("VIP") { return "VIP" }
        if row.gstin.nilIfBlank != nil { return "Corporate" }
        return "Regular"
    }

    private func capacityBadge(_ row: OperatingMasterPayload) -> String {
        let status = (row.status ?? "").uppercased()
        if status.contains("BUSY") { return "Busy" }
        return "Open"
    }

    private func showSavedToast() {
        withAnimation { showToast = true }
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.4) {
            withAnimation { showToast = false }
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
