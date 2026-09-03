import SwiftUI

/// Shared long-form Personal setup primitives (sheet-safe, body-only).
enum PersonalSetupLongForm {
    static let card = Color(hex: "#161B26")
    static let border = Color(hex: "#1E293B")
    static let surfaceDeep = Color(hex: "#0F172A")
    static let teal = Color(hex: "#10B981")
    static let blue = Color(hex: "#3B82F6")
    static let indigo = Color(hex: "#818CF8")
    static let amber = Color(hex: "#F59E0B")
    static let orange = Color(hex: "#FF7A3D")
    static let pink = Color(hex: "#E12A9E")
}

struct PersonalSetupCloseRow: View {
    var onBack: () -> Void
    var enabled: Bool = true

    var body: some View {
        HStack {
            Button(action: onBack) {
                HStack(spacing: 6) {
                    Text("×")
                    Text("Close")
                }
                .foregroundStyle(SetupTokens.textSecondary)
            }
            .buttonStyle(.plain)
            .disabled(!enabled)
            Spacer()
            Text("PERSONAL MODE")
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(SetupTokens.textSecondary)
        }
    }
}

struct PersonalSetupHeroBlock: View {
    let emoji: String
    let title: String
    let subtitle: String
    var accent: Color = SetupTokens.accentPurple
    /// Optional asset name under Assets.xcassets; falls back to `emoji` when nil/missing.
    var imageAssetName: String? = nil

    var body: some View {
        VStack(spacing: 28) {
            ZStack {
                // Figma glow-container: ~280pt radial glow behind 112pt icon
                Circle()
                    .fill(
                        RadialGradient(
                            colors: [
                                accent.opacity(0.40),
                                accent.opacity(0.18),
                                accent.opacity(0.06),
                                Color.clear,
                            ],
                            center: .center,
                            startRadius: 12,
                            endRadius: 140
                        )
                    )
                    .frame(width: 280, height: 280)
                    .blur(radius: 18)

                Group {
                    if let imageAssetName, !imageAssetName.isEmpty {
                        Image(imageAssetName)
                            .resizable()
                            .scaledToFit()
                            .padding(28)
                    } else {
                        Text(emoji)
                            .font(.system(size: 40))
                    }
                }
                .frame(width: 112, height: 112)
                .background(PersonalSetupLongForm.card)
                .overlay(RoundedRectangle(cornerRadius: 56).stroke(accent.opacity(0.2)))
                .clipShape(RoundedRectangle(cornerRadius: 56))
                .shadow(color: accent.opacity(0.35), radius: 20, y: 0)
            }
            .frame(width: 280, height: 200)

            Text(title)
                .font(.system(size: 28, weight: .bold))
                .foregroundStyle(SetupTokens.textPrimary)
                .multilineTextAlignment(.center)
            Text(subtitle)
                .font(.system(size: 14))
                .foregroundStyle(SetupTokens.textSecondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 32)
    }
}

struct PersonalSetupCategoryTabItem: Identifiable {
    var id: String { label }
    let label: String
    var icon: String = ""
    let accents: [Color]
}

struct PersonalSetupCategoryTabs: View {
    let tabs: [PersonalSetupCategoryTabItem]
    let selected: String
    let onSelect: (String) -> Void

    var body: some View {
        HStack(spacing: 4) {
            ForEach(tabs) { tab in
                let isSelected = selected == tab.label
                HStack(spacing: 2) {
                    if !tab.icon.isEmpty {
                        Text(tab.icon).font(.system(size: 11))
                    }
                    Text(tab.label)
                        .font(.system(size: 10, weight: .bold))
                        .foregroundStyle(.white)
                        .lineLimit(1)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 10)
                .background(
                    Group {
                        if isSelected {
                            LinearGradient(
                                colors: tab.accents.count >= 2
                                    ? tab.accents
                                    : [tab.accents.first ?? SetupTokens.accentPurple, tab.accents.first ?? SetupTokens.accentPurple],
                                startPoint: .leading,
                                endPoint: .trailing
                            )
                        } else {
                            Color.clear
                        }
                    }
                )
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .contentShape(Rectangle())
                .onTapGesture { onSelect(tab.label) }
            }
        }
        .padding(4)
        .background(PersonalSetupLongForm.card)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(PersonalSetupLongForm.border))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}

/// Live status: answers = keys that differ from default or are non-blank; sections = key-sets with ≥1 answered.
func personalSetupStatusCounts(
    defaults: [String: Any],
    selections: [String: Any],
    sectionKeys: [[String]]
) -> (sections: Int, answers: Int) {
    func isNonBlank(_ value: Any?) -> Bool {
        guard let value else { return false }
        if let s = value as? String { return !s.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
        if let list = value as? [Any] { return !list.isEmpty }
        if value is Bool { return true }
        return true
    }

    func normalized(_ value: Any?) -> String {
        guard let value else { return "" }
        if let s = value as? String { return s }
        if let b = value as? Bool { return b ? "true" : "false" }
        if let list = value as? [String] { return list.sorted().joined(separator: ",") }
        return "\(value)"
    }

    func isAnswered(_ key: String) -> Bool {
        let sel = selections[key]
        let def = defaults[key]
        let differs = normalized(sel) != normalized(def)
        return differs || isNonBlank(sel)
    }

    let allKeys = Array(Set(sectionKeys.flatMap { $0 }))
    let answers = allKeys.filter(isAnswered).count
    let sections = sectionKeys.filter { keys in keys.contains(where: isAnswered) }.count
    return (sections, answers)
}

func personalSetupStatusLine(
    defaults: [String: Any],
    selections: [String: Any],
    sectionKeys: [[String]]
) -> String {
    let counts = personalSetupStatusCounts(defaults: defaults, selections: selections, sectionKeys: sectionKeys)
    return "\(counts.sections) sections configured • \(counts.answers) answers saved"
}

struct PersonalSetupSectionCard<Content: View>: View {
    let number: String
    let title: String
    var accent: Color = SetupTokens.accentPurple
    @ViewBuilder var content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            // Figma: watermark "01" + title side-by-side (gap 12), title has purple glow — not overlay
            HStack(alignment: .center, spacing: 12) {
                Text(number)
                    .font(.system(size: 64, weight: .heavy))
                    .foregroundStyle(accent.opacity(0.08))
                Text(title.uppercased())
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(accent)
                    .shadow(color: accent.opacity(0.95), radius: 8)
                    .shadow(color: accent.opacity(0.65), radius: 16)
                    .shadow(color: accent.opacity(0.35), radius: 24)
            }
            .padding(.bottom, 8)
            content()
        }
        .padding(24)
        .frame(maxWidth: .infinity, alignment: .leading)
        // Shape via background/stroke only — avoid clip so title glow isn't cut off
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(PersonalSetupLongForm.card)
        )
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(PersonalSetupLongForm.border))
    }
}

struct PersonalSetupInlineDropdown: View {
    let label: String
    let hint: String
    let key: String
    let options: [String]
    @Binding var selections: [String: Any]
    var accent: Color = SetupTokens.accentPurple

    var body: some View {
        SetupDropdownField(
            label: label,
            hint: hint,
            options: options,
            value: Binding(
                get: { selections[key] as? String ?? options.first ?? "" },
                set: { selections[key] = $0 }
            ),
            accent: accent,
            testTag: "setup.dropdown.\(key)"
        )
    }
}

struct PersonalSetupMultiSelect: View {
    let label: String
    let hint: String
    let key: String
    let options: [String]
    @Binding var selections: [String: Any]
    var accent: Color = SetupTokens.accentPurple

    private var selected: Set<String> {
        if let list = selections[key] as? [String] { return Set(list) }
        if let s = selections[key] as? String { return [s] }
        return []
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(label).font(.system(size: 14)).foregroundStyle(SetupTokens.textSecondary)
            Text(hint).font(.system(size: 12)).foregroundStyle(SetupTokens.textSecondary)
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 8) {
                ForEach(options, id: \.self) { option in
                    let on = selected.contains(option)
                    Text(option)
                        .font(.system(size: 13))
                        .foregroundStyle(SetupTokens.textPrimary)
                        .frame(maxWidth: .infinity)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 10)
                        .background(PersonalSetupLongForm.card)
                        .overlay(
                            RoundedRectangle(cornerRadius: 20)
                                .stroke(on ? accent : PersonalSetupLongForm.border)
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 20))
                        .onTapGesture {
                            var next = selected
                            if on { next.remove(option) } else { next.insert(option) }
                            selections[key] = Array(next)
                        }
                }
            }
        }
        .padding(.vertical, 6)
    }
}

struct PersonalSetupActivateBlock: View {
    let statusLine: String
    let readyLine: String
    let ctaLabel: String
    let footerTagline: String
    var ctaGradient: LinearGradient = SetupTokens.personalCtaGradient
    let submitting: Bool
    let error: String?
    let onActivate: () -> Void

    var body: some View {
        VStack(spacing: 12) {
            Text(statusLine)
                .font(.system(size: 14))
                .foregroundStyle(SetupTokens.textSecondary)
                .frame(maxWidth: .infinity)
            HStack(spacing: 8) {
                Text("✓").foregroundStyle(SetupTokens.savedGreen)
                Text(readyLine)
                    .font(.system(size: 14))
                    .foregroundStyle(SetupTokens.textSecondary)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(SetupTokens.savedGreen.opacity(0.08))
            .overlay(Capsule().stroke(SetupTokens.savedGreen.opacity(0.2)))
            .clipShape(Capsule())

            if let error {
                Text(error).font(.system(size: 13)).foregroundStyle(SetupTokens.error)
            }

            Button(action: onActivate) {
                Text(submitting ? "Activating…" : ctaLabel)
                    .font(.system(size: 16, weight: .bold))
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 56)
                    .background(ctaGradient, in: RoundedRectangle(cornerRadius: 14))
            }
            .buttonStyle(.plain)
            .disabled(submitting)

            Text(footerTagline)
                .font(.system(size: 11))
                .foregroundStyle(SetupTokens.textSecondary)
                .frame(maxWidth: .infinity)
        }
        .padding(.top, 24)
    }
}

func personalSelectionString(_ selections: [String: Any], _ key: String) -> String {
    selections[key] as? String ?? "\(selections[key] ?? "")"
}

func personalSelectionList(_ selections: [String: Any], _ key: String) -> [String] {
    if let list = selections[key] as? [String] { return list }
    if let s = selections[key] as? String { return [s] }
    return []
}

struct PersonalSetupStageHeader: View {
    let label: String
    let key: String
    let options: [String]
    @Binding var selections: [String: Any]

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(label.uppercased())
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(SetupTokens.textSecondary)
            Text(personalSelectionString(selections, key))
                .font(.system(size: 22, weight: .bold))
                .foregroundStyle(SetupTokens.textPrimary)
            SetupDropdownField(
                label: label,
                options: options,
                value: Binding(
                    get: { personalSelectionString(selections, key) },
                    set: { selections[key] = $0 }
                ),
                testTag: "setup.dropdown.\(key)"
            )
        }
    }
}

struct PersonalSetupDualPills: View {
    let label: String
    let key: String
    let options: [String]
    @Binding var selections: [String: Any]
    var accent: Color = SetupTokens.accentPurple

    var body: some View {
        SetupDropdownField(
            label: label,
            options: options,
            value: Binding(
                get: { personalSelectionString(selections, key) },
                set: { selections[key] = $0 }
            ),
            accent: accent,
            testTag: "setup.dropdown.\(key)"
        )
    }
}

struct PersonalSetupAddRow: View {
    let label: String
    var accent: Color = SetupTokens.accentPurple
    var onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            Text(label)
                .font(.system(size: 13, weight: .bold))
                .foregroundStyle(accent)
                .frame(maxWidth: .infinity)
                .padding(12)
                .overlay(RoundedRectangle(cornerRadius: 10).stroke(accent.opacity(0.4)))
        }
        .buttonStyle(.plain)
    }
}

struct PersonalSetupDiamondDivider: View {
    var accent: Color = SetupTokens.accentPurple

    var body: some View {
        HStack(spacing: 12) {
            Rectangle().fill(PersonalSetupLongForm.border).frame(height: 1)
            Rectangle().fill(accent).frame(width: 8, height: 8)
            Rectangle().fill(PersonalSetupLongForm.border).frame(height: 1)
        }
        .padding(.vertical, 12)
    }
}

struct PersonalSetupSummaryProgress: View {
    var accent: Color = SetupTokens.accentPurple

    var body: some View {
        VStack(spacing: 8) {
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule().fill(Color(hex: "#1E293B")).frame(height: 4)
                    Capsule().fill(accent).frame(width: geo.size.width * 0.62, height: 4)
                }
            }
            .frame(height: 4)
            HStack {
                Text("TODAY").font(.system(size: 10, weight: .semibold)).foregroundStyle(SetupTokens.textSecondary)
                Spacer()
                Text("FORWARD").font(.system(size: 10, weight: .semibold)).foregroundStyle(SetupTokens.textSecondary)
            }
        }
    }
}

struct PersonalSetupBorderedGroup<Content: View>: View {
    let title: String
    var border: Color
    var glyph: String = "●"
    @ViewBuilder var content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 12) {
                Circle().fill(border).frame(width: 32, height: 32)
                    .overlay(Text(glyph).font(.system(size: 11, weight: .bold)).foregroundStyle(.white))
                Text(title)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(SetupTokens.textPrimary)
            }
            content()
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(PersonalSetupLongForm.surfaceDeep)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(border))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}

