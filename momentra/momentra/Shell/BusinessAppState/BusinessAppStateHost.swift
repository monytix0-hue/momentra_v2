import SwiftUI

/// Renders the correct Obsidian AppState body for the current Business shell slot.
/// CREATE empty/setup mounts guided business selection (`2:1514`), then the setup shell.
/// Keep in lockstep with `BusinessAppStateHost.kt` on Android.
struct BusinessAppStateHost: View {
    let destination: BottomDestination
    let content: ShellContentState
    let hasCompany: Bool
    let momentTypeCode: String?
    let onRetry: () -> Void
    let onPrimary: () -> Void
    let onSecondary: () -> Void
    var onCreateBusiness: (() -> Void)? = nil
    var onJoinCompany: (() -> Void)? = nil
    var onOpenQuickAdds: (() -> Void)? = nil
    var onBackToPulse: (() -> Void)? = nil
    var onCreateBack: (() -> Void)? = nil
    var companyId: String? = nil
    var onMomentCreated: ((CreateMomentOutcome) -> Void)? = nil
    @ObservedObject var createModel: MomentCreateModel

    @State private var setupFamily: BusinessSetupFamily?
    @State private var setupTypeCode: String?

    private var accent: BusinessAppStateAccent {
        .forTypeCode(momentTypeCode)
    }

    private var vertical: BusinessAppStateVertical {
        .forTypeCode(momentTypeCode)
    }

    private var screen: BusinessAppStateScreen {
        BusinessAppStateRouting.resolveScreen(
            destination: destination,
            content: content,
            hasCompany: hasCompany
        )
    }

    private var phase: BusinessAppStatePhase {
        BusinessAppStateRouting.phase(for: content, hasCompany: hasCompany)
    }

    private var showGuidedCreate: Bool {
        destination == .create && (phase == .empty || phase == .setup)
    }

    var body: some View {
        Group {
            if phase == .loading {
                BusinessAppStateLoadingBody(
                    label: BusinessAppStateRouting.loadingLabel(for: destination),
                    accent: accent
                )
            } else if showGuidedCreate {
                BusinessGuidedCreateView(
                    onBack: onCreateBack ?? onBackToPulse ?? {},
                    onContinue: { option in
                        createModel.clearError()
                        setupTypeCode = option.momentTypeCode
                        setupFamily = BusinessSetupFamily.forFamilyCode(option.familyCode)
                    },
                    submitting: createModel.state.submitting,
                    error: createModel.state.error
                )
                .fullScreenCover(item: $setupFamily) { family in
                    BusinessSetupShell(
                        family: family,
                        initialTypeCode: setupTypeCode ?? family.defaultTypeCode,
                        createModel: createModel,
                        companyId: companyId,
                        onBack: {
                            self.setupFamily = nil
                            self.setupTypeCode = nil
                        },
                        onCreated: { outcome in
                            self.setupFamily = nil
                            self.setupTypeCode = nil
                            onMomentCreated?(outcome)
                        }
                    )
                }
            } else if screen == .hub, phase == .empty {
                BusinessAppStateHubBody(
                    accent: accent,
                    vertical: vertical,
                    onPrimary: onPrimary,
                    onSecondary: onOpenQuickAdds ?? onSecondary
                )
            } else if screen == .noCompany {
                BusinessAppStateBody(
                    spec: BusinessAppStateSpecs.spec(.noCompany, .setup),
                    accent: accent,
                    onPrimary: onCreateBusiness ?? onPrimary,
                    onSecondary: onJoinCompany ?? onSecondary
                )
            } else {
                BusinessAppStateBody(
                    spec: BusinessAppStateSpecs.spec(screen, phase),
                    accent: accent,
                    onPrimary: phase == .error ? onRetry : onPrimary,
                    onSecondary: phase == .error ? onRetry : (onOpenQuickAdds ?? onSecondary)
                )
            }
        }
    }
}
