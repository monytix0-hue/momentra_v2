import SwiftUI

/// Future Building Memory populated body — Figma `505:13237`.
struct PersonalFutureMemoryActiveView: View {
    let refreshToken: UInt64
    let momentId: String?
    var onProtectMilestone: () -> Void

    @State private var pulse: APIClient.PersonalPulsePayload?
    @State private var activities: [APIClient.ActivityItemPayload] = []
    @State private var loading = true
    @State private var error: String?

    var body: some View {
        Group {
            if loading && pulse == nil {
                ProgressView().tint(Color(hex: "#7C5CFC"))
            } else {
                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                        if let error {
                            Text(error)
                                .font(.caption)
                                .foregroundStyle(Color(hex: "#F87171"))
                        }
                        identitySnapshot
                        corePattern
                        driversRow
                        returnBehaviors
                        emotionalDna
                        evolutionTimeline
                        aiInterpretation
                        growthEdge
                        insightsCard
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                }
            }
        }
        .background(Color(hex: "#14121B"))
        .task(id: "\(refreshToken)-\(momentId ?? "")") { await load() }
    }

    // MARK: - Derived

    private var stage: String { PersonalLifeOpsDerived.stageBand(wellbeing: pulse?.wellbeingScore) }

    private var thinData: Bool {
        activities.count < 2
            && PersonalLifeOpsDerived.scoreNumber(pulse?.wellbeingScore) == nil
    }

    private var confidenceLabel: String {
        if thinData { return "Building…" }
        return "\(min(92, 55 + activities.count * 3))% confidence"
    }

    private var identityBody: String {
        if thinData {
            return "Log milestones, learning, and progress to reveal your builder identity."
        }
        return "You respond best when learning and execution work together."
    }

    private var drivers: (helping: [String], hurting: [String]) {
        futureHelpingHurting(from: activities.map { ($0.activityCode, $0.title) })
    }

    private var hasMilestone: Bool {
        activities.contains { $0.activityCode.uppercased().contains("MILESTONE") }
    }

    private var hasLearning: Bool {
        activities.contains { $0.activityCode.uppercased().contains("LEARNING") }
    }

    private var hasProgress: Bool {
        activities.contains { $0.activityCode.uppercased().contains("PROGRESS") }
    }

    private var hasExpense: Bool {
        activities.contains { $0.activityCode.uppercased().contains("EXPENSE") }
    }

    private var patternConfidenceLabel: String {
        if thinData { return "Building…" }
        return "\(min(92, 40 + activities.count * 5))% pattern confidence"
    }

    private var aiBody: String {
        if thinData {
            return "Building… Log milestones, learning, and progress to unlock an interpretation."
        }
        if hasLearning && hasProgress {
            return "Your future compounds when learning is paired with execution, not stored for later."
        }
        if hasMilestone {
            return "Milestone reviews keep momentum from drifting — protect them before the week fills up."
        }
        if hasExpense {
            return "Capital signals are present — pair them with learning blocks so spend fuels growth."
        }
        return "Keep logging learning and execution together to reveal how your future compounds."
    }

    // MARK: - Sections

    private var identitySnapshot: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("1 IDENTITY SNAPSHOT")
                .font(.system(size: 12, weight: .bold))
                .tracking(1.2)
                .foregroundStyle(Color(hex: "#C9C4D8"))
            Text("Builder in Motion")
                .font(.system(size: 28, weight: .heavy))
                .foregroundStyle(Color(hex: "#E5E0EE"))
            HStack(alignment: .top, spacing: 12) {
                Text(confidenceLabel)
                    .font(.system(size: 12, weight: .heavy))
                    .foregroundStyle(Color(hex: "#10B981"))
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background(Color(hex: "#10B981").opacity(0.12))
                    .overlay(Capsule().stroke(Color(hex: "#10B981"), lineWidth: 1))
                    .clipShape(Capsule())
                Text(identityBody)
                    .font(.system(size: 14))
                    .foregroundStyle(Color(hex: "#C9C4D8"))
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(hex: "#191622"))
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(Color.white.opacity(0.08), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }

    private var corePattern: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("2 CORE PATTERN")
                .font(.system(size: 12, weight: .bold))
                .tracking(1.2)
                .foregroundStyle(Color(hex: "#C9C4D8"))
            if thinData {
                Text("Building… Need a few more logs to show Learning → Execution → Momentum.")
                    .font(.system(size: 13))
                    .foregroundStyle(Color(hex: "#C9C4D8"))
            } else {
                HStack(spacing: 8) {
                    patternStep(emoji: "📚", label: "Learning", tint: Color(hex: "#10B981"))
                    Image(systemName: "chevron.right")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundStyle(Color(hex: "#C9C4D8"))
                    patternStep(emoji: "💪", label: "Execution", tint: Color(hex: "#3B82F6"))
                    Image(systemName: "chevron.right")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundStyle(Color(hex: "#C9C4D8"))
                    patternStep(emoji: "⚖️", label: "Momentum", tint: Color(hex: "#06B6D4"))
                }
            }
            Text(patternConfidenceLabel)
                .font(.system(size: 12, weight: .heavy))
                .foregroundStyle(Color(hex: "#10B981"))
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(Color(hex: "#10B981").opacity(0.12))
                .clipShape(Capsule())
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(hex: "#191622"))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.1), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func patternStep(emoji: String, label: String, tint: Color) -> some View {
        VStack(spacing: 6) {
            Text(emoji)
                .font(.system(size: 14, weight: .heavy))
                .frame(width: 32, height: 32)
                .background(tint)
                .clipShape(RoundedRectangle(cornerRadius: 16))
            Text(label)
                .font(.system(size: 11, weight: .bold))
                .foregroundStyle(.white)
                .multilineTextAlignment(.center)
                .minimumScaleFactor(0.8)
        }
        .padding(10)
        .frame(maxWidth: .infinity)
        .background(tint.opacity(0.1))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(tint.opacity(0.3), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var driversRow: some View {
        HStack(alignment: .top, spacing: 12) {
            driverColumn(
                title: "3 BEST DRIVERS",
                titleColor: Color(hex: "#4CAF50"),
                items: drivers.helping,
                empty: "Building…"
            )
            driverColumn(
                title: "4 LOWEST DRIVERS",
                titleColor: Color(hex: "#E91E63"),
                items: drivers.hurting,
                empty: "Building…"
            )
        }
    }

    private func driverColumn(title: String, titleColor: Color, items: [String], empty: String) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(title)
                .font(.system(size: 11, weight: .bold))
                .tracking(1)
                .foregroundStyle(titleColor)
            if items.isEmpty {
                Text(empty)
                    .font(.system(size: 13))
                    .foregroundStyle(Color(hex: "#C9C4D8"))
            } else {
                ForEach(items, id: \.self) { label in
                    Text(label)
                        .font(.system(size: 13, weight: .medium))
                        .foregroundStyle(.white)
                        .lineLimit(2)
                }
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(titleColor.opacity(0.1))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.1), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var returnBehaviors: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("5 RETURN BEHAVIORS")
                .font(.system(size: 12, weight: .bold))
                .tracking(1.2)
                .foregroundStyle(Color(hex: "#C9C4D8"))
            let rows = behaviorRows()
            if rows.isEmpty {
                Text("Building…")
                    .font(.system(size: 13))
                    .foregroundStyle(Color(hex: "#C9C4D8"))
            } else {
                ForEach(rows, id: \.label) { row in
                    HStack {
                        Text(row.label)
                            .font(.system(size: 13, weight: .medium))
                            .foregroundStyle(Color(hex: "#E5E0EE"))
                        Spacer()
                        Text(row.badge)
                            .font(.system(size: 11, weight: .heavy))
                            .foregroundStyle(row.badge == "High" ? Color(hex: "#10B981") : Color(hex: "#FF9800"))
                            .padding(.horizontal, 10)
                            .padding(.vertical, 4)
                            .background(
                                (row.badge == "High" ? Color(hex: "#10B981") : Color(hex: "#FF9800")).opacity(0.15)
                            )
                            .clipShape(Capsule())
                    }
                }
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white.opacity(0.05))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func behaviorRows() -> [(label: String, badge: String)] {
        var rows: [(String, String)] = []
        if hasMilestone { rows.append(("Weekly milestone review", "High")) }
        if hasLearning { rows.append(("Same-day learning log", "High")) }
        if hasProgress { rows.append(("Progress check-ins", "Medium")) }
        if hasExpense { rows.append(("Capital logged same day", "Medium")) }
        return rows
    }

    private var emotionalDna: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("6 EMOTIONAL DNA")
                .font(.system(size: 12, weight: .bold))
                .tracking(1.2)
                .foregroundStyle(Color(hex: "#C9C4D8"))
            Text("Building…")
                .font(.system(size: 13))
                .foregroundStyle(Color(hex: "#C9C4D8"))
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white.opacity(0.05))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var evolutionTimeline: some View {
        let bands = ["Stabilizing", "Structured", "Adaptive"]
        let current: String = {
            switch stage {
            case "Thriving": return "Adaptive"
            case "Structured": return "Structured"
            default: return "Stabilizing"
            }
        }()
        return VStack(alignment: .leading, spacing: 12) {
            Text("8 EVOLUTION TIMELINE")
                .font(.system(size: 12, weight: .bold))
                .tracking(1.2)
                .foregroundStyle(Color(hex: "#C9C4D8"))
            ForEach(bands, id: \.self) { band in
                HStack {
                    Text(band)
                        .font(.system(size: 14, weight: band == current ? .heavy : .medium))
                        .foregroundStyle(band == current ? Color(hex: "#7C5CFC") : Color(hex: "#E5E0EE"))
                    Spacer()
                    if band == current {
                        Text("Now")
                            .font(.system(size: 12, weight: .heavy))
                            .foregroundStyle(.white)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 4)
                            .background(Color(hex: "#7C5CFC"))
                            .clipShape(Capsule())
                    } else if PersonalLifeOpsDerived.scoreNumber(pulse?.wellbeingScore) == nil {
                        Text("Building…")
                            .font(.system(size: 12))
                            .foregroundStyle(Color(hex: "#C9C4D8"))
                    }
                }
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white.opacity(0.05))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var aiInterpretation: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 6) {
                Text("9 AI INTERPRETATION")
                    .font(.system(size: 12, weight: .bold))
                    .tracking(1.2)
                    .foregroundStyle(Color(hex: "#C9C4D8"))
                Image(systemName: "sparkle")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundStyle(Color(hex: "#10B981"))
            }
            Text(aiBody)
                .font(.system(size: 14))
                .foregroundStyle(Color(hex: "#E5E0EE"))
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white.opacity(0.05))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var growthEdge: some View {
        VStack(spacing: 10) {
            Text("10 YOUR NEXT GROWTH EDGE")
                .font(.system(size: 11, weight: .bold))
                .tracking(1.2)
                .foregroundStyle(.white)
            Text("Protect one weekly milestone review")
                .font(.system(size: 16, weight: .heavy))
                .foregroundStyle(.white)
                .multilineTextAlignment(.center)
            Text("Schedule it before the commitment, not after the crash.")
                .font(.system(size: 13))
                .foregroundStyle(.white.opacity(0.85))
                .multilineTextAlignment(.center)
            Button(action: onProtectMilestone) {
                Text("Protect Milestone Now")
                    .font(.system(size: 14, weight: .heavy))
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
            }
            .buttonStyle(.plain)
            .disabled(momentId == nil)
            .opacity(momentId == nil ? 0.5 : 1)
        }
        .padding(20)
        .frame(maxWidth: .infinity)
        .background(
            LinearGradient(colors: [Color(hex: "#7C5CFC"), Color(hex: "#3B82F6")], startPoint: .topLeading, endPoint: .bottomTrailing)
        )
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }

    private var insightsCard: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text("AI Insights")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundStyle(Color(hex: "#E5E0EE"))
                Spacer()
                Text("Coming Soon")
                    .font(.system(size: 10, weight: .heavy))
                    .foregroundStyle(Color(hex: "#A78BFA"))
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background(Color(hex: "#7C5CFC").opacity(0.2))
                    .clipShape(Capsule())
            }
            Text("Recurring themes and growth patterns across your Future Building journey will appear here.")
                .font(.system(size: 12))
                .foregroundStyle(Color(hex: "#C9C4D8"))
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white.opacity(0.05))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

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

    private func futureHelpingHurting(from activities: [(code: String, title: String)]) -> (helping: [String], hurting: [String]) {
        var helping: [String] = []
        var hurting: [String] = []
        for item in activities.prefix(8) {
            let code = item.code.uppercased()
            if code.contains("MILESTONE") {
                helping.append("Milestone +")
            } else if code.contains("LEARNING") {
                helping.append("Learning · \(item.title)")
            } else if code.contains("PROGRESS") {
                helping.append("Progress +")
            } else if code.contains("OPPORTUNITY") {
                helping.append("Opportunity · \(item.title)")
            } else if code.contains("PIVOT") {
                hurting.append("Pivot · \(item.title)")
            } else if code.contains("EXPENSE") {
                hurting.append("Capital · \(item.title)")
            }
        }
        return (Array(helping.prefix(3)), Array(hurting.prefix(3)))
    }
}
