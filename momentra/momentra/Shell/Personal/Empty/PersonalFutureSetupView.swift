import SwiftUI

/// Figma `353:6905` — full Future Building setup body.
struct PersonalFutureSetupView: View {
    var editingMomentId: String? = nil
    var initialTitle: String? = nil
    var onBack: () -> Void
    var onCreated: (String, String, String?) -> Void

    @StateObject private var createModel = MomentCreateModel()
    @State private var selections: [String: Any] = [:]
    @State private var showHabit2 = false
    @State private var momentTitle: String = ""
    @State private var localError: String?

    private let catalog = PersonalSetupCatalog.forKind(.futureBuilding)
    private let accent = PersonalSetupLongForm.teal

    private let sectionKeys: [[String]] = [
        ["building", "today", "primaryValue", "valueGrowth", "valueSecurity"],
        ["futureFeel", "focusHorizon", "progressRhythm", "mainFriction", "supportStyle"],
        ["momentumDriver", "habit2", "remindWeekly", "learningCheckIn", "focusTimeCheckIn", "reviewCadence"],
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
                PersonalSetupCloseRow(onBack: onBack, enabled: !createModel.state.submitting)
                PersonalSetupHeroBlock(
                    emoji: "🌱",
                    title: "Set up Future Building",
                    subtitle: catalog.subtitle,
                    accent: accent
                )
                SetupTitleField(
                    label: "Moment title",
                    value: $momentTitle,
                    placeholder: catalog.defaultTitle
                )

                PersonalSetupSectionCard(number: "01", title: "Building Basics", accent: accent) {
                    PersonalSetupStageHeader(
                        label: "Building focus",
                        key: "building",
                        options: ["Career growth", "Financial freedom", "Creative work", "Family chapter"],
                        selections: $selections
                    )
                    PersonalSetupDualPills(
                        label: "Today",
                        key: "today",
                        options: ["Making progress", "Building momentum"],
                        selections: $selections,
                        accent: accent
                    )
                    fieldLabel("YOUR FOCUS")
                    PersonalSetupInlineDropdown(
                        label: "Building", hint: "What are you building?", key: "building",
                        options: ["Career growth", "Financial freedom", "Creative work", "Family chapter"],
                        selections: $selections, accent: accent
                    )
                    PersonalSetupInlineDropdown(
                        label: "Today", hint: "Where are you now?", key: "today",
                        options: ["Making progress", "Just starting", "Stuck", "Restarting"],
                        selections: $selections, accent: accent
                    )
                    PersonalSetupInlineDropdown(
                        label: "Primary value", hint: "What matters most?", key: "primaryValue",
                        options: ["Freedom", "Growth", "Stability", "Impact"],
                        selections: $selections, accent: accent
                    )
                    fieldLabel("YOUR VALUES")
                    PersonalSetupInlineDropdown(
                        label: "Growth", hint: "Lean into expansion", key: "valueGrowth",
                        options: ["Selected", "Not selected"],
                        selections: $selections, accent: accent
                    )
                    PersonalSetupInlineDropdown(
                        label: "Security", hint: "Protect what you've built", key: "valueSecurity",
                        options: ["Selected", "Not selected"],
                        selections: $selections, accent: accent
                    )
                }

                PersonalSetupDiamondDivider(accent: accent)

                PersonalSetupSectionCard(number: "02", title: "Goals & Friction", accent: accent) {
                    PersonalSetupBorderedGroup(title: "What Matters Most", border: PersonalSetupLongForm.teal, glyph: "▣") {
                        PersonalSetupInlineDropdown(
                            label: "Building", hint: "Your chosen direction", key: "building",
                            options: ["Career growth", "Financial freedom", "Creative work", "Family chapter"],
                            selections: $selections, accent: PersonalSetupLongForm.teal
                        )
                        PersonalSetupInlineDropdown(
                            label: "Today", hint: "Your starting point", key: "today",
                            options: ["Making progress", "Just starting", "Stuck", "Restarting", "Building momentum"],
                            selections: $selections, accent: PersonalSetupLongForm.teal
                        )
                    }
                    PersonalSetupBorderedGroup(title: "Goals", border: PersonalSetupLongForm.blue, glyph: "◎") {
                        PersonalSetupInlineDropdown(
                            label: "Future feel", hint: "How should the future feel?", key: "futureFeel",
                            options: ["Hopeful", "Confident", "Calm", "Ambitious", "Grounded"],
                            selections: $selections, accent: PersonalSetupLongForm.blue
                        )
                        PersonalSetupInlineDropdown(
                            label: "Focus horizon", hint: "How far ahead?", key: "focusHorizon",
                            options: ["12 months", "6 months", "3 months", "This year"],
                            selections: $selections, accent: PersonalSetupLongForm.blue
                        )
                        PersonalSetupInlineDropdown(
                            label: "Progress rhythm", hint: "How often you move", key: "progressRhythm",
                            options: ["Weekly", "Daily", "Monthly"],
                            selections: $selections, accent: PersonalSetupLongForm.blue
                        )
                    }
                    PersonalSetupBorderedGroup(title: "Friction & Support", border: PersonalSetupLongForm.indigo, glyph: "⚠") {
                        PersonalSetupInlineDropdown(
                            label: "Main friction", hint: "What slows you down", key: "mainFriction",
                            options: ["Lack of time", "Lack of clarity", "Overcommitment", "Fear", "Distraction"],
                            selections: $selections, accent: PersonalSetupLongForm.indigo
                        )
                        PersonalSetupInlineDropdown(
                            label: "Support style", hint: "What keeps you moving", key: "supportStyle",
                            options: ["Daily progress", "Focus time", "Accountability", "Learning"],
                            selections: $selections, accent: PersonalSetupLongForm.indigo
                        )
                    }
                }

                PersonalSetupDiamondDivider(accent: accent)

                PersonalSetupSectionCard(number: "03", title: "Momentum & Outlook", accent: accent) {
                    PersonalSetupBorderedGroup(title: "Momentum Drivers", border: PersonalSetupLongForm.pink, glyph: "⚡") {
                        PersonalSetupInlineDropdown(
                            label: "Momentum driver", hint: "Selected driver", key: "momentumDriver",
                            options: ["Daily progress", "Focus time", "Accountability", "Learning"],
                            selections: $selections, accent: PersonalSetupLongForm.pink
                        )
                        if showHabit2 {
                            PersonalSetupInlineDropdown(
                                label: "Second driver", hint: "Another driver", key: "habit2",
                                options: ["Daily progress", "Focus time", "Accountability", "Learning"],
                                selections: $selections, accent: PersonalSetupLongForm.pink
                            )
                        }
                        if !showHabit2 {
                            PersonalSetupAddRow(label: "+ ADD another driver", accent: PersonalSetupLongForm.pink) {
                                showHabit2 = true
                                if (selections["habit2"] as? String ?? "").isEmpty {
                                    selections["habit2"] = "Focus time"
                                }
                            }
                        }
                    }
                    PersonalSetupBorderedGroup(title: "Outlook", border: PersonalSetupLongForm.blue, glyph: "◇") {
                        PersonalSetupInlineDropdown(
                            label: "Future feel", hint: "How you want to feel", key: "futureFeel",
                            options: ["Hopeful", "Confident", "Calm", "Ambitious", "Grounded"],
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
                            label: "Learning check-in", hint: "Notice growth early", key: "learningCheckIn",
                            options: ["Enabled", "Disabled"],
                            selections: $selections, accent: PersonalSetupLongForm.indigo
                        )
                        PersonalSetupInlineDropdown(
                            label: "Focus time check-in", hint: "Protect deep work", key: "focusTimeCheckIn",
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

                PersonalSetupSectionCard(number: "04", title: "Future Summary", accent: accent) {
                    VStack(alignment: .leading, spacing: 16) {
                        summaryRow("Building", personalSelectionString(selections, "building"), big: true)
                        summaryRow("Today", personalSelectionString(selections, "today"))
                        Rectangle().fill(PersonalSetupLongForm.border).frame(height: 1)
                        HStack {
                            Text("Future feel")
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundStyle(SetupTokens.savedGreen)
                            Spacer()
                            Text(personalSelectionString(selections, "futureFeel"))
                                .font(.system(size: 18, weight: .bold))
                                .foregroundStyle(PersonalSetupLongForm.blue)
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
                    readyLine: "Your future map is ready",
                    ctaLabel: catalog.activateLabel,
                    footerTagline: catalog.footerTagline,
                    ctaGradient: LinearGradient(
                        colors: [accent, PersonalSetupLongForm.blue],
                        startPoint: .leading, endPoint: .trailing
                    ),
                    submitting: createModel.state.submitting,
                    error: localError ?? createModel.state.error,
                    onActivate: activate
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
                guard let setup = try? await APIClient.shared.personalSetup(forMomentId: editingMomentId) else { return }
                selections = PersonalSetupEditPrefill.mergeDefaults(catalog.defaultPreferences, saved: setup.preferences)
                if initialTitle == nil || initialTitle?.isEmpty == true {
                    momentTitle = setup.title
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

    private func activate() {
        guard !createModel.state.submitting else { return }
        localError = nil
        createModel.submitPersonalSetup(
            kind: .futureBuilding,
            preferences: selections,
            title: momentTitle.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                ? catalog.defaultTitle
                : momentTitle,
            editingMomentId: editingMomentId
        ) { outcome in
            onCreated(outcome.momentId, outcome.title, outcome.momentTypeCode ?? catalog.momentTypeCode)
        }
    }
}
