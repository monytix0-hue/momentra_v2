import SwiftUI

/// Thin route into `BusinessPulseScreen`. Does not read the finance score.
struct TeamOpsPulseActiveView: View {
    let refreshToken: UInt64
    let momentTitle: String?
    let momentId: String?
    var onLogDelivery: () -> Void = {}
    var onOpenQuickAdd: () -> Void = {}
    var onViewAllActivity: () -> Void = {}
    var onAddExpense: () -> Void = {}
    var onOpenMoments: () -> Void = {}
    var onToday: ((BusinessQuickAddKind) -> Void)? = nil
    var capabilities: [String]? = nil
    var momentTypeCode: String? = "TEAM_OPERATIONS"

    var body: some View {
        BusinessPulseScreen(
            family: .team,
            refreshToken: refreshToken,
            momentTitle: momentTitle,
            momentId: momentId,
            momentTypeCode: momentTypeCode,
            capabilities: capabilities,
            onToday: { kind in
                if let onToday {
                    onToday(kind)
                } else if kind == .teamUpdate {
                    onLogDelivery()
                } else if kind == .expense || kind == .spendEntry {
                    onAddExpense()
                } else {
                    onOpenQuickAdd()
                }
            },
            onOpenMoments: onOpenMoments
        )
        .onAppear { _ = onViewAllActivity }
    }
}
