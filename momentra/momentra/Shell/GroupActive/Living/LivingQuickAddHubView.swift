import SwiftUI

/// Shared Living Quick Add hub (G09–G12).
struct LivingQuickAddHubView: View {
    let theme: LivingActiveTheme
    let momentTitle: String?
    let hasActiveMoment: Bool
    var capabilityCodes: [String]? = nil
    var onClose: () -> Void
    var onTile: (LivingQuickAddKind) -> Void
    var onNewMoment: () -> Void = {}
    var onJoinCode: (String) -> Void = { _ in }

    @State private var search = ""

    private let columns = [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())]

    private var tiles: [LivingQuickAddKind] {
        let q = search.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        let all = LivingQuickAddKind.hubTiles(theme: theme)
        guard !q.isEmpty else { return all }
        return all.filter { $0.label.lowercased().contains(q) }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Quick Add")
                            .font(.plusJakarta(size: 18, weight: .bold))
                            .foregroundStyle(Color(hex: "#F7F5F2"))
                        Text("Bring your \(theme.typeLabel.lowercased()) to life")
                            .font(.plusJakarta(size: 10))
                            .foregroundStyle(theme.muted)
                    }
                    Spacer()
                    Button(action: onClose) {
                        Text("✕")
                            .font(.plusJakarta(size: 12, weight: .bold))
                            .foregroundStyle(theme.text)
                            .frame(width: 32, height: 32)
                            .background(Color(hex: "#171618"))
                            .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color(hex: "#403C40")))
                            .clipShape(RoundedRectangle(cornerRadius: 16))
                    }
                    .buttonStyle(.plain)
                }

                HStack(spacing: 8) {
                    contextChip(momentTitle ?? theme.typeLabel, solid: true, tint: theme.accent)
                    contextChip(theme.typeLabel, solid: false, tint: theme.tealChip)
                    contextChip("Living Stage", solid: false, tint: theme.purpleChip)
                }

                VStack(alignment: .leading, spacing: 12) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Make every day shared")
                            .font(.plusJakarta(size: 22, weight: .heavy))
                            .foregroundStyle(Color(hex: "#14121B"))
                        Text(
                            hasActiveMoment
                                ? "Add residents, track expenses, plan tasks and keep the household running."
                                : "Select or create a moment first."
                        )
                        .font(.plusJakarta(size: 11))
                        .foregroundStyle(Color(hex: "#14121B"))
                    }
                    HStack {
                        Spacer(minLength: 0)
                        Image(theme.hubHeroAssetName)
                            .resizable()
                            .scaledToFill()
                            .frame(width: 180, height: 120)
                            .clipShape(RoundedRectangle(cornerRadius: 16))
                    }
                }
                .padding(20)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(theme.heroGradient)
                .clipShape(RoundedRectangle(cornerRadius: 20))

                HStack(spacing: 10) {
                    HStack(spacing: 8) {
                        Image(systemName: "magnifyingglass")
                            .foregroundStyle(theme.muted)
                        TextField("Search actions...", text: $search)
                            .font(.plusJakarta(size: 14))
                            .foregroundStyle(theme.text)
                    }
                    .padding(.horizontal, 14)
                    .frame(height: 40)
                    .background(Color(hex: "#171618"))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color(hex: "#403C40")))
                }

                LazyVGrid(columns: columns, spacing: 12) {
                    ForEach(tiles) { kind in
                        Button {
                            onTile(kind)
                        } label: {
                            VStack(spacing: 10) {
                                Text(kind.emoji)
                                    .font(.system(size: 22))
                                    .frame(width: 28, height: 28)
                                Text(kind.label)
                                    .font(.plusJakarta(size: 12, weight: .semibold))
                                    .foregroundStyle(theme.text)
                                    .multilineTextAlignment(.center)
                                    .lineLimit(2)
                            }
                            .frame(maxWidth: .infinity)
                            .frame(height: 104)
                            .background(
                                LinearGradient(
                                    colors: kind.gradient(theme: theme).map { $0.opacity(0.25) },
                                    startPoint: .top,
                                    endPoint: .bottom
                                )
                            )
                            .clipShape(RoundedRectangle(cornerRadius: 16))
                            .overlay(RoundedRectangle(cornerRadius: 16).stroke(theme.border))
                        }
                        .buttonStyle(.plain)
                        .disabled(!hasActiveMoment)
                        .opacity(hasActiveMoment ? 1 : 0.45)
                    }
                }

                if !hasActiveMoment {
                    Button("Create a new moment", action: onNewMoment)
                        .font(.plusJakarta(size: 14, weight: .semibold))
                        .foregroundStyle(theme.accentLight)
                        .padding(.top, 8)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .padding(.bottom, 56)
        }
        .background(theme.bg)
    }

    private func contextChip(_ label: String, solid: Bool, tint: Color) -> some View {
        Text(label)
            .font(.plusJakarta(size: 10, weight: .semibold))
            .foregroundStyle(solid ? theme.darkText : tint)
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(solid ? tint : tint.opacity(0.15))
            .clipShape(Capsule())
            .overlay(Capsule().stroke(tint.opacity(solid ? 0 : 0.5)))
    }
}
