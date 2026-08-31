import Foundation

/// GET / PATCH / DELETE loader for personal expense + income transactions.
final class PersonalTransactionRepository {
    private let client: APIClient

    init(client: APIClient = .shared) {
        self.client = client
    }

    func loadExpenseDetail(ref: TransactionRef) async throws -> APIClient.ExpenseDetail {
        guard ref.resourceType == .expense else {
            throw NSError(domain: "PersonalTransactionRepository", code: 400, userInfo: [NSLocalizedDescriptionKey: "Income detail uses activity payload."])
        }
        return try await client.getExpense(momentId: ref.momentId, expenseId: ref.resourceId)
    }

    func updateExpense(
        ref: TransactionRef,
        amount: String? = nil,
        currencyCode: String? = nil,
        description: String? = nil,
        merchantName: String? = nil,
        categoryCode: String? = nil,
        subcategoryCode: String? = nil,
        financialAccountId: String? = nil,
        paymentMethodCode: String? = nil,
        effectiveAt: String? = nil,
        recurringScheduleId: String? = nil
    ) async throws -> APIClient.UpdateExpenseResult {
        guard ref.resourceType == .expense else {
            throw NSError(domain: "PersonalTransactionRepository", code: 400, userInfo: [NSLocalizedDescriptionKey: "Use income endpoints for INCOME resources."])
        }
        return try await client.updateExpense(
            momentId: ref.momentId,
            expenseId: ref.resourceId,
            amount: amount,
            currencyCode: currencyCode,
            description: description,
            merchantName: merchantName,
            categoryCode: categoryCode,
            subcategoryCode: subcategoryCode,
            financialAccountId: financialAccountId,
            paymentMethodCode: paymentMethodCode,
            effectiveAt: effectiveAt,
            recurringScheduleId: recurringScheduleId
        )
    }

    func void(ref: TransactionRef) async throws {
        switch ref.resourceType {
        case .expense:
            _ = try await client.voidExpense(momentId: ref.momentId, expenseId: ref.resourceId)
        case .income:
            _ = try await client.voidPersonalIncome(momentId: ref.momentId, incomeId: ref.resourceId)
        default:
            throw NSError(domain: "PersonalTransactionRepository", code: 400, userInfo: [NSLocalizedDescriptionKey: "Unsupported resource type."])
        }
    }

    func attachMedia(ref: TransactionRef, bytes: Data, contentType: String = "image/jpeg") async throws -> APIClient.ExpenseAttachment {
        guard ref.resourceType == .expense else {
            throw NSError(domain: "PersonalTransactionRepository", code: 400, userInfo: [NSLocalizedDescriptionKey: "Attachments only supported on expenses."])
        }
        return try await client.uploadAndAttachExpenseMedia(
            momentId: ref.momentId,
            expenseId: ref.resourceId,
            bytes: bytes,
            contentType: contentType
        )
    }
}
