import SwiftUI

/// Figma 575:14655 — Group Shared Experience / Trip Quick Add hub.
struct GroupQuickAddHubView: View {
    let hasActiveMoment: Bool
    var capabilityCodes: [String]? = nil
    var momentTypeCode: String? = nil
    var momentTitle: String? = nil
    var onClose: () -> Void
    var onExpense: () -> Void
    var onContribution: () -> Void
    var onSettle: () -> Void = {}
    var onParticipants: () -> Void = {}
    var onInvite: () -> Void = {}
    var onBudget: () -> Void = {}
    var onPlanning: () -> Void = {}
    var onBooking: () -> Void = {}
    var onPoll: () -> Void = {}
    var onUpdate: () -> Void = {}
    var onMemory: () -> Void = {}
    var onPurchaseItem: () -> Void = {}
    var onResident: () -> Void = {}
    var onNewMoment: () -> Void = {}
    var onJoinCode: (String) -> Void = { _ in }

    @State private var search = ""
    @State private var showScanner = false

    private var actions: [GroupActionTile] {
        GroupActionRegistry.figmaHubTiles(hasActiveMoment: hasActiveMoment, capabilityCodes: capabilityCodes)
    }

    private var filteredActions: [GroupActionTile] {
        let q = search.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard !q.isEmpty else { return actions }
        return actions.filter { $0.label.lowercased().contains(q) }
    }

    private let columns = [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Quick Add")
                            .font(.plusJakarta(size: 18, weight: .bold))
                            .foregroundStyle(Color(hex: "#F7F5F2"))
                        Text("Bring your experience to life")
                            .font(.plusJakarta(size: 10))
                            .foregroundStyle(Color(hex: "#A8A19E"))
                    }
                    Spacer()
                    Button(action: onClose) {
                        Image(systemName: "xmark")
                            .font(.system(size: 12, weight: .bold))
                            .foregroundStyle(Color(hex: "#F7F5F2"))
                            .frame(width: 32, height: 32)
                            .background(Color(hex: "#171618"))
                            .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color(hex: "#403C40")))
                            .clipShape(RoundedRectangle(cornerRadius: 16))
                    }
                    .buttonStyle(.plain)
                }

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        chip(momentTitle?.isEmpty == false ? momentTitle! : "Trip", solid: true, tint: Color(hex: "#FFB598"), onSolid: Color(hex: "#591D00"))
                        chip("Shared Experience", solid: false, tint: Color(hex: "#14B8A6"))
                        if let code = momentTypeCode, !code.isEmpty {
                            chip(code, solid: false, tint: Color(hex: "#A855F7"))
                        }
                    }
                }

                HStack(alignment: .center, spacing: 12) {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Bring your experience to life")
                            .font(.plusJakarta(size: 22, weight: .heavy))
                            .foregroundStyle(.white)
                        Text(hasActiveMoment
                             ? "Add people, plans, money, memories and decisions."
                             : "Select or create a moment first.")
                            .font(.plusJakarta(size: 11))
                            .foregroundStyle(.white.opacity(0.8))
                    }
                    Spacer(minLength: 0)
                    Image("TripHubHero")
                        .resizable()
                        .scaledToFill()
                        .frame(width: 120, height: 88)
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                }
                .padding(20)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(
                    LinearGradient(colors: [Color(hex: "#FFB598"), Color(hex: "#E8621A")], startPoint: .leading, endPoint: .trailing)
                )
                .overlay(RoundedRectangle(cornerRadius: 22).stroke(Color(hex: "#FFB598").opacity(0.3)))
                .clipShape(RoundedRectangle(cornerRadius: 22))

                HStack(spacing: 10) {
                    HStack(spacing: 8) {
                        Image(systemName: "magnifyingglass")
                            .foregroundStyle(Color(hex: "#A8A19E"))
                        TextField("Search actions...", text: $search)
                            .font(.plusJakarta(size: 13))
                            .foregroundStyle(Color(hex: "#F7F5F2"))
                    }
                    .padding(.horizontal, 14)
                    .padding(.vertical, 11)
                    .background(Color(hex: "#171618"))
                    .overlay(RoundedRectangle(cornerRadius: 14).stroke(Color(hex: "#FFB598").opacity(0.3)))
                    .clipShape(RoundedRectangle(cornerRadius: 14))

                    Button { showScanner = true } label: {
                        Image(systemName: "qrcode.viewfinder")
                            .font(.system(size: 18, weight: .semibold))
                            .foregroundStyle(Color(hex: "#FFB598"))
                            .frame(width: 40, height: 40)
                            .background(Color(hex: "#171618"))
                            .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color(hex: "#FFB598").opacity(0.3)))
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                    .buttonStyle(.plain)
                    .accessibilityIdentifier("qa.tile.qr")
                }

                LazyVGrid(columns: columns, spacing: 12) {
                    ForEach(filteredActions) { action in
                        actionCard(action)
                    }
                }

                Button(action: onNewMoment) {
                    Text("Create another Group Moment")
                        .font(.plusJakarta(size: 13, weight: .semibold))
                        .foregroundStyle(Color(hex: "#E8621A"))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 8)
                }
                .buttonStyle(.plain)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
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

    private func chip(_ label: String, solid: Bool, tint: Color, onSolid: Color = .white) -> some View {
        Text(label)
            .font(.plusJakarta(size: 10, weight: .bold))
            .foregroundStyle(solid ? onSolid : tint)
            .lineLimit(1)
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(solid ? tint : tint.opacity(0.12))
            .overlay(Capsule().stroke(tint.opacity(0.3)))
            .clipShape(Capsule())
    }

    private func actionCard(_ action: GroupActionTile) -> some View {
        Button {
            switch action.destination {
            case .expense: onExpense()
            case .contribution: onContribution()
            case .settlement: onSettle()
            case .invite: onInvite()
            case .participants: onParticipants()
            case .budget: onBudget()
            case .planning: onPlanning()
            case .booking: onBooking()
            case .poll: onPoll()
            case .update: onUpdate()
            case .memory: onMemory()
            case .purchaseItem: onPurchaseItem()
            case .resident: onResident()
            }
        } label: {
            VStack(spacing: 10) {
                Image(systemName: action.icon)
                    .font(.system(size: 22, weight: .semibold))
                    .foregroundStyle(.white)
                Text(action.label)
                    .font(.plusJakarta(size: 14, weight: .bold))
                    .foregroundStyle(.white)
            }
            .frame(maxWidth: .infinity)
            .frame(height: 104)
            .background(LinearGradient(colors: action.colors, startPoint: .leading, endPoint: .trailing))
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(action.colors[0].opacity(0.3)))
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .opacity(action.enabledWhenMomentActive ? 1 : 0.45)
        }
        .buttonStyle(.plain)
        .disabled(!action.enabledWhenMomentActive)
        .accessibilityIdentifier("qa.tile.group.\(action.tileId)")
    }
}
