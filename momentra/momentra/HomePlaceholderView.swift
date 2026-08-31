import SwiftUI

/** @deprecated Replaced by AppShellView in Phase 4. */
struct HomePlaceholderView: View {
    var body: some View {
        AppShellView(
            identity: ShellIdentity(userId: "preview", displayName: "Preview", email: nil, firebaseUid: nil),
            model: AppShellModel(),
            onSignOut: {}
        )
    }
}
