import SwiftUI
import UIKit

/// Consent + age gate before login — grant/withdraw refined in Account hub.
struct ConsentGateView: View {
    var onContinue: () -> Void

    @State private var confirmedAge13Plus = OnboardingPrefs.isAgeGateAccepted

    var body: some View {
        Form {
            Section {
                Text("Privacy & consent")
                    .font(.system(size: 22, weight: .bold))
                    .foregroundStyle(Color.white)
                    .listRowBackground(Color.clear)
                Text(
                    "Momentra uses your account and moment data to run the product. You can manage these purposes anytime in Account → Privacy:"
                )
                .font(.system(size: 14))
                .foregroundStyle(Color.white.opacity(0.75))
                .listRowBackground(Color.clear)
                VStack(alignment: .leading, spacing: 6) {
                    purposeBullet("Account & moments — operate your Personal, Group, and Business moments")
                    purposeBullet("Analytics — optional product analytics (Personal / Business)")
                    purposeBullet("AI insights — optional AI suggestions and recommendations")
                    purposeBullet("Memory patterns — optional pattern analysis on your memories")
                }
                .listRowBackground(Color.clear)
                Text("By continuing you agree to our Privacy Policy and Terms of Service.")
                    .font(.system(size: 13))
                    .foregroundStyle(Color.white.opacity(0.75))
                    .listRowBackground(Color.clear)
                Button("Privacy Policy") {
                    if let url = URL(string: "https://momentra.tech/privacy") {
                        UIApplication.shared.open(url)
                    }
                }
                .listRowBackground(Color.clear)
                Button("Terms of Service") {
                    if let url = URL(string: "https://momentra.tech/terms") {
                        UIApplication.shared.open(url)
                    }
                }
                .listRowBackground(Color.clear)
                Toggle(isOn: $confirmedAge13Plus) {
                    Text("I confirm I am 13 years of age or older")
                        .font(.system(size: 14))
                        .foregroundStyle(Color.white)
                }
                .tint(Color.accentColor)
                .listRowBackground(Color.clear)
                .accessibilityIdentifier("consent.age_gate")
            }
        }
        .scrollContentBackground(.hidden)
        .safeAreaInset(edge: .bottom) {
            Button("Continue") {
                OnboardingPrefs.markAgeGateAccepted()
                onContinue()
            }
            .buttonStyle(BrandPrimaryButtonStyle())
            .disabled(!confirmedAge13Plus)
            .opacity(confirmedAge13Plus ? 1 : 0.45)
            .padding(.horizontal, 24)
            .padding(.bottom, 16)
            .accessibilityIdentifier("consent.continue")
        }
        .brandAuthScreen()
        .accessibilityIdentifier("consent.gate")
    }

    private func purposeBullet(_ text: String) -> some View {
        HStack(alignment: .top, spacing: 8) {
            Text("•")
                .foregroundStyle(Color.white.opacity(0.75))
            Text(text)
                .font(.system(size: 13))
                .foregroundStyle(Color.white.opacity(0.75))
                .fixedSize(horizontal: false, vertical: true)
        }
    }
}
