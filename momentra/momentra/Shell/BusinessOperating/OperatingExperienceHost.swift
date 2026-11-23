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

    @State private var storyConfirm: String?

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
                storyConfirm = "Story opens when available — no second ledger."
                onStoryReady(momentId)
            }
        )
        .overlay(alignment: .bottom) {
            if let storyConfirm {
                Text(storyConfirm)
                    .font(.system(size: 12))
                    .padding(12)
                    .background(Color.black.opacity(0.75))
                    .foregroundStyle(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .padding()
                    .onTapGesture { self.storyConfirm = nil }
            }
        }
    }
}