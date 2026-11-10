import SwiftUI

// MARK: - Palette

/// The v12 widget palette (`TRD-PULSE-v12` … `TRD-HUB-v11`).
///
/// The frames were drawn for a wholesale trade, so every non-semantic accent comes off the
/// vertical: a dealership gets the same structure in orange without a single surface being
/// re-specified. Cyan, amber, red and green stay semantic — money in flight, waiting, overdue,
/// settled — because those four never change with the vertical.
enum OperatingV12 {
    /// Widget fill (`#151D2E`) — Moments, Life, Memory and the hub.
    static let surface = Color(hex: "#151D2E")
    /// Pulse widgets sit a step darker (`#111827`), so the accent border reads against the hero.
    static let pulseSurface = Color(hex: "#111827")
    static let title = Color(hex: "#F1F5F9")
    static let sectionTitle = Color(hex: "#F8FAFC")
    static let subtitle = Color(hex: "#94A3B8")
    static let rowText = Color(hex: "#E2E8F0")
    /// `View all ›` — the emerald that appears on every widget header.
    static let viewAll = Color(hex: "#34D399")
    static let cyan = Color(hex: "#06B6D4")
    static let green = Color(hex: "#22C55E")
    static let amber = Color(hex: "#F59E0B")
    static let red = Color(hex: "#EF4444")
    static let deepInk = Color(hex: "#04110C")
    static let hairline = Color.white.opacity(0.06)
    static let inset = Color.black.opacity(0.28)
    static let row = Color.black.opacity(0.24)

    /// Figures the frame prints in emerald: achievement, money already collected, commitments met.
    private static let inflowKeys: Set<String> = [
        "progress", "toCollect", "collected", "sales", "netSales", "committed", "repeat", "delivered",
    ]
    /// Figures the frame prints in cyan: money still moving.
    private static let flightKeys: Set<String> = [
        "outstanding", "credits", "tax", "net", "inTransit", "months", "records",
    ]
    /// Figures the frame prints in red: what is owed, short, or gone wrong.
    private static let pressureKeys: Set<String> = [
        "payable", "toPay", "difference", "issues", "overdue", "low", "returns",
    ]

    /// A tile's value colour.
    ///
    /// The frame colours a figure by what it *means* (inflow / in flight / pressure), not by the
    /// server's `tone` alone, so the same key never changes colour between two widgets. `tone` is
    /// still the fallback for keys this build has not seen yet.
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

    /// The left bar of a list row, from the row's own badge.
    static func bar(for badge: String?) -> Color {
        switch badge {
        case "Overdue", "Cancelled", "Rejected": return red
        case "Unavailable", "Part", "Due", "On hold", "Transit", "Open order": return amber
        case "Settled", "Done", "Delivered", "Alert", "Open", "Pattern": return green
        default: return cyan
        }
    }
}

// MARK: - Money

/// Server money arrives as `940000.0000` or `940000.0000 INR`; the frames print `₹9.4L`. A count
/// keeps its commas, and a figure the server withheld is never reformatted into a number.
enum OperatingV12Money {
    static func compact(_ raw: String, currency: String?) -> String {
        let value = raw.trimmingCharacters(in: .whitespaces)
        guard !value.isEmpty else { return "—" }
        let code = (currency ?? "INR").uppercased()
        guard value.uppercased().contains(code) || value.hasPrefix("₹") else { return value }

        let numeric = value
            .replacingOccurrences(of: code, with: "")
            .replacingOccurrences(of: "₹", with: "")
            .replacingOccurrences(of: ",", with: "")
            .trimmingCharacters(in: .whitespaces)
        guard let amount = Double(numeric) else { return value }
        return compact(amount, currency: code)
    }

    /// A computed sum — four dues added together — in the same compact register.
    static func compact(_ amount: Double, currency: String?) -> String {
        let code = (currency ?? "INR").uppercased()
        let symbol = code == "INR" ? "₹" : "\(code) "
        let magnitude = abs(amount)
        let sign = amount < 0 ? "−" : ""
        func trim(_ text: String) -> String {
            text.hasSuffix(".0") ? String(text.dropLast(2)) : text
        }
        switch magnitude {
        case 0:
            return "\(symbol)0"
        case ..<1_000:
            return symbol + sign + String(format: "%.0f", magnitude)
        case ..<100_000:
            return symbol + sign + trim(String(format: "%.1fk", magnitude / 1_000))
        case ..<10_000_000:
            return symbol + sign + trim(String(format: "%.1fL", magnitude / 100_000))
        default:
            return symbol + sign + trim(String(format: "%.1fCr", magnitude / 10_000_000))
        }
    }

    static func number(_ raw: String?) -> Double? {
        guard let raw else { return nil }
        return Double(raw.trimmingCharacters(in: .whitespaces))
    }

    static func tile(_ metric: OperatingBoardMetricPayload, currency: String?) -> String {
        let text = compact(metric.value, currency: currency)
        guard let unit = metric.unit, !unit.isEmpty, unit.lowercased() != "count" else { return text }
        return "\(text) \(unit)"
    }

    /// A raw server figure — `940000.0000` — as the frames print it. Anything unparseable
    /// (`Not recorded`, `Hidden`, `—`) is returned untouched.
    static func amount(_ raw: String?, currency: String?) -> String {
        guard let raw, !raw.trimmingCharacters(in: .whitespaces).isEmpty else { return "—" }
        guard Double(raw) != nil else { return raw }
        return compact(raw, currency: currency)
    }
}

/// One section's tile, looked up by key and formatted for the v12 grid.
enum OperatingV12Metric {
    static func value(
        _ section: OperatingBoardSectionPayload?,
        key: String,
        currency: String?
    ) -> String? {
        guard let metric = section?.metrics?.first(where: { $0.key == key }) else { return nil }
        return OperatingV12Money.tile(metric, currency: currency)
    }
}

// MARK: - Card

/// One widget: `#151D2E`, a 1 pt accent hairline, 20 pt corners, and the frame's twin shadow.
struct OperatingV12Card<Content: View>: View {
    let theme: OperatingTheme
    var fill: Color = OperatingV12.surface
    var borderOpacity: Double = 0.28
    var borderWidth: CGFloat = 1
    var radius: CGFloat = 20
    var padding: CGFloat = 16
    var spacing: CGFloat = 14
    @ViewBuilder var content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: spacing) {
            content()
        }
        .padding(padding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(fill)
        .clipShape(RoundedRectangle(cornerRadius: radius, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: radius, style: .continuous)
                .strokeBorder(theme.accent.opacity(borderOpacity), lineWidth: borderWidth)
        )
        // Twin shadow: black depth + accent soft glow (stronger Dist orange per DST-PULSE-v12).
        .shadow(color: .black.opacity(0.35), radius: 12, x: 0, y: 6)
        .shadow(
            color: theme.accent.opacity(theme.vertical == .distribution ? 0.32 : 0.18),
            radius: 28,
            x: 0,
            y: 8
        )
    }
}

// MARK: - Chips

/// `Trading` / `Books open` / `Oct 2026 · target v2` / `vs Sep 2026`.
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
                .font(.plusJakarta(size: 11, weight: .semibold))
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

/// `Alert` / `Unavailable` / `Overdue` / `Open` / `Part` / `Settled` / `Done` / `Pattern`.
///
/// The frame has no border on these; the fill alone carries severity.
struct OperatingV12StatusChip: View {
    let label: String
    var accent: Color = OperatingV12.green

    private var tone: Color {
        switch label {
        case "Overdue", "Cancelled", "Rejected": return OperatingV12.red
        case "Unavailable", "Part", "Due", "On hold", "Open order", "Transit": return OperatingV12.amber
        case "Settled", "Done", "Delivered", "Alert", "Open", "Pattern": return accent
        default: return OperatingV12.subtitle
        }
    }

    var body: some View {
        Text(label)
            .font(.plusJakarta(size: 10, weight: .semibold))
            .foregroundStyle(tone)
            .padding(.horizontal, 8)
            .padding(.vertical, 3)
            .background(tone.opacity(0.16))
            .clipShape(Capsule())
    }
}

// MARK: - Widget chrome

/// Title, one line of provenance, then the `View all ›` affordance — exactly the header stack
/// every v12 widget carries. The whole stack is the tap target for `View all`.
struct OperatingV12WidgetHeader: View {
    let title: String
    var subtitle: String? = nil
    var onViewAll: (() -> Void)? = nil

    var body: some View {
        Button {
            onViewAll?()
        } label: {
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.plusJakarta(size: 16, weight: .bold))
                    .foregroundStyle(OperatingV12.title)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .multilineTextAlignment(.leading)
                if let subtitle, !subtitle.isEmpty {
                    Text(subtitle)
                        .font(.plusJakarta(size: 12, weight: .medium))
                        .foregroundStyle(OperatingV12.subtitle)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .multilineTextAlignment(.leading)
                }
                if onViewAll != nil {
                    Text("View all ›")
                        .font(.plusJakarta(size: 12, weight: .bold))
                        .foregroundStyle(OperatingV12.viewAll)
                        .padding(.horizontal, 4)
                        .padding(.vertical, 2)
                }
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(onViewAll == nil)
    }
}

// MARK: - Tiles

struct OperatingV12Tile: View {
    let label: String
    let value: String
    var tone: Color = OperatingV12.title
    var fill: Color = OperatingV12.inset
    var radius: CGFloat = 12
    /// Hero tiles carry a softer label than widget tiles.
    var labelTone: Color = OperatingV12.subtitle

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.plusJakarta(size: 11, weight: .medium))
                .foregroundStyle(labelTone)
                .lineLimit(1)
                .minimumScaleFactor(0.6)
            Text(value)
                .font(.plusJakarta(size: 18, weight: .bold))
                .foregroundStyle(tone)
                .lineLimit(1)
                .minimumScaleFactor(0.55)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(fill)
        .clipShape(RoundedRectangle(cornerRadius: radius, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: radius, style: .continuous)
                .strokeBorder(OperatingV12.hairline, lineWidth: 1)
        )
    }
}

/// Lays tiles out in fixed-width rows so a three-tile surface never wraps to two plus a stray.
struct OperatingV12TileGrid: View {
    let theme: OperatingTheme
    let metrics: [OperatingBoardMetricPayload]
    var columns: Int = 3
    var currency: String?
    var fill: Color = OperatingV12.inset
    var radius: CGFloat = 12

    var body: some View {
        if !metrics.isEmpty {
            VStack(spacing: 8) {
                ForEach(Array(rows.enumerated()), id: \.offset) { _, row in
                    HStack(spacing: 8) {
                        ForEach(row) { metric in
                            OperatingV12Tile(
                                label: metric.label,
                                value: OperatingV12Money.tile(metric, currency: currency),
                                tone: OperatingV12.tone(metric.tone, key: metric.key, accent: theme.accent),
                                fill: fill,
                                radius: radius
                            )
                        }
                        if row.count < columns {
                            ForEach(0 ..< (columns - row.count), id: \.self) { _ in
                                Color.clear.frame(maxWidth: .infinity)
                            }
                        }
                    }
                }
            }
        }
    }

    private var rows: [[OperatingBoardMetricPayload]] {
        let width = max(columns, 1)
        return stride(from: 0, to: metrics.count, by: width).map { start in
            Array(metrics[start ..< min(start + width, metrics.count)])
        }
    }
}

/// Server tiles in the frame's order, then whatever else the section sent to fill the row.
enum OperatingV12Tiles {
    static func ordered(
        _ section: OperatingBoardSectionPayload?,
        keys: [String],
        limit: Int,
        currency: String?
    ) -> [OperatingBoardMetricPayload] {
        guard let metrics = section?.metrics, !metrics.isEmpty else { return [] }
        var picked: [OperatingBoardMetricPayload] = []
        for key in keys {
            if let metric = metrics.first(where: { $0.key == key }) { picked.append(metric) }
        }
        for metric in metrics where !picked.contains(where: { $0.id == metric.id }) {
            picked.append(metric)
        }
        return Array(picked.prefix(limit))
    }
}

// MARK: - Row

/// One line inside a widget: a 3 pt severity bar, a 6 pt dot, the title (and optional context),
/// then the figure and the chip. `TRD-*` draws all four when the row has all four.
struct OperatingV12Row: View {
    let title: String
    var subtitle: String? = nil
    var trailing: String? = nil
    var chip: String? = nil
    var bar: Color = OperatingV12.cyan
    var fill: Color = OperatingV12.row
    var accent: Color = OperatingV12.green
    var verticalPadding: CGFloat = 10

    var body: some View {
        HStack(spacing: 10) {
            Circle()
                .fill(bar)
                .frame(width: 6, height: 6)
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.plusJakarta(size: 13, weight: .semibold))
                    .foregroundStyle(OperatingV12.title)
                    .lineLimit(2)
                    .multilineTextAlignment(.leading)
                if let subtitle, !subtitle.isEmpty {
                    Text(subtitle)
                        .font(.plusJakarta(size: 11, weight: .medium))
                        .foregroundStyle(OperatingV12.subtitle)
                        .lineLimit(2)
                        .multilineTextAlignment(.leading)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            if let trailing, !trailing.isEmpty {
                Text(trailing)
                    .font(.plusJakarta(size: 12, weight: .bold))
                    .foregroundStyle(OperatingV12.viewAll)
                    .lineLimit(1)
            }
            if let chip, !chip.isEmpty {
                OperatingV12StatusChip(label: chip, accent: accent)
            }
        }
        .padding(.leading, 12)
        .padding(.trailing, 10)
        .padding(.vertical, verticalPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(fill)
        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
        .overlay(alignment: .leading) {
            Rectangle()
                .fill(bar)
                .frame(width: 3)
                .clipShape(RoundedRectangle(cornerRadius: 2, style: .continuous))
        }
    }
}

/// A row with no severity to report — `Nothing is waiting.`
struct OperatingV12QuietRow: View {
    let title: String
    var bar: Color = OperatingV12.green

    var body: some View {
        OperatingV12Row(title: title, bar: bar, verticalPadding: 10)
    }
}

// MARK: - Why

/// The Figma `Why:` footnote. It explains a rule or a gap, never restates a tile.
struct OperatingV12Why: View {
    let text: String?
    /// Pulse prints it in white at 50%; the other tabs print it in cyan.
    var tone: Color = OperatingV12.cyan

    var body: some View {
        if let text, !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            Text(text.hasPrefix("Why:") ? text : "Why: \(text)")
                .font(.plusJakarta(size: 11, weight: .medium))
                .foregroundStyle(tone)
                .frame(maxWidth: .infinity, alignment: .leading)
                .opacity(0.6)
        }
    }
}

// MARK: - Quick actions

/// The four capture shortcuts every operating tab carries under its hero.
///
/// Pulse tints each plate by action (sale and money emerald, purchase cyan); the other tabs keep
/// them dark under one accent hairline. Codes match the capture sheet's modes exactly — retail
/// family swaps the primary pair (POS/KOT, Finalize/Booking) so Pulse never opens trading sheets.
struct OperatingV12QuickActions: View {
    let theme: OperatingTheme
    var tinted: Bool = false
    var radius: CGFloat = 14
    var onOpen: (String) -> Void

    private struct Action: Identifiable {
        let code: String
        let systemImage: String
        let label: String
        let cyan: Bool
        var id: String { code }
    }

    // DST/TRD-PULSE-v12: thin SF Symbols (not emoji); first plate carries the selected accent dot.
    private var actions: [Action] {
        switch theme.vertical {
        case .restaurantFood:
            return [
                Action(code: "POS", systemImage: "fork.knife", label: "POS", cyan: false),
                Action(code: "KOT", systemImage: "flame", label: "KOT", cyan: true),
                Action(code: "COLLECTION", systemImage: "list.clipboard", label: "Collection", cyan: false),
                Action(code: "PAYMENT", systemImage: "creditcard", label: "Payment", cyan: false),
            ]
        case .consumerService:
            return [
                Action(code: "SERVICE", systemImage: "checkmark.seal", label: "Charge", cyan: false),
                Action(code: "APPOINTMENT", systemImage: "calendar", label: "Booking", cyan: true),
                Action(code: "COLLECTION", systemImage: "list.clipboard", label: "Collection", cyan: false),
                Action(code: "PAYMENT", systemImage: "creditcard", label: "Payment", cyan: false),
            ]
        case .retailStore:
            return [
                Action(code: "SALE", systemImage: "bag", label: "Sale", cyan: false),
                Action(code: "PURCHASE", systemImage: "cart", label: "Purchase", cyan: true),
                Action(code: "COLLECTION", systemImage: "list.clipboard", label: "Collection", cyan: false),
                Action(code: "PAYMENT", systemImage: "creditcard", label: "Payment", cyan: false),
            ]
        default:
            return [
                Action(code: "SALE", systemImage: "camera", label: "Sale", cyan: false),
                Action(code: "PURCHASE", systemImage: "bubble.left.and.bubble.right", label: "Purchase", cyan: true),
                Action(code: "COLLECTION", systemImage: "list.clipboard", label: "Collection", cyan: false),
                Action(code: "PAYMENT", systemImage: "creditcard", label: "Payment", cyan: false),
            ]
        }
    }

    var body: some View {
        HStack(spacing: 8) {
            ForEach(Array(actions.enumerated()), id: \.element.id) { index, action in
                let tone = action.cyan ? OperatingV12.cyan : theme.accent
                let selected = tinted && index == 0
                Button {
                    onOpen(action.code)
                } label: {
                    VStack(spacing: 6) {
                        if selected {
                            Circle()
                                .fill(theme.accent)
                                .frame(width: 4, height: 4)
                        }
                        Image(systemName: action.systemImage)
                            .font(.system(size: 15, weight: .medium))
                            .foregroundStyle(selected ? theme.accent : tone)
                        Text(action.label)
                            .font(.plusJakarta(size: 11, weight: .bold))
                            .foregroundStyle(OperatingV12.title)
                            .lineLimit(1)
                            .minimumScaleFactor(0.75)
                    }
                    .padding(.horizontal, 12)
                    .padding(.vertical, 10)
                    .frame(maxWidth: .infinity)
                    .background(tinted ? tone.opacity(0.12) : OperatingV12.inset)
                    .overlay(
                        RoundedRectangle(cornerRadius: radius, style: .continuous)
                            .stroke(
                                selected ? theme.accent.opacity(0.55) : tone.opacity(tinted ? 0.3 : 0.28),
                                lineWidth: 1
                            )
                    )
                    .clipShape(RoundedRectangle(cornerRadius: radius, style: .continuous))
                    .shadow(
                        color: selected ? theme.accent.opacity(0.35) : .clear,
                        radius: selected ? 10 : 0,
                        x: 0,
                        y: 4
                    )
                }
                .buttonStyle(.plain)
            }
        }
    }
}

// MARK: - Hero

/// `TRD-PULSE-v12`: the emerald gradient block, edge to edge, with the moment's identity chips
/// above its name and the books state below.
struct OperatingV12PulseHero: View {
    let theme: OperatingTheme
    let chips: [String]
    let title: String
    let subtitle: String
    let badge: String
    let glyph: String

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(chips, id: \.self) { chip in
                        Text(chip)
                            .font(.plusJakarta(size: 12, weight: .bold))
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
            .scrollClipDisabled()

            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.plusJakarta(size: 32, weight: .heavy))
                    .foregroundStyle(.white)
                    .lineLimit(2)
                    .minimumScaleFactor(0.65)
                Text(subtitle)
                    .font(.plusJakarta(size: 14, weight: .medium))
                    .foregroundStyle(.white.opacity(0.8))
                Text(badge)
                    .font(.plusJakarta(size: 12, weight: .bold))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .background(Color.white.opacity(0.1))
                    .overlay(Capsule().stroke(Color.white.opacity(0.2), lineWidth: 1))
                    .clipShape(Capsule())
                    .padding(.top, 2)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            LinearGradient(
                colors: [theme.vertical.heroStart, theme.vertical.heroEnd.opacity(0.8)],
                startPoint: UnitPoint(x: 0.32, y: 0),
                endPoint: UnitPoint(x: 0.68, y: 1)
            )
        )
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay(alignment: .topTrailing) {
            ZStack {
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .fill(theme.canvas.opacity(0.9))
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .stroke(theme.accent, lineWidth: 1)
                Image(systemName: glyph)
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(theme.accent)
            }
            .frame(width: 40, height: 40)
            .shadow(color: theme.accent.opacity(0.35), radius: 6, x: 0, y: 4)
            .padding(.trailing, 20)
            .padding(.top, 20)
        }
    }
}

/// `TRD-MOMENTS` / `TRD-LIFE` / `TRD-MEMORY`: a carded hero with the moment's own figures.
struct OperatingV12Hero: View {
    let theme: OperatingTheme
    let eyebrow: String
    let title: String
    var titleSize: CGFloat = 24
    var chips: [OperatingV12ChipSpec] = []
    var kpis: [OperatingV12KpiSpec] = []

    var body: some View {
        OperatingV12Card(theme: theme, borderOpacity: 0.45, spacing: 14) {
            Text(eyebrow)
                .font(.plusJakarta(size: 10, weight: .semibold))
                .tracking(1.2)
                .foregroundStyle(theme.accent)
            Text(title)
                .font(.plusJakarta(size: titleSize, weight: .heavy))
                .foregroundStyle(.white)
                .lineLimit(2)
                .minimumScaleFactor(0.7)
            if !chips.isEmpty {
                HStack(spacing: 8) {
                    ForEach(chips, id: \.label) { chip in
                        OperatingV12Chip(label: chip.label, tone: chip.tone, dot: chip.dot)
                    }
                    Spacer(minLength: 0)
                }
            }
            if !kpis.isEmpty {
                HStack(spacing: 8) {
                    ForEach(kpis, id: \.label) { kpi in
                        OperatingV12Tile(
                            label: kpi.label,
                            value: kpi.value,
                            tone: kpi.tone,
                            fill: OperatingV12.inset,
                            labelTone: .white.opacity(0.6)
                        )
                    }
                }
            }
        }
    }
}

struct OperatingV12ChipSpec: Hashable {
    let label: String
    var tone: Color = OperatingV12.subtitle
    var dot: Bool = false
}

struct OperatingV12KpiSpec: Hashable {
    let label: String
    let value: String
    var tone: Color = OperatingV12.title
}

// MARK: - Ring

/// The `Target & Budget` health ring: achievement against the effective plan.
struct OperatingV12Ring: View {
    let fraction: Double
    let caption: String
    let note: String
    let accent: Color

    var body: some View {
        ZStack {
            Circle()
                .stroke(Color.white.opacity(0.08), lineWidth: 8)
            Circle()
                .trim(from: 0, to: max(0.02, min(fraction, 1)))
                .stroke(accent, style: StrokeStyle(lineWidth: 8, lineCap: .round))
                .rotationEffect(.degrees(-90))
            VStack(spacing: 1) {
                Text(caption)
                    .font(.plusJakarta(size: 22, weight: .heavy))
                    .foregroundStyle(.white)
                Text(note)
                    .font(.plusJakarta(size: 10, weight: .medium))
                    .foregroundStyle(.white.opacity(0.55))
            }
        }
        .frame(width: 92, height: 92)
    }
}

// MARK: - Hub tile

/// `tile · Sale` from `TRD-HUB-v11`: 112 pt tall, three up, accent icon plate.
/// Pass `lockedReason` (or legacy `unavailable`) for honest non-actionable tiles — visible, never opens a capture.
struct OperatingV12ActionTile: View {
    let theme: OperatingTheme
    let title: String
    let subtitle: String
    let glyph: String
    var unavailable: Bool = false
    /// When set, tile stays visible but disabled; caption becomes this reason (`Role locked` / `Unavailable`).
    var lockedReason: String? = nil
    let action: () -> Void

    private var locked: String? {
        if let lockedReason { return lockedReason }
        return unavailable ? "Unavailable" : nil
    }

    var body: some View {
        let isLocked = locked != nil
        return Button(action: action) {
            VStack(alignment: .leading, spacing: 6) {
                ZStack {
                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                        .fill(theme.accent.opacity(isLocked ? 0.08 : 0.16))
                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                        .stroke(theme.accent.opacity(isLocked ? 0.25 : 0.45), lineWidth: 1)
                    Image(systemName: glyph)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(isLocked ? OperatingV12.subtitle : theme.accent)
                }
                .frame(width: 32, height: 32)
                .shadow(color: theme.accent.opacity(isLocked ? 0 : 0.5), radius: 6)
                Text(title)
                    .font(.plusJakarta(size: 14, weight: .bold))
                    .foregroundStyle(isLocked ? OperatingV12.subtitle : OperatingV12.title)
                    .lineLimit(2)
                    .multilineTextAlignment(.leading)
                Text(locked ?? subtitle)
                    .font(.plusJakarta(size: 10))
                    .foregroundStyle(OperatingV12.subtitle)
                    .lineLimit(2)
                    .multilineTextAlignment(.leading)
            }
            .padding(12)
            .frame(maxWidth: .infinity, minHeight: 112, maxHeight: 112, alignment: .topLeading)
            .background(OperatingV12.surface)
            .overlay(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .stroke(theme.accent.opacity(isLocked ? 0.2 : 0.35), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            .shadow(color: .black.opacity(isLocked ? 0.15 : 0.4), radius: 6, x: 0, y: 4)
            .contentShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            .opacity(isLocked ? 0.72 : 1)
        }
        .buttonStyle(.plain)
        .disabled(isLocked)
    }
}

// MARK: - Section head

struct OperatingV12SectionHead: View {
    let title: String
    var trailing: String? = nil
    var trailingTone: Color = OperatingV12.cyan

    var body: some View {
        HStack {
            Text(title)
                .font(.plusJakarta(size: 16, weight: .bold))
                .foregroundStyle(OperatingV12.sectionTitle)
            Spacer(minLength: 8)
            if let trailing {
                Text(trailing)
                    .font(.plusJakarta(size: 11, weight: .semibold))
                    .foregroundStyle(trailingTone)
            }
        }
    }
}
