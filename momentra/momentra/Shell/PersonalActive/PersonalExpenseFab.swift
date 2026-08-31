import SwiftUI

/// Figma 1035:7757 — Personal expense FAB (₹+).
struct PersonalExpenseFab: View {
    var onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            Text("₹+")
                .font(.system(size: 18, weight: .bold))
                .foregroundStyle(.white)
                .frame(width: 56, height: 56)
                .background(PersonalMasterExpenseTheme.accent)
                .clipShape(Circle())
                .shadow(color: PersonalMasterExpenseTheme.accent.opacity(0.4), radius: 8, x: 0, y: 6)
        }
        .buttonStyle(.plain)
        .accessibilityLabel("Add expense")
        .accessibilityIdentifier("personal_expense_fab")
    }
}
