import SwiftUI

/// Which Obsidian toaster (Figma 2:1375) to show in the Business shell.
enum BusinessAppStateToast: Equatable {
    case success(text: String = "₹12,450 added to today's sales")
    case offlineQueue(queuedCount: Int = 2)
    case error(code: String = "SYNC_TIMEOUT_504")
}

/// Top-of-shell host for Obsidian system toasts.
/// Keep in lockstep with `BusinessAppStateToastHost.kt` on Android.
struct BusinessAppStateToastHost: View {
    let toast: BusinessAppStateToast?
    var onRetry: () -> Void = {}

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
        case .offlineQueue(let count):
            AppStateOfflineQueueToast(queuedCount: count)
        case .error(let code):
            AppStateErrorToast(code: code, onRetry: onRetry)
        }
    }
}
