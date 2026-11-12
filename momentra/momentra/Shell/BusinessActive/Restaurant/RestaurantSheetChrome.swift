import SwiftUI
import UIKit

// MARK: - Restaurant capture chrome (Figma RST-QA Ember / Stone · Spice Route)

enum EmberSheetChrome {
    /// Gold labels / Why / Add line (`#FCD34D`).
    static let gold = BusinessTokens.Colors.restaurantAccentSoft
    /// Why body cream (`#FEF3C7`).
    static let whyBody = BusinessTokens.Colors.restaurantWhyBody
    /// Sheet / card surface (`#292524`).
    static let sheet = BusinessTokens.Colors.restaurantSurface
    /// Nested field / chip well (`#44403C`).
    static let chip = BusinessTokens.Colors.restaurantCanvasTop
    /// Deep canvas / ink on ember (`#1C1917`).
    static let inkOnEmber = BusinessTokens.Colors.restaurantCanvas
    /// Inactive chip / field hairline (`#57534E`).
    static let hairline = BusinessTokens.Colors.restaurantHairline
    /// Ember accent (`#EA580C`).
    static let ember = BusinessTokens.Colors.restaurantAccent
    /// Wine / hero start (`#881337`).
    static let wine = BusinessTokens.Colors.restaurantHeroStart
    /// Grab handle (`#78716C`).
    static let grab = Color(hex: "#78716C")
    /// Tender selected fill (`#22C55E`).
    static let tenderSelected = Color(hex: "#22C55E")
    /// Amber Hold / Later AR (`#FBBF24`).
    static let amber = Color(hex: "#FBBF24")
    /// Negative / Alert (`#EF4444`).
    static let negative = Color(hex: "#EF4444")
    /// Primary / body / faint stone text.
    static let textPrimary = Color(hex: "#FAFAF9")
    static let textBody = Color(hex: "#A8A29E")
    static let textFaint = Color(hex: "#78716C")
}

struct EmberGrabber: View {
    var body: some View {
        Capsule()
            .fill(EmberSheetChrome.grab)
            .frame(width: 40, height: 4)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 4)
    }
}

struct EmberSheetHeader: View {
    let title: String
    var subtitle: String? = nil
    var onClose: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(alignment: .center, spacing: 8) {
                Text(title)
                    .font(.plusJakarta(size: 22, weight: .bold))
                    .foregroundStyle(EmberSheetChrome.textPrimary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Button(action: onClose) {
                    Text("✕")
                        .font(.plusJakarta(size: 13, weight: .bold))
                        .foregroundStyle(EmberSheetChrome.textBody)
                        .frame(width: 32, height: 32)
                        .background(EmberSheetChrome.chip)
                        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Close")
            }
            if let subtitle {
                Text(subtitle)
                    .font(.plusJakarta(size: 12, weight: .medium))
                    .foregroundStyle(EmberSheetChrome.textBody)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
    }
}

struct EmberContextLine: View {
    let text: String
    var body: some View {
        Text(text)
            .font(.plusJakarta(size: 12, weight: .medium))
            .foregroundStyle(EmberSheetChrome.textBody)
            .frame(maxWidth: .infinity, alignment: .leading)
    }
}

struct EmberFieldLabel: View {
    let text: String
    var body: some View {
        Text(text)
            .font(.plusJakarta(size: 11, weight: .medium))
            .foregroundStyle(EmberSheetChrome.textBody)
    }
}

enum EmberChipStyle {
    /// Mode chips — dark ink on ember + gold border.
    case mode
    /// Channel / context chips — dark ink on wine + gold border.
    case channel
    /// Tender selected — green fill + gold border.
    case tender
    /// Course Now — green fill + gold border.
    case courseNow
    /// Course Hold / Fire now — amber fill + gold border.
    case courseHold
    /// Locked / informational chip.
    case locked
}

struct EmberModeChip: View {
    let text: String
    let selected: Bool
    var style: EmberChipStyle = .mode
    var locked: Bool = false
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(text)
                .font(.plusJakarta(size: 12, weight: selected ? .bold : .medium))
                .foregroundStyle(foreground)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(fill)
                .overlay(Capsule().strokeBorder(stroke, lineWidth: 1))
                .clipShape(Capsule())
                .shadow(
                    color: selected && !locked ? EmberSheetChrome.ember.opacity(0.40) : .clear,
                    radius: 8,
                    y: 2
                )
        }
        .buttonStyle(.plain)
        .disabled(locked)
    }

    private var foreground: Color {
        if locked { return EmberSheetChrome.textBody.opacity(0.7) }
        guard selected else { return EmberSheetChrome.textBody }
        return EmberSheetChrome.inkOnEmber
    }

    private var fill: Color {
        if locked { return EmberSheetChrome.chip.opacity(0.5) }
        guard selected else { return EmberSheetChrome.chip }
        switch style {
        case .tender, .courseNow: return EmberSheetChrome.tenderSelected
        case .courseHold: return EmberSheetChrome.amber
        case .channel: return EmberSheetChrome.wine
        case .mode, .locked: return EmberSheetChrome.ember
        }
    }

    private var stroke: Color {
        if locked { return EmberSheetChrome.hairline.opacity(0.5) }
        guard selected else { return EmberSheetChrome.hairline }
        return EmberSheetChrome.gold
    }
}

struct EmberChipRow: View {
    let options: [String]
    @Binding var selection: String
    var style: EmberChipStyle = .mode
    var locked: Set<String> = []

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 6) {
                ForEach(options, id: \.self) { option in
                    EmberModeChip(
                        text: option,
                        selected: selection == option,
                        style: style,
                        locked: locked.contains(option)
                    ) {
                        selection = option
                    }
                }
            }
        }
    }
}

struct EmberScanField: View {
    let placeholder: String
    @Binding var query: String
    var actionLabel: String = "Add"
    var disabled: Bool = false
    var action: () -> Void

    var body: some View {
        HStack(spacing: 8) {
            TextField(placeholder, text: $query)
                .font(.plusJakarta(size: 13))
                .foregroundStyle(EmberSheetChrome.textPrimary)
                .tint(EmberSheetChrome.ember)
            Button(action: action) {
                Text(actionLabel)
                    .font(.plusJakarta(size: 11, weight: .bold))
                    .foregroundStyle(EmberSheetChrome.inkOnEmber)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(disabled ? EmberSheetChrome.chip : EmberSheetChrome.ember)
                    .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
            }
            .buttonStyle(.plain)
            .disabled(disabled)
        }
        .padding(12)
        .background(EmberSheetChrome.chip)
        .overlay(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .strokeBorder(EmberSheetChrome.ember, lineWidth: 1.5)
        )
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        .shadow(color: EmberSheetChrome.ember.opacity(0.40), radius: 8, y: 0)
    }
}

struct EmberStepper: View {
    let quantity: Int
    let onDecrement: () -> Void
    let onIncrement: () -> Void

    var body: some View {
        HStack(spacing: 6) {
            stepButton("−", action: onDecrement)
            Text("\(quantity)")
                .font(.plusJakarta(size: 14, weight: .bold))
                .foregroundStyle(EmberSheetChrome.textPrimary)
                .frame(minWidth: 16)
            stepButton("+", action: onIncrement)
        }
    }

    private func stepButton(_ symbol: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(symbol)
                .font(.plusJakarta(size: 14, weight: .bold))
                .foregroundStyle(EmberSheetChrome.textPrimary)
                .frame(width: 28, height: 28)
                .background(EmberSheetChrome.chip)
                .overlay(
                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                        .strokeBorder(EmberSheetChrome.hairline, lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
        }
        .buttonStyle(.plain)
    }
}

struct EmberLinesCard<Content: View>: View {
    var title: String
    var trailing: String
    @ViewBuilder var content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                Text(title)
                    .font(.plusJakarta(size: 12, weight: .bold))
                    .foregroundStyle(EmberSheetChrome.gold)
                Spacer()
                Text(trailing)
                    .font(.plusJakarta(size: 11, weight: .medium))
                    .foregroundStyle(EmberSheetChrome.textBody)
            }
            .padding(.bottom, 4)
            content()
        }
        .padding(14)
        .background(EmberSheetChrome.chip)
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .strokeBorder(EmberSheetChrome.ember.opacity(0.30), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .shadow(color: EmberSheetChrome.gold.opacity(0.15), radius: 16, y: 8)
    }
}

struct EmberEmptyCard: View {
    let title: String
    let bodyText: String

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
                .font(.plusJakarta(size: 13, weight: .bold))
                .foregroundStyle(EmberSheetChrome.textPrimary)
            Text(bodyText)
                .font(.plusJakarta(size: 12))
                .foregroundStyle(EmberSheetChrome.textBody)
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(EmberSheetChrome.chip)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

struct EmberPickerRow: View {
    let label: String
    let value: String
    var action: (() -> Void)? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            EmberFieldLabel(text: label)
            Button {
                action?()
            } label: {
                HStack(spacing: 8) {
                    Text(value)
                        .font(.plusJakarta(size: 14, weight: .medium))
                        .foregroundStyle(EmberSheetChrome.textPrimary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    Text("›")
                        .font(.plusJakarta(size: 16, weight: .bold))
                        .foregroundStyle(EmberSheetChrome.textBody)
                }
                .padding(12)
                .background(EmberSheetChrome.chip)
                .overlay(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .strokeBorder(EmberSheetChrome.hairline, lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            }
            .buttonStyle(.plain)
            .disabled(action == nil)
        }
    }
}

struct EmberMoreDetails<Content: View>: View {
    let hint: String
    @Binding var open: Bool
    @ViewBuilder var content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Button {
                open.toggle()
            } label: {
                HStack(spacing: 8) {
                    Text("More details")
                        .font(.plusJakarta(size: 13, weight: .bold))
                        .foregroundStyle(EmberSheetChrome.gold)
                    Spacer(minLength: 0)
                    Text(hint)
                        .font(.plusJakarta(size: 11))
                        .foregroundStyle(EmberSheetChrome.textBody)
                        .lineLimit(1)
                    Text(open ? "⌃" : "⌄")
                        .font(.plusJakarta(size: 14, weight: .bold))
                        .foregroundStyle(EmberSheetChrome.gold)
                }
                .padding(12)
                .overlay(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .strokeBorder(EmberSheetChrome.ember.opacity(0.40), lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            }
            .buttonStyle(.plain)
            if open {
                content()
            }
        }
    }
}

struct EmberWhyBox: View {
    let text: String

    var body: some View {
        HStack(alignment: .top, spacing: 8) {
            Text("Why")
                .font(.plusJakarta(size: 11, weight: .bold))
                .foregroundStyle(EmberSheetChrome.gold)
            Text(text)
                .font(.plusJakarta(size: 11))
                .foregroundStyle(EmberSheetChrome.whyBody)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(10)
        .background(EmberSheetChrome.ember.opacity(0.12))
        .overlay(
            RoundedRectangle(cornerRadius: 10, style: .continuous)
                .strokeBorder(EmberSheetChrome.ember.opacity(0.40), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
    }
}

struct EmberStickyFooter: View {
    let title: String
    var caption: String = "Draft preserved if you close or open a picker"
    var disabled: Bool = false
    var loading: Bool = false
    let action: () -> Void

    var body: some View {
        VStack(spacing: 6) {
            Button(action: action) {
                Text(title)
                    .font(.plusJakarta(size: 15, weight: .bold))
                    .foregroundStyle(Color.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 16)
                    .background(
                        LinearGradient(
                            colors: disabled || loading
                                ? [EmberSheetChrome.wine.opacity(0.4), EmberSheetChrome.ember.opacity(0.4)]
                                : [EmberSheetChrome.wine, EmberSheetChrome.ember],
                            startPoint: .leading,
                            endPoint: .trailing
                        )
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                    .shadow(
                        color: EmberSheetChrome.ember.opacity(disabled ? 0 : 0.60),
                        radius: 12,
                        y: 4
                    )
            }
            .buttonStyle(.plain)
            .disabled(disabled || loading)
            Text(caption)
                .font(.plusJakarta(size: 10))
                .foregroundStyle(EmberSheetChrome.textBody)
                .multilineTextAlignment(.center)
        }
        .padding(.top, 8)
    }
}

/// Ticket hero — gold KOT id + 28pt headline (Figma KOT Send / Fire / Reprint).
struct EmberTicketHero: View {
    let eyebrow: String
    let ticketId: String
    let headline: String
    let detail: String

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 8) {
                Text(eyebrow)
                    .font(.plusJakarta(size: 11, weight: .medium))
                    .foregroundStyle(EmberSheetChrome.textBody)
                Spacer(minLength: 0)
                Text(ticketId)
                    .font(.plusJakarta(size: 12, weight: .bold))
                    .foregroundStyle(EmberSheetChrome.gold)
            }
            Text(headline)
                .font(.plusJakarta(size: 28, weight: .bold))
                .foregroundStyle(EmberSheetChrome.textPrimary)
                .lineLimit(1)
                .minimumScaleFactor(0.7)
            Text(detail)
                .font(.plusJakarta(size: 12))
                .foregroundStyle(EmberSheetChrome.textBody)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(EmberSheetChrome.chip)
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .strokeBorder(EmberSheetChrome.ember, lineWidth: 1.5)
        )
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .shadow(color: EmberSheetChrome.ember.opacity(0.40), radius: 10, y: 0)
    }
}

struct EmberMetaRows: View {
    let rows: [(label: String, value: String)]

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            ForEach(Array(rows.enumerated()), id: \.offset) { _, row in
                HStack(spacing: 8) {
                    Text(row.label)
                        .font(.plusJakarta(size: 12, weight: .medium))
                        .foregroundStyle(EmberSheetChrome.textBody)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    Text(row.value)
                        .font(.plusJakarta(size: 13, weight: .bold))
                        .foregroundStyle(EmberSheetChrome.textPrimary)
                        .multilineTextAlignment(.trailing)
                        .lineLimit(2)
                }
                .padding(.vertical, 4)
            }
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(EmberSheetChrome.chip)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

struct EmberTotalsCard: View {
    let rows: [(label: String, value: String, warning: Bool)]
    let payableLabel: String
    let payable: String
    let sideLabel: String
    let sideValue: String
    var sideWarning: Bool = false

    var body: some View {
        VStack(spacing: 6) {
            ForEach(Array(rows.enumerated()), id: \.offset) { _, row in
                HStack {
                    Text(row.label)
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(EmberSheetChrome.textBody)
                    Spacer()
                    Text(row.value)
                        .font(.plusJakarta(size: 12, weight: .medium))
                        .foregroundStyle(row.warning ? EmberSheetChrome.amber : EmberSheetChrome.textBody)
                }
            }
            Rectangle()
                .fill(EmberSheetChrome.hairline)
                .frame(height: 1)
            HStack(alignment: .bottom) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(payableLabel)
                        .font(.plusJakarta(size: 11, weight: .medium))
                        .foregroundStyle(EmberSheetChrome.textBody)
                    Text(payable)
                        .font(.plusJakarta(size: 28, weight: .bold))
                        .foregroundStyle(EmberSheetChrome.textPrimary)
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 2) {
                    Text(sideLabel)
                        .font(.plusJakarta(size: 10, weight: .bold))
                        .foregroundStyle(EmberSheetChrome.gold)
                    Text(sideValue)
                        .font(.plusJakarta(size: 13, weight: .bold))
                        .foregroundStyle(sideWarning ? EmberSheetChrome.amber : EmberSheetChrome.tenderSelected)
                }
            }
        }
        .padding(16)
        .background(EmberSheetChrome.chip)
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .strokeBorder(EmberSheetChrome.ember, lineWidth: 1.5)
        )
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .shadow(color: EmberSheetChrome.ember.opacity(0.40), radius: 10, y: 0)
    }
}

/// Money / qty hero — Purchase total · Collect now · Pay now · Expense · Qty delta (RST-QA-02…06).
struct EmberAmountHero: View {
    let label: String
    let amount: String
    var currencyHint: String? = nil
    var caption: String = ""
    var amountSize: CGFloat = 32

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 8) {
                Text(label)
                    .font(.plusJakarta(size: 12, weight: .medium))
                    .foregroundStyle(EmberSheetChrome.textBody)
                Spacer(minLength: 0)
                if let currencyHint, !currencyHint.isEmpty {
                    Text(currencyHint)
                        .font(.plusJakarta(size: 11, weight: .bold))
                        .foregroundStyle(EmberSheetChrome.gold)
                }
            }
            Text(amount)
                .font(.plusJakarta(size: amountSize, weight: .bold))
                .foregroundStyle(EmberSheetChrome.textPrimary)
                .lineLimit(1)
                .minimumScaleFactor(0.7)
            if !caption.isEmpty {
                Text(caption)
                    .font(.plusJakarta(size: 12, weight: .medium))
                    .foregroundStyle(EmberSheetChrome.gold)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(EmberSheetChrome.sheet)
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .strokeBorder(EmberSheetChrome.ember, lineWidth: 1.5)
        )
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .shadow(color: EmberSheetChrome.ember.opacity(0.33), radius: 12, y: 4)
    }
}

struct EmberMoneyRow: View {
    let label: String
    let value: String
    var emphasize: Bool = false
    var danger: Bool = false
    var gold: Bool = false

    var body: some View {
        HStack(spacing: 8) {
            Text(label)
                .font(.plusJakarta(size: emphasize ? 13 : 12, weight: emphasize ? .bold : .regular))
                .foregroundStyle(emphasize ? EmberSheetChrome.textPrimary : EmberSheetChrome.textBody)
                .frame(maxWidth: .infinity, alignment: .leading)
            Text(value)
                .font(.plusJakarta(size: emphasize || gold ? 13 : 13, weight: .bold))
                .foregroundStyle(
                    danger
                        ? Color(hex: "#EF4444")
                        : (gold || emphasize ? EmberSheetChrome.gold : EmberSheetChrome.textPrimary)
                )
        }
    }
}

struct EmberBreakdownCard<Content: View>: View {
    @ViewBuilder var content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            content()
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(EmberSheetChrome.sheet)
        .overlay(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .strokeBorder(EmberSheetChrome.ember.opacity(0.27), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
    }
}

struct EmberAllocateRow: View {
    let title: String
    let subtitle: String
    let amount: String
    let trailing: String
    var selected: Bool = false
    var action: (() -> Void)? = nil

    var body: some View {
        Button {
            action?()
        } label: {
            HStack(spacing: 8) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.plusJakarta(size: 14, weight: .bold))
                        .foregroundStyle(EmberSheetChrome.textPrimary)
                    Text(subtitle)
                        .font(.plusJakarta(size: 11))
                        .foregroundStyle(EmberSheetChrome.textBody)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                VStack(alignment: .trailing, spacing: 4) {
                    Text(amount)
                        .font(.plusJakarta(size: 14, weight: .bold))
                        .foregroundStyle(EmberSheetChrome.textPrimary)
                    Text(trailing)
                        .font(.plusJakarta(size: 11, weight: .medium))
                        .foregroundStyle(EmberSheetChrome.gold)
                }
            }
            .padding(12)
            .background(EmberSheetChrome.sheet)
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .strokeBorder(
                        selected ? EmberSheetChrome.ember.opacity(0.55) : EmberSheetChrome.chip,
                        lineWidth: 1
                    )
            )
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        }
        .buttonStyle(.plain)
        .disabled(action == nil)
    }
}

struct EmberTextField: View {
    let placeholder: String
    @Binding var text: String
    var keyboard: UIKeyboardType = .default

    var body: some View {
        TextField(placeholder, text: $text)
            .keyboardType(keyboard)
            .font(.plusJakarta(size: 14, weight: .medium))
            .foregroundStyle(EmberSheetChrome.textPrimary)
            .tint(EmberSheetChrome.ember)
            .padding(.horizontal, 12)
            .padding(.vertical, 12)
            .background(EmberSheetChrome.chip)
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .strokeBorder(EmberSheetChrome.hairline, lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

struct EmberSavedToast: View {
    var body: some View {
        HStack(spacing: 6) {
            Text("✓")
                .font(.plusJakarta(size: 12, weight: .semibold))
                .foregroundStyle(EmberSheetChrome.gold)
            Text("Saved · sheet closes and returns back")
                .font(.plusJakarta(size: 12, weight: .semibold))
                .foregroundStyle(EmberSheetChrome.textPrimary)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(EmberSheetChrome.sheet)
        .overlay(Capsule().strokeBorder(EmberSheetChrome.gold, lineWidth: 1))
        .clipShape(Capsule())
        .shadow(color: .black.opacity(0.25), radius: 10, y: 4)
    }
}
