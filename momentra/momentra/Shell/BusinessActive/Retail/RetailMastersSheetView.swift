import SwiftUI

/// Masters surfaces: Suppliers `2506:29417`, Catalogue `2506:29475`,
/// Locations `2506:29555`, Invite & QR `2506:29333`.
///
/// Rows come from `model.masters` — the projection derived from posted events. A
/// master cannot be created ahead of use, so the "new" action opens the existing
/// capture sheet instead of pretending a catalogue CRUD endpoint exists.
enum RetailMastersTab: String, Identifiable, CaseIterable {
    case suppliers, catalogue, locations, invite

    var id: String { rawValue }

    var title: String {
        switch self {
        case .suppliers: return "Suppliers"
        case .catalogue: return "Catalogue"
        case .locations: return "Locations"
        case .invite: return "Invite & QR"
        }
    }

    var icon: String {
        switch self {
        case .suppliers: return RetailTileIcons.suppliers
        case .catalogue: return RetailTileIcons.catalogue
        case .locations: return RetailTileIcons.locations
        case .invite: return RetailTileIcons.invite
        }
    }

    /// Master `kind` values this tab lists. `masters` is derived, so the same record
    /// can arrive under a couple of aliases per business subtype.
    var kinds: Set<String> {
        switch self {
        case .suppliers: return ["SUPPLIER", "PARTNER", "VENDOR", "PRINCIPAL"]
        case .catalogue: return ["PRODUCT", "ITEM", "SKU", "MATERIAL", "CONSUMABLE"]
        case .locations: return ["LOCATION", "STORE", "CHANNEL"]
        case .invite: return []
        }
    }

    /// Singular noun used by the create action (`New supplier`, `New product`).
    var singular: String {
        switch self {
        case .suppliers: return "supplier"
        case .catalogue: return "product"
        case .locations: return "location"
        case .invite: return "invite"
        }
    }

    var emptyNote: String {
        switch self {
        case .catalogue:
            return "Catalogue rows only change on an explicit master record — never from a quick sale."
        case .locations:
            return "A location groups stock. Opening a new one does not move existing quantities."
        case .invite:
            return "Invites never grant finance visibility without an explicit role."
        case .suppliers:
            return "Named partners are required for credit and payable tracking."
        }
    }
}

struct RetailMastersSheetView: View {
    let tab: RetailMastersTab
    let model: OperatingProjectionPayload
    var momentId: String? = nil
    var onRecord: (RetailEventDraft) async throws -> Void
    var onError: (String) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var createOpen = false

    private var rows: [OperatingMasterPayload] {
        guard !tab.kinds.isEmpty else { return [] }
        return (model.masters ?? []).filter { tab.kinds.contains($0.kind.uppercased()) }
    }

    private var canCreate: Bool {
        tab != .invite && model.role == "OWNER_ADMIN"
    }

    var body: some View {
        RoseScreenBackground {
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    HStack {
                        Spacer()
                        Capsule()
                            .fill(Color.white.opacity(0.2))
                            .frame(width: 40, height: 5)
                        Spacer()
                    }
                    .padding(.top, 8)

                    header

                    if tab == .invite {
                        inviteSurface
                    } else {
                        masterList
                    }
                    Spacer(minLength: 24)
                }
                .padding(.horizontal, RetailRose.gutter)
                .padding(.bottom, 24)
            }
        }
        .presentationDragIndicator(.hidden)
        .sheet(isPresented: $createOpen) {
            OperatingCaptureSheet(
                title: "New \(tab.singular)",
                icon: tab.icon,
                eventType: "UPDATE",
                mode: "MASTERS",
                model: model,
                onSubmit: { draft in
                    do { try await onRecord(RetailEventDraft(draft)) }
                    catch { onError(error.localizedDescription) }
                },
                onDismiss: { createOpen = false }
            )
        }
    }

    private var header: some View {
        HStack(spacing: 10) {
            Text(tab.title)
                .font(.plusJakarta(size: 24, weight: .bold))
                .foregroundStyle(RetailRose.textPrimary)
                .lineLimit(1)
                .minimumScaleFactor(0.7)
                .frame(maxWidth: .infinity, alignment: .leading)
            Button("Close") { dismiss() }
                .font(.plusJakarta(size: 12, weight: .bold))
                .foregroundStyle(RetailRose.textBody)
                .padding(.horizontal, 10)
                .padding(.vertical, 6)
        }
    }

    private var masterList: some View {
        VStack(alignment: .leading, spacing: 10) {
            if canCreate {
                Button { createOpen = true } label: {
                    Text("New \(tab.singular)")
                        .font(.plusJakarta(size: 15, weight: .bold))
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(RetailRose.rose)
                        .clipShape(RoundedRectangle(cornerRadius: RetailRose.rCardTight, style: .continuous))
                        .shadow(color: RetailRose.rose.opacity(0.45), radius: 12, x: 0, y: 8)
                }
                .buttonStyle(.plain)
            }

            if rows.isEmpty {
                RoseEmpty(title: "Nothing here yet", body: "Records you add will be listed here.")
            } else {
                VStack(spacing: 8) {
                    ForEach(rows) { master in
                        AccentRow(
                            title: master.name.isEmpty ? "Unnamed" : master.name,
                            caption: caption(master),
                            accent: RetailRose.rose,
                            severityLabel: (master.barcode?.isEmpty == false) ? "Scan" : nil,
                            severityColor: RetailRose.cyan
                        )
                    }
                }
            }

            RoseWhyNote(body: tab.emptyNote)
        }
    }

    private func caption(_ master: OperatingMasterPayload) -> String {
        [
            master.kind.lowercased().capitalized,
            master.unit?.isEmpty == false ? "UOM \(master.unit!)" : nil,
            master.gstRate?.isEmpty == false ? "GST \(master.gstRate!)%" : nil,
        ]
        .compactMap { $0 }
        .joined(separator: " · ")
    }

    /// Invite & QR `2506:29333` runs inside this sheet's chrome, so the tab renders the
    /// real surface (minted invite, QR, role chips) rather than a second nested sheet.
    private var inviteSurface: some View {
        RetailInviteSheetView(
            storeName: storeName,
            role: model.role,
            momentId: momentId,
            embedded: true
        )
    }

    private var storeName: String {
        let title = (model.title ?? "").trimmingCharacters(in: .whitespaces)
        return title.isEmpty ? "this store" : title
    }
}
