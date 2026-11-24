import SwiftUI

/// Phase 1C placeholder — Business operating hubs still rebuilding for V4.
struct BusinessV4RebuildingView: View {
    var message: String = "Business is rebuilding for V4."

    var body: some View {
        VStack(spacing: 12) {
            Text("Business V4")
                .font(.title2.weight(.bold))
                .foregroundStyle(MomentraBrandTokens.textOnDark)
            Text(message)
                .font(.subheadline)
                .foregroundStyle(Color(hex: "#C9C4D8"))
                .multilineTextAlignment(.center)
        }
        .padding(24)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(hex: "#14121B").ignoresSafeArea())
    }
}

/// Guided business selection → in-document setup shell (Figma `2:1514` → setup frames).
struct BusinessCreateFlowView: View {
    @ObservedObject var createModel: MomentCreateModel
    var companyId: String?
    var ownerName: String = ""
    var onBack: () -> Void = {}
    var onCreated: (CreateMomentOutcome) -> Void = { _ in }

    @State private var setupFamily: BusinessSetupFamily?

    var body: some View {
        BusinessGuidedCreateView(
            onBack: onBack,
            onContinue: { option in
                setupFamily = BusinessSetupFamily.forFamilyCode(option.familyCode)
            },
            submitting: createModel.state.submitting,
            error: createModel.state.error
        )
        .fullScreenCover(item: $setupFamily) { family in
            BusinessSetupShell(
                family: family,
                initialTypeCode: family.defaultTypeCode,
                createModel: createModel,
                companyId: companyId,
                onBack: { setupFamily = nil },
                onCreated: onCreated
            )
        }
        .preferredColorScheme(.dark)
    }
}

/// Manage Moment / Edit setup — opens the same Business setup shell.
struct BusinessSetupWizardView: View {
    let kind: BusinessSetupKind
    @ObservedObject var createModel: MomentCreateModel
    var companyId: String?
    var ownerName: String = ""
    var editingMomentId: String? = nil
    var editingMomentStatus: String? = nil
    var initialTitle: String? = nil
    var initialTypeCode: String? = nil
    var onClose: () -> Void
    var onCreated: (CreateMomentOutcome) -> Void = { _ in }

    var body: some View {
        let typeCode = initialTypeCode ?? kind.subtype
        BusinessSetupShell(
            family: BusinessSetupFamily.forTypeCode(typeCode),
            initialTypeCode: typeCode,
            createModel: createModel,
            companyId: companyId,
            editingMomentId: editingMomentId,
            editingMomentStatus: editingMomentStatus,
            initialTitle: initialTitle,
            onBack: onClose,
            onCreated: onCreated
        )
        .preferredColorScheme(.dark)
    }
}
