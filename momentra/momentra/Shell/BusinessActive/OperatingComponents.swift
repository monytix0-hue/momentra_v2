import SwiftUI

// MARK: - Theme

/// Palette handed to every operating surface. Both verticals share the structure and differ
/// only in accent, canvas, and copy — exactly as the TRD / DST Figma frames do.
struct OperatingTheme {
    let vertical: BusinessOperatingVertical

    var accent: Color { vertical.accent }
    var canvas: Color { vertical.canvas }
    var surface: Color { vertical.surface }
    var inset: Color { vertical.inset }
    var border: Color { vertical.border }
    var ink: Color { vertical.ink }
    var body: Color { vertical.body }
    var muted: Color { vertical.muted }
    var onAccent: Color { vertical.onAccent }
    var warning: Color { BusinessTokens.Colors.signalWarning }
    var negative: Color { BusinessTokens.Colors.signalDanger }
    var positive: Color { BusinessTokens.Colors.signalPositive }

    init(vertical: BusinessOperatingVertical) {
        self.vertical = vertical
    }
}

// MARK: - Card

struct OperatingCard<Content: View>: View {
    let theme: OperatingTheme
    var padding: CGFloat = 14
    @ViewBuilder var content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            content()
        }
        .padding(padding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(theme.surface)
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(theme.border, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}

// MARK: - Header

struct OperatingHeader: View {
    let theme: OperatingTheme
    let title: String
    let bodyText: String

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title)
                .font(.plusJakarta(size: 30, weight: .bold))
                .foregroundStyle(theme.ink)
            Text(bodyText)
                .font(.plusJakarta(size: 13))
                .foregroundStyle(theme.body)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

struct OperatingPill: View {
    let theme: OperatingTheme
    let label: String
    var accent: Bool = false

    var body: some View {
        Text(label)
            .font(.plusJakarta(size: 11, weight: .semibold))
            .foregroundStyle(accent ? theme.accent : theme.body)
            .padding(.horizontal, 10)
            .padding(.vertical, 5)
            .background(accent ? theme.accent.opacity(0.12) : theme.inset)
            .overlay(
                Capsule().stroke(accent ? theme.accent.opacity(0.45) : theme.border, lineWidth: 1)
            )
            .clipShape(Capsule())
    }
}

// MARK: - Metric tiles

/// One quantified tile. A gap in the record arrives as `Not recorded` and is rendered verbatim
/// in a warning tone — never coerced to zero, which an operator would read as a real figure.
struct OperatingMetricTile: View {
    let theme: OperatingTheme
    let metric: OperatingBoardMetricPayload

    private var tone: Color {
        switch metric.tone {
        case "WARNING": return theme.warning
        case "POSITIVE": return theme.accent
        default: return theme.ink
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 5) {
            Text(metric.label)
                .font(.plusJakarta(size: 11))
                .foregroundStyle(theme.body)
                .lineLimit(2)
            Text(OperatingMoneyFormat.tileValue(metric.value, unit: metric.unit))
                .font(.plusJakarta(size: 17, weight: .semibold))
                .foregroundStyle(tone)
                .lineLimit(2)
                .minimumScaleFactor(0.7)
        }
        .padding(11)
        .frame(maxWidth: .infinity, minHeight: 74, alignment: .leading)
        .background(theme.inset)
        .overlay(
            RoundedRectangle(cornerRadius: 13)
                .stroke(theme.border, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 13))
    }
}

struct OperatingMetricGrid: View {
    let theme: OperatingTheme
    let metrics: [OperatingBoardMetricPayload]
    var columns: Int = 2

    var body: some View {
        if !metrics.isEmpty {
            let rows = stride(from: 0, to: metrics.count, by: max(columns, 1)).map { start in
                Array(metrics[start ..< min(start + max(columns, 1), metrics.count)])
            }
            VStack(spacing: 8) {
                ForEach(Array(rows.enumerated()), id: \.offset) { _, row in
                    HStack(spacing: 8) {
                        ForEach(row) { tile in
                            OperatingMetricTile(theme: theme, metric: tile)
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
}

// MARK: - Why footnote

/// The Figma `Why: …` line. It explains a gap and never restates the tiles.
struct OperatingWhyFootnote: View {
    let theme: OperatingTheme
    let text: String?

    var body: some View {
        if let text, !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            Text("Why: \(text)")
                .font(.plusJakarta(size: 12))
                .foregroundStyle(theme.body)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
    }
}

// MARK: - Badge

/// `Open` / `Part` / `Overdue` / `Settled` / `Done` / `Transit` / `On hold`.
struct OperatingBadgeChip: View {
    let theme: OperatingTheme
    let badge: String?

    private var tone: Color {
        switch badge {
        case "Overdue", "Cancelled": return theme.negative
        case "On hold", "Open", "Approval", "Part", "Transit": return theme.warning
        case "Settled", "Delivered", "Done": return theme.accent
        default: return theme.body
        }
    }

    var body: some View {
        if let badge, !badge.isEmpty {
            Text(badge)
                .font(.plusJakarta(size: 10, weight: .semibold))
                .foregroundStyle(tone)
                .padding(.horizontal, 8)
                .padding(.vertical, 3)
                .overlay(Capsule().stroke(tone.opacity(0.5), lineWidth: 1))
                .clipShape(Capsule())
        }
    }
}

// MARK: - Row

struct OperatingActivityRow: View {
    let theme: OperatingTheme
    let row: OperatingActivityPayload
    let currency: String
    let financeVisible: Bool
    var onTap: (() -> Void)? = nil

    private var subtitle: String {
        if let subtitle = row.subtitle, !subtitle.isEmpty { return subtitle }
        var parts: [String] = []
        if let name = row.counterpartyName, !name.isEmpty { parts.append(name) }
        if let channel = row.channelName, !channel.isEmpty { parts.append(channel) }
        if let quantity = row.quantity, !quantity.isEmpty {
            parts.append([quantity, row.unit ?? ""].filter { !$0.isEmpty }.joined(separator: " "))
        }
        return parts.joined(separator: " · ")
    }

    private var title: String {
        row.title.isEmpty ? row.eventType.capitalized : row.title
    }

    var body: some View {
        Button {
            onTap?()
        } label: {
            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 8) {
                    Text(title)
                        .font(.plusJakarta(size: 14))
                        .foregroundStyle(theme.ink)
                        .lineLimit(1)
                    Spacer(minLength: 4)
                    OperatingBadgeChip(theme: theme, badge: row.badge)
                }
                if !subtitle.isEmpty {
                    Text(subtitle)
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(theme.body)
                        .lineLimit(2)
                }
                if financeVisible, let amount = row.amount, !amount.isEmpty {
                    Text(OperatingMoneyFormat.format(currency, amount))
                        .font(.plusJakarta(size: 13, weight: .semibold))
                        .foregroundStyle(theme.accent)
                }
            }
            .padding(.vertical, 8)
            .frame(maxWidth: .infinity, alignment: .leading)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(onTap == nil)
    }
}

// MARK: - Empty state

/// Nothing recorded is stated plainly. It never borrows a figure from another surface.
struct OperatingEmptyState: View {
    let theme: OperatingTheme
    let title: String
    let bodyText: String
    var glyph: String = "◻︎"

    var body: some View {
        VStack(spacing: 8) {
            Text(glyph)
                .font(.system(size: 26))
                .foregroundStyle(theme.accent.opacity(0.8))
            Text(title)
                .font(.plusJakarta(size: 15, weight: .semibold))
                .foregroundStyle(theme.ink)
                .multilineTextAlignment(.center)
            Text(bodyText)
                .font(.plusJakarta(size: 12))
                .foregroundStyle(theme.body)
                .multilineTextAlignment(.center)
        }
        .padding(18)
        .frame(maxWidth: .infinity)
        .background(theme.inset)
        .overlay(
            RoundedRectangle(cornerRadius: 14)
                .stroke(theme.border, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }
}

// MARK: - Section body

/// The body of a `View all ›` sheet for one board section: tiles from `metrics`, the gap
/// explanation from `why`, then the rows. Nothing is parsed out of the prose `detail` string.
struct OperatingBoardSectionBody: View {
    let theme: OperatingTheme
    let spec: OperatingSheetSpec
    let section: OperatingBoardSectionPayload?
    let currency: String
    let financeVisible: Bool
    var onOpenRow: ((OperatingActivityPayload) -> Void)? = nil

    var body: some View {
        if let section {
            VStack(alignment: .leading, spacing: 12) {
                OperatingMetricGrid(theme: theme, metrics: section.metrics ?? [])
                OperatingWhyFootnote(theme: theme, text: section.why)
                let rows = section.rows ?? []
                if rows.isEmpty {
                    OperatingEmptyState(
                        theme: theme,
                        title: spec.emptyTitle,
                        bodyText: spec.emptyBody,
                        glyph: "◻︎"
                    )
                } else {
                    VStack(spacing: 0) {
                        ForEach(rows) { row in
                            OperatingActivityRow(
                                theme: theme,
                                row: row,
                                currency: currency,
                                financeVisible: financeVisible,
                                onTap: onOpenRow.map { open in { open(row) } }
                            )
                            Divider().overlay(theme.border)
                        }
                    }
                }
            }
        } else {
            OperatingEmptyState(theme: theme, title: spec.emptyTitle, bodyText: spec.emptyBody)
        }
    }
}
