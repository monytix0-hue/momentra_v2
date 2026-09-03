import SwiftUI

/// Business Create chooser + interactive setup wizard bottom sheets.
struct BusinessCreateFlowView: View {
    @ObservedObject var createModel: MomentCreateModel
    var companyId: String?
    var onBack: () -> Void
    var onCreated: (CreateMomentOutcome) -> Void

    @State private var openSetup: BusinessSetupKind?

    var body: some View {
        BusinessCreateMomentView(
            onBack: onBack,
            onSelectSetup: { kind in
                openSetup = kind
            }
        )
        .sheet(item: $openSetup) { kind in
            if let companyId {
                BusinessSetupWizardView(
                    kind: kind,
                    createModel: createModel,
                    companyId: companyId,
                    onClose: dismissSetup,
                    onCreated: { outcome in
                        openSetup = nil
                        onCreated(outcome)
                    }
                )
                .presentationDetents([.fraction(0.94)])
                .presentationCornerRadius(24)
                .presentationBackground(BusinessSheetTheme.bg)
                .presentationDragIndicator(.visible)
                .interactiveDismissDisabled(createModel.state.submitting)
            }
        }
    }

    private func dismissSetup() {
        guard !createModel.state.submitting else { return }
        createModel.clearError()
        openSetup = nil
    }
}
