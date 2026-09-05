import SwiftUI

/// Figma `353:7075` — full Lifestyle setup body.
struct PersonalLifestyleSetupView: View {
    var editingMomentId: String? = nil
    var initialTitle: String? = nil
    var onBack: () -> Void
    var onCreated: (String, String, String?, String) -> Void

    @StateObject private var createModel = MomentCreateModel()
    @State private var selections: [String: Any] = [:]
    @State private var showHabit2 = false
    @State private var momentTitle: String = ""
    @State private var editingMomentStatus: String?
    @State private var localError: String?

    private let catalog = PersonalSetupCatalog.forKind(.lifestyle)
    private let accent = Color(hex: "#7C5CFC")

    private let sectionKeys: [[String]] = [
        ["vision", "current", "primaryPriority", "workLifeBalance", "homeEnvironment"],
        ["healthEnergy", "socialRhythm", "homeRhythm", "topPriority", "neglectedArea"],
        ["habit", "habit2", "desiredFeeling", "remindWeekly", "energyCheckIn", "balanceCheckIn", "reviewCadence"],
    ]

    private var statusLine: String {
        personalSetupStatusLine(
            defaults: catalog.defaultPreferences,
            selections: selections,
            sectionKeys: sectionKeys
        )
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 8) {
                PersonalSetupCloseRow(
                    onBack: {
                        personalHandleSetupDiscard(
                            createModel: createModel,
                            editingMomentId: editingMomentId,
                            editingMomentStatus: editingMomentStatus,
                            onBack: onBack
                        )
                    },
                    enabled: !createModel.state.submitting
                )
                PersonalSetupHeroBlock(
                    emoji: "✨",
                    title: "Set up Lifestyle",
                    subtitle: catalog.subtitle,
                    accent: accent
                )
                SetupTitleField(
                    label: "Moment title",
                    value: $momentTitle,
                    placeholder: catalog.defaultTitle
                )

                PersonalSetupSectionCard(number: "01", title: "Lifestyle Basics", accent: accent) {
                    PersonalSetupStageHeader(
                        label: "Lifestyle vision",
                        key: "vision",
                        options: ["Balanced & energized", "Minimal & calm", "Adventurous", "Grounded"],
                        selections: $selections
                    )
                    PersonalSetupDualPills(
                        label: "Current",
                        key: "current",
                        options: ["Good, with room to grow", "Designing more balance"],
                        selections: $selections,
                        accent: accent
                    )
                    fieldLabel("YOUR LIFESTYLE")
                    PersonalSetupInlineDropdown(
                        label: "Vision", hint: "How do you want life to feel?", key: "vision",
                        options: ["Balanced & energized", "Minimal & calm", "Adventurous", "Grounded"],
                        selections: $selections, accent: accent
                    )
                    PersonalSetupInlineDropdown(
                        label: "Current", hint: "Where are you today?", key: "current",
                        options: [
                            "Good, with room to grow",
                            "Steady",
                            "Transition",
                            "Overloaded",
                            "Renewing",
                        ],
                        selections: $selections, accent: accent
                    )
                    PersonalSetupInlineDropdown(
                        label: "Primary priority", hint: "What comes first?", key: "primaryPriority",
                        options: ["Health & energy", "Connection", "Work", "Creativity", "Home"],
                        selections: $selections, accent: accent
                    )
                    fieldLabel("YOUR PREFERENCES")
                    PersonalSetupInlineDropdown(
                        label: "Work–life balance", hint: "Protect room to breathe", key: "workLifeBalance",
                        options: ["Selected", "Not selected"],
                        selections: $selections, accent: accent
                    )
                    PersonalSetupInlineDropdown(
                        label: "Home environment", hint: "Shape where you live", key: "homeEnvironment",
                        options: ["Selected", "Not selected"],
                        selections: $selections, accent: accent
                    )
                }

                PersonalSetupDiamondDivider(accent: accent)

                PersonalSetupSectionCard(number: "02", title: "Preferences & Balance", accent: accent) {
                    PersonalSetupBorderedGroup(title: "How You Want to Live", border: PersonalSetupLongForm.teal, glyph: "▣") {
                        PersonalSetupInlineDropdown(
                            label: "Vision", hint: "Your chosen direction", key: "vision",
                            options: ["Balanced & energized", "Minimal & calm", "Adventurous", "Grounded"],
                            selections: $selections, accent: PersonalSetupLongForm.teal
                        )
                        PersonalSetupInlineDropdown(
                            label: "Current", hint: "Your starting point", key: "current",
                            options: [
                                "Good, with room to grow",
                                "Designing more balance",
                                "Steady",
                                "Transition",
                                "Overloaded",
                                "Renewing",
                            ],
                            selections: $selections, accent: PersonalSetupLongForm.teal
                        )
                    }
                    PersonalSetupBorderedGroup(title: "Balance", border: PersonalSetupLongForm.orange, glyph: "⚠") {
                        PersonalSetupInlineDropdown(
                            label: "Health & energy", hint: "How strong you feel", key: "healthEnergy",
                            options: ["Strong and consistent", "Steady", "Variable", "Low"],
                            selections: $selections, accent: PersonalSetupLongForm.orange
                        )
                        PersonalSetupInlineDropdown(
                            label: "Social rhythm", hint: "How often you connect", key: "socialRhythm",
                            options: ["A few times a week", "Daily", "Weekends", "Rarely"],
                            selections: $selections, accent: PersonalSetupLongForm.orange
                        )
                        PersonalSetupInlineDropdown(
                            label: "Home rhythm", hint: "How home feels", key: "homeRhythm",
                            options: ["Calm & organized", "Busy", "Minimal", "Creative"],
                            selections: $selections, accent: PersonalSetupLongForm.orange
                        )
                    }
                    PersonalSetupBorderedGroup(title: "Priorities & Focus", border: SetupTokens.savedGreen, glyph: "♡") {
                        PersonalSetupInlineDropdown(
                            label: "Top priority", hint: "Where attention goes", key: "topPriority",
                            options: ["Health & energy", "Connection", "Work", "Creativity", "Home"],
                            selections: $selections, accent: SetupTokens.savedGreen
                        )
                        PersonalSetupInlineDropdown(
                            label: "Neglected area", hint: "What needs more care", key: "neglectedArea",
                            options: ["Rest & recovery", "Self care", "Social time", "Planning"],
                            selections: $selections, accent: SetupTokens.savedGreen
                        )
                    }
                }

                PersonalSetupDiamondDivider(accent: accent)

                PersonalSetupSectionCard(number: "03", title: "Habits & Outlook", accent: accent) {
                    PersonalSetupBorderedGroup(title: "Lifestyle Anchors", border: PersonalSetupLongForm.pink, glyph: "⚡") {
                        PersonalSetupInlineDropdown(
                            label: "Habit", hint: "Selected anchor", key: "habit",
                            options: ["Movement routine", "Sleep routine", "Nutrition", "Learning", "Rest"],
                            selections: $selections, accent: PersonalSetupLongForm.pink
                        )
                        if showHabit2 {
                            PersonalSetupInlineDropdown(
                                label: "Second habit", hint: "Another anchor", key: "habit2",
                                options: ["Movement routine", "Sleep routine", "Nutrition", "Learning", "Rest"],
                                selections: $selections, accent: PersonalSetupLongForm.pink
                            )
                        }
                        if !showHabit2 {
                            PersonalSetupAddRow(label: "+ ADD another habit", accent: PersonalSetupLongForm.pink) {
                                showHabit2 = true
                                if (selections["habit2"] as? String ?? "").isEmpty {
                                    selections["habit2"] = "Sleep routine"
                                }
                            }
                        }
                    }
                    PersonalSetupBorderedGroup(title: "Future Lifestyle", border: PersonalSetupLongForm.blue, glyph: "◇") {
                        PersonalSetupInlineDropdown(
                            label: "Desired feeling", hint: "How you want to feel", key: "desiredFeeling",
                            options: ["Balanced", "Minimal", "Adventurous", "Grounded"],
                            selections: $selections, accent: PersonalSetupLongForm.blue
                        )
                        toggleRow(
                            title: "Remind me weekly",
                            subtitle: "Keep a light pulse check",
                            key: "remindWeekly",
                            tint: PersonalSetupLongForm.blue
                        )
                    }
                    PersonalSetupBorderedGroup(title: "Reflection Preferences", border: PersonalSetupLongForm.indigo, glyph: "✦") {
                        PersonalSetupInlineDropdown(
                            label: "Energy check-in", hint: "Notice energy early", key: "energyCheckIn",
                            options: ["Enabled", "Disabled"],
                            selections: $selections, accent: PersonalSetupLongForm.indigo
                        )
                        PersonalSetupInlineDropdown(
                            label: "Balance check-in", hint: "Protect life balance", key: "balanceCheckIn",
                            options: ["Enabled", "Disabled"],
                            selections: $selections, accent: PersonalSetupLongForm.indigo
                        )
                        PersonalSetupInlineDropdown(
                            label: "Review cadence", hint: "How often to reflect", key: "reviewCadence",
                            options: ["Every week", "Every month", "On demand"],
                            selections: $selections, accent: PersonalSetupLongForm.indigo
                        )
                    }
                }

                PersonalSetupDiamondDivider(accent: accent)

                PersonalSetupSectionCard(number: "04", title: "Lifestyle Summary", accent: accent) {
                    VStack(alignment: .leading, spacing: 16) {
                        summaryRow("Vision", personalSelectionString(selections, "vision"), big: true)
                        summaryRow("Current", personalSelectionString(selections, "current"))
                        Rectangle().fill(PersonalSetupLongForm.border).frame(height: 1)
                        HStack {
                            Text("Top priority")
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundStyle(SetupTokens.savedGreen)
                            Spacer()
                            Text(personalSelectionString(selections, "topPriority"))
                                .font(.system(size: 18, weight: .bold))
                                .foregroundStyle(accent)
                        }
                        PersonalSetupSummaryProgress(accent: accent)
                    }
                    .padding(24)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .overlay(RoundedRectangle(cornerRadius: 16).stroke(accent))
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                }

                PersonalSetupActivateBlock(
                    statusLine: statusLine,
                    readyLine: "Your lifestyle plan is ready",
                    ctaLabel: catalog.activateLabel,
                    footerTagline: catalog.footerTagline,
                    ctaGradient: LinearGradient(
                        colors: [accent, Color(hex: "#F472B6")],
                        startPoint: .leading, endPoint: .trailing
                    ),
                    submitting: createModel.state.submitting,
                    error: localError ?? createModel.state.error,
                    onSaveDraft: { submit(status: "DRAFT") },
                    onActivate: { submit(status: "ACTIVE") }
                )
            }
            .padding(.horizontal, 24)
            .padding(.vertical, 16)
        }
        .background(SetupTokens.bgPrimary.ignoresSafeArea())
        .onAppear {
            selections = catalog.defaultPreferences
            momentTitle = (initialTitle?.isEmpty == false) ? initialTitle! : catalog.defaultTitle
            let habit2 = selections["habit2"] as? String ?? ""
            showHabit2 = !habit2.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            guard let editingMomentId else { return }
            Task {
                if let prefill = await createModel.getDomainSetupPrefill(momentId: editingMomentId) {
                    editingMomentStatus = prefill.status
                    selections = PersonalSetupEditPrefill.mergeDefaults(
                        catalog.defaultPreferences,
                        saved: prefill.preferences
                    )
                    if initialTitle == nil || initialTitle?.isEmpty == true {
                        momentTitle = prefill.title
                    }
                } else if let setup = try? await APIClient.shared.personalSetup(forMomentId: editingMomentId) {
                    editingMomentStatus = setup.status
                    selections = PersonalSetupEditPrefill.mergeDefaults(
                        catalog.defaultPreferences,
                        saved: setup.preferences
                    )
                    if initialTitle == nil || initialTitle?.isEmpty == true {
                        momentTitle = setup.title
                    }
                }
                let loadedHabit2 = selections["habit2"] as? String ?? ""
                showHabit2 = !loadedHabit2.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            }
        }
    }

    private func submit(status: String) {
        guard !createModel.state.submitting else { return }
        localError = nil
        createModel.submitPersonalSetup(
            kind: .lifestyle,
            preferences: selections,
            title: momentTitle.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                ? catalog.defaultTitle
                : momentTitle,
            editingMomentId: editingMomentId,
            editingMomentStatus: editingMomentStatus,
            status: status
        ) { outcome in
            editingMomentStatus = outcome.status
            onCreated(
                outcome.momentId,
                outcome.title,
                outcome.momentTypeCode ?? catalog.momentTypeCode,
                outcome.status
            )
        }
    }

    private func fieldLabel(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 11, weight: .semibold))
            .foregroundStyle(SetupTokens.textSecondary)
            .padding(.top, 8)
    }

    private func toggleRow(title: String, subtitle: String, key: String, tint: Color) -> some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(title).font(.system(size: 14)).foregroundStyle(SetupTokens.textSecondary)
                Text(subtitle).font(.system(size: 12)).foregroundStyle(SetupTokens.textSecondary)
            }
            Spacer()
            Toggle("", isOn: Binding(
                get: { selections[key] as? Bool ?? false },
                set: { selections[key] = $0 }
            ))
            .labelsHidden()
            .tint(tint)
        }
        .padding(.vertical, 6)
    }

    private func summaryRow(_ label: String, _ value: String, big: Bool = false) -> some View {
        HStack {
            Text(label).font(.system(size: 14)).foregroundStyle(SetupTokens.textSecondary)
            Spacer()
            Text(value)
                .font(.system(size: big ? 22 : 15, weight: big ? .bold : .semibold))
                .foregroundStyle(SetupTokens.textPrimary)
        }
    }
}
