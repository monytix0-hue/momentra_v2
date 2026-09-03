import SwiftUI

/// Figma 575:14655 — Group Trip Action Center Quick Add hub.
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
        GroupActionRegistry.figmaTripHubTiles(
            hasActiveMoment: hasActiveMoment,
            capabilityCodes: capabilityCodes
        )
    }

    private var filteredActions: [GroupActionTile] {
        let q = search.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard !q.isEmpty else { return actions }
        return actions.filter { $0.label.lowercased().contains(q) }
    }

    private var actionRows: [[GroupActionTile]] {
        stride(from: 0, to: filteredActions.count, by: 3).map {
            Array(filteredActions[$0..<min($0 + 3, filteredActions.count)])
        }
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 14) {
                HStack(alignment: .top) {
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
                        GroupQaIcons.close(size: 14)
                            .frame(width: 32, height: 32)
                            .background(Color(hex: "#171618"))
                            .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color(hex: "#403C40")))
                            .clipShape(RoundedRectangle(cornerRadius: 16))
                    }
                    .buttonStyle(.plain)
                }

                HStack(spacing: 7) {
                    contextChip(momentTitle?.isEmpty == false ? momentTitle! : "Trip", solid: true, tint: Color(hex: "#FFB598"), onSolid: Color(hex: "#591D00"))
                    contextChip("Shared Experience", solid: false, tint: Color(hex: "#14B8A6"))
                    contextChip("Planning Stage", solid: false, tint: Color(hex: "#A855F7"))
                }

                VStack(alignment: .leading, spacing: 12) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Bring your experience to life")
                            .font(.plusJakarta(size: 22, weight: .heavy))
                            .foregroundStyle(.white)
                        Text(hasActiveMoment
                             ? "Add people, plans, money, memories and decisions."
                             : "Select or create a moment first.")
                            .font(.plusJakarta(size: 11))
                            .foregroundStyle(.white.opacity(0.8))
                    }
                    HStack {
                        Spacer(minLength: 0)
                        Image("TripHubHero")
                            .resizable()
                            .scaledToFill()
                            .frame(width: 180, height: 120)
                            .clipShape(RoundedRectangle(cornerRadius: 16))
                    }
                }
                .padding(20)
                .background(
                    LinearGradient(
                        colors: [Color(hex: "#FFB598"), Color(hex: "#E8621A")],
                        startPoint: .leading,
                        endPoint: .trailing
                    )
                )
                .overlay(RoundedRectangle(cornerRadius: 22).stroke(Color(hex: "#FFB598").opacity(0.3)))
                .clipShape(RoundedRectangle(cornerRadius: 22))
                .shadow(color: Color(hex: "#FFB598").opacity(0.2), radius: 24, y: 8)

                HStack(spacing: 10) {
                    HStack(spacing: 10) {
                        GroupQaIcons.search(size: 18)
                            .foregroundStyle(Color(hex: "#A8A19E"))
                        TextField("Search actions...", text: $search)
                            .font(.plusJakarta(size: 13))
                            .foregroundStyle(Color(hex: "#F7F5F2"))
                    }
                    .padding(.horizontal, 14)
                    .padding(.vertical, 11)
                    .frame(maxWidth: .infinity)
                    .background(Color(hex: "#171618"))
                    .overlay(RoundedRectangle(cornerRadius: 14).stroke(Color(hex: "#FFB598").opacity(0.3)))
                    .clipShape(RoundedRectangle(cornerRadius: 14))
                    .shadow(color: Color(hex: "#FFB598").opacity(0.2), radius: 12, y: 4)

                    Button { showScanner = true } label: {
                        GroupQaIcons.qrCode(size: 18)
                            .foregroundStyle(Color(hex: "#FFB598"))
                            .frame(width: 40, height: 40)
                            .background(Color(hex: "#171618"))
                            .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color(hex: "#FFB598").opacity(0.3)))
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                    .buttonStyle(.plain)
                    .accessibilityIdentifier("qa.tile.qr")
                }

                ForEach(Array(actionRows.enumerated()), id: \.offset) { _, row in
                    HStack(spacing: 12) {
                        ForEach(row) { action in
                            actionCard(action)
                        }
                        ForEach(0..<(3 - row.count), id: \.self) { _ in
                            Color.clear.frame(maxWidth: .infinity)
                        }
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

                Spacer(minLength: 12)
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

    private func contextChip(_ label: String, solid: Bool, tint: Color, onSolid: Color = .white) -> some View {
        Text(label)
            .font(.plusJakarta(size: 10, weight: .bold))
            .foregroundStyle(solid ? onSolid : tint)
            .lineLimit(1)
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(solid ? tint : tint.opacity(0.12))
            .overlay(Capsule().stroke(tint.opacity(0.3)))
            .clipShape(Capsule())
            .shadow(color: solid ? tint.opacity(0.25) : tint.opacity(0.25), radius: 10, y: 4)
    }

    @ViewBuilder
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
                GroupQaIcons.tileIcon(asset: action.icon, size: 28)
                    .opacity(action.enabledWhenMomentActive ? 1 : 0.45)
                Text(action.label)
                    .font(.plusJakarta(size: 14, weight: .bold))
                    .foregroundStyle(.white.opacity(action.enabledWhenMomentActive ? 1 : 0.45))
            }
            .padding(16)
            .frame(maxWidth: .infinity)
            .frame(height: 104)
            .background(LinearGradient(colors: action.colors, startPoint: .leading, endPoint: .trailing))
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(action.colors[0].opacity(0.3)))
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .shadow(color: action.colors[0].opacity(0.2), radius: 10, y: 4)
        }
        .buttonStyle(.plain)
        .disabled(!action.enabledWhenMomentActive)
        .accessibilityIdentifier("qa.tile.group.\(action.tileId)")
    }
}
