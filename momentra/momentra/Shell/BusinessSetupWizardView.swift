import SwiftUI

/// Interactive Business setup wizard — Android `BusinessSetupWizardContent` parity.
struct BusinessSetupWizardView: View {
    let kind: BusinessSetupKind
    @ObservedObject var createModel: MomentCreateModel
    let companyId: String
    var editingMomentId: String? = nil
    var initialTitle: String? = nil
    var onClose: () -> Void
    var onCreated: (CreateMomentOutcome) -> Void

    @State private var selections: [String: Any] = [:]
    @State private var momentTitle: String = ""

    private var catalog: BusinessSetupCatalogEntry {
        BusinessSetupCatalog.forKind(kind)
    }

    var body: some View {
        SetupWizardScaffold(backgroundColor: SetupTokens.bizBg) {
            VStack(alignment: .leading, spacing: 20) {
                SetupWizardHeader(
                    title: kind.title,
                    durationLabel: catalog.subtitle,
                    onClose: onClose,
                    enabled: !createModel.state.submitting
                )

                SetupTitleField(
                    label: "Moment title",
                    value: $momentTitle,
                    placeholder: catalog.defaultTitle
                )
                .padding(.horizontal, 16)

                ForEach(Array(catalog.sections.enumerated()), id: \.offset) { index, section in
                    businessSectionCard(index: index, section: section)
                }

                businessPreview
                    .padding(.horizontal, 16)

                if let error = createModel.state.error {
                    Text(error)
                        .font(.system(size: 13))
                        .foregroundStyle(SetupTokens.error)
                        .padding(.horizontal, 16)
                }
            }
        } footer: {
            SetupStickyFooter(
                tagline: catalog.footerTagline,
                ctaLabel: catalog.activateLabel,
                onCta: activate,
                submitting: createModel.state.submitting,
                accentGradient: kind.ctaGradient,
                backgroundColor: SetupTokens.bizBg
            )
            .accessibilityIdentifier("business.setup.submit")
        }
        .accessibilityIdentifier(kind.maestroTag)
        .trackScreen(kind.analyticsScreen)
        .onAppear {
            selections = catalog.defaultPreferences
            momentTitle = (initialTitle?.isEmpty == false) ? initialTitle! : catalog.defaultTitle
        }
    }

    @ViewBuilder
    private func businessSectionCard(index: Int, section: BusinessSetupSectionSpec) -> some View {
        VStack(alignment: .leading, spacing: 14) {
            Text(String(format: "%02d · %@", index + 1, section.title.uppercased()))
                .font(.system(size: 13, weight: .bold))
                .foregroundStyle(SetupTokens.bizAccent)

            ForEach(section.fields, id: \.key) { field in
                SetupPrefField(field: field, selections: $selections, selectedChipColor: kind.activateColor)
            }

            ForEach(section.textFields, id: \.key) { field in
                textField(field)
            }
        }
        .padding(16)
        .background(SetupTokens.bizCard, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(Color(hex: "#1E293B"), lineWidth: 1)
        )
        .padding(.horizontal, 16)
    }

    @ViewBuilder
    private func textField(_ field: BusinessSetupTextFieldSpec) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(field.label)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(Color(hex: "#E2E8F0"))
            TextField(
                field.label,
                text: Binding(
                    get: { selections[field.key] as? String ?? "" },
                    set: { selections[field.key] = $0 }
                )
            )
            .textFieldStyle(.plain)
            .font(.system(size: 15))
            .foregroundStyle(.white)
            .padding(12)
            .background(Color(hex: "#0F172A"), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .stroke(Color(hex: "#1E293B"), lineWidth: 1)
            )
            .accessibilityIdentifier("setup.text.\(field.key)")
        }
    }

    @ViewBuilder
    private var businessPreview: some View {
        switch kind {
        case .teamOperations:
            SetupPreviewCard(
                title: "Team Ops Preview",
                subtitle: "\(selections["teamName"] as? String ?? "") · \(selections["workMode"] as? String ?? "") · \(selections["size"] as? String ?? "")",
                bullets: [
                    "Reviews: \(selections["reviewCycle"] as? String ?? "")",
                    "Approval threshold: \(selections["approvalThreshold"] as? String ?? "")",
                ]
            )
        case .businessRunway:
            SetupPreviewCard(
                title: "Runway Preview",
                subtitle: "Cash \(selections["availableCash"] as? String ?? "") · Spend \(selections["monthlySpending"] as? String ?? "")/mo",
                bullets: [
                    "Revenue: \(selections["monthlyRevenue"] as? String ?? "") (\(selections["revenueStage"] as? String ?? ""))",
                    "Warning at \(selections["warningThreshold"] as? String ?? "") runway",
                ]
            )
        case .businessOperations:
            SetupPreviewCard(
                title: "Ops Monitoring Preview",
                subtitle: "\(selections["scope"] as? String ?? "") · \(selections["model"] as? String ?? "") · \(selections["cadence"] as? String ?? "") cadence",
                bullets: [
                    "Budget: \(selections["monthlyBudget"] as? String ?? "")",
                    "Alarm threshold: \(selections["approvalAlarm"] as? String ?? "")",
                ]
            )
        }
    }

    private func activate() {
        let title = momentTitle.trimmingCharacters(in: .whitespacesAndNewlines)
        createModel.submitBusinessSetup(
            kind: kind,
            companyId: companyId,
            preferences: selections,
            title: title.isEmpty ? catalog.defaultTitle : title,
            editingMomentId: editingMomentId
        ) { outcome in
            onCreated(outcome)
        }
    }
}
