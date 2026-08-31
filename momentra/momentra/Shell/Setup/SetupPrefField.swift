import SwiftUI

struct SetupPrefField: View {
    let field: PersonalSetupFieldSpec
    @Binding var selections: [String: Any]
    var selectedChipColor: Color = SetupTokens.bizAccent

    var body: some View {
        switch field.kind {
        case .chips, .dropdown:
            SetupDropdownField(
                label: field.label,
                options: field.options,
                value: binding(for: field.key),
                accent: selectedChipColor,
                testTag: "setup.dropdown.\(field.key)"
            )
        case .text:
            VStack(alignment: .leading, spacing: 8) {
                Text(field.label)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(Color(hex: "#E2E8F0"))
                TextField(
                    field.label,
                    text: binding(for: field.key)
                )
                .textFieldStyle(.plain)
                .font(.system(size: 15))
                .foregroundStyle(.white)
                .padding(12)
                .background(Color(hex: "#0F172A"), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .stroke(Color(hex: "#1E293B"), lineWidth: 1)
                )
                .accessibilityIdentifier("setup.text.\(field.key)")
            }
        case .toggle:
            Toggle(field.label, isOn: boolBinding(for: field.key))
                .tint(selectedChipColor)
        case .date:
            SetupDateField(
                label: field.label,
                isoValue: optionalStringBinding(for: field.key),
                testTag: "setup.date.\(field.key)"
            )
        case .dateTime:
            SetupDateTimeField(
                label: field.label,
                isoValue: optionalStringBinding(for: field.key),
                testTag: "setup.datetime.\(field.key)"
            )
        case .time:
            SetupDateTimeField(
                label: field.label,
                isoValue: optionalTimeIsoBinding(for: field.key),
                testTag: "setup.time.\(field.key)"
            )
        case .title:
            EmptyView()
        }
    }

    private func binding(for key: String) -> Binding<String> {
        Binding(
            get: { selections[key] as? String ?? "" },
            set: { selections[key] = $0 }
        )
    }

    private func boolBinding(for key: String) -> Binding<Bool> {
        Binding(
            get: { selections[key] as? Bool ?? false },
            set: { selections[key] = $0 }
        )
    }

    private func optionalStringBinding(for key: String) -> Binding<String?> {
        Binding(
            get: { selections[key] as? String },
            set: { selections[key] = $0 }
        )
    }

    private func optionalTimeIsoBinding(for key: String) -> Binding<String?> {
        Binding(
            get: {
                let raw = selections[key] as? String
                guard let raw, !raw.isEmpty else { return nil }
                return raw.contains("T") ? raw : "1970-01-01T\(raw)"
            },
            set: { newValue in
                guard let newValue else {
                    selections[key] = nil
                    return
                }
                selections[key] = newValue.components(separatedBy: "T").last ?? newValue
            }
        )
    }
}
