import SwiftUI

enum WeddingQuickAddKind: String, Identifiable, CaseIterable {
    case participant
    case planning
    case expense
    case budget
    case contribution
    case settle
    case vendor
    case attendance
    case update
    case poll
    case memory

    var id: String { rawValue }

    var isLive: Bool {
        switch self {
        case .expense, .budget, .contribution, .settle, .planning, .poll, .update, .memory, .participant: return true
        default: return false
        }
    }

    var label: String {
        switch self {
        case .participant: return "Participant"
        case .planning: return "Planning Item"
        case .expense: return "Expense"
        case .budget: return "Budget"
        case .contribution: return "Contribution"
        case .settle: return "Settle"
        case .vendor: return "Vendor"
        case .attendance: return "Attendance"
        case .update: return "Update"
        case .poll: return "Poll"
        case .memory: return "Memory"
        }
    }

    var emoji: String {
        switch self {
        case .participant: return "👤"
        case .planning: return "📋"
        case .expense: return "💳"
        case .budget: return "💰"
        case .contribution: return "🎁"
        case .settle: return "⚖️"
        case .vendor: return "🏪"
        case .attendance: return "✅"
        case .update: return "📢"
        case .poll: return "📊"
        case .memory: return "📷"
        }
    }

    var gradient: [Color] {
        switch self {
        case .participant: return [Color(hex: "#FA7387"), Color(hex: "#E01C4D")]
        case .planning: return [Color(hex: "#F573B5"), Color(hex: "#DB2675")]
        case .expense: return [Color(hex: "#BF26D4"), Color(hex: "#871A8F")]
        case .budget: return [Color(hex: "#FC7085"), Color(hex: "#E83359")]
        case .contribution: return [Color(hex: "#D945F0"), Color(hex: "#A31CB0")]
        case .settle: return [Color(hex: "#059669"), Color(hex: "#10B981")]
        case .vendor: return [Color(hex: "#ED8CB8"), Color(hex: "#D14D85")]
        case .attendance: return [Color(hex: "#BD175C"), Color(hex: "#820F42")]
        case .update: return [Color(hex: "#E878FA"), Color(hex: "#BF26D4")]
        case .poll: return [Color(hex: "#A854F7"), Color(hex: "#7D3BED")]
        case .memory: return [Color(hex: "#F53D5E"), Color(hex: "#C71F40")]
        }
    }
}

/// Figma 584:16938 — Wedding Quick Add hub.
struct WeddingQuickAddHubView: View {
    let momentTitle: String?
    let hasActiveMoment: Bool
    var capabilityCodes: [String]? = nil
    var onClose: () -> Void
    var onTile: (WeddingQuickAddKind) -> Void
    var onNewMoment: () -> Void = {}
    var onJoinCode: (String) -> Void = { _ in }

    @State private var search = ""
    @State private var showScanner = false

    private let columns = [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())]

    private var tiles: [WeddingQuickAddKind] {
        let q = search.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        let all = WeddingQuickAddKind.allCases
        guard !q.isEmpty else { return all }
        return all.filter { $0.label.lowercased().contains(q) }
    }

    var body: some View {
        List {
            Section {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Quick Add")
                            .font(.plusJakarta(size: 18, weight: .bold))
                            .foregroundStyle(Color(hex: "#F7F5F2"))
                        Text("Bring your experience to life")
                            .font(.plusJakarta(size: 10))
                            .foregroundStyle(WeddingActiveTheme.muted)
                    }
                    Spacer()
                    Button(action: onClose) {
                        Text("✕")
                            .font(.plusJakarta(size: 12, weight: .bold))
                            .foregroundStyle(WeddingActiveTheme.text)
                            .frame(width: 32, height: 32)
                            .background(Color(hex: "#171618"))
                            .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color(hex: "#403C40")))
                            .clipShape(RoundedRectangle(cornerRadius: 16))
                    }
                    .buttonStyle(.plain)
                }

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        contextChip(momentTitle ?? "Wedding", solid: true, tint: WeddingActiveTheme.peachChip)
                        contextChip("Shared Experience", solid: false, tint: WeddingActiveTheme.tealChip)
                        contextChip("Planning Stage", solid: false, tint: WeddingActiveTheme.purpleChip)
                    }
                }
            }
            .listRowInsets(EdgeInsets(top: 12, leading: 16, bottom: 4, trailing: 16))
            .listRowBackground(Color.clear)
            .listRowSeparator(.hidden)

            Section {
                VStack(alignment: .leading, spacing: 12) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Bring your experience to life")
                            .font(.plusJakarta(size: 22, weight: .heavy))
                            .foregroundStyle(Color(hex: "#14121B"))
                        Text(
                            hasActiveMoment
                                ? "Add people, plans, money, memories and decisions."
                                : "Select or create a moment first."
                        )
                        .font(.plusJakarta(size: 11))
                        .foregroundStyle(Color(hex: "#14121B"))
                    }
                    HStack {
                        Spacer(minLength: 0)
                        Image("WeddingHubCake")
                            .resizable()
                            .scaledToFill()
                            .frame(width: 180, height: 120)
                            .clipShape(RoundedRectangle(cornerRadius: 16))
                    }
                }
                .padding(20)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(
                    LinearGradient(
                        colors: [Color(hex: "#FBCFE8"), Color(hex: "#F472B6")],
                        startPoint: .leading,
                        endPoint: .trailing
                    )
                )
                .clipShape(RoundedRectangle(cornerRadius: 22))
                .overlay(RoundedRectangle(cornerRadius: 22).stroke(Color(hex: "#FFB598").opacity(0.3)))
            }
            .listRowInsets(EdgeInsets(top: 4, leading: 16, bottom: 4, trailing: 16))
            .listRowBackground(Color.clear)
            .listRowSeparator(.hidden)

            Section {
                HStack(spacing: 10) {
                    HStack {
                        TextField("Search actions...", text: $search)
                            .font(.plusJakarta(size: 13))
                            .foregroundStyle(WeddingActiveTheme.text)
                    }
                    .padding(.horizontal, 14)
                    .padding(.vertical, 11)
                    .background(Color(hex: "#171618"))
                    .clipShape(RoundedRectangle(cornerRadius: 14))
                    .overlay(RoundedRectangle(cornerRadius: 14).stroke(WeddingActiveTheme.accent.opacity(0.3)))

                    Button {
                        showScanner = true
                    } label: {
                        Image(systemName: "qrcode.viewfinder")
                            .font(.system(size: 18, weight: .semibold))
                            .foregroundStyle(WeddingActiveTheme.accent)
                            .frame(width: 40, height: 40)
                            .background(Color(hex: "#171618"))
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                            .overlay(RoundedRectangle(cornerRadius: 12).stroke(WeddingActiveTheme.accent.opacity(0.3)))
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel("Scan QR code")
                }

                LazyVGrid(columns: columns, spacing: 12) {
                    ForEach(tiles) { tile in
                        tileCard(tile)
                    }
                }
            }
            .listRowInsets(EdgeInsets(top: 4, leading: 16, bottom: 4, trailing: 16))
            .listRowBackground(Color.clear)
            .listRowSeparator(.hidden)
        }
        .listStyle(.plain)
        .scrollContentBackground(.hidden)
        .safeAreaInset(edge: .bottom) {
            Button(action: onNewMoment) {
                Text("Create another Group Moment")
                    .font(.plusJakarta(size: 13, weight: .semibold))
                    .foregroundStyle(WeddingActiveTheme.accentLight)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)
            }
            .buttonStyle(.plain)
            .padding(.horizontal, 16)
            .padding(.bottom, 8)
            .background(Color(hex: "#09090A"))
        }
        .background(Color(hex: "#09090A"))
        .fullScreenCover(isPresented: $showScanner) {
            GroupJoinQrScanner(
                onCode: { code in
                    showScanner = false
                    onJoinCode(code)
                },
                onDismiss: { showScanner = false }
            )
        }
    }

    private func contextChip(_ label: String, solid: Bool, tint: Color) -> some View {
        Text(label)
            .font(.plusJakarta(size: 10, weight: .bold))
            .foregroundStyle(solid ? .white : tint)
            .lineLimit(1)
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(solid ? tint : tint.opacity(0.12))
            .overlay(
                Capsule().stroke(solid ? tint.opacity(0.3) : tint.opacity(0.3))
            )
            .clipShape(Capsule())
    }

    private func tileCard(_ tile: WeddingQuickAddKind) -> some View {
        let enabled = hasActiveMoment && capabilityAllows(tile)
        return Button {
            onTile(tile)
        } label: {
            VStack(spacing: 10) {
                if let sfSymbol = sfSymbolForKind(tile) {
                    Image(systemName: sfSymbol)
                        .font(.system(size: 22, weight: .semibold))
                        .foregroundStyle(.white)
                } else {
                    Text(tile.emoji).font(.system(size: 22))
                }
                Text(tile.label)
                    .font(.plusJakarta(size: 13, weight: .bold))
                    .foregroundStyle(.white)
                    .multilineTextAlignment(.center)
            }
            .frame(maxWidth: .infinity)
            .padding(16)
            .background(
                LinearGradient(colors: tile.gradient, startPoint: .top, endPoint: .bottom)
            )
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(tile.gradient[0].opacity(0.3)))
            .opacity(enabled ? 1 : 0.45)
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
    }
    
    private func sfSymbolForKind(_ kind: WeddingQuickAddKind) -> String? {
        switch kind {
        case .participant: return "person.fill"
        case .planning: return "list.bullet"
        case .expense: return "creditcard.fill"
        case .budget: return "dollarsign.circle.fill"
        case .contribution: return "gift.fill"
        case .settle: return "scale.3d"
        case .vendor: return "bag.fill"
        case .attendance: return "checkmark.square.fill"
        case .update: return "megaphone.fill"
        case .poll: return "chart.bar.fill"
        case .memory: return "camera.fill"
        }
    }

    private func capabilityAllows(_ tile: WeddingQuickAddKind) -> Bool {
        guard let codes = capabilityCodes, !codes.isEmpty else { return true }
        let upper = Set(codes.map { $0.uppercased() })
        switch tile {
        case .expense:
            return upper.contains(GroupActionCode.expenseCreate.rawValue)
        case .contribution:
            return upper.contains(GroupActionCode.contributionRecord.rawValue)
        case .participant:
            return upper.contains(GroupActionCode.participantManage.rawValue)
        case .budget:
            return hasActiveMoment
        default:
            return true
        }
    }
}
