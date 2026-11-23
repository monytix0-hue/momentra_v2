import SwiftUI
import UIKit
import FirebaseAuth

/// Account hub — profile, security, notifications, privacy, devices, help.
///
/// Structured the way iOS settings screens are: one root `Form` of `NavigationLink`s into
/// per-domain screens, so the system back button navigates and each screen owns its own state.
/// The previous version swapped a `section` string inside a single `Form` and put a manual "Back"
/// button at the bottom of each screen — which showed a Close *and* a Back at once and kept all
/// six screens' state alive.
struct AccountHubView: View {
    let identity: ShellIdentity
    var onDisplayNameSaved: (String) -> Void = { _ in }
    let onSignOut: () -> Void
    let onClose: () -> Void
    let onAccountDeleted: () -> Void

    @State private var confirmSignOut = false

    var body: some View {
        NavigationStack {
            Form {
                AccountProfileSection(
                    identity: identity,
                    onDisplayNameSaved: onDisplayNameSaved
                )

                Section {
                    NavigationLink {
                        AccountSecurityView()
                    } label: {
                        Label("Security", systemImage: "lock.shield")
                    }
                    NavigationLink {
                        AccountNotificationsView()
                    } label: {
                        Label("Notifications", systemImage: "bell.badge")
                    }
                    NavigationLink {
                        AccountPrivacyView()
                    } label: {
                        Label("Privacy & consent", systemImage: "hand.raised")
                    }
                    NavigationLink {
                        AccountDevicesView()
                    } label: {
                        Label("Devices", systemImage: "iphone.gen3")
                    }
                    NavigationLink {
                        AccountHelpView()
                    } label: {
                        Label("Help & legal", systemImage: "questionmark.circle")
                    }
                    #if DEBUG
                    NavigationLink {
                        AccountDeveloperView()
                    } label: {
                        Label("Developer", systemImage: "hammer")
                    }
                    #endif
                }

                Section {
                    Button("Sign out", role: .destructive) {
                        confirmSignOut = true
                    }
                    .accessibilityIdentifier("account.sign_out")
                }

                Section {
                    NavigationLink {
                        AccountDeleteView(onAccountDeleted: onAccountDeleted)
                    } label: {
                        Label("Delete account", systemImage: "trash")
                            .foregroundStyle(.red)
                    }
                    .accessibilityHint("Shows what will be erased, then asks for confirmation.")
                }
            }
            .scrollDismissesKeyboard(.interactively)
            .navigationTitle("Account")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Done", action: onClose)
                        .accessibilityLabel("Close account")
                }
            }
            .confirmationDialog(
                "Sign out of Momentra?",
                isPresented: $confirmSignOut,
                titleVisibility: .visible
            ) {
                Button("Sign out", role: .destructive, action: onSignOut)
                Button("Cancel", role: .cancel) {}
            } message: {
                Text("Anything waiting to sync stays on this device until you sign back in.")
            }
        }
    }
}

// MARK: - Profile

/// Name editing lives in its own `@State` so a failed save does not clobber what the person typed.
private struct AccountProfileSection: View {
    let identity: ShellIdentity
    let onDisplayNameSaved: (String) -> Void

    @State private var displayName: String
    @State private var saving = false
    @State private var saveError: String?

    init(identity: ShellIdentity, onDisplayNameSaved: @escaping (String) -> Void) {
        self.identity = identity
        self.onDisplayNameSaved = onDisplayNameSaved
        _displayName = State(initialValue: identity.displayName ?? "")
    }

    private var trimmedName: String {
        displayName.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var isDirty: Bool {
        let saved = (identity.displayName ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
        return !trimmedName.isEmpty && trimmedName != saved
    }

    var body: some View {
        Section {
            HStack(spacing: 16) {
                Text(initials)
                    .font(.title3.weight(.semibold))
                    .foregroundStyle(.white)
                    .frame(width: 56, height: 56)
                    .background(Color.accentColor, in: Circle())
                    .accessibilityHidden(true)
                VStack(alignment: .leading, spacing: 2) {
                    Text(trimmedName.isEmpty ? "Your name" : trimmedName)
                        .font(.headline)
                    if let email = PersonLabel.contactEmail(identity.email) {
                        Text(email)
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                            .textSelection(.enabled)
                    }
                }
            }
            .padding(.vertical, 4)
        } footer: {
            Text("Your name is visible to people who share a moment with you.")
        }

        Section("Display name") {
            TextField("Your name", text: $displayName)
                .textContentType(.name)
                .textInputAutocapitalization(.words)
                .submitLabel(.done)
                .onSubmit { Task { await save() } }

            if let saveError {
                Text(saveError)
                    .font(.footnote)
                    .foregroundStyle(.red)
            }
        }

        if isDirty || saving {
            Section {
                Button {
                    Task { await save() }
                } label: {
                    HStack {
                        Spacer()
                        if saving {
                            ProgressView()
                        } else {
                            Text("Save changes").fontWeight(.semibold)
                        }
                        Spacer()
                    }
                }
                .disabled(saving || !isDirty)
            }
        }
    }

    private var initials: String {
        let source = PersonLabel.accountName(trimmedName)
        let letters = source.split(separator: " ").prefix(2).compactMap(\.first)
        let joined = String(letters).uppercased()
        return joined.isEmpty ? "?" : joined
    }

    private func save() async {
        guard isDirty, !saving else { return }
        saving = true
        saveError = nil
        defer { saving = false }
        do {
            let saved = try await APIClient.shared.patchMe(displayName: trimmedName)
            // Explicit steps rather than a chained `flatMap`: on an optional `String`, Swift
            // resolved `.flatMap` to `Sequence.flatMap`, so the closure was treated as returning
            // `[Character]?` and the `??` had mismatched operand types.
            let serverName = saved.displayName?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
            let applied = serverName.isEmpty ? trimmedName : serverName
            displayName = applied
            onDisplayNameSaved(applied)
        } catch {
            saveError = error.localizedDescription
        }
    }
}

// MARK: - Security

private struct AccountSecurityView: View {
    @State private var pinInput = ""
    @State private var confirmPinInput = ""
    @State private var pinError: String?
    @State private var pinEnabled = AppLockStore.isPinEnabled
    @State private var biometricsEnabled = AppLockStore.biometricsEnabled
    /// 0 means "immediately", which is only offered when a PIN actually exists — a zero delay with
    /// no lock is the same as having no lock at all.
    @State private var autoLockSeconds = AppLockStore.autoLockSeconds
    @State private var confirmRemovePin = false

    private let autoLockOptions: [Int] = [0, 30, 60, 120, 300, 600]

    private var pinIsValid: Bool {
        pinInput.count >= 4 && pinInput.count <= 8 && pinInput.allSatisfy(\.isNumber)
    }

    private var isChanging: Bool { pinEnabled }

    var body: some View {
        Form {
            Section {
                if pinEnabled {
                    Label("App lock is on", systemImage: "lock.fill")
                        .foregroundStyle(.green)
                    Toggle("Unlock with biometrics", isOn: $biometricsEnabled)
                        .onChange(of: biometricsEnabled) { _, value in
                            AppLockStore.biometricsEnabled = value
                        }
                    Picker("Require PIN", selection: $autoLockSeconds) {
                        ForEach(autoLockOptions, id: \.self) { seconds in
                            Text(autoLockLabel(seconds)).tag(seconds)
                        }
                    }
                    .onChange(of: autoLockSeconds) { _, value in
                        AppLockStore.autoLockSeconds = value
                    }
                    Button("Remove PIN", role: .destructive) {
                        confirmRemovePin = true
                    }
                } else {
                    Text("Add a PIN to lock Momentra when you leave it.")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            } header: {
                Text("Local app lock")
            } footer: {
                Text("Your PIN is stored only on this device and is never sent to us.")
            }

            Section {
                SecureField(isChanging ? "New PIN (4–8 digits)" : "PIN (4–8 digits)", text: $pinInput)
                    .keyboardType(.numberPad)
                    .textContentType(.newPassword)
                    .privacySensitive()
                    .onChange(of: pinInput) { _, _ in pinError = nil }

                SecureField("Confirm PIN", text: $confirmPinInput)
                    .keyboardType(.numberPad)
                    .textContentType(.newPassword)
                    .privacySensitive()
                    .onChange(of: confirmPinInput) { _, _ in pinError = nil }

                if let pinError {
                    Text(pinError)
                        .font(.footnote)
                        .foregroundStyle(.red)
                }

                Button(isChanging ? "Change PIN" : "Set PIN") {
                    savePin()
                }
                .disabled(!pinIsValid || pinInput != confirmPinInput)
            } header: {
                Text(isChanging ? "Change PIN" : "Set PIN")
            } footer: {
                Text("Choose 4 to 8 digits. You will need this PIN the next time Momentra locks.")
            }
        }
        .scrollDismissesKeyboard(.interactively)
        .navigationTitle("Security")
        .navigationBarTitleDisplayMode(.inline)
        .confirmationDialog(
            "Remove app lock?",
            isPresented: $confirmRemovePin,
            titleVisibility: .visible
        ) {
            Button("Remove PIN", role: .destructive) {
                AppLockStore.clearPin()
                pinEnabled = false
                pinInput = ""
                confirmPinInput = ""
                pinError = nil
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Momentra will no longer ask for a PIN when it opens.")
        }
        .onAppear {
            pinEnabled = AppLockStore.isPinEnabled
            biometricsEnabled = AppLockStore.biometricsEnabled
            autoLockSeconds = AppLockStore.autoLockSeconds
        }
    }

    private func autoLockLabel(_ seconds: Int) -> String {
        switch seconds {
        case 0: return "Immediately"
        case 60: return "After 1 minute"
        case 120: return "After 2 minutes"
        case 300: return "After 5 minutes"
        case 600: return "After 10 minutes"
        default: return "After \(seconds) seconds"
        }
    }

    private func savePin() {
        guard pinIsValid else {
            pinError = "PIN must be 4 to 8 digits."
            return
        }
        guard pinInput == confirmPinInput else {
            pinError = "The two PINs do not match."
            return
        }
        do {
            try AppLockStore.setPin(pinInput)
            pinEnabled = true
            pinInput = ""
            confirmPinInput = ""
            pinError = nil
        } catch {
            pinError = error.localizedDescription
        }
    }
}

// MARK: - Notifications

private struct AccountNotificationsView: View {
    @AppStorage("momentra_push_notifications") private var pushEnabled = true
    @AppStorage("momentra_hide_balances") private var hideBalances = false

    @State private var digestEnabled = false
    @State private var quietStart = Date()
    @State private var quietEnd = Date()
    @State private var categories = APIClient.NotificationCategoriesPayload(
        finance: true, tasks: true, social: true, invites: true, approvals: true, reminders: true
    )
    @State private var savingError: String?
    @State private var loaded = false

    private static let hhmm: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "HH:mm"
        return formatter
    }()

    var body: some View {
        Form {
            Section {
                Toggle("Allow notifications", isOn: $pushEnabled)
                    .onChange(of: pushEnabled) { _, value in
                        Task { await saveGlobalPrefs(push: value) }
                    }
            } header: {
                Text("Notifications")
            } footer: {
                Text("We send a push when something is created, changed, or needs you — not on an empty daily schedule.")
            }

            Section("What you hear about") {
                categoryToggle("Money & expenses", key: \.finance)
                categoryToggle("Tasks & planning", key: \.tasks)
                categoryToggle("Social & memories", key: \.social)
                categoryToggle("Invitations", key: \.invites)
                categoryToggle("Approvals", key: \.approvals)
                categoryToggle("Reminders", key: \.reminders)
            }
            .disabled(!pushEnabled)

            Section {
                Toggle("Smart digest", isOn: $digestEnabled)
                    .onChange(of: digestEnabled) { _, value in
                        Task { await saveGlobalPrefs(digest: value) }
                    }
                DatePicker(
                    "Quiet hours start",
                    selection: $quietStart,
                    displayedComponents: .hourAndMinute
                )
                .onChange(of: quietStart) { _, _ in saveQuietHours() }
                DatePicker(
                    "Quiet hours end",
                    selection: $quietEnd,
                    displayedComponents: .hourAndMinute
                )
                .onChange(of: quietEnd) { _, _ in saveQuietHours() }
            } header: {
                Text("Delivery")
            } footer: {
                Text("Notifications are held during quiet hours and delivered afterwards.")
            }
            .disabled(!pushEnabled)

            Section {
                Toggle("Hide balances", isOn: $hideBalances)
                Toggle("Evening check-in", isOn: Binding(
                    get: { PersonalHabitPreferences.eveningNudgeEnabled },
                    set: { value in
                        PersonalHabitPreferences.eveningNudgeEnabled = value
                        PersonalEveningNudgeScheduler.sync(enabled: value)
                    }
                ))
            } header: {
                Text("Display")
            } footer: {
                Text("The evening check-in is a local reminder at 8pm. It is not a server alert.")
            }

            if let savingError {
                Section {
                    Text(savingError)
                        .font(.footnote)
                        .foregroundStyle(.red)
                }
            }
        }
        .navigationTitle("Notifications")
        .navigationBarTitleDisplayMode(.inline)
        .task { await loadPreferences() }
    }

    @ViewBuilder
    private func categoryToggle(
        _ label: String,
        key: WritableKeyPath<APIClient.NotificationCategoriesPayload, Bool?>
    ) -> some View {
        Toggle(label, isOn: Binding(
            get: { categories[keyPath: key] ?? true },
            set: { value in
                categories[keyPath: key] = value
                Task { await saveGlobalPrefs(categories: categories) }
            }
        ))
    }

    private func loadPreferences() async {
        guard !loaded else { return }
        loaded = true
        do {
            let prefs = try await APIClient.shared.getMyNotificationPreferences()
            pushEnabled = prefs.pushNotificationsEnabled
            digestEnabled = prefs.digestEnabled ?? false
            quietStart = Self.hhmm.date(from: String((prefs.quietHoursStart ?? "22:00").prefix(5))) ?? quietStart
            quietEnd = Self.hhmm.date(from: String((prefs.quietHoursEnd ?? "07:00").prefix(5))) ?? quietEnd
            if let serverCategories = prefs.categories {
                categories = serverCategories
            }
        } catch {
            savingError = error.localizedDescription
        }
    }

    private func saveQuietHours() {
        Task {
            await saveGlobalPrefs(
                quietStart: Self.hhmm.string(from: quietStart),
                quietEnd: Self.hhmm.string(from: quietEnd)
            )
        }
    }

    private func saveGlobalPrefs(
        push: Bool? = nil,
        categories: APIClient.NotificationCategoriesPayload? = nil,
        quietStart: String? = nil,
        quietEnd: String? = nil,
        digest: Bool? = nil
    ) async {
        savingError = nil
        do {
            _ = try await APIClient.shared.patchMyNotificationPreferences(
                pushNotificationsEnabled: push,
                categories: categories,
                quietHoursStart: quietStart,
                quietHoursEnd: quietEnd,
                digestEnabled: digest
            )
        } catch {
            savingError = error.localizedDescription
        }
    }
}

// MARK: - Privacy & consent

private struct AccountPrivacyView: View {
    @State private var consents: [ConsentPurposePayload] = []
    @State private var loading = true
    @State private var errorMessage: String?
    @State private var exportItems: [Any] = []
    @State private var showExport = false

    var body: some View {
        Form {
            Section {
                if loading {
                    HStack {
                        ProgressView()
                        Text("Loading consents…")
                            .foregroundStyle(.secondary)
                    }
                } else if consents.isEmpty {
                    Text("No consent purposes are available right now.")
                        .foregroundStyle(.secondary)
                } else {
                    ForEach(consents, id: \.code) { consent in
                        Toggle(consent.displayName ?? consent.code, isOn: Binding(
                            get: { consent.granted == true },
                            set: { enabled in
                                Task { await setConsent(consent.code, granted: enabled) }
                            }
                        ))
                    }
                }
                if let errorMessage {
                    Text(errorMessage)
                        .font(.footnote)
                        .foregroundStyle(.red)
                }
            } header: {
                Text("Consent")
            } footer: {
                Text("You can withdraw any of these at any time. Withdrawing stops new use of that data; it does not delete what was already recorded.")
            }

            Section("Your data") {
                Button {
                    Task { await downloadData() }
                } label: {
                    Label("Download my data", systemImage: "square.and.arrow.down")
                }
                .accessibilityHint("Creates a JSON summary of your profile, devices and consents.")
            }

            Section("Legal") {
                Link(destination: URL(string: "https://momentra.tech/privacy")!) {
                    Label("Privacy Policy", systemImage: "doc.text")
                }
                Link(destination: URL(string: "https://momentra.tech/terms")!) {
                    Label("Terms of Service", systemImage: "doc.text")
                }
            }

            Section {
                Link(destination: URL(string: "mailto:hello@momentra.app?subject=Momentra%20privacy%20%2F%20grievance%20request")!) {
                    Label("Contact grievance officer", systemImage: "envelope")
                }
            } footer: {
                Text("hello@momentra.app. We acknowledge within 72 hours and aim to resolve within 14 days. Please use this channel before approaching the Data Protection Board.")
            }
        }
        .navigationTitle("Privacy & consent")
        .navigationBarTitleDisplayMode(.inline)
        .task { await loadConsents() }
        .refreshable { await loadConsents() }
        .sheet(isPresented: $showExport) {
            ActivityShareSheet(items: exportItems)
        }
    }

    private func loadConsents() async {
        loading = true
        errorMessage = nil
        do {
            consents = try await APIClient.shared.listConsents().purposes
        } catch {
            errorMessage = error.localizedDescription
        }
        loading = false
    }

    private func setConsent(_ code: String, granted: Bool) async {
        errorMessage = nil
        do {
            if granted {
                _ = try await APIClient.shared.grantConsent(purposeCode: code)
            } else {
                _ = try await APIClient.shared.withdrawConsent(purposeCode: code)
            }
            consents = try await APIClient.shared.listConsents().purposes
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    private func downloadData() async {
        errorMessage = nil
        do {
            let export = try await APIClient.shared.exportMe()
            let encoder = JSONEncoder()
            encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
            let data = try encoder.encode(export)
            exportItems = [String(data: data, encoding: .utf8) ?? "{}"]
            showExport = true
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}

// MARK: - Devices

private struct AccountDevicesView: View {
    @State private var devices: [DeviceItemPayload] = []
    @State private var loading = true
    @State private var errorMessage: String?
    @State private var pendingRevoke: DeviceItemPayload?

    private var currentDeviceId: String { PushNotifications.currentDeviceId }

    var body: some View {
        Form {
            Section {
                if loading {
                    HStack {
                        ProgressView()
                        Text("Loading devices…")
                            .foregroundStyle(.secondary)
                    }
                } else if devices.isEmpty {
                    Text("No devices are signed in.")
                        .foregroundStyle(.secondary)
                } else {
                    ForEach(devices, id: \.deviceId) { device in
                        VStack(alignment: .leading, spacing: 2) {
                            HStack {
                                Text(device.platform ?? "Device")
                                Spacer()
                                if device.deviceId == currentDeviceId {
                                    Text("This device")
                                        .font(.caption)
                                        .foregroundStyle(.secondary)
                                }
                            }
                            Text(device.deviceId)
                                .font(.caption2)
                                .foregroundStyle(.secondary)
                                .lineLimit(1)
                            if device.deviceId != currentDeviceId {
                                Button("Revoke", role: .destructive) {
                                    pendingRevoke = device
                                }
                                .font(.footnote)
                            }
                        }
                    }
                }
                if let errorMessage {
                    Text(errorMessage)
                        .font(.footnote)
                        .foregroundStyle(.red)
                }
            } header: {
                Text("Signed in")
            } footer: {
                Text("Revoking a device signs it out of Momentra. Your account stays active everywhere else.")
            }
        }
        .navigationTitle("Devices")
        .navigationBarTitleDisplayMode(.inline)
        .task { await load() }
        .refreshable { await load() }
        .confirmationDialog(
            "Revoke this device?",
            isPresented: Binding(
                get: { pendingRevoke != nil },
                set: { if !$0 { pendingRevoke = nil } }
            ),
            titleVisibility: .visible,
            presenting: pendingRevoke
        ) { device in
            Button("Revoke \(device.platform ?? "device")", role: .destructive) {
                Task { await revoke(device) }
            }
            Button("Cancel", role: .cancel) { pendingRevoke = nil }
        }
    }

    private func load() async {
        loading = true
        errorMessage = nil
        await PushNotifications.syncDeviceWithBackend()
        do {
            devices = try await APIClient.shared.listDevices().items.filter { !($0.revoked ?? false) }
        } catch {
            errorMessage = error.localizedDescription
        }
        loading = false
    }

    private func revoke(_ device: DeviceItemPayload) async {
        pendingRevoke = nil
        errorMessage = nil
        do {
            _ = try await APIClient.shared.revokeDevice(deviceId: device.deviceId)
            devices = try await APIClient.shared.listDevices().items.filter { !($0.revoked ?? false) }
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}

// MARK: - Help & legal

private struct AccountHelpView: View {
    var body: some View {
        Form {
            Section {
                LabeledContent("Version", value: appVersion)
            }

            Section {
                Text("Momentra brings personal, group and business moments into one app.")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }

            Section("Legal") {
                Link(destination: URL(string: "https://momentra.tech/privacy")!) {
                    Label("Privacy Policy", systemImage: "doc.text")
                }
                Link(destination: URL(string: "https://momentra.tech/terms")!) {
                    Label("Terms of Service", systemImage: "doc.text")
                }
            }
        }
        .navigationTitle("Help & legal")
        .navigationBarTitleDisplayMode(.inline)
    }

    private var appVersion: String {
        let info = Bundle.main.infoDictionary
        let short = info?["CFBundleShortVersionString"] as? String ?? "—"
        let build = info?["CFBundleVersion"] as? String ?? "—"
        return "\(short) (\(build))"
    }
}

// MARK: - Delete account

/// Account deletion needs, per App Store 5.1.1(v), to be doable in-app, and per good practice to
/// show what will be erased before asking. Blockers are read from the preflight, so a user who
/// cannot delete learns why here rather than from a failed request.
private struct AccountDeleteView: View {
    let onAccountDeleted: () -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var preflight: ErasurePreflightPayload?
    @State private var loading = true
    @State private var errorMessage: String?
    @State private var confirmDelete = false
    @State private var deleting = false

    private var blockers: [ErasureBlockerPayload] {
        preflight?.blockers ?? []
    }

    var body: some View {
        Form {
            Section {
                if loading {
                    HStack {
                        ProgressView()
                        Text("Checking your account…")
                            .foregroundStyle(.secondary)
                    }
                } else if !blockers.isEmpty {
                    Text("Resolve these before deleting your account:")
                        .font(.footnote.weight(.semibold))
                    ForEach(Array(blockers.enumerated()), id: \.offset) { _, blocker in
                        VStack(alignment: .leading, spacing: 2) {
                            Text(blocker.title ?? blocker.id ?? "Item")
                                .font(.subheadline.weight(.semibold))
                            Text(blocker.message ?? blocker.kind ?? "")
                                .font(.footnote)
                                .foregroundStyle(.secondary)
                        }
                    }
                } else {
                    Text("This permanently erases data you own:")
                        .font(.footnote.weight(.semibold))
                    Label("Your profile identifiers are anonymised", systemImage: "person.crop.circle.badge.xmark")
                    Label("Your devices are signed out and revoked", systemImage: "iphone.slash")
                    Label("Personal-only moments and your media are removed", systemImage: "trash")
                    Label("Your consents are withdrawn", systemImage: "hand.raised.slash")
                    Text("Shared group and business content stays for the other members after you leave.")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
                if let errorMessage {
                    Text(errorMessage)
                        .font(.footnote)
                        .foregroundStyle(.red)
                }
            } header: {
                Text("What deleting does")
            }

            if !loading && blockers.isEmpty {
                Section {
                    Button(deleting ? "Deleting…" : "Delete my account", role: .destructive) {
                        confirmDelete = true
                    }
                    .disabled(deleting)
                }
            }

            Section {
                Text("hello@momentra.app")
                    .textSelection(.enabled)
            } header: {
                Text("Grievance")
            } footer: {
                Text("Contact us if you believe data was erased incorrectly.")
            }
        }
        .navigationTitle("Delete account")
        .navigationBarTitleDisplayMode(.inline)
        .task { await checkPreflight() }
        .confirmationDialog(
            "Delete your account?",
            isPresented: $confirmDelete,
            titleVisibility: .visible
        ) {
            Button("Delete account", role: .destructive) {
                Task { await deleteAccount() }
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This cannot be undone. Shared content stays for other members.")
        }
    }

    private func checkPreflight() async {
        loading = true
        errorMessage = nil
        do {
            preflight = try await APIClient.shared.erasurePreflight()
        } catch {
            errorMessage = error.localizedDescription
        }
        loading = false
    }

    private func deleteAccount() async {
        guard preflight?.canErase == true else { return }
        deleting = true
        errorMessage = nil
        defer { deleting = false }
        do {
            _ = try await APIClient.shared.softDeleteMe()
            try await Auth.auth().currentUser?.delete()
            OnboardingPrefs.clearAgeGateAccepted()
            UserDefaults.standard.set(false, forKey: "momentra_consent_gate_seen")
            AppLockStore.clearPin()
            UserDefaults.standard.removeObject(forKey: "retail.parked.holds.v1")
            onAccountDeleted()
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}

// MARK: - Developer (DEBUG only)

#if DEBUG
private struct AccountDeveloperView: View {
    @State private var override = APIConfig.baseURLOverride

    var body: some View {
        Form {
            Section {
                LabeledContent("Active", value: APIConfig.baseURLDescription)
                TextField("Override URL", text: $override)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .keyboardType(.URL)
            } header: {
                Text("API server")
            } footer: {
                Text("A physical device cannot reach 127.0.0.1. Production is https://api.momentra.tech. Debug builds may point at a private LAN http host.")
            }

            Section {
                Button("Save override") {
                    APIConfig.baseURLOverride = override
                    override = APIConfig.baseURLOverride
                }
                Button("Use production") {
                    APIConfig.baseURLOverride = APIConfig.productionAPIBase
                    override = APIConfig.baseURLOverride
                }
                Button("Clear override", role: .destructive) {
                    APIConfig.baseURLOverride = ""
                    override = ""
                }
            } footer: {
                Text("Sign out and back in after changing the server.")
            }
        }
        .navigationTitle("Developer")
        .navigationBarTitleDisplayMode(.inline)
    }
}
#endif
