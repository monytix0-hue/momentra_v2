import SwiftUI

struct MomentEmptyConfig {
    var eyebrow: String? = nil
    var title: String
    var body: String
    var primaryLabel: String? = nil
    var onPrimary: (() -> Void)? = nil
    var secondaryLabel: String? = nil
    var onSecondary: (() -> Void)? = nil
    var historyTitle: String? = nil
    var history: [MomentSummary] = []
    var accent: Color = Color(hex: "#7C5CFC")
}

struct MomentEmptyStateView: View {
    let config: MomentEmptyConfig

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                if let eyebrow = config.eyebrow {
                    Text(eyebrow.uppercased())
                        .font(.system(size: 11, weight: .bold))
                        .foregroundStyle(config.accent)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 5)
                        .background(config.accent.opacity(0.12))
                        .overlay(
                            Capsule().stroke(config.accent, lineWidth: 1)
                        )
                        .clipShape(Capsule())
                }

                Text(config.title)
                    .font(.system(size: 28, weight: .bold))
                    .foregroundStyle(MomentraBrandTokens.textOnDark)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: .infinity)

                Text(config.body)
                    .font(.system(size: 14))
                    .foregroundStyle(Color(hex: "#C9C4D8"))
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: .infinity)

                if !config.history.isEmpty {
                    Spacer().frame(height: 8)
                    Text(config.historyTitle ?? "Past moments")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(MomentraBrandTokens.textOnDark)
                        .frame(maxWidth: .infinity, alignment: .leading)

                    ForEach(config.history) { moment in
                        historyRow(moment)
                    }
                }

                Spacer().frame(height: 8)

                if let label = config.primaryLabel, let action = config.onPrimary {
                    Button(action: action) {
                        Text(label)
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundStyle(MomentraBrandTokens.textOnDark)
                            .frame(maxWidth: .infinity)
                            .frame(height: 52)
                            .background(config.accent.opacity(0.18))
                            .overlay(
                                RoundedRectangle(cornerRadius: 14)
                                    .stroke(config.accent, lineWidth: 1.5)
                            )
                            .clipShape(RoundedRectangle(cornerRadius: 14))
                    }
                    .buttonStyle(.plain)
                }

                if let label = config.secondaryLabel, let action = config.onSecondary {
                    Button(label, action: action)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(config.accent)
                }

                Text("Or tap + in the top bar to create a Moment")
                    .font(.system(size: 12))
                    .foregroundStyle(Color(hex: "#C9C4D8").opacity(0.7))
                    .multilineTextAlignment(.center)
            }
            .padding(.horizontal, 24)
            .padding(.top, 32)
            .padding(.bottom, 40)
        }
    }

    private func historyRow(_ moment: MomentSummary) -> some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(moment.title)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(MomentraBrandTokens.textOnDark)
                Text(moment.status.capitalized)
                    .font(.system(size: 12))
                    .foregroundStyle(Color(hex: "#C9C4D8"))
            }
            Spacer()
            Text(String(moment.status.prefix(1)))
                .font(.system(size: 11, weight: .bold))
                .foregroundStyle(config.accent)
                .padding(.horizontal, 10)
                .padding(.vertical, 4)
                .background(config.accent.opacity(0.15))
                .clipShape(Capsule())
        }
        .padding(14)
        .background(Color.white.opacity(0.06))
        .overlay(
            RoundedRectangle(cornerRadius: 14)
                .stroke(Color.white.opacity(0.08), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }
}

struct ContextEmptyExperienceView: View {
    @ObservedObject var createModel: MomentCreateModel
    let context: AppContextKind
    let destination: BottomDestination
    let experience: MomentExperienceKind
    let moments: [MomentSummary]
    let hasCompany: Bool
    var selectedCompanyId: String? = nil
    /// Operating-moment subtype code used to resolve the kit's vertical + accent.
    var momentTypeCode: String? = nil
    let onCreateMoment: () -> Void
    let onCreateBack: () -> Void
    var onCompanyActivated: (CompanySummary) -> Void = { _ in }
    var onMomentCreated: (CreateMomentOutcome) -> Void = { _ in }
    var groupCreatePhase: GroupCreatePhase = .chooser
    var onSelectExperience: () -> Void = {}
    var onSelectPurchase: () -> Void = {}
    var onSelectLiving: () -> Void = {}
    var onExitGroupSetup: () -> Void = {}
    var onSetupTypeChanged: (String) -> Void = Self.ignoreString
    var onJoinCode: (String) -> Void = Self.ignoreString
    var onOpenCompletedMoments: (() -> Void)? = nil
    var onCreateBusiness: () -> Void = {}
    var onJoinCompany: () -> Void = {}

    private static func ignoreString(_: String) {}

    private var accent: Color {
        switch context {
        case .group: return Color(hex: "#E8621A")
        case .business: return Color(hex: "#818CF8")
        default: return Color(hex: "#7C5CFC")
        }
    }

    private var history: [MomentSummary] { recentHistoryMoments(moments) }
    private var first: Bool { experience == .firstMoment }

    var body: some View {
        switch context {
        case .personal:
            personalEmpty
        case .group:
            groupEmpty
        case .business:
            businessEmpty
        case .circle:
            CircleComingSoonView()
        }
    }

    private var between: Bool {
        experience == .betweenMoments || experience == .pausedOnly
    }

    @ViewBuilder
    private var personalEmpty: some View {
        PersonalExperienceHost(destination: destination)
    }

    @ViewBuilder
    private var groupEmpty: some View {
        switch destination {
        case .create:
            GroupCreateMomentView(
                onBack: onCreateBack,
                onSelectExperience: onSelectExperience,
                onSelectPurchase: onSelectPurchase,
                onSelectLiving: onSelectLiving,
                onJoinCode: onJoinCode
            )
            .sheet(isPresented: Binding(
                get: {
                    switch groupCreatePhase {
                    case .experienceSetup, .purchaseSetup, .livingSetup: return true
                    case .chooser: return false
                    }
                },
                set: { if !$0 { onExitGroupSetup() } }
            )) {
                Group {
                    switch groupCreatePhase {
                    case .experienceSetup:
                        GroupExperienceSetupView(
                            createModel: createModel,
                            onBack: onExitGroupSetup,
                            onCreated: onMomentCreated,
                            onSetupTypeChanged: onSetupTypeChanged
                        )
                    case .purchaseSetup:
                        GroupPurchaseSetupView(
                            createModel: createModel,
                            onBack: onExitGroupSetup,
                            onCreated: onMomentCreated,
                            onSetupTypeChanged: onSetupTypeChanged
                        )
                    case .livingSetup:
                        GroupLivingSetupView(
                            createModel: createModel,
                            onBack: onExitGroupSetup,
                            onCreated: onMomentCreated,
                            onSetupTypeChanged: onSetupTypeChanged
                        )
                    case .chooser:
                        EmptyView()
                    }
                }
                .presentationDetents([.fraction(0.92)])
                .presentationCornerRadius(24)
                .presentationBackground(GroupSetupTheme.card)
                .presentationDragIndicator(.visible)
            }
        case .pulse:
            if first {
                GroupPulseEmptyView(
                    onStartCta: onCreateMoment,
                    onSelectExperience: onSelectExperience,
                    onSelectPurchase: onSelectPurchase,
                    onSelectLiving: onSelectLiving,
                    onJoinCode: onJoinCode
                )
            } else {
                groupBetweenEmpty
            }
        case .moments:
            if first {
                GroupMomentsEmptyView(onStartCta: onCreateMoment)
            } else {
                groupBetweenEmpty
            }
        case .life:
            if first {
                GroupLifeEmptyView(onStartCta: onCreateMoment)
            } else {
                groupBetweenEmpty
            }
        case .memory:
            if first {
                GroupMemoryEmptyView(onStartCta: onCreateMoment)
            } else {
                groupBetweenEmpty
            }
        }
    }

    private var groupBetweenEmpty: some View {
        MomentEmptyStateView(
            config: MomentEmptyConfig(
                eyebrow: "GROUP",
                title: "Between group moments",
                body: "Nothing is active together right now. Recent moments together stay here.",
                primaryLabel: "+ Start something new",
                onPrimary: onCreateMoment,
                secondaryLabel: onOpenCompletedMoments == nil ? nil : "View completed moments",
                onSecondary: onOpenCompletedMoments,
                historyTitle: "Recent moments together",
                history: history,
                accent: Color(hex: "#E8621A")
            )
        )
    }

    /// Business empty states render the shared Figma "App States" kit so every
    /// destination matches the same 402x874 frame family on iOS and Android.
    @ViewBuilder
    private var businessEmpty: some View {
        if !hasCompany {
            BusinessAppStateBody(
                spec: BusinessAppStateSpecs.spec(.noCompany, .setup),
                accent: businessAccent,
                onPrimary: onCreateBusiness,
                onSecondary: onJoinCompany
            )
        } else {
            businessEmptyWithCompany
        }
    }

    @ViewBuilder
    private var businessEmptyWithCompany: some View {
        if destination == .create {
            BusinessAppStateHubBody(
                accent: businessAccent,
                vertical: BusinessAppStateVertical.forTypeCode(momentTypeCode),
                onPrimary: onCreateMoment,
                onSecondary: onCreateMoment
            )
        } else {
            BusinessAppStateBody(
                spec: BusinessAppStateSpecs.spec(Self.appStateScreen(for: destination), .empty),
                accent: businessAccent,
                onPrimary: onCreateMoment,
                onSecondary: onCreateMoment
            )
        }
    }

    private var businessAccent: BusinessAppStateAccent {
        BusinessAppStateAccent.forTypeCode(momentTypeCode)
    }

    /// Maps the shell's bottom destination onto the kit's screen family.
    private static func appStateScreen(for destination: BottomDestination) -> BusinessAppStateScreen {
        switch destination {
        case .pulse: return .pulse
        case .moments: return .moments
        case .life: return .life
        case .memory: return .memory
        case .create: return .hub
        }
    }
}
