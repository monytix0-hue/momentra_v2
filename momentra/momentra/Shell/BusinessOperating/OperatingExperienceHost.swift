import SwiftUI

/// Hosts Pulse / Moments / Life / Memory / Quick Adds for business operating verticals.
struct OperatingExperienceHost: View {
    let momentId: String
    var companyId: String? = nil
    let subtype: String?
    let destination: BottomDestination
    let refreshToken: Int
    var momentTitle: String? = nil
    var onStoryReady: (String) -> Void = { _ in }
    var onOpenQuickAdd: () -> Void = {}
    var onViewAllAttention: () -> Void = {}
    var onQuickAddCode: (String) -> Void = { _ in }

    var body: some View {
        OperatingMomentHubView(
            momentId: momentId,
            momentTitle: momentTitle,
            momentTypeCode: subtype,
            companyId: companyId,
            tab: OperatingHubTab.from(destination),
            refreshToken: refreshToken,
            onOpenQuickAdd: onOpenQuickAdd,
            onViewAllAttention: onViewAllAttention,
            onQuickAddCode: onQuickAddCode,
            onGenerateStory: {
                let code = subtype?.uppercased() ?? ""
                if code == "TRADING_WHOLESALE" || code == "DISTRIBUTION_DEALERSHIP" {
                    return
                }
                onStoryReady(momentId)
            }
        )
    }
}