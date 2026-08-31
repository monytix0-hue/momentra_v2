package com.example.momentra.data.repository

import com.example.momentra.data.api.ActivityItemDto
import com.example.momentra.data.api.ApiClient
import com.example.momentra.data.api.ApiService
import com.example.momentra.data.api.CreateExpenseBody
import com.example.momentra.data.api.CreateExpenseResultDto
import com.example.momentra.data.api.CreateMovementBody
import com.example.momentra.data.api.CreateMovementResultDto
import com.example.momentra.data.api.CreateFutureItemBody
import com.example.momentra.data.api.CreateFutureItemResultDto
import com.example.momentra.data.api.CreateLifestyleActivityBody
import com.example.momentra.data.api.CreateLifestyleActivityResultDto
import com.example.momentra.data.api.CreateObservationBody
import com.example.momentra.data.api.CreateObservationResultDto
import com.example.momentra.data.api.CreateAttentionCaptureBody
import com.example.momentra.data.api.CreateAttentionCaptureResultDto
import com.example.momentra.data.api.CreateLifeOpsAdjustBody
import com.example.momentra.data.api.CreateLifeOpsAdjustResultDto
import com.example.momentra.data.api.LifeOpsPriorityWeightBody
import com.example.momentra.data.api.PersonalRuntimeSummaryDto
import com.example.momentra.data.api.FutureAxisSnapshotDto
import com.example.momentra.data.api.LifestyleVitalitySnapshotDto
import com.example.momentra.data.api.RelationshipsBondSnapshotDto
import com.example.momentra.data.api.CreateRelationshipActivityBody
import com.example.momentra.data.api.CreateRelationshipActivityResultDto
import com.example.momentra.data.api.AttachExpenseMediaBody
import com.example.momentra.data.api.CreatePersonalIncomeBody
import com.example.momentra.data.api.CreateRecurringScheduleBody
import com.example.momentra.data.api.ExpenseAttachmentDto
import com.example.momentra.data.api.ExpenseDetailDto
import com.example.momentra.data.api.CreateFinancialAccountBody
import com.example.momentra.data.api.FinancialAccountDto
import com.example.momentra.data.api.MediaUploadCompleteBody
import com.example.momentra.data.api.MediaUploadIntentBody
import com.example.momentra.data.api.PersonalIncomeResultDto
import com.example.momentra.data.api.RecurringScheduleDto
import com.example.momentra.data.api.PersonalLifeDto
import com.example.momentra.data.api.PersonalMemoryDto
import com.example.momentra.data.api.PersonalPulseDto
import com.example.momentra.data.api.UpdateExpenseBody
import com.example.momentra.data.api.UpdateLifestyleActivityBody
import com.example.momentra.data.api.mapHttpFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.IOException
import java.util.UUID

data class ActivityPage(
    val items: List<ActivityItemDto>,
    val nextCursor: String?,
)

class PersonalSliceRepository(
    private val api: ApiService = ApiClient.apiService,
) {
    suspend fun getPulse(momentId: String? = null): Result<PersonalPulseDto> = runCatching {
        api.getPersonalPulse(momentId = momentId).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun getLife(): Result<PersonalLifeDto> = runCatching {
        api.getPersonalLife().data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun getActivity(
        momentId: String? = null,
        cursor: String? = null,
        limit: Int = 20,
    ): Result<ActivityPage> = runCatching {
        val env = api.getPersonalActivity(momentId = momentId, cursor = cursor, limit = limit)
        ActivityPage(items = env.data.items, nextCursor = env.nextCursor)
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun getMemory(): Result<PersonalMemoryDto> = runCatching {
        api.getPersonalMemory().data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun recordRelationshipActivity(
        momentId: String,
        activityKind: String,
        displayName: String,
        note: String? = null,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<CreateRelationshipActivityResultDto> = runCatching {
        api.recordRelationshipActivity(
            momentId = momentId,
            idempotencyKey = idempotencyKey,
            body = CreateRelationshipActivityBody(
                activityKind = activityKind,
                displayName = displayName,
                note = note,
            ),
        ).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun createExpense(
        momentId: String,
        amount: String,
        currencyCode: String,
        description: String? = null,
        merchantName: String? = null,
        categoryCode: String? = null,
        subcategoryCode: String? = null,
        financialAccountId: String? = null,
        paymentMethodCode: String? = null,
        effectiveAt: String? = null,
        recurringScheduleId: String? = null,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<CreateExpenseResultDto> = runCatching {
        api.createExpense(
            momentId = momentId,
            idempotencyKey = idempotencyKey,
            body = CreateExpenseBody(
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
            ),
        ).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun getExpense(momentId: String, expenseId: String): Result<ExpenseDetailDto> = runCatching {
        api.getExpense(momentId = momentId, expenseId = expenseId).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun listFinancialAccounts(): Result<List<FinancialAccountDto>> = runCatching {
        api.listFinancialAccounts().data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun createFinancialAccount(
        accountType: String,
        accountName: String,
        currencyCode: String,
        institutionName: String? = null,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<FinancialAccountDto> = runCatching {
        api.createFinancialAccount(
            idempotencyKey = idempotencyKey,
            body = CreateFinancialAccountBody(
                accountType = accountType,
                accountName = accountName,
                currencyCode = currencyCode,
                institutionName = institutionName,
            ),
        ).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun voidExpense(momentId: String, expenseId: String): Result<CreateExpenseResultDto> = runCatching {
        api.voidExpense(momentId = momentId, expenseId = expenseId).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun createPersonalIncome(
        momentId: String,
        amount: String,
        currencyCode: String,
        description: String? = null,
        merchantName: String? = null,
        categoryCode: String? = null,
        financialAccountId: String? = null,
        paymentMethodCode: String? = null,
        effectiveAt: String? = null,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<PersonalIncomeResultDto> = runCatching {
        api.createPersonalIncome(
            momentId = momentId,
            idempotencyKey = idempotencyKey,
            body = CreatePersonalIncomeBody(
                amount = amount,
                currencyCode = currencyCode,
                description = description,
                merchantName = merchantName,
                categoryCode = categoryCode,
                financialAccountId = financialAccountId,
                paymentMethodCode = paymentMethodCode,
                effectiveAt = effectiveAt,
            ),
        ).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun voidPersonalIncome(momentId: String, incomeId: String): Result<PersonalIncomeResultDto> = runCatching {
        api.voidPersonalIncome(momentId = momentId, incomeId = incomeId).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun uploadAndAttachExpenseMedia(
        momentId: String,
        expenseId: String,
        bytes: ByteArray,
        contentType: String = "image/jpeg",
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<ExpenseAttachmentDto> = runCatching {
        val intent = api.createMediaUploadIntent(
            idempotencyKey = idempotencyKey,
            body = MediaUploadIntentBody(
                contentType = contentType,
                byteSize = bytes.size,
                scopeType = "MOMENT",
                scopeId = momentId,
            ),
        ).data
        val storageKey = intent.storageKey ?: error("Upload intent missing storageKey")
        val client = okhttp3.OkHttpClient()
        val putReq = okhttp3.Request.Builder()
            .url(intent.signedUrl)
            .put(bytes.toRequestBody(contentType.toMediaType()))
            .header("Content-Type", contentType)
            .build()
        val putOk = withContext(Dispatchers.IO) {
            client.newCall(putReq).execute().use { it.isSuccessful }
        }
        if (!putOk) error("Failed to upload media bytes to storage")
        api.completeMediaUpload(
            uploadId = intent.uploadId,
            idempotencyKey = UUID.randomUUID().toString(),
            body = MediaUploadCompleteBody(storageKey = storageKey),
        )
        api.attachExpenseMedia(
            momentId = momentId,
            expenseId = expenseId,
            body = AttachExpenseMediaBody(uploadId = intent.uploadId),
        ).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun createRecurringSchedule(
        momentId: String,
        resourceKind: String,
        templatePayload: Map<String, Any?>,
        frequency: String,
        startDate: String,
        intervalCount: Int = 1,
        endDate: String? = null,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<RecurringScheduleDto> = runCatching {
        api.createRecurringSchedule(
            momentId = momentId,
            idempotencyKey = idempotencyKey,
            body = CreateRecurringScheduleBody(
                resourceKind = resourceKind,
                templatePayload = templatePayload,
                frequency = frequency,
                intervalCount = intervalCount,
                startDate = startDate,
                endDate = endDate,
            ),
        ).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun createMovement(
        momentId: String,
        movementType: String,
        amount: String,
        currencyCode: String,
        accountId: String? = null,
        goalId: String? = null,
        description: String? = null,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<CreateMovementResultDto> = runCatching {
        api.createMovement(
            momentId = momentId,
            idempotencyKey = idempotencyKey,
            body = CreateMovementBody(
                movementType = movementType,
                amount = amount,
                currencyCode = currencyCode,
                accountId = accountId,
                goalId = goalId,
                description = description,
            ),
        ).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun recordObservation(
        momentId: String,
        observationType: String,
        numericValue: Double? = null,
        textValue: String? = null,
        note: String? = null,
        activityTypeCode: String? = null,
        durationMinutes: Int? = null,
        energyBeforePct: Double? = null,
        energyAfterPct: Double? = null,
        feelingStateCode: String? = null,
        moodDrivers: List<String>? = null,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<CreateObservationResultDto> = runCatching {
        api.recordObservation(
            momentId = momentId,
            idempotencyKey = idempotencyKey,
            body = CreateObservationBody(
                observationType = observationType,
                numericValue = numericValue,
                textValue = textValue,
                note = note,
                activityTypeCode = activityTypeCode,
                durationMinutes = durationMinutes,
                energyBeforePct = energyBeforePct,
                energyAfterPct = energyAfterPct,
                feelingStateCode = feelingStateCode,
                moodDrivers = moodDrivers,
            ),
        ).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun recordAttentionCapture(
        momentId: String,
        categoryCode: String,
        intensityCode: String,
        timeBlockCode: String,
        energyRemaining: Int? = null,
        note: String? = null,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<CreateAttentionCaptureResultDto> = runCatching {
        api.recordAttentionCapture(
            momentId = momentId,
            idempotencyKey = idempotencyKey,
            body = CreateAttentionCaptureBody(
                categoryCode = categoryCode,
                intensityCode = intensityCode,
                timeBlockCode = timeBlockCode,
                energyRemaining = energyRemaining,
                note = note,
            ),
        ).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun recordLifeOpsAdjust(
        momentId: String,
        rhythmActionCode: String? = null,
        signalDirectionCode: String? = null,
        reason: String? = null,
        priorityWeights: List<LifeOpsPriorityWeightBody>? = null,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<CreateLifeOpsAdjustResultDto> = runCatching {
        api.recordLifeOpsAdjust(
            momentId = momentId,
            idempotencyKey = idempotencyKey,
            body = CreateLifeOpsAdjustBody(
                rhythmActionCode = rhythmActionCode,
                signalDirectionCode = signalDirectionCode,
                reason = reason,
                priorityWeights = priorityWeights,
            ),
        ).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun getRuntimeSummary(momentId: String): Result<PersonalRuntimeSummaryDto> = runCatching {
        api.getPersonalRuntimeSummary(momentId).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun getFutureAxisSnapshot(momentId: String): Result<FutureAxisSnapshotDto> = runCatching {
        api.getFutureAxisSnapshot(momentId).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun getLifestyleVitalitySnapshot(momentId: String): Result<LifestyleVitalitySnapshotDto> = runCatching {
        api.getLifestyleVitalitySnapshot(momentId).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun getRelationshipsBondSnapshot(momentId: String): Result<RelationshipsBondSnapshotDto> = runCatching {
        api.getRelationshipsBondSnapshot(momentId).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun createFutureItem(
        momentId: String,
        kind: String,
        title: String,
        description: String? = null,
        progressValue: Double? = null,
        opportunityType: String? = null,
        pivotReason: String? = null,
        providerName: String? = null,
        progressType: String? = null,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<CreateFutureItemResultDto> = runCatching {
        api.createFutureItem(
            momentId = momentId,
            idempotencyKey = idempotencyKey,
            body = CreateFutureItemBody(
                kind = kind,
                title = title,
                description = description,
                progressValue = progressValue,
                opportunityType = opportunityType,
                pivotReason = pivotReason,
                providerName = providerName,
                progressType = progressType,
            ),
        ).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun createLifestyleActivity(
        momentId: String,
        lifestyleContext: String,
        title: String,
        description: String? = null,
        wellbeingRating: Double? = null,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<CreateLifestyleActivityResultDto> = runCatching {
        api.createLifestyleActivity(
            momentId = momentId,
            idempotencyKey = idempotencyKey,
            body = CreateLifestyleActivityBody(
                lifestyleContext = lifestyleContext,
                title = title,
                description = description,
                wellbeingRating = wellbeingRating,
            ),
        ).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun updateLifestyleActivity(
        momentId: String,
        activityId: String,
        title: String? = null,
        description: String? = null,
        wellbeingRating: Double? = null,
    ): Result<CreateLifestyleActivityResultDto> = runCatching {
        api.updateLifestyleActivity(
            momentId = momentId,
            activityId = activityId,
            body = UpdateLifestyleActivityBody(
                title = title,
                description = description,
                wellbeingRating = wellbeingRating,
            ),
        ).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun updateExpense(
        momentId: String,
        expenseId: String,
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
    ): Result<CreateExpenseResultDto> = runCatching {
        api.updateExpense(
            momentId = momentId,
            expenseId = expenseId,
            body = UpdateExpenseBody(
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
            ),
        ).data
    }.recoverCatching { e -> throw mapError(e) }

    private fun mapError(e: Throwable): Throwable = when (e) {
        is HttpException -> {
            val body = e.response()?.errorBody()?.string()
            val code = Regex("\"code\"\\s*:\\s*\"([^\"]+)\"").find(body.orEmpty())?.groupValues?.getOrNull(1)
            val msg = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"").find(body.orEmpty())?.groupValues?.getOrNull(1)
            mapHttpFailure(e.code(), code, msg)
        }
        is IOException -> com.example.momentra.data.api.ApiResultException.Network(cause = e)
        else -> e
    }
}
