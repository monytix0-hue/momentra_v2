import SwiftUI

/// Figma 575:14655 — Experience Quick Add hub (Trip Action Center chrome).
struct ExperienceQuickAddHubView: View {
    let theme: ExperienceActiveTheme
    let momentTitle: String?
    let hasActiveMoment: Bool
    var capabilityCodes: [String]? = nil
    var onClose: () -> Void
    var onTile: (ExperienceQuickAddKind) -> Void
    var onNewMoment: () -> Void = {}
    var onJoinCode: (String) -> Void = { _ in }

    @State private var search = ""

    private var tiles: [ExperienceQuickAddKind] {
        let q = search.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        let all = ExperienceQuickAddKind.hubTiles(includesVendor: theme.includesVendor)
        guard !q.isEmpty else { return all }
        return all.filter { $0.label.lowercased().contains(q) }
    }

    private var actionRows: [[ExperienceQuickAddKind]] {
        stride(from: 0, to: tiles.count, by: 3).map {
            Array(tiles[$0..<min($0 + 3, tiles.count)])
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
                    contextChip(momentTitle ?? theme.typeLabel, solid: true, tint: Color(hex: "#FFB598"), onSolid: Color(hex: "#591D00"))
                    contextChip("Shared Experience", solid: false, tint: Color(hex: "#14B8A6"))
                    contextChip("Planning Stage", solid: false, tint: Color(hex: "#A855F7"))
                }

                VStack(alignment: .leading, spacing: 12) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Bring your experience to life")
                            .font(.plusJakarta(size: 22, weight: .heavy))
                            .foregroundStyle(.white)
                        Text(
                            hasActiveMoment
                                ? "Add people, plans, money, memories and decisions."
                                : "Select or create a moment first."
                        )
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
                }

                ForEach(Array(actionRows.enumerated()), id: \.offset) { _, row in
                    HStack(spacing: 12) {
                        ForEach(row) { kind in
                            actionCard(kind)
                        }
                        ForEach(0..<(3 - row.count), id: \.self) { _ in
                            Color.clear.frame(maxWidth: .infinity)
                        }
                    }
                }

                if !hasActiveMoment {
                    Button(action: onNewMoment) {
                        Text("Create a new moment")
                            .font(.plusJakarta(size: 13, weight: .semibold))
                            .foregroundStyle(Color(hex: "#E8621A"))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 8)
                    }
                    .buttonStyle(.plain)
                }

                Spacer(minLength: 12)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
        }
        .background(Color(hex: "#09090A"))
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
            .shadow(color: tint.opacity(0.25), radius: 10, y: 4)
    }

    @ViewBuilder
    private func actionCard(_ kind: ExperienceQuickAddKind) -> some View {
        Button {
            onTile(kind)
        } label: {
            VStack(spacing: 10) {
                GroupQaIcons.tileIcon(asset: kind.hubIconAsset, size: 28)
                Text(kind.hubLabel)
                    .font(.plusJakarta(size: 14, weight: .bold))
                    .foregroundStyle(.white)
                    .multilineTextAlignment(.center)
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity)
            .frame(height: 104)
            .background(
                LinearGradient(
                    colors: kind.hubGradient,
                    startPoint: .leading,
                    endPoint: .trailing
                )
            )
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(kind.hubGradient.first?.opacity(0.3) ?? .clear))
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .shadow(color: (kind.hubGradient.first ?? .clear).opacity(0.2), radius: 10, y: 4)
        }
        .buttonStyle(.plain)
        .disabled(!hasActiveMoment)
        .opacity(hasActiveMoment ? 1 : 0.45)
    }
}
