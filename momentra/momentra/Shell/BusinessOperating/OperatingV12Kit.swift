import SwiftUI

/// Shared TRD/DST v12 widget kit — mirrors Android `OperatingV12Kit.kt`.
enum OperatingV12 {
    static let pulseSurface = Color(hex: "#111827")
    static let surface = Color(hex: "#151D2E")
    static let title = Color(hex: "#F1F5F9")
    static let sectionTitle = Color(hex: "#F8FAFC")
    static let subtitle = Color(hex: "#94A3B8")
    static let viewAll = Color(hex: "#34D399")
    static let cyan = Color(hex: "#06B6D4")
    static let green = Color(hex: "#22C55E")
    static let amber = Color(hex: "#F59E0B")
    static let red = Color(hex: "#EF4444")
    static let deepInk = Color(hex: "#04110C")
    static let hairline = Color.white.opacity(0.06)
    static let inset = Color.black.opacity(0.28)
    static let row = Color.black.opacity(0.24)

    private static let inflowKeys: Set<String> = [
        "progress", "toCollect", "collected", "sales", "netSales", "committed", "repeat", "delivered",
    ]
    private static let flightKeys: Set<String> = [
        "outstanding", "credits", "tax", "net", "inTransit", "months", "records",
    ]
    private static let pressureKeys: Set<String> = [
        "payable", "toPay", "difference", "issues", "overdue", "low", "returns",
    ]

    static func tone(_ tone: String?, key: String, accent: Color) -> Color {
        if inflowKeys.contains(key) { return accent }
        if flightKeys.contains(key) { return cyan }
        if pressureKeys.contains(key) { return red }
        switch (tone ?? "NORMAL").uppercased() {
        case "POSITIVE": return accent
        case "WARNING": return amber
        default: return title
        }
    }

    static func bar(_ badge: String?) -> Color {
        switch badge {
        case "Overdue", "Cancelled", "Rejected": return red
        case "Unavailable", "Part", "Due", "On hold", "Transit", "Open order": return amber
        case "Settled", "Done", "Delivered", "Alert", "Open", "Pattern": return green
        default: return cyan
        }
    }
}

struct OperatingV12Theme {
    let accent: Color
    let canvas: Color
    let heroStart: Color
    let heroEnd: Color
    let label: String
    let partyNoun: String
    let motionLine: String
    let isTrading: Bool
    let verticalKey: String
    var surface: Color = OperatingV12.pulseSurface
    var canvasTop: Color? = nil
    var inset: Color = OperatingV12.inset
    var softLink: Color = OperatingV12.viewAll

    var isManufacturing: Bool { verticalKey == "manufacturing" || verticalKey == "madeToOrder" }
    var isMadeToOrder: Bool { verticalKey == "madeToOrder" }

    static func trading() -> OperatingV12Theme { tradingForScreen(.pulse) }

    static func tradingForScreen(_ tab: OperatingHubTab) -> OperatingV12Theme {
        let c = BusinessTokens.Colors
        let canvas: Color
        let surface: Color
        let heroStart: Color
        let heroEnd: Color
        switch tab {
        case .pulse:
            canvas = c.tradingPulseCanvas; surface = c.tradingPulseSurface
            heroStart = c.tradingPulseHeroStart; heroEnd = c.tradingPulseHeroEnd
        case .moments:
            canvas = c.tradingMomentsCanvas; surface = c.tradingMomentsSurface
            heroStart = c.tradingMomentsHeroStart; heroEnd = c.tradingMomentsHeroEnd
        case .life:
            canvas = c.tradingLifeCanvas; surface = c.tradingLifeSurface
            heroStart = c.tradingLifeHeroStart; heroEnd = c.tradingLifeHeroEnd
        case .memory:
            canvas = c.tradingMemoryCanvas; surface = c.tradingMemorySurface
            heroStart = c.tradingMemoryHeroStart; heroEnd = c.tradingMemoryHeroEnd
        case .quickAdd:
            canvas = c.tradingQuickAddCanvas; surface = c.tradingQuickAddSurface
            heroStart = c.tradingQuickAddHeroStart; heroEnd = c.tradingQuickAddHeroEnd
        }
        return OperatingV12Theme(
            accent: c.tradingAccent,
            canvas: canvas,
            heroStart: heroStart,
            heroEnd: heroEnd,
            label: "Trading",
            partyNoun: "Buyers",
            motionLine: "Source. Sell. Collect.",
            isTrading: true,
            verticalKey: "trading",
            surface: surface
        )
    }
}

enum OperatingV12Date {
    private static let dayFormatter: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.dateFormat = "yyyy-MM-dd"
        return f
    }()

    private static func parse(_ raw: String?) -> Date? {
        guard let raw, raw.count >= 10 else { return nil }
        return dayFormatter.date(from: String(raw.prefix(10)))
    }

    static func short(_ raw: String?) -> String? {
        guard let d = parse(raw) else { return nil }
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US")
        f.dateFormat = "d MMM"
        return f.string(from: d)
    }

    static func monthYear(_ raw: String?) -> String? {
        guard let d = parse(raw) else { return nil }
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US")
        f.dateFormat = "MMMM yyyy"
        return f.string(from: d)
    }

    static func monthDayYear(_ raw: String?) -> String? {
        guard let d = parse(raw) else { return nil }
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US")
        f.dateFormat = "d MMM yyyy"
        return f.string(from: d)
    }

    static func today() -> String {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US")
        f.dateFormat = "d MMM yyyy"
        return f.string(from: Date())
    }

    static func fiscalYear(_ raw: String?) -> String {
        guard let d = parse(raw) else { return "FY" }
        let cal = Calendar(identifier: .gregorian)
        let y = cal.component(.year, from: d)
        let m = cal.component(.month, from: d)
        let start = m >= 4 ? y : y - 1
        return "FY \(start)–\(String(start + 1).suffix(2))"
    }
}

enum OperatingV12Metric {
    static func value(section: OperatingBoardSectionPayload?, key: String, currency: String?) -> String? {
        guard let metric = section?.metrics?.first(where: { $0.key == key }) else { return nil }
        return OperatingV12Money.amount(metric.value, currency: currency)
    }
}

enum OperatingV12Tiles {
    static func ordered(
        section: OperatingBoardSectionPayload?,
        keys: [String],
        limit: Int,
        currency: String?
    ) -> [OperatingBoardMetricPayload] {
        let metrics = section?.metrics ?? []
        guard !metrics.isEmpty else { return [] }
        var picked: [OperatingBoardMetricPayload] = []
        for key in keys {
            if let m = metrics.first(where: { $0.key == key }) { picked.append(m) }
        }
        if picked.isEmpty { picked = Array(metrics.prefix(limit)) }
        return Array(picked.prefix(limit))
    }
}

extension OperatingProjectionPayload {
    func pulseGroup(_ code: String) -> OperatingPulseMetricGroupPayload? {
        pulseMetrics?.first(where: { $0.code.caseInsensitiveCompare(code) == .orderedSame })
    }

    func sectionByCode(_ code: String) -> OperatingBoardSectionPayload? {
        let all = (boards?.moments ?? []) + (boards?.life ?? []) + (boards?.memory ?? [])
        return all.first(where: { $0.code.caseInsensitiveCompare(code) == .orderedSame })
    }

    func hasUsefulPulseChrome() -> Bool {
        if !(activity ?? []).isEmpty || !(attention ?? []).isEmpty { return true }
        if !(paymentReadiness ?? []).isEmpty { return true }
        if budget?.configured == true { return true }
        if target?.configured == true { return true }
        if gstTracking?.caseInsensitiveCompare("YES") == .orderedSame { return true }
        return false
    }
}

private func v12Font(_ size: CGFloat, weight: Font.Weight = .regular) -> Font {
    let name: String
    switch weight {
    case .black, .heavy: name = "PlusJakartaSans-ExtraBold"
    case .bold: name = "PlusJakartaSans-Bold"
    case .semibold: name = "PlusJakartaSans-SemiBold"
    case .medium: name = "PlusJakartaSans-Medium"
    default: name = "PlusJakartaSans-Regular"
    }
    return Font.custom(name, size: size)
}

struct OperatingV12Card<Content: View>: View {
    let theme: OperatingV12Theme
    var fill: Color = OperatingV12.surface
    var borderOpacity: Double = 0.28
    var radius: CGFloat = 20
    var padding: CGFloat = 16
    var spacing: CGFloat = 14
    @ViewBuilder var content: () -> Content

    var body: some View {
        let glow = theme.isTrading ? 0.18 : 0.32
        VStack(alignment: .leading, spacing: spacing) {
            content()
        }
        .padding(padding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(fill)
        .clipShape(RoundedRectangle(cornerRadius: radius, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: radius, style: .continuous)
                .stroke(theme.accent.opacity(borderOpacity), lineWidth: 1)
        )
        .shadow(color: Color.black.opacity(0.35), radius: 12, y: 6)
        .shadow(color: theme.accent.opacity(glow), radius: 10, y: 4)
    }
}

struct OperatingV12Chip: View {
    let label: String
    var tone: Color = OperatingV12.subtitle
    var dot: Bool = false

    var body: some View {
        HStack(spacing: 6) {
            if dot {
                Circle().fill(tone).frame(width: 6, height: 6)
            }
            Text(label)
                .font(v12Font(11, weight: .semibold))
                .foregroundStyle(tone)
                .lineLimit(1)
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 4)
        .background(tone.opacity(0.16))
        .overlay(Capsule().stroke(tone.opacity(0.35), lineWidth: 1))
        .clipShape(Capsule())
    }
}

struct OperatingV12StatusChip: View {
    let label: String
    var accent: Color = OperatingV12.green

    var body: some View {
        let tone: Color = {
            switch label {
            case "Overdue", "Cancelled", "Rejected": return OperatingV12.red
            case "Unavailable", "Part", "Due", "On hold", "Open order", "Transit": return OperatingV12.amber
            case "Settled", "Done", "Delivered", "Alert", "Open", "Pattern": return accent
            default: return OperatingV12.subtitle
            }
        }()
        Text(label)
            .font(v12Font(10, weight: .semibold))
            .foregroundStyle(tone)
            .padding(.horizontal, 8)
            .padding(.vertical, 3)
            .background(tone.opacity(0.16))
            .clipShape(Capsule())
    }
}

struct OperatingV12WidgetHeader: View {
    let title: String
    let subtitle: String?
    var onViewAll: (() -> Void)? = nil

    var body: some View {
        Button(action: { onViewAll?() }) {
            VStack(alignment: .leading, spacing: 2) {
                HStack {
                    Text(title)
                        .font(v12Font(15, weight: .bold))
                        .foregroundStyle(OperatingV12.sectionTitle)
                    Spacer()
                    if onViewAll != nil {
                        Text("View all")
                            .font(v12Font(11, weight: .semibold))
                            .foregroundStyle(OperatingV12.viewAll)
                    }
                }
                if let subtitle, !subtitle.isEmpty {
                    Text(subtitle)
                        .font(v12Font(11, weight: .medium))
                        .foregroundStyle(OperatingV12.subtitle)
                }
            }
        }
        .buttonStyle(.plain)
        .disabled(onViewAll == nil)
    }
}

struct OperatingV12Tile: View {
    let label: String
    let value: String
    var tone: Color = OperatingV12.title
    var fill: Color = OperatingV12.inset
    var labelTone: Color = OperatingV12.subtitle

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(v12Font(10, weight: .medium))
                .foregroundStyle(labelTone)
                .lineLimit(1)
            Text(value)
                .font(v12Font(14, weight: .bold))
                .foregroundStyle(tone)
                .lineLimit(1)
                .minimumScaleFactor(0.7)
        }
        .padding(10)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(fill)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

struct OperatingV12TileGrid: View {
    let theme: OperatingV12Theme
    let metrics: [OperatingBoardMetricPayload]
    var columns: Int = 3
    var currency: String? = nil
    var fill: Color = OperatingV12.inset

    var body: some View {
        if !metrics.isEmpty {
            let width = max(columns, 1)
            let rows = stride(from: 0, to: metrics.count, by: width).map {
                Array(metrics[$0..<min($0 + width, metrics.count)])
            }
            VStack(spacing: 8) {
                ForEach(Array(rows.enumerated()), id: \.offset) { _, row in
                    HStack(spacing: 8) {
                        ForEach(row) { metric in
                            OperatingV12Tile(
                                label: metric.label,
                                value: OperatingV12Money.amount(metric.value, currency: currency),
                                tone: OperatingV12.tone(metric.tone, key: metric.key, accent: theme.accent),
                                fill: fill
                            )
                        }
                        ForEach(0..<(width - row.count), id: \.self) { _ in
                            Color.clear.frame(maxWidth: .infinity)
                        }
                    }
                }
            }
        }
    }
}

struct OperatingV12Row: View {
    let title: String
    var subtitle: String? = nil
    var trailing: String? = nil
    var chip: String? = nil
    var bar: Color = OperatingV12.cyan
    var fill: Color = OperatingV12.row
    var accent: Color = OperatingV12.green
    var onClick: (() -> Void)? = nil

    var body: some View {
        Button(action: { onClick?() }) {
            HStack(spacing: 10) {
                RoundedRectangle(cornerRadius: 2)
                    .fill(bar)
                    .frame(width: 3, height: 28)
                Circle().fill(bar).frame(width: 6, height: 6)
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(v12Font(13, weight: .semibold))
                        .foregroundStyle(OperatingV12.title)
                        .lineLimit(2)
                    if let subtitle, !subtitle.isEmpty {
                        Text(subtitle)
                            .font(v12Font(11, weight: .medium))
                            .foregroundStyle(OperatingV12.subtitle)
                            .lineLimit(2)
                    }
                }
                Spacer(minLength: 0)
                if let trailing, !trailing.isEmpty {
                    Text(trailing)
                        .font(v12Font(12, weight: .bold))
                        .foregroundStyle(OperatingV12.viewAll)
                        .lineLimit(1)
                }
                if let chip, !chip.isEmpty {
                    OperatingV12StatusChip(label: chip, accent: accent)
                }
            }
            .padding(.leading, 12)
            .padding(.trailing, 10)
            .padding(.vertical, 10)
            .background(fill)
            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
        }
        .buttonStyle(.plain)
        .disabled(onClick == nil)
    }
}

struct OperatingV12QuietRow: View {
    let title: String
    var bar: Color = OperatingV12.green
    var body: some View { OperatingV12Row(title: title, bar: bar) }
}

struct OperatingV12Why: View {
    let text: String?
    var tone: Color = OperatingV12.cyan

    var body: some View {
        let body = text?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if !body.isEmpty {
            Text(body.hasPrefix("Why:") ? body : "Why: \(body)")
                .font(v12Font(11, weight: .medium))
                .foregroundStyle(tone.opacity(0.6))
                .frame(maxWidth: .infinity, alignment: .leading)
        }
    }
}

struct OperatingV12QuickActions: View {
    let theme: OperatingV12Theme
    var tinted: Bool = false
    var onOpen: (String) -> Void

    private let actions: [(code: String, label: String, cyan: Bool)] = [
        ("SALE", "Sale", false),
        ("PURCHASE", "Purchase", true),
        ("COLLECTION", "Collection", false),
        ("PAYMENT", "Payment", false),
    ]

    /// Twin shadow recipe (locked with Android `OperatingV12Card` / pulse actions).
    private enum Chrome {
        static let cardGlowTrading = 0.18
        static let selectedElevationOpacity = 0.45
        static let actionRadius: CGFloat = 14
    }

    var body: some View {
        HStack(spacing: 8) {
            ForEach(Array(actions.enumerated()), id: \.offset) { index, action in
                let tone = action.cyan ? OperatingV12.cyan : theme.accent
                let selected = tinted && index == 0
                Button { onOpen(action.code) } label: {
                    VStack(spacing: 6) {
                        if selected {
                            Circle().fill(theme.accent).frame(width: 4, height: 4)
                        }
                        Image(TradingChromeIcons.pulseActionAsset(action.code))
                            .renderingMode(.template)
                            .resizable()
                            .scaledToFit()
                            .frame(width: 18, height: 18)
                            .foregroundStyle(selected ? theme.accent : tone)
                        Text(action.label)
                            .font(v12Font(11, weight: .bold))
                            .foregroundStyle(OperatingV12.title)
                            .lineLimit(1)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 10)
                    .background(tinted ? tone.opacity(0.12) : OperatingV12.inset)
                    .overlay(
                        RoundedRectangle(cornerRadius: Chrome.actionRadius, style: .continuous)
                            .stroke(
                                selected ? theme.accent.opacity(0.55) : tone.opacity(tinted ? 0.3 : 0.28),
                                lineWidth: 1
                            )
                    )
                    .clipShape(RoundedRectangle(cornerRadius: Chrome.actionRadius, style: .continuous))
                    .shadow(
                        color: selected ? theme.accent.opacity(Chrome.selectedElevationOpacity) : .clear,
                        radius: selected ? 10 : 0,
                        y: selected ? 4 : 0
                    )
                }
                .buttonStyle(.plain)
            }
        }
    }
}

struct OperatingV12PulseHero: View {
    let theme: OperatingV12Theme
    let chips: [String]
    let title: String
    let subtitle: String
    let badge: String

    var body: some View {
        ZStack(alignment: .topTrailing) {
            VStack(alignment: .leading, spacing: 16) {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(chips, id: \.self) { chip in
                            Text(chip)
                                .font(v12Font(12, weight: .bold))
                                .foregroundStyle(.white)
                                .lineLimit(1)
                                .padding(.horizontal, 10)
                                .padding(.vertical, 6)
                                .background(Color.white.opacity(0.1))
                                .overlay(Capsule().stroke(Color.white.opacity(0.2), lineWidth: 1))
                                .clipShape(Capsule())
                        }
                    }
                }
                VStack(alignment: .leading, spacing: 4) {
                    Text(title)
                        .font(v12Font(32, weight: .black))
                        .foregroundStyle(.white)
                        .lineLimit(2)
                    Text(subtitle)
                        .font(v12Font(14, weight: .medium))
                        .foregroundStyle(Color.white.opacity(0.8))
                    Text(badge)
                        .font(v12Font(12, weight: .bold))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 4)
                        .background(Color.white.opacity(0.1))
                        .overlay(Capsule().stroke(Color.white.opacity(0.2), lineWidth: 1))
                        .clipShape(Capsule())
                        .padding(.top, 2)
                }
            }
            .padding(20)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                LinearGradient(
                    colors: [theme.heroStart, theme.heroEnd.opacity(0.8)],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
            )
            .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))

            Image(systemName: "arrow.left.arrow.right")
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(theme.accent)
                .frame(width: 40, height: 40)
                .background(theme.canvas.opacity(0.9))
                .overlay(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .stroke(theme.accent, lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                .padding(16)
        }
    }
}

struct OperatingV12ChipSpec {
    let label: String
    var tone: Color = OperatingV12.subtitle
    var dot: Bool = false
}

struct OperatingV12KpiSpec {
    let label: String
    let value: String
    var tone: Color = OperatingV12.title
}

struct OperatingV12Hero: View {
    let theme: OperatingV12Theme
    let eyebrow: String
    let title: String
    var titleSize: CGFloat = 24
    var chips: [OperatingV12ChipSpec] = []
    var kpis: [OperatingV12KpiSpec] = []

    var body: some View {
        OperatingV12Card(theme: theme, borderOpacity: 0.45, spacing: 14) {
            Text(eyebrow)
                .font(v12Font(10, weight: .semibold))
                .foregroundStyle(theme.accent)
                .tracking(1.2)
            Text(title)
                .font(v12Font(titleSize, weight: .black))
                .foregroundStyle(.white)
                .lineLimit(2)
            if !chips.isEmpty {
                HStack(spacing: 8) {
                    ForEach(Array(chips.enumerated()), id: \.offset) { _, chip in
                        OperatingV12Chip(label: chip.label, tone: chip.tone, dot: chip.dot)
                    }
                }
            }
            if !kpis.isEmpty {
                HStack(spacing: 8) {
                    ForEach(Array(kpis.enumerated()), id: \.offset) { _, kpi in
                        OperatingV12Tile(
                            label: kpi.label,
                            value: kpi.value,
                            tone: kpi.tone,
                            fill: OperatingV12.inset,
                            labelTone: Color.white.opacity(0.6)
                        )
                    }
                }
            }
        }
    }
}

struct OperatingV12Ring: View {
    let fraction: Double
    let caption: String
    let note: String
    let accent: Color

    var body: some View {
        VStack(spacing: 4) {
            ZStack {
                Circle()
                    .stroke(accent.opacity(0.2), lineWidth: 6)
                Circle()
                    .trim(from: 0, to: CGFloat(min(max(fraction, 0), 1)))
                    .stroke(accent, style: StrokeStyle(lineWidth: 6, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                Text(caption)
                    .font(v12Font(14, weight: .bold))
                    .foregroundStyle(.white)
            }
            .frame(width: 72, height: 72)
            Text(note)
                .font(v12Font(10, weight: .medium))
                .foregroundStyle(OperatingV12.subtitle)
        }
    }
}
