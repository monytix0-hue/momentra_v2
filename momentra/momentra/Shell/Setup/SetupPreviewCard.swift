import SwiftUI

struct SetupPreviewBar: Identifiable {
    let id = UUID()
    let label: String
    let progress: Double
    let color: Color
}

struct SetupPreviewCard: View {
    let title: String
    var subtitle: String? = nil
    var bullets: [String] = []
    var bars: [SetupPreviewBar] = []
    var badge: String? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text(title)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(SetupTokens.textPrimary)
                Spacer()
                if let badge {
                    Text(badge)
                        .font(.system(size: 10, weight: .bold))
                        .foregroundStyle(SetupTokens.brandPrimary)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(SetupTokens.accentPurple.opacity(0.2), in: RoundedRectangle(cornerRadius: 8))
                }
            }

            if let subtitle {
                Text(subtitle)
                    .font(.system(size: 12))
                    .foregroundStyle(SetupTokens.textSecondary)
            }

            ForEach(bars) { bar in
                VStack(alignment: .leading, spacing: 4) {
                    Text(bar.label.uppercased())
                        .font(.system(size: 10, weight: .bold))
                        .foregroundStyle(SetupTokens.textSecondary)
                    GeometryReader { geo in
                        ZStack(alignment: .leading) {
                            RoundedRectangle(cornerRadius: 3)
                                .fill(Color.white.opacity(0.08))
                            RoundedRectangle(cornerRadius: 3)
                                .fill(bar.color)
                                .frame(width: geo.size.width * min(max(bar.progress, 0), 1))
                        }
                    }
                    .frame(height: 6)
                }
            }

            ForEach(bullets, id: \.self) { bullet in
                Text("• \(bullet)")
                    .font(.system(size: 12))
                    .foregroundStyle(SetupTokens.textSecondary)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(SetupTokens.surfaceCard, in: RoundedRectangle(cornerRadius: 16))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(SetupTokens.borderSubtle, lineWidth: 1))
    }
}

struct SetupMissionCard: View {
    let title: String
    let bodyText: String

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title.uppercased())
                .font(.system(size: 11, weight: .bold))
                .kerning(1)
                .foregroundStyle(SetupTokens.brandPrimary)
            Text(bodyText)
                .font(.system(size: 14))
                .foregroundStyle(SetupTokens.textPrimary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(SetupTokens.surfaceCard, in: RoundedRectangle(cornerRadius: 16))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(SetupTokens.borderSubtle, lineWidth: 1))
    }
}
