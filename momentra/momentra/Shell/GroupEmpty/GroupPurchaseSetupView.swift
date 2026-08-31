import SwiftUI

/// Figma 575:9919 — Shared Purchase setup (4 variants).
struct GroupPurchaseSetupView: View {
    @ObservedObject var createModel: MomentCreateModel
    var onBack: () -> Void
    var onCreated: (CreateMomentOutcome) -> Void
    var onSetupTypeChanged: (String) -> Void = { _ in }
    var editingMomentId: String? = nil
    var initialTitle: String? = nil
    var initialTypeCode: String? = nil

    var body: some View {
        GroupSectionSetupView(
            variant: GroupSetupCatalog.purchase,
            createModel: createModel,
            onBack: onBack,
            onCreated: onCreated,
            onSetupTypeChanged: onSetupTypeChanged,
            editingMomentId: editingMomentId,
            initialTitle: initialTitle,
            initialTypeCode: initialTypeCode
        )
    }
}
