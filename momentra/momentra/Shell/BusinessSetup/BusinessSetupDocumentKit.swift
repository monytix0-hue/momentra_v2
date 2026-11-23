import SwiftUI

/// Shared atoms for Business setup documents. Theme-driven Obsidian chrome (rknf setup frames).
enum BusinessSetupDocumentKit {
    struct HeroCard: View {
        let theme: BusinessSetupTheme
        let eyebrow: String
        let title: String
        let subtitle: String

        var body: some View {
            VStack(alignment: .leading, spacing: 16) {
                ZStack {
                    Circle()
                        .fill(
                            RadialGradient(
                                colors: [
                                    theme.accent.opacity(0.55),
                                    theme.accentSoft.opacity(0.22),
                                    Color.clear,
                                ],
                                center: .center,
                                startRadius: 8,
                                endRadius: 72
                            )
                        )
                        .frame(width: 140, height: 140)
                        .blur(radius: 8)
                    Circle()
                        .fill(theme.inset)
                        .frame(width: 72, height: 72)
                        .overlay(
                            Circle().stroke(theme.accent.opacity(0.45), lineWidth: 1.5)
                        )
                    Image(systemName: "building.2.fill")
                        .font(.system(size: 28, weight: .semibold))
                        .foregroundStyle(theme.accent)
                }
                .frame(maxWidth: .infinity)

                Text(eyebrow.uppercased())
                    .font(.system(size: 11, weight: .bold))
                    .tracking(0.8)
                    .foregroundStyle(theme.accentSoft)
                Text(title)
                    .font(.system(size: 28, weight: .bold))
                    .foregroundStyle(BusinessTokens.Colors.textPrimary)
                Text(subtitle)
                    .font(.system(size: 14, weight: .medium))
                    .foregroundStyle(theme.hint)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .padding(20)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: 24, style: .continuous)
                    .fill(
                        LinearGradient(
                            colors: [theme.canvasTop.opacity(0.95), theme.surface.opacity(0.88)],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
            )
            .overlay(
                RoundedRectangle(cornerRadius: 24, style: .continuous)
                    .stroke(theme.accent.opacity(0.28), lineWidth: 1)
            )
        }
    }

    /// Figma `Kind of business` panel (`5:71178`) — equal tiles with glyph + border select.
    struct SubtypeChips: View {
        let theme: BusinessSetupTheme
        let chips: [BusinessSetupChip]
        let selected: String
        var onSelect: (String) -> Void

        var body: some View {
            VStack(alignment: .leading, spacing: 12) {
                Text("Kind of business")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(theme.hint)
                HStack(spacing: 6) {
                    ForEach(chips) { chip in
                        let isOn = chip.typeCode == selected
                        Button {
                            onSelect(chip.typeCode)
                        } label: {
                            VStack(spacing: 6) {
                                Text("◈")
                                    .font(.system(size: 20, weight: .semibold))
                                    .foregroundStyle(isOn ? BusinessTokens.Colors.textPrimary : theme.hint)
                                Text(chip.label)
                                    .font(.system(size: 11, weight: .semibold))
                                    .foregroundStyle(isOn ? BusinessTokens.Colors.textPrimary : theme.hint)
                                    .multilineTextAlignment(.center)
                                    .lineLimit(2)
                                    .minimumScaleFactor(0.85)
                            }
                            .frame(maxWidth: .infinity)
                            .frame(height: 80)
                            .padding(.horizontal, 4)
                            .background(theme.surface.opacity(0.95), in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                            .overlay(
                                RoundedRectangle(cornerRadius: 10, style: .continuous)
                                    .stroke(
                                        isOn ? theme.accent : theme.accent.opacity(0.22),
                                        lineWidth: isOn ? 1.5 : 1
                                    )
                            )
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            .padding(15)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(theme.surface.opacity(0.88), in: RoundedRectangle(cornerRadius: 18, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .stroke(theme.accent.opacity(0.18), lineWidth: 1)
            )
        }
    }

    struct SectionCard<Content: View>: View {
        let theme: BusinessSetupTheme
        let index: Int
        let title: String
        let subtitle: String?
        @ViewBuilder var content: () -> Content

        var body: some View {
            VStack(alignment: .leading, spacing: 14) {
                HStack(alignment: .firstTextBaseline, spacing: 10) {
                    Text(String(format: "%02d", index))
                        .font(.system(size: 13, weight: .heavy))
                        .foregroundStyle(theme.accent)
                    VStack(alignment: .leading, spacing: 4) {
                        Text(title)
                            .font(.system(size: 18, weight: .bold))
                            .foregroundStyle(BusinessTokens.Colors.textPrimary)
                        if let subtitle, !subtitle.isEmpty {
                            Text(subtitle)
                                .font(.system(size: 13, weight: .medium))
                                .foregroundStyle(theme.hint)
                        }
                    }
                }
                content()
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .fill(theme.surface.opacity(0.78))
            )
            .overlay(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .stroke(theme.accent.opacity(0.18), lineWidth: 1)
            )
        }
    }

    struct FieldRow: View {
        let theme: BusinessSetupTheme
        let label: String
        let placeholder: String
        @Binding var text: String
        var helper: String = ""
        var error: String? = nil
        var forceUppercase: Bool = false

        private var showError: Bool { BusinessSetupFieldChrome.hasError(error) }

        var body: some View {
            VStack(alignment: .leading, spacing: 6) {
                Text(label)
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(theme.label)
                TextField(placeholder, text: uppercaseBinding)
                    .font(.system(size: 15, weight: .medium))
                    .foregroundStyle(BusinessTokens.Colors.textPrimary)
                    .textInputAutocapitalization(forceUppercase ? .characters : .sentences)
                    .padding(.horizontal, 14)
                    .frame(height: 44)
                    .background(theme.inset, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: 12, style: .continuous)
                            .stroke(
                                showError ? BusinessTokens.Colors.signalDanger : theme.accent.opacity(0.12),
                                lineWidth: 1
                            )
                    )
                if showError, let error {
                    Text(error)
                        .font(.system(size: 11, weight: .medium))
                        .foregroundStyle(BusinessTokens.Colors.signalDanger)
                } else if !helper.isEmpty {
                    Text(helper)
                        .font(.system(size: 11, weight: .regular))
                        .foregroundStyle(theme.hint)
                }
            }
        }

        private var uppercaseBinding: Binding<String> {
            Binding(
                get: { text },
                set: { newValue in
                    text = forceUppercase
                        ? BusinessSetupDateFormatting.forceUppercaseInput(newValue)
                        : newValue
                }
            )
        }
    }

    struct PanelHeader: View {
        let theme: BusinessSetupTheme
        let title: String

        var body: some View {
            Text(title)
                .font(.system(size: 11, weight: .bold))
                .foregroundStyle(theme.accent)
                .padding(.top, 4)
        }
    }

    struct ChoiceChips: View {
        let theme: BusinessSetupTheme
        let options: [String]
        @Binding var selected: String

        var body: some View {
            LazyVGrid(columns: [GridItem(.adaptive(minimum: 96), spacing: 8)], alignment: .leading, spacing: 8) {
                ForEach(options, id: \.self) { option in
                    let isOn = option == selected
                    Button {
                        selected = option
                    } label: {
                        Text(option)
                            .font(.system(size: 12, weight: .bold))
                            .foregroundStyle(isOn ? theme.onAccent : theme.label)
                            .padding(.horizontal, 12)
                            .frame(height: 32)
                            .frame(maxWidth: .infinity)
                            .background(Capsule().fill(isOn ? theme.accent : theme.inset))
                            .overlay(
                                Capsule().stroke(theme.accent.opacity(isOn ? 0.5 : 0.2), lineWidth: 1)
                            )
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    struct MultiChoiceChips: View {
        let theme: BusinessSetupTheme
        let options: [String]
        @Binding var selectedPipe: String
        var error: String? = nil
        var helper: String = ""

        private var selected: Set<String> {
            Set(selectedPipe.split(separator: "|").map(String.init).filter { !$0.isEmpty })
        }

        private var showError: Bool { BusinessSetupFieldChrome.hasError(error) }

        var body: some View {
            VStack(alignment: .leading, spacing: 6) {
                LazyVGrid(columns: [GridItem(.adaptive(minimum: 96), spacing: 8)], alignment: .leading, spacing: 8) {
                    ForEach(options, id: \.self) { option in
                        let isOn = selected.contains(option)
                        Button {
                            var next = selected
                            if isOn { next.remove(option) } else { next.insert(option) }
                            selectedPipe = options.filter { next.contains($0) }.joined(separator: "|")
                        } label: {
                            Text(option)
                                .font(.system(size: 12, weight: .bold))
                                .foregroundStyle(isOn ? theme.onAccent : theme.label)
                                .padding(.horizontal, 12)
                                .frame(height: 32)
                                .frame(maxWidth: .infinity)
                                .background(Capsule().fill(isOn ? theme.accent : theme.inset))
                                .overlay(
                                    Capsule().stroke(theme.accent.opacity(isOn ? 0.5 : 0.2), lineWidth: 1)
                                )
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(showError ? 6 : 0)
                .overlay(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .stroke(showError ? BusinessTokens.Colors.signalDanger : Color.clear, lineWidth: 1)
                )
                if showError, let error {
                    Text(error)
                        .font(.system(size: 11, weight: .medium))
                        .foregroundStyle(BusinessTokens.Colors.signalDanger)
                } else if !helper.isEmpty {
                    Text(helper)
                        .font(.system(size: 11, weight: .regular))
                        .foregroundStyle(theme.hint)
                }
            }
        }
    }

    struct ToggleRow: View {
        let theme: BusinessSetupTheme
        let label: String
        @Binding var value: String

        private var isOn: Bool { value != "No" && value.lowercased() != "false" }

        var body: some View {
            HStack {
                Text(label)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(theme.label)
                Spacer()
                Button {
                    value = isOn ? "No" : "Yes"
                } label: {
                    Text(isOn ? "Yes" : "No")
                        .font(.system(size: 13, weight: .bold))
                        .foregroundStyle(isOn ? theme.onAccent : theme.label)
                        .padding(.horizontal, 16)
                        .frame(height: 34)
                        .background(Capsule().fill(isOn ? theme.accent : theme.inset))
                        .overlay(
                            Capsule().stroke(theme.accent.opacity(0.35), lineWidth: 1)
                        )
                }
                .buttonStyle(.plain)
            }
        }
    }

    struct DateRow: View {
        let theme: BusinessSetupTheme
        let label: String
        let placeholder: String
        @Binding var text: String
        var helper: String = ""
        var error: String? = nil
        var fieldKey: String = ""

        @State private var showPicker = false
        @State private var draftDate = Date()

        private var mode: BusinessSetupDateFormatting.Mode {
            BusinessSetupDateFormatting.modeForKey(fieldKey)
        }
        private var allowClear: Bool {
            BusinessSetupDateFormatting.allowClearForKey(fieldKey)
        }
        private var showError: Bool { BusinessSetupFieldChrome.hasError(error) }
        private var display: String {
            BusinessSetupDateFormatting.formatDisplay(text, mode: mode, placeholder: placeholder)
        }

        var body: some View {
            VStack(alignment: .leading, spacing: 6) {
                Text(label)
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(theme.label)
                Button {
                    draftDate = BusinessSetupDateFormatting.parseToDate(text) ?? Date()
                    showPicker = true
                } label: {
                    HStack {
                        Text(display)
                            .font(.system(size: 15, weight: .medium))
                            .foregroundStyle(
                                text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                                    ? theme.hint
                                    : BusinessTokens.Colors.textPrimary
                            )
                        Spacer()
                        Text("▼")
                            .font(.system(size: 12))
                            .foregroundStyle(theme.hint)
                    }
                    .padding(.horizontal, 14)
                    .frame(height: 44)
                    .background(theme.inset, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: 12, style: .continuous)
                            .stroke(
                                showError ? BusinessTokens.Colors.signalDanger : theme.accent.opacity(0.12),
                                lineWidth: 1
                            )
                    )
                }
                .buttonStyle(.plain)
                if showError, let error {
                    Text(error)
                        .font(.system(size: 11, weight: .medium))
                        .foregroundStyle(BusinessTokens.Colors.signalDanger)
                } else if !helper.isEmpty {
                    Text(helper)
                        .font(.system(size: 11, weight: .regular))
                        .foregroundStyle(theme.hint)
                }
            }
            .sheet(isPresented: $showPicker) {
                BusinessGlassSheetPanel {
                    NavigationStack {
                        DatePicker("", selection: $draftDate, displayedComponents: .date)
                            .datePickerStyle(.graphical)
                            .labelsHidden()
                            .padding()
                            .navigationTitle(label)
                            .navigationBarTitleDisplayMode(.inline)
                            .toolbar {
                                ToolbarItem(placement: .cancellationAction) {
                                    Button("Cancel") { showPicker = false }
                                }
                                if allowClear {
                                    ToolbarItem(placement: .destructiveAction) {
                                        Button("Clear") {
                                            text = ""
                                            showPicker = false
                                        }
                                    }
                                }
                                ToolbarItem(placement: .confirmationAction) {
                                    Button("OK") {
                                        text = BusinessSetupDateFormatting.toStorage(from: draftDate, mode: mode)
                                        showPicker = false
                                    }
                                }
                            }
                            .toolbarBackground(.hidden, for: .navigationBar)
                            .toolbarColorScheme(.dark, for: .navigationBar)
                    }
                    .scrollContentBackground(.hidden)
                    .toolbarBackground(.hidden, for: .navigationBar)
                    .background(Color.clear)
                }
                .businessGlassBottomSheet(detents: [.medium, .large])
                .preferredColorScheme(.dark)
            }
        }
    }

    struct CurrencyDropdown: View {
        let theme: BusinessSetupTheme
        let label: String
        let options: [String]
        @Binding var selected: String
        var helper: String = ""
        var error: String? = nil

        private var showError: Bool { BusinessSetupFieldChrome.hasError(error) }
        private var shown: String {
            selected.isEmpty ? (options.first ?? "") : selected
        }

        var body: some View {
            VStack(alignment: .leading, spacing: 6) {
                Text(label)
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(theme.label)
                Menu {
                    ForEach(options, id: \.self) { option in
                        Button(option) { selected = option }
                    }
                } label: {
                    HStack {
                        Text(shown.isEmpty ? "Select currency" : shown)
                            .font(.system(size: 15, weight: .medium))
                            .foregroundStyle(
                                shown.isEmpty ? theme.hint : BusinessTokens.Colors.textPrimary
                            )
                        Spacer()
                        Text("▼")
                            .font(.system(size: 12))
                            .foregroundStyle(theme.hint)
                    }
                    .padding(.horizontal, 14)
                    .frame(height: 44)
                    .background(theme.inset, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: 12, style: .continuous)
                            .stroke(
                                showError ? BusinessTokens.Colors.signalDanger : theme.accent.opacity(0.12),
                                lineWidth: 1
                            )
                    )
                }
                if showError, let error {
                    Text(error)
                        .font(.system(size: 11, weight: .medium))
                        .foregroundStyle(BusinessTokens.Colors.signalDanger)
                } else if !helper.isEmpty {
                    Text(helper)
                        .font(.system(size: 11, weight: .regular))
                        .foregroundStyle(theme.hint)
                }
            }
        }
    }

    struct RowAction: View {
        let theme: BusinessSetupTheme
        let label: String
        let action: String
        var onTap: () -> Void = {}

        var body: some View {
            Button(action: onTap) {
                HStack {
                    Text(label)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(BusinessTokens.Colors.textPrimary)
                    Spacer()
                    Text(action)
                        .font(.system(size: 13, weight: .bold))
                        .foregroundStyle(theme.onAccent)
                        .padding(.horizontal, 12)
                        .frame(height: 32)
                        .background(Capsule().fill(theme.accent))
                }
                .padding(.horizontal, 14)
                .frame(height: 48)
                .background(theme.inset, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .stroke(theme.accent.opacity(0.18), lineWidth: 1)
                )
            }
            .buttonStyle(.plain)
        }
    }

    struct MemberList: View {
        let theme: BusinessSetupTheme
        let members: [String]

        var body: some View {
            VStack(alignment: .leading, spacing: 8) {
                ForEach(members, id: \.self) { member in
                    HStack(spacing: 10) {
                        Circle()
                            .fill(theme.accent.opacity(0.35))
                            .frame(width: 36, height: 36)
                            .overlay(
                                Text(String(member.prefix(1)))
                                    .font(.system(size: 14, weight: .bold))
                                    .foregroundStyle(theme.onAccent)
                            )
                        Text(member)
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundStyle(BusinessTokens.Colors.textPrimary)
                        Spacer()
                    }
                    .padding(.horizontal, 12)
                    .frame(height: 48)
                    .background(theme.inset.opacity(0.9), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                }
            }
        }
    }

    struct ModuleList: View {
        let theme: BusinessSetupTheme
        let modules: [BusinessSetupModuleRow]
        var helper: String = ""

        var body: some View {
            VStack(alignment: .leading, spacing: 8) {
                ForEach(modules, id: \.title) { module in
                    let isAction = module.title.trimmingCharacters(in: .whitespaces).hasPrefix("+")
                    HStack(alignment: .center, spacing: 12) {
                        ZStack {
                            RoundedRectangle(cornerRadius: 10, style: .continuous)
                                .fill(theme.accent.opacity(0.28))
                                .frame(width: 36, height: 36)
                            Text(isAction ? "+" : "▣")
                                .font(.system(size: 14, weight: .bold))
                                .foregroundStyle(theme.accent)
                        }
                        VStack(alignment: .leading, spacing: 2) {
                            Text(module.title)
                                .font(.system(size: 14, weight: .semibold))
                                .foregroundStyle(isAction ? theme.accent : BusinessTokens.Colors.textPrimary)
                            if !module.subtitle.isEmpty {
                                Text(module.subtitle)
                                    .font(.system(size: 12, weight: .regular))
                                    .foregroundStyle(theme.hint)
                            }
                        }
                        Spacer(minLength: 0)
                        Text("›")
                            .font(.system(size: 18, weight: .medium))
                            .foregroundStyle(theme.accentSoft)
                    }
                    .padding(12)
                    .background(theme.inset, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: 14, style: .continuous)
                            .stroke(theme.accent.opacity(0.35), lineWidth: 1)
                    )
                }
                if !helper.isEmpty {
                    Text(helper)
                        .font(.system(size: 12, weight: .regular))
                        .foregroundStyle(theme.hint)
                }
            }
        }
    }

    struct ReviewSummary: View {
        let theme: BusinessSetupTheme
        let keys: [String]
        let values: [String: String]
        let labelsByKey: [String: String]

        var body: some View {
            VStack(alignment: .leading, spacing: 10) {
                ForEach(keys, id: \.self) { key in
                    let label = labelsByKey[key] ?? key
                    let raw = values[key]?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
                    let display = raw.isEmpty ? "—" : raw.replacingOccurrences(of: "|", with: ", ")
                    HStack(alignment: .firstTextBaseline) {
                        Text(label)
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundStyle(theme.hint)
                        Spacer()
                        Text(display)
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(BusinessTokens.Colors.textPrimary)
                            .multilineTextAlignment(.trailing)
                    }
                }
            }
            .padding(14)
            .background(theme.inset.opacity(0.85), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .stroke(theme.accent.opacity(0.2), lineWidth: 1)
            )
        }
    }

    struct StickyActivateBar: View {
        let theme: BusinessSetupTheme
        let label: String
        let enabled: Bool
        let submitting: Bool
        var onActivate: () -> Void
        var draftEnabled: Bool = false
        var savingDraft: Bool = false
        var onSaveDraft: (() -> Void)? = nil
        var onScheduleLater: (() -> Void)? = nil

        var body: some View {
            VStack(spacing: 0) {
                LinearGradient(
                    colors: [theme.canvas.opacity(0), theme.canvas],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .frame(height: 24)
                VStack(spacing: 8) {
                    if onSaveDraft != nil || onScheduleLater != nil {
                        HStack(spacing: 12) {
                            if let onSaveDraft {
                                Button(action: onSaveDraft) {
                                    HStack(spacing: 8) {
                                        if savingDraft {
                                            ProgressView().tint(theme.accent)
                                        }
                                        Text(savingDraft ? "Saving…" : "Save draft")
                                            .font(.system(size: 14, weight: .bold))
                                    }
                                    .foregroundStyle(theme.accent)
                                    .frame(maxWidth: .infinity, minHeight: 44)
                                    .background(theme.inset, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 14, style: .continuous)
                                            .stroke(theme.accent.opacity(0.35), lineWidth: 1)
                                    )
                                }
                                .buttonStyle(.plain)
                                .disabled(!draftEnabled || savingDraft || submitting)
                            }
                            if let onScheduleLater {
                                Button(action: onScheduleLater) {
                                    Text("Schedule later")
                                        .font(.system(size: 14, weight: .medium))
                                        .foregroundStyle(BusinessTokens.Colors.textSecondary)
                                        .frame(maxWidth: .infinity, minHeight: 44)
                                        .overlay(
                                            RoundedRectangle(cornerRadius: 14, style: .continuous)
                                                .stroke(theme.accent.opacity(0.25), lineWidth: 1)
                                        )
                                }
                                .buttonStyle(.plain)
                                .disabled(savingDraft || submitting)
                            }
                        }
                    }
                    Button(action: onActivate) {
                        HStack(spacing: 8) {
                            if submitting {
                                ProgressView().tint(theme.onAccent)
                            }
                            Text(submitting ? "Activating…" : label)
                                .font(.system(size: 15, weight: .bold))
                        }
                        .foregroundStyle(theme.onAccent)
                        .frame(maxWidth: .infinity, minHeight: 51)
                        .background(
                            LinearGradient(
                                colors: enabled && !submitting
                                    ? [theme.accent, theme.accentSoft]
                                    : [theme.accent.opacity(0.35), theme.accentSoft.opacity(0.25)],
                                startPoint: .leading,
                                endPoint: .trailing
                            ),
                            in: RoundedRectangle(cornerRadius: 16, style: .continuous)
                        )
                    }
                    .buttonStyle(.plain)
                    .disabled(!enabled || submitting || savingDraft)
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 16)
                .background(theme.canvas)
            }
        }
    }
}
