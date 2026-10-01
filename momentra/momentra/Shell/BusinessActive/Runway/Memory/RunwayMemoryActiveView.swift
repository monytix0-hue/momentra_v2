import SwiftUI

/// Money opens the company Memory surface with the Money lens.
struct RunwayMemoryActiveView: View {
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
            momentTypeCode: "BUSINESS_RUNWAY",
            companyId: companyId,
            onOpenQuickAdd: onRecordLearning
        )
    }
}
