import SwiftUI

struct SetupWizardScaffold<Content: View, Footer: View>: View {
    var backgroundColor: Color = SetupTokens.bgPrimary
    @ViewBuilder var content: () -> Content
    @ViewBuilder var footer: () -> Footer

    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                content()
                    .padding(.bottom, 8)
            }
            footer()
        }
        .background(backgroundColor.ignoresSafeArea())
    }
}
