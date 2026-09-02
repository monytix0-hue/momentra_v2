import SwiftUI

/// Figma 575:14327 — Group Moments active tab (live API only).
struct GroupMomentsActiveView: View {
    let refreshToken: UInt64
    let momentId: String?
    let momentTitle: String?
    var onCreateMoment: () -> Void = {}

    @State private var pulse: APIClient.GroupPulsePayload?
    @State private var finance: APIClient.GroupFinancePayload?
    @State private var life: APIClient.GroupLifePayload?
    @State private var listPlanning: [APIClient.GroupLifePayload.LifeInner.PlanningItem] = []
    @State private var listBookings: [APIClient.GroupLifePayload.LifeInner.BookingItem] = []
    @State private var listUpdates: [APIClient.GroupLifePayload.LifeInner.UpdateItem] = []
    @State private var listPolls: [APIClient.GroupPollItemPayload] = []
    @State private var selectedPollId: String?
    @State private var loading = true
    @State private var error: String?

    private var planningItems: [APIClient.GroupLifePayload.LifeInner.PlanningItem] {
        listPlanning.isEmpty ? (life?.payload?.planningItems ?? []) : listPlanning
    }

    private var bookings: [APIClient.GroupLifePayload.LifeInner.BookingItem] {
        listBookings.isEmpty ? (life?.payload?.bookings ?? []) : listBookings
    }

    private var updates: [APIClient.GroupLifePayload.LifeInner.UpdateItem] {
        listUpdates.isEmpty ? (life?.payload?.updates ?? []) : listUpdates
    }

    private let accents: [Color] = [
        Color(hex: "#14B8A6"),
        Color(hex: "#B45309"),
        Color(hex: "#A855F7"),
        GroupActiveTheme.accentOrange,
    ]

    var body: some View {
        Group {
            if loading && pulse == nil {
                ProgressView().tint(GroupActiveTheme.brand)
            } else {
                NativeDashboardScaffold(background: GroupActiveTheme.bg) {

                    NativeListSection {

                    VStack(alignment: .leading, spacing: 14) {
                        if let error {
                            Text(error).font(.caption).foregroundStyle(Color(hex: "#F87171"))
                        }
                        GroupHeroHeader(
                            title: momentTitle ?? "Shared Moments",
                            subtitle: "Plan, share, and relive together",
                            meta: "PLANNING"
                        )
                        GroupSectionCard(title: "Shared Experience") {
                            HStack(spacing: 10) {
                                momentsMetric("PEOPLE", "\(pulse?.payload?.participantCount ?? 0)", "👥", [Color(hex: "#0F766E"), Color(hex: "#14B8A6")])
                                let plans = pulse?.payload?.openTaskCount ?? planningItems.count
                                momentsMetric("PLANS", "\(plans)", "🗺️", [Color(hex: "#E89574"), GroupActiveTheme.brand])
                            }
                            HStack(spacing: 10) {
                                momentsMetric(
                                    "BUDGET",
                                    GroupFinanceFormat.compactMoney(
                                        finance?.totals?.first?.budgetTotal,
                                        currencyCode: finance?.totals?.first?.currencyCode ?? "INR"
                                    ),
                                    "💰",
                                    [Color(hex: "#9A3412"), GroupActiveTheme.accentOrange]
                                )
                                momentsMetric("UPDATES", "\(updates.count)", "✨", [Color(hex: "#6B21A8"), Color(hex: "#A855F7")])
                            }
                        }
                        GroupSectionCard(title: "Itinerary") {
                            if planningItems.isEmpty {
                                GroupEmptySection(message: "No itinerary days yet", detail: "Add a planning item from Quick Add — nothing is invented.")
                            } else {
                                ForEach(Array(planningItems.enumerated()), id: \.offset) { index, item in
                                    momentsRow(
                                        title: item.title ?? item.planningItemId ?? "Plan",
                                        meta: formatTripInstant(item.dueAt) ?? item.status,
                                        accent: accents[index % accents.count],
                                        glyph: "📍"
                                    )
                                }
                            }
                        }
                        GroupSectionCard(title: "Bookings") {
                            if bookings.isEmpty {
                                GroupEmptySection(message: "No bookings yet", detail: "Add a booking from Quick Add when ready.")
                            } else {
                                ForEach(Array(bookings.enumerated()), id: \.offset) { index, item in
                                    momentsRow(
                                        title: item.title ?? item.bookingId ?? "Booking",
                                        meta: item.status,
                                        accent: accents[(index + 1) % accents.count],
                                        glyph: "🏨"
                                    )
                                }
                            }
                        }
                        GroupSectionCard(title: "Polls") {
                            if listPolls.isEmpty {
                                GroupEmptySection(message: "No polls yet", detail: "Create a poll from Quick Add to decide together.")
                            } else {
                                ForEach(Array(listPolls.enumerated()), id: \.offset) { index, item in
                                    Button {
                                        if let id = item.pollId { selectedPollId = id }
                                    } label: {
                                        momentsRow(
                                            title: item.question ?? item.pollId ?? "Poll",
                                            meta: item.status,
                                            accent: accents[(index + 3) % accents.count],
                                            glyph: "📊"
                                        )
                                    }
                                    .buttonStyle(.plain)
                                }
                            }
                        }
                        GroupSectionCard(title: "Updates") {
                            if updates.isEmpty {
                                GroupEmptySection(message: "No updates yet", detail: "Share a status update from Quick Add.")
                            } else {
                                ForEach(Array(updates.prefix(8).enumerated()), id: \.offset) { index, item in
                                    momentsRow(
                                        title: item.message ?? item.updateId ?? "Update",
                                        meta: formatTripInstant(item.createdAt),
                                        accent: accents[(index + 2) % accents.count],
                                        glyph: "✏️"
                                    )
                                }
                            }
                        }
                        GroupSectionCard(title: "Shared Gallery", badge: {
                            HStack(spacing: 6) {
                                GroupComingSoonBadge()
                                GroupApiGapBadge()
                            }
                        }) {
                            GroupEmptySection(message: "Gallery empty", detail: "Shared media will appear here when group media API is live.")
                        }
                        momentsQuickAddCta
                    }
                

                    }

                }
            }
        }
        .background(GroupActiveTheme.bg)
        .task(id: "\(refreshToken)-\(momentId ?? "")") { await load() }
        .sheet(item: Binding(
            get: { selectedPollId.map { PollSheetItem(id: $0) } },
            set: { selectedPollId = $0?.id }
        )) { item in
            PollDetailSheet(
                pollId: item.id,
                onDismiss: { selectedPollId = nil },
                onSaved: { Task { await load() } }
            )
        }
    }

    private struct PollSheetItem: Identifiable {
        let id: String
    }

    private var momentsQuickAddCta: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Create the next shared moment")
                .font(.plusJakarta(size: 16, weight: .heavy))
                .foregroundStyle(.white)
            Text("Plan, booking, poll, expense, memory or update.")
                .font(.plusJakarta(size: 12))
                .foregroundStyle(.white.opacity(0.85))
            Button(action: onCreateMoment) {
                Text("+ Open Quick Add")
                    .font(.plusJakarta(size: 14, weight: .bold))
                    .foregroundStyle(GroupActiveTheme.accentOrange)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(Color.white)
                    .clipShape(RoundedRectangle(cornerRadius: 14))
            }
            .buttonStyle(.plain)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(LinearGradient(colors: [GroupActiveTheme.brand, GroupActiveTheme.accentOrange], startPoint: .leading, endPoint: .trailing))
        .clipShape(RoundedRectangle(cornerRadius: 18))
    }

    private func momentsMetric(_ label: String, _ value: String, _ glyph: String, _ colors: [Color]) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(glyph).font(.system(size: 18))
            Text(label)
                .font(.plusJakarta(size: 10, weight: .bold))
                .foregroundStyle(.white.opacity(0.85))
            Text(value)
                .font(.plusJakarta(size: 22, weight: .heavy))
                .foregroundStyle(.white)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(14)
        .background(LinearGradient(colors: colors, startPoint: .topLeading, endPoint: .bottomTrailing))
        .clipShape(RoundedRectangle(cornerRadius: 18))
    }

    private func momentsRow(title: String, meta: String?, accent: Color, glyph: String) -> some View {
        HStack(spacing: 12) {
            Capsule().fill(accent).frame(width: 3, height: 36)
            Text(glyph)
                .frame(width: 36, height: 36)
                .background(accent.opacity(0.18))
                .clipShape(RoundedRectangle(cornerRadius: 12))
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.plusJakarta(size: 13, weight: .semibold))
                    .foregroundStyle(GroupActiveTheme.text)
                if let meta, !meta.isEmpty {
                    Text(meta)
                        .font(.plusJakarta(size: 11))
                        .foregroundStyle(GroupActiveTheme.secondary)
                }
            }
            Spacer(minLength: 0)
        }
        .padding(12)
        .background(Color(hex: "#181716"))
        .overlay(RoundedRectangle(cornerRadius: 14).stroke(GroupActiveTheme.border))
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }

    private func formatTripInstant(_ raw: String?) -> String? {
        guard let raw, !raw.isEmpty else { return nil }
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        if let date = formatter.date(from: raw) ?? ISO8601DateFormatter().date(from: raw) {
            let out = DateFormatter()
            out.locale = Locale(identifier: "en_US")
            out.dateFormat = "MMM d · h:mm a"
            return out.string(from: date)
        }
        return String(raw.prefix(16)).replacingOccurrences(of: "T", with: " ")
    }

    private func load() async {
        guard let momentId else { loading = false; return }
        error = nil
        if let cached = GroupTabDataCache.peekPulse(momentId) {
            pulse = cached.pulse
            finance = cached.finance
            loading = false
        } else {
            loading = true
        }
        do {
            async let pulseResult = APIClient.shared.getGroupPulse(momentId: momentId)
            async let financeResult = APIClient.shared.getGroupFinance(momentId: momentId)
            async let lifeResult = APIClient.shared.getGroupLife(momentId: momentId)
            async let plansResult = APIClient.shared.listPlanningItems(momentId: momentId)
            async let bookingsResult = APIClient.shared.listBookings(momentId: momentId)
            async let updatesResult = APIClient.shared.listGroupUpdates(momentId: momentId)
            async let pollsResult = APIClient.shared.listPolls(momentId: momentId)
            let loadedPulse = try await pulseResult
            let finFacet = try await financeResult
            let loadedLife = try await lifeResult
            let loadedFinance = finFacet.payload ?? loadedPulse.payload?.finance
            pulse = loadedPulse
            finance = loadedFinance
            life = loadedLife
            listPlanning = (try? await plansResult)?.items ?? loadedLife.payload?.planningItems ?? []
            listBookings = (try? await bookingsResult)?.items ?? loadedLife.payload?.bookings ?? []
            listUpdates = (try? await updatesResult)?.items ?? loadedLife.payload?.updates ?? []
            listPolls = (try? await pollsResult)?.items ?? []
            GroupTabDataCache.putPulse(momentId, .init(
                title: loadedPulse.title,
                pulse: loadedPulse,
                finance: loadedFinance,
                activities: []
            ))
            GroupTabDataCache.putLife(momentId, loadedLife)
        } catch {
            self.error = error.localizedDescription
        }
        loading = false
    }
}
