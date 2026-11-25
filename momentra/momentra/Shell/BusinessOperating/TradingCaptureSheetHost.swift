import SwiftUI

/// Identifiable Quick Add code for `.sheet(item:)` presentation.
struct CaptureSheetCode: Identifiable, Hashable {
    let code: String
    var id: String { code }
}

/// Bottom-sheet host for Trading / Distribution Quick Add capture (APK `OperatingCaptureSheet` parity).
/// Uses mode-shaped [TradingCaptureForm] for trading family; falls back to typed form otherwise.
struct TradingCaptureSheetHost: View {
    let momentId: String
    let projection: OperatingProjectionPayload?
    let initialCode: String
    var accent: Color = Color(red: 0.06, green: 0.73, blue: 0.51)
    var revisesEventId: String? = nil
    var seedActivity: OperatingActivityPayload? = nil
    var onDismiss: () -> Void = {}
    var onSaved: () -> Void = {}

    private var isTradingFamily: Bool {
        let code = (projection?.subtype ?? "").uppercased()
        return code == "TRADING_WHOLESALE" || code == "DISTRIBUTION_DEALERSHIP"
    }

    var body: some View {
        NavigationStack {
            Group {
                if isTradingFamily {
                    TradingCaptureForm(
                        momentId: momentId,
                        projection: projection,
                        initialCode: initialCode,
                        accent: accent,
                        revisesEventId: revisesEventId,
                        seedActivity: seedActivity,
                        onSaved: {
                            onSaved()
                            onDismiss()
                        }
                    )
                } else {
                    TypedOperatingQuickAddForm(
                        momentId: momentId,
                        projection: projection,
                        initialCode: initialCode,
                        accentOverride: accent,
                        pinStickyFooter: true,
                        onSaved: {
                            onSaved()
                            onDismiss()
                        }
                    )
                }
            }
            .background(Color(red: 0.04, green: 0.05, blue: 0.09).ignoresSafeArea())
            .navigationTitle(titleFor(initialCode))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { onDismiss() }
                        .foregroundStyle(accent)
                }
            }
        }
        .presentationDetents([.medium, .large])
        .presentationDragIndicator(.visible)
    }

    private func titleFor(_ code: String) -> String {
        switch code.uppercased() {
        case "SALE": return "Record Sale"
        case "PURCHASE": return "Record Purchase"
        case "COLLECTION": return "Record Collection"
        case "PAYMENT": return "Record Payment"
        case "SPEND": return "Record Expense"
        case "ORDER": return "Order"
        case "SHIPMENT": return "Shipment"
        case "DELIVERY": return "Record Delivery"
        case "STOCK": return "Stock Update"
        case "BUDGET": return "Revise Budget"
        case "TARGET": return "Revise Target"
        case "RETURN": return "Return / Credit Note"
        case "HOLD": return "Hold a record"
        case "TRANSFER": return "Transfer stock"
        case "REPRINT": return "Reprint a document"
        case "APPROVAL", "REIMBURSE": return "Approval / Reimbursement"
        case "UPDATE", "ISSUE": return "Post Update / Issue"
        default: return code.replacingOccurrences(of: "_", with: " ").capitalized
        }
    }
}
