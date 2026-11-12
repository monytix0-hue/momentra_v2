import SwiftUI

/// Consumer Service kit — Figma `SVC-HUB-v5` `2401:29887`.
/// Port of Android `ConsumerServiceActive` / `ConsumerServiceQuickAdds`.
/// Hero = Finalize Charge; Booking is a distinct non-revenue tile.
/// Moments/Life/Memory card IA lives in `RetailFamilyActiveScreens` (Clients & Services / Rooms & Staff).

struct ServiceActiveView: View {
    @ObservedObject var model: BusinessOperatingModel
    let destination: BottomDestination
    let refreshToken: UInt64
    var onOpenQuickAdd: () -> Void = {}
    var onOpenStory: () -> Void = {}
    var onOpenMasters: (OperatingMasterTab) -> Void = { _ in }
    var onCapture: ((String) -> Void)? = nil

    var body: some View {
        RetailFamilyActiveView(
            model: model,
            destination: destination,
            refreshToken: refreshToken,
            onOpenQuickAdd: onOpenQuickAdd,
            onOpenStory: onOpenStory,
            onOpenMasters: onOpenMasters,
            onCapture: onCapture,
            remapCapture: { code in
                switch code.uppercased() {
                case "SALE": return "SERVICE"
                default: return code
                }
            }
        ) {
            ServiceHubScreen(
                model: model,
                theme: OperatingTheme(vertical: model.vertical),
                onCapture: { onCapture?($0) ?? onOpenQuickAdd() },
                onOpenMasters: onOpenMasters,
                onOpenStory: onOpenStory
            )
        }
    }
}
