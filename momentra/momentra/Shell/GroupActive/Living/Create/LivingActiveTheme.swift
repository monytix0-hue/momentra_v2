import SwiftUI

/// Shared Living active tokens — Flatmates / Family Household / Co-living / Custom Living (G09–G12).
struct LivingActiveTheme {
    let bg: Color
    let accent: Color
    let accentSolid: Color
    let accentLight: Color
    let accentSoft: Color
    let text: Color
    let secondary: Color
    let muted: Color
    let card: Color
    let border: Color
    let darkText: Color
    let peachChip: Color
    let tealChip: Color
    let purpleChip: Color
    let typeLabel: String
    let pulseTitle: String
    let contributionsTitle: String
    let budgetTitle: String
    let financeTitle: String
    let hubHeroAssetName: String
    let heroEmoji: String
    let participantRoles: [String]
    let participantSubtitle: String
    let heroGradientColors: [Color]
    let pulseHeroGradientColors: [Color]
    let statGradients: [[Color]]
    let includesContribution: Bool
    let quickChips: [(emoji: String, label: String, kind: LivingQuickAddKind)]

    var heroGradient: LinearGradient {
        LinearGradient(colors: heroGradientColors, startPoint: .leading, endPoint: .trailing)
    }

    var pulseHeroGradient: LinearGradient {
        LinearGradient(
            colors: pulseHeroGradientColors,
            startPoint: UnitPoint(x: 0.1, y: 0),
            endPoint: UnitPoint(x: 0.9, y: 1)
        )
    }

    var sectionRadius: CGFloat { 20 }

    static let flatmates = LivingActiveTheme(
        bg: Color(hex: "#14121B"),
        accent: Color(hex: "#E8744F"),
        accentSolid: Color(hex: "#FF7A3D"),
        accentLight: Color(hex: "#FF7A3D"),
        accentSoft: Color(hex: "#E8744F").opacity(0.2),
        text: Color(hex: "#E5E2E1"),
        secondary: Color(hex: "#9CA3AF"),
        muted: Color(hex: "#9CA3AF"),
        card: Color(hex: "#1C1926"),
        border: Color(hex: "#2A2538"),
        darkText: Color(hex: "#14121B"),
        peachChip: Color(hex: "#FBBF24"),
        tealChip: Color(hex: "#14B8A6"),
        purpleChip: Color(hex: "#A855F7"),
        typeLabel: "Flatmates",
        pulseTitle: "Flatmates Pulse",
        contributionsTitle: "Member Contributions",
        budgetTitle: "Household Budget",
        financeTitle: "Group Finance",
        hubHeroAssetName: "FlatmatesHubHero",
        heroEmoji: "🏠",
        participantRoles: ["Organizer", "Resident", "Guest"],
        participantSubtitle: "Invite people living in this household",
        heroGradientColors: [Color(hex: "#FF7A3D"), Color(hex: "#E8744F")],
        pulseHeroGradientColors: [Color(hex: "#FDBA74"), Color(hex: "#EA580C").opacity(0.9)],
        statGradients: [
            [Color(hex: "#C2410C"), Color(hex: "#9A3412")],
            [Color(hex: "#E8744F"), Color(hex: "#EA580C")],
            [Color(hex: "#7C3AED"), Color(hex: "#5B21B6")],
            [Color(hex: "#FF7A3D"), Color(hex: "#C2410C")],
        ],
        includesContribution: true,
        quickChips: [
            ("👤", "Resident", .resident),
            ("💳", "Expense", .expense),
            ("✅", "Task", .task),
            ("📷", "Memory", .memory),
        ]
    )

    static let coLiving = LivingActiveTheme(
        bg: Color(hex: "#14121B"),
        // Figma 629:10541 hub — cyan #06B6D4
        accent: Color(hex: "#06B6D4"),
        accentSolid: Color(hex: "#06B6D4"),
        accentLight: Color(hex: "#22D3EE"),
        accentSoft: Color(hex: "#06B6D4").opacity(0.2),
        text: Color(hex: "#E5E2E1"),
        secondary: Color(hex: "#9CA3AF"),
        muted: Color(hex: "#9CA3AF"),
        card: Color(hex: "#1C1926"),
        border: Color(hex: "#2A2538"),
        darkText: Color(hex: "#14121B"),
        peachChip: Color(hex: "#FBBF24"),
        tealChip: Color(hex: "#14B8A6"),
        purpleChip: Color(hex: "#A855F7"),
        typeLabel: "Co-living",
        pulseTitle: "Co-living Pulse",
        contributionsTitle: "Member Contributions",
        budgetTitle: "Community Budget",
        financeTitle: "Community Hub",
        hubHeroAssetName: "ColivingHubHero",
        heroEmoji: "🏘️",
        participantRoles: ["Organizer", "Resident", "Member"],
        participantSubtitle: "Invite people sharing this co-living space",
        heroGradientColors: [Color(hex: "#068CA6"), Color(hex: "#043744")],
        pulseHeroGradientColors: [Color(hex: "#22D3EE"), Color(hex: "#0E7490").opacity(0.9)],
        statGradients: [
            [Color(hex: "#0E7490"), Color(hex: "#155E75")],
            [Color(hex: "#06B6D4"), Color(hex: "#0891B2")],
            [Color(hex: "#7C3AED"), Color(hex: "#5B21B6")],
            [Color(hex: "#22D3EE"), Color(hex: "#0E7490")],
        ],
        includesContribution: true,
        quickChips: [
            ("👤", "Resident", .resident),
            ("💳", "Expense", .expense),
            ("✅", "Task", .task),
            ("📷", "Memory", .memory),
        ]
    )

    static let familyHousehold = LivingActiveTheme(
        bg: Color(hex: "#14121B"),
        // Figma 629:16126 hub — amber #F59E0B
        accent: Color(hex: "#F59E0B"),
        accentSolid: Color(hex: "#F59E0B"),
        accentLight: Color(hex: "#FBBF24"),
        accentSoft: Color(hex: "#F59E0B").opacity(0.2),
        text: Color(hex: "#E5E2E1"),
        secondary: Color(hex: "#9CA3AF"),
        muted: Color(hex: "#9CA3AF"),
        card: Color(hex: "#1C1926"),
        border: Color(hex: "#2A2538"),
        darkText: Color(hex: "#14121B"),
        peachChip: Color(hex: "#FBBF24"),
        tealChip: Color(hex: "#14B8A6"),
        purpleChip: Color(hex: "#A855F7"),
        typeLabel: "Family Household",
        pulseTitle: "Family Pulse",
        contributionsTitle: "Member Contributions",
        budgetTitle: "Household Budget",
        financeTitle: "Group Finance",
        hubHeroAssetName: "FamilyHouseholdHubHero",
        heroEmoji: "👨‍👩‍👧",
        participantRoles: ["Organizer", "Family", "Guest"],
        participantSubtitle: "Invite family members into this household",
        heroGradientColors: [Color(hex: "#F59E0B"), Color(hex: "#78350F")],
        pulseHeroGradientColors: [Color(hex: "#FBBF24"), Color(hex: "#B45309").opacity(0.9)],
        statGradients: [
            [Color(hex: "#B45309"), Color(hex: "#92400E")],
            [Color(hex: "#F59E0B"), Color(hex: "#D97706")],
            [Color(hex: "#7C3AED"), Color(hex: "#5B21B6")],
            [Color(hex: "#FBBF24"), Color(hex: "#B45309")],
        ],
        includesContribution: false,
        quickChips: [
            ("👤", "Resident", .resident),
            ("💳", "Expense", .expense),
            ("✅", "Task", .task),
            ("📷", "Memory", .memory),
        ]
    )

    static let customLiving = LivingActiveTheme(
        bg: Color(hex: "#14121B"),
        // Figma 629:15586 hub — emerald #10B981
        accent: Color(hex: "#10B981"),
        accentSolid: Color(hex: "#10B981"),
        accentLight: Color(hex: "#34D399"),
        accentSoft: Color(hex: "#10B981").opacity(0.2),
        text: Color(hex: "#E5E2E1"),
        secondary: Color(hex: "#9CA3AF"),
        muted: Color(hex: "#9CA3AF"),
        card: Color(hex: "#1C1926"),
        border: Color(hex: "#2A2538"),
        darkText: Color(hex: "#14121B"),
        peachChip: Color(hex: "#FBBF24"),
        tealChip: Color(hex: "#14B8A6"),
        purpleChip: Color(hex: "#A855F7"),
        typeLabel: "Custom Living",
        pulseTitle: "Living Pulse",
        contributionsTitle: "Member Contributions",
        budgetTitle: "Property Budget",
        financeTitle: "Property Hub",
        hubHeroAssetName: "CustomLivingHubHero",
        heroEmoji: "✨",
        participantRoles: ["Organizer", "Resident", "Member"],
        participantSubtitle: "Invite people into this custom living space",
        heroGradientColors: [Color(hex: "#10B981"), Color(hex: "#0F766E")],
        pulseHeroGradientColors: [Color(hex: "#34D399"), Color(hex: "#047857").opacity(0.9)],
        statGradients: [
            [Color(hex: "#047857"), Color(hex: "#065F46")],
            [Color(hex: "#10B981"), Color(hex: "#059669")],
            [Color(hex: "#7C3AED"), Color(hex: "#5B21B6")],
            [Color(hex: "#2DD4BF"), Color(hex: "#0F766E")],
        ],
        includesContribution: false,
        quickChips: [
            ("👤", "Resident", .resident),
            ("💳", "Expense", .expense),
            ("✅", "Task", .task),
            ("📷", "Memory", .memory),
        ]
    )

    static func forFamily(_ family: GroupExperienceFamily) -> LivingActiveTheme {
        switch family {
        case .coLiving: return .coLiving
        case .familyHousehold: return .familyHousehold
        case .customLiving: return .customLiving
        default: return .flatmates
        }
    }
}

struct LivingSectionCard<Content: View, Trailing: View>: View {
    let theme: LivingActiveTheme
    let title: String
    @ViewBuilder var trailing: () -> Trailing
    @ViewBuilder var content: () -> Content

    init(
        theme: LivingActiveTheme,
        title: String,
        @ViewBuilder trailing: @escaping () -> Trailing = { EmptyView() },
        @ViewBuilder content: @escaping () -> Content
    ) {
        self.theme = theme
        self.title = title
        self.trailing = trailing
        self.content = content
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text(title)
                    .font(.plusJakarta(size: 18, weight: .heavy))
                    .foregroundStyle(theme.text)
                Spacer()
                trailing()
            }
            content()
        }
        .padding(20)
        .background(theme.card)
        .clipShape(RoundedRectangle(cornerRadius: theme.sectionRadius))
        .overlay(RoundedRectangle(cornerRadius: theme.sectionRadius).stroke(theme.border))
    }
}

struct LivingEmptyBlock: View {
    let theme: LivingActiveTheme
    let message: String
    let detail: String

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(message)
                .font(.plusJakarta(size: 13, weight: .semibold))
                .foregroundStyle(theme.text)
            Text(detail)
                .font(.plusJakarta(size: 12))
                .foregroundStyle(theme.secondary)
        }
    }
}

struct LivingStatCard: View {
    let label: String
    let value: String
    let colors: [Color]
    var icon: String? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            if let icon {
                Image(systemName: icon)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(Color.white.opacity(0.9))
            }
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
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.1)))
    }
}

struct LivingEmojiChip: View {
    let theme: LivingActiveTheme
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
                    .background(theme.card)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                    .overlay(RoundedRectangle(cornerRadius: 16).stroke(theme.border))
                Text(label)
                    .font(.plusJakarta(size: 11, weight: .semibold))
                    .foregroundStyle(theme.text)
            }
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
        .opacity(enabled ? 1 : 0.85)
    }
}

struct LivingCrewRow: View {
    let theme: LivingActiveTheme
    let name: String
    let amountLabel: String
    let percent: Int
    var featured: Bool = false

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                HStack(spacing: 8) {
                    Text(String(name.prefix(1)).uppercased())
                        .font(.plusJakarta(size: 14, weight: .bold))
                        .foregroundStyle(theme.accentLight)
                        .frame(width: 40, height: 40)
                        .background(theme.accentSoft)
                        .clipShape(Circle())
                    VStack(alignment: .leading, spacing: 2) {
                        HStack(spacing: 4) {
                            Text(name)
                                .font(.plusJakarta(size: 13, weight: .semibold))
                                .foregroundStyle(theme.text)
                            if featured {
                                Text("★").foregroundStyle(Color(hex: "#FBBF24")).font(.system(size: 12))
                            }
                        }
                        Text(featured ? "Top contributor" : "Contributor")
                            .font(.plusJakarta(size: 11))
                            .foregroundStyle(theme.muted)
                    }
                }
                Spacer()
                Text(amountLabel)
                    .font(.plusJakarta(size: 13, weight: .bold))
                    .foregroundStyle(theme.text)
            }
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule().fill(theme.accentSoft)
                    Capsule()
                        .fill(theme.accent)
                        .frame(width: geo.size.width * CGFloat(min(max(percent, 0), 100)) / 100)
                }
            }
            .frame(height: 6)
        }
    }
}
