import SwiftUI

/// Fallback Business Moments when the type code is not Money, Daily, or Team.
struct BusinessMomentsActiveView: View {
    let refreshToken: UInt64
    let momentId: String?
    let momentTitle: String?
    var momentTypeCode: String? = nil
    var onAddExpense: () -> Void = {}
    var onOpenQuickAdd: () -> Void = {}

    private var family: BusinessMomentFamily {
        BusinessMomentFamilyConfig.familyOrNil(momentTypeCode) ?? .team
    }

    var body: some View {
        BusinessMomentsScreen(
            family: family,
            momentId: momentId,
            momentTitle: momentTitle,
            refreshToken: refreshToken,
            onOpenQuickAdd: onOpenQuickAdd
        )
    }
}
