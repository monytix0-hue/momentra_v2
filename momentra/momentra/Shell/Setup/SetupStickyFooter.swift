import SwiftUI

struct SetupStickyFooter: View {
    let tagline: String
    let ctaLabel: String
    let onCta: () -> Void
    var submitting: Bool = false
    var saved: Bool = true
    var onPreview: (() -> Void)? = nil
    var onSaveDraft: (() -> Void)? = nil
    var accentGradient: LinearGradient = SetupTokens.personalCtaGradient
    var backgroundColor: Color = SetupTokens.bgPrimary
    var ctaTextColor: Color = .white

    var body: some View {
        VStack(spacing: 10) {
            Text(tagline.uppercased())
                .font(.system(size: 10, weight: .bold))
                .kerning(2)
                .foregroundStyle(SetupTokens.brandPrimary)
                .frame(maxWidth: .infinity)

            HStack {
                if saved {
                    HStack(spacing: 6) {
                        Text("✓")
                            .font(.system(size: 14))
                            .foregroundStyle(SetupTokens.savedGreen)
                        Text("Saved")
                            .font(.system(size: 12))
                            .foregroundStyle(SetupTokens.textSecondary)
                    }
                }
                Spacer()
                if let onPreview {
                    Button(action: onPreview) {
                        Text("Preview")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(SetupTokens.brandPrimary)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel("Preview setup")
                }
            }

            Button(action: onCta) {
                ZStack {
                    if submitting {
                        ProgressView()
                            .tint(ctaTextColor)
                    } else {
                        Text(ctaLabel)
                            .font(.system(size: 16, weight: .heavy))
                            .foregroundStyle(ctaTextColor)
                    }
                }
                .frame(maxWidth: .infinity)
                .frame(height: 52)
                .background(accentGradient, in: Capsule())
            }
            .buttonStyle(.plain)
            .disabled(submitting)
            .accessibilityLabel(ctaLabel)

            if let onSaveDraft {
                Button(action: onSaveDraft) {
                    Text("Save draft")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(SetupTokens.textPrimary)
                        .frame(maxWidth: .infinity)
                        .frame(height: 48)
                        .overlay(RoundedRectangle(cornerRadius: 14).stroke(Color(hex: "#1E293B")))
                }
                .buttonStyle(.plain)
                .disabled(submitting)
                .accessibilityLabel("Save draft")
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(backgroundColor)
    }
}
