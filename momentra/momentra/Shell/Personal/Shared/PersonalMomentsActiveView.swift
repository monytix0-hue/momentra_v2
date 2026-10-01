import SwiftUI

/// Shared Personal Moments body — browse what happened (no Pulse scores / Life analytics).
struct PersonalMomentsActiveView: View {
    let refreshToken: UInt64
    let momentId: String?
    let momentTitle: String?
    let momentTypeCode: String?
    var onOpenQuickAdd: () -> Void = {}
    /// Called after an activity is edited/deleted from a Moments card.
    var onActivityChanged: () -> Void = {}

    @State private var pulse: APIClient.PersonalPulsePayload?
    @State private var activities: [APIClient.ActivityItemPayload] = []
    @State private var mayHaveMore = false
    @State private var loading = true
    @State private var error: String?
    @State private var editing: APIClient.ActivityItemPayload?

    private var family: PersonalPulseFamily { PersonalPulseFamily.forTypeCode(momentTypeCode) }
    private var theme: PersonalPulseFamilyTheme { family.theme }

    private var cards: [MomentCardModel] {
        MomentClustering.cards(from: activities, family: family)
    }

    private var spendPairs: [(String, String)] {
        guard let map = pulse?.widgetPayload?["spendByCurrency"]?.value as? [String: Any] else {
            return []
        }
        return map.compactMap { key, value in
            if let s = value as? String { return (key, s) }
            if let d = value as? Double { return (key, String(d)) }
            if let i = value as? Int { return (key, String(i)) }
            if let n = value as? NSNumber { return (key, n.stringValue) }
            return (key, "\(value)")
        }
    }

    var body: some View {
        Group {
            if loading && activities.isEmpty && pulse == nil {
                ProgressView().tint(theme.accent)
            } else {
                NativeDashboardScaffold(background: Color(hex: "#14121B")) {
                    NativeListSection {
                        VStack(alignment: .leading, spacing: 16) {
                            if let error {
                                Text(error)
                                    .font(.caption)
                                    .foregroundStyle(Color(hex: "#F87171"))
                            }
                            if let momentTitle, !momentTitle.isEmpty {
                                Text(momentTitle)
                                    .font(.system(size: 11, weight: .semibold))
                                    .foregroundStyle(Color(hex: "#C9C4D8"))
                            }
                            journeyHeader
                            if let money = PersonalMomentsMoneyLine.line(family: family, spendPairs: spendPairs) {
                                Text(money)
                                    .font(.plusJakarta(size: 13))
                                    .foregroundStyle(Color(hex: "#C9C4D8"))
                                    .padding(12)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                    .background(Color.white.opacity(0.05))
                                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.white.opacity(0.08)))
                                    .clipShape(RoundedRectangle(cornerRadius: 12))
                            }
                            if let highlight = PersonalMomentsHighlight.select(family: family, activities: activities) {
                                highlightCard(highlight)
                            }
                            stream
                            if mayHaveMore {
                                Text("Showing recent moments — more history may exist.")
                                    .font(.plusJakarta(size: 11))
                                    .foregroundStyle(Color(hex: "#C9C4D8"))
                            }
                            Button(action: onOpenQuickAdd) {
                                Text("Capture another moment")
                                    .font(.plusJakarta(size: 13, weight: .semibold))
                                    .foregroundStyle(theme.accent)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 12)
                                    .background(theme.accent.opacity(0.12))
                                    .clipShape(RoundedRectangle(cornerRadius: 12))
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
            }
        }
        .background(Color(hex: "#14121B"))
        .task(id: "\(refreshToken)-\(momentId ?? "")") { await load() }
        .sheet(item: $editing) { item in
            if let momentId, !momentId.isEmpty {
                if PersonalActivityTimelineDerived.isExpense(item) {
                    PersonalEditTransactionSheet(
                        momentId: momentId,
                        item: item,
                        onClose: { editing = nil },
                        onSaved: {
                            editing = nil
                            onActivityChanged()
                            Task { await load() }
                        },
                        onDeleted: {
                            editing = nil
                            onActivityChanged()
                            Task { await load() }
                        }
                    )
                    .presentationDetents([.large])
                } else {
                    PersonalEditActivitySheet(
                        momentId: momentId,
                        item: item,
                        onClose: { editing = nil },
                        onSaved: {
                            editing = nil
                            onActivityChanged()
                            Task { await load() }
                        },
                        onDeleted: {
                            editing = nil
                            onActivityChanged()
                            Task { await load() }
                        }
                    )
                    .presentationDetents([.large])
                }
            }
        }
    }

    private var journeyHeader: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(PersonalMomentsHeaderCopy.periodLabel())
                .font(.plusJakarta(size: 22, weight: .heavy))
                .foregroundStyle(Color(hex: "#E5E0EE"))
            Text(PersonalMomentsHeaderCopy.subtitle(momentCount: cards.count))
                .font(.plusJakarta(size: 14))
                .foregroundStyle(Color(hex: "#C9C4D8"))
            Text(family.switcherLabel)
                .font(.plusJakarta(size: 11, weight: .bold))
                .foregroundStyle(theme.accent)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            LinearGradient(colors: [theme.heroStart.opacity(0.35), theme.heroEnd.opacity(0.2)], startPoint: .topLeading, endPoint: .bottomTrailing)
        )
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08)))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func highlightCard(_ item: PersonalMomentsHighlight.Item) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(PersonalMomentsHighlight.sectionTitle(for: family))
                .font(.plusJakarta(size: 12, weight: .bold))
                .foregroundStyle(theme.accent)
            Text(item.title)
                .font(.plusJakarta(size: 15, weight: .semibold))
                .foregroundStyle(Color(hex: "#E5E0EE"))
            Text(item.detail)
                .font(.plusJakarta(size: 12))
                .foregroundStyle(Color(hex: "#C9C4D8"))
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(theme.accent.opacity(0.1))
        .overlay(RoundedRectangle(cornerRadius: 14).stroke(theme.accent.opacity(0.3)))
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }

    private var stream: some View {
        let sections = MomentStreamGroup.sections(from: cards)
        return VStack(alignment: .leading, spacing: 14) {
            if sections.isEmpty {
                Text("No moments yet. Capture one from Add.")
                    .font(.plusJakarta(size: 13))
                    .foregroundStyle(Color(hex: "#C9C4D8"))
            } else {
                ForEach(Array(sections.enumerated()), id: \.offset) { _, section in
                    VStack(alignment: .leading, spacing: 8) {
                        Text(section.header)
                            .font(.plusJakarta(size: 12, weight: .bold))
                            .foregroundStyle(Color(hex: "#C9C4D8"))
                        ForEach(section.cards) { card in
                            MomentCard(model: card, accent: theme.accent) {
                                if card.tapDestination == .activityDetail {
                                    openDetail(for: card)
                                }
                            }
                            Divider().overlay(Color.white.opacity(0.05))
                        }
                    }
                }
            }
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white.opacity(0.05))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08)))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func openDetail(for card: MomentCardModel) {
        // Prefer primary source id (first in cluster / single).
        for sid in card.sourceActivityIds {
            if let match = activities.first(where: { activity in
                let id = activity.activityPayload?.activityId
                    ?? activity.activityPayload?.expenseId
                    ?? activity.activityPayload?.incomeId
                    ?? activity.id
                return id == sid || activity.id == sid
            }) {
                editing = match
                return
            }
        }
        editing = activities.first(where: { $0.id == card.id })
    }

    private func load() async {
        error = nil
        loading = activities.isEmpty
        do {
            let page = try await PersonalTabLoad.loadMomentsTab(momentId: momentId)
            pulse = page.pulse
            activities = page.activities
            mayHaveMore = page.mayHaveMore
        } catch {
            self.error = error.localizedDescription
        }
        loading = false
    }
}
