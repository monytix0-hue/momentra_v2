import SwiftUI

struct SetupWizardHeader: View {
    let title: String
    var durationLabel: String = "About 3 minutes"
    var onClose: () -> Void
    var onSummary: (() -> Void)? = nil
    var enabled: Bool = true
    var textColor: Color = SetupTokens.textPrimary
    var secondaryColor: Color = SetupTokens.textSecondary

    var body: some View {
        HStack(alignment: .center, spacing: 8) {
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(textColor)
                Text(durationLabel)
                    .font(.system(size: 12))
                    .foregroundStyle(secondaryColor)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            if let onSummary {
                Button(action: onSummary) {
                    VStack(spacing: 3) {
                        ForEach(0..<3, id: \.self) { _ in
                            RoundedRectangle(cornerRadius: 1)
                                .fill(textColor)
                                .frame(width: 14, height: 2)
                        }
                    }
                    .padding(8)
                }
                .buttonStyle(.plain)
                .disabled(!enabled)
                .accessibilityLabel("Summary")
            }

            Button(action: onClose) {
                Text("×")
                    .font(.system(size: 22, weight: .medium))
                    .foregroundStyle(textColor)
                    .padding(8)
            }
            .buttonStyle(.plain)
            .disabled(!enabled)
            .accessibilityLabel("Close setup")
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
    }
}
