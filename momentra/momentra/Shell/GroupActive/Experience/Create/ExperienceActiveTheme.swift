import SwiftUI

/// Shared Experience active tokens — House Party 584:* (blue) / Office Outing 584:* (teal).
struct ExperienceActiveTheme {
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
    let crewTitle: String
    let budgetTitle: String
    let progressTitle: String
    let insightsTitle: String
    let healthLabel: String
    let hubHeroAssetName: String
    let heroEmoji: String
    let participantRoles: [String]
    let participantSubtitle: String
    let heroGradientColors: [Color]
    let pulseHeroGradientColors: [Color]
    let statGradients: [[Color]]
    let includesVendor: Bool
    let quickChips: [(emoji: String, label: String, kind: ExperienceQuickAddKind)]

    var heroGradient: LinearGradient {
        LinearGradient(colors: heroGradientColors, startPoint: .leading, endPoint: .trailing)
    }

    /// Figma 584:15689 / outing twin — diagonal hero wash.
    var pulseHeroGradient: LinearGradient {
        LinearGradient(
            colors: pulseHeroGradientColors,
            startPoint: UnitPoint(x: 0.1, y: 0),
            endPoint: UnitPoint(x: 0.9, y: 1)
        )
    }

    var sectionRadius: CGFloat { 20 }

    static let houseParty = ExperienceActiveTheme(
        bg: Color(hex: "#131313"),
        accent: Color(hex: "#3B82F6"),
        accentSolid: Color(hex: "#3B82F6"),
        accentLight: Color(hex: "#60A5FA"),
        accentSoft: Color(hex: "#3B82F6").opacity(0.2),
        text: Color(hex: "#E5E2E1"),
        secondary: Color(hex: "#A8B4C8"),
        muted: Color(hex: "#A8A19E"),
        card: Color(hex: "#201F1F"),
        border: Color.white.opacity(0.1),
        darkText: Color(hex: "#14121B"),
        peachChip: Color(hex: "#FBBF24"),
        tealChip: Color(hex: "#14B8A6"),
        purpleChip: Color(hex: "#A855F7"),
        typeLabel: "House Party",
        pulseTitle: "Party Pulse",
        crewTitle: "Party Crew",
        budgetTitle: "Party Budget",
        progressTitle: "Party Progress",
        insightsTitle: "Party Insights",
        healthLabel: "Party Health",
        hubHeroAssetName: "HousePartyHubHero",
        heroEmoji: "🎉",
        participantRoles: ["Host", "Co-host", "Guest"],
        participantSubtitle: "Invite and manage party guest list",
        heroGradientColors: [Color(hex: "#60A5FA"), Color(hex: "#2563EB")],
        pulseHeroGradientColors: [Color(hex: "#669EFA"), Color(hex: "#2659D9").opacity(0.85)],
        statGradients: [
            [Color(hex: "#2563EB"), Color(hex: "#1D4ED8")],
            [Color(hex: "#3B82F6"), Color(hex: "#1E40AF")],
            [Color(hex: "#60A5FA"), Color(hex: "#2563EB")],
        ],
        includesVendor: true,
        quickChips: [
            ("📷", "Photos", .memory),
            ("🎵", "Playlist", .update),
            ("📋", "Menu", .planning),
            ("🍹", "Drinks", .expense),
        ]
    )

    static let officeOuting = ExperienceActiveTheme(
        bg: Color(hex: "#131313"),
        accent: Color(hex: "#14B8A6"),
        accentSolid: Color(hex: "#14B8A6"),
        accentLight: Color(hex: "#2DD4BF"),
        accentSoft: Color(hex: "#14B8A6").opacity(0.2),
        text: Color(hex: "#E5E2E1"),
        secondary: Color(hex: "#A8C4C0"),
        muted: Color(hex: "#A8A19E"),
        card: Color(hex: "#201F1F"),
        border: Color.white.opacity(0.1),
        darkText: Color(hex: "#14121B"),
        peachChip: Color(hex: "#FBBF24"),
        tealChip: Color(hex: "#14B8A6"),
        purpleChip: Color(hex: "#A855F7"),
        typeLabel: "Office Outing",
        pulseTitle: "Team Retreat Pulse",
        crewTitle: "Team Leads",
        budgetTitle: "Retreat Budget",
        progressTitle: "Retreat Progress",
        insightsTitle: "Team Retreat Insights",
        healthLabel: "Team Health",
        hubHeroAssetName: "OfficeOutingHubHero",
        heroEmoji: "🧳",
        participantRoles: ["Organizer", "Teammate", "Guest"],
        participantSubtitle: "Invite and manage outing attendees",
        heroGradientColors: [Color(hex: "#2DD4BF"), Color(hex: "#0F766E")],
        pulseHeroGradientColors: [Color(hex: "#2DD4BF"), Color(hex: "#0F766E").opacity(0.9)],
        statGradients: [
            [Color(hex: "#0D9488"), Color(hex: "#0F766E")],
            [Color(hex: "#14B8A6"), Color(hex: "#0F766E")],
            [Color(hex: "#2DD4BF"), Color(hex: "#0D9488")],
        ],
        includesVendor: false,
        quickChips: [
            ("📷", "Photos", .memory),
            ("💬", "Agenda", .planning),
            ("🧳", "Transport", .booking),
            ("💰", "Expenses", .expense),
        ]
    )

    static func forFamily(_ family: GroupExperienceFamily) -> ExperienceActiveTheme {
        switch family {
        case .officeOuting: return .officeOuting
        default: return .houseParty
        }
    }
}

struct ExperienceSectionCard<Content: View, Trailing: View>: View {
    let theme: ExperienceActiveTheme
    let title: String
    @ViewBuilder var trailing: () -> Trailing
    @ViewBuilder var content: () -> Content

    init(
        theme: ExperienceActiveTheme,
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

struct ExperienceEmptyBlock: View {
    let theme: ExperienceActiveTheme
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

struct ExperienceStatCard: View {
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
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.1)))
    }
}

struct ExperienceEmojiChip: View {
    let theme: ExperienceActiveTheme
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

struct ExperienceCrewRow: View {
    let theme: ExperienceActiveTheme
    let name: String
    let role: String
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
                            Text("\(name) (\(role))")
                                .font(.plusJakarta(size: 13, weight: .semibold))
                                .foregroundStyle(theme.text)
                            if featured {
                                Text("★").foregroundStyle(Color(hex: "#FBBF24")).font(.system(size: 12))
                            }
                        }
                        Text(featured ? "Most active" : "Active")
                            .font(.plusJakarta(size: 11))
                            .foregroundStyle(theme.muted)
                    }
                }
                Spacer()
                Text("\(percent)%")
                    .font(.plusJakarta(size: 12, weight: .bold))
                    .foregroundStyle(theme.accentLight)
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
