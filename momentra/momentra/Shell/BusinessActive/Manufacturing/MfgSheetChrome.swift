import SwiftUI

// MARK: - Manufacturing capture chrome (Figma MFG amber / zinc · Forge & Form)

enum MfgSheetChrome {
    /// Amber labels / Why / Add line (`#FBBF24`).
    static let label = BusinessTokens.Colors.manufacturingAccentSoft
    /// Legacy cyan context — prefer `whyBody` on POS Why notes.
    static let context = Color(hex: "#CFEFF6")
    /// Why body cream / rose soft (`#FFE4E6`) from RTL POS.
    static let whyBody = Color(hex: "#FEF3C7")
    /// Sheet / tile / chip slate (`#1E293B`).
    static let sheet = BusinessTokens.Colors.manufacturingSurface
    /// Inactive chip / field stroke (`#E11D48`).
    static let roseBorder = BusinessTokens.Colors.manufacturingAccentDeep
    /// Grab + ALT soft rose (`#FB7185`).
    static let grab = BusinessTokens.Colors.manufacturingAccentSoft
    /// Tender selected fill (`#22C55E`).
    static let tenderSelected = Color(hex: "#22C55E")
    /// Body / muted slate text.
    static let textBody = Color(hex: "#A1A1AA")
    static let textMuted = Color(hex: "#71717A")
}

struct MfgGrabber: View {
    var body: some View {
        Capsule()
            .fill(Color(hex: "#71717A"))
            .frame(width: 40, height: 4)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 4)
    }
}

struct MfgSheetHeader: View {
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
                        .background(MfgSheetChrome.grab)
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
                        .foregroundStyle(MfgSheetChrome.textBody)
                        .frame(width: 32, height: 32)
                        .background(MfgSheetChrome.sheet)
                        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Close")
            }
            // Optional legacy subtitle kept for API compatibility; POS uses MfgPlantContext.
            if let subtitle {
                Text(subtitle)
                    .font(.plusJakarta(size: 12, weight: .medium))
                    .foregroundStyle(MfgSheetChrome.textBody)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
    }
}

struct MfgFieldLabel: View {
    let text: String
    var body: some View {
        // Figma RTL labels are title case (Scan or search SKU), not all-caps.
        Text(text)
            .font(.plusJakarta(size: 11, weight: .medium))
            .foregroundStyle(MfgSheetChrome.textBody)
    }
}

struct MfgModeChip: View {
    let theme: OperatingTheme
    let text: String
    let selected: Bool
    var style: MfgChipStyle = .accent
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(text)
                .font(.plusJakarta(size: 12, weight: selected ? .bold : .medium))
                .foregroundStyle(selected ? (style == .tender ? Color.white : BusinessTokens.Colors.manufacturingOnAccent) : MfgSheetChrome.textBody)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(selectedFill)
                .overlay(
                    Capsule()
                        .strokeBorder(selectedStroke, lineWidth: 1)
                )
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }

    private var selectedFill: Color {
        guard selected else { return MfgSheetChrome.sheet }
        return style == .tender ? MfgSheetChrome.tenderSelected : theme.accent
    }

    private var selectedStroke: Color {
        guard selected else { return MfgSheetChrome.roseBorder }
        return style == .tender ? theme.accent : theme.accent
    }
}

enum MfgChipStyle {
    case accent
    case tender
}

struct MfgChipRow: View {
    let theme: OperatingTheme
    let options: [String]
    @Binding var selection: String
    var style: MfgChipStyle = .accent

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 6) {
                ForEach(options, id: \.self) { option in
                    MfgModeChip(
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

struct MfgTextField: View {
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
                    .foregroundStyle(MfgSheetChrome.textBody)
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 12)
        .background(MfgSheetChrome.sheet)
        .overlay(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .strokeBorder(MfgSheetChrome.roseBorder, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

struct MfgScanField: View {
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
        .background(MfgSheetChrome.sheet)
        .overlay(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .strokeBorder(theme.accent, lineWidth: 1.5)
        )
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

struct MfgStepper: View {
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
                .background(MfgSheetChrome.sheet)
                .overlay(
                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                        .strokeBorder(MfgSheetChrome.roseBorder, lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
        }
        .buttonStyle(.plain)
    }
}

struct MfgLinesCard<Content: View>: View {
    let theme: OperatingTheme
    var title: String
    var trailing: String
    @ViewBuilder var content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(title)
                    .font(.plusJakarta(size: 12, weight: .bold))
                    .foregroundStyle(MfgSheetChrome.label)
                Spacer()
                Text(trailing)
                    .font(.plusJakarta(size: 11, weight: .medium))
                    .foregroundStyle(MfgSheetChrome.textBody)
            }
            content()
        }
        .padding(14)
        .background(MfgSheetChrome.sheet)
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .strokeBorder(theme.accent.opacity(0.30), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}

struct MfgEmptyCard: View {
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

struct MfgTotalsCard: View {
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

struct MfgAmountHero: View {
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
                    .foregroundStyle(MfgSheetChrome.textBody)
                Spacer()
                Text(currency.isEmpty ? "INR" : currency)
                    .font(.plusJakarta(size: 11, weight: .bold))
                    .foregroundStyle(MfgSheetChrome.label)
            }
            if let amountBinding {
                MfgTextField(theme: theme, placeholder: "0.00", text: amountBinding, keyboard: .decimalPad)
            } else {
                Text(amountText.isEmpty ? "—" : amountText)
                    .font(.plusJakarta(size: 40, weight: .bold))
                    .foregroundStyle(OperatingV12.title)
            }
            if let caption {
                Text(caption)
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(MfgSheetChrome.textBody)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(MfgSheetChrome.sheet)
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .strokeBorder(theme.accent, lineWidth: 1.5)
        )
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}

struct MfgPickerRow: View {
    let theme: OperatingTheme
    let label: String
    let value: String
    var action: (() -> Void)? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            MfgFieldLabel(text: label)
            Button {
                action?()
            } label: {
                HStack {
                    Text(value.isEmpty ? "Select…" : value)
                        .font(.plusJakarta(size: 14, weight: .medium))
                        .foregroundStyle(value.isEmpty ? MfgSheetChrome.textMuted : OperatingV12.title)
                    Spacer()
                    Text("›")
                        .font(.plusJakarta(size: 16, weight: .bold))
                        .foregroundStyle(MfgSheetChrome.textBody)
                }
                .padding(12)
                .background(MfgSheetChrome.sheet)
                .overlay(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .strokeBorder(MfgSheetChrome.roseBorder, lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            }
            .buttonStyle(.plain)
            .disabled(action == nil)
        }
    }
}

/// Products & BOM catalogue row — Figma `2545:29477`.
struct MfgCatalogPartRow: View {
    let theme: OperatingTheme
    let name: String
    let meta: String
    let priceLabel: String
    var gstBadge: String? = nil
    var editLocked: Bool = true
    var accentDot: Bool = false
    var onEdit: (() -> Void)? = nil

    var body: some View {
        HStack(spacing: 10) {
            RoundedRectangle(cornerRadius: 2, style: .continuous)
                .fill(accentDot ? theme.accent : Color(hex: "#22C55E"))
                .frame(width: 10, height: 10)
            VStack(alignment: .leading, spacing: 2) {
                Text(name)
                    .font(.plusJakarta(size: 14, weight: .semibold))
                    .foregroundStyle(OperatingV12.title)
                    .lineLimit(1)
                Text(meta)
                    .font(.plusJakarta(size: 11, weight: .medium))
                    .foregroundStyle(MfgSheetChrome.textBody)
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            Text(priceLabel)
                .font(.plusJakarta(size: 14, weight: .bold))
                .foregroundStyle(theme.accentSoft)
            if let gstBadge, !gstBadge.isEmpty {
                Text(gstBadge)
                    .font(.plusJakarta(size: 10, weight: .bold))
                    .foregroundStyle(theme.accentSoft)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(theme.inset, in: Capsule())
            }
            Button {
                if !editLocked { onEdit?() }
            } label: {
                Text(editLocked ? "Edit · locked" : "Edit")
                    .font(.plusJakarta(size: 12, weight: .bold))
                    .foregroundStyle(editLocked ? MfgSheetChrome.textMuted : OperatingV12.title)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(MfgSheetChrome.sheet)
                    .overlay(
                        RoundedRectangle(cornerRadius: 10, style: .continuous)
                            .strokeBorder(theme.border, lineWidth: 1)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
            }
            .buttonStyle(.plain)
            .disabled(editLocked)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(theme.inset)
        .overlay(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .strokeBorder(theme.border, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
    }
}

struct MfgMoreDetails<Content: View>: View {
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
                        .foregroundStyle(MfgSheetChrome.label)
                    Spacer()
                    Text(hint)
                        .font(.plusJakarta(size: 11))
                        .foregroundStyle(MfgSheetChrome.textBody)
                        .lineLimit(1)
                    Text(open ? "⌃" : "⌄")
                        .font(.plusJakarta(size: 14, weight: .bold))
                        .foregroundStyle(MfgSheetChrome.label)
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

struct MfgWhyBox: View {
    let theme: OperatingTheme
    let text: String

    var body: some View {
        HStack(alignment: .top, spacing: 8) {
            Text("Why")
                .font(.plusJakarta(size: 11, weight: .bold))
                .foregroundStyle(MfgSheetChrome.label)
            Text(text)
                .font(.plusJakarta(size: 11))
                .foregroundStyle(MfgSheetChrome.whyBody)
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

struct MfgStickyFooter: View {
    let theme: OperatingTheme
    let title: String
    var caption: String = "Draft preserved if you close or open a picker"
    var disabled: Bool = false
    var loading: Bool = false
    let action: () -> Void

    var body: some View {
        VStack(spacing: 6) {
            Button(action: action) {
                Text(loading ? "Posting…" : title)
                    .font(.plusJakarta(size: 15, weight: .bold))
                    .foregroundStyle(disabled || loading ? MfgSheetChrome.textMuted : BusinessTokens.Colors.manufacturingOnAccent)
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
                .foregroundStyle(MfgSheetChrome.textBody)
                .multilineTextAlignment(.center)
        }
        .padding(.top, 8)
    }
}

struct MfgTenderChips: View {
    let theme: OperatingTheme
    @Binding var tender: String
    var options: [String] = ["UPI", "Cash", "Card", "Split"]

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            MfgFieldLabel(text: "Tender")
            HStack(spacing: 6) {
                ForEach(options, id: \.self) { option in
                    MfgModeChip(
                        theme: theme,
                        text: option,
                        selected: tender == option,
                        style: .tender
                    ) {
                        tender = option
                    }
                }
            }
        }
    }
}

/// Figma denser browser CTA (`+ New stock item` / `+ New product`) under catalogue grids.
struct MfgDenseCreateCta: View {
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

struct MfgContextLine: View {
    let text: String
    var body: some View {
        Text(text)
            .font(.plusJakarta(size: 11, weight: .medium))
            .foregroundStyle(MfgSheetChrome.context)
            .frame(maxWidth: .infinity, alignment: .leading)
    }
}

struct MfgLockedChip: View {
    let theme: OperatingTheme
    let label: String

    var body: some View {
        Text(label)
            .font(.plusJakarta(size: 12, weight: .medium))
            .foregroundStyle(MfgSheetChrome.textMuted.opacity(0.7))
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(MfgSheetChrome.sheet.opacity(0.5))
            .overlay(Capsule().strokeBorder(MfgSheetChrome.roseBorder.opacity(0.5), lineWidth: 1))
            .clipShape(Capsule())
            .accessibilityLabel("\(label), unavailable")
    }
}

/// Create | Edit existing mode chips shared by master sheets.
struct MfgCreateEditMode: View {
    let theme: OperatingTheme
    @Binding var isCreate: Bool
    var editLocked: Bool = false
    var onEdit: (() -> Void)? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            MfgFieldLabel(text: "mode")
            HStack(spacing: 6) {
                MfgModeChip(theme: theme, text: "Create", selected: isCreate) {
                    isCreate = true
                }
                if editLocked {
                    MfgLockedChip(theme: theme, label: "Edit existing")
                } else {
                    MfgModeChip(theme: theme, text: "Edit existing", selected: !isCreate) {
                        isCreate = false
                        onEdit?()
                    }
                }
            }
        }
    }
}

/// GST / tax total row that never invents a computed amount.
struct MfgUnavailableGstRow: View {
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
struct MfgNumericKeypad: View {
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
                            value = MfgKeypad.apply(current: value, key: key, maxLength: maxLength)
                        } label: {
                            Text(key)
                                .font(.plusJakarta(size: 16, weight: .bold))
                                .foregroundStyle(Color.white)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 14)
                                .background(MfgSheetChrome.sheet)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                                        .strokeBorder(MfgSheetChrome.roseBorder, lineWidth: 1)
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

enum MfgKeypad {
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

struct MfgSectionCard<Content: View>: View {
    let theme: OperatingTheme
    let label: String
    var trailing: String? = nil
    @ViewBuilder var content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(label)
                    .font(.plusJakarta(size: 12, weight: .bold))
                    .foregroundStyle(MfgSheetChrome.label)
                Spacer()
                if let trailing {
                    Text(trailing)
                        .font(.plusJakarta(size: 11, weight: .medium))
                        .foregroundStyle(MfgSheetChrome.textBody)
                }
            }
            VStack(alignment: .leading, spacing: 10) {
                content()
            }
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(MfgSheetChrome.sheet)
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .strokeBorder(theme.accent.opacity(0.30), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}

/// Register strip under POS title — location-derived; Register/Shift are Figma chrome labels.
struct MfgPlantContext: View {
    let text: String
    var body: some View {
        Text(text)
            .font(.plusJakarta(size: 12, weight: .medium))
            .foregroundStyle(MfgSheetChrome.textBody)
            .frame(maxWidth: .infinity, alignment: .leading)
    }
}
