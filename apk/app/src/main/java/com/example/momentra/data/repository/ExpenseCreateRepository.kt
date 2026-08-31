package com.example.momentra.data.repository

import android.content.Context
import com.example.momentra.data.api.ApiClient
import com.example.momentra.data.api.ApiResultException
import com.example.momentra.data.api.ApiService
import com.example.momentra.data.api.CreateExpenseBody
import com.example.momentra.data.api.CreateExpenseResultDto
import com.example.momentra.data.api.SuccessEnvelope
import com.example.momentra.data.api.mapHttpFailure
import com.example.momentra.data.create.IdempotencyKeyStore
import com.example.momentra.domain.CreateExpenseOutcome
import com.example.momentra.domain.ProjectionHint
import java.io.IOException
import retrofit2.HttpException

interface ExpenseCreateGateway {
    suspend fun createExpense(
        draftKey: String,
        momentId: String,
        amount: String,
        currencyCode: String,
        description: String? = null,
        merchantName: String? = null,
        categoryCode: String? = null,
        effectiveAt: String? = null,
    ): Result<CreateExpenseOutcome>
}

class ExpenseCreateRepository(
    context: Context,
    private val api: ApiService = ApiClient.apiService,
    private val idempotency: IdempotencyKeyStore = IdempotencyKeyStore(context),
) : ExpenseCreateGateway {

    override suspend fun createExpense(
        draftKey: String,
        momentId: String,
        amount: String,
        currencyCode: String,
        description: String?,
        merchantName: String?,
        categoryCode: String?,
        effectiveAt: String?,
    ): Result<CreateExpenseOutcome> = runCatching {
        val key = idempotency.keyFor(draftKey)
        val envelope = api.createExpense(
            momentId = momentId,
            idempotencyKey = key,
            body = CreateExpenseBody(
                amount = amount,
                currencyCode = currencyCode.uppercase(),
                description = description?.takeIf { it.isNotBlank() },
                merchantName = merchantName?.takeIf { it.isNotBlank() },
                categoryCode = categoryCode?.takeIf { it.isNotBlank() },
                effectiveAt = effectiveAt?.takeIf { it.isNotBlank() },
            ),
        )
        idempotency.clear(draftKey)
        envelope.toOutcome()
    }.recoverCatching { e -> throw mapThrowable(e) }

    private fun SuccessEnvelope<CreateExpenseResultDto>.toOutcome() =
        CreateExpenseOutcome(
            expenseId = data.expenseId,
            momentId = data.momentId,
            amount = data.amount,
            currencyCode = data.currencyCode,
            status = data.status,
            version = data.version,
            projectionHints = projectionHints?.map {
                ProjectionHint(it.projection, it.action ?: "invalidate")
            } ?: emptyList(),
        )

    private fun mapThrowable(e: Throwable): Throwable = when (e) {
        is ApiResultException -> e
        is HttpException -> {
            val body = e.response()?.errorBody()?.string()
            val code = Regex("\"code\"\\s*:\\s*\"([^\"]+)\"").find(body.orEmpty())?.groupValues?.getOrNull(1)
            val msg = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"").find(body.orEmpty())?.groupValues?.getOrNull(1)
            mapHttpFailure(e.code(), code, msg)
        }
        is IOException -> ApiResultException.Network(cause = e)
        else -> e
    }
}
