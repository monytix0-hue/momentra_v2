import SwiftUI

/// Life Ops Memory populated body — Figma `353:10273`.
struct PersonalLifeOpsMemoryActiveView: View {
    let refreshToken: UInt64
    let momentId: String?
    var onProtectRecovery: () -> Void

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

    private var identity: (title: String, confidence: String, body: String) {
        PersonalLifeOpsDerived.identityLabel(
            wellbeing: pulse?.wellbeingScore,
            recovery: pulse?.recoveryScore,
            activityCount: activities.count
        )
    }

    private var stage: String { PersonalLifeOpsDerived.stageBand(wellbeing: pulse?.wellbeingScore) }

    private var drivers: (helping: [PersonalLifeOpsDerived.DriverItem], hurting: [PersonalLifeOpsDerived.DriverItem]) {
        PersonalLifeOpsDerived.helpingHurting(
            from: activities.map { ($0.activityCode, $0.title) }
        )
    }

    private var hasRecovery: Bool {
        activities.contains { $0.activityCode.uppercased().contains("RECOVERY") }
            || PersonalLifeOpsDerived.scoreNumber(pulse?.recoveryScore) != nil
    }

    private var hasExpense: Bool {
        activities.contains { $0.activityCode.uppercased().contains("EXPENSE") }
    }

    private var hasRhythm: Bool {
        activities.contains {
            let c = $0.activityCode.uppercased()
            return c.contains("RHYTHM") || c.contains("ATTENTION") || c.contains("WELLBEING")
        }
    }

    private var thinData: Bool {
        activities.count < 2
            && PersonalLifeOpsDerived.scoreNumber(pulse?.wellbeingScore) == nil
            && PersonalLifeOpsDerived.scoreNumber(pulse?.recoveryScore) == nil
    }

    private var moodBars: [(label: String, emoji: String, color: Color, count: Int)] {
        var counts: [String: Int] = ["Calm": 0, "Focused": 0, "Strained": 0, "Lifted": 0]
        for item in activities where item.activityCode.uppercased().contains("MOOD") {
            let title = item.title.lowercased()
            if title.contains("calm") { counts["Calm", default: 0] += 1 }
            else if title.contains("focus") { counts["Focused", default: 0] += 1 }
            else if title.contains("strain") || title.contains("stain") || title.contains("stress") {
                counts["Strained", default: 0] += 1
            } else if title.contains("lift") || title.contains("energ") {
                counts["Lifted", default: 0] += 1
            }
        }
        if let mood = pulse?.moodState?.trimmingCharacters(in: .whitespacesAndNewlines), !mood.isEmpty {
            let key: String
            let lower = mood.lowercased()
            if lower.contains("calm") { key = "Calm" }
            else if lower.contains("focus") { key = "Focused" }
            else if lower.contains("strain") || lower.contains("stress") { key = "Strained" }
            else if lower.contains("lift") { key = "Lifted" }
            else { key = "Calm" }
            if counts.values.reduce(0, +) == 0 { counts[key, default: 0] = 1 }
        }
        let palette: [(String, String, Color)] = [
            ("Calm", "😌", Color(hex: "#2196F3")),
            ("Focused", "🎯", Color(hex: "#7C5CFC")),
            ("Strained", "😮‍💨", Color(hex: "#FF9800")),
            ("Lifted", "✨", Color(hex: "#10B981")),
        ]
        return palette.map { ($0.0, $0.1, $0.2, counts[$0.0] ?? 0) }
    }

    private var patternConfidenceLabel: String {
        if thinData { return "Building…" }
        let n = min(92, 40 + activities.count * 5)
        return "\(n)% pattern confidence"
    }

    private var aiBody: String {
        if thinData {
            return "Building… Log recovery after pressure and a few moods to unlock an interpretation."
        }
        if hasRecovery {
            return "Your system rewards recovery placed immediately after pressure, not the next morning."
        }
        if hasExpense {
            return "Spend signals are present — pair them with recovery blocks to keep pressure from compounding."
        }
        return "Keep logging pressure and recovery together to reveal how your system stabilizes."
    }

    // MARK: - Sections

    private var identitySnapshot: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("1 IDENTITY SNAPSHOT")
                .font(.system(size: 12, weight: .bold))
                .tracking(1.2)
                .foregroundStyle(.white)
            Text(identity.title)
                .font(.system(size: 32, weight: .heavy))
                .foregroundStyle(.white)
            HStack(alignment: .top, spacing: 12) {
                Text(identity.confidence)
                    .font(.system(size: 12, weight: .heavy))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background(Color.white.opacity(0.2))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                Text(identity.body)
                    .font(.system(size: 14))
                    .foregroundStyle(.white.opacity(0.8))
            }
        }
        .padding(24)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            LinearGradient(colors: [Color(hex: "#7C5CFC"), Color(hex: "#A78BFA")], startPoint: .leading, endPoint: .trailing)
        )
        .overlay(RoundedRectangle(cornerRadius: 24).stroke(Color.white.opacity(0.1), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 24))
    }

    private var corePattern: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("2 CORE PATTERN")
                .font(.system(size: 12, weight: .bold))
                .tracking(1.2)
                .foregroundStyle(.white)
            if thinData {
                Text("Building… Need a few more logs to show Pressure → Recovery → Stability.")
                    .font(.system(size: 13))
                    .foregroundStyle(Color(hex: "#C9C4D8"))
            } else {
                HStack(spacing: 8) {
                    patternStep(emoji: "🔥", label: "Pressure peak", tint: Color(hex: "#E91E63"))
                    Image(systemName: "chevron.right")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundStyle(Color(hex: "#C9C4D8"))
                    patternStep(emoji: "🌿", label: "Recovery block", tint: Color(hex: "#4CAF50"))
                    Image(systemName: "chevron.right")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundStyle(Color(hex: "#C9C4D8"))
                    patternStep(emoji: "✨", label: "Stability", tint: Color(hex: "#2196F3"))
                }
            }
            Text(patternConfidenceLabel)
                .font(.system(size: 12, weight: .heavy))
                .foregroundStyle(Color(hex: "#7C5CFC"))
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(Color(hex: "#7C5CFC").opacity(0.15))
                .clipShape(Capsule())
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(hex: "#14121C"))
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
                items: drivers.helping.map(\.label),
                empty: "Building…"
            )
            driverColumn(
                title: "4 LOWEST DRIVERS",
                titleColor: Color(hex: "#E91E63"),
                items: drivers.hurting.map(\.label),
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
        if hasRecovery { rows.append(("Recovery before meetings", "High")) }
        if hasExpense { rows.append(("Same-day expense log", "High")) }
        if hasRhythm { rows.append(("Rhythm check-ins", "Medium")) }
        return rows
    }

    private var emotionalDna: some View {
        let bars = moodBars
        let total = bars.map(\.count).reduce(0, +)
        return VStack(alignment: .leading, spacing: 12) {
            Text("6 EMOTIONAL DNA")
                .font(.system(size: 12, weight: .bold))
                .tracking(1.2)
                .foregroundStyle(Color(hex: "#C9C4D8"))
            if total == 0 {
                Text("No mood signals yet. Log a mood to map Calm / Focused / Strained / Lifted.")
                    .font(.system(size: 13))
                    .foregroundStyle(Color(hex: "#C9C4D8"))
            } else {
                ForEach(bars, id: \.label) { bar in
                    HStack(spacing: 10) {
                        Text(bar.emoji)
                        Text(bar.label)
                            .font(.system(size: 13, weight: .medium))
                            .foregroundStyle(Color(hex: "#E5E0EE"))
                            .frame(width: 64, alignment: .leading)
                        GeometryReader { geo in
                            ZStack(alignment: .leading) {
                                Capsule().fill(Color.white.opacity(0.08))
                                Capsule()
                                    .fill(bar.color)
                                    .frame(width: geo.size.width * CGFloat(bar.count) / CGFloat(total))
                            }
                        }
                        .frame(height: 8)
                        Text("\(Int((Double(bar.count) / Double(total) * 100).rounded()))%")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundStyle(Color(hex: "#C9C4D8"))
                            .frame(width: 36, alignment: .trailing)
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
                    .foregroundStyle(Color(hex: "#7C5CFC"))
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
            Text("Protect a recovery block on high-pressure days")
                .font(.system(size: 16, weight: .heavy))
                .foregroundStyle(.white)
                .multilineTextAlignment(.center)
            Text("Schedule it before the commitment, not after the crash.")
                .font(.system(size: 13))
                .foregroundStyle(.white.opacity(0.85))
                .multilineTextAlignment(.center)
            Button(action: onProtectRecovery) {
                Text("Protect Recovery Now")
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
            LinearGradient(colors: [Color(hex: "#7C5CFC"), Color(hex: "#8B5CF6")], startPoint: .topLeading, endPoint: .bottomTrailing)
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
            Text("Recurring themes and growth patterns across your Life Ops journey will appear here.")
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
}
