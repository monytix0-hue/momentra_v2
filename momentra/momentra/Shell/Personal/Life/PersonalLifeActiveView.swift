import SwiftUI

/// M3 Life — overall state only (five honest blocks via PersonalLifeSummaryModel).
struct PersonalLifeActiveView: View {
    let refreshToken: UInt64
    var onLogRecovery: () -> Void = {}
    var onLogSpend: () -> Void = {}
    var onOpenAdd: () -> Void = {}

    @State private var life: APIClient.PersonalLifePayload?
    @State private var loading = true
    @State private var error: String?
    /// nil = All; otherwise LIFE_OPERATIONS | FUTURE_BUILDING | LIFESTYLE | RELATIONSHIPS
    @State private var selectedFamilyFilter: String? = nil
    @State private var allocationMode: PersonalLifeAllocationMode = .activity

    private let bg = Color(red: 0.078, green: 0.071, blue: 0.106)
    private let card = Color(red: 0.110, green: 0.106, blue: 0.180)
    private let cardAlt = Color(red: 0.086, green: 0.106, blue: 0.149)
    private let text = Color(red: 0.898, green: 0.878, blue: 0.933)
    private let muted = Color(red: 0.788, green: 0.769, blue: 0.847)
    private let dim = Color(red: 0.549, green: 0.549, blue: 0.620)
    private let purple = Color(red: 0.486, green: 0.361, blue: 0.988)
    private let green = Color(red: 0.063, green: 0.725, blue: 0.506)
    private let red = Color(red: 0.937, green: 0.267, blue: 0.267)
    private let amber = Color(red: 0.961, green: 0.620, blue: 0.043)
    private let blue = Color(red: 0.231, green: 0.510, blue: 0.965)
    private let pink = Color(red: 0.882, green: 0.165, blue: 0.620)

    private var summary: PersonalLifeSummaryModel? {
        guard let life else { return nil }
        return PersonalLifeSummaryModel.from(life, familyFilter: selectedFamilyFilter)
    }

    var body: some View {
        Group {
            if loading && life == nil {
                ProgressView().tint(purple).frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                NativeDashboardScaffold(background: bg) {
                    NativeListSection(insets: EdgeInsets()) {
                        chipRow
                    }
                    NativeListSection {
                        if let error {
                            Text(error).font(.plusJakarta(size: 12)).foregroundStyle(red)
                        }
                        if let summary {
                            overviewBlock(summary)
                            thisWeekBlock(summary.weekSummary)
                            whereLifeWentBlock(summary)
                            if let slipping = summary.slipping {
                                insightBlock(
                                    chromeTitle: "Something slipping",
                                    insight: slipping,
                                    accent: red,
                                    tintBg: Color(red: 0.165, green: 0.082, blue: 0.125)
                                )
                            }
                            if let working = summary.working {
                                insightBlock(
                                    chromeTitle: "What’s working",
                                    insight: working,
                                    accent: green,
                                    tintBg: card
                                )
                            }
                            if let money = summary.moneySnapshot {
                                moneyBlock(money)
                            }
                            if let score = summary.globalScore {
                                secondaryScoreLine(score: score, max: summary.scoreMax)
                            }
                        }
                    }
                }
            }
        }
        .task(id: refreshToken) { await load() }
    }

    private func load() async {
        error = nil
        if let cached = PersonalTabDataCache.peekLife() {
            life = cached
            loading = false
        } else if life != nil {
            loading = false
        } else {
            loading = true
        }
        do {
            let payload = try await APIClient.shared.getPersonalLife()
            life = payload
            PersonalTabDataCache.putLife(payload)
        } catch {
            self.error = error.localizedDescription
        }
        loading = false
    }

    // MARK: - Chips (This week filter only)

    private var chipRow: some View {
        let chips: [(String, String?, Color)] = [
            ("All", nil, dim),
            ("Everyday", "LIFE_OPERATIONS", purple),
            ("Future", "FUTURE_BUILDING", blue),
            ("Lifestyle", "LIFESTYLE", amber),
            ("People", "RELATIONSHIPS", pink),
        ]
        return VStack(spacing: 0) {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    ForEach(Array(chips.enumerated()), id: \.offset) { _, chip in
                        familyChip(chip.0, familyCode: chip.1, color: chip.2)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 10)
            }
            Rectangle().fill(Color.white.opacity(0.06)).frame(height: 1)
        }
        .background(Color(red: 0.047, green: 0.059, blue: 0.082))
    }

    private func familyChip(_ label: String, familyCode: String?, color: Color) -> some View {
        let active = selectedFamilyFilter == familyCode
        return HStack(spacing: 6) {
            Circle().fill(color).frame(width: 6, height: 6)
            Text(label)
                .font(.plusJakarta(size: active ? 12 : 11, weight: active ? .semibold : .medium))
                .foregroundStyle(active ? Color.white : dim)
        }
        .padding(.horizontal, active ? 12 : 10)
        .padding(.vertical, 6)
        .background(active ? color.opacity(0.12) : cardAlt)
        .overlay(
            RoundedRectangle(cornerRadius: 20)
                .stroke(active ? color.opacity(0.5) : Color.white.opacity(0.08), lineWidth: active ? 1.5 : 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .onTapGesture { selectedFamilyFilter = familyCode }
    }

    // MARK: - 1. Life overview

    private func overviewBlock(_ summary: PersonalLifeSummaryModel) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Life overview")
                .font(.plusJakarta(size: 12, weight: .bold))
                .foregroundStyle(purple)
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 10) {
                ForEach(summary.familyStates) { state in
                    VStack(alignment: .leading, spacing: 6) {
                        Text(state.label)
                            .font(.plusJakarta(size: 11, weight: .semibold))
                            .foregroundStyle(dim)
                        Text(state.status.rawValue)
                            .font(.plusJakarta(size: 15, weight: .bold))
                            .foregroundStyle(statusColor(state.status))
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(14)
                    .background(cardAlt)
                    .overlay(RoundedRectangle(cornerRadius: 14).stroke(Color.white.opacity(0.06)))
                    .clipShape(RoundedRectangle(cornerRadius: 14))
                }
            }
        }
        .padding(16)
        .background(card)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08)))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func statusColor(_ status: PersonalLifeFamilyStatus) -> Color {
        switch status {
        case .strong: return green
        case .growing: return blue
        case .steady: return muted
        case .quiet: return dim
        case .needsAttention: return amber
        }
    }

    // MARK: - 2. This week

    private func thisWeekBlock(_ week: PersonalLifeWeekSummary) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("This week")
                .font(.plusJakarta(size: 12, weight: .bold))
                .foregroundStyle(purple)
            if let filter = week.filterLabel {
                Text("This week · \(filter)")
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(dim)
            } else {
                Text("Across Everyday, Future, Lifestyle, and People")
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(dim)
            }

            switch week.tier {
            case .empty:
                Text("Nothing logged this week yet")
                    .font(.plusJakarta(size: 13))
                    .foregroundStyle(muted)
            case .thin:
                familyCountLines(week.familyCounts)
            case .partial:
                if let sentence = week.sentence {
                    Text(sentence)
                        .font(.plusJakarta(size: 14, weight: .semibold))
                        .foregroundStyle(text)
                }
                familyCountLines(week.familyCounts)
            case .rich:
                if let sentence = week.sentence {
                    Text(sentence)
                        .font(.plusJakarta(size: 14, weight: .semibold))
                        .foregroundStyle(text)
                }
                familyCountLines(week.familyCounts)
            }
        }
        .padding(16)
        .background(card)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08)))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    @ViewBuilder
    private func familyCountLines(_ counts: [PersonalLifeWeekFamilyCount]) -> some View {
        ForEach(counts) { row in
            if row.periodLogs > 0 {
                Text("\(row.label) · \(row.periodLogs) \(row.periodLogs == 1 ? "activity" : "activities")")
                    .font(.plusJakarta(size: 13))
                    .foregroundStyle(muted)
            }
        }
    }

    // MARK: - 3. Where your life went

    private func whereLifeWentBlock(_ summary: PersonalLifeSummaryModel) -> some View {
        let allocation = allocationMode == .activity
            ? summary.activityAllocation
            : summary.moneyAllocation
        return VStack(alignment: .leading, spacing: 12) {
            Text("Where your life went")
                .font(.plusJakarta(size: 12, weight: .bold))
                .foregroundStyle(purple)
            HStack(spacing: 0) {
                allocationToggle("Activity", mode: .activity)
                allocationToggle("Money", mode: .money)
            }
            .padding(3)
            .background(cardAlt)
            .clipShape(RoundedRectangle(cornerRadius: 12))

            if !allocation.hasData {
                Text(allocationMode == .activity
                     ? "No activity to allocate this week"
                     : "No spend to allocate this week")
                    .font(.plusJakarta(size: 13))
                    .foregroundStyle(muted)
                    .padding(.top, 4)
            } else {
                ForEach(allocation.slices) { slice in
                    VStack(alignment: .leading, spacing: 6) {
                        HStack {
                            Text(slice.label)
                                .font(.plusJakarta(size: 13, weight: .medium))
                                .foregroundStyle(text)
                            Spacer()
                            Text("\(slice.percent)%")
                                .font(.plusJakarta(size: 13, weight: .semibold))
                                .foregroundStyle(muted)
                        }
                        GeometryReader { geo in
                            ZStack(alignment: .leading) {
                                Capsule().fill(Color.white.opacity(0.06))
                                Capsule()
                                    .fill(familyAccent(slice.familyCode))
                                    .frame(width: geo.size.width * CGFloat(slice.percent) / 100.0)
                            }
                        }
                        .frame(height: 8)
                    }
                }
            }
        }
        .padding(16)
        .background(card)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08)))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func allocationToggle(_ label: String, mode: PersonalLifeAllocationMode) -> some View {
        let active = allocationMode == mode
        return Text(label)
            .font(.plusJakarta(size: 12, weight: active ? .bold : .medium))
            .foregroundStyle(active ? text : dim)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 8)
            .background(active ? purple.opacity(0.25) : Color.clear)
            .clipShape(RoundedRectangle(cornerRadius: 10))
            .onTapGesture { allocationMode = mode }
    }

    private func familyAccent(_ code: String) -> Color {
        switch code.uppercased() {
        case "LIFE_OPERATIONS": return purple
        case "FUTURE_BUILDING": return green
        case "LIFESTYLE": return amber
        case "RELATIONSHIPS": return pink
        default: return blue
        }
    }

    // MARK: - 4. Slipping / Working

    private func insightBlock(
        chromeTitle: String,
        insight: PersonalLifeInsight,
        accent: Color,
        tintBg: Color
    ) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(chromeTitle)
                .font(.plusJakarta(size: 11, weight: .bold))
                .foregroundStyle(accent)
            Text(insight.headline)
                .font(.plusJakarta(size: 16, weight: .bold))
                .foregroundStyle(text)
            Text(insight.body)
                .font(.plusJakarta(size: 13))
                .foregroundStyle(muted)
            if let cta = insight.ctaLabel, let action = resolveCta(insight.ctaAction) {
                Button(action: action) {
                    Text(cta)
                        .font(.plusJakarta(size: 13, weight: .semibold))
                        .foregroundStyle(accent)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .background(accent.opacity(0.12))
                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(accent.opacity(0.35)))
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                .buttonStyle(.plain)
            }
        }
        .padding(16)
        .background(tintBg)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(accent.opacity(0.3)))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    // MARK: - 5. Money supporting your life

    private func moneyBlock(_ money: PersonalLifeMoneySnapshot) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Money supporting your life")
                .font(.plusJakarta(size: 12, weight: .bold))
                .foregroundStyle(purple)
            if money.incomeTotal > 0 {
                moneyRow("Income", BalanceMask.mask(PersonalLifeSummaryModel.formatMoney(money.incomeTotal, currencyCode: money.currencyCode)))
            }
            if money.expenseTotal > 0 {
                moneyRow("Spent", BalanceMask.mask(PersonalLifeSummaryModel.formatMoney(money.expenseTotal, currencyCode: money.currencyCode)))
            }
            if let available = money.available {
                moneyRow("Available", BalanceMask.mask(PersonalLifeSummaryModel.formatMoney(available, currencyCode: money.currencyCode)))
            }
            if !money.byFamilySpend.isEmpty {
                Rectangle().fill(Color.white.opacity(0.06)).frame(height: 1).padding(.vertical, 4)
                ForEach(money.byFamilySpend) { slice in
                    moneyRow(
                        slice.label,
                        BalanceMask.mask(PersonalLifeSummaryModel.formatMoney(slice.value, currencyCode: money.currencyCode))
                    )
                }
            }
        }
        .padding(16)
        .background(card)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08)))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func moneyRow(_ label: String, _ value: String) -> some View {
        HStack {
            Text(label)
                .font(.plusJakarta(size: 13))
                .foregroundStyle(muted)
            Spacer()
            Text(value)
                .font(.plusJakarta(size: 13, weight: .semibold))
                .foregroundStyle(text)
        }
    }

    private func secondaryScoreLine(score: Int, max: Int) -> some View {
        Text("Overall score · \(score)/\(max)")
            .font(.plusJakarta(size: 11))
            .foregroundStyle(dim)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 4)
    }

    private func resolveCta(_ ctaAction: String?) -> (() -> Void)? {
        switch ctaAction?.uppercased() {
        case "LOG_RECOVERY": return onLogRecovery
        case "LOG_SPEND": return onLogSpend
        case "OPEN_ADD": return onOpenAdd
        default: return nil
        }
    }
}
