import SwiftUI

/// Money Moments. The shared screen owns the activity timeline.
struct RunwayMomentsActiveView: View {
    let refreshToken: UInt64
    let momentTitle: String?
    let momentId: String?
    var onLogExpense: () -> Void = {}
    var onOpenQuickAdd: () -> Void = {}
    var onOpenMoments: () -> Void = {}
    var onViewAllActivity: () -> Void = {}

    var body: some View {
        BusinessMomentsScreen(
            family: .money,
            momentId: momentId,
            momentTitle: momentTitle,
            refreshToken: refreshToken,
            onOpenQuickAdd: onOpenQuickAdd
        )
    }
}
