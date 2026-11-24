import ActivityKit
import Foundation

/// The Live Activity contract shared by the app and the widget extension.
///
/// The app requests the activity (`FlushLiveActivity` in `OfflineOutbox.swift`) and the extension
/// renders it (`OutboxFlushWidgetLiveActivity`). Both sides must agree on this shape, so it is
/// compiled by both targets — the extension must not depend on the outbox queue or the API client
/// just to render a progress view.
@available(iOS 16.1, *)
struct OutboxFlushAttributes: ActivityAttributes {
    struct ContentState: Codable, Hashable {
        let synced: Int
        let remaining: Int
        let currentItemDescription: String
    }
    let total: Int
}
