import SwiftUI

/// Daily Business Moments. The shared screen owns the activity timeline.
struct OpsMomentsActiveView: View {
    let refreshToken: UInt64
    let momentId: String?
    let momentTitle: String?
    var onLogSpend: () -> Void = {}
    var onOpenQuickAdd: () -> Void = {}
    var onOpenMoments: () -> Void = {}
    var onViewAllActivity: () -> Void = {}

    var body: some View {
        BusinessMomentsScreen(
            family: .daily,
            momentId: momentId,
            momentTitle: momentTitle,
            refreshToken: refreshToken,
            onOpenQuickAdd: onOpenQuickAdd
        )
    }
}
