import SwiftUI

// MARK: - Retail capture chrome (Figma RTL-QA rose / amber)

enum RoseSheetChrome {
    static let label = Color(hex: "#FBBF24")
    static let context = Color(hex: "#CFEFF6")
}

struct RoseGrabber: View {
    var body: some View {
        Capsule()
            .fill(Color.white.opacity(0.22))
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
    var onClose: () -> Void

    var body: some View {
        HStack(alignment: .top) {
            VStack(alignment: .leading, spacing: 4) {
                if let qaCode {
                    Text(qaCode)
                        .font(.plusJakarta(size: 10, weight: .bold))
                        .tracking(0.6)
                        .foregroundStyle(theme.accent)
                }
                Text(title)
                    .font(.plusJakarta(size: 22, weight: .semibold))
                    .foregroundStyle(OperatingV12.title)
                if let subtitle {
                    Text(subtitle)
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(OperatingV12.subtitle)
                }
            }
            Spacer(minLength: 8)
            Button(action: onClose) {
                Image(systemName: "xmark")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundStyle(OperatingV12.subtitle)
                    .frame(width: 32, height: 32)
                    .background(theme.inset)
                    .clipShape(Circle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Close")
        }
    }
}

struct RoseFieldLabel: View {
    let text: String
    var body: some View {
        Text(text.uppercased())
            .font(.plusJakarta(size: 10, weight: .semibold))
            .tracking(0.4)
            .foregroundStyle(RoseSheetChrome.label)
    }
}

struct RoseModeChip: View {
    let theme: OperatingTheme
    let text: String
    let selected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(text)
                .font(.plusJakarta(size: 12, weight: .semibold))
                .foregroundStyle(selected ? theme.onAccent : OperatingV12.subtitle)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(selected ? theme.accent : theme.inset)
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }
}

struct RoseChipRow: View {
    let theme: OperatingTheme
    let options: [String]
    @Binding var selection: String

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 6) {
                ForEach(options, id: \.self) { option in
                    RoseModeChip(theme: theme, text: option, selected: selection == option) {
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

    var body: some View {
        TextField(placeholder, text: $text)
            .keyboardType(keyboard)
            .font(.plusJakarta(size: 14))
            .foregroundStyle(OperatingV12.title)
            .padding(.horizontal, 12)
            .padding(.vertical, 12)
            .background(theme.inset)
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .strokeBorder(theme.border.opacity(0.6), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

struct RoseScanField: View {
    let theme: OperatingTheme
    let placeholder: String
    @Binding var query: String
    var actionLabel: String = "Add"
    var action: () -> Void
    var disabled: Bool = false

    var body: some View {
        HStack(spacing: 8) {
            RoseTextField(theme: theme, placeholder: placeholder, text: $query)
            Button(action: action) {
                Text(actionLabel)
                    .font(.plusJakarta(size: 13, weight: .bold))
                    .foregroundStyle(disabled ? OperatingV12.subtitle : theme.onAccent)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 12)
                    .background(disabled ? theme.inset : theme.accent)
                    .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
            }
            .buttonStyle(.plain)
            .disabled(disabled)
        }
    }
}

struct RoseStepper: View {
    let theme: OperatingTheme
    let quantity: Int
    let onDecrement: () -> Void
    let onIncrement: () -> Void

    var body: some View {
        HStack(spacing: 6) {
            stepButton("minus", action: onDecrement)
            Text("\(quantity)")
                .font(.plusJakarta(size: 12, weight: .bold))
                .foregroundStyle(OperatingV12.title)
                .frame(minWidth: 16)
            stepButton("plus", action: onIncrement)
        }
        .foregroundStyle(theme.accent)
    }

    private func stepButton(_ symbol: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: symbol)
                .font(.system(size: 11, weight: .bold))
                .frame(width: 24, height: 24)
                .background(theme.canvas)
                .clipShape(RoundedRectangle(cornerRadius: 6))
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
                    .font(.plusJakarta(size: 13, weight: .bold))
                    .foregroundStyle(theme.accent)
                Spacer()
                Text(trailing)
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(OperatingV12.subtitle)
            }
            content()
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .background(theme.inset)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
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
        VStack(alignment: .leading, spacing: 6) {
            RoseFieldLabel(text: label)
            if let amountBinding {
                RoseTextField(theme: theme, placeholder: "0.00", text: amountBinding, keyboard: .decimalPad)
            } else {
                Text(amountText.isEmpty ? "—" : amountText)
                    .font(.plusJakarta(size: 28, weight: .bold))
                    .foregroundStyle(OperatingV12.title)
            }
            if let caption {
                Text(caption)
                    .font(.plusJakarta(size: 10))
                    .foregroundStyle(OperatingV12.subtitle)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(theme.accent.opacity(0.12))
        .overlay(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .strokeBorder(theme.accent.opacity(0.45), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

struct RosePickerRow: View {
    let theme: OperatingTheme
    let label: String
    let value: String
    var action: (() -> Void)? = nil

    var body: some View {
        Button {
            action?()
        } label: {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    RoseFieldLabel(text: label)
                    Text(value.isEmpty ? "Select…" : value)
                        .font(.plusJakarta(size: 14, weight: .medium))
                        .foregroundStyle(value.isEmpty ? OperatingV12.subtitle : OperatingV12.title)
                }
                Spacer()
                Text("›")
                    .font(.plusJakarta(size: 18, weight: .semibold))
                    .foregroundStyle(theme.accent)
            }
            .padding(12)
            .background(theme.inset)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        }
        .buttonStyle(.plain)
        .disabled(action == nil)
    }
}

struct RoseMoreDetails<Content: View>: View {
    @State private var open = false
    let title: String
    @ViewBuilder var content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Button {
                withAnimation(.easeInOut(duration: 0.2)) { open.toggle() }
            } label: {
                HStack {
                    Text(title)
                        .font(.plusJakarta(size: 13, weight: .semibold))
                        .foregroundStyle(OperatingV12.title)
                    Spacer()
                    Image(systemName: open ? "chevron.up" : "chevron.down")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundStyle(OperatingV12.subtitle)
                }
            }
            .buttonStyle(.plain)
            if open {
                content()
            }
        }
    }
}

struct RoseWhyBox: View {
    let theme: OperatingTheme
    let text: String

    var body: some View {
        Text(text)
            .font(.plusJakarta(size: 11, weight: .medium))
            .foregroundStyle(RoseSheetChrome.context)
            .padding(12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(theme.accent.opacity(0.08))
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .strokeBorder(theme.accent.opacity(0.3), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

struct RoseStickyFooter: View {
    let theme: OperatingTheme
    let title: String
    var disabled: Bool = false
    var loading: Bool = false
    let action: () -> Void

    var body: some View {
        VStack(spacing: 8) {
            Button(action: action) {
                Text(loading ? "Saving…" : title)
                    .font(.plusJakarta(size: 15, weight: .bold))
                    .foregroundStyle(theme.onAccent)
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .background(disabled || loading ? theme.accent.opacity(0.4) : theme.accent)
                    .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            }
            .buttonStyle(.plain)
            .disabled(disabled || loading)
            Text("Draft preserved if you close or open a picker")
                .font(.plusJakarta(size: 10))
                .foregroundStyle(OperatingV12.subtitle)
        }
    }
}

struct RoseTenderChips: View {
    let theme: OperatingTheme
    @Binding var tender: String
    let options: [String]

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            RoseFieldLabel(text: "Tender")
            HStack(spacing: 6) {
                ForEach(options, id: \.self) { option in
                    RoseModeChip(theme: theme, text: option, selected: tender == option) {
                        tender = option
                    }
                }
                Text("Split")
                    .font(.plusJakarta(size: 12, weight: .medium))
                    .foregroundStyle(OperatingV12.subtitle.opacity(0.7))
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(theme.inset.opacity(0.5))
                    .overlay(Capsule().strokeBorder(theme.border.opacity(0.5), lineWidth: 1))
                    .clipShape(Capsule())
            }
            Text("Split tender is locked — one payment state per post.")
                .font(.plusJakarta(size: 10))
                .foregroundStyle(OperatingV12.subtitle)
        }
    }
}

struct RoseContextLine: View {
    let text: String
    var body: some View {
        Text(text)
            .font(.plusJakarta(size: 11, weight: .medium))
            .foregroundStyle(RoseSheetChrome.context)
            .frame(maxWidth: .infinity, alignment: .leading)
    }
}

struct RoseLockedChip: View {
    let theme: OperatingTheme
    let label: String

    var body: some View {
        Text(label)
            .font(.plusJakarta(size: 12, weight: .medium))
            .foregroundStyle(OperatingV12.subtitle.opacity(0.7))
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(theme.inset.opacity(0.5))
            .overlay(Capsule().strokeBorder(theme.border.opacity(0.5), lineWidth: 1))
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
                                .font(.plusJakarta(size: 18, weight: .semibold))
                                .foregroundStyle(OperatingV12.title)
                                .frame(maxWidth: .infinity)
                                .frame(height: 48)
                                .background(theme.inset)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                                        .strokeBorder(theme.border.opacity(0.5), lineWidth: 1)
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
    @ViewBuilder var content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            RoseFieldLabel(text: label)
            VStack(alignment: .leading, spacing: 10) {
                content()
            }
            .padding(12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(theme.inset)
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .strokeBorder(theme.accent.opacity(0.35), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        }
    }
}
