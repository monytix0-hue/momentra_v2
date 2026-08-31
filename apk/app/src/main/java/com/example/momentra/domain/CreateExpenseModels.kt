package com.example.momentra.domain

/** Command result from POST /v1/moments/{momentId}/expenses. */
data class CreateExpenseOutcome(
    val expenseId: String,
    val momentId: String,
    val amount: String,
    val currencyCode: String,
    val status: String,
    val version: Long,
    val projectionHints: List<ProjectionHint> = emptyList(),
)
