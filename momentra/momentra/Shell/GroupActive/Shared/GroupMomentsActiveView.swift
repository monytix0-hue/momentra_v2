import SwiftUI

/// Figma 575:14327 — Group Moments active tab (live API only).
struct GroupMomentsActiveView: View {
    let refreshToken: UInt64
    let momentId: String?
    let momentTitle: String?
    var momentTypeCode: String? = nil
    var onCreateMoment: () -> Void = {}

    @State private var pulse: APIClient.GroupPulsePayload?
    @State private var finance: APIClient.GroupFinancePayload?
    @State private var life: APIClient.GroupLifePayload?
    @State private var listPlanning: [GroupPlanningItem] = []
    @State private var listBookings: [APIClient.GroupLifePayload.LifeInner.BookingItem] = []
    @State private var listUpdates: [GroupUpdateItem] = []
    @State private var listPolls: [APIClient.GroupPollItemPayload] = []
    @State private var listMemoryItems: [GroupMemoryItem] = []
    @State private var listExpenses: [APIClient.GroupExpenseListItemPayload] = []
    @State private var memoryCount: Int = 0
    @State private var selectedPollId: String?
    @State private var pollsListOpen = false
    @State private var scheduleOpen = false
    @State private var loading = true
    @State private var error: String?

    private var planningItems: [GroupPlanningItem] {
        listPlanning.isEmpty ? (life?.payload?.planningItems ?? []) : listPlanning
    }

    private var bookings: [APIClient.GroupLifePayload.LifeInner.BookingItem] {
        listBookings.isEmpty ? (life?.payload?.bookings ?? []) : listBookings
    }

    private var updates: [GroupUpdateItem] {
        listUpdates.isEmpty ? (life?.payload?.updates ?? []) : listUpdates
    }

    private let itineraryAccents: [Color] = [
        Color(hex: "#14B8A6"),
        Color(hex: "#E88A4F"),
        Color(hex: "#A855F7"),
    ]

    private let avatarColors: [Color] = [
        Color(hex: "#FDBA74"),
        Color(hex: "#86EFAC"),
        Color(hex: "#93C5FD"),
        Color(hex: "#C4B5FD"),
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
                            sharedExperienceHero
                            pollsSection
                            itinerarySection
                            updatesSection
                            gallerySection
                            bookingsSection
                            upcomingSection
                            expensesSection
                            momentsQuickAddCta
                        }
                    }
                }
            }
        }
        .background(GroupActiveTheme.bg)
        .task(id: "\(refreshToken)-\(momentId ?? "")") { await load() }
        .sheet(isPresented: $scheduleOpen) {
            PlanningScheduleSheet(
                items: planningItems,
                momentTypeCode: momentTypeCode,
                accent: Color(hex: "#14B8A6"),
                surface: GroupActiveTheme.bg,
                field: GroupActiveTheme.card,
                border: GroupActiveTheme.border,
                text: GroupActiveTheme.text,
                muted: GroupActiveTheme.secondary,
                onDismiss: { scheduleOpen = false }
            )
        }
        .sheet(isPresented: $pollsListOpen) {
            GroupPollsListSheet(
                momentTitle: momentTitle ?? pulse?.title,
                chrome: .trip,
                polls: listPolls,
                onDismiss: { pollsListOpen = false },
                onChanged: { Task { await load() } }
            )
        }
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

    // MARK: - Hero

    private var sharedExperienceHero: some View {
        let people = pulse?.payload?.participantCount ?? 0
        let plansPct = planningPlansPercent(planningItems)
        let currency = finance?.totals?.first?.currencyCode ?? "INR"
        let budget = GroupFinanceFormat.compactMoney(
            finance?.totals?.first?.budgetTotal,
            currencyCode: currency
        )
        let moments = memoryCount > 0 ? memoryCount : listMemoryItems.count
        let status = (pulse?.status ?? life?.status ?? "PLANNING").uppercased()
        let title = momentTitle ?? pulse?.title ?? "Shared Moments"

        return VStack(alignment: .leading, spacing: 16) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("SHARED EXPERIENCE")
                        .font(.plusJakarta(size: 11, weight: .semibold))
                        .foregroundStyle(GroupActiveTheme.secondary)
                        .tracking(0.6)
                    Text(title)
                        .font(.plusJakarta(size: 24, weight: .bold))
                        .foregroundStyle(GroupActiveTheme.text)
                }
                Spacer(minLength: 8)
                Text(status)
                    .font(.plusJakarta(size: 10, weight: .bold))
                    .foregroundStyle(Color(hex: "#591D00"))
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(GroupActiveTheme.brand)
                    .clipShape(RoundedRectangle(cornerRadius: 8))
            }
            VStack(spacing: 12) {
                HStack(spacing: 12) {
                    momentsMetric("PEOPLE", "\(people)", "👥", [Color(hex: "#14B8A6"), Color(hex: "#0F766E")])
                    momentsMetric("PLANS", "\(plansPct)%", "✅", [Color(hex: "#FF8E63"), Color(hex: "#E8744F")])
                }
                HStack(spacing: 12) {
                    momentsMetric("BUDGET", budget, "₹", [Color(hex: "#E88A4F"), Color(hex: "#C2410C")])
                    momentsMetric("MOMENTS", "\(moments)", "✨", [Color(hex: "#A855F7"), Color(hex: "#7C3AED")])
                }
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(GroupActiveTheme.card)
        .overlay(
            RoundedRectangle(cornerRadius: 20)
                .stroke(GroupActiveTheme.border)
                .overlay(alignment: .leading) {
                    Rectangle()
                        .fill(GroupActiveTheme.brand)
                        .frame(width: 4)
                        .clipShape(RoundedRectangle(cornerRadius: 2))
                }
        )
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }

    // MARK: - Polls

    private var pollsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            sectionHeader("Polls  🗳️", action: { pollsListOpen = true })
            if listPolls.isEmpty {
                GroupEmptySection(message: "No polls yet", detail: "Create a poll from Quick Add to decide together.")
            } else {
                ForEach(listPolls.prefix(2)) { poll in
                    Button {
                        if let id = poll.pollId { selectedPollId = id }
                    } label: {
                        pollPreviewCard(poll)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private func pollPreviewCard(_ poll: APIClient.GroupPollItemPayload) -> some View {
        let options = Array((poll.options ?? []).prefix(2))
        let total = max(poll.totalVotes ?? options.reduce(0) { $0 + ($1.voteCount ?? 0) }, 1)
        return VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(poll.question ?? "Poll")
                        .font(.plusJakarta(size: 14, weight: .bold))
                        .foregroundStyle(GroupActiveTheme.text)
                    Text(formatPollClosesMeta(closesAt: poll.closesAt, totalVotes: poll.totalVotes))
                        .font(.plusJakarta(size: 11))
                        .foregroundStyle(GroupActiveTheme.secondary)
                }
                Spacer(minLength: 8)
                Text((poll.status ?? "OPEN").uppercased())
                    .font(.plusJakarta(size: 10, weight: .bold))
                    .foregroundStyle(GroupActiveTheme.brand)
                    .padding(.horizontal, 6)
                    .padding(.vertical, 2)
                    .background(GroupActiveTheme.brand.opacity(0.1))
                    .clipShape(RoundedRectangle(cornerRadius: 4))
            }
            VStack(alignment: .leading, spacing: 10) {
                ForEach(Array(options.enumerated()), id: \.offset) { index, option in
                    let votes = option.voteCount ?? 0
                    let fraction = CGFloat(votes) / CGFloat(total)
                    VStack(alignment: .leading, spacing: 6) {
                        HStack {
                            Text(option.text ?? "Option")
                                .font(.plusJakarta(size: 13, weight: .semibold))
                                .foregroundStyle(GroupActiveTheme.text)
                            Spacer()
                            Text("\(votes) votes")
                                .font(.plusJakarta(size: 12))
                                .foregroundStyle(GroupActiveTheme.secondary)
                        }
                        GeometryReader { geo in
                            ZStack(alignment: .leading) {
                                Capsule().fill(Color(hex: "#252332"))
                                Capsule()
                                    .fill(index == 0 ? GroupActiveTheme.brand : Color(hex: "#E88A4F"))
                                    .frame(width: max(6, geo.size.width * fraction))
                            }
                        }
                        .frame(height: 6)
                    }
                }
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(GroupActiveTheme.card)
        .overlay(alignment: .leading) {
            Rectangle().fill(GroupActiveTheme.brand).frame(width: 4)
        }
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(GroupActiveTheme.border))
    }

    // MARK: - Itinerary

    private var itinerarySection: some View {
        let groups = itineraryDayGroups(planningItems)
        return VStack(alignment: .leading, spacing: 12) {
            sectionHeader("Itinerary", action: { scheduleOpen = true })
            if groups.isEmpty {
                GroupEmptySection(message: "No itinerary days yet", detail: "Add a planning item from Quick Add — nothing is invented.")
            } else {
                ForEach(Array(groups.enumerated()), id: \.offset) { index, group in
                    let accent = itineraryAccents[index % itineraryAccents.count]
                    let first = group.items.first
                    VStack(alignment: .leading, spacing: 8) {
                        HStack(spacing: 8) {
                            Text(formatItineraryDayLabel(dayIndex: index + 1, date: group.day))
                                .font(.plusJakarta(size: 12, weight: .bold))
                                .foregroundStyle(GroupActiveTheme.brand)
                            Text(["☀️", "🏛️", "🌅"][index % 3])
                                .font(.system(size: 12))
                        }
                        Text(first?.title ?? "Plan")
                            .font(.plusJakarta(size: 15, weight: .semibold))
                            .foregroundStyle(GroupActiveTheme.text)
                        Text(formatPlanningTime(first?.dueAt) ?? "All day")
                            .font(.plusJakarta(size: 12))
                            .foregroundStyle(GroupActiveTheme.secondary)
                    }
                    .padding(16)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(accent.opacity(0.08))
                    .overlay(alignment: .leading) {
                        Rectangle().fill(accent.opacity(0.6)).frame(width: 3)
                    }
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                }
            }
        }
    }

    // MARK: - Updates

    private var updatesSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            sectionHeader("Updates / Feed  📱")
            if updates.isEmpty {
                GroupEmptySection(message: "No updates yet", detail: "Share a status update from Quick Add.")
            } else {
                ForEach(Array(updates.prefix(3).enumerated()), id: \.offset) { index, item in
                    let name = item.authorDisplayName ?? "Member"
                    HStack(alignment: .top, spacing: 12) {
                        Text(initialsFromName(name))
                            .font(.plusJakarta(size: 14, weight: .bold))
                            .foregroundStyle(Color(hex: "#14121B"))
                            .frame(width: 40, height: 40)
                            .background(avatarColors[index % avatarColors.count])
                            .clipShape(Circle())
                        VStack(alignment: .leading, spacing: 2) {
                            HStack {
                                Text(name)
                                    .font(.plusJakarta(size: 14, weight: .bold))
                                    .foregroundStyle(GroupActiveTheme.text)
                                Spacer()
                                Text(formatRelativeShort(item.createdAt))
                                    .font(.plusJakarta(size: 11))
                                    .foregroundStyle(GroupActiveTheme.secondary)
                            }
                            Text(item.message ?? "")
                                .font(.plusJakarta(size: 13))
                                .foregroundStyle(GroupActiveTheme.text)
                                .fixedSize(horizontal: false, vertical: true)
                        }
                    }
                    .padding(16)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(GroupActiveTheme.card)
                    .overlay(alignment: .leading) {
                        Rectangle().fill(GroupActiveTheme.brand).frame(width: 4)
                    }
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                    .overlay(RoundedRectangle(cornerRadius: 16).stroke(GroupActiveTheme.border))
                }
            }
        }
    }

    // MARK: - Gallery

    private var gallerySection: some View {
        VStack(alignment: .leading, spacing: 12) {
            sectionHeader("Shared Gallery  📸")
            MemoryPhotoGalleryStrip(
                items: listMemoryItems,
                emptyMessage: "Gallery empty",
                emptyDetail: "Add a memory with a photo from Quick Add.",
                text: GroupActiveTheme.text,
                muted: GroupActiveTheme.secondary,
                field: GroupActiveTheme.card,
                border: GroupActiveTheme.border,
                showMediaCountBadge: true
            )
        }
    }

    // MARK: - Bookings

    private var bookingsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            sectionHeader("Bookings  🛎️")
            if bookings.isEmpty {
                GroupEmptySection(message: "No bookings yet", detail: "Add a booking from Quick Add when ready.")
            } else {
                ForEach(Array(bookings.prefix(4).enumerated()), id: \.offset) { _, item in
                    bookingCard(item)
                }
            }
        }
    }

    private func bookingCard(_ item: APIClient.GroupLifePayload.LifeInner.BookingItem) -> some View {
        let status = (item.status ?? "PLANNED").uppercased()
        let confirmed = status == "CONFIRMED" || status == "BOOKED" || status == "COMPLETED"
        let typeLabel = (item.bookingType ?? "Booking")
            .replacingOccurrences(of: "_", with: " ")
            .capitalized
        let day = formatBookingDay(item.startAt ?? item.bookedAt)
        let meta = [typeLabel, day].compactMap { $0 }.joined(separator: " · ")
        let when = formatBookingDayTime(item.startAt) ?? formatBookingDay(item.bookedAt) ?? "—"

        return VStack(alignment: .leading, spacing: 12) {
            HStack {
                HStack(spacing: 12) {
                    Text(confirmed ? "🏨" : "🎟️")
                        .frame(width: 40, height: 40)
                        .background(GroupActiveTheme.card)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                    VStack(alignment: .leading, spacing: 2) {
                        Text(item.title ?? item.bookingId ?? "Booking")
                            .font(.plusJakarta(size: 14, weight: .bold))
                            .foregroundStyle(GroupActiveTheme.text)
                        Text(meta)
                            .font(.plusJakarta(size: 11))
                            .foregroundStyle(GroupActiveTheme.secondary)
                    }
                }
                Spacer()
                Text(status)
                    .font(.plusJakarta(size: 10, weight: .bold))
                    .foregroundStyle(confirmed ? Color(hex: "#22C55E") : GroupActiveTheme.brand)
                    .padding(.horizontal, 6)
                    .padding(.vertical, 2)
                    .background((confirmed ? Color(hex: "#22C55E") : GroupActiveTheme.brand).opacity(0.1))
                    .clipShape(RoundedRectangle(cornerRadius: 4))
            }
            VStack(alignment: .leading, spacing: 2) {
                Text(confirmed ? "Check-in" : "Start time")
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(GroupActiveTheme.secondary)
                Text(when)
                    .font(.plusJakarta(size: 13, weight: .semibold))
                    .foregroundStyle(GroupActiveTheme.text)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(GroupActiveTheme.card)
        .overlay(alignment: .leading) {
            Rectangle().fill(GroupActiveTheme.brand).frame(width: 4)
        }
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(GroupActiveTheme.border))
    }

    // MARK: - Upcoming

    private var upcomingSection: some View {
        let events = upcomingEvents()
        return VStack(alignment: .leading, spacing: 12) {
            sectionHeader("Upcoming Events  🗓")
            if events.isEmpty {
                GroupEmptySection(message: "Nothing upcoming", detail: "Near-term bookings and plans will show here.")
            } else {
                ForEach(Array(events.enumerated()), id: \.offset) { index, event in
                    HStack(alignment: .top, spacing: 12) {
                        Text(event.glyph)
                            .frame(width: 40, height: 40)
                            .background(GroupActiveTheme.card)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                        VStack(alignment: .leading, spacing: 4) {
                            HStack {
                                Text(event.title)
                                    .font(.plusJakarta(size: 14, weight: .bold))
                                    .foregroundStyle(GroupActiveTheme.text)
                                Spacer()
                                if let badge = event.badge {
                                    Text(badge)
                                        .font(.plusJakarta(size: 10, weight: .bold))
                                        .foregroundStyle(GroupActiveTheme.brand)
                                        .padding(.horizontal, 6)
                                        .padding(.vertical, 2)
                                        .background(GroupActiveTheme.brand.opacity(0.1))
                                        .clipShape(RoundedRectangle(cornerRadius: 4))
                                }
                            }
                            Text(event.detail)
                                .font(.plusJakarta(size: 12))
                                .foregroundStyle(GroupActiveTheme.secondary)
                        }
                    }
                    .padding(16)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(index == 0 ? Color(hex: "#14E85940").opacity(0.08) : Color(hex: "#10E88A4F"))
                    .overlay(
                        RoundedRectangle(cornerRadius: 16)
                            .stroke(index == 0 ? Color(hex: "#4DE85940") : Color(hex: "#33E88A4F"))
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                }
            }
        }
    }

    private struct UpcomingEvent {
        let title: String
        let detail: String
        let badge: String?
        let glyph: String
    }

    private func upcomingEvents() -> [UpcomingEvent] {
        var out: [UpcomingEvent] = []
        let now = Date()
        let cal = Calendar.current
        let tomorrow = cal.date(byAdding: .day, value: 1, to: cal.startOfDay(for: now))!

        for booking in bookings.prefix(6) {
            guard let start = parsePlanningInstant(booking.startAt ?? booking.bookedAt), start >= now else { continue }
            let dayStart = cal.startOfDay(for: start)
            let badge: String? = dayStart == tomorrow ? "TOMORROW" : (dayStart == cal.startOfDay(for: now) ? "TODAY" : nil)
            let type = (booking.bookingType ?? "Booking").replacingOccurrences(of: "_", with: " ").capitalized
            out.append(UpcomingEvent(
                title: booking.title ?? "Booking",
                detail: "\(type) · \(formatBookingDay(booking.startAt ?? booking.bookedAt) ?? "")",
                badge: badge,
                glyph: "🏠"
            ))
            if out.count >= 2 { break }
        }

        if out.count < 2 {
            for plan in recentOpenPlanningItems(planningItems, limit: 8) {
                guard let due = parsePlanningInstant(plan.dueAt), due >= now else { continue }
                let dayStart = cal.startOfDay(for: due)
                let badge: String? = dayStart == tomorrow ? "TOMORROW" : (dayStart == cal.startOfDay(for: now) ? "TODAY" : nil)
                out.append(UpcomingEvent(
                    title: plan.title ?? "Plan",
                    detail: "Plan · \(formatBookingDay(plan.dueAt) ?? "")",
                    badge: badge,
                    glyph: "📅"
                ))
                if out.count >= 2 { break }
            }
        }

        let currency = finance?.totals?.first?.currencyCode ?? "INR"
        let budget = GroupFinanceFormat.parseAmount(finance?.totals?.first?.budgetTotal)
        let spent = GroupFinanceFormat.parseAmount(finance?.totals?.first?.expenseTotal)
        let remaining = budget - spent
        if remaining > 0, budget > 0, out.count < 3 {
            out.append(UpcomingEvent(
                title: "Budget remaining",
                detail: GroupFinanceFormat.formatMoney(
                    (remaining as NSDecimalNumber).stringValue,
                    currencyCode: currency
                ),
                badge: nil,
                glyph: "📅"
            ))
        }
        return out
    }

    // MARK: - Expenses

    private var expensesSection: some View {
        let currency = finance?.totals?.first?.currencyCode ?? "INR"
        let spent = finance?.totals?.first?.expenseTotal
        let people = max(pulse?.payload?.participantCount ?? 0, 1)
        let spentDecimal = GroupFinanceFormat.parseAmount(spent)
        let perPerson: String = {
            guard spentDecimal > 0 else { return "—" }
            let share = spentDecimal / Decimal(people)
            return GroupFinanceFormat.formatMoney(
                (share as NSDecimalNumber).stringValue,
                currencyCode: currency
            )
        }()

        return VStack(alignment: .leading, spacing: 12) {
            sectionHeader("Expenses & Budget  💸")
            VStack(alignment: .leading, spacing: 12) {
                HStack(spacing: 12) {
                    expenseSummaryTile("Total spent", GroupFinanceFormat.formatMoney(spent, currencyCode: currency))
                    expenseSummaryTile("Per-person split", perPerson)
                }
                if listExpenses.isEmpty {
                    GroupEmptySection(message: "No expenses yet", detail: "Add a group expense from Quick Add.")
                } else {
                    Text("RECENT EXPENSES")
                        .font(.plusJakarta(size: 11, weight: .bold))
                        .foregroundStyle(GroupActiveTheme.secondary)
                    ForEach(listExpenses.prefix(3)) { expense in
                        HStack(spacing: 12) {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(expense.description ?? "Expense")
                                    .font(.plusJakarta(size: 13, weight: .semibold))
                                    .foregroundStyle(GroupActiveTheme.text)
                                Text((expense.categoryCode ?? "General").replacingOccurrences(of: "_", with: " ").capitalized)
                                    .font(.plusJakarta(size: 11))
                                    .foregroundStyle(GroupActiveTheme.secondary)
                            }
                            Spacer()
                            Text(GroupFinanceFormat.formatMoney(expense.amount, currencyCode: expense.currencyCode ?? currency))
                                .font(.plusJakarta(size: 13, weight: .bold))
                                .foregroundStyle(GroupActiveTheme.text)
                            Text(expense.paidByDisplayName ?? "—")
                                .font(.plusJakarta(size: 11))
                                .foregroundStyle(GroupActiveTheme.secondary)
                                .frame(width: 56, alignment: .trailing)
                        }
                    }
                }
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(GroupActiveTheme.card)
            .overlay(alignment: .leading) {
                Rectangle().fill(GroupActiveTheme.brand).frame(width: 4)
            }
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(GroupActiveTheme.border))
        }
    }

    private func expenseSummaryTile(_ label: String, _ value: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.plusJakarta(size: 11, weight: .semibold))
                .foregroundStyle(GroupActiveTheme.secondary)
            Text(value)
                .font(.plusJakarta(size: 18, weight: .bold))
                .foregroundStyle(GroupActiveTheme.text)
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(GroupActiveTheme.card)
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(GroupActiveTheme.border))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    // MARK: - Chrome

    private func sectionHeader(_ title: String, action: (() -> Void)? = nil) -> some View {
        HStack {
            Text(title)
                .font(.plusJakarta(size: 17, weight: .semibold))
                .foregroundStyle(GroupActiveTheme.text)
            Spacer()
            if let action {
                Button("View all", action: action)
                    .font(.plusJakarta(size: 10, weight: .semibold))
                    .foregroundStyle(GroupActiveTheme.brand)
                    .buttonStyle(.plain)
            } else {
                Text("View all")
                    .font(.plusJakarta(size: 10, weight: .semibold))
                    .foregroundStyle(GroupActiveTheme.brand.opacity(0.45))
            }
        }
    }

    private var momentsQuickAddCta: some View {
        VStack(spacing: 16) {
            VStack(spacing: 4) {
                Text("Create the next shared moment")
                    .font(.plusJakarta(size: 18, weight: .bold))
                    .foregroundStyle(GroupActiveTheme.text)
                Text("Add a plan, expense, memory, poll or update.")
                    .font(.plusJakarta(size: 13))
                    .foregroundStyle(GroupActiveTheme.secondary)
            }
            Button(action: onCreateMoment) {
                HStack(spacing: 10) {
                    Text("+")
                    Text("Open Quick Add")
                }
                .font(.plusJakarta(size: 14, weight: .bold))
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(
                    LinearGradient(
                        colors: [GroupActiveTheme.brand, GroupActiveTheme.accentOrange],
                        startPoint: .leading,
                        endPoint: .trailing
                    )
                )
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            .buttonStyle(.plain)
        }
        .padding(20)
        .frame(maxWidth: .infinity)
        .background(
            LinearGradient(
                colors: [GroupActiveTheme.brand, GroupActiveTheme.accentOrange],
                startPoint: .leading,
                endPoint: .trailing
            )
        )
        .overlay(alignment: .leading) {
            Rectangle().fill(GroupActiveTheme.border).frame(width: 4)
        }
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }

    private func momentsMetric(_ label: String, _ value: String, _ glyph: String, _ colors: [Color]) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 8) {
                Text(glyph)
                    .font(.system(size: 14))
                    .frame(width: 32, height: 32)
                    .background(Color.white.opacity(0.1))
                    .clipShape(RoundedRectangle(cornerRadius: 10))
                Text(label)
                    .font(.plusJakarta(size: 10, weight: .semibold))
                    .foregroundStyle(GroupActiveTheme.secondary)
            }
            Text(value)
                .font(.plusJakarta(size: 22, weight: .bold))
                .foregroundStyle(GroupActiveTheme.text)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(LinearGradient(colors: colors, startPoint: .leading, endPoint: .trailing))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(GroupActiveTheme.border))
        .clipShape(RoundedRectangle(cornerRadius: 16))
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
            async let memoriesResult = APIClient.shared.listGroupMemories(momentId: momentId)
            async let expensesResult = APIClient.shared.listGroupExpenses(momentId: momentId, limit: 10)
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
            listExpenses = (try? await expensesResult)?.items ?? []
            if let listed = try? await memoriesResult {
                listMemoryItems = listed.items
                memoryCount = listed.memoryCount ?? listed.items.count
            } else if let cached = GroupTabDataCache.peekMemory(momentId)?.memory?.payload?.items {
                listMemoryItems = cached
                memoryCount = cached.count
            } else if let facet = try? await APIClient.shared.getGroupMemory(momentId: momentId) {
                listMemoryItems = facet.payload?.items ?? []
                memoryCount = facet.payload?.memoryCount ?? listMemoryItems.count
            } else {
                listMemoryItems = []
                memoryCount = 0
            }
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
