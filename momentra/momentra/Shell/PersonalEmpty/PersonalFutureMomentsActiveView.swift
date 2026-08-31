import SwiftUI

/// Future Building Moments populated body — Figma `505:13146`.
struct PersonalFutureMomentsActiveView: View {
    let refreshToken: UInt64
    let momentId: String?
    let momentTitle: String?
    var onOpenQuickAdd: () -> Void
    var onAddExpense: () -> Void

    @State private var pulse: APIClient.PersonalPulsePayload?
    @State private var activities: [APIClient.ActivityItemPayload] = []
    @State private var loading = true
    @State private var error: String?

    var body: some View {
        Group {
            if loading && pulse == nil {
                ProgressView().tint(Color(hex: "#7C5CFC"))
            } else {
                ZStack(alignment: .bottomTrailing) {
                    ScrollView {
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
                            heroCard
                            journeyTimeline
                            capitalJourney
                            bestAndTurning
                            insightsCard
                            captureCta
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 12)
                        .padding(.bottom, 56)
                    }
                    Button(action: onAddExpense) {
                        Text("₹+")
                            .font(.system(size: 16, weight: .heavy))
                            .foregroundStyle(.white)
                            .frame(width: 52, height: 52)
                            .background(Color(hex: "#7C5CFC"))
                            .clipShape(Circle())
                            .shadow(color: Color(hex: "#7C5CFC").opacity(0.4), radius: 10, y: 4)
                    }
                    .disabled(momentId == nil)
                    .opacity(momentId == nil ? 0.45 : 1)
                    .padding(.trailing, 20)
                    .padding(.bottom, 16)
                }
            }
        }
        .background(Color(hex: "#14121B"))
        .task(id: "\(refreshToken)-\(momentId ?? "")") { await load() }
    }

    // MARK: - Derived

    private var futureScore: String { PersonalLifeOpsDerived.displayScore(pulse?.wellbeingScore) }
    private var stage: String { PersonalLifeOpsDerived.stageBand(wellbeing: pulse?.wellbeingScore) }

    private var spendPairs: [(String, String)] {
        guard let map = pulse?.widgetPayload?["spendByCurrency"]?.value as? [String: Any] else { return [] }
        return map.compactMap { key, value in
            if let s = value as? String { return (key, s) }
            if let d = value as? Double { return (key, String(d)) }
            if let i = value as? Int { return (key, String(i)) }
            if let n = value as? NSNumber { return (key, n.stringValue) }
            return (key, "\(value)")
        }
    }

    private var milestoneCount: Int {
        activities.filter { $0.activityCode.uppercased().contains("MILESTONE") }.count
    }

    private var learningCount: Int {
        activities.filter { $0.activityCode.uppercased().contains("LEARNING") }.count
    }

    private var progressCount: Int {
        activities.filter { $0.activityCode.uppercased().contains("PROGRESS") }.count
    }

    private var spendCompact: String {
        guard let first = spendPairs.first, let n = Double(first.1) else { return "—" }
        let symbol = currencySymbol(first.0)
        if n >= 1000 {
            let k = n / 1000
            let formatted = k >= 10 ? String(format: "%.0fK", k) : String(format: "%.1fK", k)
            return "\(symbol)\(formatted)"
        }
        return "\(symbol)\(Self.moneyFormatter.string(from: NSNumber(value: n)) ?? first.1)"
    }

    private var totalInvestedLabel: String {
        guard !spendPairs.isEmpty else { return "—" }
        return spendPairs.map { "\(currencySymbol($0.0))\(formatMoney($0.1))" }.joined(separator: " · ")
    }

    private var bandBadge: String {
        switch stage {
        case "Thriving": return "Thriving"
        case "Structured": return "Stable and Improving"
        default: return futureScore == "—" ? "Building" : "Stabilizing"
        }
    }

    private var insightLine: String {
        switch stage {
        case "Thriving":
            return "Learning and execution are compounding together."
        case "Structured":
            return "Your future is compounding through learning and execution."
        default:
            return futureScore == "—"
                ? "Log milestones, learning, and progress to reveal your Future Score."
                : "Your future is compounding…"
        }
    }

    private var milestoneStreak: Int {
        PersonalLifeOpsDerived.streakDays(
            from: activities
                .filter { $0.activityCode.uppercased().contains("MILESTONE") }
                .map(\.occurredAt)
        )
    }

    private var learningStreak: Int {
        PersonalLifeOpsDerived.streakDays(
            from: activities
                .filter { $0.activityCode.uppercased().contains("LEARNING") }
                .map(\.occurredAt)
        )
    }

    private var hasExpense: Bool {
        activities.contains { $0.activityCode.uppercased().contains("EXPENSE") } || !spendPairs.isEmpty
    }

    // MARK: - Sections

    private var heroCard: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Future Journey")
                        .font(.system(size: 22, weight: .semibold))
                        .foregroundStyle(Color(hex: "#E5E0EE"))
                    Text(insightLine)
                        .font(.system(size: 13))
                        .foregroundStyle(Color(hex: "#C9C4D8"))
                }
                Spacer(minLength: 8)
                VStack(spacing: 2) {
                    Text(futureScore)
                        .font(.system(size: 28, weight: .heavy))
                        .foregroundStyle(Color(hex: "#E5E0EE"))
                    Text("SCORE")
                        .font(.system(size: 10, weight: .bold))
                        .foregroundStyle(Color(hex: "#C9C4D8"))
                }
                .frame(width: 88, height: 88)
                .background(
                    Circle().stroke(
                        futureScore == "—" ? Color.white.opacity(0.15) : Color(hex: "#10B981"),
                        lineWidth: 6
                    )
                )
            }
            Text(bandBadge)
                .font(.system(size: 12, weight: .bold))
                .foregroundStyle(.white)
                .padding(.horizontal, 14)
                .padding(.vertical, 6)
                .background(
                    LinearGradient(
                        colors: [Color(hex: "#10B981"), Color(hex: "#34D399")],
                        startPoint: .leading,
                        endPoint: .trailing
                    )
                )
                .clipShape(Capsule())
            HStack(spacing: 8) {
                ForEach(["Stabilizing", "Structured", "Thriving"], id: \.self) { band in
                    let active = band == stage && futureScore != "—"
                    let filled = bandRank(band) <= bandRank(stage) && futureScore != "—"
                    VStack(spacing: 6) {
                        Capsule()
                            .fill(filled ? Color(hex: "#7C5CFC") : Color(hex: "#35333E"))
                            .frame(height: 6)
                        Text(band)
                            .font(.system(size: 10, weight: active ? .bold : .medium))
                            .foregroundStyle(Color(hex: "#C9C4D8"))
                    }
                    .frame(maxWidth: .infinity)
                }
            }
            HStack(spacing: 8) {
                miniStat(
                    emoji: "🏆",
                    value: milestoneCount > 0 ? "\(milestoneCount)" : "—",
                    label: "Milestones",
                    tint: Color(hex: "#10B981")
                )
                miniStat(
                    emoji: "📚",
                    value: learningCount > 0 ? "\(learningCount)" : "—",
                    label: "Learning",
                    tint: Color(hex: "#3B82F6")
                )
                miniStat(emoji: "💰", value: spendCompact, label: "Invested", tint: Color(hex: "#06B6D4"))
                miniStat(
                    emoji: "📈",
                    value: progressCount > 0 ? "\(progressCount)" : "—",
                    label: "Progress",
                    tint: Color(hex: "#A855F7")
                )
            }
        }
        .padding(24)
        .background(Color(hex: "#191622"))
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(Color.white.opacity(0.08), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }

    private func miniStat(emoji: String, value: String, label: String, tint: Color) -> some View {
        VStack(spacing: 8) {
            Text(emoji)
                .font(.system(size: 16))
                .frame(width: 32, height: 32)
                .background(Color.white.opacity(0.1))
                .clipShape(RoundedRectangle(cornerRadius: 16))
            Text(value)
                .font(.system(size: 16, weight: .heavy))
                .foregroundStyle(.white)
                .lineLimit(1)
                .minimumScaleFactor(0.7)
            Text(label)
                .font(.system(size: 9, weight: .bold))
                .foregroundStyle(.white)
        }
        .padding(12)
        .frame(maxWidth: .infinity)
        .background(tint)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(tint, lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var journeyTimeline: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Journey Timeline")
                .font(.system(size: 16, weight: .semibold))
                .foregroundStyle(Color(hex: "#E5E0EE"))
            if activities.isEmpty {
                Text("No journey entries yet. Capture a milestone, learning, or progress to start the timeline.")
                    .font(.system(size: 13))
                    .foregroundStyle(Color(hex: "#C9C4D8"))
                    .padding(16)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color.white.opacity(0.05))
                    .overlay(RoundedRectangle(cornerRadius: 20).stroke(Color.white.opacity(0.08), lineWidth: 1))
                    .clipShape(RoundedRectangle(cornerRadius: 20))
            } else {
                ForEach(activities.prefix(8)) { item in
                    journeyRow(item)
                }
            }
        }
    }

    private func journeyRow(_ item: APIClient.ActivityItemPayload) -> some View {
        let meta = activityVisual(item.activityCode)
        return HStack(alignment: .top, spacing: 12) {
            Text(meta.emoji)
                .font(.system(size: 18))
                .frame(width: 40, height: 40)
                .background(meta.color)
                .clipShape(RoundedRectangle(cornerRadius: 20))
            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text(meta.label)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(Color(hex: "#E5E0EE"))
                    Spacer()
                    Text(PersonalLifeOpsDerived.relativeTime(item.occurredAt))
                        .font(.system(size: 10, weight: .heavy))
                        .foregroundStyle(meta.color)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 2)
                        .background(meta.color.opacity(0.12))
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                }
                Text(item.title)
                    .font(.system(size: 12))
                    .foregroundStyle(Color(hex: "#C9C4D8"))
            }
        }
        .padding(16)
        .background(meta.color.opacity(0.1))
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(meta.color.opacity(0.4), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }

    private var capitalJourney: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .firstTextBaseline) {
                Text("Capital Journey")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(Color(hex: "#E5E0EE"))
                Spacer()
                Text("From pulse")
                    .font(.system(size: 11))
                    .foregroundStyle(Color(hex: "#C9C4D8"))
            }
            if spendPairs.isEmpty {
                Text("No capital invested for this moment yet.")
                    .font(.system(size: 13))
                    .foregroundStyle(Color(hex: "#C9C4D8"))
                    .padding(16)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color.white.opacity(0.05))
                    .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08), lineWidth: 1))
                    .clipShape(RoundedRectangle(cornerRadius: 16))
            } else {
                VStack(alignment: .leading, spacing: 8) {
                    Text("Total Invested")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundStyle(Color(hex: "#C9C4D8"))
                    Text(totalInvestedLabel)
                        .font(.system(size: 22, weight: .heavy))
                        .foregroundStyle(Color(hex: "#E5E0EE"))
                    ForEach(spendPairs, id: \.0) { currency, amount in
                        HStack {
                            Text(currency).foregroundStyle(Color(hex: "#E5E0EE"))
                            Spacer()
                            Text(formatMoney(amount))
                                .fontWeight(.semibold)
                                .foregroundStyle(Color(hex: "#E5E0EE"))
                        }
                        .font(.system(size: 13))
                    }
                }
                .padding(16)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color.white.opacity(0.05))
                .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08), lineWidth: 1))
                .clipShape(RoundedRectangle(cornerRadius: 16))
            }
        }
    }

    private var bestAndTurning: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Best Breakthroughs")
                .font(.system(size: 16, weight: .semibold))
                .foregroundStyle(Color(hex: "#E5E0EE"))
            HStack(spacing: 10) {
                bestCard(
                    title: milestoneStreak > 0 ? "Milestone streak" : "Milestones",
                    detail: milestoneStreak > 0 ? "+\(milestoneStreak) streak" : "Building…",
                    accent: Color(hex: "#10B981"),
                    emoji: "⚡"
                )
                bestCard(
                    title: learningStreak > 0 ? "Learning streak" : (hasExpense ? "Capital logged" : "Balance"),
                    detail: learningStreak > 0
                        ? "+\(learningStreak) streak"
                        : (hasExpense ? "Expense presence on this moment" : "Building…"),
                    accent: Color(hex: "#3B82F6"),
                    emoji: "⚖️"
                )
            }
            Text("Turning Points")
                .font(.system(size: 16, weight: .semibold))
                .foregroundStyle(Color(hex: "#E5E0EE"))
                .padding(.top, 4)
            turningRow(
                emoji: "🔥",
                title: "First milestone streak",
                body: milestoneStreak > 0
                    ? "\(milestoneStreak) consecutive milestone day\(milestoneStreak == 1 ? "" : "s")"
                    : "Log milestones on consecutive days to unlock this turning point."
            )
            turningRow(
                emoji: "🔒",
                title: "Capital signal",
                body: hasExpense
                    ? "Spend is flowing into this moment’s capital journey."
                    : "Log an expense to mark your first capital turning point."
            )
        }
    }

    private func bestCard(title: String, detail: String, accent: Color, emoji: String) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(emoji)
                .font(.system(size: 16))
                .frame(width: 32, height: 32)
                .background(accent)
                .clipShape(RoundedRectangle(cornerRadius: 16))
            Text(title)
                .font(.system(size: 13, weight: .bold))
                .foregroundStyle(Color(hex: "#E5E0EE"))
            Text(detail)
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(accent)
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(accent.opacity(0.1))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(accent.opacity(0.5), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func turningRow(emoji: String, title: String, body: String) -> some View {
        HStack(alignment: .top, spacing: 12) {
            Text(emoji).font(.system(size: 18))
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.system(size: 13, weight: .bold))
                    .foregroundStyle(Color(hex: "#E5E0EE"))
                Text(body)
                    .font(.system(size: 12))
                    .foregroundStyle(Color(hex: "#C9C4D8"))
            }
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white.opacity(0.05))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var insightsCard: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 8) {
                Text("AI Insights")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundStyle(Color(hex: "#E5E0EE"))
                Text("Coming Soon")
                    .font(.system(size: 10, weight: .heavy))
                    .foregroundStyle(Color(hex: "#A78BFA"))
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background(Color(hex: "#7C5CFC").opacity(0.2))
                    .clipShape(Capsule())
            }
            Text("Patterns across milestones, learning, progress, and capital will surface here.")
                .font(.system(size: 12))
                .foregroundStyle(Color(hex: "#C9C4D8"))
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white.opacity(0.05))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var captureCta: some View {
        VStack(spacing: 10) {
            Text("Capture a new moment")
                .font(.system(size: 16, weight: .heavy))
                .foregroundStyle(.white)
            Text("Add a milestone, learning, progress, or capital log")
                .font(.system(size: 13))
                .foregroundStyle(.white.opacity(0.85))
            Button(action: onOpenQuickAdd) {
                Text("+ Open Quick Add")
                    .font(.system(size: 14, weight: .heavy))
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .overlay(RoundedRectangle(cornerRadius: 14).stroke(Color.white.opacity(0.55), lineWidth: 1.5))
            }
            .buttonStyle(.plain)
        }
        .padding(20)
        .frame(maxWidth: .infinity)
        .background(
            LinearGradient(colors: [Color(hex: "#7C5CFC"), Color(hex: "#8B5CF6")], startPoint: .topLeading, endPoint: .bottomTrailing)
        )
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }

    // MARK: - Helpers

    private func load() async {
        error = nil
        if let cached = PersonalTabDataCache.peekPulse(momentId: momentId) {
            pulse = cached.pulse
            activities = cached.activities
            loading = false
        } else {
            loading = true
        }
        do {
            let data = try await PersonalTabLoad.loadPulseTab(momentId: momentId)
            pulse = data.pulse
            activities = data.activities
        } catch {
            self.error = error.localizedDescription
        }
        loading = false
    }

    private func bandRank(_ band: String) -> Int {
        switch band {
        case "Thriving": return 3
        case "Structured": return 2
        default: return 1
        }
    }

    private func activityVisual(_ code: String) -> (label: String, emoji: String, color: Color) {
        let upper = code.uppercased()
        if upper.contains("MILESTONE") { return ("Milestone", "🏆", Color(hex: "#10B981")) }
        if upper.contains("LEARNING") { return ("Learning", "📚", Color(hex: "#3B82F6")) }
        if upper.contains("PROGRESS") { return ("Progress", "📈", Color(hex: "#A855F7")) }
        if upper.contains("OPPORTUNITY") { return ("Opportunity", "✨", Color(hex: "#06B6D4")) }
        if upper.contains("PIVOT") { return ("Pivot", "🔄", Color(hex: "#F59E0B")) }
        if upper.contains("EXPENSE") || upper.contains("MONEY") { return ("Capital", "💰", Color(hex: "#F59E0B")) }
        return ("Activity", "◎", Color(hex: "#7C5CFC"))
    }

    private func currencySymbol(_ code: String) -> String {
        switch code.uppercased() {
        case "INR": return "₹"
        case "USD": return "$"
        case "EUR": return "€"
        case "GBP": return "£"
        default: return "\(code) "
        }
    }

    private func formatMoney(_ amount: String) -> String {
        guard let n = Double(amount) else { return amount }
        return Self.moneyFormatter.string(from: NSNumber(value: n)) ?? amount
    }

    private static let moneyFormatter: NumberFormatter = {
        let f = NumberFormatter()
        f.numberStyle = .decimal
        f.minimumFractionDigits = 0
        f.maximumFractionDigits = 2
        return f
    }()
}
