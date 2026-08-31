import SwiftUI

struct PersonalExpenseSheet: View {
    let momentId: String
    @Binding var isPresented: Bool
    var pulseFamily: PersonalPulseFamily = .lifeOperations
    var onSaved: () -> Void

    var body: some View {
        PersonalMasterExpenseSheet(
            momentId: momentId,
            pulseFamily: pulseFamily,
            onClose: { isPresented = false },
            onSaved: onSaved
        )
    }
}
