import PhotosUI
import SwiftUI
import UIKit

/// Chrome + subtype chips + active setup document. Activate → `submitBusinessSetup`.
struct BusinessSetupShell: View {
    let family: BusinessSetupFamily
    let initialTypeCode: String
    @ObservedObject var createModel: MomentCreateModel
    var companyId: String?
    var editingMomentId: String? = nil
    var editingMomentStatus: String? = nil
    var initialTitle: String? = nil
    var onBack: () -> Void
    var onCreated: (CreateMomentOutcome) -> Void

    @State private var typeCode: String
    @State private var values: [String: String] = [:]
    @State private var didSeed = false
    @State private var fieldErrors: [String: String] = [:]
    @State private var savingDraft = false
    @State private var draftMomentId: String?
    @State private var draftMomentStatus: String?
    @State private var draftSavedMessage: String?
    @State private var showGstStatePicker = false
    @State private var linkedCompanyId: String?
    @State private var logoImage: UIImage?
    @State private var logoUploading = false
    @State private var logoError: String?

    init(
        family: BusinessSetupFamily,
        initialTypeCode: String? = nil,
        createModel: MomentCreateModel,
        companyId: String? = nil,
        editingMomentId: String? = nil,
        editingMomentStatus: String? = nil,
        initialTitle: String? = nil,
        onBack: @escaping () -> Void,
        onCreated: @escaping (CreateMomentOutcome) -> Void
    ) {
        self.family = family
        let code = initialTypeCode?.uppercased() ?? family.defaultTypeCode
        self.initialTypeCode = code
        self.createModel = createModel
        self.companyId = companyId
        self.editingMomentId = editingMomentId
        self.editingMomentStatus = editingMomentStatus
        self.initialTitle = initialTitle
        self.onBack = onBack
        self.onCreated = onCreated
        _typeCode = State(initialValue: code)
        _draftMomentId = State(initialValue: editingMomentId)
        _draftMomentStatus = State(initialValue: editingMomentStatus)
        _linkedCompanyId = State(initialValue: companyId)
    }

    private var theme: BusinessSetupTheme { .forTypeCode(typeCode) }
    private var spec: BusinessSetupDocumentSpec { BusinessSetupDocuments.spec(for: typeCode) }
    private var kind: BusinessSetupKind { .from(typeCode: typeCode) }

    private var titleValue: String {
        let key = spec.defaultTitleKey
        let raw = values[key]?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if !raw.isEmpty { return raw }
        return initialTitle?.trimmingCharacters(in: .whitespacesAndNewlines).nilIfEmpty
            ?? spec.heroTitle
    }

    private var canActivate: Bool {
        let key = spec.defaultTitleKey
        let raw = values[key]?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        return !raw.isEmpty || !(initialTitle ?? "").trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    /// Live hero headline — updates as the business-name field changes (Android parity).
    private var heroTitleLive: String {
        let typed = values[spec.defaultTitleKey]?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let seed = initialTitle?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let name = typed.isEmpty ? (seed.isEmpty ? nil : seed) : typed
        return BusinessSetupHeroTitle.format(name: name, placeholder: spec.heroTitle)
    }

    private var setupInProgress: Bool {
        (draftMomentStatus ?? "").caseInsensitiveCompare("DRAFT") == .orderedSame
    }

    var body: some View {
        ZStack(alignment: .bottom) {
            LinearGradient(
                colors: [theme.canvasTop.opacity(0.85), theme.canvas, theme.canvas],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()

            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    header
                    BusinessSetupDocumentKit.HeroCard(
                        theme: theme,
                        eyebrow: spec.eyebrow,
                        title: heroTitleLive,
                        subtitle: spec.heroSubtitle,
                        logoImage: logoImage,
                        logoUploading: logoUploading,
                        onLogoSelected: { item in
                            Task { await handleLogoPick(item) }
                        }
                    )
                    if let logoError, !logoError.isEmpty {
                        Text(logoError)
                            .font(.system(size: 12, weight: .medium))
                            .foregroundStyle(BusinessTokens.Colors.signalDanger)
                    }
                    if setupInProgress {
                        Text("Setup in progress")
                            .font(.system(size: 11, weight: .semibold))
                            .foregroundStyle(theme.accent)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(theme.surface, in: Capsule())
                    }
                    BusinessSetupDocumentKit.SubtypeChips(
                        theme: theme,
                        chips: family.chips,
                        selected: typeCode,
                        onSelect: { code in
                            let priorTitle = values[spec.defaultTitleKey]
                            typeCode = code
                            reseeds(for: code, priorTitle: priorTitle)
                        }
                    )
                    ForEach(Array(spec.sections.enumerated()), id: \.element.id) { index, section in
                        BusinessSetupDocumentKit.SectionCard(
                            theme: theme,
                            index: index + 1,
                            title: section.title,
                            subtitle: section.subtitle
                        ) {
                            ForEach(section.fields.filter(isFieldVisible)) { field in
                                fieldView(field)
                            }
                            if section.id == "people" {
                                BusinessSetupInviteSection(
                                    theme: theme,
                                    companyId: linkedCompanyId ?? companyId,
                                    embedded: true
                                ) { id in
                                    await createModel.mintCompanyInvite(companyId: id)
                                }
                            }
                            if section.id == "channels",
                               ["RETAIL_STORE", "DISTRIBUTION_DEALERSHIP", "TRADING_WHOLESALE", "CLIENT_CAMPAIGN"].contains(typeCode) {
                                let status = OperatingSetupPayloadBuilder.planStatusMessage(
                                    preferences: preferencesPayload()
                                )
                                Text("●  \(status.message)")
                                    .font(.system(size: 11, weight: .bold))
                                    .foregroundStyle(
                                        status.ok
                                            ? BusinessTokens.Colors.signalPositive
                                            : theme.accent
                                    )
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 6)
                                    .background(
                                        (status.ok
                                            ? BusinessTokens.Colors.signalPositive.opacity(0.14)
                                            : theme.accent.opacity(0.08)),
                                        in: Capsule()
                                    )
                            }
                        }
                    }
                    Color.clear.frame(height: 120)
                }
                .padding(.horizontal, 20)
                .padding(.top, 8)
            }
            .scrollDismissesKeyboard(BusinessSetupFieldInputPolicy.scrollDismissesKeyboard)

            VStack(spacing: 8) {
                if let draftSavedMessage {
                    Text(draftSavedMessage)
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(theme.accent)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 20)
                } else if let error = createModel.state.error {
                    Text(error)
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(BusinessTokens.Colors.signalDanger)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 20)
                }
                BusinessSetupDocumentKit.StickyActivateBar(
                    theme: theme,
                    label: spec.activateLabel,
                    enabled: canActivate,
                    submitting: createModel.state.submitting,
                    onActivate: activate,
                    draftEnabled: canActivate,
                    savingDraft: savingDraft,
                    onSaveDraft: saveDraft,
                    onScheduleLater: onBack
                )
            }
        }
        .preferredColorScheme(.dark)
        .sheet(isPresented: $showGstStatePicker) {
            SetupStatePickerSheet(
                theme: theme,
                selected: values["gstState"] ?? "",
                onPick: { state in
                    values["gstState"] = state.name
                    fieldErrors["gstState"] = nil
                    showGstStatePicker = false
                },
                onCancel: { showGstStatePicker = false }
            )
            .presentationDetents([.medium, .large])
            .presentationDragIndicator(.visible)
            .preferredColorScheme(.dark)
        }
        .onAppear {
            if !didSeed {
                reseeds(for: typeCode, priorTitle: initialTitle)
                didSeed = true
                if let momentId = editingMomentId {
                    Task { await loadDraftPreferences(momentId: momentId) }
                }
            }
        }
    }

    private var header: some View {
        HStack {
            Button(action: onBack) {
                Image(systemName: "chevron.left")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(BusinessTokens.Colors.textPrimary)
                    .frame(width: 44, height: 44)
            }
            .buttonStyle(.plain)
            Text(family.title)
                .font(.system(size: 17, weight: .semibold))
                .foregroundStyle(BusinessTokens.Colors.textPrimary)
            Spacer()
            if setupInProgress, let draftId = draftMomentId {
                Button("Discard draft") {
                    createModel.discardMomentDraft(momentId: draftId) {
                        draftMomentId = nil
                        draftMomentStatus = nil
                        onBack()
                    }
                }
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(BusinessTokens.Colors.signalDanger)
                .buttonStyle(.plain)
                .disabled(createModel.state.submitting)
            }
        }
    }

    private func isFieldVisible(_ field: BusinessSetupFieldSpec) -> Bool {
        let gstDependent: Set<String> = [
            "gstType", "gstState", "gstin", "filingFrequency",
            "gstRegistration", "salePriceIncludesGst", "gstEffectiveFrom",
        ]
        guard gstDependent.contains(field.key) else { return true }
        let registered = values["gstRegistered"] ?? values["gstTracking"] ?? "Yes"
        let off = registered == "No"
            || registered.lowercased() == "false"
            || registered.caseInsensitiveCompare("Not now") == .orderedSame
        return !off
    }

    private var labelIndex: [String: String] {
        Dictionary(
            uniqueKeysWithValues: spec.sections.flatMap { section in
                section.fields.map { ($0.key, $0.label) }
            }
        )
    }

    @ViewBuilder
    private func fieldView(_ field: BusinessSetupFieldSpec) -> some View {
        switch field.kind {
        case .panelHeader(let title):
            BusinessSetupDocumentKit.PanelHeader(theme: theme, title: title)
        case .text:
            if field.key == "gstState" {
                let selected = values[field.key] ?? ""
                let code = IndiaStateCatalog.codeOf(selected)
                let display: String = {
                    if selected.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty { return "" }
                    if !code.isEmpty {
                        return "\(IndiaStateCatalog.displayName(selected)) · \(code)"
                    }
                    return selected
                }()
                BusinessSetupDocumentKit.PickerRow(
                    theme: theme,
                    label: field.label,
                    value: display,
                    placeholder: field.placeholder.isEmpty ? "Select state" : field.placeholder,
                    helper: field.helper,
                    error: fieldErrors[field.key],
                    onTap: { showGstStatePicker = true }
                )
            } else {
                BusinessSetupDocumentKit.FieldRow(
                    theme: theme,
                    label: field.label,
                    placeholder: field.placeholder,
                    text: binding(for: field.key),
                    helper: field.helper,
                    error: fieldErrors[field.key],
                    forceUppercase: field.key == "gstin"
                )
            }
        case .choice(let options):
            if field.key == "currency" {
                BusinessSetupDocumentKit.CurrencyDropdown(
                    theme: theme,
                    label: field.label,
                    options: options,
                    selected: Binding(
                        get: {
                            let current = values[field.key] ?? ""
                            if current.isEmpty { return options.first ?? "" }
                            return current
                        },
                        set: { values[field.key] = $0; fieldErrors[field.key] = nil }
                    ),
                    helper: field.helper,
                    error: fieldErrors[field.key]
                )
            } else {
                VStack(alignment: .leading, spacing: 6) {
                    Text(field.label)
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(theme.label)
                    BusinessSetupDocumentKit.ChoiceChips(
                        theme: theme,
                        options: options,
                        selected: binding(for: field.key)
                    )
                    if !field.helper.isEmpty, fieldErrors[field.key] == nil {
                        Text(field.helper)
                            .font(.system(size: 11, weight: .regular))
                            .foregroundStyle(theme.hint)
                    }
                }
            }
        case .multiChoice(let options):
            VStack(alignment: .leading, spacing: 6) {
                Text(field.label)
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(theme.label)
                BusinessSetupDocumentKit.MultiChoiceChips(
                    theme: theme,
                    options: options,
                    selectedPipe: binding(for: field.key),
                    error: fieldErrors[field.key],
                    helper: field.helper
                )
            }
        case .toggle:
            BusinessSetupDocumentKit.ToggleRow(
                theme: theme,
                label: field.label,
                value: binding(for: field.key)
            )
        case .date:
            BusinessSetupDocumentKit.DateRow(
                theme: theme,
                label: field.label,
                placeholder: field.placeholder,
                text: binding(for: field.key),
                helper: field.helper,
                error: fieldErrors[field.key],
                fieldKey: field.key
            )
        case .rowAction(let action):
            BusinessSetupDocumentKit.RowAction(
                theme: theme,
                label: field.label,
                action: action
            )
        case .memberList(let members):
            let stored = values[field.key] ?? ""
            let list = stored.isEmpty
                ? members
                : stored.split(separator: "|").map(String.init)
            BusinessSetupDocumentKit.MemberList(theme: theme, members: list)
        case .moduleList(let modules):
            BusinessSetupDocumentKit.ModuleList(theme: theme, modules: modules, helper: field.helper)
        case .reviewSummary(let keys):
            BusinessSetupDocumentKit.ReviewSummary(
                theme: theme,
                keys: keys,
                values: values,
                labelsByKey: labelIndex
            )
        }
    }

    private func binding(for key: String) -> Binding<String> {
        Binding(
            get: { values[key] ?? "" },
            set: {
                values[key] = $0
                fieldErrors.removeValue(forKey: key)
            }
        )
    }

    private func reseeds(for code: String, priorTitle: String? = nil) {
        let next = BusinessSetupDocuments.spec(for: code)
        var seeded = next.defaultPreferences
        let carried = priorTitle?.trimmingCharacters(in: .whitespacesAndNewlines)
        if let carried, !carried.isEmpty {
            seeded[next.defaultTitleKey] = carried
        } else if let initialTitle, !initialTitle.isEmpty {
            seeded[next.defaultTitleKey] = initialTitle
        }
        for (key, value) in values where !value.isEmpty {
            if seeded.keys.contains(key) { seeded[key] = value }
        }
        if let logoId = values["logoMediaId"]?.trimmingCharacters(in: .whitespacesAndNewlines),
           !logoId.isEmpty {
            seeded["logoMediaId"] = logoId
        }
        values = seeded
    }

    private func preferencesPayload() -> [String: Any] {
        values.reduce(into: [String: Any]()) { partial, pair in
            let trimmed = pair.value.trimmingCharacters(in: .whitespacesAndNewlines)
            if !trimmed.isEmpty { partial[pair.key] = trimmed }
        }
    }

    private func activate() {
        fieldErrors = [:]
        draftSavedMessage = nil
        let company = linkedCompanyId ?? companyId
        createModel.submitBusinessSetup(
            kind: kind,
            companyId: company,
            preferences: preferencesPayload(),
            title: titleValue,
            subtype: typeCode,
            editingMomentId: draftMomentId,
            editingMomentStatus: draftMomentStatus,
            status: "ACTIVE",
            companyName: (company?.isEmpty != false) ? titleValue : nil,
            onSuccess: onCreated,
            onFieldErrors: { errors in
                fieldErrors = errors
            }
        )
    }

    private func saveDraft() {
        savingDraft = true
        draftSavedMessage = nil
        fieldErrors = [:]
        let existingId = draftMomentId
        let company = linkedCompanyId ?? companyId
        if let existingId,
           (draftMomentStatus ?? "").caseInsensitiveCompare("DRAFT") == .orderedSame {
            Task {
                let ok = await createModel.saveBusinessSetupDraft(
                    momentId: existingId,
                    title: titleValue,
                    preferences: preferencesPayload()
                )
                await MainActor.run {
                    savingDraft = false
                    draftSavedMessage = ok ? "Draft saved" : "Could not save draft"
                }
            }
        } else {
            createModel.submitBusinessSetup(
                kind: kind,
                companyId: company,
                preferences: preferencesPayload(),
                title: titleValue,
                subtype: typeCode,
                editingMomentId: draftMomentId,
                editingMomentStatus: draftMomentStatus,
                status: "DRAFT",
                companyName: (company?.isEmpty != false) ? titleValue : nil,
                onSuccess: { outcome in
                    savingDraft = false
                    draftMomentId = outcome.momentId
                    draftMomentStatus = "DRAFT"
                    draftSavedMessage = "Draft saved"
                },
                onFieldErrors: { _ in }
            )
        }
    }

    private func handleLogoPick(_ item: PhotosPickerItem) async {
        guard let picked = await decodeSetupLogo(item) else {
            await MainActor.run { logoError = "Could not open that photo" }
            return
        }
        await MainActor.run {
            logoImage = picked.image
            logoError = nil
            logoUploading = true
        }
        let ensured = await ensureDraftMomentForLogo()
        guard let ensured else {
            await MainActor.run {
                logoUploading = false
                logoError = "Could not upload logo"
            }
            return
        }
        if let company = ensured.companyId, !company.isEmpty {
            await MainActor.run { linkedCompanyId = company }
        }
        let companyName: String = await MainActor.run {
            values["companyName"]?.trimmingCharacters(in: .whitespacesAndNewlines).nilIfEmpty
                ?? values[spec.defaultTitleKey]?.trimmingCharacters(in: .whitespacesAndNewlines).nilIfEmpty
                ?? initialTitle?.trimmingCharacters(in: .whitespacesAndNewlines).nilIfEmpty
                ?? spec.heroTitle
        }
        let companyIdForUpload = await MainActor.run { linkedCompanyId ?? ensured.companyId }
        let result = await createModel.uploadSetupLogo(
            companyId: companyIdForUpload,
            momentId: ensured.momentId,
            companyName: companyName,
            bytes: picked.bytes
        )
        await MainActor.run {
            logoUploading = false
            switch result {
            case .success(let uploaded):
                linkedCompanyId = uploaded.companyId
                values["logoMediaId"] = uploaded.mediaId
                logoError = nil
            case .failure:
                logoError = "Could not upload logo"
            }
        }
    }

    /// Prefer in-session DRAFT create when draftMomentId is null so upload can complete
    /// without forcing Save draft first. Lockstep with Android `ensureDraftMomentForLogo`.
    private func ensureDraftMomentForLogo() async -> (momentId: String, companyId: String?)? {
        if let existing = draftMomentId?.trimmingCharacters(in: .whitespacesAndNewlines), !existing.isEmpty {
            let linked = await createModel.getDomainSetupPrefill(momentId: existing)?.companyId
            let company = SetupLogoCompanyResolver.preferExisting(
                passedCompanyId: linkedCompanyId ?? companyId,
                momentLinkedCompanyId: linked
            )
            return (existing, company)
        }
        if createModel.state.submitting { return nil }

        let prefs = await MainActor.run { preferencesPayload() }
        let title = await MainActor.run { titleValue }
        let company = await MainActor.run { linkedCompanyId ?? companyId }
        let status = await MainActor.run { draftMomentStatus }

        let momentId: String? = await withCheckedContinuation { continuation in
            var resumed = false
            createModel.submitBusinessSetup(
                kind: kind,
                companyId: company,
                preferences: prefs,
                title: title,
                subtype: typeCode,
                editingMomentId: nil,
                editingMomentStatus: status,
                status: "DRAFT",
                companyName: (company?.isEmpty != false) ? title : nil,
                onSuccess: { outcome in
                    guard !resumed else { return }
                    resumed = true
                    continuation.resume(returning: outcome.momentId)
                },
                onFieldErrors: { _ in
                    guard !resumed else { return }
                    resumed = true
                    continuation.resume(returning: nil)
                }
            )
            // Fail-safe if submit no-ops (already submitting) without callbacks.
            Task {
                try? await Task.sleep(nanoseconds: 15_000_000_000)
                guard !resumed else { return }
                resumed = true
                continuation.resume(returning: nil)
            }
        }
        guard let momentId else { return nil }
        let linked = await createModel.getDomainSetupPrefill(momentId: momentId)?.companyId
        await MainActor.run {
            draftMomentId = momentId
            draftMomentStatus = "DRAFT"
            if let linked, !linked.isEmpty { linkedCompanyId = linked }
        }
        return (momentId, linked)
    }

    private func loadDraftPreferences(momentId: String) async {
        guard let prefs = await createModel.loadBusinessSetupPreferences(momentId: momentId) else { return }
        await MainActor.run {
            for (key, value) in prefs {
                if let text = value as? String, !text.isEmpty {
                    values[key] = text
                } else if let list = value as? [Any] {
                    let joined = list.map { "\($0)" }.filter { !$0.isEmpty }.joined(separator: "|")
                    if !joined.isEmpty { values[key] = joined }
                }
            }
        }
    }
}

private extension String {
    var nilIfEmpty: String? {
        let t = trimmingCharacters(in: .whitespacesAndNewlines)
        return t.isEmpty ? nil : t
    }
}

private struct SetupLogoPick {
    let image: UIImage
    let bytes: Data
}

/// Mirror of Android `decodeSetupLogo` — JPEG bytes + preview image, max edge 1600.
private func decodeSetupLogo(_ item: PhotosPickerItem) async -> SetupLogoPick? {
    guard let data = try? await item.loadTransferable(type: Data.self),
          let image = UIImage(data: data) else { return nil }
    let largest = max(image.size.width, image.size.height)
    let scaled: UIImage
    if largest <= 1600 {
        scaled = image
    } else {
        let ratio = 1600 / largest
        let size = CGSize(
            width: max(1, image.size.width * ratio),
            height: max(1, image.size.height * ratio)
        )
        let renderer = UIGraphicsImageRenderer(size: size)
        scaled = renderer.image { _ in
            image.draw(in: CGRect(origin: .zero, size: size))
        }
    }
    guard let bytes = scaled.jpegData(compressionQuality: 0.85) else { return nil }
    return SetupLogoPick(image: scaled, bytes: bytes)
}
