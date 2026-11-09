import SwiftUI

/// The document chrome shared by `TRD-SETUP-v6` and `DST-SETUP-v6`.
///
/// View-for-view port of `SetupDocumentKit.kt`: each component here owns one piece of the Figma
/// document (`SetupSection` → `SetupSectionCard`, and so on). Keeping them in one file means the
/// six sections in `BusinessSetupWizardView` compose the document instead of redrawing its
/// chrome with operating-screen tokens.
enum SetupDocSectionGap {
    static let value: CGFloat = 14
}

// MARK: - Section

/// One numbered section: accent `01`, 18 bold title, 12 subtitle, then a 20 radius card whose
/// 1dp stroke is the accent at 28%. Body content is 16 padded with 14 stacking.
struct SetupSectionCard<Content: View>: View {
    let spec: BusinessSetupSectionSpec
    let theme: BusinessSetupDocTheme
    @ViewBuilder var content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 8) {
                Text(spec.number)
                    .font(.plusJakarta(size: 13, weight: .bold))
                    .foregroundStyle(theme.accent)
                Text(spec.title)
                    .font(.plusJakarta(size: 18, weight: .bold))
                    .foregroundStyle(theme.ink)
                    .lineLimit(1)
                Spacer(minLength: 0)
            }
            if !spec.subtitle.isEmpty {
                Text(spec.subtitle)
                    .font(.plusJakarta(size: 12, weight: .medium))
                    .foregroundStyle(theme.muted)
                    .fixedSize(horizontal: false, vertical: true)
            }
            content()
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(theme.card)
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .stroke(theme.accent.opacity(0.28), lineWidth: 1)
        )
        .shadow(color: theme.accent.opacity(0.18), radius: 14, x: 0, y: 8)
    }
}

// MARK: - Panel

/// A `panel · Title` inset: 14 radius, 12 padding, 12 stacking, accent 11 bold caption.
struct SetupPanelCard<Content: View>: View {
    let title: String
    let theme: BusinessSetupDocTheme
    @ViewBuilder var content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(title)
                .font(.plusJakarta(size: 11, weight: .bold))
                .foregroundStyle(theme.accent)
            content()
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(theme.inset)
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .stroke(theme.panelStroke, lineWidth: 1)
        )
    }
}

// MARK: - Fields

/// `Field · label` over an input, with the frame's optional 11 helper below it.
struct SetupDocField: View {
    let label: String
    @Binding var text: String
    let theme: BusinessSetupDocTheme
    var hint: String? = nil
    var placeholder: String = ""
    var keyboard: UIKeyboardType = .default
    var axis: Axis = .horizontal
    var tag: String? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label)
                .font(.plusJakarta(size: 11, weight: .medium))
                .foregroundStyle(theme.muted)
            TextField(
                "",
                text: $text,
                prompt: Text(placeholder).foregroundStyle(theme.faint),
                axis: axis
            )
            .font(.plusJakarta(size: 14, weight: .medium))
            .foregroundStyle(theme.ink)
            .keyboardType(keyboard)
            .textInputAutocapitalization(keyboard == .default ? .words : .never)
            .autocorrectionDisabled()
            .tint(theme.accent)
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(theme.inset)
            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .stroke(theme.panelStroke, lineWidth: 1)
            )
            .accessibilityIdentifier(tag ?? "setup.field.\(label)")
            if let hint, !hint.isEmpty {
                Text(hint)
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(theme.faint)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }
}

/// A field that opens a picker instead of a keyboard (`Currency`, dates).
struct SetupDocPickerField: View {
    let label: String
    let value: String
    let theme: BusinessSetupDocTheme
    var placeholder: String = "Tap to pick"
    var hint: String? = nil
    var tag: String
    let onTap: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label)
                .font(.plusJakarta(size: 11, weight: .medium))
                .foregroundStyle(theme.muted)
                .lineLimit(1)
            Button(action: onTap) {
                HStack(spacing: 8) {
                    Text(value.isEmpty ? placeholder : value)
                        .font(.plusJakarta(size: 14, weight: .medium))
                        .foregroundStyle(value.isEmpty ? theme.faint : theme.ink)
                        .lineLimit(1)
                    Spacer(minLength: 0)
                    Text("›")
                        .font(.plusJakarta(size: 16))
                        .foregroundStyle(theme.muted)
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 10)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(theme.inset)
                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                        .stroke(theme.panelStroke, lineWidth: 1)
                )
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityIdentifier(tag)
            if let hint, !hint.isEmpty {
                Text(hint)
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(theme.faint)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }
}

// MARK: - Chips

/// A row of pill chips that wraps; selected is solid accent with inset ink.
struct SetupDocChipRow: View {
    let options: [String]
    let selected: Set<String>
    let theme: BusinessSetupDocTheme
    var label: String? = nil
    var hint: String? = nil
    let onSelect: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            if let label, !label.isEmpty {
                Text(label)
                    .font(.plusJakarta(size: 12, weight: .medium))
                    .foregroundStyle(theme.muted)
            }
            FlowLayout(spacing: 6) {
                ForEach(options, id: \.self) { option in
                    let on = selected.contains(option)
                    Button {
                        onSelect(option)
                    } label: {
                        Text(option)
                            .font(.plusJakarta(size: 11, weight: on ? .bold : .medium))
                            .foregroundStyle(on ? theme.inset : theme.muted)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 6)
                            .background(on ? theme.accent : theme.inset)
                            .clipShape(Capsule())
                            .overlay(
                                Capsule().stroke(on ? theme.accent : theme.panelStroke, lineWidth: 1)
                            )
                    }
                    .buttonStyle(.plain)
                    .accessibilityAddTraits(on ? [.isSelected] : [])
                }
            }
            if let hint, !hint.isEmpty {
                Text(hint)
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(theme.faint)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }
}

// MARK: - Blocks

/// `why` block — accent-tinted card with a bold accent title and a muted explanation.
struct SetupDocWhyCard: View {
    let title: String
    let bodyText: String
    let theme: BusinessSetupDocTheme

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
                .font(.plusJakarta(size: 12, weight: .bold))
                .foregroundStyle(theme.accent)
            Text(bodyText)
                .font(.plusJakarta(size: 11))
                .foregroundStyle(theme.muted)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(theme.accent.opacity(0.10))
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .stroke(theme.accent.opacity(0.32), lineWidth: 1)
        )
    }
}

/// `status` pill — a filled dot plus a one-line verdict.
struct SetupDocStatusPill: View {
    let text: String
    let theme: BusinessSetupDocTheme
    var tone: Color? = nil

    private var resolvedTone: Color { tone ?? theme.positive }

    var body: some View {
        HStack(spacing: 6) {
            Text("●")
                .font(.plusJakarta(size: 9))
                .foregroundStyle(resolvedTone)
            Text(text)
                .font(.plusJakarta(size: 11, weight: .bold))
                .foregroundStyle(resolvedTone)
                .multilineTextAlignment(.leading)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 6)
        .background(resolvedTone.opacity(0.14))
        .clipShape(Capsule())
    }
}

/// Round avatar initials used by the §02 owner and team rows.
struct SetupDocAvatar: View {
    let initials: String
    let theme: BusinessSetupDocTheme
    var size: CGFloat = 36
    var tone: Color? = nil

    var body: some View {
        Text(initials)
            .font(.plusJakarta(size: max(size / 2 - 1, 10), weight: .bold))
            .foregroundStyle(tone ?? theme.muted)
            .frame(width: size, height: size)
            .background(theme.inset)
            .clipShape(Circle())
    }
}

// MARK: - Rows

/// A key/value line — `Company   Northstar Trading`.
struct SetupDocKeyValue: View {
    let label: String
    let value: String
    let theme: BusinessSetupDocTheme

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: 10) {
            Text(label)
                .font(.plusJakarta(size: 12))
                .foregroundStyle(theme.muted)
                .frame(maxWidth: .infinity, alignment: .leading)
            Text(value)
                .font(.plusJakarta(size: 12, weight: .semibold))
                .foregroundStyle(theme.ink)
                .lineLimit(1)
                .truncationMode(.tail)
                .multilineTextAlignment(.trailing)
                .fixedSize(horizontal: false, vertical: true)
        }
    }
}

/// A §04 master row: icon tile, title/subtitle, chevron.
struct SetupDocMasterRow: View {
    let glyph: String
    let title: String
    let subtitle: String
    let theme: BusinessSetupDocTheme
    var tag: String? = nil
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 10) {
                Text(glyph)
                    .font(.plusJakarta(size: 14))
                    .foregroundStyle(theme.ink)
                    .frame(width: 34, height: 34)
                    .background(theme.border)
                    .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.plusJakarta(size: 14, weight: .semibold))
                        .foregroundStyle(theme.ink)
                        .lineLimit(1)
                    Text(subtitle)
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(theme.muted)
                        .lineLimit(2)
                        .multilineTextAlignment(.leading)
                }
                Spacer(minLength: 0)
                Text("›")
                    .font(.plusJakarta(size: 18))
                    .foregroundStyle(theme.muted)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(theme.inset)
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .stroke(theme.panelStroke, lineWidth: 1)
            )
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier(tag ?? "setup.master.\(title)")
    }
}

// MARK: - Actions

/// The document's single filled call to action.
struct SetupDocCta: View {
    let label: String
    let theme: BusinessSetupDocTheme
    var enabled: Bool = true
    var tag: String? = nil
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            Text(label)
                .font(.plusJakarta(size: 15, weight: .bold))
                .foregroundStyle(theme.ink)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(enabled ? theme.accent : theme.accent.opacity(0.35))
                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
        .accessibilityIdentifier(tag ?? "setup.submit")
    }
}

/// A dashed, unfilled action — the `Share QR` / `WhatsApp` pair under §02's people card.
struct SetupDocOutlineAction: View {
    let title: String
    let theme: BusinessSetupDocTheme
    var enabled: Bool = true
    /// A mint in flight: the row shows it rather than greying out with no reason.
    var busy: Bool = false
    let onTap: () -> Void

    private var live: Bool { enabled && !busy }

    var body: some View {
        Button(action: onTap) {
            Group {
                if busy {
                    ProgressView()
                        .tint(theme.accent)
                        .scaleEffect(0.7)
                        .frame(height: 16)
                } else {
                    Text(title)
                        .font(.plusJakarta(size: 13, weight: .semibold))
                        .foregroundStyle(live ? theme.ink : theme.muted)
                }
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 12)
            .overlay(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .stroke(theme.border, lineWidth: 1)
            )
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(!live)
    }
}

// MARK: - Text

/// A muted footnote that closes a section. `SetupFootnote` is 12; the inline notes Android
/// prints from within a panel are 11, so callers can ask for that size.
struct SetupDocFootnote: View {
    let text: String
    let theme: BusinessSetupDocTheme
    var size: CGFloat = 12

    var body: some View {
        if !text.isEmpty {
            Text(text)
                .font(.plusJakarta(size: size))
                .foregroundStyle(theme.muted)
                .frame(maxWidth: .infinity, alignment: .leading)
                .fixedSize(horizontal: false, vertical: true)
        }
    }
}
