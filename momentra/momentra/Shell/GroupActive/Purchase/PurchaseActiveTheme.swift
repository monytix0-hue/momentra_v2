import SwiftUI

/// Shared Purchase active tokens — Gift Pool 601:* / Group Purchase / Shared Asset / Custom Purchase.
struct PurchaseActiveTheme {
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
    let hubHeroAssetName: String
    let heroEmoji: String
    let participantRoles: [String]
    let participantSubtitle: String
    let heroGradientColors: [Color]
    let pulseHeroGradientColors: [Color]
    let statGradients: [[Color]]
    let includesVendor: Bool
    let includesOwnership: Bool
    let includesDelivery: Bool
    let includesContributor: Bool
    let includesBudget: Bool
    let quickChips: [(emoji: String, label: String, kind: PurchaseQuickAddKind)]

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

    static let giftPool = PurchaseActiveTheme(
        bg: Color(hex: "#14121B"),
        accent: Color(hex: "#EC4899"),
        accentSolid: Color(hex: "#EC4899"),
        accentLight: Color(hex: "#F472B6"),
        accentSoft: Color(hex: "#EC4899").opacity(0.2),
        text: Color(hex: "#E5E2E1"),
        secondary: Color(hex: "#9CA3AF"),
        muted: Color(hex: "#9CA3AF"),
        card: Color(hex: "#1C1926"),
        border: Color(hex: "#2A2538"),
        darkText: Color(hex: "#14121B"),
        peachChip: Color(hex: "#FBBF24"),
        tealChip: Color(hex: "#14B8A6"),
        purpleChip: Color(hex: "#A855F7"),
        typeLabel: "Gift Pool",
        pulseTitle: "Gift Pool Pulse",
        contributionsTitle: "Member Contributions",
        budgetTitle: "Gift Budget",
        hubHeroAssetName: "GiftPoolHubHero",
        heroEmoji: "🎁",
        participantRoles: ["Organizer", "Contributor", "Recipient"],
        participantSubtitle: "Invite people who will chip in toward the gift",
        heroGradientColors: [Color(hex: "#F472B6"), Color(hex: "#EC4899")],
        pulseHeroGradientColors: [Color(hex: "#F9A8D4"), Color(hex: "#DB2777").opacity(0.9)],
        statGradients: [
            [Color(hex: "#BE185D"), Color(hex: "#9D174D")],
            [Color(hex: "#EC4899"), Color(hex: "#DB2777")],
            [Color(hex: "#7C3AED"), Color(hex: "#5B21B6")],
            [Color(hex: "#C026D3"), Color(hex: "#86198F")],
        ],
        includesVendor: false,
        includesOwnership: false,
        includesDelivery: true,
        includesContributor: true,
        includesBudget: true,
        quickChips: [
            ("🎁", "Gift", .purchaseItem),
            ("💰", "Contribute", .contribution),
            ("📋", "Budget", .budget),
            ("📷", "Memory", .memory),
        ]
    )

    static let groupPurchase = PurchaseActiveTheme(
        bg: Color(hex: "#14121B"),
        accent: Color(hex: "#FF7A3D"),
        accentSolid: Color(hex: "#FF7A3D"),
        accentLight: Color(hex: "#FB923C"),
        accentSoft: Color(hex: "#FF7A3D").opacity(0.2),
        text: Color(hex: "#E5E2E1"),
        secondary: Color(hex: "#9CA3AF"),
        muted: Color(hex: "#9CA3AF"),
        card: Color(hex: "#1C1926"),
        border: Color(hex: "#2A2538"),
        darkText: Color(hex: "#14121B"),
        peachChip: Color(hex: "#FBBF24"),
        tealChip: Color(hex: "#14B8A6"),
        purpleChip: Color(hex: "#A855F7"),
        typeLabel: "Group Purchase",
        pulseTitle: "Purchase Pulse",
        contributionsTitle: "Member Contributions",
        budgetTitle: "Purchase Budget",
        hubHeroAssetName: "GroupPurchaseHubHero",
        heroEmoji: "🛒",
        participantRoles: ["Organizer", "Buyer", "Contributor"],
        participantSubtitle: "Invite people sharing this group purchase",
        heroGradientColors: [Color(hex: "#FB923C"), Color(hex: "#FF7A3D")],
        pulseHeroGradientColors: [Color(hex: "#FDBA74"), Color(hex: "#EA580C").opacity(0.9)],
        statGradients: [
            [Color(hex: "#C2410C"), Color(hex: "#9A3412")],
            [Color(hex: "#FF7A3D"), Color(hex: "#EA580C")],
            [Color(hex: "#7C3AED"), Color(hex: "#5B21B6")],
            [Color(hex: "#FB923C"), Color(hex: "#C2410C")],
        ],
        includesVendor: true,
        includesOwnership: false,
        includesDelivery: true,
        includesContributor: true,
        includesBudget: true,
        quickChips: [
            ("🛒", "Item", .purchaseItem),
            ("💰", "Contribute", .contribution),
            ("🏪", "Vendor", .vendor),
            ("📷", "Memory", .memory),
        ]
    )

    static let sharedAsset = PurchaseActiveTheme(
        bg: Color(hex: "#14121B"),
        accent: Color(hex: "#8B5CF6"),
        accentSolid: Color(hex: "#8B5CF6"),
        accentLight: Color(hex: "#A78BFA"),
        accentSoft: Color(hex: "#8B5CF6").opacity(0.2),
        text: Color(hex: "#E5E2E1"),
        secondary: Color(hex: "#9CA3AF"),
        muted: Color(hex: "#9CA3AF"),
        card: Color(hex: "#1C1926"),
        border: Color(hex: "#2A2538"),
        darkText: Color(hex: "#14121B"),
        peachChip: Color(hex: "#FBBF24"),
        tealChip: Color(hex: "#14B8A6"),
        purpleChip: Color(hex: "#A855F7"),
        typeLabel: "Shared Asset",
        pulseTitle: "Asset Pulse",
        contributionsTitle: "Member Contributions",
        budgetTitle: "Asset Budget",
        hubHeroAssetName: "SharedAssetHubHero",
        heroEmoji: "🏠",
        participantRoles: ["Owner", "Co-owner", "Contributor"],
        participantSubtitle: "Invite people sharing ownership of this asset",
        heroGradientColors: [Color(hex: "#A78BFA"), Color(hex: "#8B5CF6")],
        pulseHeroGradientColors: [Color(hex: "#C4B5FD"), Color(hex: "#6D28D9").opacity(0.9)],
        statGradients: [
            [Color(hex: "#6D28D9"), Color(hex: "#5B21B6")],
            [Color(hex: "#8B5CF6"), Color(hex: "#6D28D9")],
            [Color(hex: "#7C3AED"), Color(hex: "#4C1D95")],
            [Color(hex: "#A78BFA"), Color(hex: "#7C3AED")],
        ],
        includesVendor: true,
        includesOwnership: true,
        includesDelivery: true,
        includesContributor: true,
        includesBudget: true,
        quickChips: [
            ("🏠", "Asset", .purchaseItem),
            ("💰", "Contribute", .contribution),
            ("🔑", "Ownership", .ownership),
            ("📷", "Memory", .memory),
        ]
    )

    static let customPurchase = PurchaseActiveTheme(
        bg: Color(hex: "#14121B"),
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
        typeLabel: "Custom Purchase",
        pulseTitle: "Purchase Pulse",
        contributionsTitle: "Member Contributions",
        budgetTitle: "Purchase Budget",
        hubHeroAssetName: "CustomPurchaseHubHero",
        heroEmoji: "✨",
        participantRoles: ["Organizer", "Member", "Guest"],
        participantSubtitle: "Invite people into this custom purchase",
        heroGradientColors: [Color(hex: "#FBBF24"), Color(hex: "#F59E0B")],
        pulseHeroGradientColors: [Color(hex: "#FCD34D"), Color(hex: "#D97706").opacity(0.9)],
        statGradients: [
            [Color(hex: "#B45309"), Color(hex: "#92400E")],
            [Color(hex: "#F59E0B"), Color(hex: "#D97706")],
            [Color(hex: "#7C3AED"), Color(hex: "#5B21B6")],
            [Color(hex: "#FBBF24"), Color(hex: "#B45309")],
        ],
        includesVendor: true,
        includesOwnership: true,
        includesDelivery: true,
        includesContributor: false,
        includesBudget: false,
        quickChips: [
            ("✨", "Item", .purchaseItem),
            ("💳", "Expense", .expense),
            ("🏪", "Vendor", .vendor),
            ("📷", "Memory", .memory),
        ]
    )

    static func forFamily(_ family: GroupExperienceFamily) -> PurchaseActiveTheme {
        switch family {
        case .groupPurchase: return .groupPurchase
        case .sharedAsset: return .sharedAsset
        case .customPurchase: return .customPurchase
        default: return .giftPool
        }
    }
}

struct PurchaseSectionCard<Content: View, Trailing: View>: View {
    let theme: PurchaseActiveTheme
    let title: String
    @ViewBuilder var trailing: () -> Trailing
    @ViewBuilder var content: () -> Content

    init(
        theme: PurchaseActiveTheme,
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

struct PurchaseEmptyBlock: View {
    let theme: PurchaseActiveTheme
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

struct PurchaseStatCard: View {
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

struct PurchaseEmojiChip: View {
    let theme: PurchaseActiveTheme
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

struct PurchaseCrewRow: View {
    let theme: PurchaseActiveTheme
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
