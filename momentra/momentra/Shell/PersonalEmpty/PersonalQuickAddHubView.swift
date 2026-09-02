import SwiftUI

/// Figma `1006:7553` personal-quick-add-grid.
/// Relationships family: Figma `1006:8274` Action Center / Relationships.
/// Bottom-tab + opens this hub. Tile set follows active moment family.
struct PersonalQuickAddHubView: View {
    let hasActiveMoment: Bool
    var momentTypeCode: String? = nil
    /// Optional V019 capability filter; nil uses family defaults from `PersonalActionRegistry`.
    var capabilityCodes: [String]? = nil
    var onClose: () -> Void
    var onIncome: () -> Void
    var onRecovery: () -> Void = {}
    var onMood: () -> Void = {}
    var onAttention: () -> Void = {}
    var onAdjust: () -> Void = {}
    var onTransfer: () -> Void = {}
    var onSavings: () -> Void = {}
    var onFutureQuickAdd: (FutureQuickAddKind) -> Void = { _ in }
    var onLifestyleQuickAdd: (LifestyleQuickAddKind) -> Void = { _ in }
    var onRelationshipsQuickAdd: (RelationshipsQuickAddKind) -> Void = { _ in }

    @State private var search = ""

    private var family: PersonalPulseFamily {
        PersonalPulseFamily.forTypeCode(momentTypeCode)
    }

    private var isRelationships: Bool { family == .relationships }
    private var isLifestyle: Bool { family == .lifestyle }
    private var isLifeOps: Bool { family == .lifeOperations }
    private var useWideTiles: Bool { isRelationships || isLifestyle || isLifeOps }

    private var heroTitle: String {
        switch family {
        case .futureBuilding: return "Build your future"
        case .relationships: return "Nurture your bonds"
        case .lifestyle: return "Curate your lifestyle"
        default: return "Design your focus"
        }
    }

    private var blurb: String {
        switch family {
        case .futureBuilding:
            return "Track milestones, opportunities, pivots, goals and investments."
        case .lifestyle:
            return "Track experiences, wellbeing, discoveries, expressions and adjustments."
        case .relationships:
            return "Track connections, support, shared experiences, investments and adjustments."
        case .lifeOperations:
            return "Quickly record expenses, recovery states, mood, attention targets, and reflections."
        }
    }

    private var searchPlaceholder: String {
        switch family {
        case .futureBuilding: return "Search future actions…"
        case .lifestyle: return "Search lifestyle actions..."
        case .relationships: return "Search relationships..."
        default: return "Search personal actions..."
        }
    }

    private var chipAccent: Color {
        switch family {
        case .relationships: return Color(hex: "#E12A9E")
        case .lifestyle: return Color(hex: "#6C4EF2")
        default: return Color(hex: "#6C4EF2")
        }
    }

    private var heroGradient: [Color] {
        switch family {
        case .relationships:
            return [Color(hex: "#14B8A6").opacity(0.2), Color(hex: "#10B981").opacity(0.122)]
        case .lifestyle:
            return [Color(hex: "#EC4899").opacity(0.122), Color(hex: "#A78BFA").opacity(0.122)]
        default:
            return [Color(hex: "#8B5CF6").opacity(0.24), Color(hex: "#6C4EF2").opacity(0.12)]
        }
    }

    private var heroShadowColor: Color {
        switch family {
        case .relationships: return Color(hex: "#10B981")
        case .lifestyle: return Color(hex: "#EC4899")
        default: return Color(hex: "#6C4EF2")
        }
    }

    private var actions: [PersonalActionTile] {
        PersonalActionRegistry.tiles(
            for: family,
            hasActiveMoment: hasActiveMoment,
            capabilityCodes: capabilityCodes
        )
    }

    private var filteredActions: [PersonalActionTile] {
        let q = search.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard !q.isEmpty else { return actions }
        return actions.filter { $0.label.lowercased().contains(q) }
    }

    private var gridGap: CGFloat { useWideTiles ? 12 : 10 }

    /// APK `PersonalQuickAddHub.kt` row builder — 3+3+2 for LifeOps; 3+remainder for other wide families.
    private var actionRows: [[PersonalActionTile]] {
        let isSearchBlank = search.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        if isLifeOps && isSearchBlank {
            return [
                Array(filteredActions.prefix(3)),
                Array(filteredActions.dropFirst(3).prefix(3)),
                Array(filteredActions.dropFirst(6).prefix(2)),
            ]
        }
        if useWideTiles && isSearchBlank {
            return [
                Array(filteredActions.prefix(3)),
                Array(filteredActions.dropFirst(3)),
            ]
        }
        return stride(from: 0, to: filteredActions.count, by: 3).map {
            Array(filteredActions[$0..<min($0 + 3, filteredActions.count)])
        }
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 14) {
                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Quick Add")
                            .font(.plusJakarta(size: useWideTiles ? 22 : 20, weight: .heavy))
                            .foregroundStyle(Color(hex: "#E5E0EE"))
                        Text("Simplify your day and align focus")
                            .font(.plusJakarta(size: 12))
                            .foregroundStyle(Color(hex: "#C9C4D8"))
                    }
                    Spacer()
                    Button(action: onClose) {
                        PersonalQaIcons.close(size: 12)
                            .frame(width: 32, height: 32)
                            .background(Color(hex: "#201E28"))
                            .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color(hex: "#938EA1"), lineWidth: 1))
                            .clipShape(RoundedRectangle(cornerRadius: 16))
                    }
                    .buttonStyle(.plain)
                }

                HStack(spacing: 7) {
                    switch family {
                    case .futureBuilding:
                        hubChip("Future Building", selected: true)
                        hubChip("Growth Mindset", selected: false)
                    case .lifeOperations:
                        hubChip("Personal Space", selected: true)
                        hubChip("Introspective", selected: false)
                    case .lifestyle:
                        hubChip("Lifestyle", selected: true)
                        hubChip("Wellness", selected: false)
                    case .relationships:
                        hubChip("Relationships", selected: true, selectedBorder: Color(hex: "#14B8A6"))
                        hubChip("Connections", selected: false, filledUnselected: true)
                    }
                }

                VStack(alignment: .leading, spacing: 12) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(heroTitle)
                            .font(.plusJakarta(size: 18, weight: .heavy))
                            .foregroundStyle(Color(hex: "#E5E0EE"))
                        Text(blurb)
                            .font(.plusJakarta(size: 12))
                            .foregroundStyle(Color(hex: "#C9C4D8"))
                    }
                    HStack {
                        Spacer(minLength: 0)
                        Image(isRelationships ? "qa_hero_relationships" : "qa_hero")
                            .resizable()
                            .scaledToFill()
                            .frame(width: 180, height: 120)
                            .clipShape(RoundedRectangle(cornerRadius: 16))
                    }
                }
                .padding(20)
                .background(
                    LinearGradient(
                        colors: heroGradient,
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
                .clipShape(RoundedRectangle(cornerRadius: 22))
                .shadow(color: heroShadowColor.opacity(0.1), radius: 24, y: 8)

                HStack(spacing: 10) {
                    PersonalQaIcons.search(size: 15)
                        .foregroundStyle(isRelationships ? Color(hex: "#14B8A6") : Color(hex: "#C9C4D8"))
                    TextField(searchPlaceholder, text: $search)
                        .font(.plusJakarta(size: 13))
                        .foregroundStyle(Color(hex: "#E5E0EE"))
                    Spacer()
                }
                .padding(.horizontal, useWideTiles ? 14 : 12)
                .padding(.vertical, useWideTiles ? 11 : 10)
                .background(Color(hex: "#201E28"))
                .clipShape(RoundedRectangle(cornerRadius: useWideTiles ? 14 : 12))
                .shadow(color: Color(hex: "#6C4EF2").opacity(0.14), radius: 10, y: 4)

                ForEach(Array(actionRows.enumerated()), id: \.offset) { _, row in
                    actionRow(row)
                }

                if !hasActiveMoment {
                    Text("Create a Personal Moment from the top-bar + to unlock Quick Add actions.")
                        .font(.plusJakarta(size: 11))
                        .foregroundStyle(Color(hex: "#C9C4D8"))
                }

                Spacer(minLength: 12)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 10)
        }
        .background(Color(hex: "#14121B"))
    }

    @ViewBuilder
    private func actionRow(_ row: [PersonalActionTile]) -> some View {
        let isSearchBlank = search.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        let fillEmptySlots = !(useWideTiles && row.count == 2 && isSearchBlank)
        HStack(spacing: gridGap) {
            ForEach(Array(row.enumerated()), id: \.offset) { _, action in
                actionCard(action)
            }
            if fillEmptySlots {
                ForEach(0..<(3 - row.count), id: \.self) { _ in
                    Color.clear.frame(maxWidth: .infinity)
                }
            }
        }
    }

    private func hubChip(
        _ label: String,
        selected: Bool,
        selectedBorder: Color? = nil,
        filledUnselected: Bool = false
    ) -> some View {
        let bg: Color = {
            if selected { return chipAccent }
            if filledUnselected { return Color(hex: "#201E28") }
            return .clear
        }()
        let border: Color = {
            if selected { return selectedBorder ?? chipAccent.opacity(0.3) }
            return Color(hex: "#938EA1")
        }()
        return Text(label)
            .font(.plusJakarta(size: selected ? 11 : 12, weight: selected ? .bold : .semibold))
            .foregroundStyle(selected ? .white : Color(hex: "#C9C4D8"))
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(bg)
            .overlay(Capsule().stroke(border, lineWidth: 1))
            .clipShape(Capsule())
            .shadow(color: selected ? chipAccent.opacity(0.35) : .clear, radius: 8, y: 2)
    }

    @ViewBuilder
    private func actionCard(_ action: PersonalActionTile) -> some View {
        let isVisuallyActive = action.tappable ? action.enabledWhenMomentActive : true
        Button {
            switch action.label {
            case "Expense", "Income": onIncome()
            case "Recovery": onRecovery()
            case "Mood": onMood()
            case "Attention": onAttention()
            case "Adjust":
                switch family {
                case .lifestyle: onLifestyleQuickAdd(.adjust)
                case .relationships: onRelationshipsQuickAdd(.adjust)
                default: onAdjust()
                }
            case "Milestone": onFutureQuickAdd(.milestone)
            case "Opportunity": onFutureQuickAdd(.opportunity)
            case "Pivot": onFutureQuickAdd(.pivot)
            case "Progress": onFutureQuickAdd(.progress)
            case "Learning": onFutureQuickAdd(.learning)
            case "Experience": onLifestyleQuickAdd(.experience)
            case "Wellbeing": onLifestyleQuickAdd(.wellbeing)
            case "Discovery": onLifestyleQuickAdd(.discovery)
            case "Create", "Expression": onLifestyleQuickAdd(.expression)
            case "Connection": onRelationshipsQuickAdd(.connection)
            case "Shared", "Shared Exp": onRelationshipsQuickAdd(.shared)
            case "Investment": onRelationshipsQuickAdd(.investment)
            case "Support": onRelationshipsQuickAdd(.support)
            case "Transfer": onTransfer()
            case "Savings": onSavings()
            default: break
            }
        } label: {
            VStack(spacing: 10) {
                PersonalQaIcons.tileIcon(asset: action.icon, size: 28)
                    .opacity(isVisuallyActive ? 1 : 0.55)
                Text(action.label)
                    .font(.plusJakarta(size: useWideTiles ? 14 : 12, weight: .bold))
                    .foregroundStyle(.white.opacity(isVisuallyActive ? 1 : 0.55))
            }
            .padding(16)
            .frame(maxWidth: .infinity)
            .frame(height: useWideTiles ? 104 : 88)
            .background(LinearGradient(colors: action.colors, startPoint: .leading, endPoint: .trailing))
            .overlay(
                RoundedRectangle(cornerRadius: useWideTiles ? 16 : 14)
                    .stroke(Color.white.opacity(0.12), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: useWideTiles ? 16 : 14))
            .shadow(color: Color(hex: "#6C4EF2").opacity(0.14), radius: 10, y: 4)
        }
        .buttonStyle(.plain)
        .disabled(!action.tappable || !action.enabledWhenMomentActive)
        .accessibilityIdentifier(qaTileId(for: action.label))
    }

    private func qaTileId(for label: String) -> String {
        switch label {
        case "Expense": return "qa.tile.expense"
        case "Income": return "qa.tile.income"
        case "Experience": return "qa.tile.experience"
        case "Wellbeing": return "qa.tile.wellbeing"
        case "Discovery": return "qa.tile.discovery"
        case "Create", "Expression": return "qa.tile.expression"
        case "Connection": return "qa.tile.connection"
        case "Support": return "qa.tile.support"
        case "Shared Exp": return "qa.tile.shared_exp"
        case "Investment": return "qa.tile.investment"
        default: return "qa.tile.\(label.lowercased().replacingOccurrences(of: " ", with: "_"))"
        }
    }
}
