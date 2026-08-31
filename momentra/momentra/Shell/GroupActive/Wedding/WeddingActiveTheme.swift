import SwiftUI

/// Figma Wedding populated tokens — 575:14939 / 575:14768 / 575:15203 / 584:16938.
enum WeddingActiveTheme {
    static let bg = Color(hex: "#131313")
    static let accent = Color(hex: "#ED4A99")
    static let accentSolid = Color(hex: "#ED4A99")
    static let accentLight = Color(hex: "#F472B6")
    static let accentSoft = Color(hex: "#EC4899").opacity(0.2)
    static let text = Color(hex: "#E5E2E1")
    static let secondary = Color(hex: "#DFC0B4")
    static let muted = Color(hex: "#A8A19E")
    static let card = Color(hex: "#201F1F")
    static let border = Color.white.opacity(0.1)
    static let darkText = Color(hex: "#14121B")
    
    // Chip colors
    static let peachChip = Color(hex: "#FBBF24")
    static let tealChip = Color(hex: "#14B8A6")
    static let purpleChip = Color(hex: "#A855F7")
    
    static let sectionRadius: CGFloat = 20

    static var heroGradient: LinearGradient {
        LinearGradient(colors: [Color(hex: "#FBCFE8"), Color(hex: "#F472B6")], startPoint: .leading, endPoint: .trailing)
    }
}

struct WeddingSectionCard<Content: View, Trailing: View>: View {
    let title: String
    @ViewBuilder var trailing: () -> Trailing
    @ViewBuilder var content: () -> Content

    init(
        title: String,
        @ViewBuilder trailing: @escaping () -> Trailing = { EmptyView() },
        @ViewBuilder content: @escaping () -> Content
    ) {
        self.title = title
        self.trailing = trailing
        self.content = content
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text(title)
                    .font(.plusJakarta(size: 18, weight: .heavy))
                    .foregroundStyle(WeddingActiveTheme.text)
                Spacer()
                trailing()
            }
            content()
        }
        .padding(20)
        .background(WeddingActiveTheme.card)
        .clipShape(RoundedRectangle(cornerRadius: WeddingActiveTheme.sectionRadius))
        .overlay(RoundedRectangle(cornerRadius: WeddingActiveTheme.sectionRadius).stroke(WeddingActiveTheme.border))
    }
}

struct WeddingPinkCta: View {
    let title: String
    let subtitle: String
    let buttonLabel: String
    let enabled: Bool
    var outlinedButton: Bool = false
    let action: () -> Void

    var body: some View {
        VStack(spacing: 16) {
            Text(title)
                .font(.plusJakarta(size: 18, weight: .bold))
                .foregroundStyle(WeddingActiveTheme.darkText)
            Text(subtitle)
                .font(.plusJakarta(size: 13))
                .foregroundStyle(WeddingActiveTheme.darkText.opacity(0.8))
            Button(action: action) {
                Text("+ \(buttonLabel)")
                    .font(.plusJakarta(size: 14, weight: .bold))
                    .foregroundStyle(outlinedButton ? WeddingActiveTheme.darkText : .white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(outlinedButton ? Color.clear : Color.white)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .overlay(outlinedButton ? RoundedRectangle(cornerRadius: 12).stroke(WeddingActiveTheme.darkText.opacity(0.3), lineWidth: 2) : nil)
            }
            .buttonStyle(.plain)
            .disabled(!enabled)
            .opacity(enabled ? 1 : 0.45)
        }
        .padding(20)
        .frame(maxWidth: .infinity)
        .background(WeddingActiveTheme.heroGradient)
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }
}

struct WeddingStatCard: View {
    let label: String
    let value: String
    let colors: [Color]

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(label)
                .font(.plusJakarta(size: 10, weight: .semibold))
                .foregroundStyle(Color.white.opacity(0.95))
            Text(value)
                .font(.plusJakarta(size: 22, weight: .bold))
                .foregroundStyle(Color.white.opacity(0.95))
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(LinearGradient(colors: colors, startPoint: .leading, endPoint: .trailing))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(WeddingActiveTheme.border))
    }
}

struct WeddingEmptyBlock: View {
    let message: String
    let detail: String
    var showApiGap: Bool = false

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            if showApiGap { GroupApiGapBadge() }
            Text(message)
                .font(.plusJakarta(size: 13, weight: .semibold))
                .foregroundStyle(WeddingActiveTheme.text)
            Text(detail)
                .font(.plusJakarta(size: 12))
                .foregroundStyle(WeddingActiveTheme.secondary)
        }
    }
}

struct WeddingViewAllLink: View {
    var enabled: Bool = false
    var action: () -> Void = {}

    var body: some View {
        Button(action: action) {
            Text("View all")
                .font(.plusJakarta(size: 12, weight: .semibold))
                .foregroundStyle(WeddingActiveTheme.accent)
                .opacity(enabled ? 1 : 0.45)
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
    }
}

struct WeddingDemoBadge: View {
    var body: some View {
        EmptyView()
    }
}

struct WeddingIconChip: View {
    let label: String
    let systemImage: String
    var enabled: Bool = false
    var action: () -> Void = {}

    var body: some View {
        Button(action: action) {
            VStack(spacing: 6) {
                Image(systemName: systemImage)
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(Color(hex: "#EAB308"))
                    .frame(width: 56, height: 56)
                    .background(WeddingActiveTheme.card)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                    .overlay(RoundedRectangle(cornerRadius: 16).stroke(WeddingActiveTheme.border))
                Text(label)
                    .font(.plusJakarta(size: 11, weight: .semibold))
                    .foregroundStyle(WeddingActiveTheme.text)
            }
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
        .opacity(enabled ? 1 : 0.85)
    }
}

struct WeddingAttentionRow: View {
    let emoji: String
    let title: String
    let detail: String
    let cta: String

    var body: some View {
        HStack(spacing: 10) {
            Rectangle()
                .fill(WeddingActiveTheme.accent)
                .frame(width: 3)
            VStack(alignment: .leading, spacing: 2) {
                HStack(spacing: 6) {
                    Text(emoji).font(.system(size: 14))
                    Text(title)
                        .font(.plusJakarta(size: 13, weight: .semibold))
                        .foregroundStyle(WeddingActiveTheme.text)
                }
                Text(detail)
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(WeddingActiveTheme.secondary)
            }
            Spacer()
            Text(cta)
                .font(.plusJakarta(size: 11, weight: .bold))
                .foregroundStyle(.white)
                .padding(.horizontal, 10)
                .padding(.vertical, 7)
                .background(WeddingActiveTheme.accent)
                .clipShape(Capsule())
        }
        .padding(12)
        .background(WeddingActiveTheme.bg)
        .clipShape(RoundedRectangle(cornerRadius: 14))
        .overlay(RoundedRectangle(cornerRadius: 14).stroke(WeddingActiveTheme.border))
    }
}

struct WeddingPartyProgressRow: View {
    let name: String
    let role: String
    let percent: Int
    var featured: Bool = false

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                HStack(spacing: 8) {
                    Text(String(name.prefix(1)))
                        .font(.plusJakarta(size: 14, weight: .bold))
                        .foregroundStyle(WeddingActiveTheme.accentLight)
                        .frame(width: 40, height: 40)
                        .background(WeddingActiveTheme.accentSoft)
                        .clipShape(Circle())
                    VStack(alignment: .leading, spacing: 2) {
                        HStack(spacing: 4) {
                            Text("\(name) (\(role))")
                                .font(.plusJakarta(size: 13, weight: .semibold))
                                .foregroundStyle(WeddingActiveTheme.text)
                            if featured {
                                Text("★").foregroundStyle(Color(hex: "#FBBF24")).font(.system(size: 12))
                            }
                        }
                        Text(featured ? "Most active" : "Active")
                            .font(.plusJakarta(size: 11))
                            .foregroundStyle(WeddingActiveTheme.muted)
                    }
                }
                Spacer()
                Text("\(percent)%")
                    .font(.plusJakarta(size: 12, weight: .bold))
                    .foregroundStyle(WeddingActiveTheme.accentLight)
            }
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule().fill(WeddingActiveTheme.accentSoft)
                    Capsule()
                        .fill(WeddingActiveTheme.accent)
                        .frame(width: geo.size.width * CGFloat(min(max(percent, 0), 100)) / 100)
                }
            }
            .frame(height: 6)
        }
    }
}

struct WeddingCategoryBar: View {
    let label: String
    let amountLabel: String
    let fraction: CGFloat

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text(label)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(WeddingActiveTheme.secondary)
                Spacer()
                Text(amountLabel)
                    .font(.plusJakarta(size: 12, weight: .semibold))
                    .foregroundStyle(WeddingActiveTheme.text)
            }
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule().fill(WeddingActiveTheme.accentSoft)
                    Capsule()
                        .fill(WeddingActiveTheme.accent)
                        .frame(width: geo.size.width * min(max(fraction, 0.05), 1))
                }
            }
            .frame(height: 8)
        }
    }
}

struct WeddingEmojiChip: View {
    let emoji: String
    let label: String
    var enabled: Bool = false
    var action: () -> Void = {}
    
    var body: some View {
        Button(action: action) {
            VStack(spacing: 6) {
                Text(emoji)
                    .font(.system(size: 22))
                    .frame(width: 56, height: 56)
                    .background(WeddingActiveTheme.card)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                    .overlay(RoundedRectangle(cornerRadius: 16).stroke(WeddingActiveTheme.border))
                Text(label)
                    .font(.plusJakarta(size: 11, weight: .semibold))
                    .foregroundStyle(WeddingActiveTheme.text)
            }
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
        .opacity(enabled ? 1 : 0.85)
    }
}

struct WeddingSegmentedProgress: View {
    let filled: Int
    let total: Int
    
    var body: some View {
        HStack(spacing: 4) {
            ForEach(0..<total, id: \.self) { index in
                Capsule()
                    .fill(index < filled ? WeddingActiveTheme.accent : WeddingActiveTheme.accentSoft)
                    .frame(height: 6)
            }
        }
    }
}

struct WeddingActivityRow: View {
    let emoji: String
    let title: String
    let whenLabel: String
    
    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            HStack(spacing: 6) {
                Text(emoji).font(.system(size: 14))
                Text(title)
                    .font(.plusJakarta(size: 13, weight: .medium))
                    .foregroundStyle(WeddingActiveTheme.text)
            }
            Text(whenLabel)
                .font(.plusJakarta(size: 11))
                .foregroundStyle(WeddingActiveTheme.secondary)
        }
        .padding(.vertical, 6)
    }
}

struct WeddingFadeIn: ViewModifier {
    @State private var opacity: Double = 0
    
    func body(content: Content) -> some View {
        content
            .opacity(opacity)
            .onAppear {
                withAnimation(.easeIn(duration: 0.3)) {
                    opacity = 1
                }
            }
    }
}
