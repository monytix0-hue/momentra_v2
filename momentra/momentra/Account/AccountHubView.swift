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
    @State private var pushNotificationsEnabled = UserDefaults.standard.object(forKey: "momentra_push_notifications") as? Bool ?? true
    @State private var digestEnabled = false
    @State private var quietStart = "22:00"
    @State private var quietEnd = "07:00"
    @State private var categories = APIClient.NotificationCategoriesPayload(
        finance: true, tasks: true, social: true, invites: true, approvals: true, reminders: true
    )
    @State private var section = "home"
    @State private var consents: [ConsentPurposePayload] = []
    @State private var devices: [DeviceItemPayload] = []
    @State private var confirmDelete = false
    @State private var autoLockSec = AppLockStore.autoLockSeconds
    @State private var apiBaseOverride = APIConfig.baseURLOverride

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
                        Button("Developer / API server") { section = "developer" }
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
                    Section("Notifications") {
                        Toggle("Notifications", isOn: $pushNotificationsEnabled)
                            .onChange(of: pushNotificationsEnabled) { _, v in
                                UserDefaults.standard.set(v, forKey: "momentra_push_notifications")
                                Task { await saveGlobalPrefs(push: v) }
                            }
                    }
                    Section("What you hear about") {
                        categoryToggle("Money & expenses", key: \.finance)
                        categoryToggle("Tasks & planning", key: \.tasks)
                        categoryToggle("Social & memories", key: \.social)
                        categoryToggle("Invitations", key: \.invites)
                        categoryToggle("Approvals", key: \.approvals)
                        categoryToggle("Reminders", key: \.reminders)
                    }
                    Section("Delivery") {
                        Toggle("Smart digest", isOn: $digestEnabled)
                            .onChange(of: digestEnabled) { _, v in
                                Task { await saveGlobalPrefs(digest: v) }
                            }
                        TextField("Quiet hours start (HH:MM)", text: $quietStart)
                            .onSubmit { Task { await saveGlobalPrefs(quietStart: quietStart, quietEnd: quietEnd) } }
                        TextField("Quiet hours end (HH:MM)", text: $quietEnd)
                            .onSubmit { Task { await saveGlobalPrefs(quietStart: quietStart, quietEnd: quietEnd) } }
                        Text("Example: 22:00 – 07:00")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    Section("Preferences") {
                        Toggle("Hide balances", isOn: $hideBalances)
                            .onChange(of: hideBalances) { _, v in
                                UserDefaults.standard.set(v, forKey: "momentra_hide_balances")
                            }
                        Button("Back") { section = "home" }
                    }
                case "developer":
                    Section("API server") {
                        Text("Active: \(APIConfig.baseURLDescription)")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                        TextField("Override URL (LAN IP)", text: $apiBaseOverride)
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled()
                            .keyboardType(.URL)
                        Text("Physical devices cannot reach 127.0.0.1. Use your Mac’s LAN IP, e.g. http://192.168.1.10:3000/")
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                        Button("Save override") {
                            APIConfig.baseURLOverride = apiBaseOverride
                            apiBaseOverride = APIConfig.baseURLOverride
                            status = "API base set to \(APIConfig.baseURLDescription). Restart shell or sign out/in if needed."
                        }
                        Button("Use production (api.mallaapp.org)") {
                            APIConfig.baseURLOverride = "https://api.mallaapp.org/"
                            apiBaseOverride = APIConfig.baseURLOverride
                            status = "API base set to \(APIConfig.baseURLDescription)"
                        }
                        Button("Clear override", role: .destructive) {
                            APIConfig.baseURLOverride = ""
                            apiBaseOverride = ""
                            status = "Override cleared. Active: \(APIConfig.baseURLDescription)"
                        }
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
                    await PushNotifications.syncDeviceWithBackend()
                    devices = (try? await APIClient.shared.listDevices().items) ?? []
                    if let prefs = try? await APIClient.shared.getMyNotificationPreferences() {
                        pushNotificationsEnabled = prefs.pushNotificationsEnabled
                        UserDefaults.standard.set(prefs.pushNotificationsEnabled, forKey: "momentra_push_notifications")
                        digestEnabled = prefs.digestEnabled ?? false
                        quietStart = String((prefs.quietHoursStart ?? "22:00").prefix(5))
                        quietEnd = String((prefs.quietHoursEnd ?? "07:00").prefix(5))
                        if let c = prefs.categories {
                            categories = c
                        }
                    }
                }
            }
        }
    }

    @ViewBuilder
    private func categoryToggle(_ label: String, key: WritableKeyPath<APIClient.NotificationCategoriesPayload, Bool?>) -> some View {
        Toggle(label, isOn: Binding(
            get: { categories[keyPath: key] ?? true },
            set: { v in
                categories[keyPath: key] = v
                Task { await saveGlobalPrefs(categories: categories) }
            }
        ))
    }

    private func saveGlobalPrefs(
        push: Bool? = nil,
        categories: APIClient.NotificationCategoriesPayload? = nil,
        quietStart: String? = nil,
        quietEnd: String? = nil,
        digest: Bool? = nil
    ) async {
        do {
            _ = try await APIClient.shared.patchMyNotificationPreferences(
                pushNotificationsEnabled: push,
                categories: categories,
                quietHoursStart: quietStart,
                quietHoursEnd: quietEnd,
                digestEnabled: digest
            )
            status = "Notification preferences saved"
        } catch {
            status = error.localizedDescription
        }
    }
}
