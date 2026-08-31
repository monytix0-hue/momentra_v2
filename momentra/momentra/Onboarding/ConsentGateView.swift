import SwiftUI

/// Minimal consent gate (FIGMA_GAP) before login — grant/withdraw refined in Account hub.
struct ConsentGateView: View {
    var onContinue: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Privacy & consent")
                .font(.system(size: 22, weight: .bold))
                .foregroundStyle(Color.white)
            Text(
                "Momentra uses account data to run your moments. You can grant or withdraw analytics and AI consents anytime in Account → Privacy."
            )
            .font(.system(size: 14))
            .foregroundStyle(Color.white.opacity(0.75))
            Button("Continue", action: onContinue)
                .buttonStyle(BrandPrimaryButtonStyle())
                .padding(.top, 8)
                .accessibilityIdentifier("consent.continue")
        }
        .padding(24)
        .brandAuthScreen()
        .accessibilityIdentifier("consent.gate")
    }
}
