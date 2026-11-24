import SwiftUI

/// Every Business setup field is single-line, so its return key commits the entry and
/// dismisses the keyboard. Lockstep with Android `BusinessSetupFieldInputPolicy`.
enum BusinessSetupFieldInputPolicy {
    /// A single-line setup field commits on return and dismisses the keyboard.
    static let submitLabel: SubmitLabel = .done
    static let scrollDismissesKeyboard: ScrollDismissesKeyboardMode = .interactively
}
