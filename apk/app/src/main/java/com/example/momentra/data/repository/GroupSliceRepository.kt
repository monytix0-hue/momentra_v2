package com.example.momentra.data.repository

import com.example.momentra.data.api.AddParticipantBody
import com.example.momentra.data.api.AddResidentBody
import com.example.momentra.data.api.AnalyticsInsightItemDto
import com.example.momentra.data.api.AnalyticsMetricItemDto
import com.example.momentra.data.api.AnalyticsRefreshBody
import com.example.momentra.data.api.ApiClient
import com.example.momentra.data.api.ApiService
import com.example.momentra.data.api.AttachMemoryMediaBody
import com.example.momentra.data.api.CreateBookingBody
import com.example.momentra.data.api.CreateGroupExpenseBody
import com.example.momentra.data.api.CreateGroupExpenseResultDto
import com.example.momentra.data.api.GroupExpenseDetailDto
import com.example.momentra.data.api.CreateMemoryBody
import com.example.momentra.data.api.CreatePlanningItemBody
import com.example.momentra.data.api.CreatePollBody
import com.example.momentra.data.api.CreateDeliveryHandoverBody
import com.example.momentra.data.api.CreateOwnershipRecordBody
import com.example.momentra.data.api.CreatePurchaseItemBody
import com.example.momentra.data.api.CreateSharedAssetBody
import com.example.momentra.data.api.CreateLivingRuleBody
import com.example.momentra.data.api.CreateMaintenanceRecordBody
import com.example.momentra.data.api.CreateGroupVendorBody
import com.example.momentra.data.api.CreateSettlementBody
import com.example.momentra.data.api.CreateSettlementResultDto
import com.example.momentra.data.api.GroupExpenseSplitInputDto
import com.example.momentra.data.api.GroupFacetDto
import com.example.momentra.data.api.GroupFinancePayloadDto
import com.example.momentra.data.api.GroupLifePayloadDto
import com.example.momentra.data.api.GroupMemoryPayloadDto
import com.example.momentra.data.api.GroupParticipantsDto
import com.example.momentra.data.api.GroupPulsePayloadDto
import com.example.momentra.data.api.GroupInviteDto
import com.example.momentra.data.api.CompanyInviteDto
import com.example.momentra.data.api.MediaUploadCompleteBody
import com.example.momentra.data.api.MediaUploadIntentBody
import com.example.momentra.data.api.MintGroupInviteBody
import com.example.momentra.data.api.PatchGroupBudgetBody
import com.example.momentra.data.api.PatchGroupBudgetResultDto
import com.example.momentra.data.api.RemoveGroupParticipantResultDto
import com.example.momentra.data.api.UpdateGroupParticipantRoleBody
import com.example.momentra.data.api.UpdateGroupParticipantRoleResultDto
import com.example.momentra.data.api.PostUpdateBody
import com.example.momentra.data.api.RecordAttendanceBody
import com.example.momentra.data.api.RecordContributionBody
import com.example.momentra.data.api.RecordContributionResultDto
import com.example.momentra.data.api.RedeemGroupInviteResultDto
import com.example.momentra.data.api.VotePollBody
import com.example.momentra.data.api.mapHttpFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Builds group expense split request bodies.
 * Server computes share amounts for all strategies (EQUAL / PERCENTAGE / EXACT / SHARES).
 */
object GroupExpenseSplitBuilder {
    fun equalSplit(
        amount: String,
        currencyCode: String,
        paidByParticipantId: String,
        participantIds: List<String>,
        description: String? = null,
    ): CreateGroupExpenseBody = build(
        amount = amount,
        currencyCode = currencyCode,
        paidByParticipantId = paidByParticipantId,
        splitStrategy = "EQUAL",
        splitInputs = participantIds.map { GroupExpenseSplitInputDto(participantId = it) },
        description = description,
    )

    fun build(
        amount: String,
        currencyCode: String,
        paidByParticipantId: String,
        splitStrategy: String,
        splitInputs: List<GroupExpenseSplitInputDto>,
        description: String? = null,
    ): CreateGroupExpenseBody = CreateGroupExpenseBody(
        amount = amount,
        currencyCode = currencyCode.uppercase(),
        description = description?.takeIf { it.isNotBlank() },
        paidByParticipantId = paidByParticipantId,
        splitStrategy = splitStrategy.uppercase(),
        splitInputs = splitInputs,
    )
}

class GroupSliceRepository(
    private val api: ApiService = ApiClient.apiService,
) {
    suspend fun getPulse(momentId: String): Result<GroupFacetDto<GroupPulsePayloadDto>> = runCatching {
        api.getGroupPulse(momentId).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun getLife(momentId: String): Result<GroupFacetDto<GroupLifePayloadDto>> = runCatching {
        api.getGroupLife(momentId).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun getMemory(momentId: String): Result<GroupFacetDto<GroupMemoryPayloadDto>> = runCatching {
        api.getGroupMemory(momentId).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun getFinance(momentId: String): Result<GroupFacetDto<GroupFinancePayloadDto>> = runCatching {
        api.getGroupFinance(momentId).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun getActivity(
        momentId: String,
        cursor: String? = null,
        limit: Int = 20,
    ): Result<ActivityPage> = runCatching {
        val env = api.getGroupActivity(momentId = momentId, cursor = cursor, limit = limit)
        ActivityPage(items = env.data.items, nextCursor = env.nextCursor ?: env.data.nextCursor)
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun getParticipants(momentId: String): Result<GroupParticipantsDto> = runCatching {
        api.getGroupParticipants(momentId).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun updateParticipantRole(
        momentId: String,
        participantId: String,
        roleCode: String,
    ): Result<UpdateGroupParticipantRoleResultDto> = runCatching {
        api.updateGroupParticipantRole(
            momentId = momentId,
            participantId = participantId,
            idempotencyKey = UUID.randomUUID().toString(),
            body = UpdateGroupParticipantRoleBody(roleCode = roleCode),
        ).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun removeParticipant(
        momentId: String,
        participantId: String,
    ): Result<RemoveGroupParticipantResultDto> = runCatching {
        api.removeGroupParticipant(
            momentId = momentId,
            participantId = participantId,
            idempotencyKey = UUID.randomUUID().toString(),
        ).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun listAnalyticsInsights(
        scopeType: String = "MOMENT",
        scopeId: String,
    ): Result<List<AnalyticsInsightItemDto>> = runCatching {
        api.listAnalyticsInsights(scopeType = scopeType, scopeId = scopeId).data.items
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun listAnalyticsMetrics(
        scopeType: String = "MOMENT",
        scopeId: String,
    ): Result<List<AnalyticsMetricItemDto>> = runCatching {
        api.listAnalyticsMetrics(scopeType = scopeType, scopeId = scopeId).data.items
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun refreshAnalytics(
        context: String = "GROUP_PULSE",
        momentId: String,
    ): Result<Unit> = runCatching {
        api.refreshAnalytics(
            idempotencyKey = UUID.randomUUID().toString(),
            body = AnalyticsRefreshBody(context = context, momentId = momentId),
        )
        Unit
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun createGroupExpense(
        momentId: String,
        body: CreateGroupExpenseBody,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<CreateGroupExpenseResultDto> = runCatching {
        api.createGroupExpense(
            momentId = momentId,
            idempotencyKey = idempotencyKey,
            body = body,
        ).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun getGroupExpense(
        momentId: String,
        expenseId: String,
    ): Result<GroupExpenseDetailDto> = runCatching {
        api.getGroupExpense(momentId = momentId, expenseId = expenseId).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun updateGroupExpense(
        momentId: String,
        expenseId: String,
        body: CreateGroupExpenseBody,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<CreateGroupExpenseResultDto> = runCatching {
        api.updateGroupExpense(
            momentId = momentId,
            expenseId = expenseId,
            idempotencyKey = idempotencyKey,
            body = body,
        ).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun voidGroupExpense(
        momentId: String,
        expenseId: String,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<CreateGroupExpenseResultDto> = runCatching {
        api.voidGroupExpense(
            momentId = momentId,
            expenseId = expenseId,
            idempotencyKey = idempotencyKey,
        ).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun recordContribution(
        momentId: String,
        amount: String,
        currencyCode: String,
        label: String? = null,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<RecordContributionResultDto> = runCatching {
        api.recordContribution(
            momentId = momentId,
            idempotencyKey = idempotencyKey,
            body = RecordContributionBody(
                amount = amount,
                currencyCode = currencyCode.uppercase(),
                label = label?.takeIf { it.isNotBlank() },
            ),
        ).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun createSettlement(
        momentId: String,
        payerParticipantId: String,
        payeeParticipantId: String,
        amount: String,
        currencyCode: String,
        obligationIds: List<String>? = null,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<CreateSettlementResultDto> = runCatching {
        api.createSettlement(
            momentId = momentId,
            idempotencyKey = idempotencyKey,
            body = CreateSettlementBody(
                payerParticipantId = payerParticipantId,
                payeeParticipantId = payeeParticipantId,
                amount = amount,
                currencyCode = currencyCode.uppercase(),
                obligationIds = obligationIds,
            ),
        ).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun redeemGroupInvite(
        code: String,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<RedeemGroupInviteResultDto> = runCatching {
        api.redeemGroupInvite(
            code = code,
            idempotencyKey = idempotencyKey,
        ).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun previewGroupInvite(code: String): Result<GroupInviteDto> = runCatching {
        api.getGroupInvite(code).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun previewCompanyInvite(code: String): Result<CompanyInviteDto> = runCatching {
        api.getCompanyInvite(code).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun mintCompanyInvite(
        companyId: String,
        membershipType: String = "MEMBER",
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<com.example.momentra.data.api.CompanyInviteDto> = runCatching {
        api.mintCompanyInvite(
            idempotencyKey = idempotencyKey,
            body = com.example.momentra.data.api.MintCompanyInviteBody(
                companyId = companyId,
                membershipType = membershipType,
            ),
        ).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun redeemCompanyInvite(
        code: String,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<com.example.momentra.data.api.RedeemCompanyInviteResultDto> = runCatching {
        api.redeemCompanyInvite(
            code = code,
            idempotencyKey = idempotencyKey,
        ).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun patchGroupBudget(
        momentId: String,
        budgetAmount: String,
        budgetCurrencyCode: String,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<PatchGroupBudgetResultDto> = runCatching {
        api.patchGroupBudget(
            momentId = momentId,
            idempotencyKey = idempotencyKey,
            body = PatchGroupBudgetBody(
                budgetAmount = budgetAmount,
                budgetCurrencyCode = budgetCurrencyCode.uppercase(),
            ),
        ).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun listPlanningItems(momentId: String) = runCatching {
        api.listPlanningItems(momentId).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun listBookings(momentId: String) = runCatching {
        api.listBookings(momentId).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun listUpdates(momentId: String) = runCatching {
        api.listUpdates(momentId).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun listPolls(momentId: String) = runCatching {
        api.listPolls(momentId).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun getPoll(pollId: String) = runCatching {
        api.getPoll(pollId).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun listMemories(momentId: String) = runCatching {
        api.listMemories(momentId).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun createPlanningItem(
        momentId: String,
        title: String,
        dueAt: String? = null,
        categoryCode: String? = null,
        location: String? = null,
        priorityCode: String? = null,
        description: String? = null,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ) = runCatching {
        api.createPlanningItem(
            momentId,
            idempotencyKey,
            CreatePlanningItemBody(
                title = title,
                dueAt = dueAt,
                categoryCode = categoryCode,
                location = location,
                priorityCode = priorityCode,
                description = description,
            ),
        ).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun createBooking(
        momentId: String,
        title: String,
        bookedAt: String? = null,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ) = runCatching {
        api.createBooking(
            momentId,
            idempotencyKey,
            CreateBookingBody(title = title, bookedAt = bookedAt),
        ).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun createPoll(
        momentId: String,
        question: String,
        options: List<String>,
        closesAt: String? = null,
        pollType: String? = null,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ) = runCatching {
        api.createPoll(
            momentId,
            idempotencyKey,
            CreatePollBody(
                question = question,
                options = options,
                closesAt = closesAt,
                pollType = pollType,
            ),
        ).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun postUpdate(
        momentId: String,
        message: String,
        notifyMembers: Boolean = true,
        urgencyCode: String = "NORMAL",
        idempotencyKey: String = UUID.randomUUID().toString(),
    ) = runCatching {
        api.postUpdate(
            momentId,
            idempotencyKey,
            PostUpdateBody(message = message, notifyMembers = notifyMembers, urgencyCode = urgencyCode),
        ).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun createMemory(
        momentId: String,
        title: String,
        capturedAt: String? = null,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ) = runCatching {
        api.createMemory(
            momentId,
            idempotencyKey,
            CreateMemoryBody(title = title, capturedAt = capturedAt),
        ).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun uploadAndAttachMemoryMedia(
        momentId: String,
        memoryId: String,
        bytes: ByteArray,
        contentType: String = "image/jpeg",
        idempotencyKey: String = UUID.randomUUID().toString(),
    ) = runCatching {
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
        val putOk = putBytesToSignedUrl(intent.signedUrl, bytes, contentType)
        if (!putOk) error("Failed to upload media bytes to storage")
        api.completeMediaUpload(
            uploadId = intent.uploadId,
            idempotencyKey = UUID.randomUUID().toString(),
            body = MediaUploadCompleteBody(storageKey = storageKey),
        )
        api.attachMemoryMedia(
            momentId = momentId,
            memoryId = memoryId,
            idempotencyKey = UUID.randomUUID().toString(),
            body = AttachMemoryMediaBody(uploadId = intent.uploadId),
        ).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun mintInviteForMoment(
        title: String,
        momentTypeCode: String,
        momentId: String,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ) = runCatching {
        api.mintGroupInvite(
            idempotencyKey,
            MintGroupInviteBody(
                title = title,
                momentTypeCode = momentTypeCode,
                momentId = momentId,
            ),
        ).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun addParticipant(
        momentId: String,
        displayName: String,
        roleCode: String = "PARTICIPANT",
        email: String? = null,
        phone: String? = null,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ) = runCatching {
        api.addParticipant(
            momentId,
            idempotencyKey,
            AddParticipantBody(
                displayName = displayName,
                roleCode = roleCode,
                email = email,
                phone = phone,
            ),
        ).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun createPurchaseItem(
        momentId: String,
        label: String,
        amount: String? = null,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ) = runCatching {
        api.createPurchaseItem(
            momentId,
            idempotencyKey,
            CreatePurchaseItemBody(label = label, amount = amount),
        ).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun createDeliveryHandover(
        momentId: String,
        recipientName: String? = null,
        handoverType: String? = null,
        scheduledAt: String? = null,
        address: String? = null,
        note: String? = null,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ) = runCatching {
        api.createDeliveryHandover(
            momentId,
            idempotencyKey,
            CreateDeliveryHandoverBody(
                recipientName = recipientName,
                handoverType = handoverType,
                scheduledAt = scheduledAt,
                address = address,
                note = note,
            ),
        ).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun createOwnershipRecord(
        momentId: String,
        assetLabel: String? = null,
        fromOwnerName: String? = null,
        toParticipantName: String? = null,
        ownershipShare: Double? = null,
        ownershipNote: String? = null,
        effectiveAt: String? = null,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ) = runCatching {
        api.createOwnershipRecord(
            momentId,
            idempotencyKey,
            CreateOwnershipRecordBody(
                assetLabel = assetLabel,
                fromOwnerName = fromOwnerName,
                toParticipantName = toParticipantName,
                ownershipShare = ownershipShare,
                ownershipNote = ownershipNote,
                effectiveAt = effectiveAt,
            ),
        ).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun addResident(
        momentId: String,
        name: String,
        roleCode: String? = null,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ) = runCatching {
        api.addResident(momentId, idempotencyKey, AddResidentBody(name = name, roleCode = roleCode)).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun createLivingRule(
        momentId: String,
        title: String,
        ruleText: String,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ) = runCatching {
        api.createLivingRule(
            momentId,
            idempotencyKey,
            CreateLivingRuleBody(title = title, ruleText = ruleText),
        ).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun votePoll(
        pollId: String,
        pollOptionId: String,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ) = runCatching {
        api.votePoll(pollId, idempotencyKey, VotePollBody(pollOptionId)).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun closePoll(
        pollId: String,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ) = runCatching {
        api.closePoll(pollId, idempotencyKey).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun createGroupVendor(
        momentId: String,
        vendorName: String,
        vendorType: String? = null,
        phone: String? = null,
        email: String? = null,
        notes: String? = null,
        quotedPrice: String? = null,
        statusLabel: String? = null,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ) = runCatching {
        api.createGroupVendor(
            momentId,
            idempotencyKey,
            CreateGroupVendorBody(
                vendorName = vendorName,
                vendorType = vendorType,
                phone = phone,
                email = email,
                notes = notes,
                quotedPrice = quotedPrice,
                statusLabel = statusLabel,
            ),
        ).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun recordAttendance(
        momentId: String,
        participantId: String,
        attendanceStatus: String,
        note: String? = null,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ) = runCatching {
        api.recordAttendance(
            momentId,
            idempotencyKey,
            RecordAttendanceBody(
                participantId = participantId,
                attendanceStatus = attendanceStatus,
                note = note,
            ),
        ).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun listPurchaseItems(momentId: String) = runCatching {
        api.listPurchaseItems(momentId).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun listResidents(momentId: String) = runCatching {
        api.listResidents(momentId).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun listSharedAssets(momentId: String) = runCatching {
        api.listSharedAssets(momentId).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun createSharedAsset(
        momentId: String,
        title: String,
        assetType: String? = null,
        conditionCode: String? = null,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ) = runCatching {
        api.createSharedAsset(
            momentId,
            idempotencyKey,
            CreateSharedAssetBody(
                title = title,
                assetType = assetType,
                conditionCode = conditionCode,
            ),
        ).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun listMaintenanceRecords(momentId: String) = runCatching {
        api.listMaintenanceRecords(momentId).data
    }.recoverCatching { e -> throw mapError(e) }

    suspend fun createMaintenanceRecord(
        momentId: String,
        title: String,
        description: String? = null,
        sharedAssetId: String? = null,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ) = runCatching {
        api.createMaintenanceRecord(
            momentId,
            idempotencyKey,
            CreateMaintenanceRecordBody(
                title = title,
                description = description,
                sharedAssetId = sharedAssetId,
            ),
        ).data
    }.recoverCatching { e -> throw mapError(e) }

    private suspend fun putBytesToSignedUrl(
        signedUrl: String,
        bytes: ByteArray,
        contentType: String,
    ): Boolean = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
        val request = Request.Builder()
            .url(signedUrl)
            .put(bytes.toRequestBody(contentType.toMediaType()))
            .header("Content-Type", contentType)
            .build()
        client.newCall(request).execute().use { it.isSuccessful }
    }

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
