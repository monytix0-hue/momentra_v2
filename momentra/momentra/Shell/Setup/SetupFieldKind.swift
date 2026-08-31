import Foundation

enum SetupFieldKind {
    case text
    case chips
    case dropdown
    case toggle
    case date
    case dateTime
    case time
    case title
}

enum GroupSetupFields {
    static let name = "name"
    static let dates = "dates"
    static let targetDate = "targetDate"
    static let moveIn = "moveIn"

    static func kindFor(_ key: String) -> SetupFieldKind {
        switch key {
        case name: return .title
        case dates, targetDate, moveIn: return .date
        default: return .chips
        }
    }
}
