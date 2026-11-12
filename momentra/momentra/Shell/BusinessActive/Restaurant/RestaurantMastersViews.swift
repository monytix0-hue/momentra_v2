import SwiftUI

/// Figma RST-MASTER Invite / Vendors / Menu / Stations (`2465:*`).
enum RestaurantMastersTab: String, CaseIterable, Identifiable {
    case vendors, menu, stations, invite
    var id: String { rawValue }

    var title: String {
        switch self {
        case .vendors: return "Vendors"
        case .menu: return "Menu"
        case .stations: return "Stations"
        case .invite: return "Invite & QR"
        }
    }

    static func from(_ tab: OperatingMasterTab) -> RestaurantMastersTab {
        switch tab {
        case .principals, .priceLists: return .vendors
        case .catalogue: return .menu
        case .locations: return .stations
        case .invite: return .invite
        }
    }

    var kinds: Set<String> {
        switch self {
        case .vendors: return ["VENDOR", "SUPPLIER"]
        case .menu: return ["MENU_DISH", "DISH", "MENU_ITEM", "PRODUCT"]
        case .stations: return ["STATION", "KITCHEN_STATION"]
        case .invite: return []
        }
    }

    var why: String {
        switch self {
        case .invite: return RestaurantCaptureCopy.inviteWhy
        case .vendors: return RestaurantCaptureCopy.vendorsWhy
        case .menu: return RestaurantCaptureCopy.menuWhy
        case .stations: return RestaurantCaptureCopy.stationsWhy
        }
    }

    var subtitle: String {
        switch self {
        case .invite: return "invite grants a role"
        case .vendors: return "Supplier master · GSTIN drives input credit on Purchase / GRN"
        case .menu: return "Menu master · dishes with price + GST before they reach SO / POS"
        case .stations: return "Kitchen & service stations · KOT routing targets"
        }
    }
}

struct RestaurantMastersView: View {
    let theme: OperatingTheme
    @ObservedObject var model: BusinessOperatingModel
    var initialTab: OperatingMasterTab = .catalogue

    @Environment(\.dismiss) private var dismiss
    @State private var tab: RestaurantMastersTab
    @State private var formKind: RestaurantMasterFormKind?
    @State private var category = "Mains"
    @State private var showToast = false
    @State private var inviteError: String?

    init(theme: OperatingTheme, model: BusinessOperatingModel, initialTab: OperatingMasterTab = .catalogue) {
        self.theme = theme
        self.model = model
        self.initialTab = initialTab
        _tab = State(initialValue: RestaurantMastersTab.from(initialTab))
    }

    private var brand: String {
        let t = model.title.trimmingCharacters(in: .whitespacesAndNewlines)
        return t.isEmpty ? "Spice Route" : t
    }

    private var rows: [DemoMasterRow] {
        let source = model.masters.isEmpty ? (model.projection?.masters ?? []) : model.masters
        let live = source
            .filter { tab.kinds.contains($0.kind.uppercased()) }
            .sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
            .map { DemoMasterRow(id: $0.id, name: $0.name, sellPrice: $0.sellPrice, gstRate: $0.gstRate, gstin: $0.gstin) }
        if !live.isEmpty { return live }
        switch tab {
        case .menu: return demoMenu
        case .vendors: return demoVendors
        case .stations: return demoStations
        case .invite: return []
        }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                EmberGrabber()
                EmberSheetHeader(
                    title: tab == .invite ? "Invite team · \(brand)" : tab.title,
                    subtitle: tab == .invite
                        ? "\(brand) Kitchen · Kiran Rao (Owner) · \(tab.subtitle)"
                        : tab.subtitle,
                    onClose: { dismiss() }
                )

                switch tab {
                case .invite:
                    inviteBlock
                case .vendors:
                    vendorsList
                case .menu:
                    menuList
                case .stations:
                    stationsList
                }

                EmberWhyBox(text: tab.why)
                EmberStickyFooter(
                    title: footerTitle,
                    caption: footerCaption,
                    action: {
                        showToast = true
                        dismiss()
                    }
                )
                if showToast {
                    Text(RestaurantCaptureCopy.savedToast)
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(EmberSheetChrome.gold)
                }
            }
            .padding(16)
            .padding(.bottom, 28)
        }
        .background(EmberSheetChrome.sheet)
        .task { if model.masters.isEmpty { await model.loadMasters() } }
        .sheet(item: $formKind) { kind in
            RestaurantMasterFormSheet(theme: theme, kind: kind, model: model) { _ in
                formKind = nil
                showToast = true
                Task { await model.loadMasters() }
            }
            .presentationDetents([.large])
            .preferredColorScheme(.dark)
        }
    }

    private var footerTitle: String {
        switch tab {
        case .invite, .menu: return "Done"
        case .vendors: return "Save vendor"
        case .stations: return "Save stations"
        }
    }

    private var footerCaption: String {
        switch tab {
        case .invite: return "3 invites pending · roles apply on join"
        case .vendors: return "Vendor saved · available in Purchase Order"
        case .menu: return "\(max(rows.count, 4)) of 38 dishes · \(category)"
        case .stations: return "Stations saved · KOT routing updates"
        }
    }

    private var inviteBlock: some View {
        VStack(alignment: .leading, spacing: 12) {
            BusinessInviteQr(
                companyId: model.companyId,
                title: brand,
                role: model.projection?.role,
                theme: theme,
                roles: RestaurantInviteRoles.all,
                roleAccess: RestaurantInviteRoles.accessNote(for:),
                defaultRoleLabel: "Cashier",
                noun: "kitchen",
                accent: .ember,
                onError: { inviteError = $0 }
            )
            if let inviteError, !inviteError.isEmpty {
                Text(inviteError)
                    .font(.plusJakarta(size: 12, weight: .medium))
                    .foregroundStyle(theme.negative)
            }
        }
    }

    private var vendorsList: some View {
        VStack(alignment: .leading, spacing: 8) {
            ForEach(rows) { master in
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(master.name).font(.system(size: 14, weight: .semibold)).foregroundStyle(EmberSheetChrome.textPrimary)
                        Text(master.gstin.map { "GSTIN \($0)" } ?? "GSTIN not recorded")
                            .font(.system(size: 11)).foregroundStyle(EmberSheetChrome.textBody)
                    }
                    Spacer()
                    Button("Edit") { formKind = .vendor }
                        .font(.system(size: 12, weight: .bold))
                        .foregroundStyle(EmberSheetChrome.textPrimary)
                }
                .padding(12)
                .background(EmberSheetChrome.chip)
                .clipShape(RoundedRectangle(cornerRadius: 14))
            }
            Button("+ New vendor") { formKind = .vendor }
                .font(.system(size: 12, weight: .bold))
                .foregroundStyle(EmberSheetChrome.gold)
        }
    }

    private var menuList: some View {
        VStack(alignment: .leading, spacing: 8) {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 6) {
                    ForEach(RestaurantCaptureCopy.menuCategories, id: \.self) { cat in
                        EmberModeChip(text: cat, selected: category == cat) { category = cat }
                    }
                }
            }
            ForEach(rows) { master in
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(master.name).font(.system(size: 14, weight: .semibold)).foregroundStyle(EmberSheetChrome.textPrimary)
                        Text("\(category) · Tandoor").font(.system(size: 11)).foregroundStyle(EmberSheetChrome.textBody)
                    }
                    Spacer()
                    if let price = master.sellPrice {
                        Text("₹\(price)").font(.system(size: 14, weight: .bold)).foregroundStyle(EmberSheetChrome.gold)
                    }
                    Text("GST \(master.gstRate ?? "5")%")
                        .font(.system(size: 10, weight: .bold))
                        .foregroundStyle(EmberSheetChrome.gold)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(EmberSheetChrome.wine)
                        .clipShape(Capsule())
                    Button("Edit") { formKind = .dish }
                        .font(.system(size: 12, weight: .bold))
                }
                .padding(12)
                .background(EmberSheetChrome.chip)
                .clipShape(RoundedRectangle(cornerRadius: 14))
            }
            Button("+ New dish") { formKind = .dish }
                .font(.system(size: 12, weight: .bold))
                .foregroundStyle(EmberSheetChrome.gold)
        }
    }

    private var stationsList: some View {
        VStack(alignment: .leading, spacing: 8) {
            ForEach(Array(rows.enumerated()), id: \.element.id) { idx, master in
                let tickets = [6, 3, 1, 4, 2][min(idx, 4)]
                let busy = tickets >= 4
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(master.name).font(.system(size: 14, weight: .semibold)).foregroundStyle(EmberSheetChrome.textPrimary)
                        Text("Kitchen · \(tickets) open tickets").font(.system(size: 11)).foregroundStyle(EmberSheetChrome.textBody)
                    }
                    Spacer()
                    Text(busy ? "Busy" : "Open")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundStyle(busy ? Color(red: 0.96, green: 0.25, blue: 0.37) : EmberSheetChrome.amber)
                }
                .padding(12)
                .background(EmberSheetChrome.chip)
                .overlay(RoundedRectangle(cornerRadius: 14).stroke(busy ? EmberSheetChrome.ember : EmberSheetChrome.hairline, lineWidth: 1))
                .clipShape(RoundedRectangle(cornerRadius: 14))
            }
            Button("+ Add station") { formKind = .station }
                .font(.system(size: 12, weight: .bold))
                .foregroundStyle(EmberSheetChrome.gold)
        }
    }

    private var demoMenu: [DemoMasterRow] {
        [
            .init(id: "d1", name: "Butter Chicken", sellPrice: "480", gstRate: "5"),
            .init(id: "d2", name: "Paneer Tikka Masala", sellPrice: "420", gstRate: "5"),
            .init(id: "d3", name: "Dal Makhani", sellPrice: "340", gstRate: "5"),
            .init(id: "d4", name: "Garlic Naan", sellPrice: "90", gstRate: "5"),
        ]
    }

    private var demoVendors: [DemoMasterRow] {
        [
            .init(id: "v1", name: "Fresh Farm", gstin: "27AABCF1234K1Z5"),
            .init(id: "v2", name: "Metro Dairy", gstin: "27AAECM5678L1Z2"),
            .init(id: "v3", name: "Spice Hub", gstin: "27AAHCS9012M1Z9"),
        ]
    }

    private var demoStations: [DemoMasterRow] {
        ["Tandoor", "Hot", "Cold", "Bar", "Floor"].enumerated().map {
            .init(id: "s\($0.offset)", name: $0.element)
        }
    }
}

private struct DemoMasterRow: Identifiable {
    let id: String
    let name: String
    var sellPrice: String? = nil
    var gstRate: String? = nil
    var gstin: String? = nil
}

enum RestaurantInviteRoles {
    static let all: [BusinessInviteRole] = [
        .init(code: "FINANCE", label: "Owner"),
        .init(code: "APPROVER", label: "Manager"),
        .init(code: "OPERATIONS_LEAD", label: "Cashier"),
        .init(code: "CONTRIBUTOR", label: "Kitchen"),
        .init(code: "CONTRIBUTOR", label: "Waiter"),
    ]

    static func accessNote(for roleCode: String) -> String {
        switch roleCode {
        case "FINANCE":
            return "Owner → full kitchen · reports, payouts and invites. The role you pick sets their access when they join"
        case "APPROVER":
            return "Manager → floor, menu, approvals · no payouts. The role you pick sets their access when they join"
        case "OPERATIONS_LEAD":
            return "Cashier → POS, bills & KOT view · no reports or payouts. The role you pick sets their access when they join"
        default:
            return "Kitchen / Waiter → KOT pass & floor tickets · no reports or payouts. The role you pick sets their access when they join"
        }
    }
}
