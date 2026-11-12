import SwiftUI

// MARK: - Retail capture chrome (Figma RTL-QA rose / amber · Harbor Mart)

enum RoseSheetChrome {
    /// Amber labels / Why / Add line (`#FBBF24`).
    static let label = Color(hex: "#FBBF24")
    /// Legacy cyan context — prefer `whyBody` on POS Why notes.
    static let context = Color(hex: "#CFEFF6")
    /// Why body cream (`#FEF3C7`) — Figma RTL HTR / POS Why notes.
    static let whyBody = Color(hex: "#FEF3C7")
    /// Soft rose wash (legacy soft token).
    static let whyBodySoft = BusinessTokens.Colors.retailWhyBody
    /// Sheet / tile / chip slate (`#1E293B`).
    static let sheet = BusinessTokens.Colors.retailSheet
    /// Nested field / line row fill (`#0F172A`).
    static let field = Color(hex: "#0F172A")
    /// Inactive chip fill (`#334155`) — Figma HTR pills.
    static let chip = Color(hex: "#334155")
    /// Inactive chip stroke (`#475569`).
    static let chipBorder = Color(hex: "#475569")
    /// Deep rose context fill (`#881337`).
    static let deepRose = Color(hex: "#881337")
    /// Dark ink on rose mode chips (`#0F172A`).
    static let inkOnChip = Color(hex: "#0F172A")
    /// Inactive chip / field stroke (`#E11D48`).
    static let roseBorder = BusinessTokens.Colors.retailAccentDeep
    /// Grab + ALT soft rose (`#FB7185`).
    static let grab = BusinessTokens.Colors.retailAccentSoft
    /// Tender selected fill (`#22C55E`).
    static let tenderSelected = Color(hex: "#22C55E")
    /// Body / muted slate text.
    static let textBody = Color(hex: "#CBD5E1")
    static let textMuted = Color(hex: "#94A3B8")
}

struct RoseGrabber: View {
    var body: some View {
        Capsule()
            .fill(RoseSheetChrome.grab)
            .frame(width: 40, height: 4)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 4)
    }
}

struct RoseSheetHeader: View {
    let theme: OperatingTheme
    let title: String
    var subtitle: String? = nil
    var qaCode: String? = nil
    /// Figma ALT badge on Quick amount / Hold recall.
    var showAltBadge: Bool = false
    var onClose: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(alignment: .center, spacing: 8) {
                Text(title)
                    .font(.plusJakarta(size: 22, weight: .bold))
                    .foregroundStyle(OperatingV12.title)
                    .frame(maxWidth: .infinity, alignment: .leading)
                if showAltBadge {
                    Text("ALT")
                        .font(.plusJakarta(size: 12, weight: .bold))
                        .foregroundStyle(Color.white)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(RoseSheetChrome.grab)
                        .overlay(
                            Capsule().strokeBorder(theme.accent, lineWidth: 1)
                        )
                        .clipShape(Capsule())
                } else if let qaCode {
                    Text(qaCode)
                        .font(.plusJakarta(size: 10, weight: .bold))
                        .tracking(0.6)
                        .foregroundStyle(theme.accent)
                }
                Button(action: onClose) {
                    Text("✕")
                        .font(.plusJakarta(size: 13, weight: .bold))
                        .foregroundStyle(RoseSheetChrome.textBody)
                        .frame(width: 32, height: 32)
                        .background(RoseSheetChrome.sheet)
                        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Close")
            }
            // Optional legacy subtitle kept for API compatibility; POS uses RoseRegisterContext.
            if let subtitle {
                Text(subtitle)
                    .font(.plusJakarta(size: 12, weight: .medium))
                    .foregroundStyle(RoseSheetChrome.textBody)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
    }
}

struct RoseFieldLabel: View {
    let text: String
    var body: some View {
        // Figma RTL labels are title case (Scan or search SKU), not all-caps.
        Text(text)
            .font(.plusJakarta(size: 11, weight: .medium))
            .foregroundStyle(RoseSheetChrome.textBody)
    }
}

struct RoseModeChip: View {
    let theme: OperatingTheme
    let text: String
    let selected: Bool
    var style: RoseChipStyle = .accent
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(text)
                .font(.plusJakarta(size: 12, weight: selected ? .bold : .medium))
                .foregroundStyle(selectedForeground)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(selectedFill)
                .overlay(
                    Capsule()
                        .strokeBorder(selectedStroke, lineWidth: 1)
                )
                .clipShape(Capsule())
                .shadow(
                    color: selected && (style == .ticket || style == .deep)
                        ? Color(hex: "#EA580C").opacity(0.35)
                        : .clear,
                    radius: 8,
                    y: 2
                )
        }
        .buttonStyle(.plain)
    }

    private var selectedForeground: Color {
        guard selected else {
            return (style == .ticket || style == .deep) ? RoseSheetChrome.textMuted : RoseSheetChrome.textBody
        }
        switch style {
        case .ticket, .deep, .tender: return RoseSheetChrome.inkOnChip
        case .accent: return Color.white
        }
    }

    private var selectedFill: Color {
        guard selected else {
            return (style == .ticket || style == .deep) ? RoseSheetChrome.chip : RoseSheetChrome.sheet
        }
        switch style {
        case .tender: return RoseSheetChrome.tenderSelected
        case .deep: return RoseSheetChrome.deepRose
        case .ticket, .accent: return theme.accent
        }
    }

    private var selectedStroke: Color {
        guard selected else {
            return (style == .ticket || style == .deep) ? RoseSheetChrome.chipBorder : RoseSheetChrome.roseBorder
        }
        switch style {
        case .ticket, .deep, .tender: return RoseSheetChrome.label
        case .accent: return theme.accent
        }
    }
}

enum RoseChipStyle {
    /// POS default — white ink on rose fill.
    case accent
    /// Tender selected — green fill.
    case tender
    /// HTR mode chips — dark ink on rose + amber border (Figma Hold|Transfer|Reprint).
    case ticket
    /// HTR context chips — deep rose fill + amber border.
    case deep
}

/// Ticket hero for Hold / Transfer / Reprint (Figma slip card · rose chrome).
struct RoseTicketHero: View {
    let theme: OperatingTheme
    let eyebrow: String
    let code: String
    let title: String
    let subtitle: String

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(eyebrow)
                    .font(.plusJakarta(size: 11, weight: .medium))
                    .foregroundStyle(RoseSheetChrome.textMuted)
                Spacer(minLength: 8)
                Text(code)
                    .font(.plusJakarta(size: 12, weight: .bold))
                    .foregroundStyle(RoseSheetChrome.label)
            }
            Text(title)
                .font(.plusJakarta(size: 28, weight: .bold))
                .foregroundStyle(OperatingV12.title)
                .lineLimit(2)
            Text(subtitle)
                .font(.plusJakarta(size: 12))
                .foregroundStyle(RoseSheetChrome.textMuted)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoseSheetChrome.chip)
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .strokeBorder(theme.accent, lineWidth: 1.5)
        )
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .shadow(color: Color(hex: "#EA580C").opacity(0.35), radius: 16, y: 0)
    }
}

/// Line card for utility tickets — HSN · ₹ · % meta + category + status pills.
struct RoseTicketLineCard: View {
    let theme: OperatingTheme
    let title: String
    let qtyLabel: String
    let meta: String
    let category: String
    let status: String
    var statusStyle: RoseTicketStatusStyle = .parked
    var onDecrement: (() -> Void)? = nil
    var onIncrement: (() -> Void)? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(alignment: .center, spacing: 8) {
                Text(title)
                    .font(.plusJakarta(size: 14, weight: .bold))
                    .foregroundStyle(OperatingV12.title)
                    .lineLimit(1)
                    .frame(maxWidth: .infinity, alignment: .leading)
                if let onDecrement, let onIncrement {
                    RoseStepper(theme: theme, quantity: qtyFromLabel, onDecrement: onDecrement, onIncrement: onIncrement)
                } else {
                    Text(qtyLabel)
                        .font(.plusJakarta(size: 14, weight: .bold))
                        .foregroundStyle(RoseSheetChrome.label)
                }
            }
            HStack(alignment: .center, spacing: 6) {
                Text(meta)
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(RoseSheetChrome.textMuted)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .lineLimit(2)
                Text(category)
                    .font(.plusJakarta(size: 12, weight: .medium))
                    .foregroundStyle(RoseSheetChrome.textMuted)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(RoseSheetChrome.chip)
                    .overlay(Capsule().strokeBorder(RoseSheetChrome.chipBorder, lineWidth: 1))
                    .clipShape(Capsule())
                Text(status)
                    .font(.plusJakarta(size: 12, weight: .bold))
                    .foregroundStyle(RoseSheetChrome.inkOnChip)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(statusFill)
                    .overlay(Capsule().strokeBorder(RoseSheetChrome.label, lineWidth: 1))
                    .clipShape(Capsule())
                    .shadow(color: Color(hex: "#EA580C").opacity(0.25), radius: 8, y: 2)
            }
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoseSheetChrome.chip)
        .overlay(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .strokeBorder(statusBorder, lineWidth: statusStyle == .move || statusStyle == .amberParked ? 1 : 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .shadow(
            color: (statusStyle == .move || statusStyle == .amberParked)
                ? RoseSheetChrome.label.opacity(0.2)
                : .clear,
            radius: 8,
            y: 2
        )
    }

    private var qtyFromLabel: Int {
        Int(qtyLabel.replacingOccurrences(of: "×", with: "")) ?? 1
    }

    private var statusFill: Color {
        switch statusStyle {
        case .parked, .posted: return RoseSheetChrome.tenderSelected
        case .move, .amberParked: return RoseSheetChrome.label
        }
    }

    private var statusBorder: Color {
        switch statusStyle {
        case .parked, .posted: return theme.accent.opacity(0.3)
        case .move, .amberParked: return RoseSheetChrome.label.opacity(0.4)
        }
    }
}

enum RoseTicketStatusStyle {
    case parked
    case amberParked
    case move
    case posted
}

struct RoseChipRow: View {
    let theme: OperatingTheme
    let options: [String]
    @Binding var selection: String
    var style: RoseChipStyle = .accent

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 6) {
                ForEach(options, id: \.self) { option in
                    RoseModeChip(
                        theme: theme,
                        text: option,
                        selected: selection == option,
                        style: style
                    ) {
                        selection = option
                    }
                }
            }
        }
    }
}

struct RoseTextField: View {
    let theme: OperatingTheme
    let placeholder: String
    @Binding var text: String
    var keyboard: UIKeyboardType = .default
    var trailing: String? = nil

    var body: some View {
        HStack(spacing: 8) {
            TextField(placeholder, text: $text)
                .keyboardType(keyboard)
                .font(.plusJakarta(size: 14, weight: .medium))
                .foregroundStyle(OperatingV12.title)
                .tint(theme.accent)
            if let trailing {
                Text(trailing)
                    .font(.plusJakarta(size: 14, weight: .bold))
                    .foregroundStyle(RoseSheetChrome.textBody)
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 12)
        .background(RoseSheetChrome.sheet)
        .overlay(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .strokeBorder(RoseSheetChrome.roseBorder, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

struct RoseScanField: View {
    let theme: OperatingTheme
    let placeholder: String
    @Binding var query: String
    var actionLabel: String = "Scan"
    var action: () -> Void
    var disabled: Bool = false

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: "barcode.viewfinder")
                .font(.system(size: 16, weight: .semibold))
                .foregroundStyle(theme.accent)
            TextField(placeholder, text: $query)
                .font(.plusJakarta(size: 13))
                .foregroundStyle(OperatingV12.title)
                .tint(theme.accent)
            Button(action: action) {
                Text(actionLabel)
                    .font(.plusJakarta(size: 11, weight: .bold))
                    .foregroundStyle(Color.white)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(disabled ? theme.inset : theme.accent)
                    .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
            }
            .buttonStyle(.plain)
            .disabled(disabled)
        }
        .padding(12)
        .background(RoseSheetChrome.sheet)
        .overlay(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .strokeBorder(theme.accent, lineWidth: 1.5)
        )
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

struct RoseStepper: View {
    let theme: OperatingTheme
    let quantity: Int
    let onDecrement: () -> Void
    let onIncrement: () -> Void

    var body: some View {
        HStack(spacing: 6) {
            stepButton("−", action: onDecrement)
            Text("\(quantity)")
                .font(.plusJakarta(size: 14, weight: .bold))
                .foregroundStyle(OperatingV12.title)
                .frame(minWidth: 16)
            stepButton("+", action: onIncrement)
        }
    }

    private func stepButton(_ symbol: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(symbol)
                .font(.plusJakarta(size: 14, weight: .bold))
                .foregroundStyle(Color.white)
                .frame(width: 28, height: 28)
                .background(RoseSheetChrome.sheet)
                .overlay(
                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                        .strokeBorder(RoseSheetChrome.roseBorder, lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
        }
        .buttonStyle(.plain)
    }
}

struct RoseLinesCard<Content: View>: View {
    let theme: OperatingTheme
    var title: String
    var trailing: String
    @ViewBuilder var content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(title)
                    .font(.plusJakarta(size: 12, weight: .bold))
                    .foregroundStyle(RoseSheetChrome.label)
                Spacer()
                Text(trailing)
                    .font(.plusJakarta(size: 11, weight: .medium))
                    .foregroundStyle(RoseSheetChrome.textBody)
            }
            content()
        }
        .padding(14)
        .background(RoseSheetChrome.sheet)
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .strokeBorder(theme.accent.opacity(0.30), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}

struct RoseEmptyCard: View {
    let theme: OperatingTheme
    let title: String
    let bodyText: String

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
                .font(.plusJakarta(size: 13, weight: .bold))
                .foregroundStyle(OperatingV12.title)
            Text(bodyText)
                .font(.plusJakarta(size: 12))
                .foregroundStyle(OperatingV12.subtitle)
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(theme.inset)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

struct RoseTotalsCard: View {
    let theme: OperatingTheme
    let currency: String
    let subtotal: String
    let payable: String
    var tender: String? = nil
    var footnote: String? = nil

    var body: some View {
        VStack(spacing: 8) {
            roseTotalRow("Subtotal", subtotal)
            roseTotalRow("GST", "Unavailable", warning: true, caption: "Stamped server-side — no client tax engine")
            if let footnote {
                Text(footnote)
                    .font(.plusJakarta(size: 10))
                    .foregroundStyle(OperatingV12.subtitle)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            Divider().overlay(theme.border.opacity(0.4))
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Payable now")
                        .font(.plusJakarta(size: 10))
                        .foregroundStyle(OperatingV12.subtitle)
                    Text(payable)
                        .font(.plusJakarta(size: 24, weight: .bold))
                        .foregroundStyle(OperatingV12.title)
                }
                Spacer()
                if let tender {
                    VStack(alignment: .trailing, spacing: 2) {
                        Text("Tender")
                            .font(.plusJakarta(size: 10))
                            .foregroundStyle(OperatingV12.subtitle)
                        Text(tender)
                            .font(.plusJakarta(size: 14, weight: .bold))
                            .foregroundStyle(theme.accent)
                    }
                }
            }
        }
        .padding(12)
        .background(Color.black.opacity(0.24))
        .overlay(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .strokeBorder(Color.white.opacity(0.06), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private func roseTotalRow(_ label: String, _ value: String, warning: Bool = false, caption: String? = nil) -> some View {
        HStack {
            VStack(alignment: .leading, spacing: 1) {
                Text(label)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(OperatingV12.subtitle)
                if let caption {
                    Text(caption)
                        .font(.plusJakarta(size: 10))
                        .foregroundStyle(OperatingV12.subtitle.opacity(0.8))
                }
            }
            Spacer()
            Text(value)
                .font(.plusJakarta(size: 14, weight: .semibold))
                .foregroundStyle(warning ? theme.warning : OperatingV12.title)
        }
    }
}

/// Figma RTL-QA-02…04 money totals — rose 1.5pt border + hero amount + method/AP status.
struct RoseMoneyTotalsCard<Rows: View>: View {
    let theme: OperatingTheme
    let heroLabel: String
    let heroAmount: String
    var statusEyebrow: String = "Method"
    var statusValue: String
    var statusTone: RoseMoneyStatusTone = .tender
    @ViewBuilder var rows: () -> Rows

    var body: some View {
        VStack(spacing: 6) {
            rows()
            Rectangle()
                .fill(RoseSheetChrome.roseBorder)
                .frame(height: 1)
            HStack(alignment: .bottom) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(heroLabel)
                        .font(.plusJakarta(size: 11, weight: .medium))
                        .foregroundStyle(RoseSheetChrome.textBody)
                    Text(heroAmount.isEmpty ? "—" : heroAmount)
                        .font(.plusJakarta(size: 28, weight: .bold))
                        .foregroundStyle(OperatingV12.title)
                }
                Spacer(minLength: 8)
                VStack(alignment: .trailing, spacing: 2) {
                    Text(statusEyebrow)
                        .font(.plusJakarta(size: 10, weight: .bold))
                        .foregroundStyle(RoseSheetChrome.label)
                    Text(statusValue)
                        .font(.plusJakarta(size: 13, weight: .bold))
                        .foregroundStyle(statusColor)
                }
            }
        }
        .padding(16)
        .background(RoseSheetChrome.sheet)
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .strokeBorder(theme.accent, lineWidth: 1.5)
        )
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .shadow(color: theme.accent.opacity(0.18), radius: 16, y: 0)
    }

    private var statusColor: Color {
        switch statusTone {
        case .tender: return RoseSheetChrome.tenderSelected
        case .amber: return RoseSheetChrome.label
        case .danger: return theme.negative
        case .body: return RoseSheetChrome.textBody
        }
    }
}

enum RoseMoneyStatusTone {
    case tender
    case amber
    case danger
    case body
}

struct RoseMoneyRow: View {
    let label: String
    let value: String
    var valueTone: RoseMoneyStatusTone = .body

    var body: some View {
        HStack {
            Text(label)
                .font(.plusJakarta(size: 12))
                .foregroundStyle(RoseSheetChrome.textBody)
            Spacer()
            Text(value)
                .font(.plusJakarta(size: 12, weight: .medium))
                .foregroundStyle(toneColor)
        }
    }

    private var toneColor: Color {
        switch valueTone {
        case .tender: return RoseSheetChrome.tenderSelected
        case .amber: return RoseSheetChrome.label
        case .danger: return Color(hex: "#EF4444")
        case .body: return RoseSheetChrome.textBody
        }
    }
}


struct RoseAmountHero: View {
    let theme: OperatingTheme
    var label: String = "Quick amount"
    var amountText: String
    var caption: String? = "Posts revenue once · never moves SKU stock without an itemized line."
    /// When set, shows an editable field instead of the display-only amount (legacy callers).
    var amountBinding: Binding<String>? = nil
    var currency: String = ""

    init(
        theme: OperatingTheme,
        currency: String,
        amount: Binding<String>,
        label: String = "Quick amount",
        caption: String? = "Posts revenue once · never moves SKU stock without an itemized line."
    ) {
        self.theme = theme
        self.currency = currency
        self.amountBinding = amount
        self.amountText = amount.wrappedValue
        self.label = label
        self.caption = caption
    }

    init(
        theme: OperatingTheme,
        label: String,
        amountText: String,
        caption: String? = nil
    ) {
        self.theme = theme
        self.label = label
        self.amountText = amountText
        self.caption = caption
        self.amountBinding = nil
        self.currency = ""
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(label)
                    .font(.plusJakarta(size: 12, weight: .medium))
                    .foregroundStyle(RoseSheetChrome.textBody)
                Spacer()
                Text(currency.isEmpty ? "INR" : currency)
                    .font(.plusJakarta(size: 11, weight: .bold))
                    .foregroundStyle(RoseSheetChrome.label)
            }
            if let amountBinding {
                RoseTextField(theme: theme, placeholder: "0.00", text: amountBinding, keyboard: .decimalPad)
            } else {
                Text(amountText.isEmpty ? "—" : amountText)
                    .font(.plusJakarta(size: 40, weight: .bold))
                    .foregroundStyle(OperatingV12.title)
            }
            if let caption {
                Text(caption)
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(RoseSheetChrome.textBody)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoseSheetChrome.sheet)
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .strokeBorder(theme.accent, lineWidth: 1.5)
        )
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}

struct RosePickerRow: View {
    let theme: OperatingTheme
    let label: String
    let value: String
    var action: (() -> Void)? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            RoseFieldLabel(text: label)
            Button {
                action?()
            } label: {
                HStack {
                    Text(value.isEmpty ? "Select…" : value)
                        .font(.plusJakarta(size: 14, weight: .medium))
                        .foregroundStyle(value.isEmpty ? RoseSheetChrome.textMuted : OperatingV12.title)
                    Spacer()
                    Text("›")
                        .font(.plusJakarta(size: 16, weight: .bold))
                        .foregroundStyle(RoseSheetChrome.textBody)
                }
                .padding(12)
                .background(RoseSheetChrome.sheet)
                .overlay(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .strokeBorder(RoseSheetChrome.roseBorder, lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            }
            .buttonStyle(.plain)
            .disabled(action == nil)
        }
    }
}

struct RoseMoreDetails<Content: View>: View {
    @State private var open = false
    let title: String
    var hint: String = "Notes · Attachment · Tags"
    @ViewBuilder var content: () -> Content

    init(title: String, hint: String = "Notes · Attachment · Tags", @ViewBuilder content: @escaping () -> Content) {
        self.title = title
        self.hint = hint
        self.content = content
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Button {
                withAnimation(.easeInOut(duration: 0.2)) { open.toggle() }
            } label: {
                HStack(spacing: 8) {
                    Text(title)
                        .font(.plusJakarta(size: 13, weight: .bold))
                        .foregroundStyle(RoseSheetChrome.label)
                    Spacer()
                    Text(hint)
                        .font(.plusJakarta(size: 11))
                        .foregroundStyle(RoseSheetChrome.textBody)
                        .lineLimit(1)
                    Text(open ? "⌃" : "⌄")
                        .font(.plusJakarta(size: 14, weight: .bold))
                        .foregroundStyle(RoseSheetChrome.label)
                }
                .padding(12)
                .overlay(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .strokeBorder(themeAccentBorder, lineWidth: 1)
                )
            }
            .buttonStyle(.plain)
            if open {
                content()
            }
        }
    }

    private var themeAccentBorder: Color {
        Color(hex: "#F43F5E").opacity(0.40)
    }
}

struct RoseWhyBox: View {
    let theme: OperatingTheme
    let text: String

    var body: some View {
        HStack(alignment: .top, spacing: 8) {
            Text("Why")
                .font(.plusJakarta(size: 11, weight: .bold))
                .foregroundStyle(RoseSheetChrome.label)
            Text(text)
                .font(.plusJakarta(size: 11))
                .foregroundStyle(RoseSheetChrome.whyBody)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(10)
        .background(theme.accent.opacity(0.12))
        .overlay(
            RoundedRectangle(cornerRadius: 10, style: .continuous)
                .strokeBorder(theme.accent.opacity(0.40), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
    }
}

struct RoseStickyFooter: View {
    let theme: OperatingTheme
    let title: String
    var caption: String = "Draft preserved if you close or open a picker"
    var disabled: Bool = false
    var loading: Bool = false
    /// Master supplier form (`2506:29612`) — dark CTA ink + amber caveat. QA sheets stay white/slate.
    var formStyle: Bool = false
    let action: () -> Void

    var body: some View {
        VStack(spacing: 6) {
            Button(action: action) {
                Text(loading ? "Posting…" : title)
                    .font(.plusJakarta(size: 15, weight: .bold))
                    .foregroundStyle(formStyle ? Color(hex: "#0F172A") : Color.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 16)
                    .background(disabled || loading ? theme.accent.opacity(0.4) : theme.accent)
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                    .shadow(color: theme.accent.opacity(disabled ? 0 : 0.45), radius: 12, y: 4)
            }
            .buttonStyle(.plain)
            .disabled(disabled || loading)
            Text(caption)
                .font(.plusJakarta(size: 10))
                .foregroundStyle(formStyle ? RoseSheetChrome.label : RoseSheetChrome.textBody)
                .multilineTextAlignment(.center)
        }
        .padding(.top, 8)
    }
}

/// Figma toast: `✓ Saved · sheet closes and returns back`
struct RoseSavedToast: View {
    var body: some View {
        HStack(spacing: 6) {
            Text("✓")
                .font(.plusJakarta(size: 12, weight: .semibold))
                .foregroundStyle(RoseSheetChrome.label)
            Text("Saved · sheet closes and returns back")
                .font(.plusJakarta(size: 12, weight: .semibold))
                .foregroundStyle(OperatingV12.title)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(RoseSheetChrome.sheet)
        .overlay(
            Capsule().strokeBorder(RoseSheetChrome.label, lineWidth: 1)
        )
        .clipShape(Capsule())
        .shadow(color: .black.opacity(0.25), radius: 10, y: 4)
    }
}

struct RoseTenderChips: View {
    let theme: OperatingTheme
    @Binding var tender: String
    @Binding var splitTender: Bool
    @Binding var splitUpi: String
    @Binding var splitCash: String
    @Binding var splitCard: String
    var splitOk: Bool = true
    var options: [String] = ["UPI", "Cash", "Card"]
    var allowSplit: Bool = true

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            RoseFieldLabel(text: "Tender")
            HStack(spacing: 6) {
                ForEach(options, id: \.self) { option in
                    RoseModeChip(
                        theme: theme,
                        text: option,
                        selected: !splitTender && tender == option,
                        style: .tender
                    ) {
                        splitTender = false
                        tender = option
                    }
                }
                if allowSplit {
                    RoseModeChip(
                        theme: theme,
                        text: "Split",
                        selected: splitTender,
                        style: .tender
                    ) {
                        splitTender = true
                    }
                }
            }
            if splitTender {
                RoseFieldLabel(text: "UPI")
                RoseTextField(theme: theme, placeholder: "0", text: $splitUpi)
                RoseFieldLabel(text: "Cash")
                RoseTextField(theme: theme, placeholder: "0", text: $splitCash)
                RoseFieldLabel(text: "Card")
                RoseTextField(theme: theme, placeholder: "0", text: $splitCard)
                Text(splitOk
                     ? "Split instruments post on the contract and must add up to payable."
                     : "Split amounts must add up to payable before Charge.")
                    .font(.plusJakarta(size: 10))
                    .foregroundStyle(splitOk ? OperatingV12.subtitle.opacity(0.8) : Color(hex: "#EF4444"))
            } else {
                Text("One instrument posts as contract.payments · use Split for UPI+Cash+Card.")
                    .font(.plusJakarta(size: 10))
                    .foregroundStyle(OperatingV12.subtitle.opacity(0.8))
            }
        }
    }
}

/// Figma denser browser CTA (`+ New stock item` / `+ New product`) under catalogue grids.
struct RoseDenseCreateCta: View {
    let theme: OperatingTheme
    let title: String
    let hint: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack {
                Text(title)
                    .font(.plusJakarta(size: 13, weight: .bold))
                    .foregroundStyle(theme.accent)
                Spacer()
                Text(hint)
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(OperatingV12.subtitle)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 12)
            .background(theme.accent.opacity(0.08))
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .strokeBorder(theme.accent.opacity(0.40), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        }
        .buttonStyle(.plain)
    }
}

struct RoseContextLine: View {
    let text: String
    var body: some View {
        Text(text)
            .font(.plusJakarta(size: 12, weight: .medium))
            .foregroundStyle(RoseSheetChrome.textBody)
            .frame(maxWidth: .infinity, alignment: .leading)
    }
}

struct RoseLockedChip: View {
    let theme: OperatingTheme
    let label: String

    var body: some View {
        Text(label)
            .font(.plusJakarta(size: 12, weight: .medium))
            .foregroundStyle(RoseSheetChrome.textMuted.opacity(0.7))
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(RoseSheetChrome.sheet.opacity(0.5))
            .overlay(Capsule().strokeBorder(RoseSheetChrome.roseBorder.opacity(0.5), lineWidth: 1))
            .clipShape(Capsule())
            .accessibilityLabel("\(label), unavailable")
    }
}

/// Create | Edit existing mode chips shared by master sheets.
struct RoseCreateEditMode: View {
    let theme: OperatingTheme
    @Binding var isCreate: Bool
    var editLocked: Bool = false
    var onEdit: (() -> Void)? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            RoseFieldLabel(text: "mode")
            HStack(spacing: 6) {
                RoseModeChip(theme: theme, text: "Create", selected: isCreate) {
                    isCreate = true
                }
                if editLocked {
                    RoseLockedChip(theme: theme, label: "Edit existing")
                } else {
                    RoseModeChip(theme: theme, text: "Edit existing", selected: !isCreate) {
                        isCreate = false
                        onEdit?()
                    }
                }
            }
        }
    }
}

/// GST / tax total row that never invents a computed amount.
struct RoseUnavailableGstRow: View {
    let theme: OperatingTheme
    var label: String = "Input GST · CGST+SGST"
    var caption: String = "Unavailable — stamped server-side on bill / GRN"

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 1) {
                Text(label)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(OperatingV12.subtitle)
                Text(caption)
                    .font(.plusJakarta(size: 10))
                    .foregroundStyle(OperatingV12.subtitle.opacity(0.8))
            }
            Spacer()
            Text("Unavailable")
                .font(.plusJakarta(size: 14, weight: .semibold))
                .foregroundStyle(theme.warning)
        }
    }
}

/// GST estimate when rates+amounts exist; else Unavailable. UI-only — never posts ledger tax.
struct RoseGstEstimateRow: View {
    let theme: OperatingTheme
    var label: String
    var estimate: Decimal?
    var currency: String
    var captionWhenKnown: String = "Estimate only — GST posts on Charge / Purchase"
    var captionWhenMissing: String = "Unavailable — rate missing on line / master"

    var body: some View {
        if let estimate {
            HStack {
                VStack(alignment: .leading, spacing: 1) {
                    Text(label)
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(OperatingV12.subtitle)
                    Text(captionWhenKnown)
                        .font(.plusJakarta(size: 10))
                        .foregroundStyle(OperatingV12.subtitle.opacity(0.8))
                }
                Spacer()
                Text(OperatingV12Money.amount(NSDecimalNumber(decimal: estimate).stringValue, currency: currency))
                    .font(.plusJakarta(size: 14, weight: .semibold))
                    .foregroundStyle(theme.warning)
            }
        } else {
            RoseUnavailableGstRow(theme: theme, label: label, caption: captionWhenMissing)
        }
    }
}

/// On-sheet numeric keypad for Quick amount — same amount string as the text field path.
struct RoseNumericKeypad: View {
    let theme: OperatingTheme
    @Binding var value: String
    var maxLength: Int = 12

    private let keys = [
        ["1", "2", "3"],
        ["4", "5", "6"],
        ["7", "8", "9"],
        [".", "0", "⌫"],
    ]

    var body: some View {
        VStack(spacing: 8) {
            ForEach(keys, id: \.self) { row in
                HStack(spacing: 8) {
                    ForEach(row, id: \.self) { key in
                        Button {
                            value = RoseKeypad.apply(current: value, key: key, maxLength: maxLength)
                        } label: {
                            Text(key)
                                .font(.plusJakarta(size: 16, weight: .bold))
                                .foregroundStyle(Color.white)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 14)
                                .background(RoseSheetChrome.sheet)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                                        .strokeBorder(RoseSheetChrome.roseBorder, lineWidth: 1)
                                )
                                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
    }
}

enum RoseKeypad {
    static func apply(current: String, key: String, maxLength: Int = 12) -> String {
        switch key {
        case "⌫":
            return String(current.dropLast())
        case ".":
            if current.contains(".") { return current }
            if current.isEmpty { return "0." }
            return String((current + ".").prefix(maxLength))
        default:
            if current == "0" { return String(key.prefix(maxLength)) }
            return String((current + key).prefix(maxLength))
        }
    }
}

struct RoseSectionCard<Content: View>: View {
    let theme: OperatingTheme
    let label: String
    var trailing: String? = nil
    @ViewBuilder var content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text(label)
                    .font(.plusJakarta(size: 14, weight: .bold))
                    .foregroundStyle(OperatingV12.title)
                Spacer()
                if let trailing {
                    Text(trailing)
                        .font(.plusJakarta(size: 11))
                        .foregroundStyle(RoseSheetChrome.textMuted)
                }
            }
            VStack(alignment: .leading, spacing: 10) {
                content()
            }
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoseSheetChrome.sheet)
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .strokeBorder(theme.accent.opacity(0.40), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}

/// Register strip under POS title — location-derived; Register/Shift are Figma chrome labels.
struct RoseRegisterContext: View {
    let text: String
    var body: some View {
        Text(text)
            .font(.plusJakarta(size: 12, weight: .medium))
            .foregroundStyle(RoseSheetChrome.textBody)
            .frame(maxWidth: .infinity, alignment: .leading)
    }
}
