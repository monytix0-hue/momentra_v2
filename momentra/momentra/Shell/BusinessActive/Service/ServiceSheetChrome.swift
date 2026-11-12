import SwiftUI

// MARK: - Service capture chrome (Figma SVC teal / lime)

enum TealSheetChrome {
    static let label = Color(hex: "#5EEAD4")
    static let whyBody = ServiceCaptureCopy.whyBody
    static let chipBorder = ServiceCaptureCopy.chipBorder
    static let inkOnAccent = ServiceCaptureCopy.inkOnAccent
    static let lime = Color(hex: "#84CC16")
}

struct TealGrabber: View {
    var body: some View {
        Capsule()
            .fill(Color(hex: "#5EEAD4").opacity(0.55))
            .frame(width: 40, height: 4)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 4)
    }
}

struct TealSheetHeader: View {
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
                    .font(.plusJakarta(size: 22, weight: .bold))
                    .foregroundStyle(OperatingV12.title)
                if let subtitle {
                    Text(subtitle)
                        .font(.plusJakarta(size: 12, weight: .medium))
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

struct TealFieldLabel: View {
    let text: String
    var body: some View {
        // Figma SVC labels are title case (Client, Service), not all-caps.
        Text(text)
            .font(.plusJakarta(size: 11, weight: .medium))
            .foregroundStyle(TealSheetChrome.label)
    }
}

struct TealModeChip: View {
    let theme: OperatingTheme
    let text: String
    let selected: Bool
    /// `.accent` = teal fill + lime border (QA mode chips). `.lime` = lime fill (categories / invite).
    var style: TealChipStyle = .accent
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(text)
                .font(.plusJakarta(size: 12, weight: .semibold))
                .foregroundStyle(selected ? TealSheetChrome.inkOnAccent : OperatingV12.subtitle)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(selectedFill)
                .overlay(
                    Capsule()
                        .strokeBorder(selectedStroke, lineWidth: selected ? 1.5 : 1)
                )
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }

    private var selectedFill: Color {
        guard selected else { return theme.inset }
        return style == .lime ? TealSheetChrome.lime : theme.accent
    }

    private var selectedStroke: Color {
        guard selected else { return TealSheetChrome.chipBorder }
        return TealSheetChrome.lime
    }
}

enum TealChipStyle {
    case accent
    case lime
}

struct TealChipRow: View {
    let theme: OperatingTheme
    let options: [String]
    @Binding var selection: String
    var style: TealChipStyle = .accent

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 6) {
                ForEach(options, id: \.self) { option in
                    TealModeChip(
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

struct TealMultiChipRow: View {
    let theme: OperatingTheme
    let options: [String]
    @Binding var selection: Set<String>
    var style: TealChipStyle = .accent

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 6) {
                ForEach(options, id: \.self) { option in
                    TealModeChip(
                        theme: theme,
                        text: option,
                        selected: selection.contains(option),
                        style: style
                    ) {
                        if selection.contains(option) {
                            selection.remove(option)
                        } else {
                            selection.insert(option)
                        }
                    }
                }
            }
        }
    }
}

struct TealTextField: View {
    let theme: OperatingTheme
    let placeholder: String
    @Binding var text: String
    var keyboard: UIKeyboardType = .default

    var body: some View {
        TextField(placeholder, text: $text)
            .font(.plusJakarta(size: 14, weight: .medium))
            .foregroundStyle(OperatingV12.title)
            .keyboardType(keyboard)
            .padding(.horizontal, 12)
            .padding(.vertical, 12)
            .background(theme.inset)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .strokeBorder(TealSheetChrome.chipBorder.opacity(0.7), lineWidth: 1)
            )
    }
}

struct TealWhyBox: View {
    let theme: OperatingTheme
    let text: String

    var body: some View {
        // Figma: lime “Why” + cream body on one horizontal row.
        HStack(alignment: .top, spacing: 8) {
            Text("Why")
                .font(.plusJakarta(size: 11, weight: .bold))
                .foregroundStyle(TealSheetChrome.lime)
            Text(text)
                .font(.plusJakarta(size: 11, weight: .medium))
                .foregroundStyle(TealSheetChrome.whyBody)
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(theme.accent.opacity(0.12))
        .overlay(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .strokeBorder(theme.accent.opacity(0.4), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

struct TealStickyFooter: View {
    let theme: OperatingTheme
    let title: String
    var caption: String? = nil
    var disabled: Bool = false
    var loading: Bool = false
    let action: () -> Void

    var body: some View {
        // Figma sticky footer: CTA first, caption under the button.
        VStack(spacing: 6) {
            Button(action: action) {
                Text(loading ? "Saving…" : title)
                    .font(.plusJakarta(size: 15, weight: .bold))
                    .foregroundStyle(TealSheetChrome.inkOnAccent)
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .background(
                        LinearGradient(
                            colors: disabled
                                ? [theme.accent.opacity(0.35), TealSheetChrome.lime.opacity(0.35)]
                                : [theme.accent, TealSheetChrome.lime],
                            startPoint: .leading,
                            endPoint: .trailing
                        )
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                    .shadow(color: theme.accent.opacity(disabled ? 0 : 0.45), radius: 12, y: 6)
            }
            .buttonStyle(.plain)
            .disabled(disabled || loading)
            if let caption {
                Text(caption)
                    .font(.plusJakarta(size: 10, weight: .medium))
                    .foregroundStyle(OperatingV12.subtitle)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: .infinity)
            }
        }
        .padding(.top, 8)
    }
}

/// Ticket hero used by Hold / Transfer / Reprint (Figma slip card).
struct TealTicketHero: View {
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
                    .foregroundStyle(OperatingV12.subtitle)
                Spacer(minLength: 8)
                Text(code)
                    .font(.plusJakarta(size: 12, weight: .bold))
                    .foregroundStyle(Color(hex: "#FEF3C7"))
            }
            Text(title)
                .font(.plusJakarta(size: 24, weight: .bold))
                .foregroundStyle(OperatingV12.title)
                .lineLimit(2)
            Text(subtitle)
                .font(.plusJakarta(size: 12, weight: .medium))
                .foregroundStyle(OperatingV12.subtitle)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(theme.inset)
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .strokeBorder(TealSheetChrome.lime, lineWidth: 1.5)
        )
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .shadow(color: TealSheetChrome.lime.opacity(0.2), radius: 12, y: 0)
    }
}

struct TealMetaBlock: View {
    let theme: OperatingTheme
    let rows: [(String, String)]

    var body: some View {
        VStack(spacing: 2) {
            ForEach(Array(rows.enumerated()), id: \.offset) { _, row in
                HStack {
                    Text(row.0)
                        .font(.plusJakarta(size: 12, weight: .medium))
                        .foregroundStyle(OperatingV12.subtitle)
                    Spacer(minLength: 8)
                    Text(row.1)
                        .font(.plusJakarta(size: 13, weight: .bold))
                        .foregroundStyle(OperatingV12.title)
                        .multilineTextAlignment(.trailing)
                }
                .padding(.vertical, 4)
            }
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(theme.inset)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

struct TealSectionCard<Content: View>: View {
    let theme: OperatingTheme
    var label: String? = nil
    @ViewBuilder var content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            if let label {
                Text(label.uppercased())
                    .font(.plusJakarta(size: 10, weight: .bold))
                    .tracking(0.5)
                    .foregroundStyle(TealSheetChrome.label)
            }
            content()
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(theme.surface)
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .strokeBorder(theme.accent.opacity(0.28), lineWidth: 1)
        )
    }
}

struct TealEmptyCard: View {
    let theme: OperatingTheme
    let title: String
    let bodyText: String

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
                .font(.plusJakarta(size: 13, weight: .semibold))
                .foregroundStyle(OperatingV12.title)
            Text(bodyText)
                .font(.plusJakarta(size: 12, weight: .medium))
                .foregroundStyle(OperatingV12.subtitle)
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(theme.inset)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

struct TealCreateEditMode: View {
    let theme: OperatingTheme
    @Binding var isCreate: Bool
    var editLocked: Bool = false
    var onEdit: () -> Void = {}

    var body: some View {
        HStack(spacing: 8) {
            TealModeChip(theme: theme, text: "Create", selected: isCreate, style: .lime) {
                isCreate = true
            }
            TealModeChip(theme: theme, text: "Edit existing", selected: !isCreate, style: .lime) {
                guard !editLocked else { return }
                isCreate = false
                onEdit()
            }
            Spacer(minLength: 0)
        }
    }
}

struct TealPickerRow: View {
    let theme: OperatingTheme
    let label: String
    let value: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack {
                Text(label)
                    .font(.plusJakarta(size: 11, weight: .semibold))
                    .foregroundStyle(TealSheetChrome.label)
                Spacer(minLength: 8)
                Text(value)
                    .font(.plusJakarta(size: 13, weight: .semibold))
                    .foregroundStyle(OperatingV12.title)
                    .lineLimit(1)
                Image(systemName: "chevron.right")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundStyle(OperatingV12.subtitle)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 12)
            .background(theme.inset)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        }
        .buttonStyle(.plain)
    }
}

struct TealSavedToast: View {
    var body: some View {
        Text(ServiceCaptureCopy.savedToast)
            .font(.plusJakarta(size: 12, weight: .semibold))
            .foregroundStyle(TealSheetChrome.inkOnAccent)
            .padding(.horizontal, 14)
            .padding(.vertical, 10)
            .background(TealSheetChrome.lime)
            .clipShape(Capsule())
            .shadow(color: .black.opacity(0.25), radius: 10, y: 4)
    }
}
