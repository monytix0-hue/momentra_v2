import SwiftUI

// MARK: - Screenshot helper (Figma frame → SwiftUI)

/// Shared visual primitives for the restaurant section, matching Figma `2487:37611`.
/// Every Pulse card and every Moments / Life / Memory board is the same `#292524`
/// surface with a 20pt radius, so they share one `EmberCard` implementation.
enum EmberTone {
    case pulse
    case board
}

/// 3-stop vertical wash behind every restaurant screen.
struct EmberScreenBackground<Content: View>: View {
    @ViewBuilder var content: () -> Content

    var body: some View {
        ZStack {
            RestaurantEmber.screenGradient.ignoresSafeArea()
            content()
        }
    }
}

/// Card with the Figma ember double shadow plus the inset top highlight.
struct EmberCard<Content: View>: View {
    var tone: EmberTone = .pulse
    var strokeAlpha: Double? = nil
    var gap: CGFloat = 14
    var padding: CGFloat = 16
    var onTap: (() -> Void)? = nil
    @ViewBuilder var content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: gap, content: content)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(padding)
            .background(RestaurantEmber.card)
            .clipShape(RoundedRectangle(cornerRadius: RestaurantEmber.rCard, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: RestaurantEmber.rCard, style: .continuous)
                    .stroke(RestaurantEmber.ember.opacity(strokeAlpha ?? 0.18), lineWidth: 1)
            )
            .emberTopHighlight()
            .emberCardShadow()
            .contentShape(Rectangle())
            .onTapGesture { onTap?() }
    }
}

/// Screen title (24/Bold) + gold role line (12/Medium).
struct EmberScreenHeader: View {
    let title: String
    let roleLine: String

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(title)
                .font(.plusJakarta(size: 24, weight: .bold))
                .foregroundStyle(RestaurantEmber.textPrimary)
                .lineLimit(1)
                .minimumScaleFactor(0.8)
            Text(roleLine)
                .font(.plusJakarta(size: 12, weight: .medium))
                .foregroundStyle(RestaurantEmber.gold)
                .lineLimit(1)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

/// Filled wine chip (`Restaurant`) + outline chips (`Nov 2026 · INR`, `GST on`)
/// + the ember `+ Quick Adds` chip from `2400:29655`.
struct EmberContextChips: View {
    let subtypeLabel: String
    let periodLine: String
    let gstOn: Bool
    var quickAdds: Bool = false
    var onPeriod: (() -> Void)? = nil
    var onQuickAdds: (() -> Void)? = nil

    var body: some View {
        HStack(spacing: 6) {
            FilledEmberChip(text: subtypeLabel)
            if let onPeriod {
                Button { onPeriod() } label: { OutlineEmberChip(text: periodLine) }
                    .buttonStyle(.plain)
            } else {
                OutlineEmberChip(text: periodLine)
            }
            OutlineEmberChip(text: gstOn ? "GST on" : "GST off")
            if quickAdds, let onQuickAdds {
                Button { onQuickAdds() } label: {
                    Text("+ Quick Adds")
                        .font(.plusJakarta(size: 12, weight: .medium))
                        .foregroundStyle(.white)
                        .lineLimit(1)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(RestaurantEmber.ember)
                        .clipShape(Capsule())
                }
                .buttonStyle(.plain)
            }
            Spacer(minLength: 0)
        }
    }
}

struct FilledEmberChip: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.plusJakarta(size: 12, weight: .bold))
            .foregroundStyle(.white)
            .lineLimit(1)
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(RestaurantEmber.wine)
            .clipShape(Capsule())
            .overlay(Capsule().stroke(RestaurantEmber.wine, lineWidth: 1))
    }
}

struct OutlineEmberChip: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.plusJakarta(size: 12, weight: .medium))
            .foregroundStyle(RestaurantEmber.textBody)
            .lineLimit(1)
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(RestaurantEmber.chip)
            .clipShape(Capsule())
            .overlay(Capsule().stroke(Color.white.opacity(0.06), lineWidth: 1))
    }
}

/// Card header: bold title, optional trailing chip, optional `View all ›`.
struct EmberSectionHeader: View {
    let title: String
    var chipText: String? = nil
    var onViewAll: (() -> Void)? = nil

    var body: some View {
        HStack(spacing: 8) {
            Text(title)
                .font(.plusJakarta(size: 16, weight: .bold))
                .foregroundStyle(RestaurantEmber.textPrimary)
                .lineLimit(1)
                .frame(maxWidth: .infinity, alignment: .leading)
            if let chipText { FilledEmberChip(text: chipText) }
            if let onViewAll {
                Button(action: onViewAll) {
                    Text("View all ›")
                        .font(.plusJakarta(size: 12, weight: .bold))
                        .foregroundStyle(RestaurantEmber.gold)
                        .padding(.horizontal, 4)
                        .padding(.vertical, 2)
                }
                .buttonStyle(.plain)
            }
        }
    }
}

/// Board header: title + gold trailing note.
struct EmberBoardHeader: View {
    let title: String
    var trailingNote: String? = nil
    var onViewAll: (() -> Void)? = nil

    var body: some View {
        HStack(spacing: 8) {
            Text(title)
                .font(.plusJakarta(size: 16, weight: .bold))
                .foregroundStyle(RestaurantEmber.textPrimary)
                .lineLimit(1)
            Spacer(minLength: 8)
            if let trailingNote {
                Text(trailingNote)
                    .font(.plusJakarta(size: 12, weight: .bold))
                    .foregroundStyle(RestaurantEmber.gold)
                    .lineLimit(1)
            }
            if let onViewAll {
                Button(action: onViewAll) {
                    Text("View all ›")
                        .font(.plusJakarta(size: 12, weight: .bold))
                        .foregroundStyle(RestaurantEmber.gold)
                }
                .buttonStyle(.plain)
            }
        }
    }
}

/// `#1c1917` metric chip: 11/Medium label + 18/Bold value.
struct EmberMetricChip: View {
    let label: String
    let value: String
    var valueColor: Color = RestaurantEmber.textPrimary

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.plusJakarta(size: 11, weight: .medium))
                .foregroundStyle(RestaurantEmber.textBody.opacity(0.62))
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
        .background(RestaurantEmber.surfaceWell)
        .clipShape(RoundedRectangle(cornerRadius: RestaurantEmber.rMetric, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: RestaurantEmber.rMetric, style: .continuous)
                .stroke(Color.white.opacity(0.06), lineWidth: 1)
        )
    }
}

/// KPI tile inside a board card (`rgba(0,0,0,.24)` + 6% hairline).
struct EmberKpiChip: View {
    let label: String
    let value: String
    var valueColor: Color = RestaurantEmber.textPrimary

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
        .background(Color.black.opacity(0.24))
        .clipShape(RoundedRectangle(cornerRadius: RestaurantEmber.rRow, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: RestaurantEmber.rRow, style: .continuous)
                .stroke(Color.white.opacity(0.06), lineWidth: 1)
        )
    }
}

/// Tonal severity pill (`Unavailable`, `Overdue`, `Alert`, `Part`, `Open`).
struct EmberSeverityChip: View {
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
            .background(RestaurantEmber.tone(color))
            .clipShape(Capsule())
            .overlay(bordered ? Capsule().stroke(RestaurantEmber.toneStroke(color), lineWidth: 1) : nil)
    }
}

/// Solid tinted status pill (`● On track`).
struct EmberStatusPill: View {
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
            .background(RestaurantEmber.tone(color))
            .clipShape(Capsule())
    }
}

/// `Why` + body at 50% — the honesty rule line under each Pulse card.
struct EmberWhyCaption: View {
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

/// Board variant: ember `Why`, optional ember-tinted note box.
struct EmberWhyNote: View {
    let body: String
    var tinted: Bool = false

    var body: some View {
        HStack(alignment: .top, spacing: 8) {
            Text("Why")
                .font(.plusJakarta(size: 11, weight: .bold))
                .foregroundStyle(RestaurantEmber.ember)
                .lineLimit(1)
            Text(body)
                .font(.plusJakarta(size: 11, weight: .regular))
                .foregroundStyle(RestaurantEmber.textBody)
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(tinted ? 10 : 2)
        .background(tinted ? RestaurantEmber.ember.opacity(0.10) : .clear)
        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
        .overlay(
            tinted
                ? RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .stroke(RestaurantEmber.ember.opacity(0.35), lineWidth: 1)
                : nil
        )
    }
}

/// Health ring from Figma `2400:29677`: 92pt track (white 8%) + ember arc with glow.
struct EmberHealthRing: View {
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
                    .stroke(RestaurantEmber.ember, style: StrokeStyle(lineWidth: 8.28, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                    .shadow(color: RestaurantEmber.ember.opacity(0.6), radius: 6)
            }
            VStack(spacing: 0) {
                Text(percentLabel)
                    .font(.plusJakarta(size: 22, weight: .heavy))
                    .foregroundStyle(RestaurantEmber.textPrimary)
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
struct EmberAccentRow: View {
    let title: String
    let caption: String
    let accent: Color
    var trailingValue: String? = nil
    var trailingColor: Color = RestaurantEmber.textPrimary
    var severityLabel: String? = nil
    var severityColor: Color? = nil
    var onTap: (() -> Void)? = nil

    var body: some View {
        HStack(spacing: 10) {
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.plusJakarta(size: 13, weight: .medium))
                    .foregroundStyle(RestaurantEmber.textPrimary)
                    .lineLimit(2)
                Text(caption)
                    .font(.plusJakarta(size: 11, weight: .regular))
                    .foregroundStyle(RestaurantEmber.textBody)
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
                EmberSeverityChip(label: severityLabel, color: severityColor ?? accent)
            }
        }
        .padding(.leading, 14)
        .padding(.trailing, 10)
        .padding(.vertical, 10)
        .background(RestaurantEmber.surfaceWell)
        .clipShape(RoundedRectangle(cornerRadius: RestaurantEmber.rRow, style: .continuous))
        .overlay(alignment: .leading) {
            Rectangle()
                .fill(accent)
                .frame(width: 3)
                .clipShape(UnevenRoundedRectangle(
                    topLeadingRadius: RestaurantEmber.rRow,
                    bottomLeadingRadius: RestaurantEmber.rRow,
                    bottomTrailingRadius: 0,
                    topTrailingRadius: 0,
                    style: .continuous
                ))
        }
        .contentShape(Rectangle())
        .onTapGesture { onTap?() }
    }
}

/// Divider row inside a Pulse card (bottom hairline `#44403c`).
struct EmberDividedRow: View {
    let title: String
    let caption: String?
    let trailingValue: String
    var trailingColor: Color = RestaurantEmber.textPrimary
    var captionColor: Color = RestaurantEmber.textBody
    var onTap: (() -> Void)? = nil

    var body: some View {
        HStack(spacing: 8) {
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.plusJakarta(size: 13, weight: .medium))
                    .foregroundStyle(RestaurantEmber.textPrimary)
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
            Rectangle().fill(RestaurantEmber.divider).frame(height: 1)
        }
        .contentShape(Rectangle())
        .onTapGesture { onTap?() }
    }
}

/// Row inside a board card: 3pt bar + inner KPI well + trailing status.
struct EmberBoardRow: View {
    let eyebrow: String
    let value: String
    var valueColor: Color = RestaurantEmber.textPrimary
    let trailing: String
    var trailingColor: Color = RestaurantEmber.textPrimary
    var accent: Color = RestaurantEmber.ember
    var severity: String? = nil
    var severityColor: Color? = nil
    var onTap: (() -> Void)? = nil

    var body: some View {
        HStack(spacing: 10) {
            VStack(alignment: .leading, spacing: 4) {
                Text(eyebrow)
                    .font(.plusJakarta(size: 11, weight: .medium))
                    .foregroundStyle(RestaurantEmber.textPrimary.opacity(0.92))
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
            .background(Color.black.opacity(0.24))
            .clipShape(RoundedRectangle(cornerRadius: RestaurantEmber.rRow, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: RestaurantEmber.rRow, style: .continuous)
                    .stroke(Color.white.opacity(0.06), lineWidth: 1)
            )
            Text(trailing)
                .font(.plusJakarta(size: 13, weight: .bold))
                .foregroundStyle(trailingColor)
                .lineLimit(1)
            if let severity {
                EmberSeverityChip(label: severity, color: severityColor ?? accent, bordered: false)
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
}

/// Board hero used by Moments / Life / Memory.
struct EmberBoardHero<Chips: View, Kpis: View>: View {
    let eyebrow: String
    let headline: String
    var strokeAlpha: Double = 0.22
    @ViewBuilder var chips: () -> Chips
    @ViewBuilder var kpis: () -> Kpis

    var body: some View {
        EmberCard(tone: .board, strokeAlpha: strokeAlpha, gap: 14, padding: 16) {
            Text(eyebrow)
                .font(.plusJakarta(size: 10, weight: .semibold))
                .tracking(1.2)
                .foregroundStyle(RestaurantEmber.ember)
                .lineLimit(1)
            Text(headline)
                .font(.plusJakarta(size: 24, weight: .heavy))
                .foregroundStyle(RestaurantEmber.textPrimary)
                .lineLimit(2)
                .minimumScaleFactor(0.8)
            chips()
            kpis()
        }
    }
}

/// Small hero chip (`Restaurant`, `Service open`, `vs Oct 2026`).
struct EmberBoardHeroChip: View {
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
        .background(RestaurantEmber.tone(color))
        .clipShape(Capsule())
        .overlay(Capsule().stroke(color.opacity(0.35), lineWidth: 1))
    }
}

/// White pill CTA (`+  Record Sale`).
struct EmberCta: View {
    let label: String
    var plus: Bool = false
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 6) {
                if plus {
                    Image("RestaurantPlus")
                        .renderingMode(.template)
                        .resizable()
                        .scaledToFit()
                        .frame(width: 12, height: 12)
                        .foregroundStyle(RestaurantEmber.ember)
                }
                Text(label)
                    .font(.plusJakarta(size: 15, weight: .bold))
            }
            .foregroundStyle(RestaurantEmber.ember)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: RestaurantEmber.rCardTight, style: .continuous))
            .shadow(color: RestaurantEmber.ember.opacity(0.55), radius: 12, x: 0, y: 8)
        }
        .buttonStyle(.plain)
    }
}

/// `Core · 9` + hairline + `RFC Food journey`.
struct EmberGridSectionHeader: View {
    let title: String
    let note: String

    var body: some View {
        HStack(spacing: 8) {
            Text(title)
                .font(.plusJakarta(size: 16, weight: .bold))
                .foregroundStyle(RestaurantEmber.textPrimary)
                .lineLimit(1)
            Rectangle()
                .fill(Color.white.opacity(0.08))
                .frame(height: 1)
                .frame(maxWidth: .infinity)
            Text(note)
                .font(.plusJakarta(size: 11, weight: .medium))
                .foregroundStyle(RestaurantEmber.gold)
                .lineLimit(1)
        }
    }
}

/// Quick Add tile: 112 tall, 32 icon well, 14/Bold label, 10/Regular caption.
struct EmberQuickAddTile: View {
    let asset: String
    let label: String
    let caption: String
    var locked: String? = nil
    var onTap: (() -> Void)? = nil

    private var open: Bool { locked == nil }

    var body: some View {
        Button {
            if open { onTap?() }
        } label: {
            VStack(alignment: .leading, spacing: 6) {
                HStack {
                    ZStack {
                        RoundedRectangle(cornerRadius: RestaurantEmber.rTileIcon, style: .continuous)
                            .fill(open ? RestaurantEmber.ember : RestaurantEmber.hairline)
                        Image(asset)
                            .renderingMode(.template)
                            .resizable()
                            .scaledToFit()
                            .frame(width: 18, height: 18)
                            .foregroundStyle(open ? Color.white : RestaurantEmber.textFaint)
                    }
                    .frame(width: 32, height: 32)
                    .shadow(color: RestaurantEmber.ember.opacity(open ? 0.45 : 0), radius: 6)
                    Spacer(minLength: 0)
                }
                Text(label)
                    .font(.plusJakarta(size: 14, weight: .bold))
                    .foregroundStyle(open ? RestaurantEmber.textPrimary : RestaurantEmber.textFaint)
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)
                Text(locked ?? caption)
                    .font(.plusJakarta(size: 10, weight: .regular))
                    .foregroundStyle(open ? RestaurantEmber.textBody : RestaurantEmber.amber.opacity(0.8))
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)
                Spacer(minLength: 0)
            }
            .frame(maxWidth: .infinity, minHeight: 112, alignment: .topLeading)
            .padding(12)
            .background(RestaurantEmber.tile)
            .clipShape(RoundedRectangle(cornerRadius: RestaurantEmber.rTile, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: RestaurantEmber.rTile, style: .continuous)
                    .stroke(open ? RestaurantEmber.ember : Color.white.opacity(0.08), lineWidth: 1)
            )
            .shadow(color: RestaurantEmber.ember.opacity(open ? 0.18 : 0), radius: 12, x: 0, y: 8)
            .shadow(color: .black.opacity(0.35), radius: 6, x: 0, y: 4)
        }
        .buttonStyle(.plain)
        .disabled(!open)
    }
}

/// Empty state in the ember language — never shows a fabricated `0%`.
struct EmberEmpty: View {
    let title: String
    let body: String
    var glyph: String = "◇"

    var body: some View {
        HStack(spacing: 12) {
            Text(glyph)
                .font(.plusJakarta(size: 18, weight: .semibold))
                .foregroundStyle(RestaurantEmber.ember)
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.plusJakarta(size: 13, weight: .medium))
                    .foregroundStyle(RestaurantEmber.textPrimary)
                Text(body)
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(RestaurantEmber.textBody)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(14)
        .background(RestaurantEmber.surfaceWell)
        .clipShape(RoundedRectangle(cornerRadius: RestaurantEmber.rRow, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: RestaurantEmber.rRow, style: .continuous)
                .stroke(Color.white.opacity(0.06), lineWidth: 1)
        )
    }
}

/// 24pt filled ember glyph chip used by the Pulse card headers (`icon chip · alert`, `· target`).
struct EmberCardGlyph: View {
    let asset: String
    var size: CGFloat = 24

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: size * 0.34, style: .continuous)
                .fill(RestaurantEmber.ember)
            Image(asset)
                .renderingMode(.template)
                .resizable()
                .scaledToFit()
                .frame(width: size * 0.58, height: size * 0.58)
                .foregroundStyle(.white)
        }
        .frame(width: size, height: size)
    }
}

/// Card header used by every Pulse card: glyph chip, 16/Bold title, optional filled chip,
/// optional `View all ›` — matching `2400:29711` … `2400:29844`.
struct EmberCardHeader: View {
    let asset: String
    let title: String
    var chipText: String? = nil
    var chipColor: Color = RestaurantEmber.negative
    var viewAllLabel: String = "View all ›"
    var onViewAll: (() -> Void)? = nil

    var body: some View {
        HStack(spacing: 8) {
            EmberCardGlyph(asset: asset)
            Text(title)
                .font(.plusJakarta(size: 16, weight: .bold))
                .foregroundStyle(RestaurantEmber.textPrimary)
                .lineLimit(1)
                .frame(maxWidth: .infinity, alignment: .leading)
            if let chipText {
                Text(chipText)
                    .font(.plusJakarta(size: 12, weight: .bold))
                    .foregroundStyle(RestaurantEmber.inkOnEmber)
                    .lineLimit(1)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(chipColor)
                    .clipShape(Capsule())
            }
            if let onViewAll {
                Button(action: onViewAll) {
                    Text(viewAllLabel)
                        .font(.plusJakarta(size: 12, weight: .bold))
                        .foregroundStyle(RestaurantEmber.gold)
                        .lineLimit(1)
                }
                .buttonStyle(.plain)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

/// 36pt rounded ember icon well used by sheet and CTA headers.
struct EmberIconWell: View {
    let asset: String
    var size: CGFloat = 36

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: size * 0.33, style: .continuous)
                .fill(RestaurantEmber.ember.opacity(0.16))
            RoundedRectangle(cornerRadius: size * 0.33, style: .continuous)
                .stroke(RestaurantEmber.ember.opacity(0.45), lineWidth: 1)
            Image(asset)
                .renderingMode(.template)
                .resizable()
                .scaledToFit()
                .frame(width: size * 0.55, height: size * 0.55)
                .foregroundStyle(RestaurantEmber.ember)
        }
        .frame(width: size, height: size)
    }
}

/// Chip used by the POS / KOT mode rows: filled ember when selected, `#44403c` otherwise.
struct EmberModeChip: View {
    let text: String
    var selected: Bool = false
    var locked: String? = nil
    var onTap: (() -> Void)? = nil

    var body: some View {
        Button { if locked == nil { onTap?() } } label: {
            Text(text)
                .font(.plusJakarta(size: 12, weight: selected ? .bold : .medium))
                .foregroundStyle(locked != nil ? RestaurantEmber.textFaint : (selected ? Color.white : RestaurantEmber.textBody))
                .lineLimit(1)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(selected ? RestaurantEmber.ember : RestaurantEmber.chip)
                .clipShape(Capsule())
                .overlay(
                    Capsule().stroke(
                        locked != nil ? RestaurantEmber.hairline.opacity(0.5)
                            : (selected ? RestaurantEmber.ember : Color.clear),
                        lineWidth: 1
                    )
                )
        }
        .buttonStyle(.plain)
        .disabled(locked != nil)
    }
}

/// Filled ember CTA used in every sheet footer.
struct EmberPrimaryButton: View {
    let label: String
    var enabled: Bool = true
    var loading: Bool = false
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 8) {
                if loading { ProgressView().tint(.white).scaleEffect(0.8) }
                Text(label)
                    .font(.plusJakarta(size: 15, weight: .bold))
            }
            .foregroundStyle(enabled ? Color.white : RestaurantEmber.textFaint)
            .frame(maxWidth: .infinity)
            .frame(height: 51)
            .background(enabled ? RestaurantEmber.ember : RestaurantEmber.surfaceWell)
            .clipShape(RoundedRectangle(cornerRadius: RestaurantEmber.rRow, style: .continuous))
            .shadow(color: enabled ? RestaurantEmber.ember.opacity(0.45) : .clear, radius: 10, x: 0, y: 4)
        }
        .buttonStyle(.plain)
        .disabled(!enabled || loading)
    }
}

// MARK: - Sheet chrome
//
// The same six helpers the Android kit carries (`RestaurantQuickAdds.kt:436-583`), so the
// POS / KOT / masters sheets read identically on both platforms.

/// 40 × 4 ember-55% grabber pill at the top of every `RST-QA-*` sheet.
struct EmberSheetGrabber: View {
    var body: some View {
        HStack { Spacer(minLength: 0) }
            .frame(height: 22)
            .overlay(alignment: .top) {
                Capsule()
                    .fill(RestaurantEmber.ember.opacity(0.55))
                    .frame(width: 40, height: 4)
                    .padding(.top, 9)
            }
    }
}

/// 20/Bold title, 32pt close chip, then the sheet's one-line subtitle.
struct EmberSheetHeader: View {
    let title: String
    var subtitle: String? = nil
    let onClose: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 10) {
                Text(title)
                    .font(.plusJakarta(size: 20, weight: .bold))
                    .foregroundStyle(RestaurantEmber.textPrimary)
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Button(action: onClose) {
                    Text("✕")
                        .font(.plusJakarta(size: 12, weight: .bold))
                        .foregroundStyle(RestaurantEmber.textBody)
                        .frame(width: 32, height: 32)
                        .background(Color.white.opacity(0.06))
                        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Close")
            }
            if let subtitle, !subtitle.trimmingCharacters(in: .whitespaces).isEmpty {
                Text(subtitle)
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(RestaurantEmber.textBody)
                    .lineLimit(2)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }
}

/// 10pt tracked field label above an input — every `RST-QA-*` frame carries one.
struct EmberFieldLabel: View {
    let text: String

    init(_ text: String) { self.text = text }

    var body: some View {
        Text(text)
            .font(.plusJakarta(size: 10, weight: .medium))
            .tracking(0.4)
            .foregroundStyle(RestaurantEmber.textFaint)
            .lineLimit(1)
    }
}

/// `#44403c` input row used by the POS / KOT / master forms.
struct EmberTextField: View {
    @Binding var value: String
    var placeholder: String

    var body: some View {
        TextField(
            "",
            text: $value,
            prompt: Text(placeholder).foregroundStyle(RestaurantEmber.textFaint)
        )
        .font(.plusJakarta(size: 14))
        .foregroundStyle(RestaurantEmber.textPrimary)
        .textInputAutocapitalization(.sentences)
        .autocorrectionDisabled()
        .frame(height: 46)
        .padding(.horizontal, 12)
        .background(RestaurantEmber.chip)
        .overlay(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .stroke(RestaurantEmber.hairline.opacity(0.6), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

/// Disabled row that states exactly why a frame control cannot be posted.
struct EmberLockedField: View {
    let label: String
    let value: String
    let reason: String

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            EmberFieldLabel(label)
            HStack(spacing: 8) {
                Text(value)
                    .font(.plusJakarta(size: 13, weight: .medium))
                    .foregroundStyle(RestaurantEmber.textFaint)
                    .lineLimit(1)
                Spacer(minLength: 0)
                EmberSeverityChip(label: "Unavailable", color: RestaurantEmber.warning)
            }
            .frame(height: 46)
            .padding(.horizontal, 12)
            .background(RestaurantEmber.surfaceWell)
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .stroke(RestaurantEmber.hairline.opacity(0.35), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            Text(reason)
                .font(.plusJakarta(size: 10))
                .foregroundStyle(RestaurantEmber.amber.opacity(0.85))
                .fixedSize(horizontal: false, vertical: true)
        }
    }
}

/// Footer CTA + the sheet's one-line caveat underneath.
struct EmberSheetFooter: View {
    let label: String
    var enabled: Bool = true
    var caveat: String
    let onTap: () -> Void

    var body: some View {
        VStack(spacing: 6) {
            EmberPrimaryButton(label: label, enabled: enabled, onTap: onTap)
            Text(caveat)
                .font(.plusJakarta(size: 10))
                .foregroundStyle(RestaurantEmber.textFaint)
                .lineLimit(2)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
    }
}

/// `Subtotal / GST / Service` money row.
struct EmberTotalRow: View {
    let label: String
    let value: String
    var valueColor: Color = RestaurantEmber.textPrimary
    var caption: String? = nil

    var body: some View {
        HStack(alignment: .top, spacing: 8) {
            VStack(alignment: .leading, spacing: 1) {
                Text(label)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(RestaurantEmber.textBody)
                if let caption {
                    Text(caption)
                        .font(.plusJakarta(size: 10))
                        .foregroundStyle(RestaurantEmber.textFaint)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            Text(value)
                .font(.plusJakarta(size: 14, weight: .bold))
                .foregroundStyle(valueColor)
                .lineLimit(1)
        }
    }
}

/// Tile assets (real Figma vectors in `Assets.xcassets/RestaurantEmber`).
enum RestaurantTileIcons {
    static let sale = "RestaurantTileSale"
    static let purchase = "RestaurantTilePurchase"
    static let collection = "RestaurantTileCollection"
    static let payment = "RestaurantTilePayment"
    static let expense = "RestaurantTileExpense"
    static let stock = "RestaurantTileStock"
    static let order = "RestaurantTileOrder"
    static let status = "RestaurantTileStatus"
    static let budget = "RestaurantTileBudget"
    static let vendors = "RestaurantTileVendors"
    static let menu = "RestaurantTileMenu"
    static let stations = "RestaurantTileStations"
    static let invite = "RestaurantTileInvite"
    static let issue = "RestaurantTileIssue"
    static let heroGlyph = "RestaurantHeroGlyph"
    static let plus = "RestaurantPlus"
    static let tabPulse = "RestaurantTabPulse"
    static let tabMoments = "RestaurantTabMoments"
    static let tabQuickAdd = "RestaurantTabQuickAdd"
    static let tabLife = "RestaurantTabLife"
    static let tabMemory = "RestaurantTabMemory"
    static let qrGlyph = "RestaurantQrGlyph"
    static let qrBg = "RestaurantQrBg"
    static let ringTrack = "RestaurantRingTrack"
    static let ringProgress = "RestaurantRingProgress"
    static let chevronUp = "RestaurantChevronUp"
    static let statusDot = "RestaurantStatusDot"
    static let activeDot = "RestaurantActiveDot"
}
