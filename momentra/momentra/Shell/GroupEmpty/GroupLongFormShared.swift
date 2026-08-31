import SwiftUI

let groupLocalOnlyNote = "Saved on this device — syncs when Group preferences API ships"

func experienceChipLabel(_ opt: GroupTypeOption) -> String {
    switch opt.code {
    case "TRIP": return "Trip"
    case "WEDDING": return "Wedding"
    case "HOUSE_PARTY": return "Party"
    case "OFFICE_OUTING": return "Office"
    default: return String(opt.label.prefix(8))
    }
}

func purchaseChipLabel(_ opt: GroupTypeOption) -> String {
    switch opt.code {
    case "GIFT_POOL": return "Gift"
    case "GROUP_PURCHASE": return "Purchase"
    case "SHARED_ASSET": return "Asset"
    case "COMMUNITY_PURCHASE", "CUSTOM": return "Custom"
    default: return String(opt.label.prefix(8))
    }
}

func livingChipLabel(_ opt: GroupTypeOption) -> String {
    switch opt.code {
    case "FLATMATES": return "Flatmates"
    case "FAMILY_HOUSEHOLD": return "Family"
    case "CO_LIVING": return "Co-living"
    case "COMMUNITY_LIVING", "CUSTOM": return "Custom"
    default: return String(opt.label.prefix(8))
    }
}

struct GroupLongFormTypeChipStrip: View {
    let title: String
    let types: [GroupTypeOption]
    let selectedCode: String
    var shortLabel: (GroupTypeOption) -> String
    var onSelect: (GroupTypeOption) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title.uppercased())
                .font(.plusJakarta(size: 9, weight: .medium))
                .foregroundStyle(Color(hex: "#938EA1"))
                .tracking(0.7)
                .padding(.horizontal, 4)
            HStack(spacing: 6) {
                ForEach(types) { opt in
                    let selected = opt.code == selectedCode
                    let accent = GroupSetupTheme.palette(for: opt.code).accent
                    Button { onSelect(opt) } label: {
                        VStack(spacing: 4) {
                            Image(opt.iconName ?? "ges_type_trip")
                                .renderingMode(.template)
                                .resizable()
                                .scaledToFit()
                                .frame(width: 18, height: 18)
                                .foregroundStyle(selected ? accent : GroupSetupTheme.textSecondary)
                            Text(shortLabel(opt))
                                .font(.plusJakarta(size: 8.5, weight: .medium))
                                .foregroundStyle(selected ? accent : GroupSetupTheme.textSecondary)
                                .lineLimit(1)
                        }
                        .frame(maxWidth: .infinity, minHeight: 56)
                        .background(
                            (selected ? accent.opacity(0.16) : Color(hex: "#1E293B")),
                            in: RoundedRectangle(cornerRadius: 14)
                        )
                        .overlay(
                            RoundedRectangle(cornerRadius: 14)
                                .stroke(selected ? accent.opacity(0.6) : Color(hex: "#3A3842").opacity(0.8), lineWidth: 1)
                        )
                    }
                    .buttonStyle(.plain)
                }
            }
        }
        .padding(8)
        .background(Color(hex: "#161B26"), in: RoundedRectangle(cornerRadius: 18))
        .overlay(RoundedRectangle(cornerRadius: 18).stroke(Color(hex: "#3A3842").opacity(0.75), lineWidth: 1))
    }
}

struct GroupLongFormDiamondDivider: View {
    var body: some View {
        HStack(spacing: 12) {
            Rectangle().fill(Color(hex: "#3A3842")).frame(height: 1)
            RoundedRectangle(cornerRadius: 1).fill(Color(hex: "#C9BFFF")).frame(width: 8, height: 8)
            Rectangle().fill(Color(hex: "#3A3842")).frame(height: 1)
        }
        .padding(.vertical, 16)
    }
}

struct GroupLongFormSectionCard<Content: View>: View {
    let step: String
    let title: String
    let accent: Color
    @ViewBuilder var content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            HStack(spacing: 12) {
                Text(step)
                    .font(.plusJakarta(size: 64, weight: .heavy))
                    .foregroundStyle(accent.opacity(0.06))
                Text(title.uppercased())
                    .font(.plusJakarta(size: 11, weight: .semibold))
                    .foregroundStyle(accent)
            }
            content()
        }
        .padding(24)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(hex: "#3A3842"), in: RoundedRectangle(cornerRadius: 16))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color(hex: "#938EA1").opacity(0.35), lineWidth: 1))
    }
}

let groupDestinationSuggestions = ["Goa, India", "Jaipur", "Manali", "Singapore"]

struct GroupLongFormDestinationField: View {
    let label: String
    let hint: String
    @Binding var value: String
    var placeholder: String = "City, country or venue"
    var suggestions: [String] = groupDestinationSuggestions
    var testTag: String = "setup.field.destination"

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(label)
                .font(.plusJakarta(size: 14, weight: .medium))
                .foregroundStyle(GroupSetupTheme.textSecondary)
            Text(hint)
                .font(.plusJakarta(size: 12))
                .foregroundStyle(GroupSetupTheme.textSecondary.opacity(0.85))
            TextField(placeholder, text: $value)
                .font(.plusJakarta(size: 16, weight: .semibold))
                .foregroundStyle(Color(hex: "#E5E0EE"))
                .padding(.horizontal, 12)
                .padding(.vertical, 10)
                .background(Color(hex: "#161B26"), in: RoundedRectangle(cornerRadius: 8))
                .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color(hex: "#1E293B"), lineWidth: 1))
                .accessibilityIdentifier(testTag)
                .onChange(of: value) { _, newValue in
                    if newValue.count > 200 {
                        value = String(newValue.prefix(200))
                    }
                }
            if !suggestions.isEmpty {
                FlowLayout(spacing: 8) {
                    ForEach(suggestions, id: \.self) { suggestion in
                        let selected = value.caseInsensitiveCompare(suggestion) == .orderedSame
                        Button {
                            value = suggestion
                        } label: {
                            Text(suggestion)
                                .font(.plusJakarta(size: 12, weight: selected ? .semibold : .regular))
                                .foregroundStyle(selected ? Color(hex: "#C9BFFF") : GroupSetupTheme.textSecondary)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 6)
                                .background(
                                    (selected ? Color(hex: "#2D2640") : Color(hex: "#161B26")),
                                    in: Capsule()
                                )
                                .overlay(
                                    Capsule().stroke(selected ? Color(hex: "#7C5CFC") : Color(hex: "#1E293B"), lineWidth: 1)
                                )
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
        .padding(.vertical, 8)
    }
}

struct GroupBudgetCustomField: View {
    @Binding var value: String
    let currencyCode: String
    var testTag: String = "setup.field.budgetCustom"

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("Custom amount (\(currencyCode))")
                .font(.plusJakarta(size: 12))
                .foregroundStyle(GroupSetupTheme.textSecondary)
            TextField("84,000", text: $value)
                .font(.plusJakarta(size: 16, weight: .semibold))
                .foregroundStyle(Color(hex: "#E5E0EE"))
                .keyboardType(.numberPad)
                .padding(.horizontal, 12)
                .padding(.vertical, 10)
                .background(Color(hex: "#161B26"))
                .clipShape(RoundedRectangle(cornerRadius: 8))
                .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color(hex: "#1E293B")))
                .accessibilityIdentifier(testTag)
                .onChange(of: value) { _, newValue in
                    let formatted = GroupBudgetUtils.formatCustomAmountInput(newValue)
                    if formatted != newValue {
                        value = formatted
                    }
                }
        }
    }
}

struct GroupLongFormPrefRow: View {
    let label: String
    let hint: String
    let value: String
    let options: [String]
    let onValueChange: (String) -> Void
    var editableGlyph: Bool = false
    var testTag: String = "setup.dropdown"

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(label).font(.plusJakarta(size: 14, weight: .medium)).foregroundStyle(GroupSetupTheme.textSecondary)
                Text(hint).font(.plusJakarta(size: 12)).foregroundStyle(GroupSetupTheme.textSecondary.opacity(0.85))
            }
            Spacer(minLength: 8)
            Menu {
                ForEach(options, id: \.self) { option in
                    Button(option) { onValueChange(option) }
                }
            } label: {
                HStack(spacing: 8) {
                    Text(value).font(.plusJakarta(size: 13, weight: .semibold)).foregroundStyle(Color(hex: "#E5E0EE")).lineLimit(1)
                    Text(editableGlyph ? "✎" : "▼").font(.plusJakarta(size: 10, weight: .semibold)).foregroundStyle(GroupSetupTheme.textSecondary)
                }
                .padding(.horizontal, 12).padding(.vertical, 6)
                .background(Color(hex: "#161B26"), in: RoundedRectangle(cornerRadius: 8))
                .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color(hex: "#1E293B"), lineWidth: 1))
            }
            .accessibilityIdentifier("\(testTag).trigger")
        }
        .padding(.vertical, 14)
        .accessibilityIdentifier(testTag)
    }
}

struct GroupLongFormToggleRow: View {
    let title: String
    let subtitle: String
    @Binding var checked: Bool
    let accent: Color

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(title).font(.plusJakarta(size: 14, weight: .medium)).foregroundStyle(GroupSetupTheme.textSecondary)
                Text(subtitle).font(.plusJakarta(size: 12)).foregroundStyle(GroupSetupTheme.textSecondary.opacity(0.85))
            }
            Spacer()
            Toggle("", isOn: $checked).labelsHidden().tint(accent)
        }
        .padding(.vertical, 14)
    }
}

struct GroupLongFormNamePills: View {
    let primary: String
    let secondary: String
    let accent: Color

    var body: some View {
        HStack(spacing: 8) {
            ForEach([primary, secondary], id: \.self) { text in
                HStack(spacing: 8) {
                    Circle().fill(accent).frame(width: 6, height: 6)
                    Text(text).font(.plusJakarta(size: 14, weight: .medium)).foregroundStyle(Color(hex: "#E5E0EE")).lineLimit(1)
                    Text("▼").font(.plusJakarta(size: 12, weight: .semibold)).foregroundStyle(GroupSetupTheme.textSecondary)
                }
                .padding(.horizontal, 16).padding(.vertical, 10)
                .background(Color(hex: "#161B26"), in: Capsule())
                .overlay(Capsule().stroke(Color(hex: "#1E293B"), lineWidth: 1))
            }
        }
    }
}

struct GroupLongFormLocalOnlyNote: View {
    var body: some View {
        Text(groupLocalOnlyNote)
            .font(.plusJakarta(size: 11))
            .foregroundStyle(GroupSetupTheme.textSecondary)
            .frame(maxWidth: .infinity, alignment: .leading)
    }
}

struct GroupLongFormReadyBanner: View {
    let message: String

    var body: some View {
        HStack(spacing: 10) {
            Text("✓").font(.plusJakarta(size: 14, weight: .bold)).foregroundStyle(Color(hex: "#10B981"))
            Text(message).font(.plusJakarta(size: 13, weight: .semibold)).foregroundStyle(Color(hex: "#E5E0EE"))
        }
        .padding(.horizontal, 16).padding(.vertical, 12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(hex: "#10B981").opacity(0.15), in: Capsule())
        .overlay(Capsule().stroke(Color(hex: "#10B981").opacity(0.35), lineWidth: 1))
    }
}
