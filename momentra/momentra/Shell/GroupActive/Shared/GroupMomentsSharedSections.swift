import SwiftUI

/// Theme tokens for Moments shared sections (Trip / Wedding / Experience / Purchase / Living).
struct MomentsChrome {
    var bg: Color
    var text: Color
    var secondary: Color
    var card: Color
    var border: Color
    var accent: Color
    var accentAlt: Color
    var darkText: Color
    var brandSoft: Color

    static let trip = MomentsChrome(
        bg: GroupActiveTheme.bg,
        text: GroupActiveTheme.text,
        secondary: GroupActiveTheme.secondary,
        card: GroupActiveTheme.card,
        border: GroupActiveTheme.border,
        accent: GroupActiveTheme.brand,
        accentAlt: GroupActiveTheme.accentOrange,
        darkText: Color(hex: "#14121B"),
        brandSoft: GroupActiveTheme.brand.opacity(0.1)
    )

    static let wedding = MomentsChrome(
        bg: WeddingActiveTheme.bg,
        text: WeddingActiveTheme.text,
        secondary: WeddingActiveTheme.secondary,
        card: WeddingActiveTheme.card,
        border: WeddingActiveTheme.border,
        accent: WeddingActiveTheme.accent,
        accentAlt: WeddingActiveTheme.accentLight,
        darkText: WeddingActiveTheme.darkText,
        brandSoft: WeddingActiveTheme.accentSoft
    )

    static func experience(_ theme: ExperienceActiveTheme) -> MomentsChrome {
        MomentsChrome(
            bg: theme.bg,
            text: theme.text,
            secondary: theme.secondary,
            card: theme.card,
            border: theme.border,
            accent: theme.accent,
            accentAlt: theme.accentLight,
            darkText: theme.darkText,
            brandSoft: theme.accentSoft
        )
    }

    static func purchase(_ theme: PurchaseActiveTheme) -> MomentsChrome {
        MomentsChrome(
            bg: theme.bg,
            text: theme.text,
            secondary: theme.secondary,
            card: theme.card,
            border: theme.border,
            accent: theme.accent,
            accentAlt: theme.accentLight,
            darkText: theme.darkText,
            brandSoft: theme.accentSoft
        )
    }

    static func living(_ theme: LivingActiveTheme) -> MomentsChrome {
        MomentsChrome(
            bg: theme.bg,
            text: theme.text,
            secondary: theme.secondary,
            card: theme.card,
            border: theme.border,
            accent: theme.accent,
            accentAlt: theme.accentLight,
            darkText: theme.darkText,
            brandSoft: theme.accentSoft
        )
    }
}

private let momentsAvatarColors: [Color] = [
    Color(hex: "#FDBA74"), Color(hex: "#86EFAC"), Color(hex: "#93C5FD"), Color(hex: "#C4B5FD"),
]
private let momentsItineraryAccents: [Color] = [
    Color(hex: "#14B8A6"), Color(hex: "#E88A4F"), Color(hex: "#A855F7"),
]

// MARK: - Section chrome

struct MomentsSectionHeader: View {
    let title: String
    var chrome: MomentsChrome
    var onViewAll: (() -> Void)? = nil

    var body: some View {
        HStack {
            Text(title)
                .font(.plusJakarta(size: 17, weight: .semibold))
                .foregroundStyle(chrome.text)
            Spacer()
            if let onViewAll {
                Button("View all", action: onViewAll)
                    .font(.plusJakarta(size: 10, weight: .semibold))
                    .foregroundStyle(chrome.accent)
                    .buttonStyle(.plain)
            } else {
                Text("View all")
                    .font(.plusJakarta(size: 10, weight: .semibold))
                    .foregroundStyle(chrome.accent.opacity(0.45))
            }
        }
    }
}

struct MomentsHeroHeader: View {
    let eyebrow: String
    let title: String
    let status: String
    let stats: [(label: String, value: String, colors: [Color])]
    var chrome: MomentsChrome

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(eyebrow)
                        .font(.plusJakarta(size: 11, weight: .semibold))
                        .foregroundStyle(chrome.secondary)
                        .tracking(0.6)
                    Text(title)
                        .font(.plusJakarta(size: 24, weight: .bold))
                        .foregroundStyle(chrome.text)
                }
                Spacer(minLength: 8)
                Text(status.uppercased())
                    .font(.plusJakarta(size: 10, weight: .bold))
                    .foregroundStyle(chrome.darkText)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(chrome.accent)
                    .clipShape(RoundedRectangle(cornerRadius: 8))
            }
            let rows = stride(from: 0, to: stats.count, by: 2).map { i in
                Array(stats[i..<min(i + 2, stats.count)])
            }
            VStack(spacing: 12) {
                ForEach(Array(rows.enumerated()), id: \.offset) { _, row in
                    HStack(spacing: 12) {
                        ForEach(Array(row.enumerated()), id: \.offset) { _, stat in
                            MomentsStatCard(label: stat.label, value: stat.value, colors: stat.colors, chrome: chrome)
                        }
                        if row.count == 1 { Spacer().frame(maxWidth: .infinity) }
                    }
                }
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(chrome.card)
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(chrome.border))
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }
}

struct MomentsStatCard: View {
    let label: String
    let value: String
    let colors: [Color]
    var chrome: MomentsChrome

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(label)
                .font(.plusJakarta(size: 10, weight: .semibold))
                .foregroundStyle(chrome.secondary)
            Text(value)
                .font(.plusJakarta(size: 22, weight: .bold))
                .foregroundStyle(chrome.text)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(LinearGradient(colors: colors, startPoint: .leading, endPoint: .trailing))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(chrome.border))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}

// MARK: - Polls / Updates / Itinerary

struct MomentsPollPreviewCard: View {
    let poll: APIClient.GroupPollItemPayload
    var chrome: MomentsChrome
    var onTap: () -> Void

    var body: some View {
        let options = Array((poll.options ?? []).prefix(2))
        let total = max(poll.totalVotes ?? options.reduce(0) { $0 + ($1.voteCount ?? 0) }, 1)
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 12) {
                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(poll.question ?? "Poll")
                            .font(.plusJakarta(size: 14, weight: .bold))
                            .foregroundStyle(chrome.text)
                        Text(formatPollClosesMeta(closesAt: poll.closesAt, totalVotes: poll.totalVotes))
                            .font(.plusJakarta(size: 11))
                            .foregroundStyle(chrome.secondary)
                    }
                    Spacer(minLength: 8)
                    Text((poll.status ?? "OPEN").uppercased())
                        .font(.plusJakarta(size: 10, weight: .bold))
                        .foregroundStyle(chrome.accent)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(chrome.brandSoft)
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
                                    .foregroundStyle(chrome.text)
                                Spacer()
                                Text("\(votes) votes")
                                    .font(.plusJakarta(size: 12))
                                    .foregroundStyle(chrome.secondary)
                            }
                            GeometryReader { geo in
                                ZStack(alignment: .leading) {
                                    Capsule().fill(Color(hex: "#252332"))
                                    Capsule()
                                        .fill(index == 0 ? chrome.accent : chrome.accentAlt)
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
            .background(chrome.card)
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(chrome.border))
        }
        .buttonStyle(.plain)
    }
}

struct MomentsUpdateFeedRow: View {
    let item: GroupUpdateItem
    let index: Int
    var chrome: MomentsChrome

    var body: some View {
        let name = item.authorDisplayName ?? "Member"
        HStack(alignment: .top, spacing: 12) {
            Text(initialsFromName(name))
                .font(.plusJakarta(size: 14, weight: .bold))
                .foregroundStyle(chrome.darkText)
                .frame(width: 40, height: 40)
                .background(momentsAvatarColors[index % momentsAvatarColors.count])
                .clipShape(Circle())
            VStack(alignment: .leading, spacing: 2) {
                HStack {
                    Text(name)
                        .font(.plusJakarta(size: 14, weight: .bold))
                        .foregroundStyle(chrome.text)
                    Spacer()
                    Text(formatRelativeShort(item.createdAt))
                        .font(.plusJakarta(size: 11))
                        .foregroundStyle(chrome.secondary)
                }
                Text(item.message ?? "")
                    .font(.plusJakarta(size: 13))
                    .foregroundStyle(chrome.text)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(chrome.card)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(chrome.border))
    }
}

struct MomentsItineraryDayCard: View {
    let dayIndex: Int
    let day: Date
    let title: String
    let timeLabel: String
    var chrome: MomentsChrome

    var body: some View {
        let accent = momentsItineraryAccents[(dayIndex - 1) % momentsItineraryAccents.count]
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 8) {
                Text(formatItineraryDayLabel(dayIndex: dayIndex, date: day))
                    .font(.plusJakarta(size: 12, weight: .bold))
                    .foregroundStyle(chrome.accent)
                Text(["☀️", "🏛️", "🌅"][(dayIndex - 1) % 3])
                    .font(.system(size: 12))
            }
            Text(title)
                .font(.plusJakarta(size: 15, weight: .semibold))
                .foregroundStyle(chrome.text)
            Text(timeLabel)
                .font(.plusJakarta(size: 12))
                .foregroundStyle(chrome.secondary)
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

// MARK: - Expenses / Upcoming / CTA

struct MomentsExpensesCard: View {
    let spent: String?
    let currency: String
    let peopleCount: Int
    let expenses: [APIClient.GroupExpenseListItemPayload]
    var chrome: MomentsChrome

    var body: some View {
        let people = max(peopleCount, 1)
        let spentDecimal = GroupFinanceFormat.parseAmount(spent)
        let perPerson: String = {
            guard spentDecimal > 0 else { return "—" }
            let share = spentDecimal / Decimal(people)
            return GroupFinanceFormat.formatMoney((share as NSDecimalNumber).stringValue, currencyCode: currency)
        }()
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 12) {
                expenseTile("Total spent", GroupFinanceFormat.formatMoney(spent, currencyCode: currency))
                expenseTile("Per-person split", perPerson)
            }
            if expenses.isEmpty {
                GroupEmptySection(message: "No expenses yet", detail: "Add a group expense from Quick Add.")
            } else {
                Text("RECENT EXPENSES")
                    .font(.plusJakarta(size: 11, weight: .bold))
                    .foregroundStyle(chrome.secondary)
                ForEach(expenses.prefix(3)) { expense in
                    HStack(spacing: 12) {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(expense.description ?? "Expense")
                                .font(.plusJakarta(size: 13, weight: .semibold))
                                .foregroundStyle(chrome.text)
                            Text((expense.categoryCode ?? "General").replacingOccurrences(of: "_", with: " ").capitalized)
                                .font(.plusJakarta(size: 11))
                                .foregroundStyle(chrome.secondary)
                        }
                        Spacer()
                        Text(GroupFinanceFormat.formatMoney(expense.amount, currencyCode: expense.currencyCode ?? currency))
                            .font(.plusJakarta(size: 13, weight: .bold))
                            .foregroundStyle(chrome.text)
                        Text(expense.paidByDisplayName ?? "—")
                            .font(.plusJakarta(size: 11))
                            .foregroundStyle(chrome.secondary)
                            .frame(width: 56, alignment: .trailing)
                    }
                }
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(chrome.card)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(chrome.border))
    }

    private func expenseTile(_ label: String, _ value: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.plusJakarta(size: 11, weight: .semibold))
                .foregroundStyle(chrome.secondary)
            Text(value)
                .font(.plusJakarta(size: 18, weight: .bold))
                .foregroundStyle(chrome.text)
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(chrome.card)
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(chrome.border))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

struct MomentsUpcomingEvent: Identifiable {
    var id: String { "\(title)-\(detail)" }
    let title: String
    let detail: String
    let badge: String?
    let glyph: String
}

func momentsUpcomingFromPlanning(
    planning: [GroupPlanningItem],
    bookings: [APIClient.GroupLifePayload.LifeInner.BookingItem] = [],
    finance: APIClient.GroupFinancePayload? = nil,
    limit: Int = 3
) -> [MomentsUpcomingEvent] {
    var out: [MomentsUpcomingEvent] = []
    let now = Date()
    let cal = Calendar.current
    let tomorrow = cal.date(byAdding: .day, value: 1, to: cal.startOfDay(for: now))!

    for booking in bookings.prefix(6) {
        guard let start = parsePlanningInstant(booking.startAt ?? booking.bookedAt), start >= now else { continue }
        let dayStart = cal.startOfDay(for: start)
        let badge: String? = dayStart == tomorrow ? "TOMORROW" : (dayStart == cal.startOfDay(for: now) ? "TODAY" : nil)
        let type = (booking.bookingType ?? "Booking").replacingOccurrences(of: "_", with: " ").capitalized
        out.append(MomentsUpcomingEvent(
            title: booking.title ?? "Booking",
            detail: "\(type) · \(formatBookingDay(booking.startAt ?? booking.bookedAt) ?? "")",
            badge: badge,
            glyph: "🏠"
        ))
        if out.count >= 2 { break }
    }
    if out.count < 2 {
        for plan in recentOpenPlanningItems(planning, limit: 8) {
            guard let due = parsePlanningInstant(plan.dueAt), due >= now else { continue }
            let dayStart = cal.startOfDay(for: due)
            let badge: String? = dayStart == tomorrow ? "TOMORROW" : (dayStart == cal.startOfDay(for: now) ? "TODAY" : nil)
            out.append(MomentsUpcomingEvent(
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
    if remaining > 0, budget > 0, out.count < limit {
        out.append(MomentsUpcomingEvent(
            title: "Budget remaining",
            detail: GroupFinanceFormat.formatMoney((remaining as NSDecimalNumber).stringValue, currencyCode: currency),
            badge: nil,
            glyph: "📅"
        ))
    }
    return Array(out.prefix(limit))
}

struct MomentsUpcomingEventCard: View {
    let event: MomentsUpcomingEvent
    var highlight: Bool
    var chrome: MomentsChrome

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Text(event.glyph)
                .frame(width: 40, height: 40)
                .background(chrome.card)
                .clipShape(RoundedRectangle(cornerRadius: 12))
            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text(event.title)
                        .font(.plusJakarta(size: 14, weight: .bold))
                        .foregroundStyle(chrome.text)
                    Spacer()
                    if let badge = event.badge {
                        Text(badge)
                            .font(.plusJakarta(size: 10, weight: .bold))
                            .foregroundStyle(chrome.accent)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(chrome.brandSoft)
                            .clipShape(RoundedRectangle(cornerRadius: 4))
                    }
                }
                Text(event.detail)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(chrome.secondary)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(highlight ? Color(hex: "#14E85940").opacity(0.08) : Color(hex: "#10E88A4F"))
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(highlight ? Color(hex: "#4DE85940") : Color(hex: "#33E88A4F"))
        )
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}

struct MomentsQuickAddCta: View {
    var title: String = "Create the next shared moment"
    var subtitle: String = "Add a plan, expense, memory, poll or update."
    var chrome: MomentsChrome
    var onTap: () -> Void

    var body: some View {
        VStack(spacing: 16) {
            VStack(spacing: 4) {
                Text(title)
                    .font(.plusJakarta(size: 18, weight: .bold))
                    .foregroundStyle(.white)
                Text(subtitle)
                    .font(.plusJakarta(size: 13))
                    .foregroundStyle(.white.opacity(0.9))
            }
            Button(action: onTap) {
                Text("+ Open Quick Add")
                    .font(.plusJakarta(size: 14, weight: .bold))
                    .foregroundStyle(chrome.darkText)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(Color.white)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            .buttonStyle(.plain)
        }
        .padding(20)
        .frame(maxWidth: .infinity)
        .background(LinearGradient(colors: [chrome.accent, chrome.accentAlt], startPoint: .leading, endPoint: .trailing))
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }
}

struct MomentsSimpleRowCard: View {
    let title: String
    var meta: String? = nil
    var status: String? = nil
    var statusColor: Color? = nil
    var chrome: MomentsChrome

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.plusJakarta(size: 14, weight: .bold))
                    .foregroundStyle(chrome.text)
                if let meta, !meta.isEmpty {
                    Text(meta)
                        .font(.plusJakarta(size: 11))
                        .foregroundStyle(chrome.secondary)
                }
            }
            Spacer()
            if let status {
                Text(status.uppercased())
                    .font(.plusJakarta(size: 10, weight: .bold))
                    .foregroundStyle(statusColor ?? chrome.accent)
                    .padding(.horizontal, 6)
                    .padding(.vertical, 2)
                    .background((statusColor ?? chrome.accent).opacity(0.12))
                    .clipShape(RoundedRectangle(cornerRadius: 4))
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(chrome.card)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(chrome.border))
    }
}

/// Figma 575:14327 — booking card in Moments / Experience tabs.
struct MomentsBookingCard: View {
    let booking: APIClient.GroupLifePayload.LifeInner.BookingItem
    var chrome: MomentsChrome

    var body: some View {
        let status = (booking.status ?? "PLANNED").uppercased()
        let confirmed = status == "CONFIRMED" || status == "BOOKED" || status == "COMPLETED"
        let typeLabel = (booking.bookingType ?? "Booking")
            .replacingOccurrences(of: "_", with: " ")
            .capitalized
        let day = formatBookingDay(booking.startAt ?? booking.bookedAt)
        let meta = [typeLabel, day].compactMap { $0 }.joined(separator: " · ")
        let when = formatBookingDayTime(booking.startAt) ?? formatBookingDay(booking.bookedAt) ?? "—"

        VStack(alignment: .leading, spacing: 12) {
            HStack {
                HStack(spacing: 12) {
                    Text(confirmed ? "🏨" : "🎟️")
                        .frame(width: 40, height: 40)
                        .background(chrome.card)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                    VStack(alignment: .leading, spacing: 2) {
                        Text(booking.title ?? booking.bookingId ?? "Booking")
                            .font(.plusJakarta(size: 14, weight: .bold))
                            .foregroundStyle(chrome.text)
                        Text(meta)
                            .font(.plusJakarta(size: 11))
                            .foregroundStyle(chrome.secondary)
                    }
                }
                Spacer()
                Text(status)
                    .font(.plusJakarta(size: 10, weight: .bold))
                    .foregroundStyle(confirmed ? Color(hex: "#22C55E") : chrome.accent)
                    .padding(.horizontal, 6)
                    .padding(.vertical, 2)
                    .background((confirmed ? Color(hex: "#22C55E") : chrome.accent).opacity(0.1))
                    .clipShape(RoundedRectangle(cornerRadius: 4))
            }
            VStack(alignment: .leading, spacing: 2) {
                Text(confirmed ? "Check-in" : "Start time")
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(chrome.secondary)
                Text(when)
                    .font(.plusJakarta(size: 13, weight: .semibold))
                    .foregroundStyle(chrome.text)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(chrome.card)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(chrome.border))
    }
}
