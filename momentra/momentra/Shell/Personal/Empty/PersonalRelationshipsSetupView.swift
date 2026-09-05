import SwiftUI

/// Figma `353:7217` — full Relationships setup body.
struct PersonalRelationshipsSetupView: View {
    var editingMomentId: String? = nil
    var initialTitle: String? = nil
    var onBack: () -> Void
    var onCreated: (String, String, String?) -> Void

    @StateObject private var createModel = MomentCreateModel()
    @State private var selections: [String: Any] = [:]
    @State private var showHabit2 = false
    @State private var momentTitle: String = ""
    @State private var localError: String?

    private let catalog = PersonalSetupCatalog.forKind(.relationships)
    private let accent = PersonalSetupLongForm.pink

    private let sectionKeys: [[String]] = [
        ["relationshipFocus", "current", "primaryCircle", "partnerFamily", "friendsCommunity"],
        ["timeTogether", "reachOutRhythm", "communicationStyle", "strongestConnection", "needsInvestment"],
        ["ritual", "habit2", "desiredFeeling", "remindWeekly", "connectionCheckIn", "reachOutReminder", "reviewCadence"],
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
                    emoji: "💞",
                    title: "Set up Relationships",
                    subtitle: catalog.subtitle,
                    accent: accent
                )
                SetupTitleField(
                    label: "Moment title",
                    value: $momentTitle,
                    placeholder: catalog.defaultTitle
                )

                PersonalSetupSectionCard(number: "01", title: "Relationship Basics", accent: accent) {
                    PersonalSetupStageHeader(
                        label: "Relationship focus",
                        key: "relationshipFocus",
                        options: ["Deeper connection", "More presence", "Better repair", "Wider community"],
                        selections: $selections
                    )
                    PersonalSetupDualPills(
                        label: "Current",
                        key: "current",
                        options: ["Connected, but busy", "Making room for people"],
                        selections: $selections,
                        accent: accent
                    )
                    fieldLabel("YOUR CONNECTIONS")
                    PersonalSetupInlineDropdown(
                        label: "Relationship focus", hint: "What do you want more of?", key: "relationshipFocus",
                        options: ["Deeper connection", "More presence", "Better repair", "Wider community"],
                        selections: $selections, accent: accent
                    )
                    PersonalSetupInlineDropdown(
                        label: "Current", hint: "How connected do you feel?", key: "current",
                        options: [
                            "Connected, but busy",
                            "Growing",
                            "Repairing",
                            "Distant",
                            "Deepening",
                        ],
                        selections: $selections, accent: accent
                    )
                    PersonalSetupInlineDropdown(
                        label: "Primary circle", hint: "Who comes first?", key: "primaryCircle",
                        options: ["Family", "Friendship", "Partnership", "Community"],
                        selections: $selections, accent: accent
                    )
                    fieldLabel("YOUR CIRCLES")
                    PersonalSetupInlineDropdown(
                        label: "Partner & family", hint: "Closest bonds", key: "partnerFamily",
                        options: ["Selected", "Not selected"],
                        selections: $selections, accent: accent
                    )
                    PersonalSetupInlineDropdown(
                        label: "Friends & community", hint: "Wider circle", key: "friendsCommunity",
                        options: ["Selected", "Not selected"],
                        selections: $selections, accent: accent
                    )
                }

                PersonalSetupDiamondDivider(accent: accent)

                PersonalSetupSectionCard(number: "02", title: "People & Circles", accent: accent) {
                    PersonalSetupBorderedGroup(title: "Who Matters Most", border: PersonalSetupLongForm.teal, glyph: "▣") {
                        PersonalSetupInlineDropdown(
                            label: "Relationship focus", hint: "Your chosen priority", key: "relationshipFocus",
                            options: ["Deeper connection", "More presence", "Better repair", "Wider community"],
                            selections: $selections, accent: PersonalSetupLongForm.teal
                        )
                        PersonalSetupInlineDropdown(
                            label: "Current", hint: "Your starting point", key: "current",
                            options: [
                                "Connected, but busy",
                                "Making room for people",
                                "Growing",
                                "Repairing",
                                "Distant",
                                "Deepening",
                            ],
                            selections: $selections, accent: PersonalSetupLongForm.teal
                        )
                    }
                    PersonalSetupBorderedGroup(title: "Connection", border: PersonalSetupLongForm.orange, glyph: "⚠") {
                        PersonalSetupInlineDropdown(
                            label: "Time together", hint: "How you show up", key: "timeTogether",
                            options: ["Meaningful moments", "Quality time", "Fun", "Support"],
                            selections: $selections, accent: PersonalSetupLongForm.orange
                        )
                        PersonalSetupInlineDropdown(
                            label: "Reach-out rhythm", hint: "How often you connect", key: "reachOutRhythm",
                            options: ["Every week", "Daily", "Monthly"],
                            selections: $selections, accent: PersonalSetupLongForm.orange
                        )
                        PersonalSetupInlineDropdown(
                            label: "Communication style", hint: "How you relate", key: "communicationStyle",
                            options: ["Thoughtful check-ins", "Direct", "Playful", "Listening"],
                            selections: $selections, accent: PersonalSetupLongForm.orange
                        )
                    }
                    PersonalSetupBorderedGroup(title: "Strengths & Investment", border: SetupTokens.savedGreen, glyph: "♡") {
                        PersonalSetupInlineDropdown(
                            label: "Strongest connection", hint: "Where bonds are solid", key: "strongestConnection",
                            options: ["Family", "Friends", "Partner", "Community"],
                            selections: $selections, accent: SetupTokens.savedGreen
                        )
                        PersonalSetupInlineDropdown(
                            label: "Needs investment", hint: "Where to put more care", key: "needsInvestment",
                            options: ["Friends", "Family", "Partner", "Community"],
                            selections: $selections, accent: SetupTokens.savedGreen
                        )
                    }
                }

                PersonalSetupDiamondDivider(accent: accent)

                PersonalSetupSectionCard(number: "03", title: "Care & Outlook", accent: accent) {
                    PersonalSetupBorderedGroup(title: "Connection Anchors", border: PersonalSetupLongForm.pink, glyph: "⚡") {
                        PersonalSetupInlineDropdown(
                            label: "Ritual", hint: "Selected ritual", key: "ritual",
                            options: ["Weekly check-in", "Date night", "Call a friend", "Shared meal"],
                            selections: $selections, accent: PersonalSetupLongForm.pink
                        )
                        if showHabit2 {
                            PersonalSetupInlineDropdown(
                                label: "Second ritual", hint: "Another ritual", key: "habit2",
                                options: ["Weekly check-in", "Date night", "Call a friend", "Shared meal"],
                                selections: $selections, accent: PersonalSetupLongForm.pink
                            )
                        }
                        if !showHabit2 {
                            PersonalSetupAddRow(label: "+ ADD another ritual", accent: PersonalSetupLongForm.pink) {
                                showHabit2 = true
                                if (selections["habit2"] as? String ?? "").isEmpty {
                                    selections["habit2"] = "Date night"
                                }
                            }
                        }
                    }
                    PersonalSetupBorderedGroup(title: "Connection Outlook", border: PersonalSetupLongForm.blue, glyph: "◇") {
                        PersonalSetupInlineDropdown(
                            label: "Desired feeling", hint: "How you want to feel", key: "desiredFeeling",
                            options: ["Close & supported", "Playful", "Calm", "Deep"],
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
                            label: "Connection check-in", hint: "Notice closeness early", key: "connectionCheckIn",
                            options: ["Enabled", "Disabled"],
                            selections: $selections, accent: PersonalSetupLongForm.indigo
                        )
                        PersonalSetupInlineDropdown(
                            label: "Reach-out reminder", hint: "Protect time for people", key: "reachOutReminder",
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

                PersonalSetupSectionCard(number: "04", title: "Relationships Summary", accent: accent) {
                    VStack(alignment: .leading, spacing: 16) {
                        summaryRow(
                            "Relationship focus",
                            personalSelectionString(selections, "relationshipFocus"),
                            big: true
                        )
                        summaryRow("Current", personalSelectionString(selections, "current"))
                        Rectangle().fill(PersonalSetupLongForm.border).frame(height: 1)
                        HStack {
                            Text("Primary circle")
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundStyle(SetupTokens.savedGreen)
                            Spacer()
                            Text(personalSelectionString(selections, "primaryCircle"))
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
                    readyLine: "Your relationship plan is ready",
                    ctaLabel: catalog.activateLabel,
                    footerTagline: catalog.footerTagline,
                    ctaGradient: LinearGradient(
                        colors: [accent, Color(hex: "#F472B6")],
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
            kind: .relationships,
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
