import SwiftUI

/// Team Moments. The shared screen owns the activity timeline.
struct TeamOpsMomentsActiveView: View {
    let refreshToken: UInt64
    let momentTitle: String?
    let momentId: String?
    var onLogWin: () -> Void = {}
    var onOpenQuickAdd: () -> Void = {}
    var onViewAllActivity: () -> Void = {}

    var body: some View {
        BusinessMomentsScreen(
            family: .team,
            momentId: momentId,
            momentTitle: momentTitle,
            refreshToken: refreshToken,
            onOpenQuickAdd: onOpenQuickAdd
        )
    }
}
