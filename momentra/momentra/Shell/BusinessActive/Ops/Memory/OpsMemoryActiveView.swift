import SwiftUI

/// Daily opens the company Memory surface with the Daily lens.
struct OpsMemoryActiveView: View {
    let refreshToken: UInt64
    let momentId: String?
    let momentTitle: String?
    var companyId: String? = nil
    var onRecordLearning: () -> Void = {}

    var body: some View {
        BusinessMemoryActiveView(
            refreshToken: refreshToken,
            momentId: momentId,
            momentTitle: momentTitle,
            momentTypeCode: "BUSINESS_OPERATIONS",
            companyId: companyId,
            onOpenQuickAdd: onRecordLearning
        )
    }
}
