import SwiftUI

/// Thin route into `BusinessPulseScreen`.
struct RunwayPulseActiveView: View {
    let refreshToken: UInt64
    let momentTitle: String?
    let momentId: String?
    var onLogExpense: () -> Void = {}
    var onOpenQuickAdd: () -> Void = {}
    var onOpenMoments: () -> Void = {}
    var onViewAllActivity: () -> Void = {}
    var onToday: ((BusinessQuickAddKind) -> Void)? = nil
    var capabilities: [String]? = nil
    var momentTypeCode: String? = "BUSINESS_RUNWAY"

    var body: some View {
        BusinessPulseScreen(
            family: .money,
            refreshToken: refreshToken,
            momentTitle: momentTitle,
            momentId: momentId,
            momentTypeCode: momentTypeCode,
            capabilities: capabilities,
            onToday: { kind in
                if let onToday {
                    onToday(kind)
                } else if kind == .expense || kind == .spendEntry {
                    onLogExpense()
                } else {
                    onOpenQuickAdd()
                }
            },
            onOpenMoments: onOpenMoments
        )
        .onAppear { _ = onViewAllActivity }
    }
}
