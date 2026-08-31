import LocalAuthentication
import SwiftUI

/// Local App Lock gate — PIN / biometrics never leave the device.
struct AppLockGateView: View {
    var onUnlocked: () -> Void

    @State private var pin = ""
    @State private var error: String?

    var body: some View {
        VStack(spacing: 16) {
            Text("App Locked")
                .font(.system(size: 22, weight: .bold))
                .foregroundStyle(Color.white)
            Text("Enter your local PIN to continue.")
                .foregroundStyle(Color.white.opacity(0.7))
            SecureField("PIN", text: $pin)
                .keyboardType(.numberPad)
                .textFieldStyle(.roundedBorder)
                .padding(.horizontal, 24)
            if let error {
                Text(error).foregroundStyle(.red).font(.caption)
            }
            Button("Unlock") {
                if AppLockStore.verifyPin(pin) {
                    AppLockSession.unlocked = true
                    onUnlocked()
                } else {
                    error = "Incorrect PIN"
                    pin = ""
                }
            }
            .buttonStyle(BrandPrimaryButtonStyle())
            .padding(.horizontal, 24)

            if AppLockStore.biometricsEnabled {
                Button("Use biometrics") {
                    authenticateBiometrics()
                }
                .foregroundStyle(Color.white.opacity(0.85))
            }
        }
        .brandAuthScreen()
        .onAppear {
            if AppLockStore.biometricsEnabled {
                authenticateBiometrics()
            }
        }
    }

    private func authenticateBiometrics() {
        let context = LAContext()
        var authError: NSError?
        guard context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &authError) else {
            error = authError?.localizedDescription ?? "Biometrics unavailable"
            return
        }
        context.evaluatePolicy(
            .deviceOwnerAuthenticationWithBiometrics,
            localizedReason: "Unlock Momentra"
        ) { success, evalError in
            DispatchQueue.main.async {
                if success {
                    AppLockSession.unlocked = true
                    onUnlocked()
                } else if let evalError {
                    error = evalError.localizedDescription
                }
            }
        }
    }
}
