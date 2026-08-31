import SwiftUI
import UIKit
import FirebaseAuth

/// S7 Account hub — FIGMA_GAP shell UI.
struct AccountHubView: View {
    let identity: ShellIdentity
    let onSignOut: () -> Void
    let onClose: () -> Void
    let onAccountDeleted: () -> Void

    @State private var displayName: String = ""
    @State private var status: String?
    @State private var pinInput = ""
    @State private var hideBalances = UserDefaults.standard.bool(forKey: "momentra_hide_balances")
    @State private var section = "home"
    @State private var consents: [ConsentPurposePayload] = []
    @State private var devices: [DeviceItemPayload] = []
    @State private var confirmDelete = false
    @State private var autoLockSec = AppLockStore.autoLockSeconds

    private var currentDeviceId: String {
        UIDevice.current.identifierForVendor?.uuidString ?? "unknown"
    }

    var body: some View {
        NavigationStack {
            Form {
                if let status { Text(status).foregroundStyle(.secondary) }
                switch section {
                case "home":
                    Section("Profile") {
                        TextField("Display name", text: $displayName)
                        Button("Save profile") {
                            Task {
                                do {
                                    _ = try await APIClient.shared.patchMe(displayName: displayName)
                                    status = "Profile saved"
                                } catch {
                                    status = error.localizedDescription
                                }
                            }
                        }
                        Text(identity.email ?? "No email").foregroundStyle(.secondary)
                    }
                    Section {
                        Button("App Security") { section = "security" }
                        Button("Privacy & Consent") { section = "privacy" }
                        Button("Devices") { section = "devices" }
                        Button("Preferences") { section = "prefs" }
                        Button("Help & Legal") { section = "legal" }
                    }
                    Section {
                        Button("Sign out", role: .destructive, action: onSignOut)
                        if confirmDelete {
                            Text("Soft-deletes profile (DELETED). Domain history may be retained.")
                                .font(.caption)
                            Button("Confirm delete account", role: .destructive) {
                                Task {
                                    do {
                                        _ = try await APIClient.shared.softDeleteMe()
                                        try await Auth.auth().currentUser?.delete()
                                        onAccountDeleted()
                                    } catch {
                                        status = error.localizedDescription
                                    }
                                }
                            }
                            Button("Cancel") { confirmDelete = false }
                        } else {
                            Button("Delete account…", role: .destructive) { confirmDelete = true }
                        }
                    }
                case "security":
                    Section("Local App Lock") {
                        Text("PIN never leaves this device.")
                            .font(.caption)
                        SecureField("PIN 4–8 digits", text: $pinInput)
                            .keyboardType(.numberPad)
                        Button(AppLockStore.isPinEnabled ? "Change PIN" : "Enable PIN") {
                            do {
                                try AppLockStore.setPin(pinInput)
                                pinInput = ""
                                status = "PIN saved locally"
                            } catch {
                                status = error.localizedDescription
                            }
                        }
                        if AppLockStore.isPinEnabled {
                            Button("Remove PIN", role: .destructive) {
                                AppLockStore.clearPin()
                                status = "PIN removed"
                            }
                            Toggle("Biometrics", isOn: Binding(
                                get: { AppLockStore.biometricsEnabled },
                                set: { AppLockStore.biometricsEnabled = $0 }
                            ))
                            Stepper("Auto-lock \(autoLockSec)s", value: $autoLockSec, in: 0...600, step: 30)
                                .onChange(of: autoLockSec) { _, v in
                                    AppLockStore.autoLockSeconds = v
                                }
                        }
                        Button("Back") { section = "home" }
                    }
                case "prefs":
                    Section("Preferences") {
                        Toggle("Hide balances", isOn: $hideBalances)
                            .onChange(of: hideBalances) { _, v in
                                UserDefaults.standard.set(v, forKey: "momentra_hide_balances")
                            }
                        Text("Currency / language / appearance deferred (FIGMA_GAP).")
                            .font(.caption)
                        Button("Back") { section = "home" }
                    }
                case "privacy":
                    Section("Consent") {
                        ForEach(consents, id: \.code) { c in
                            Toggle(c.displayName ?? c.code, isOn: Binding(
                                get: { c.granted == true },
                                set: { enabled in
                                    Task {
                                        do {
                                            if enabled {
                                                _ = try await APIClient.shared.grantConsent(purposeCode: c.code)
                                            } else {
                                                _ = try await APIClient.shared.withdrawConsent(purposeCode: c.code)
                                            }
                                            consents = try await APIClient.shared.listConsents().purposes
                                        } catch {
                                            status = error.localizedDescription
                                        }
                                    }
                                }
                            ))
                        }
                        Button("Back") { section = "home" }
                    }
                case "devices":
                    Section("Devices") {
                        ForEach(devices.filter { !($0.revoked ?? false) }, id: \.deviceId) { d in
                            HStack {
                                VStack(alignment: .leading) {
                                    Text(d.platform ?? "Device")
                                    Text(d.deviceId).font(.caption2).foregroundStyle(.secondary)
                                    if d.deviceId == currentDeviceId {
                                        Text("This device").font(.caption2).foregroundStyle(.secondary)
                                    }
                                }
                                Spacer()
                                if d.deviceId != currentDeviceId {
                                    Button("Revoke", role: .destructive) {
                                        Task {
                                            _ = try? await APIClient.shared.revokeDevice(deviceId: d.deviceId)
                                            devices = (try? await APIClient.shared.listDevices().items) ?? devices
                                        }
                                    }
                                }
                            }
                        }
                        Text("Logout-all sessions deferred (no session table).")
                            .font(.caption)
                        Button("Back") { section = "home" }
                    }
                default:
                    Section("Help & Legal") {
                        Text("About Momentra")
                        Text("Placeholder Privacy / Terms (FIGMA_GAP).")
                            .font(.caption)
                        Button("Back") { section = "home" }
                    }
                }
            }
            .navigationTitle("Account")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close", action: onClose)
                }
            }
            .onAppear {
                displayName = identity.displayName ?? ""
                Task {
                    consents = (try? await APIClient.shared.listConsents().purposes) ?? []
                    devices = (try? await APIClient.shared.listDevices().items) ?? []
                    _ = try? await APIClient.shared.registerDevice(deviceId: currentDeviceId)
                }
            }
        }
    }
}
