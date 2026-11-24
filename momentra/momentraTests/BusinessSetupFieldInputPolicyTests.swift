import SwiftUI
import Testing
@testable import momentra

struct BusinessSetupFieldInputPolicyTests {

    /// `SubmitLabel` and `ScrollDismissesKeyboardMode` are SwiftUI enums that do not
    /// conform to `Equatable`, so equality, `~=` and `if case` are all unavailable.
    /// Their case names are stable API surface, so compare the rendered case name.
    private func caseName<T>(_ value: T) -> String {
        String(describing: value)
    }

    @Test func setupFieldsSubmitWithDone() {
        #expect(
            caseName(BusinessSetupFieldInputPolicy.submitLabel) == "done",
            "Setup fields must submit with .done"
        )
    }

    @Test func setupScrollDismissesTheKeyboardInteractively() {
        #expect(
            caseName(BusinessSetupFieldInputPolicy.scrollDismissesKeyboard) == "interactively",
            "Setup scroll must dismiss the keyboard interactively"
        )
    }
}
