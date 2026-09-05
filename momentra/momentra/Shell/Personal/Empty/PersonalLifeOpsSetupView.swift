import SwiftUI

/// Figma `353:6809` — full Life Operations setup body.
struct PersonalLifeOpsSetupView: View {
    var editingMomentId: String? = nil
    var initialTitle: String? = nil
    var onBack: () -> Void
    var onCreated: (String, String, String?, String) -> Void

    @StateObject private var createModel = MomentCreateModel()
    @State private var selections: [String: Any] = [:]
    @State private var category = "Financials"
    @State private var showHabit2 = false
    @State private var momentTitle: String = ""
    @State private var editingMomentStatus: String?

    @State private var localError: String?

    private let catalog = PersonalSetupCatalog.forKind(.lifeOperations)
    private let accent = SetupTokens.accentPurple

    private let sectionKeys: [[String]] = [
        ["lifeFocus", "currentRhythm", "primaryNeed", "healthEnergy", "timeBalance"],
        ["shapesFocus", "shapesRhythm", "mainPressure", "recoveryWindow", "checkInRhythm", "helpfulSupport", "recoveryStyle"],
        ["habit", "habit2", "currentEnergy", "reflectWeekly", "stressCheckIn", "recoveryCheckIn", "reviewCadence"],
    ]

    private let tabToSection: [String: String] = [
        "Financials": "01",
        "Wellbeing": "02",
        "Routine": "03",
        "Personal": "04",
    ]

    private var statusLine: String {
        personalSetupStatusLine(
            defaults: catalog.defaultPreferences,
            selections: selections,
            sectionKeys: sectionKeys
        )
    }

    var body: some View {
        ScrollViewReader { proxy in
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
                        emoji: "🚀",
                        title: "Set up Life Operations",
                        subtitle: catalog.subtitle,
                        accent: accent
                    )
                    SetupTitleField(
                        label: "Moment title",
                        value: $momentTitle,
                        placeholder: catalog.defaultTitle
                    )

                    PersonalSetupCategoryTabs(
                        tabs: [
                            .init(label: "Financials", icon: "💳", accents: [Color(hex: "#4CD6FF"), accent]),
                            .init(label: "Wellbeing", icon: "♡", accents: [SetupTokens.savedGreen, Color(hex: "#A78BFA")]),
                            .init(label: "Routine", icon: "⏱", accents: [PersonalSetupLongForm.amber, PersonalSetupLongForm.orange]),
                            .init(label: "Personal", icon: "👤", accents: [Color(hex: "#F2CA50"), PersonalSetupLongForm.orange]),
                        ],
                        selected: category,
                        onSelect: { label in
                            category = label
                            if let id = tabToSection[label] {
                                withAnimation(.easeInOut(duration: 0.35)) {
                                    proxy.scrollTo(id, anchor: .top)
                                }
                            }
                        }
                    )

                    PersonalSetupSectionCard(number: "01", title: "Life Basics", accent: accent) {
                        PersonalSetupStageHeader(
                            label: "Life focus",
                            key: "lifeFocus",
                            options: ["Daily balance", "Career focus", "Family first", "Health reset"],
                            selections: $selections
                        )
                        PersonalSetupDualPills(
                            label: "Current rhythm",
                            key: "currentRhythm",
                            options: ["Busy but manageable", "Building steadiness"],
                            selections: $selections,
                            accent: accent
                        )
                        fieldLabel("YOUR FOUNDATION")
                        PersonalSetupInlineDropdown(
                            label: "Life focus", hint: "What needs attention?", key: "lifeFocus",
                            options: ["Daily balance", "Career focus", "Family first", "Health reset"],
                            selections: $selections, accent: accent
                        )
                        PersonalSetupInlineDropdown(
                            label: "Current pressure", hint: "How does life feel?", key: "currentRhythm",
                            options: ["Busy but manageable", "Overwhelmed", "Calm", "Building steadiness"],
                            selections: $selections, accent: accent
                        )
                        PersonalSetupInlineDropdown(
                            label: "Primary need", hint: "What would help most?", key: "primaryNeed",
                            options: ["More breathing room", "Clearer structure", "More energy", "Better support"],
                            selections: $selections, accent: accent
                        )
                        fieldLabel("YOUR PRIORITIES")
                        PersonalSetupInlineDropdown(
                            label: "Health & energy", hint: "Feel stronger day to day", key: "healthEnergy",
                            options: ["Selected", "Not selected"],
                            selections: $selections, accent: accent
                        )
                        PersonalSetupInlineDropdown(
                            label: "Time & balance", hint: "Create room for what matters", key: "timeBalance",
                            options: ["Selected", "Not selected"],
                            selections: $selections, accent: accent
                        )
                    }
                    .id("01")

                    PersonalSetupDiamondDivider(accent: accent)

                    PersonalSetupSectionCard(number: "02", title: "Pressures & Supports", accent: accent) {
                        PersonalSetupBorderedGroup(title: "What Shapes Your Days", border: PersonalSetupLongForm.teal, glyph: "▣") {
                            PersonalSetupInlineDropdown(
                                label: "Life focus", hint: "Your chosen priority", key: "shapesFocus",
                                options: ["Daily balance", "Career focus", "Family first", "Health reset"],
                                selections: $selections, accent: PersonalSetupLongForm.teal
                            )
                            PersonalSetupInlineDropdown(
                                label: "Current rhythm", hint: "Your starting point", key: "shapesRhythm",
                                options: ["Busy but manageable", "Building steadiness", "Overwhelmed", "Calm"],
                                selections: $selections, accent: PersonalSetupLongForm.teal
                            )
                        }
                        PersonalSetupBorderedGroup(title: "Pressure", border: PersonalSetupLongForm.orange, glyph: "⚠") {
                            PersonalSetupInlineDropdown(
                                label: "Main pressure", hint: "What drains you now", key: "mainPressure",
                                options: ["Too many commitments", "Money stress", "Health load", "Work overload"],
                                selections: $selections, accent: PersonalSetupLongForm.orange
                            )
                            PersonalSetupInlineDropdown(
                                label: "Recovery window", hint: "When you can reset", key: "recoveryWindow",
                                options: ["Weekends", "Evenings", "Mornings", "Anytime"],
                                selections: $selections, accent: PersonalSetupLongForm.orange
                            )
                            PersonalSetupInlineDropdown(
                                label: "Check-in rhythm", hint: "How often to pause", key: "checkInRhythm",
                                options: ["Weekly", "Daily", "Monthly"],
                                selections: $selections, accent: PersonalSetupLongForm.orange
                            )
                        }
                        PersonalSetupBorderedGroup(title: "Support & Recovery", border: SetupTokens.savedGreen, glyph: "♡") {
                            PersonalSetupInlineDropdown(
                                label: "Helpful support", hint: "What makes life easier", key: "helpfulSupport",
                                options: ["Clear routines", "Accountability", "Quiet mornings", "Community"],
                                selections: $selections, accent: SetupTokens.savedGreen
                            )
                            PersonalSetupInlineDropdown(
                                label: "Recovery style", hint: "How you recharge", key: "recoveryStyle",
                                options: ["Quiet time", "Walks", "Sleep", "Movement", "Nature"],
                                selections: $selections, accent: SetupTokens.savedGreen
                            )
                        }
                    }
                    .id("02")

                    PersonalSetupDiamondDivider(accent: accent)

                    PersonalSetupSectionCard(number: "03", title: "Habits & Energy", accent: accent) {
                        PersonalSetupBorderedGroup(title: "Energy Anchors", border: PersonalSetupLongForm.pink, glyph: "⚡") {
                            PersonalSetupInlineDropdown(
                                label: "Morning routine", hint: "Selected anchor", key: "habit",
                                options: ["Morning routine", "Planning routine", "Evening wind-down", "Movement block"],
                                selections: $selections, accent: PersonalSetupLongForm.pink
                            )
                            if showHabit2 {
                                PersonalSetupInlineDropdown(
                                    label: "Second habit", hint: "Another anchor", key: "habit2",
                                    options: ["Morning routine", "Planning routine", "Evening wind-down", "Movement block"],
                                    selections: $selections, accent: PersonalSetupLongForm.pink
                                )
                            }
                            if !showHabit2 {
                                PersonalSetupAddRow(label: "+ ADD another habit", accent: PersonalSetupLongForm.pink) {
                                    showHabit2 = true
                                    if (selections["habit2"] as? String ?? "").isEmpty {
                                        selections["habit2"] = "Planning routine"
                                    }
                                }
                            }
                        }
                        PersonalSetupBorderedGroup(title: "Wellbeing", border: PersonalSetupLongForm.blue, glyph: "◇") {
                            PersonalSetupInlineDropdown(
                                label: "Current energy", hint: "How you feel most days", key: "currentEnergy",
                                options: ["Steady", "Low", "High", "Variable"],
                                selections: $selections, accent: PersonalSetupLongForm.blue
                            )
                            toggleRow(
                                title: "Remind me weekly",
                                subtitle: "Keep a light pulse check",
                                key: "reflectWeekly",
                                tint: PersonalSetupLongForm.blue
                            )
                        }
                        PersonalSetupBorderedGroup(title: "Reflection Preferences", border: PersonalSetupLongForm.indigo, glyph: "✦") {
                            PersonalSetupInlineDropdown(
                                label: "Stress check-in", hint: "Notice pressure early", key: "stressCheckIn",
                                options: ["Enabled", "Disabled"],
                                selections: $selections, accent: PersonalSetupLongForm.indigo
                            )
                            PersonalSetupInlineDropdown(
                                label: "Recovery check-in", hint: "Protect time to recharge", key: "recoveryCheckIn",
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
                    .id("03")

                    PersonalSetupDiamondDivider(accent: accent)

                    PersonalSetupSectionCard(number: "04", title: "Life Summary", accent: accent) {
                        VStack(alignment: .leading, spacing: 16) {
                            summaryRow("Life focus", personalSelectionString(selections, "lifeFocus"), big: true)
                            summaryRow("Current rhythm", personalSelectionString(selections, "currentRhythm"))
                            Rectangle().fill(PersonalSetupLongForm.border).frame(height: 1)
                            HStack {
                                Text("Energy")
                                    .font(.system(size: 15, weight: .semibold))
                                    .foregroundStyle(SetupTokens.savedGreen)
                                Spacer()
                                Text(personalSelectionString(selections, "currentEnergy"))
                                    .font(.system(size: 18, weight: .bold))
                                    .foregroundStyle(PersonalSetupLongForm.blue)
                            }
                            PersonalSetupSummaryProgress(accent: Color(hex: "#A286FA"))
                        }
                        .padding(24)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .overlay(RoundedRectangle(cornerRadius: 16).stroke(accent))
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                    }
                    .id("04")

                    PersonalSetupActivateBlock(
                        statusLine: statusLine,
                        readyLine: "Your life plan is ready",
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
                    if initialTitle == nil || initialTitle?.isEmpty == true { momentTitle = prefill.title }
                } else if let setup = try? await APIClient.shared.personalSetup(forMomentId: editingMomentId) {
                    editingMomentStatus = setup.status
                    selections = PersonalSetupEditPrefill.mergeDefaults(
                        catalog.defaultPreferences,
                        saved: setup.preferences
                    )
                    if initialTitle == nil || initialTitle?.isEmpty == true { momentTitle = setup.title }
                }
                let loadedHabit2 = selections["habit2"] as? String ?? ""
                showHabit2 = !loadedHabit2.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            }
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

    private func submit(status: String) {
        guard !createModel.state.submitting else { return }
        localError = nil
        createModel.submitPersonalSetup(
            kind: .lifeOperations,
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
}
