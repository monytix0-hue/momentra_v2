import SwiftUI

/// Shared chrome so Business bottom sheets actually read as frost glass.
/// Callers: BusinessSetupDocumentKit.DateRow .sheet; ManageMomentFlowSheet (Business);
/// AppShellView attention ViewAll .sheet; ManageMoment fullScreenCover (Business).
/// Sit over live UI with clear presentation — do not stack a second black scrim under the panel.
extension View {
    func businessGlassBottomSheet(
        detents: Set<PresentationDetent> = [.medium, .large]
    ) -> some View {
        self
            .presentationDetents(detents)
            .presentationCornerRadius(BusinessAppStateTokens.cardRadius)
            .presentationBackground(.clear)
            .presentationDragIndicator(.visible)
            .presentationBackgroundInteraction(.enabled(upThrough: .medium))
    }

    /// Clear fullScreenCover chrome for Manage Moment (Business).
    func businessGlassFullScreenCover() -> some View {
        self
            .presentationBackground(.clear)
    }
}

/// Sheet panel fill: ultraThinMaterial blur + translucent tint (~0.55) so UI behind shows through.
struct BusinessGlassSheetPanel<Content: View>: View {
    @ViewBuilder var content: () -> Content

    private var sheetTint: Color {
        BusinessAppStateTokens.glassCardFill.opacity(0.76) // 0.72 * 0.76 ≈ 0.55 effective
    }

    private var shape: UnevenRoundedRectangle {
        UnevenRoundedRectangle(
            topLeadingRadius: BusinessAppStateTokens.cardRadius,
            topTrailingRadius: BusinessAppStateTokens.cardRadius
        )
    }

    var body: some View {
        content()
            .frame(maxWidth: .infinity)
            .background {
                ZStack {
                    shape.fill(.ultraThinMaterial)
                    shape.fill(Color.white.opacity(0.06))
                    shape.fill(sheetTint)
                }
            }
            .overlay {
                shape.stroke(
                    BusinessAppStateTokens.glassCardStroke,
                    lineWidth: BusinessAppStateTokens.glassCardStrokeWidth
                )
            }
    }
}
