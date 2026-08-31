import SwiftUI

enum CompanyLifeColors {
    static let bg = Color(hex: "#0C0F15")
    static let card = Color(hex: "#161B26")
    static let border = Color(hex: "#1E293B")
    static let text = Color(hex: "#E5E0EE")
    static let secondary = Color(hex: "#94A3B8")
    static let muted = Color(hex: "#64748B")
    static let indigo = Color(hex: "#818CF8")
    static let indigoSolid = Color(hex: "#6366F1")
    static let lavender = Color(hex: "#A78BFA")
    static let team = Color(hex: "#10B981")
    static let runway = Color(hex: "#F59E0B")
    static let ops = Color(hex: "#A78BFA")
    static let red = Color(hex: "#EF4444")
    static let watch = Color(hex: "#F59E0B")
}

enum CompanyLifeFilter: String, CaseIterable, Identifiable {
    case all, team, runway, ops
    var id: String { rawValue }
    var label: String {
        switch self {
        case .all: return "All Modules"
        case .team: return "Team Ops"
        case .runway: return "Runway"
        case .ops: return "Biz Ops"
        }
    }
    var familyKey: String? {
        switch self {
        case .all: return nil
        case .team: return "TEAM_OPS"
        case .runway: return "RUNWAY"
        case .ops: return "OPERATIONS"
        }
    }
    var accent: Color {
        switch self {
        case .all: return CompanyLifeColors.indigo
        case .team: return CompanyLifeColors.team
        case .runway: return CompanyLifeColors.runway
        case .ops: return CompanyLifeColors.lavender
        }
    }
}

func companyLifeFamilyColor(_ family: String?) -> Color {
    let f = (family ?? "").uppercased()
    if f.contains("TEAM") { return CompanyLifeColors.team }
    if f.contains("RUNWAY") { return CompanyLifeColors.runway }
    return CompanyLifeColors.ops
}

struct CompanyLifeFilterChips: View {
    @Binding var selected: CompanyLifeFilter

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(CompanyLifeFilter.allCases) { filter in
                    Button {
                        selected = filter
                    } label: {
                        HStack(spacing: 6) {
                            Circle().fill(filter.accent).frame(width: 6, height: 6)
                            Text(filter.label)
                                .font(.plusJakarta(size: 11, weight: .semibold))
                                .foregroundStyle(filter.accent)
                        }
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(filter.accent.opacity(selected == filter ? 0.12 : 0.06))
                        .overlay(
                            Capsule().stroke(filter.accent.opacity(selected == filter ? 0.35 : 0.2))
                        )
                        .clipShape(Capsule())
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }
}

struct CompanyLifeHealthRing: View {
    let score: String
    var ringSize: CGFloat = 106

    private var fraction: CGFloat {
        guard let n = Double(score), n > 0 else { return 0 }
        return CGFloat(min(max(n / 100, 0), 1))
    }

    var body: some View {
        ZStack {
            Circle()
                .stroke(CompanyLifeColors.indigo.opacity(0.25), lineWidth: 10)
                .frame(width: ringSize, height: ringSize)
            Circle()
                .trim(from: 0, to: fraction)
                .stroke(CompanyLifeColors.indigo, style: StrokeStyle(lineWidth: 10, lineCap: .round))
                .rotationEffect(.degrees(-90))
                .frame(width: ringSize, height: ringSize)
            VStack(spacing: 0) {
                Text(score)
                    .font(.plusJakarta(size: 28, weight: .heavy))
                    .foregroundStyle(CompanyLifeColors.text)
                if score != "—" {
                    Text("/100")
                        .font(.plusJakarta(size: 12, weight: .semibold))
                        .foregroundStyle(CompanyLifeColors.muted)
                }
            }
        }
        .frame(width: ringSize, height: ringSize)
    }
}

struct CompanyLifeHealthHeader: View {
    let score: String
    let narrative: String
    let subtitle: String
    let activeModules: String
    let totalMoments: String
    let avgRunway: String

    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            VStack(alignment: .leading, spacing: 4) {
                Text("BUSINESS LIFE")
                    .font(.plusJakarta(size: 10, weight: .bold))
                    .foregroundStyle(CompanyLifeColors.indigo)
                Text("Your business, unified")
                    .font(.plusJakarta(size: 22, weight: .bold))
                    .foregroundStyle(CompanyLifeColors.text)
                Text("Health across team operations, financial runway, and business operations.")
                    .font(.plusJakarta(size: 13))
                    .foregroundStyle(CompanyLifeColors.secondary)
            }
            HStack(alignment: .center, spacing: 16) {
                CompanyLifeHealthRing(score: score)
                VStack(alignment: .leading, spacing: 4) {
                    Text(narrative)
                        .font(.plusJakarta(size: 18, weight: .bold))
                        .foregroundStyle(CompanyLifeColors.text)
                    Text(subtitle)
                        .font(.plusJakarta(size: 13))
                        .foregroundStyle(CompanyLifeColors.secondary)
                }
            }
            Rectangle().fill(CompanyLifeColors.border).frame(height: 1)
            HStack {
                statCell("Active Modules", activeModules)
                Spacer()
                statCell("Total Moments", totalMoments)
                Spacer()
                statCell("Avg Runway", avgRunway)
            }
        }
        .padding(20)
        .background(CompanyLifeColors.card)
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(CompanyLifeColors.border))
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }

    private func statCell(_ label: String, _ value: String) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(label.uppercased())
                .font(.plusJakarta(size: 10, weight: .bold))
                .foregroundStyle(CompanyLifeColors.muted)
            Text(value)
                .font(.plusJakarta(size: 14, weight: .heavy))
                .foregroundStyle(CompanyLifeColors.text)
        }
    }
}

struct CompanyLifeModuleCards: View {
    let team: APIClient.BusinessLifePayload.LifeInner.LifeModuleCard?
    let runway: APIClient.BusinessLifePayload.LifeInner.LifeModuleCard?
    let ops: APIClient.BusinessLifePayload.LifeInner.LifeModuleCard?
    var vendor: APIClient.BusinessLifePayload.LifeInner.LifeModuleCard? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Active Business Systems")
                .font(.plusJakarta(size: 14, weight: .semibold))
                .foregroundStyle(CompanyLifeColors.text)
            HStack(alignment: .top, spacing: 8) {
                moduleCard(
                    title: "Team Operations",
                    accent: CompanyLifeColors.team,
                    card: team,
                    fallback: "Delivery Command"
                )
                moduleCard(
                    title: "Business Runway",
                    accent: CompanyLifeColors.runway,
                    card: runway,
                    fallback: "Runway",
                    runwayMonths: runway?.runwayMonths
                )
                moduleCard(
                    title: "Business Operations",
                    accent: CompanyLifeColors.ops,
                    card: ops,
                    fallback: "Control Center"
                )
            }
            if vendor?.active == true {
                moduleCard(
                    title: "Vendor Operations",
                    accent: CompanyLifeColors.ops,
                    card: vendor,
                    fallback: "Vendor health"
                )
            }
        }
    }

    private func moduleCard(
        title: String,
        accent: Color,
        card: APIClient.BusinessLifePayload.LifeInner.LifeModuleCard?,
        fallback: String,
        runwayMonths: String? = nil
    ) -> some View {
        let active = card?.active == true
        let subtitle: String = {
            if !active { return "Not activated" }
            if let m = runwayMonths, !m.isEmpty { return "\(m) months" }
            if let s = card?.statusLabel, !s.isEmpty { return s }
            return fallback
        }()
        let score = active ? (card?.score?.nilIfEmpty ?? "—") : "—"
        let chip: String = {
            if !active { return "Inactive" }
            return card?.statusLabel?.nilIfEmpty ?? "Active"
        }()
        return VStack(alignment: .leading, spacing: 10) {
            VStack(alignment: .leading, spacing: 2) {
                Text(title.uppercased())
                    .font(.plusJakarta(size: 10, weight: .bold))
                    .foregroundStyle(accent)
                    .lineLimit(2)
                Text(subtitle)
                    .font(.plusJakarta(size: 9, weight: .bold))
                    .foregroundStyle(accent.opacity(0.85))
                    .lineLimit(1)
            }
            HStack {
                Text(score)
                    .font(.plusJakarta(size: 18, weight: .heavy))
                    .foregroundStyle(CompanyLifeColors.text)
                Spacer(minLength: 4)
                Text(chip)
                    .font(.plusJakarta(size: 9, weight: .bold))
                    .foregroundStyle(accent)
                    .lineLimit(1)
            }
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(accent.opacity(0.04))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(accent.opacity(0.12)))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

struct CompanyLifeSignalsSection: View {
    let signals: [APIClient.BusinessLifePayload.LifeInner.LifeSignal]

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Cross-Module Signals")
                    .font(.plusJakarta(size: 14, weight: .semibold))
                    .foregroundStyle(CompanyLifeColors.text)
                Spacer()
                Text(signals.isEmpty ? "0 signals" : "\(signals.count) signals")
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(CompanyLifeColors.secondary)
            }
            if signals.isEmpty {
                emptyCard("No signals yet")
            } else {
                VStack(spacing: 8) {
                    ForEach(signals) { signal in
                        signalCard(signal)
                    }
                }
            }
        }
    }

    private func signalCard(_ signal: APIClient.BusinessLifePayload.LifeInner.LifeSignal) -> some View {
        let familyColor = companyLifeFamilyColor(signal.family)
        let status = signal.statusLabel ?? "Watch"
        let statusColor: Color = {
            switch status.uppercased() {
            case "HEALTHY": return CompanyLifeColors.team
            case "ACTION": return CompanyLifeColors.red
            default: return CompanyLifeColors.watch
            }
        }()
        return VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text(familyLabel(signal.family))
                    .font(.plusJakarta(size: 9, weight: .heavy))
                    .foregroundStyle(familyColor)
                    .padding(.horizontal, 6)
                    .padding(.vertical, 3)
                    .background(familyColor.opacity(0.12))
                    .clipShape(RoundedRectangle(cornerRadius: 4))
                Spacer()
                Text(status)
                    .font(.plusJakarta(size: 9, weight: .heavy))
                    .foregroundStyle(statusColor)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 2)
                    .background(statusColor.opacity(0.1))
                    .clipShape(RoundedRectangle(cornerRadius: 4))
            }
            Text(signal.title)
                .font(.plusJakarta(size: 14, weight: .semibold))
                .foregroundStyle(CompanyLifeColors.text)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(CompanyLifeColors.card)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(CompanyLifeColors.border))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}

struct CompanyLifeActivitySection: View {
    let items: [APIClient.BusinessLifePayload.LifeInner.LifeActivity]

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Live Activity Feed")
                .font(.plusJakarta(size: 14, weight: .semibold))
                .foregroundStyle(CompanyLifeColors.text)
            VStack(alignment: .leading, spacing: 16) {
                if items.isEmpty {
                    Text("No activity yet")
                        .font(.plusJakarta(size: 13))
                        .foregroundStyle(CompanyLifeColors.secondary)
                } else {
                    ForEach(items) { item in
                        activityRow(item)
                    }
                }
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(CompanyLifeColors.card)
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(CompanyLifeColors.border))
            .clipShape(RoundedRectangle(cornerRadius: 16))
        }
    }

    private func activityRow(_ item: APIClient.BusinessLifePayload.LifeInner.LifeActivity) -> some View {
        let accent = companyLifeFamilyColor(item.family)
        return HStack(alignment: .top, spacing: 12) {
            VStack(spacing: 4) {
                Circle().fill(accent).frame(width: 10, height: 10)
                Rectangle().fill(CompanyLifeColors.border).frame(width: 2, height: 36)
            }
            VStack(alignment: .leading, spacing: 4) {
                HStack(alignment: .top) {
                    Text(item.title.isEmpty ? item.activityCode : item.title)
                        .font(.plusJakarta(size: 13, weight: .bold))
                        .foregroundStyle(CompanyLifeColors.text)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    Text(formatLifeDate(item.occurredAt))
                        .font(.plusJakarta(size: 10))
                        .foregroundStyle(CompanyLifeColors.secondary)
                }
                Text(item.description?.nilIfEmpty ?? "\(familyDisplay(item.family)) · \(item.activityCode)")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(CompanyLifeColors.secondary)
            }
        }
    }
}

struct CompanyLifeJourneySection: View {
    let steps: [APIClient.BusinessLifePayload.LifeInner.LifeJourney]

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Business Journey")
                .font(.plusJakarta(size: 14, weight: .semibold))
                .foregroundStyle(CompanyLifeColors.text)
            VStack(alignment: .leading, spacing: 18) {
                if steps.isEmpty {
                    Text("No modules activated yet")
                        .font(.plusJakarta(size: 13))
                        .foregroundStyle(CompanyLifeColors.secondary)
                } else {
                    ForEach(steps) { step in
                        HStack(alignment: .top, spacing: 12) {
                            Circle()
                                .fill(companyLifeFamilyColor(step.family))
                                .frame(width: 10, height: 10)
                                .padding(.top, 4)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(journeyTitle(step))
                                    .font(.plusJakarta(size: 13, weight: .bold))
                                    .foregroundStyle(CompanyLifeColors.text)
                                Text(formatLifeDateLong(step.createdAt))
                                    .font(.plusJakarta(size: 11))
                                    .foregroundStyle(CompanyLifeColors.secondary)
                            }
                        }
                    }
                }
            }
            .padding(18)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(CompanyLifeColors.card)
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(CompanyLifeColors.border))
            .clipShape(RoundedRectangle(cornerRadius: 16))
        }
    }
}

struct CompanyLifeTrendsSection: View {
    let trends: APIClient.BusinessLifePayload.LifeInner.LifeTrends?

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Health Trends (6-Month)")
                .font(.plusJakarta(size: 14, weight: .semibold))
                .foregroundStyle(CompanyLifeColors.text)
            VStack(alignment: .leading, spacing: 8) {
                let series = trends?.series ?? []
                if series.isEmpty {
                    Text("Trend history builds as monthly snapshots are recorded.")
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(CompanyLifeColors.secondary)
                } else {
                    ForEach(series) { point in
                        HStack {
                            Text(point.month)
                                .font(.plusJakarta(size: 12))
                                .foregroundStyle(CompanyLifeColors.secondary)
                            Spacer()
                            Text(point.financialHealthScore.map(String.init) ?? "—")
                                .font(.plusJakarta(size: 12, weight: .bold))
                                .foregroundStyle(CompanyLifeColors.text)
                        }
                    }
                }
            }
            .padding(14)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(CompanyLifeColors.card)
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(CompanyLifeColors.border))
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
    }
}

struct CompanyLifeTrendsDeferred: View {
    var body: some View {
        CompanyLifeTrendsSection(trends: nil)
    }
}

struct CompanyLifeGradientButton: View {
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
                .background(
                    LinearGradient(
                        colors: [CompanyLifeColors.indigo, CompanyLifeColors.indigoSolid],
                        startPoint: .leading,
                        endPoint: .trailing
                    )
                )
                .clipShape(RoundedRectangle(cornerRadius: 24))
        }
        .disabled(!enabled)
        .buttonStyle(.plain)
    }
}

struct CompanyLifeOutlineButton: View {
    let label: String
    let enabled: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(label)
                .font(.plusJakarta(size: 13, weight: .bold))
                .foregroundStyle(CompanyLifeColors.text.opacity(enabled ? 1 : 0.45))
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
                .overlay(RoundedRectangle(cornerRadius: 24).stroke(CompanyLifeColors.border))
                .clipShape(RoundedRectangle(cornerRadius: 24))
        }
        .disabled(!enabled)
        .buttonStyle(.plain)
    }
}

private func emptyCard(_ message: String) -> some View {
    Text(message)
        .font(.plusJakarta(size: 13))
        .foregroundStyle(CompanyLifeColors.secondary)
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(CompanyLifeColors.card)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(CompanyLifeColors.border))
        .clipShape(RoundedRectangle(cornerRadius: 16))
}

private func familyLabel(_ family: String?) -> String {
    let f = (family ?? "").uppercased()
    if f.contains("TEAM") { return "TEAM OPS" }
    if f.contains("RUNWAY") { return "RUNWAY" }
    return "OPERATIONS"
}

private func familyDisplay(_ family: String?) -> String {
    let f = (family ?? "").uppercased()
    if f.contains("TEAM") { return "Team Ops" }
    if f.contains("RUNWAY") { return "Runway" }
    return "Operations"
}

private func journeyTitle(_ step: APIClient.BusinessLifePayload.LifeInner.LifeJourney) -> String {
    switch step.familyCode.uppercased() {
    case "TEAM_OPERATIONS": return "Team Operations activated"
    case "BUSINESS_RUNWAY": return "Business Runway launched"
    case "BUSINESS_OPERATIONS": return "Operations module added"
    default:
        return step.title.isEmpty ? "\(familyDisplay(step.family)) activated" : step.title
    }
}

private func formatLifeDate(_ iso: String) -> String {
    guard iso.count >= 10 else { return iso }
    let parts = iso.prefix(10).split(separator: "-").map(String.init)
    guard parts.count == 3, let m = Int(parts[1]), m >= 1, m <= 12 else { return String(iso.prefix(10)) }
    let months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"]
    let day = Int(parts[2]) ?? 0
    return "\(day) \(months[m - 1])"
}

private func formatLifeDateLong(_ iso: String) -> String {
    guard iso.count >= 10 else { return iso }
    let parts = iso.prefix(10).split(separator: "-").map(String.init)
    guard parts.count == 3, let m = Int(parts[1]), m >= 1, m <= 12 else { return String(iso.prefix(10)) }
    let months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"]
    let day = Int(parts[2]) ?? 0
    return "\(day) \(months[m - 1]) \(parts[0])"
}

private extension String {
    var nilIfEmpty: String? { isEmpty ? nil : self }
}
