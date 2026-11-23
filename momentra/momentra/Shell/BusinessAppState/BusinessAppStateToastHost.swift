import SwiftUI

/// Copy for the Operating “Setup activated” toast after business moment activate.
enum BusinessSetupActivateToastCopy {
    static let defaultTitle = "Setup activated"

    static func subtitle(momentTitle: String, momentTypeCode: String?) -> String {
        let name = momentTitle.trimmingCharacters(in: .whitespacesAndNewlines)
        let code = momentTypeCode?
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .uppercased()
        let livePhrase: String
        switch code {
        case "DISTRIBUTION_DEALERSHIP":
            livePhrase = "Distribution is live"
        case "TRADING_WHOLESALE":
            livePhrase = "Trading is live"
        case "CLIENT_CAMPAIGN":
            livePhrase = "Campaign is live"
        default:
            livePhrase = "is live"
        }
        return "\(name) · \(livePhrase) · opening Pulse"
    }
}

/// Which Obsidian toaster (Figma 2:1375) to show in the Business shell.
enum BusinessAppStateToast: Equatable {
    case success(text: String = "Moment activated")
    case setupActivated(title: String, subtitle: String)
    case offlineQueue(queuedCount: Int = 2)
    case error(code: String = "SYNC_TIMEOUT_504")
}

/// Top-of-shell host for Obsidian system toasts.
/// Keep in lockstep with `BusinessAppStateToastHost.kt` on Android.
struct BusinessAppStateToastHost: View {
    let toast: BusinessAppStateToast?
    var onRetry: () -> Void = {}
    /// Dismiss handler for setup-activated toast (auto-dismiss + “Pulse ›”).
    var onDismiss: () -> Void = {}

    var body: some View {
        Group {
            if let toast {
                content(for: toast)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 8)
            }
        }
    }

    @ViewBuilder
    private func content(for toast: BusinessAppStateToast) -> some View {
        switch toast {
        case .success(let text):
            AppStateSuccessToast(text: text)
        case .setupActivated(let title, let subtitle):
            AppStateSetupActivatedToast(title: title, subtitle: subtitle, onDismiss: onDismiss)
        case .offlineQueue(let count):
            AppStateOfflineQueueToast(queuedCount: count)
        case .error(let code):
            AppStateErrorToast(code: code, onRetry: onRetry)
        }
    }
}
