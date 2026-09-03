import SwiftUI

struct SetupDropdownField: View {
    let label: String
    var hint: String? = nil
    let options: [String]
    @Binding var value: String
    var accent: Color = SetupTokens.brandPrimary
    var testTag: String = "setup.dropdown"

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            VStack(alignment: .leading, spacing: 4) {
                Text(label)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(SetupTokens.textPrimary)
                if let hint {
                    Text(hint)
                        .font(.system(size: 12))
                        .foregroundStyle(SetupTokens.textSecondary)
                }
            }
            HStack {
                Spacer()
                Menu {
                    ForEach(options, id: \.self) { option in
                        Button(option) { value = option }
                    }
                } label: {
                    HStack(spacing: 8) {
                        Text(value.isEmpty ? "Select" : value)
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundStyle(value.isEmpty ? SetupTokens.textSecondary : .white)
                        Text("▼")
                            .font(.system(size: 12))
                            .foregroundStyle(SetupTokens.textSecondary)
                    }
                    .frame(maxWidth: .infinity, alignment: .trailing)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 12)
                    .background(SetupTokens.bizCard, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: 12, style: .continuous)
                            .stroke(Color(hex: "#1E293B"), lineWidth: 1)
                    )
                }
                .accessibilityIdentifier("\(testTag).trigger")
            }
        }
        .padding(.vertical, 6)
        .accessibilityIdentifier(testTag)
    }
}
