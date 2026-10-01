import SwiftUI

/// Thin route into `BusinessPulseScreen`.
struct OpsPulseActiveView: View {
    let refreshToken: UInt64
    let momentTitle: String?
    let momentId: String?
    var onLogSpend: () -> Void = {}
    var onOpenQuickAdd: () -> Void = {}
    var onOpenMoments: () -> Void = {}
    var onViewAllActivity: () -> Void = {}
    var onToday: ((BusinessQuickAddKind) -> Void)? = nil
    var capabilities: [String]? = nil
    var momentTypeCode: String? = "BUSINESS_OPERATIONS"

    var body: some View {
        BusinessPulseScreen(
            family: .daily,
            refreshToken: refreshToken,
            momentTitle: momentTitle,
            momentId: momentId,
            momentTypeCode: momentTypeCode,
            capabilities: capabilities,
            onToday: { kind in
                if let onToday {
                    onToday(kind)
                } else if kind == .spendEntry || kind == .expense {
                    onLogSpend()
                } else {
                    onOpenQuickAdd()
                }
            },
            onOpenMoments: onOpenMoments
        )
        .onAppear { _ = onViewAllActivity }
    }
}
