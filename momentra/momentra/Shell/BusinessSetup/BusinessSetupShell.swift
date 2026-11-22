import SwiftUI

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
                        title: spec.heroTitle,
                        subtitle: spec.heroSubtitle
                    )
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
                                    companyId: companyId,
                                    embedded: true
                                ) { id in
                                    await createModel.mintCompanyInvite(companyId: id)
                                }
                            }
                        }
                    }
                    Color.clear.frame(height: 120)
                }
                .padding(.horizontal, 20)
                .padding(.top, 8)
            }

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
            BusinessSetupDocumentKit.FieldRow(
                theme: theme,
                label: field.label,
                placeholder: field.placeholder,
                text: binding(for: field.key),
                helper: field.helper,
                error: fieldErrors[field.key]
            )
        case .choice(let options):
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
                error: fieldErrors[field.key]
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
            BusinessSetupDocumentKit.ModuleList(theme: theme, modules: modules)
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
        createModel.submitBusinessSetup(
            kind: kind,
            companyId: companyId,
            preferences: preferencesPayload(),
            title: titleValue,
            subtype: typeCode,
            editingMomentId: draftMomentId,
            editingMomentStatus: draftMomentStatus,
            status: "ACTIVE",
            companyName: (companyId?.isEmpty != false) ? titleValue : nil,
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
                companyId: companyId,
                preferences: preferencesPayload(),
                title: titleValue,
                subtype: typeCode,
                editingMomentId: draftMomentId,
                editingMomentStatus: draftMomentStatus,
                status: "DRAFT",
                companyName: (companyId?.isEmpty != false) ? titleValue : nil,
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
