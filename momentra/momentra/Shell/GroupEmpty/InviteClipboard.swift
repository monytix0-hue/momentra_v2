import Foundation
import UIKit
import UniformTypeIdentifiers

/// Copies a join code without letting it leave the device or live forever.
///
/// `UIPasteboard.general.string = …` is readable by Universal Clipboard, so a code copied on the
/// phone turns up on every device the owner is signed into — and a pasted string never expires.
/// `.localOnly` keeps it on this device; `.expirationDate` drops it from the paste stack on its
/// own, so a code the operator copied and forgot about is not still sitting there tomorrow.
enum InviteClipboard {
    static func copy(_ text: String, expiresAfter seconds: TimeInterval = 120) {
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        UIPasteboard.general.setItems(
            [[UTType.utf8PlainText.identifier: trimmed]],
            options: [
                .localOnly: true,
                .expirationDate: Date().addingTimeInterval(seconds),
            ]
        )
    }
}
