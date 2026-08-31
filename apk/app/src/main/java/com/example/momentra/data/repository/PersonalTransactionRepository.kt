package com.example.momentra.data.repository

import com.example.momentra.data.api.CreateExpenseResultDto
import com.example.momentra.data.api.ExpenseAttachmentDto
import com.example.momentra.data.api.ExpenseDetailDto
import com.example.momentra.data.api.PersonalIncomeResultDto
import java.util.UUID

/** GET / PATCH / DELETE loader for personal expense + income transactions. */
class PersonalTransactionRepository(
    private val slice: PersonalSliceRepository = PersonalSliceRepository(),
) {
    suspend fun loadDetail(ref: TransactionRef): Result<ExpenseDetailDto> = when (ref.resourceType) {
        TransactionResourceType.EXPENSE -> slice.getExpense(ref.momentId, ref.resourceId)
        else -> Result.failure(IllegalArgumentException("Income detail load uses activity payload until GET income ships on client."))
    }

    suspend fun updateExpense(
        ref: TransactionRef,
        amount: String? = null,
        currencyCode: String? = null,
        description: String? = null,
        merchantName: String? = null,
        categoryCode: String? = null,
        subcategoryCode: String? = null,
        financialAccountId: String? = null,
        paymentMethodCode: String? = null,
        effectiveAt: String? = null,
        recurringScheduleId: String? = null,
    ): Result<CreateExpenseResultDto> {
        require(ref.resourceType == TransactionResourceType.EXPENSE) { "Use income endpoints for INCOME resources." }
        return slice.updateExpense(
            momentId = ref.momentId,
            expenseId = ref.resourceId,
            amount = amount,
            currencyCode = currencyCode,
            description = description,
            merchantName = merchantName,
            categoryCode = categoryCode,
            subcategoryCode = subcategoryCode,
            financialAccountId = financialAccountId,
            paymentMethodCode = paymentMethodCode,
            effectiveAt = effectiveAt,
            recurringScheduleId = recurringScheduleId,
        )
    }

    suspend fun voidExpense(ref: TransactionRef): Result<CreateExpenseResultDto> {
        require(ref.resourceType == TransactionResourceType.EXPENSE) { "Use voidIncome for INCOME resources." }
        return slice.voidExpense(ref.momentId, ref.resourceId)
    }

    suspend fun voidIncome(ref: TransactionRef): Result<PersonalIncomeResultDto> {
        require(ref.resourceType == TransactionResourceType.INCOME) { "Use voidExpense for EXPENSE resources." }
        return slice.voidPersonalIncome(ref.momentId, ref.resourceId)
    }

    suspend fun attachMedia(
        ref: TransactionRef,
        bytes: ByteArray,
        contentType: String = "image/jpeg",
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<ExpenseAttachmentDto> {
        require(ref.resourceType == TransactionResourceType.EXPENSE) { "Attachments only supported on expenses." }
        return slice.uploadAndAttachExpenseMedia(
            momentId = ref.momentId,
            expenseId = ref.resourceId,
            bytes = bytes,
            contentType = contentType,
            idempotencyKey = idempotencyKey,
        )
    }
}
