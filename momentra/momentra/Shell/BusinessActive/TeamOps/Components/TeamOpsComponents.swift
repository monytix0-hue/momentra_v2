import SwiftUI

enum TeamOpsColors {
    static let emerald = Color(hex: "#10B981")
    static let emeraldDark = Color(hex: "#059669")
    static let emeraldLight = Color(hex: "#34D399")
    static let indigo = Color(hex: "#6366F1")
    static let indigoLight = Color(hex: "#818CF8")
    static let lavender = Color(hex: "#A78BFA")
    static let amber = Color(hex: "#F59E0B")
    static let red = Color(hex: "#EF4444")
    static let linkBlue = Color(hex: "#818CF8")
    static let ctaText = Color(hex: "#0C0F15")
    static let dayMuted = Color(hex: "#475569")
}

struct TeamOpsHeroHealthRing: View {
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
                    .stroke(TeamOpsColors.emerald.opacity(0.25), lineWidth: 10)
                    .frame(width: ringSize, height: ringSize)
                Circle()
                    .trim(from: 0, to: fraction)
                    .stroke(TeamOpsColors.emerald, style: StrokeStyle(lineWidth: 10, lineCap: .round))
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
                    Circle().fill(TeamOpsColors.emerald).frame(width: 5, height: 5)
                    Text("LIVE")
                        .font(.plusJakarta(size: 8, weight: .bold))
                        .foregroundStyle(TeamOpsColors.emerald)
                }
                .padding(.horizontal, 6)
                .padding(.vertical, 2)
                .background(TeamOpsColors.emerald.opacity(0.12))
                .overlay(Capsule().stroke(TeamOpsColors.emerald.opacity(0.2)))
                .clipShape(Capsule())
                .offset(y: -6)
            }
        }
        .frame(width: ringSize, height: ringSize)
    }
}

struct TeamOpsTintedMetricTile: View {
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

struct TeamOpsFilterChipRow: View {
    let chips: [String]
    let selected: String
    let onSelect: (String) -> Void
    let theme: BusinessActiveTheme

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(chips, id: \.self) { chip in
                    let isSelected = selected == chip
                    Text(chip)
                        .font(.plusJakarta(size: 12, weight: .bold))
                        .foregroundStyle(isSelected ? TeamOpsColors.ctaText : theme.secondary)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 7)
                        .background(
                            Group {
                                if isSelected {
                                    LinearGradient(
                                        colors: [TeamOpsColors.emeraldLight, TeamOpsColors.emerald],
                                        startPoint: .leading,
                                        endPoint: .trailing
                                    )
                                } else {
                                    theme.card
                                }
                            }
                        )
                        .overlay(
                            Capsule().stroke(
                                isSelected ? TeamOpsColors.emerald.opacity(0.4) : theme.border,
                                lineWidth: 1
                            )
                        )
                        .clipShape(Capsule())
                        .onTapGesture { onSelect(chip) }
                }
            }
        }
    }
}

struct TeamOpsGradientPrimaryButton: View {
    let label: String
    var enabled: Bool = true
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(label)
                .font(.plusJakarta(size: 13, weight: .bold))
                .foregroundStyle(.white.opacity(enabled ? 1 : 0.5))
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
                .background(
                    LinearGradient(
                        colors: [TeamOpsColors.emeraldLight, TeamOpsColors.emeraldDark],
                        startPoint: .leading,
                        endPoint: .trailing
                    )
                )
                .clipShape(RoundedRectangle(cornerRadius: 24))
        }
        .disabled(!enabled)
    }
}

struct TeamOpsOutlineButton: View {
    let label: String
    var enabled: Bool = true
    let theme: BusinessActiveTheme
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(label)
                .font(.plusJakarta(size: 13, weight: .bold))
                .foregroundStyle(theme.text.opacity(enabled ? 1 : 0.55))
                .lineLimit(1)
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .frame(maxWidth: .infinity)
                .overlay(RoundedRectangle(cornerRadius: 24).stroke(theme.border))
                .clipShape(RoundedRectangle(cornerRadius: 24))
        }
        .disabled(!enabled)
    }
}

struct TeamOpsDiamondDivider: View {
    let theme: BusinessActiveTheme

    var body: some View {
        HStack {
            Rectangle().fill(theme.border).frame(height: 1)
            RoundedRectangle(cornerRadius: 2)
                .fill(
                    LinearGradient(
                        colors: [TeamOpsColors.emeraldLight, TeamOpsColors.emeraldDark],
                        startPoint: .leading,
                        endPoint: .trailing
                    )
                )
                .frame(width: 8, height: 8)
                .padding(.horizontal, 12)
            Rectangle().fill(theme.border).frame(height: 1)
        }
    }
}

struct TeamOpsWorkloadSection: View {
    let theme: BusinessActiveTheme
    var workloadData: APIClient.BusinessWorkloadPayload? = nil

    private var deptRows: [(name: String, count: Int)] {
        workloadData?.byDepartment.map { ($0.name, $0.count) } ?? []
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                Text("Workload by Department")
                    .font(.plusJakarta(size: 14, weight: .semibold))
                    .foregroundStyle(theme.text)
                Text("Daily workload intensity this week")
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(theme.muted)
            }
            if deptRows.isEmpty {
                Text("Workload heatmap unavailable — department intensity API not mounted.")
                    .font(.plusJakarta(size: 13))
                    .foregroundStyle(theme.secondary)
                ForEach(["Engineering", "Design", "Operations"], id: \.self) { dept in
                    workloadDeptPlaceholder(dept)
                }
            } else {
                ForEach(Array(deptRows.enumerated()), id: \.offset) { _, row in
                    VStack(alignment: .leading, spacing: 6) {
                        HStack {
                            Text(row.name)
                                .font(.plusJakarta(size: 13, weight: .semibold))
                                .foregroundStyle(theme.text)
                            Spacer()
                            Text("\(row.count) open")
                                .font(.plusJakarta(size: 12, weight: .bold))
                                .foregroundStyle(theme.muted)
                        }
                        let intensity = min(max(CGFloat(row.count) / 10, 0), 1)
                        RoundedRectangle(cornerRadius: 2)
                            .fill(TeamOpsColors.emerald.opacity(0.15 + intensity * 0.75))
                            .frame(height: 10)
                    }
                }
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(theme.card)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(theme.border))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func workloadDeptPlaceholder(_ dept: String) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text(dept)
                    .font(.plusJakarta(size: 13, weight: .semibold))
                    .foregroundStyle(theme.text)
                Spacer()
                Text("—")
                    .font(.plusJakarta(size: 12, weight: .bold))
                    .foregroundStyle(theme.muted)
            }
            HStack(spacing: 3) {
                ForEach(0..<7, id: \.self) { _ in
                    RoundedRectangle(cornerRadius: 2)
                        .fill(TeamOpsColors.dayMuted.opacity(0.35))
                        .frame(height: 10)
                }
            }
            HStack {
                ForEach(["M", "T", "W", "T", "F", "S", "S"], id: \.self) { d in
                    Text(d)
                        .font(.plusJakarta(size: 9))
                        .foregroundStyle(theme.muted)
                        .frame(maxWidth: .infinity)
                }
            }
        }
    }
}

struct TeamOpsIntelligenceSection: View {
    let theme: BusinessActiveTheme

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            VStack(alignment: .leading, spacing: 6) {
                Text("Team Intelligence")
                    .font(.plusJakarta(size: 14, weight: .semibold))
                    .foregroundStyle(theme.text)
                LinearGradient(
                    colors: [TeamOpsColors.emeraldLight, TeamOpsColors.emerald, TeamOpsColors.emeraldLight],
                    startPoint: .leading,
                    endPoint: .trailing
                )
                .frame(width: 120, height: 2)
                .clipShape(RoundedRectangle(cornerRadius: 1))
                Text("AI-powered insights based on your team data.")
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(theme.muted)
            }
            ForEach(["Capacity Alert", "Pattern Found"], id: \.self) { title in
                HStack(spacing: 12) {
                    RoundedRectangle(cornerRadius: 6)
                        .fill(TeamOpsColors.lavender.opacity(0.08))
                        .frame(width: 28, height: 28)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(title)
                            .font(.plusJakarta(size: 13, weight: .bold))
                            .foregroundStyle(theme.text)
                        Text("Insights unavailable until team intelligence API projects signals.")
                            .font(.plusJakarta(size: 12))
                            .foregroundStyle(theme.secondary)
                    }
                    Spacer(minLength: 0)
                }
                .padding(16)
                .background(theme.card)
                .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08)))
                .clipShape(RoundedRectangle(cornerRadius: 16))
            }
        }
    }
}

struct TeamOpsTimelineHeroCard: View {
    let members: String
    let pending: String
    let issues: String
    let theme: BusinessActiveTheme

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("TEAM OPERATIONS • MOMENTS")
                .font(.plusJakarta(size: 10, weight: .bold))
                .foregroundStyle(TeamOpsColors.indigoLight)
            Text("Team Timeline")
                .font(.plusJakarta(size: 22, weight: .heavy))
                .foregroundStyle(theme.text)
            Text("Track milestones, decisions, and what your team shipped.")
                .font(.plusJakarta(size: 13))
                .foregroundStyle(theme.secondary)
            HStack(spacing: 8) {
                statChip(members, "MEMBERS", "from pulse", theme.text)
                statChip(pending, "PENDING", "needs review", TeamOpsColors.amber)
                statChip(issues, "ISSUES", "this week", TeamOpsColors.red)
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            LinearGradient(colors: [Color(hex: "#161B26"), Color(hex: "#1A1F2E")], startPoint: .topLeading, endPoint: .bottomTrailing)
        )
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(TeamOpsColors.emerald.opacity(0.25)))
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }

    private func statChip(_ value: String, _ label: String, _ detail: String, _ valueColor: Color) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(value)
                .font(.plusJakarta(size: 16, weight: .heavy))
                .foregroundStyle(valueColor)
            Text(label)
                .font(.plusJakarta(size: 9, weight: .bold))
                .foregroundStyle(theme.muted)
            Text(detail)
                .font(.plusJakarta(size: 10))
                .foregroundStyle(theme.muted)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(10)
        .background(theme.card)
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(theme.border))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

struct TeamOpsProgressSnapshot: View {
    let deliveryRatio: CGFloat?
    let capacityRatio: CGFloat?
    let approvalsRatio: CGFloat?
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
            Text("How your team is performing this quarter.")
                .font(.plusJakarta(size: 11))
                .foregroundStyle(theme.muted)
            HStack(spacing: 12) {
                miniGauge("Delivery", deliveryRatio, TeamOpsColors.indigoLight)
                miniGauge("Capacity", capacityRatio, TeamOpsColors.emerald)
                miniGauge("Approvals", approvalsRatio, TeamOpsColors.amber)
            }
            Text("Trend unavailable")
                .font(.plusJakarta(size: 11))
                .foregroundStyle(theme.muted)
                .frame(maxWidth: .infinity)
                .frame(height: 36)
                .background(theme.border.opacity(0.4))
                .clipShape(RoundedRectangle(cornerRadius: 8))
        }
        .padding(16)
        .background(theme.card)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(theme.border))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func miniGauge(_ label: String, _ ratio: CGFloat?, _ color: Color) -> some View {
        let pct = ratio.map { "\(Int($0 * 100))%" } ?? "—"
        return VStack(spacing: 6) {
            ZStack {
                Circle()
                    .stroke(color.opacity(0.2), lineWidth: 6)
                    .frame(width: 56, height: 56)
                Circle()
                    .trim(from: 0, to: ratio ?? 0)
                    .stroke(color, style: StrokeStyle(lineWidth: 6, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                    .frame(width: 56, height: 56)
                Text(pct)
                    .font(.plusJakarta(size: 11, weight: .bold))
                    .foregroundStyle(theme.text)
            }
            Text(label)
                .font(.plusJakarta(size: 10, weight: .semibold))
                .foregroundStyle(theme.muted)
        }
        .frame(maxWidth: .infinity)
    }
}

struct TeamOpsMemoryHeroSection: View {
    let ringLabel: String
    let learnings: String
    let patterns: String
    let accuracy: String
    let showLive: Bool
    let theme: BusinessActiveTheme

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(spacing: 16) {
                TeamOpsHeroHealthRing(score: ringLabel, showLive: showLive, ringSize: 130, theme: theme)
                VStack(alignment: .leading, spacing: 8) {
                    Text("Team Memory")
                        .font(.plusJakarta(size: 10, weight: .bold))
                        .foregroundStyle(theme.muted)
                    Text(ringLabel == "—" ? "Awaiting signal" : "Growing")
                        .font(.plusJakarta(size: 18, weight: .bold))
                        .foregroundStyle(theme.text)
                    Text(showLive ? "Trends vs benchmarks: live learnings" : "Record learnings to grow memory")
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(theme.secondary)
                }
            }
            HStack(spacing: 8) {
                TeamOpsTintedMetricTile(value: learnings, label: "patterns", detail: "discovered", tint: TeamOpsColors.lavender, theme: theme)
                TeamOpsTintedMetricTile(value: patterns, label: "active rules", detail: "playbook", tint: TeamOpsColors.indigoLight, theme: theme)
                TeamOpsTintedMetricTile(value: accuracy, label: "accuracy", detail: "rate", tint: TeamOpsColors.emerald, theme: theme)
            }
        }
        .padding(20)
        .background(
            LinearGradient(colors: [Color(hex: "#161B26"), Color(hex: "#1A1F2E")], startPoint: .topLeading, endPoint: .bottomTrailing)
        )
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(theme.border))
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }
}

struct TeamOpsEmptyAiCard: View {
    let title: String
    let emptyCopy: String
    let theme: BusinessActiveTheme

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.plusJakarta(size: 14, weight: .semibold))
                .foregroundStyle(theme.text)
            Text(emptyCopy)
                .font(.plusJakarta(size: 13))
                .foregroundStyle(theme.secondary)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(theme.card)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(theme.border))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}
