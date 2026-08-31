import SwiftUI

/// Figma Action Center hubs — Team Ops / Runway / Ops (B01–B03).
struct BusinessQuickAddHub: View {
    let hasActiveMoment: Bool
    let hasCompany: Bool
    var capabilityCodes: [String]? = nil
    var momentTypeCode: String? = nil
    var onClose: () -> Void
    var onTile: (BusinessQuickAddKind) -> Void
    var onNewMoment: () -> Void = {}
    /// Legacy callbacks kept for AppShell wiring of live finance sheets.
    var onExpense: () -> Void = {}
    var onRevenue: () -> Void = {}
    var onInvoice: () -> Void = {}
    var onMembers: () -> Void = {}

    @State private var search = ""

    private var theme: BusinessActiveTheme { .forTypeCode(momentTypeCode) }
    private let columns = [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())]

    private var tiles: [BusinessQuickAddKind] {
        let q = search.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        let all = BusinessQuickAddKind.hubTiles(theme: theme)
        guard !q.isEmpty else { return all }
        return all.filter {
            $0.label.lowercased().contains(q) || $0.subtitle.lowercased().contains(q)
        }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Quick Add")
                            .font(.plusJakarta(size: 24, weight: .heavy))
                            .foregroundStyle(.white)
                        Text(theme.hubSubtitle)
                            .font(.plusJakarta(size: 13, weight: .medium))
                            .foregroundStyle(theme.secondary)
                    }
                    Spacer()
                    Button(action: onClose) {
                        Image("TeamOpsQaClose")
                            .resizable()
                            .scaledToFit()
                            .frame(width: 10, height: 10)
                            .frame(width: 24, height: 24)
                            .background(Color(hex: "#818CF8").opacity(0.9))
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                    .buttonStyle(.plain)
                }

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        chip(theme.typeLabel, selected: true)
                        ForEach(theme.filterChips, id: \.self) { chip($0, selected: false) }
                    }
                }

                HStack(spacing: 16) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(theme.hubHeroTitle)
                            .font(.plusJakarta(size: 18, weight: .semibold))
                            .foregroundStyle(.white)
                        Text(theme.hubHeroDetail)
                            .font(.plusJakarta(size: 12))
                            .foregroundStyle(Color.white.opacity(0.7))
                    }
                    Spacer(minLength: 0)
                    Image(theme.hubHeroAssetName)
                        .resizable()
                        .scaledToFill()
                        .frame(width: 100, height: 100)
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                }
                .padding(20)
                .frame(maxWidth: .infinity, minHeight: 140)
                .background(theme.heroGradient)
                .overlay(RoundedRectangle(cornerRadius: 20).stroke(theme.border))
                .clipShape(RoundedRectangle(cornerRadius: 20))

                HStack(spacing: 10) {
                    Image("TeamOpsQaSearch")
                        .resizable()
                        .scaledToFit()
                        .frame(width: 16, height: 16)
                    TextField("Search actions...", text: $search)
                        .font(.plusJakarta(size: 13, weight: .medium))
                        .foregroundStyle(theme.text)
                }
                .padding(12)
                .background(theme.card)
                .overlay(RoundedRectangle(cornerRadius: 24).stroke(theme.border))
                .clipShape(RoundedRectangle(cornerRadius: 24))

                LazyVGrid(columns: columns, spacing: 12) {
                    ForEach(tiles) { kind in
                        let capOk = BusinessActionRegistry.isKindEnabled(kind, capabilities: capabilityCodes)
                        let momentOk = hasActiveMoment || kind == .memory
                        Button { handle(kind) } label: {
                            let tall = kind == .activityLog || kind == .poll || kind == .memory
                            VStack(spacing: 8) {
                                if let icon = kind.teamOpsHubIconName {
                                    Image(icon)
                                        .resizable()
                                        .scaledToFit()
                                        .frame(width: 40, height: 40)
                                } else {
                                    Text(kind.emoji)
                                        .font(.system(size: 22))
                                        .frame(width: 40, height: 40)
                                }
                                Text(kind.label)
                                    .font(.plusJakarta(size: 11, weight: .semibold))
                                    .foregroundStyle(kind.stripeColor)
                                    .multilineTextAlignment(.center)
                                    .lineLimit(2)
                                    .minimumScaleFactor(0.8)
                                Text(kind.subtitle)
                                    .font(.plusJakarta(size: 9))
                                    .foregroundStyle(theme.muted)
                                    .lineLimit(1)
                            }
                            .frame(maxWidth: .infinity)
                            .frame(height: tall ? 120 : 100)
                            .padding(8)
                            .background(
                                LinearGradient(
                                    colors: [kind.stripeColor.opacity(0.12), .clear],
                                    startPoint: .leading,
                                    endPoint: .trailing
                                )
                                .background(theme.card)
                            )
                            .overlay(alignment: .leading) {
                                Rectangle()
                                    .fill(kind.stripeColor)
                                    .frame(width: 3)
                            }
                            .overlay(RoundedRectangle(cornerRadius: tall ? 20 : 16).stroke(theme.border))
                            .clipShape(RoundedRectangle(cornerRadius: tall ? 20 : 16))
                            .opacity(momentOk && capOk ? 1 : 0.45)
                        }
                        .buttonStyle(.plain)
                        .disabled(!(momentOk && capOk))
                    }
                }
                if tiles.isEmpty {
                    Text("No actions match this search.")
                        .font(.plusJakarta(size: 13))
                        .foregroundStyle(theme.secondary)
                        .padding(14)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(theme.card)
                        .overlay(RoundedRectangle(cornerRadius: 16).stroke(theme.border))
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                }

                Button(action: onNewMoment) {
                    Text("Create another Business Moment")
                        .font(.plusJakarta(size: 13, weight: .semibold))
                        .foregroundStyle(theme.accent)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                }
                .buttonStyle(.plain)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 16)
            .padding(.bottom, 28)
        }
        .background(theme.bg.ignoresSafeArea())
    }

    private func handle(_ kind: BusinessQuickAddKind) {
        onTile(kind)
    }

    private func chip(_ label: String, selected: Bool) -> some View {
        Text(label)
            .font(.plusJakarta(size: 10, weight: .bold))
            .foregroundStyle(selected ? .white : Color(hex: "#818CF8"))
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(selected ? Color(hex: "#6366F1") : theme.card)
            .overlay(Capsule().stroke(Color(hex: "#6366F1").opacity(0.3)))
            .clipShape(Capsule())
    }
}
