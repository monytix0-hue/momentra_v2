import SwiftUI

/// Team opens the company Memory surface with the Team lens.
struct TeamOpsMemoryActiveView: View {
    let refreshToken: UInt64
    let momentId: String?
    let momentTitle: String?
    var companyId: String? = nil
    var onRecordLearning: () -> Void = {}
    var onOpenQuickAdd: () -> Void = {}

    var body: some View {
        BusinessMemoryActiveView(
            refreshToken: refreshToken,
            momentId: momentId,
            momentTitle: momentTitle,
            momentTypeCode: "TEAM_OPERATIONS",
            companyId: companyId,
            onOpenQuickAdd: onRecordLearning
        )
    }
}
