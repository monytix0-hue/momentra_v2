import SwiftUI

struct SetupTitleField: View {
    let label: String
    @Binding var value: String
    let placeholder: String
    var testTag: String = "setup.title"

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(label)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(SetupTokens.textPrimary)
            TextField(placeholder, text: $value)
                .textFieldStyle(.plain)
                .font(.system(size: 16))
                .foregroundStyle(.white)
                .padding(.horizontal, 14)
                .padding(.vertical, 12)
                .background(SetupTokens.bizCard, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .stroke(Color(hex: "#1E293B"), lineWidth: 1)
                )
                .accessibilityIdentifier(testTag)
                .onChange(of: value) { _, newValue in
                    if newValue.count > 500 {
                        value = String(newValue.prefix(500))
                    }
                }
        }
    }
}
