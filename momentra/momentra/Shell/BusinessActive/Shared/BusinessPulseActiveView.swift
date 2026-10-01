import SwiftUI

/// Fallback Pulse for an unrecognized Business type code.
struct BusinessPulseActiveView: View {
    let refreshToken: UInt64
    let momentTitle: String?
    let momentId: String?
    var momentTypeCode: String? = nil
    var onAddExpense: () -> Void = {}
    var onOpenQuickAdd: () -> Void = {}
    var onToday: ((BusinessQuickAddKind) -> Void)? = nil
    var capabilities: [String]? = nil

    private var family: BusinessMomentFamily {
        BusinessTabLoad.loadFamily(for: momentTypeCode)
    }

    var body: some View {
        BusinessPulseScreen(
            family: family,
            refreshToken: refreshToken,
            momentTitle: momentTitle,
            momentId: momentId,
            momentTypeCode: momentTypeCode,
            capabilities: capabilities,
            onToday: { kind in
                if let onToday {
                    onToday(kind)
                } else if kind == .expense || kind == .spendEntry {
                    onAddExpense()
                } else {
                    onOpenQuickAdd()
                }
            }
        )
    }
}
