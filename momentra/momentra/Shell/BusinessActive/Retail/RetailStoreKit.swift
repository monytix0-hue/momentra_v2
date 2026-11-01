import SwiftUI

// MARK: - Screenshot helper (Figma frame → SwiftUI)

/// Shared visual primitives for the retail section, matching Figma `2487:37758`.
/// The Pulse cards (`#2a1a1f`, 28% rose) and the board cards (`#1e293b`, 40–50% rose)
/// are the same component with a different tone, so they share one implementation.
enum RoseTone {
    case pulse
    case board
}

/// 3-stop vertical wash behind every retail screen.
struct RoseScreenBackground<Content: View>: View {
    @ViewBuilder var content: () -> Content

    var body: some View {
        ZStack {
            RetailRose.screenGradient.ignoresSafeArea()
            content()
        }
    }
}

/// Card with the Figma double drop shadow plus the inset top highlight.
struct RoseCard<Content: View>: View {
    var tone: RoseTone = .pulse
    var strokeAlpha: Double? = nil
    var gap: CGFloat = 14
    var padding: CGFloat = 16
    var onTap: (() -> Void)? = nil
    @ViewBuilder var content: () -> Content

    private var fill: Color { tone == .pulse ? RetailRose.card : RetailRose.cardSlate }
    private var stroke: Color {
        RetailRose.rose.opacity(strokeAlpha ?? (tone == .pulse ? 0.28 : 0.40))
    }

    var body: some View {
        VStack(alignment: .leading, spacing: gap, content: content)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(padding)
            .background(fill)
            .clipShape(RoundedRectangle(cornerRadius: RetailRose.rCard, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: RetailRose.rCard, style: .continuous)
                    .stroke(stroke, lineWidth: tone == .pulse ? 1 : 1)
            )
            .roseTopHighlight()
            .roseCardShadow()
            .contentShape(Rectangle())
            .onTapGesture { onTap?() }
    }
}

/// Screen title (24/Bold) + amber role line (12/Medium).
struct RetailScreenHeader: View {
    let title: String
    let roleLine: String

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(title)
                .font(.plusJakarta(size: 24, weight: .bold))
                .foregroundStyle(RetailRose.textPrimary)
                .lineLimit(1)
            Text(roleLine)
                .font(.plusJakarta(size: 12, weight: .medium))
                .foregroundStyle(RetailRose.amber)
                .lineLimit(1)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

/// Filled rose chip (`Retail Store`) + outline chips (`Nov 2026 · INR`, `GST on`).
struct RoseContextChips: View {
    let subtypeLabel: String
    let periodLine: String
    let gstOn: Bool
    var onPeriod: (() -> Void)? = nil

    var body: some View {
        HStack(spacing: 6) {
            FilledRoseChip(text: subtypeLabel)
            Button { onPeriod?() } label: { OutlineRoseChip(text: periodLine) }
                .buttonStyle(.plain)
            OutlineRoseChip(text: gstOn ? "GST on" : "GST off")
            Spacer(minLength: 0)
        }
    }
}

struct FilledRoseChip: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.plusJakarta(size: 12, weight: .bold))
            .foregroundStyle(.white)
            .lineLimit(1)
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(RetailRose.rose)
            .clipShape(Capsule())
            .overlay(Capsule().stroke(RetailRose.rose, lineWidth: 1))
            .shadow(color: RetailRose.indigo.opacity(0.4), radius: 6, x: 0, y: 2)
    }
}

struct OutlineRoseChip: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.plusJakarta(size: 12, weight: .medium))
            .foregroundStyle(RetailRose.textBody)
            .lineLimit(1)
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(RetailRose.chip)
            .clipShape(Capsule())
            .overlay(Capsule().stroke(RetailRose.roseBorder, lineWidth: 1))
    }
}

/// Card header: bold title, optional trailing chip, optional `View all ›`.
struct RoseSectionHeader: View {
    let title: String
    var chipText: String? = nil
    var onViewAll: (() -> Void)? = nil

    var body: some View {
        HStack(spacing: 8) {
            Text(title)
                .font(.plusJakarta(size: 16, weight: .bold))
                .foregroundStyle(RetailRose.textPrimary)
                .lineLimit(1)
                .frame(maxWidth: .infinity, alignment: .leading)
            if let chipText { FilledRoseChip(text: chipText) }
            if let onViewAll {
                Button(action: onViewAll) {
                    Text("View all ›")
                        .font(.plusJakarta(size: 12, weight: .bold))
                        .foregroundStyle(RetailRose.amber)
                        .padding(.horizontal, 4)
                        .padding(.vertical, 2)
                }
                .buttonStyle(.plain)
            }
        }
    }
}

/// Board header: title + amber trailing note (used above the slate grids).
struct RoseBoardHeader: View {
    let title: String
    var trailingNote: String? = nil
    var onViewAll: (() -> Void)? = nil

    var body: some View {
        HStack(spacing: 8) {
            Text(title)
                .font(.plusJakarta(size: 16, weight: .bold))
                .foregroundStyle(RetailRose.textPrimary)
                .lineLimit(1)
            Spacer(minLength: 8)
            if let trailingNote {
                Text(trailingNote)
                    .font(.plusJakarta(size: 12, weight: .bold))
                    .foregroundStyle(RetailRose.amber)
                    .lineLimit(1)
            }
            if let onViewAll {
                Button(action: onViewAll) {
                    Text("View all ›")
                        .font(.plusJakarta(size: 12, weight: .bold))
                        .foregroundStyle(RetailRose.amber)
                }
                .buttonStyle(.plain)
            }
        }
    }
}

/// `#170d11` metric chip: 11/Medium label + 18/Bold value.
struct MetricChip: View {
    let label: String
    let value: String
    var valueColor: Color = RetailRose.textPrimary

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.plusJakarta(size: 11, weight: .medium))
                .foregroundStyle(RetailRose.textBody.opacity(0.62))
                .lineLimit(1)
                .minimumScaleFactor(0.8)
            Text(value)
                .font(.plusJakarta(size: 18, weight: .bold))
                .foregroundStyle(valueColor)
                .lineLimit(1)
                .minimumScaleFactor(0.7)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .background(RetailRose.surfaceRow)
        .clipShape(RoundedRectangle(cornerRadius: RetailRose.rMetric, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: RetailRose.rMetric, style: .continuous)
                .stroke(RetailRose.chipStroke, lineWidth: 1)
        )
    }
}

/// KPI tile inside a slate board card (`rgba(0,0,0,.28)` + 6% white hairline).
struct KpiChip: View {
    let label: String
    let value: String
    var valueColor: Color = RetailRose.textPrimary

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.plusJakarta(size: 11, weight: .medium))
                .foregroundStyle(Color.white.opacity(0.6))
                .lineLimit(1)
                .minimumScaleFactor(0.75)
            Text(value)
                .font(.plusJakarta(size: 18, weight: .bold))
                .foregroundStyle(valueColor)
                .lineLimit(1)
                .minimumScaleFactor(0.7)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .background(RetailRose.kpiOnSlate)
        .clipShape(RoundedRectangle(cornerRadius: RetailRose.rRow, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: RetailRose.rRow, style: .continuous)
                .stroke(RetailRose.chipStroke, lineWidth: 1)
        )
    }
}

/// Tonal severity pill (`Unavailable`, `Overdue`, `Alert`, `Part`, `Open`).
struct SeverityChip: View {
    let label: String
    let color: Color
    var bordered: Bool = true

    var body: some View {
        Text(label)
            .font(.plusJakarta(size: 10, weight: .bold))
            .foregroundStyle(color)
            .lineLimit(1)
            .fixedSize()
            .padding(.horizontal, 9)
            .padding(.vertical, 4)
            .background(RetailRose.tone(color))
            .clipShape(Capsule())
            .overlay(bordered ? Capsule().stroke(RetailRose.toneStroke(color), lineWidth: 1) : nil)
    }
}

/// Solid tinted status pill (`● On track`).
struct StatusPill: View {
    let label: String
    let color: Color

    var body: some View {
        Text(label)
            .font(.plusJakarta(size: 10, weight: .bold))
            .foregroundStyle(color)
            .lineLimit(1)
            .fixedSize()
            .padding(.horizontal, 8)
            .padding(.vertical, 3)
            .background(RetailRose.tone(color))
            .clipShape(Capsule())
    }
}

/// `Why` + body at 50% — the honesty rule line under each Pulse card.
struct RoseWhyCaption: View {
    let body: String

    var body: some View {
        HStack(alignment: .top, spacing: 8) {
            Text("Why")
                .font(.plusJakarta(size: 11, weight: .bold))
                .foregroundStyle(Color.white.opacity(0.5))
                .lineLimit(1)
            Text(body)
                .font(.plusJakarta(size: 11, weight: .regular))
                .foregroundStyle(Color.white.opacity(0.5))
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.leading, 2)
        .padding(.top, 2)
    }
}

/// Slate-board variant: amber `Why`, optional rose-tinted note box.
struct RoseWhyNote: View {
    let body: String
    var tinted: Bool = false

    var body: some View {
        HStack(alignment: .top, spacing: 8) {
            Text("Why")
                .font(.plusJakarta(size: 11, weight: .bold))
                .foregroundStyle(RetailRose.amber)
                .lineLimit(1)
            Text(body)
                .font(.plusJakarta(size: 11, weight: .regular))
                .foregroundStyle(RetailRose.roseSoft)
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(tinted ? 10 : 2)
        .background(tinted ? RetailRose.rose.opacity(0.12) : .clear)
        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
        .overlay(
            tinted
                ? RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .stroke(RetailRose.rose.opacity(0.4), lineWidth: 1)
                : nil
        )
    }
}

/// Health ring from Figma `2603:29970`: 92pt track (white 8%) + rose arc with glow.
struct HealthRing: View {
    let percentLabel: String
    let caption: String
    /// `nil` renders the bare track — the design never prints `0%` for missing coverage.
    let fraction: Double?

    var body: some View {
        ZStack {
            Circle()
                .stroke(Color.white.opacity(0.08), style: StrokeStyle(lineWidth: 8.28))
            if let fraction {
                Circle()
                    .trim(from: 0, to: min(max(fraction, 0), 1))
                    .stroke(RetailRose.rose, style: StrokeStyle(lineWidth: 8.28, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                    .shadow(color: RetailRose.rose.opacity(0.6), radius: 6)
            }
            VStack(spacing: 0) {
                Text(percentLabel)
                    .font(.plusJakarta(size: 22, weight: .heavy))
                    .foregroundStyle(RetailRose.textPrimary)
                    .lineLimit(1)
                    .minimumScaleFactor(0.6)
                Text(caption)
                    .font(.plusJakarta(size: 10, weight: .medium))
                    .foregroundStyle(Color.white.opacity(0.55))
                    .lineLimit(1)
            }
        }
        .frame(width: 92, height: 92)
    }
}

/// Row with a 3pt coloured left bar (Attention / Open-Commitments rows).
struct AccentRow: View {
    let title: String
    let caption: String
    let accent: Color
    var trailingValue: String? = nil
    var trailingColor: Color = RetailRose.textPrimary
    var severityLabel: String? = nil
    var severityColor: Color? = nil
    var onTap: (() -> Void)? = nil

    var body: some View {
        HStack(spacing: 10) {
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.plusJakarta(size: 13, weight: .medium))
                    .foregroundStyle(RetailRose.textPrimary)
                    .lineLimit(2)
                Text(caption)
                    .font(.plusJakarta(size: 11, weight: .regular))
                    .foregroundStyle(RetailRose.textBody)
                    .lineLimit(2)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            if let trailingValue {
                Text(trailingValue)
                    .font(.plusJakarta(size: 13, weight: .bold))
                    .foregroundStyle(trailingColor)
                    .lineLimit(1)
            }
            if let severityLabel {
                SeverityChip(label: severityLabel, color: severityColor ?? accent)
            }
        }
        .padding(.leading, 14)
        .padding(.trailing, 10)
        .padding(.vertical, 10)
        .background(RetailRose.surfaceRow)
        .clipShape(RoundedRectangle(cornerRadius: RetailRose.rRow, style: .continuous))
        .overlay(alignment: .leading) {
            Rectangle()
                .fill(accent)
                .frame(width: 3)
                .clipShape(UnevenRoundedRectangle(
                    topLeadingRadius: RetailRose.rRow,
                    bottomLeadingRadius: RetailRose.rRow,
                    bottomTrailingRadius: 0,
                    topTrailingRadius: 0,
                    style: .continuous
                ))
        }
        .contentShape(Rectangle())
        .onTapGesture { onTap?() }
    }
}

/// Divider row inside a Pulse card (bottom hairline `#4c0519` at 60%).
struct DividedRow: View {
    let title: String
    let caption: String?
    let trailingValue: String
    var trailingColor: Color = RetailRose.textPrimary
    var captionColor: Color = RetailRose.textBody
    var onTap: (() -> Void)? = nil

    var body: some View {
        HStack(spacing: 8) {
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.plusJakarta(size: 13, weight: .medium))
                    .foregroundStyle(RetailRose.textPrimary)
                    .lineLimit(1)
                if let caption, !caption.isEmpty {
                    Text(caption)
                        .font(.plusJakarta(size: 11, weight: .regular))
                        .foregroundStyle(captionColor)
                        .lineLimit(1)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            Text(trailingValue)
                .font(.plusJakarta(size: 13, weight: .bold))
                .foregroundStyle(trailingColor)
                .lineLimit(1)
        }
        .padding(.vertical, 8)
        .overlay(alignment: .bottom) {
            Rectangle().fill(RetailRose.divider).frame(height: 1)
        }
        .contentShape(Rectangle())
        .onTapGesture { onTap?() }
    }
}

/// Row inside a slate board card: 3pt bar + inner KPI well + trailing status.
struct BoardRow: View {
    let eyebrow: String
    let value: String
    var valueColor: Color = RetailRose.textBody
    let trailing: String
    var trailingColor: Color = RetailRose.textPrimary
    var accent: Color = RetailRose.rose
    var severity: String? = nil
    var severityColor: Color? = nil
    var onTap: (() -> Void)? = nil

    var body: some View {
        HStack(spacing: 10) {
            VStack(alignment: .leading, spacing: 4) {
                Text(eyebrow)
                    .font(.plusJakarta(size: 11, weight: .medium))
                    .foregroundStyle(colorOpacity(RetailRose.textPrimary))
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)
                Text(value)
                    .font(.plusJakarta(size: 18, weight: .bold))
                    .foregroundStyle(valueColor)
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .background(RetailRose.kpiOnSlate)
            .clipShape(RoundedRectangle(cornerRadius: RetailRose.rRow, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: RetailRose.rRow, style: .continuous)
                    .stroke(RetailRose.chipStroke, lineWidth: 1)
            )
            Text(trailing)
                .font(.plusJakarta(size: 13, weight: .bold))
                .foregroundStyle(trailingColor)
                .lineLimit(1)
            if let severity {
                SeverityChip(label: severity, color: severityColor ?? accent, bordered: false)
            }
        }
        .padding(.leading, 12)
        .padding(.trailing, 10)
        .padding(.vertical, 10)
        .background(Color.black.opacity(0.24))
        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
        .overlay(alignment: .leading) {
            Rectangle().fill(accent).frame(width: 3)
        }
        .contentShape(Rectangle())
        .onTapGesture { onTap?() }
    }

    private func colorOpacity(_ color: Color) -> Color {
        color.opacity(0.92)
    }
}

/// Board hero used by Moments / Life / Memory.
struct BoardHero<Chips: View, Kpis: View>: View {
    let eyebrow: String
    let headline: String
    var strokeAlpha: Double = 0.45
    @ViewBuilder var chips: () -> Chips
    @ViewBuilder var kpis: () -> Kpis

    var body: some View {
        RoseCard(tone: .board, strokeAlpha: strokeAlpha, gap: 14, padding: 16) {
            Text(eyebrow)
                .font(.plusJakarta(size: 10, weight: .semibold))
                .tracking(1.2)
                .foregroundStyle(RetailRose.rose)
                .lineLimit(1)
            Text(headline)
                .font(.plusJakarta(size: 22, weight: .heavy))
                .foregroundStyle(RetailRose.textPrimary)
                .lineLimit(2)
                .minimumScaleFactor(0.8)
            chips()
            kpis()
        }
    }
}

/// Small hero chip (`Retail`, `Store open`, `vs Oct 2026`).
struct BoardHeroChip: View {
    let text: String
    let color: Color
    var dot: Bool = false

    var body: some View {
        HStack(spacing: 6) {
            if dot {
                Circle().fill(color).frame(width: 6, height: 6)
            }
            Text(text)
                .font(.plusJakarta(size: 11, weight: .semibold))
                .foregroundStyle(color)
                .lineLimit(1)
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 4)
        .background(RetailRose.tone(color))
        .clipShape(Capsule())
        .overlay(Capsule().stroke(color.opacity(0.35), lineWidth: 1))
    }
}

/// White pill CTA (`+ Record Sale`).
struct RoseCta: View {
    let label: String
    var plus: Bool = false
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 6) {
                if plus {
                    Image("RetailPlus")
                        .renderingMode(.template)
                        .resizable()
                        .scaledToFit()
                        .frame(width: 12, height: 12)
                        .foregroundStyle(RetailRose.rose)
                }
                Text(label)
                    .font(.plusJakarta(size: 15, weight: .bold))
            }
            .foregroundStyle(RetailRose.rose)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 16)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: RetailRose.rCardTight, style: .continuous))
            .shadow(color: RetailRose.rose.opacity(0.6), radius: 12, x: 0, y: 8)
        }
        .buttonStyle(.plain)
    }
}

/// `Core · 9` + hairline + `Retail journey`.
struct GridSectionHeader: View {
    let title: String
    let note: String

    var body: some View {
        HStack(spacing: 8) {
            Text(title)
                .font(.plusJakarta(size: 16, weight: .bold))
                .foregroundStyle(RetailRose.textPrimary)
                .lineLimit(1)
            Rectangle()
                .fill(Color.white.opacity(0.08))
                .frame(height: 1)
                .frame(maxWidth: .infinity)
            Text(note)
                .font(.plusJakarta(size: 11, weight: .medium))
                .foregroundStyle(RetailRose.amber)
                .lineLimit(1)
        }
    }
}

/// Quick Add tile: 112 tall, 32 icon well, 14/Bold label, 10/Regular caption.
struct QuickAddTile: View {
    let asset: String
    let label: String
    let caption: String
    var locked: String? = nil
    var onTap: (() -> Void)? = nil

    var body: some View {
        Button {
            if locked == nil { onTap?() }
        } label: {
            VStack(alignment: .leading, spacing: 6) {
                HStack {
                    ZStack {
                        RoundedRectangle(cornerRadius: RetailRose.rTileIcon, style: .continuous)
                            .fill(RetailRose.tone(RetailRose.rose))
                        RoundedRectangle(cornerRadius: RetailRose.rTileIcon, style: .continuous)
                            .stroke(RetailRose.toneStroke(RetailRose.rose), lineWidth: 1)
                        Image(asset)
                            .renderingMode(.template)
                            .resizable()
                            .scaledToFit()
                            .frame(width: 18, height: 18)
                            .foregroundStyle(locked == nil ? RetailRose.rose : RetailRose.textFaint)
                    }
                    .frame(width: 32, height: 32)
                    .shadow(color: RetailRose.rose.opacity(locked == nil ? 0.5 : 0), radius: 6)
                    Spacer(minLength: 0)
                }
                Text(label)
                    .font(.plusJakarta(size: 14, weight: .bold))
                    .foregroundStyle(locked == nil ? RetailRose.textPrimary : RetailRose.textMuted)
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)
                Text(locked ?? caption)
                    .font(.plusJakarta(size: 10, weight: .regular))
                    .foregroundStyle(locked == nil ? RetailRose.textMuted : RetailRose.amber.opacity(0.8))
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)
                Spacer(minLength: 0)
            }
            .frame(maxWidth: .infinity, minHeight: 112, alignment: .topLeading)
            .padding(12)
            .background(RetailRose.tile)
            .clipShape(RoundedRectangle(cornerRadius: RetailRose.rTile, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: RetailRose.rTile, style: .continuous)
                    .stroke(locked == nil ? RetailRose.tileStroke : Color.white.opacity(0.08), lineWidth: 1)
            )
            .shadow(color: RetailRose.rose.opacity(0.15), radius: 12, x: 0, y: 8)
            .shadow(color: .black.opacity(0.4), radius: 6, x: 0, y: 4)
        }
        .buttonStyle(.plain)
        .disabled(locked != nil)
    }
}

/// Empty state in the rose language — never shows a fabricated `0%`.
struct RoseEmpty: View {
    let title: String
    let body: String
    var glyph: String = "◇"

    var body: some View {
        HStack(spacing: 12) {
            Text(glyph)
                .font(.plusJakarta(size: 18, weight: .semibold))
                .foregroundStyle(RetailRose.rose)
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.plusJakarta(size: 13, weight: .medium))
                    .foregroundStyle(RetailRose.textPrimary)
                Text(body)
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(RetailRose.textBody)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(14)
        .background(RetailRose.surfaceRow)
        .clipShape(RoundedRectangle(cornerRadius: RetailRose.rRow, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: RetailRose.rRow, style: .continuous)
                .stroke(Color.white.opacity(0.06), lineWidth: 1)
        )
    }
}

/// Tile assets (real Figma vectors in `Assets.xcassets/RetailRose`).
enum RetailTileIcons {
    static let sale = "RetailTileSale"
    static let purchase = "RetailTilePurchase"
    static let collection = "RetailTileCollection"
    static let payment = "RetailTilePayment"
    static let expense = "RetailTileExpense"
    static let stock = "RetailTileStock"
    static let order = "RetailTileOrder"
    static let status = "RetailTileStatus"
    static let budget = "RetailTileBudget"
    static let suppliers = "RetailTileSuppliers"
    static let catalogue = "RetailTileCatalogue"
    static let locations = "RetailTileLocations"
    static let invite = "RetailTileInvite"
    static let issue = "RetailTileIssue"
}
