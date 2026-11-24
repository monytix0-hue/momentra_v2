import Foundation

/// Payload shown in the Operating setup-activated toast (Figma `5:63174`).
struct BusinessSetupActivatedToast: Equatable {
    let title: String
    let subtitle: String
}