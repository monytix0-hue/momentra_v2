import SwiftUI

enum OpsColors {
    static let lavender = Color(hex: "#A78BFA")
    static let indigo = Color(hex: "#6366F1")
    static let indigoLight = Color(hex: "#818CF8")
    static let green = Color(hex: "#10B981")
    static let amber = Color(hex: "#F59E0B")
    static let red = Color(hex: "#EF4444")
    static let linkBlue = Color(hex: "#60A5FA")
    static let ctaText = Color(hex: "#14121B")
}

struct OpsHeroHealthRing: View {
    let score: String
    let showLive: Bool
    var ringSize: CGFloat = 110
    let theme: BusinessActiveTheme

    private var fraction: CGFloat {
        guard let n = Double(score), n > 0 else { return 0 }
        return CGFloat(min(max(n / 100, 0), 1))
    }

    var body: some View {
        ZStack(alignment: .top) {
            ZStack {
                Circle()
                    .stroke(OpsColors.lavender.opacity(0.25), lineWidth: 10)
                    .frame(width: ringSize, height: ringSize)
                Circle()
                    .trim(from: 0, to: fraction)
                    .stroke(OpsColors.lavender, style: StrokeStyle(lineWidth: 10, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                    .frame(width: ringSize, height: ringSize)
                VStack(spacing: 0) {
                    Text(score)
                        .font(.plusJakarta(size: ringSize >= 130 ? 28 : 30, weight: .heavy))
                        .foregroundStyle(theme.text)
                    if score != "—", ringSize < 130 {
                        Text("/100")
                            .font(.plusJakarta(size: 11, weight: .semibold))
                            .foregroundStyle(theme.muted)
                    }
                }
            }
            if showLive {
                HStack(spacing: 4) {
                    Circle().fill(OpsColors.green).frame(width: 5, height: 5)
                    Text("LIVE")
                        .font(.plusJakarta(size: 8, weight: .bold))
                        .foregroundStyle(OpsColors.green)
                }
                .padding(.horizontal, 6)
                .padding(.vertical, 2)
                .background(OpsColors.green.opacity(0.12))
                .overlay(Capsule().stroke(OpsColors.green.opacity(0.2)))
                .clipShape(Capsule())
                .offset(y: -6)
            }
        }
        .frame(width: ringSize, height: ringSize)
    }
}

struct OpsTintedMetricTile: View {
    let value: String
    let label: String
    let detail: String
    let tint: Color
    let theme: BusinessActiveTheme
    var valueColor: Color?

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(value)
                .font(.plusJakarta(size: 18, weight: .heavy))
                .foregroundStyle(valueColor ?? theme.text)
                .lineLimit(1)
                .minimumScaleFactor(0.7)
            Text(label.uppercased())
                .font(.plusJakarta(size: 10, weight: .semibold))
                .foregroundStyle(theme.muted)
            Text(detail)
                .font(.plusJakarta(size: 10))
                .foregroundStyle(theme.muted)
                .lineLimit(1)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(10)
        .background(tint.opacity(0.04))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(tint.opacity(0.12)))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

struct OpsFilterChipRow: View {
    let chips: [String]
    let selected: String
    let onSelect: (String) -> Void
    let theme: BusinessActiveTheme

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(chips, id: \.self) { chip in
                    Button { onSelect(chip) } label: {
                        Text(chip)
                            .font(.plusJakarta(size: 12, weight: .bold))
                            .foregroundStyle(selected == chip ? OpsColors.ctaText : theme.secondary)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 7)
                            .background(
                                selected == chip
                                    ? AnyShapeStyle(LinearGradient(colors: [OpsColors.indigoLight, OpsColors.lavender], startPoint: .leading, endPoint: .trailing))
                                    : AnyShapeStyle(theme.card)
                            )
                            .overlay(Capsule().stroke(selected == chip ? OpsColors.indigoLight.opacity(0.4) : theme.border))
                            .clipShape(Capsule())
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }
}

struct OpsDiamondDivider: View {
    let theme: BusinessActiveTheme

    var body: some View {
        HStack(spacing: 12) {
            Rectangle().fill(theme.border).frame(height: 1)
            RoundedRectangle(cornerRadius: 2)
                .fill(LinearGradient(colors: [OpsColors.lavender, OpsColors.indigo], startPoint: .topLeading, endPoint: .bottomTrailing))
                .frame(width: 8, height: 8)
            Rectangle().fill(theme.border).frame(height: 1)
        }
    }
}

struct OpsGradientPrimaryButton: View {
    let label: String
    let enabled: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(label)
                .font(.plusJakarta(size: 13, weight: .bold))
                .foregroundStyle(.white.opacity(enabled ? 1 : 0.5))
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
                .background(LinearGradient(colors: [OpsColors.lavender, OpsColors.indigo], startPoint: .leading, endPoint: .trailing))
                .clipShape(RoundedRectangle(cornerRadius: 24))
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
    }
}

struct OpsOutlineButton: View {
    let label: String
    let enabled: Bool
    let action: () -> Void
    let theme: BusinessActiveTheme

    var body: some View {
        Button(action: action) {
            Text(label)
                .font(.plusJakarta(size: 13, weight: .bold))
                .foregroundStyle(theme.text.opacity(enabled ? 1 : 0.55))
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .overlay(RoundedRectangle(cornerRadius: 24).stroke(theme.border))
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
    }
}

struct OpsCategoryBarSection: View {
    let categories: [APIClient.BusinessPulsePayload.PulseInner.OperationsExtras.SpendCategorySlice]
    let theme: BusinessActiveTheme

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Spend by Category")
                .font(.plusJakarta(size: 14, weight: .semibold))
                .foregroundStyle(theme.text)
            if categories.isEmpty {
                Text("No categorized spend yet")
                    .font(.plusJakarta(size: 13))
                    .foregroundStyle(theme.secondary)
            } else {
                ForEach(Array(categories.enumerated()), id: \.element.id) { index, slice in
                    let barColor = index % 2 == 0 ? OpsColors.lavender : OpsColors.indigo
                    VStack(spacing: 6) {
                        HStack {
                            Text(slice.label)
                                .font(.plusJakarta(size: 13, weight: .semibold))
                                .foregroundStyle(theme.text)
                            Spacer()
                            Text("\(slice.pct)%")
                                .font(.plusJakarta(size: 12, weight: .bold))
                                .foregroundStyle(barColor)
                        }
                        GeometryReader { geo in
                            ZStack(alignment: .leading) {
                                Capsule().fill(theme.border).frame(height: 8)
                                Capsule()
                                    .fill(barColor)
                                    .frame(width: geo.size.width * CGFloat(slice.pct) / 100, height: 8)
                            }
                        }
                        .frame(height: 8)
                    }
                }
            }
        }
        .padding(16)
        .background(theme.card)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(theme.border))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}

struct OpsAttentionCard: View {
    let title: String
    let severity: String
    let hasAction: Bool
    let theme: BusinessActiveTheme

    private var badgeColor: Color {
        let s = severity.uppercased()
        if s.contains("HIGH") || s.contains("CRITICAL") { return OpsColors.red }
        if s.contains("MED") { return OpsColors.amber }
        return theme.accent
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 8) {
                if !severity.isEmpty {
                    Text(String(severity.prefix(6)))
                        .font(.plusJakarta(size: 9, weight: .bold))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(badgeColor)
                        .clipShape(RoundedRectangle(cornerRadius: 4))
                }
                Circle()
                    .fill(badgeColor.opacity(0.15))
                    .frame(width: 24, height: 24)
                    .overlay(
                        Text(String(title.prefix(1)).uppercased())
                            .font(.plusJakarta(size: 11, weight: .bold))
                            .foregroundStyle(badgeColor)
                    )
                Text(title)
                    .font(.plusJakarta(size: 14, weight: .semibold))
                    .foregroundStyle(theme.text)
                    .lineLimit(2)
                Spacer(minLength: 0)
            }
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule().fill(theme.border).frame(height: 4)
                    Capsule().fill(badgeColor.opacity(0.6)).frame(width: geo.size.width * 0.4, height: 4)
                }
            }
            .frame(height: 4)
            if hasAction {
                Text(severity.uppercased().contains("HIGH") ? "Escalate →" : "Send reminder →")
                    .font(.plusJakarta(size: 12, weight: .bold))
                    .foregroundStyle(OpsColors.linkBlue)
            }
        }
        .padding(16)
        .background(theme.card)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(badgeColor.opacity(0.35)))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}

struct OpsIntelligenceSection: View {
    let theme: BusinessActiveTheme

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            VStack(alignment: .leading, spacing: 6) {
                Text("Operations Intelligence")
                    .font(.plusJakarta(size: 14, weight: .semibold))
                    .foregroundStyle(theme.text)
                RoundedRectangle(cornerRadius: 1)
                    .fill(LinearGradient(colors: [OpsColors.lavender, OpsColors.indigo, OpsColors.lavender], startPoint: .leading, endPoint: .trailing))
                    .frame(width: 120, height: 2)
                Text("AI-powered insights based on your operations data")
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(theme.muted)
            }
            ForEach(["Cost Optimization", "Vendor Pattern"], id: \.self) { title in
                HStack(spacing: 12) {
                    RoundedRectangle(cornerRadius: 6)
                        .fill(OpsColors.lavender.opacity(0.08))
                        .frame(width: 28, height: 24)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(title)
                            .font(.plusJakarta(size: 13, weight: .bold))
                            .foregroundStyle(theme.text)
                        Text("Insights unavailable until operations pulse projects signals.")
                            .font(.plusJakarta(size: 12))
                            .foregroundStyle(theme.secondary)
                    }
                }
                .padding(16)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(theme.card)
                .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08)))
                .clipShape(RoundedRectangle(cornerRadius: 16))
            }
        }
    }
}

struct OpsTimelineHeroCard: View {
    let entries: Int
    let vendors: Int
    let issues: Int
    let theme: BusinessActiveTheme

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Operations Timeline")
                .font(.plusJakarta(size: 18, weight: .bold))
                .foregroundStyle(theme.text)
            Text("Live ops events from spend, vendors, issues, and updates.")
                .font(.plusJakarta(size: 12))
                .foregroundStyle(theme.secondary)
            HStack(spacing: 8) {
                heroStat("\(entries)", "ENTRIES")
                heroStat("\(vendors)", "VENDORS")
                heroStat("\(issues)", "ISSUES")
            }
        }
        .padding(20)
        .background(LinearGradient(colors: [Color(hex: "#161B26"), Color(hex: "#1A1F2E")], startPoint: .topLeading, endPoint: .bottomTrailing))
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(OpsColors.indigoLight.opacity(0.2)))
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }

    private func heroStat(_ value: String, _ label: String) -> some View {
        VStack(spacing: 4) {
            Text(value)
                .font(.plusJakarta(size: 20, weight: .heavy))
                .foregroundStyle(theme.text)
            Text(label)
                .font(.plusJakarta(size: 9, weight: .bold))
                .foregroundStyle(theme.muted)
        }
        .frame(maxWidth: .infinity)
        .padding(12)
        .background(OpsColors.indigoLight.opacity(0.06))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(OpsColors.indigoLight.opacity(0.12)))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

struct OpsProgressSnapshot: View {
    let budgetRatio: CGFloat?
    let issuesRatio: CGFloat?
    let milestonesRatio: CGFloat?
    let theme: BusinessActiveTheme

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Progress Snapshot")
                    .font(.plusJakarta(size: 14, weight: .semibold))
                    .foregroundStyle(theme.text)
                Spacer()
                Text("This Quarter")
                    .font(.plusJakarta(size: 10, weight: .bold))
                    .foregroundStyle(theme.secondary)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .overlay(Capsule().stroke(theme.border))
            }
            OpsSparklinePlaceholder()
            HStack(spacing: 10) {
                gauge("Budget", budgetRatio)
                gauge("Issues", issuesRatio)
                gauge("Milestones", milestonesRatio)
            }
        }
        .padding(16)
        .background(theme.card)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(theme.border))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func gauge(_ label: String, _ ratio: CGFloat?) -> some View {
        VStack(spacing: 6) {
            ZStack {
                Circle().stroke(theme.border.opacity(0.45), lineWidth: 6).frame(width: 56, height: 56)
                Circle()
                    .trim(from: 0, to: ratio ?? 0)
                    .stroke(ratio == nil ? theme.border : theme.accent, style: StrokeStyle(lineWidth: 6, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                    .frame(width: 56, height: 56)
                Text(ratio.map { "\(Int($0 * 100))%" } ?? "—")
                    .font(.plusJakarta(size: 11, weight: .heavy))
                    .foregroundStyle(theme.text)
            }
            Text(label)
                .font(.plusJakarta(size: 11, weight: .semibold))
                .foregroundStyle(theme.secondary)
        }
        .frame(maxWidth: .infinity)
    }
}

struct OpsSparklinePlaceholder: View {
    var body: some View {
        Path { path in
            path.move(to: CGPoint(x: 0, y: 28))
            path.addLine(to: CGPoint(x: 60, y: 20))
            path.addLine(to: CGPoint(x: 120, y: 24))
            path.addLine(to: CGPoint(x: 180, y: 14))
            path.addLine(to: CGPoint(x: 240, y: 18))
            path.addLine(to: CGPoint(x: 300, y: 12))
        }
        .stroke(OpsColors.lavender.opacity(0.35), style: StrokeStyle(lineWidth: 2, lineCap: .round))
        .frame(height: 40)
        .frame(maxWidth: .infinity)
    }
}

struct OpsMemoryHeroSection: View {
    let ringLabel: String
    let learnings: String
    let patterns: String
    let accuracy: String
    let showLive: Bool
    let theme: BusinessActiveTheme

    var body: some View {
        HStack(spacing: 16) {
            OpsHeroHealthRing(score: ringLabel, showLive: showLive, ringSize: 130, theme: theme)
            VStack(alignment: .leading, spacing: 8) {
                Text("OPERATIONS MEMORY")
                    .font(.plusJakarta(size: 10, weight: .bold))
                    .foregroundStyle(theme.muted)
                Text(ringLabel == "—" ? "Awaiting signal" : "Optimizing")
                    .font(.plusJakarta(size: 18, weight: .bold))
                    .foregroundStyle(theme.text)
                HStack(spacing: 12) {
                    inlineStat(learnings, "LEARNINGS")
                    inlineStat(patterns, "PATTERNS")
                    inlineStat(accuracy, "ACCURACY")
                }
            }
        }
        .padding(20)
        .background(LinearGradient(colors: [Color(hex: "#161B26"), Color(hex: "#1A1F2E")], startPoint: .topLeading, endPoint: .bottomTrailing))
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(theme.border))
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }

    private func inlineStat(_ value: String, _ label: String) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(value)
                .font(.plusJakarta(size: 16, weight: .heavy))
                .foregroundStyle(theme.text)
            Text(label)
                .font(.plusJakarta(size: 9, weight: .bold))
                .foregroundStyle(theme.muted)
        }
    }
}

struct OpsBiggestLearningCard: View {
    let quote: String?
    let theme: BusinessActiveTheme

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Biggest Learning")
                .font(.plusJakarta(size: 14, weight: .semibold))
                .foregroundStyle(theme.text)
            HStack(spacing: 0) {
                Rectangle()
                    .fill(OpsColors.lavender)
                    .frame(width: 4)
                Text(quote ?? "Your top ops learning appears here once memories are recorded.")
                    .font(.plusJakarta(size: quote != nil ? 14 : 12, weight: quote != nil ? .semibold : .regular))
                    .italic(quote != nil)
                    .foregroundStyle(quote != nil ? theme.text : theme.secondary)
                    .padding(16)
            }
            .background(theme.card)
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(theme.border))
            .clipShape(RoundedRectangle(cornerRadius: 16))
        }
    }
}

struct OpsScopeDropdown: View {
    let label: String
    let theme: BusinessActiveTheme

    var body: some View {
        HStack(spacing: 8) {
            Text(label)
                .font(.plusJakarta(size: 13, weight: .semibold))
                .foregroundStyle(theme.text)
            Text("▾")
                .font(.plusJakarta(size: 12))
                .foregroundStyle(theme.secondary)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
        .background(theme.card)
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(theme.border))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}
