import SwiftUI

struct SetupWizardScaffold<Content: View, Footer: View>: View {
    var backgroundColor: Color = SetupTokens.bgPrimary
    @ViewBuilder var content: () -> Content
    @ViewBuilder var footer: () -> Footer

    var body: some View {
        ScrollView {
            content()
                .padding(.bottom, 8)
        }
        .safeAreaInset(edge: .bottom) {
            footer()
        }
        .background(backgroundColor.ignoresSafeArea())
    }
}
