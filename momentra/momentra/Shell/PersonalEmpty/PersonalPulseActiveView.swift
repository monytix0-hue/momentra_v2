import SwiftUI

struct PersonalPulseActiveView: View {
    let refreshToken: UInt64
    let momentTitle: String?
    let momentId: String?
    let momentTypeCode: String?
    var onAddExpense: () -> Void
    var onLifeOpsQuickAdd: (LifeOpsQuickAddKind) -> Void = { _ in }
    var onFutureQuickAdd: (FutureQuickAddKind) -> Void = { _ in }
    var onLifestyleQuickAdd: (LifestyleQuickAddKind) -> Void = { _ in }
    var onViewAllActivity: () -> Void = {}

    @State private var pulse: APIClient.PersonalPulsePayload?
    @State private var activities: [APIClient.ActivityItemPayload] = []
    @State private var loading = true
    @State private var error: String?

    private var family: PersonalPulseFamily { PersonalPulseFamily.forTypeCode(momentTypeCode) }
    private var theme: PersonalPulseFamilyTheme { family.theme }
    private var isLifeOps: Bool { family == .lifeOperations }
    private var isFuture: Bool { family == .futureBuilding }
    private var isLifestyle: Bool { family == .lifestyle }

    var body: some View {
        Group {
            if loading && pulse == nil {
                ProgressView().tint(Color(hex: "#7C5CFC"))
            } else {
                NativeDashboardScaffold(background: Color(hex: "#14121B")) {

                    NativeListSection {

                    VStack(alignment: .leading, spacing: 10) {
                        if let error {
                            Text(error)
                                .font(.caption)
                                .foregroundStyle(Color(hex: "#F87171"))
                        }
                        if let momentTitle, !momentTitle.isEmpty {
                            Text(momentTitle)
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(Color(hex: "#C9C4D8"))
                        }
                        heroCard
                        tileGrid
                        momentumCard
                        if isLifeOps || isFuture || isLifestyle {
                            helpingHurtingCard
                        }
                        activityCard
                        moneyCard
                        nudgeCard
                        insightsCard
                        quickActionsRow
                    }
                

                    }

                }
            }
        }
        .background(Color(hex: "#14121B"))
        .task(id: "\(refreshToken)-\(momentId ?? "")") { await load() }
    }

    private var wellbeing: String { PersonalLifeOpsDerived.displayScore(pulse?.wellbeingScore) }
    private var recovery: String { PersonalLifeOpsDerived.displayScore(pulse?.recoveryScore) }
    private var rhythm: String { PersonalLifeOpsDerived.displayScore(pulse?.rhythmScore) }
    private var pressure: String { PersonalLifeOpsDerived.pressure(fromRecovery: pulse?.recoveryScore) }
    private var attention: String {
        if isLifeOps || isFuture || isLifestyle {
            return PersonalLifeOpsDerived.attentionDisplay(count: pulse?.attentionCount)
        }
        guard let n = pulse?.attentionCount, n > 0 else { return "—" }
        return "\(n)"
    }
    private var vision: String { wellbeing } // Vision ← wellbeing_score
    private var growth: String { recovery } // Growth ← recovery_score
    private var momentum: String { rhythm } // Momentum ← rhythm_score
    private var discipline: String { attention } // Discipline ← attention score-like
    // Lifestyle axes
    private var joy: String { recovery } // Joy ← recovery_score
    private var fulfillment: String { wellbeing } // Fulfillment ← wellbeing_score
    private var vitality: String { rhythm } // Vitality ← rhythm_score
    private var exploration: String { attention } // Exploration ← attention
    private var experienceCount: Int {
        if let n = pulse?.widgetPayload?["experienceCount"]?.value as? Int { return n }
        if let n = pulse?.widgetPayload?["experienceCount"]?.value as? Double { return Int(n) }
        return activities.filter { $0.activityCode.uppercased().contains("EXPERIENCE") }.count
    }
    private var mood: String {
        let m = pulse?.moodState?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        return m.isEmpty ? "—" : m
    }
    private var streak: Int {
        PersonalLifeOpsDerived.streakDays(from: activities.map(\.occurredAt))
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

    private var lifeOpsAxisValues: [String] { [pressure, recovery, rhythm, attention] }
    private var futureAxisValues: [String] { [vision, growth, momentum, discipline] }
    private var lifestyleAxisValues: [String] { [joy, fulfillment, vitality, exploration] }

    private var heroCard: some View {
        let values: [String] = {
            if isLifeOps { return lifeOpsAxisValues }
            if isFuture { return futureAxisValues }
            if isLifestyle { return lifestyleAxisValues }
            return [recovery, rhythm, attention, mood]
        }()
        return VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(theme.heroTitle)
                        .font(.plusJakarta(size: 11, weight: .bold))
                        .foregroundStyle(.white.opacity(0.8))
                    Text(wellbeing)
                        .font(.plusJakarta(size: 40, weight: .heavy))
                        .foregroundStyle(.white)
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 6) {
                    Text(wellbeing == "—" ? theme.heroSubtitleEmpty : theme.heroSubtitleFilled)
                        .font(.plusJakarta(size: 11, weight: .heavy))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 5)
                        .background(Color.white.opacity(0.1))
                        .overlay(Capsule().stroke(Color.white.opacity(0.2), lineWidth: 1))
                        .clipShape(Capsule())
                    if streak > 0 {
                        Text("\(streak) Day Streak")
                            .font(.plusJakarta(size: 10, weight: .heavy))
                            .foregroundStyle(Color(hex: "#10B981"))
                            .padding(.horizontal, 10)
                            .padding(.vertical, 4)
                            .background(Color(hex: "#10B981").opacity(0.15))
                            .clipShape(Capsule())
                    }
                    HStack(spacing: 6) {
                        Image(systemName: "bolt.fill")
                            .font(.system(size: 11, weight: .semibold))
                        Text(Self.todayLabel)
                            .font(.plusJakarta(size: 12, weight: .semibold))
                    }
                    .foregroundStyle(.white.opacity(0.8))
                }
            }
            HStack(spacing: 8) {
                ForEach(Array(theme.heroMetrics.enumerated()), id: \.offset) { index, label in
                    VStack(alignment: .leading, spacing: 2) {
                        Text(label)
                            .font(.plusJakarta(size: 9, weight: .semibold))
                            .foregroundStyle(.white.opacity(0.75))
                        Text(values.indices.contains(index) ? values[index] : "—")
                            .font(.plusJakarta(size: 14, weight: .heavy))
                            .foregroundStyle(.white)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(8)
                    .background(Color.white.opacity(0.08))
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.white.opacity(0.1), lineWidth: 1))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
            }
        }
        .padding(14)
        .background(
            LinearGradient(colors: [theme.heroStart, theme.heroEnd], startPoint: .leading, endPoint: .trailing)
        )
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(Color.white.opacity(0.1), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .shadow(color: theme.accent.opacity(0.25), radius: 16, y: 6)
    }

    private var tileGrid: some View {
        let values: [String]
        let accents: [Color]
        let icons: [String]
        let progress: [CGFloat]
        let badges: [String]
        if isLifeOps {
            values = lifeOpsAxisValues
            accents = [Color(hex: "#7C5CFC"), Color(hex: "#4CD6FF"), Color(hex: "#2196F3"), Color(hex: "#FF9800")]
            icons = ["bolt.fill", "waveform.path.ecg", "chart.line.uptrend.xyaxis", "scope"]
            let pressureRaw: String? = {
                guard let n = PersonalLifeOpsDerived.scoreNumber(pulse?.recoveryScore) else { return nil }
                return String(100 - n)
            }()
            progress = [
                scoreFraction(pressureRaw),
                scoreFraction(pulse?.recoveryScore),
                scoreFraction(pulse?.rhythmScore),
                attention == "—" ? 0 : min(1, CGFloat(pulse?.attentionCount ?? 0) / 10),
            ]
            badges = [
                PersonalLifeOpsDerived.statusBadge(forScore: pressureRaw, axis: "pressure"),
                PersonalLifeOpsDerived.statusBadge(forScore: pulse?.recoveryScore, axis: "recovery"),
                PersonalLifeOpsDerived.statusBadge(forScore: pulse?.rhythmScore, axis: "discipline"),
                PersonalLifeOpsDerived.statusBadge(forScore: attention == "—" ? nil : attention, axis: "attention"),
            ]
        } else if isFuture {
            values = futureAxisValues
            accents = [Color(hex: "#8B5CF6"), Color(hex: "#3B82F6"), Color(hex: "#10B981"), Color(hex: "#FF9800")]
            icons = ["bolt.fill", "waveform.path.ecg", "globe", "gearshape"]
            progress = [
                scoreFraction(pulse?.wellbeingScore),
                scoreFraction(pulse?.recoveryScore),
                scoreFraction(pulse?.rhythmScore),
                attention == "—" ? 0 : min(1, CGFloat(pulse?.attentionCount ?? 0) / 10),
            ]
            badges = [
                PersonalLifeOpsDerived.statusBadge(forScore: pulse?.wellbeingScore, axis: "pressure"),
                PersonalLifeOpsDerived.statusBadge(forScore: pulse?.recoveryScore, axis: "recovery"),
                PersonalLifeOpsDerived.statusBadge(forScore: pulse?.rhythmScore, axis: "discipline"),
                PersonalLifeOpsDerived.statusBadge(forScore: attention == "—" ? nil : attention, axis: "attention"),
            ]
        } else if isLifestyle {
            values = lifestyleAxisValues
            accents = [Color(hex: "#EC4899"), Color(hex: "#A78BFA"), Color(hex: "#10B981"), Color(hex: "#F59E0B")]
            icons = ["face.smiling", "heart.fill", "leaf.fill", "safari"]
            progress = [
                scoreFraction(pulse?.recoveryScore),
                scoreFraction(pulse?.wellbeingScore),
                scoreFraction(pulse?.rhythmScore),
                attention == "—" ? 0 : min(1, CGFloat(pulse?.attentionCount ?? 0) / 10),
            ]
            badges = [
                PersonalLifeOpsDerived.statusBadge(forScore: pulse?.recoveryScore, axis: "recovery"),
                PersonalLifeOpsDerived.statusBadge(forScore: pulse?.wellbeingScore, axis: "discipline"),
                PersonalLifeOpsDerived.statusBadge(forScore: pulse?.rhythmScore, axis: "discipline"),
                PersonalLifeOpsDerived.statusBadge(forScore: attention == "—" ? nil : attention, axis: "attention"),
            ]
        } else {
            values = [recovery, attention, rhythm, "\(pulse?.activeMomentCount ?? 0)"]
            accents = [Color(hex: "#4CD6FF"), Color(hex: "#FF9800"), Color(hex: "#2196F3"), theme.accent]
            icons = ["waveform.path.ecg", "scope", "chart.line.uptrend.xyaxis", "bolt.fill"]
            progress = [
                scoreFraction(pulse?.recoveryScore),
                attention == "—" ? 0 : min(1, CGFloat(pulse?.attentionCount ?? 0) / 10),
                scoreFraction(pulse?.rhythmScore),
                min(1, CGFloat(pulse?.activeMomentCount ?? 0) / 4),
            ]
            badges = values.map { $0 != "—" && $0 != "0" ? "Live" : "Empty" }
        }
        return VStack(spacing: 8) {
            HStack(spacing: 8) {
                ForEach(0..<2, id: \.self) { index in
                    metricTile(
                        label: theme.tileLabels[index],
                        value: values[index],
                        accent: accents[index],
                        icon: icons[index],
                        badge: badges[index],
                        progress: progress[index]
                    )
                }
            }
            HStack(spacing: 8) {
                ForEach(2..<4, id: \.self) { index in
                    metricTile(
                        label: theme.tileLabels[index],
                        value: values[index],
                        accent: accents[index],
                        icon: icons[index],
                        badge: badges[index],
                        progress: progress[index]
                    )
                }
            }
        }
    }

    private func metricTile(
        label: String,
        value: String,
        accent: Color,
        icon: String,
        badge: String,
        progress: CGFloat
    ) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                HStack(spacing: 6) {
                    Image(systemName: icon)
                        .font(.system(size: 11, weight: .semibold))
                    Text(label)
                        .font(.system(size: 11, weight: .bold))
                }
                .foregroundStyle(accent)
                Spacer()
                Text(badge)
                    .font(.system(size: 9, weight: .heavy))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 6)
                    .padding(.vertical, 2)
                    .background(Color.white.opacity(0.1))
                    .clipShape(Capsule())
            }
            Text(value)
                .font(.system(size: 20, weight: .heavy))
                .foregroundStyle(accent)
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule().fill(Color.white.opacity(0.1))
                    Capsule()
                        .fill(accent)
                        .frame(width: max(0, geo.size.width * progress))
                }
            }
            .frame(height: 4)
        }
        .padding(12)
        .frame(maxWidth: .infinity, minHeight: 92, alignment: .topLeading)
        .background(accent.opacity(0.1))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(accent.opacity(0.4), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var momentumCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 6) {
                Image(systemName: "bolt.fill")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(Color(hex: "#7C5CFC"))
                Text("TODAY'S MOMENTUM")
                    .font(.plusJakarta(size: 11, weight: .bold))
                    .foregroundStyle(.white)
            }
            let pills: [(String, Color)] = {
                if isFuture {
                    return [
                        (growth == "—" ? "Career pending" : "Career Rising", Color(hex: "#10B981")),
                        (discipline == "—" ? "Skills quiet" : "Skills Improving", Color(hex: "#FF9800")),
                        (spendPairs.isEmpty ? "Savings quiet" : "Savings Strong", spendPairs.isEmpty ? Color(hex: "#FF9800") : Color(hex: "#10B981")),
                        (momentum == "—" ? "Network quiet" : "Network Growing", Color(hex: "#10B981")),
                    ]
                }
                if isLifestyle {
                    return [
                        (joy == "—" ? "Joy pending" : "Joy Rising", Color(hex: "#10B981")),
                        (vitality == "—" ? "Ritual quiet" : "Ritual Steady", Color(hex: "#FF9800")),
                        (mood == "—" ? "Mood pending" : "Mood · \(mood)", Color(hex: "#10B981")),
                        (spendPairs.isEmpty ? "Budget quiet" : "Budget Strong", spendPairs.isEmpty ? Color(hex: "#FF9800") : Color(hex: "#10B981")),
                    ]
                }
                return [
                    (recovery == "—" ? "Recovery pending" : "Recovery Rising", Color(hex: "#10B981")),
                    (pressure == "—" ? "Pressure quiet" : "Pressure Stable", Color(hex: "#FF9800")),
                    (mood == "—" ? "Mood pending" : "Mood · \(mood)", Color(hex: "#10B981")),
                    (spendPairs.isEmpty ? "Budget quiet" : "Budget Strong", spendPairs.isEmpty ? Color(hex: "#FF9800") : Color(hex: "#10B981")),
                ]
            }()
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 8) {
                ForEach(Array(pills.enumerated()), id: \.offset) { _, pill in
                    momentumPill(label: pill.0, tint: pill.1)
                }
            }
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(hex: "#14121C"))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var helpingHurtingCard: some View {
        let drivers = PersonalLifeOpsDerived.helpingHurting(
            from: activities.map { (code: $0.activityCode, title: $0.title) }
        )
        return HStack(alignment: .top, spacing: 10) {
            driverColumn(
                title: "HELPING",
                tint: Color(hex: "#10B981"),
                items: drivers.helping.map(\.label),
                empty: isFuture
                    ? "Log a milestone or learning"
                    : (isLifestyle ? "Log an experience or wellbeing" : "Log recovery or mood")
            )
            driverColumn(
                title: "HURTING",
                tint: Color(hex: "#F87171"),
                items: drivers.hurting.map(\.label),
                empty: isFuture || isLifestyle ? "No drag signals" : "No pressure signals"
            )
        }
    }

    private func driverColumn(title: String, tint: Color, items: [String], empty: String) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.plusJakarta(size: 11, weight: .bold))
                .foregroundStyle(tint)
            if items.isEmpty {
                Text(empty)
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(Color(hex: "#C9C4D8"))
            } else {
                ForEach(items, id: \.self) { item in
                    HStack(spacing: 6) {
                        Circle().fill(tint).frame(width: 6, height: 6)
                        Text(item)
                            .font(.plusJakarta(size: 11, weight: .semibold))
                            .foregroundStyle(Color(hex: "#E5E0EE"))
                            .lineLimit(1)
                    }
                }
            }
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(tint.opacity(0.08))
        .overlay(RoundedRectangle(cornerRadius: 14).stroke(tint.opacity(0.35), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 14))
        .shadow(color: tint.opacity(0.2), radius: 8, y: 2)
    }

    private func momentumPill(label: String, tint: Color) -> some View {
        HStack(spacing: 8) {
            Image(systemName: "arrow.up")
                .font(.system(size: 10, weight: .bold))
                .foregroundStyle(tint)
                .frame(width: 26, height: 26)
                .background(tint.opacity(0.15))
                .clipShape(RoundedRectangle(cornerRadius: 13))
            Text(label)
                .font(.system(size: 11, weight: .semibold))
                .foregroundStyle(.white)
                .lineLimit(1)
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 8)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(tint.opacity(0.1))
        .clipShape(RoundedRectangle(cornerRadius: 10))
    }

    private var activityCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("RECENT ACTIVITY")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundStyle(Color(hex: "#C9C4D8"))
                Spacer()
                Button(action: onViewAllActivity) {
                    Text("View All")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundStyle(Color(hex: "#7C5CFC"))
                }
                .buttonStyle(.plain)
            }
            if activities.isEmpty {
                Text("No activity yet.")
                    .font(.caption)
                    .foregroundStyle(Color(hex: "#C9C4D8"))
            } else {
                ForEach(Array(activities.prefix(8).enumerated()), id: \.element.id) { index, item in
                    activityRow(item)
                    if index < min(7, activities.count - 1) {
                        Divider().overlay(Color.white.opacity(0.05))
                    }
                }
            }
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white.opacity(0.05))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func activityRow(_ item: APIClient.ActivityItemPayload) -> some View {
        let isExpense = item.activityCode.localizedCaseInsensitiveContains("EXPENSE")
        let badge = isExpense ? Color(hex: "#F87171") : Color(hex: "#4CD6FF")
        return HStack(spacing: 10) {
            Image(systemName: isExpense ? "cart" : "bolt.fill")
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(badge)
                .frame(width: 32, height: 32)
                .background(badge.opacity(0.1))
                .clipShape(RoundedRectangle(cornerRadius: 10))
            VStack(alignment: .leading, spacing: 2) {
                Text(item.title)
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(Color(hex: "#E5E0EE"))
                Text(formatOccurredAt(item.occurredAt))
                    .font(.system(size: 10))
                    .foregroundStyle(Color(hex: "#C9C4D8"))
            }
            Spacer()
            Text(item.activityCode.replacingOccurrences(of: "_", with: " ").capitalized)
                .font(.system(size: 10, weight: .heavy))
                .foregroundStyle(Color(hex: "#14121B"))
                .padding(.horizontal, 8)
                .padding(.vertical, 3)
                .background(badge)
                .clipShape(Capsule())
        }
        .padding(.vertical, 4)
    }

    private var moneyCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(theme.moneyTitle)
                .font(.system(size: 11, weight: .bold))
                .foregroundStyle(Color(hex: "#C9C4D8"))
            Text(
                spendPairs.isEmpty
                    ? "—"
                    : spendPairs.map { "\($0.0) \(formatMoney($0.1))" }.joined(separator: " · ")
            )
            .font(.system(size: 22, weight: .heavy))
            .foregroundStyle(Color(hex: "#E5E0EE"))
            if spendPairs.isEmpty {
                Text("No spend recorded for this moment yet.")
                    .font(.caption)
                    .foregroundStyle(Color(hex: "#C9C4D8"))
            } else {
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
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white.opacity(0.05))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var nudgeCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 8) {
                Image(systemName: "shield.fill")
                    .font(.system(size: 14, weight: .semibold))
                Text(theme.nudgeTitle)
                    .font(.plusJakarta(size: 16, weight: .heavy))
            }
            .foregroundStyle(.white)
            Text(theme.nudgeBody)
                .font(.plusJakarta(size: 13))
                .foregroundStyle(.white.opacity(0.9))
            Button {
                if isLifeOps { onLifeOpsQuickAdd(.recovery) }
                else if isFuture { onFutureQuickAdd(.milestone) }
                else if isLifestyle { onLifestyleQuickAdd(.experience) }
            } label: {
                Text(theme.nudgeCta)
                    .font(.plusJakarta(size: 14, weight: .heavy))
                    .foregroundStyle(Color(hex: "#7C5CFC"))
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(Color.white)
                    .clipShape(RoundedRectangle(cornerRadius: 14))
            }
            .buttonStyle(.plain)
            .disabled((!isLifeOps && !isFuture && !isLifestyle) || momentId == nil)
        }
        .padding(14)
        .background(LinearGradient(colors: [theme.heroStart, theme.heroEnd], startPoint: .top, endPoint: .bottom))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .shadow(color: theme.accent.opacity(0.3), radius: 14, y: 4)
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
            Text(
                isFuture
                    ? "Patterns across milestones, learning, progress, and capital will surface here."
                    : (isLifestyle
                        ? "Patterns across experiences, wellbeing, discovery, and spend will surface here."
                        : "Patterns across pressure, recovery, mood, and money will surface here.")
            )
                .font(.system(size: 12))
                .foregroundStyle(Color(hex: "#C9C4D8"))
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white.opacity(0.05))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var quickActionsRow: some View {
        let colors: [Color] = [
            Color(hex: "#4CD6FF"),
            Color(hex: "#FF9800"),
            Color(hex: "#A78BFA"),
            Color(hex: "#10B981"),
            Color(hex: "#C9C4D8"),
        ]
        return HStack {
            ForEach(Array(theme.quickActions.enumerated()), id: \.offset) { index, label in
                let tint = colors.indices.contains(index) ? colors[index] : Color(hex: "#C9C4D8")
                Button {
                    switch label {
                    case "Money": onAddExpense()
                    case "Recovery": onLifeOpsQuickAdd(.recovery)
                    case "Mood": onLifeOpsQuickAdd(.mood)
                    case "Attention": onLifeOpsQuickAdd(.attention)
                    case "Milestone": onFutureQuickAdd(.milestone)
                    case "Opportunity": onFutureQuickAdd(.opportunity)
                    case "Pivot": onFutureQuickAdd(.pivot)
                    case "Progress": onFutureQuickAdd(.progress)
                    case "Learning": onFutureQuickAdd(.learning)
                    case "Experience": onLifestyleQuickAdd(.experience)
                    case "Wellbeing": onLifestyleQuickAdd(.wellbeing)
                    case "Discovery": onLifestyleQuickAdd(.discovery)
                    case "Expression", "Create": onLifestyleQuickAdd(.expression)
                    case "Adjust":
                        if isLifestyle { onLifestyleQuickAdd(.adjust) }
                        else { onLifeOpsQuickAdd(.adjust) }
                    default: break
                    }
                } label: {
                    VStack(spacing: 4) {
                        Image(systemName: pulseQuickActionSymbol(label))
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundStyle(tint)
                            .frame(width: 40, height: 40)
                            .background(tint.opacity(0.12))
                            .overlay(Circle().stroke(tint.opacity(0.3), lineWidth: 1))
                            .clipShape(Circle())
                            .shadow(color: tint.opacity(0.35), radius: 6, y: 0)
                        Text(label)
                            .font(.plusJakarta(size: 10))
                            .foregroundStyle(Color(hex: "#C9C4D8"))
                    }
                }
                .buttonStyle(.plain)
                .disabled(momentId == nil && ["Money", "Recovery", "Mood", "Attention", "Adjust"].contains(label))
                .frame(maxWidth: .infinity)
            }
        }
        .padding(.vertical, 4)
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

    private static var todayLabel: String {
        let f = DateFormatter()
        f.dateFormat = "EEE, d MMM"
        return f.string(from: Date())
    }
}

private func displayScore(_ raw: String?) -> String {
    guard var s = raw?.trimmingCharacters(in: .whitespacesAndNewlines), !s.isEmpty else { return "—" }
    if s.hasSuffix(".00") { s.removeLast(3) }
    else if s.hasSuffix(".0") { s.removeLast(2) }
    return s
}

private func scoreFraction(_ raw: String?) -> CGFloat {
    guard let n = Double(raw ?? "") else { return 0 }
    return CGFloat(min(max(n / 100, 0), 1))
}

private func formatMoney(_ amount: String) -> String {
    let hide = UserDefaults.standard.bool(forKey: "momentra_hide_balances")
    if hide { return "••••" }
    guard let n = Double(amount) else { return amount }
    let f = NumberFormatter()
    f.numberStyle = .decimal
    f.minimumFractionDigits = 2
    f.maximumFractionDigits = 2
    return f.string(from: NSNumber(value: n)) ?? amount
}

private func formatOccurredAt(_ iso: String) -> String {
    let parsers: [ISO8601DateFormatter] = {
        let a = ISO8601DateFormatter()
        a.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        let b = ISO8601DateFormatter()
        b.formatOptions = [.withInternetDateTime]
        return [a, b]
    }()
    guard let date = parsers.lazy.compactMap({ $0.date(from: iso) }).first else { return iso }
    let f = DateFormatter()
    f.dateStyle = .medium
    f.timeStyle = .short
    return f.string(from: date)
}
