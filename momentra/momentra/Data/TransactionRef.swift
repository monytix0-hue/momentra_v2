import Foundation

enum TransactionDomain: String {
    case personal = "PERSONAL"
    case group = "GROUP"
    case business = "BUSINESS"
}

enum TransactionResourceType: String {
    case expense = "EXPENSE"
    case income = "INCOME"
    case contribution = "CONTRIBUTION"
    case revenue = "REVENUE"
}

/// Unified client handle for governed transaction CRUD (QH-T1).
struct TransactionRef: Equatable {
    let domain: TransactionDomain
    let resourceType: TransactionResourceType
    let resourceId: String
    let momentId: String

    static func fromActivity(momentId: String, item: APIClient.ActivityItemPayload) -> TransactionRef? {
        if let expenseId = item.activityPayload?.expenseId {
            return TransactionRef(domain: .personal, resourceType: .expense, resourceId: expenseId, momentId: momentId)
        }
        if let incomeId = item.activityPayload?.incomeId {
            return TransactionRef(domain: .personal, resourceType: .income, resourceId: incomeId, momentId: momentId)
        }
        return nil
    }
}
