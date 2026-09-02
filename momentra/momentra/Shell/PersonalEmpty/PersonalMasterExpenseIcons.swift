import SwiftUI

/// Native SF Symbols for Master Expense UI chrome (categories + structural UI).
enum PersonalMasterExpenseIcons {
    static func categorySymbol(code: String) -> String {
        switch code.uppercased() {
        case "FOOD": return "fork.knife"
        case "TRANSPORT": return "car.fill"
        case "SHOPPING": return "bag.fill"
        case "CAFE": return "cup.and.saucer.fill"
        case "HEALTH": return "cross.case.fill"
        case "ENTERTAINMENT": return "film.fill"
        case "BILLS": return "doc.text.fill"
        default: return "shippingbox.fill"
        }
    }

    enum Chrome: String {
        case header = "creditcard.fill"
        case info = "info.circle.fill"
        case edit = "pencil"
        case amount = "indianrupeesign.circle.fill"
        case calendar = "calendar"
        case folder = "folder.fill"
        case back = "chevron.left"
        case expandUp = "chevron.up"
        case expandDown = "chevron.down"
    }
}

struct MeIcon: View {
    let symbol: String
    var tint: Color = .primary
    var size: CGFloat = 20
    var accessibilityLabel: String?

    var body: some View {
        Image(systemName: symbol)
            .font(.system(size: size))
            .foregroundStyle(tint)
            .accessibilityLabel(accessibilityLabel ?? "")
    }
}
