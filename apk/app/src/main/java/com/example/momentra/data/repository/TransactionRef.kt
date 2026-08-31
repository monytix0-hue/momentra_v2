package com.example.momentra.data.repository

import com.example.momentra.data.api.ActivityItemDto

enum class TransactionDomain {
    PERSONAL,
    GROUP,
    BUSINESS,
}

enum class TransactionResourceType {
    EXPENSE,
    INCOME,
    CONTRIBUTION,
    REVENUE,
}

/** Unified client handle for governed transaction CRUD (QH-T1). */
data class TransactionRef(
    val domain: TransactionDomain,
    val resourceType: TransactionResourceType,
    val resourceId: String,
    val momentId: String,
) {
    companion object {
        fun fromActivity(momentId: String, item: ActivityItemDto): TransactionRef? {
            val payload = item.activityPayload ?: return null
            payload.expenseId?.let {
                return TransactionRef(TransactionDomain.PERSONAL, TransactionResourceType.EXPENSE, it, momentId)
            }
            payload.incomeId?.let {
                return TransactionRef(TransactionDomain.PERSONAL, TransactionResourceType.INCOME, it, momentId)
            }
            return null
        }
    }
}
