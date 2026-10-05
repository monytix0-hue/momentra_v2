import SwiftUI
import UIKit

/// Minimal consent gate before login — grant/withdraw refined in Account hub.
struct ConsentGateView: View {
    var onContinue: () -> Void

    var body: some View {
        Form {
            Section {
                Text("Privacy & consent")
                    .font(.system(size: 22, weight: .bold))
                    .foregroundStyle(Color.white)
                    .listRowBackground(Color.clear)
                Text(
                    "Momentra uses account data to run your moments. You can grant or withdraw analytics and AI consents anytime in Account → Privacy."
                )
                .font(.system(size: 14))
                .foregroundStyle(Color.white.opacity(0.75))
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
            }
        }
        .scrollContentBackground(.hidden)
        .safeAreaInset(edge: .bottom) {
            Button("Continue", action: onContinue)
                .buttonStyle(BrandPrimaryButtonStyle())
                .padding(.horizontal, 24)
                .padding(.bottom, 16)
                .accessibilityIdentifier("consent.continue")
        }
        .brandAuthScreen()
        .accessibilityIdentifier("consent.gate")
    }
}
