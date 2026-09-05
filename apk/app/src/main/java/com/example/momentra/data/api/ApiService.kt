package com.example.momentra.data.api

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Hand-maintained Retrofit contract — Phase 4/5 shell endpoints only. */
interface ApiService {

    @GET("v1/me")
    suspend fun getMe(): SuccessEnvelope<MeBootstrapDto>

    @PATCH("v1/me")
    suspend fun patchMe(@Body body: PatchMeBody): SuccessEnvelope<PatchMeResultDto>

    @DELETE("v1/me")
    suspend fun deleteMe(): SuccessEnvelope<SoftDeleteMeResultDto>

    @GET("v1/me/devices")
    suspend fun listDevices(): SuccessEnvelope<DeviceListDto>

    @GET("v1/me/consents")
    suspend fun listConsents(): SuccessEnvelope<ConsentListDto>

    @POST("v1/me/consents/grant")
    suspend fun grantConsent(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: ConsentPurposeBody,
    ): SuccessEnvelope<ConsentMutationResultDto>

    @POST("v1/me/consents/withdraw")
    suspend fun withdrawConsent(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: ConsentPurposeBody,
    ): SuccessEnvelope<ConsentMutationResultDto>

    @POST("v1/telemetry/events")
    suspend fun ingestTelemetry(@Body body: TelemetryIngestBody): SuccessEnvelope<TelemetryIngestResultDto>

    @POST("v1/me/devices")
    suspend fun registerDevice(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: RegisterDeviceBody,
    ): SuccessEnvelope<RegisterDeviceResultDto>

    @DELETE("v1/me/devices/{deviceId}")
    suspend fun revokeDevice(@Path("deviceId") deviceId: String): SuccessEnvelope<RevokeDeviceResultDto>

    @GET("v1/me/notification-preferences")
    suspend fun getMyNotificationPreferences(): SuccessEnvelope<GlobalNotificationPrefsDto>

    @PATCH("v1/me/notification-preferences")
    suspend fun patchMyNotificationPreferences(
        @Body body: PatchGlobalNotificationPrefsBody,
    ): SuccessEnvelope<GlobalNotificationPrefsDto>

    @GET("v1/moments/{momentId}/notification-preferences")
    suspend fun getMomentNotificationPreferences(
        @Path("momentId") momentId: String,
    ): SuccessEnvelope<MomentNotificationPrefsDto>

    @PATCH("v1/moments/{momentId}/notification-preferences")
    suspend fun patchMomentNotificationPreferences(
        @Path("momentId") momentId: String,
        @Body body: PatchMomentNotificationPrefsBody,
    ): SuccessEnvelope<MomentNotificationPrefsDto>

    @GET("v1/me/notifications")
    suspend fun listMyNotifications(
        @Query("limit") limit: Int? = null,
        @Query("unreadOnly") unreadOnly: Boolean? = null,
        @Query("cursor") cursor: String? = null,
    ): SuccessEnvelope<NotificationInboxDto>

    @POST("v1/me/notifications/read")
    suspend fun markMyNotificationsRead(
        @Body body: MarkNotificationsReadBody,
    ): SuccessEnvelope<MarkNotificationsReadResultDto>

    @GET("v1/me/notifications/metrics")
    suspend fun getMyNotificationMetrics(): SuccessEnvelope<NotificationDeliveryMetricsDto>

    @GET("v1/analytics/metrics")
    suspend fun listAnalyticsMetrics(
        @Query("scopeType") scopeType: String? = null,
        @Query("scopeId") scopeId: String? = null,
    ): SuccessEnvelope<AnalyticsMetricsDto>

    @GET("v1/analytics/insights")
    suspend fun listAnalyticsInsights(
        @Query("scopeType") scopeType: String? = null,
        @Query("scopeId") scopeId: String? = null,
    ): SuccessEnvelope<AnalyticsInsightsDto>

    @POST("v1/analytics/refresh")
    suspend fun refreshAnalytics(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: AnalyticsRefreshBody,
    ): SuccessEnvelope<AnalyticsRefreshResultDto>

    @GET("v1/personal/pulse")
    suspend fun getPersonalPulse(
        @Query("momentId") momentId: String? = null,
    ): SuccessEnvelope<PersonalPulseDto>

    @GET("v1/personal/life")
    suspend fun getPersonalLife(): SuccessEnvelope<PersonalLifeDto>

    @GET("v1/personal/moments")
    suspend fun listPersonalMoments(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20,
    ): SuccessEnvelope<CursorPageDto<PersonalMomentItemDto>>

    @GET("v1/group/moments")
    suspend fun listGroupMoments(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20,
    ): SuccessEnvelope<CursorPageDto<GroupMomentItemDto>>

    @GET("v1/business/moments")
    suspend fun listBusinessMoments(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20,
    ): SuccessEnvelope<CursorPageDto<BusinessMomentItemDto>>

    @GET("v1/companies")
    suspend fun listCompanies(): SuccessEnvelope<CompanyListDto>

    @GET("v1/life360")
    suspend fun getLife360(): SuccessEnvelope<Map<String, Any>>

    @POST("v1/companies")
    suspend fun createCompany(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateCompanyBody,
    ): SuccessEnvelope<CreateCompanyResultDto>

    @GET("v1/companies/{companyId}/locations")
    suspend fun listLocations(@Path("companyId") companyId: String): SuccessEnvelope<LocationListDto>

    @POST("v1/companies/{companyId}/locations")
    suspend fun createLocation(
        @Path("companyId") companyId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateLocationBody,
    ): SuccessEnvelope<CreateLocationResultDto>

    // --- S1 Personal slice ---
    @POST("v1/moments")
    suspend fun createMoment(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateMomentBody,
    ): SuccessEnvelope<CreateMomentResultDto>

    @GET("v1/moments/{momentId}")
    suspend fun getMoment(@Path("momentId") momentId: String): SuccessEnvelope<MomentDetailDto>

    @GET("v1/group/moments/{momentId}/setup")
    suspend fun getGroupSetupPrefill(@Path("momentId") momentId: String): SuccessEnvelope<GroupSetupPrefillDto>

    @GET("v1/moments/{momentId}/setup")
    suspend fun getDomainSetupPrefill(@Path("momentId") momentId: String): SuccessEnvelope<DomainSetupPrefillDto>

    @POST("v1/moments/{momentId}/activate")
    suspend fun activateMoment(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
    ): SuccessEnvelope<CreateMomentResultDto>

    @POST("v1/moments/{momentId}/discard-draft")
    suspend fun discardMomentDraft(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
    ): SuccessEnvelope<DiscardMomentDraftResultDto>

    @PATCH("v1/moments/{momentId}")
    suspend fun updateMoment(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: UpdateMomentBody,
    ): SuccessEnvelope<MomentLifecycleResultDto>

    @POST("v1/moments/{momentId}/archive")
    suspend fun archiveMoment(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: MomentVersionBody,
    ): SuccessEnvelope<MomentLifecycleResultDto>

    @POST("v1/moments/{momentId}/cancel")
    suspend fun cancelMoment(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: MomentVersionBody,
    ): SuccessEnvelope<MomentLifecycleResultDto>

    @POST("v1/moments/{momentId}/delete")
    suspend fun deleteMoment(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: MomentVersionBody,
    ): SuccessEnvelope<MomentLifecycleResultDto>

    @POST("v1/moments/{momentId}/expenses")
    suspend fun createExpense(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateExpenseBody,
    ): SuccessEnvelope<CreateExpenseResultDto>

    @POST("v1/moments/{momentId}/movements")
    suspend fun createMovement(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateMovementBody,
    ): SuccessEnvelope<CreateMovementResultDto>

    @POST("v1/moments/{momentId}/observations")
    suspend fun recordObservation(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateObservationBody,
    ): SuccessEnvelope<CreateObservationResultDto>

    @POST("v1/moments/{momentId}/attention-captures")
    suspend fun recordAttentionCapture(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateAttentionCaptureBody,
    ): SuccessEnvelope<CreateAttentionCaptureResultDto>

    @POST("v1/moments/{momentId}/life-ops-adjustments")
    suspend fun recordLifeOpsAdjust(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateLifeOpsAdjustBody,
    ): SuccessEnvelope<CreateLifeOpsAdjustResultDto>

    @GET("v1/personal/moments/{momentId}/runtime-summary")
    suspend fun getPersonalRuntimeSummary(
        @Path("momentId") momentId: String,
    ): SuccessEnvelope<PersonalRuntimeSummaryDto>

    @POST("v1/moments/{momentId}/future-items")
    suspend fun createFutureItem(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateFutureItemBody,
    ): SuccessEnvelope<CreateFutureItemResultDto>

    @POST("v1/moments/{momentId}/lifestyle-activities")
    suspend fun createLifestyleActivity(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateLifestyleActivityBody,
    ): SuccessEnvelope<CreateLifestyleActivityResultDto>

    @PATCH("v1/moments/{momentId}/lifestyle-activities/{activityId}")
    suspend fun updateLifestyleActivity(
        @Path("momentId") momentId: String,
        @Path("activityId") activityId: String,
        @Body body: UpdateLifestyleActivityBody,
    ): SuccessEnvelope<CreateLifestyleActivityResultDto>

    @DELETE("v1/moments/{momentId}/lifestyle-activities/{activityId}")
    suspend fun voidLifestyleActivity(
        @Path("momentId") momentId: String,
        @Path("activityId") activityId: String,
    ): SuccessEnvelope<VoidLifestyleActivityResultDto>

    @GET("v1/moments/{momentId}/expenses/{expenseId}")
    suspend fun getExpense(
        @Path("momentId") momentId: String,
        @Path("expenseId") expenseId: String,
    ): SuccessEnvelope<ExpenseDetailDto>

    @PATCH("v1/moments/{momentId}/expenses/{expenseId}")
    suspend fun updateExpense(
        @Path("momentId") momentId: String,
        @Path("expenseId") expenseId: String,
        @Body body: UpdateExpenseBody,
    ): SuccessEnvelope<CreateExpenseResultDto>

    @DELETE("v1/moments/{momentId}/expenses/{expenseId}")
    suspend fun voidExpense(
        @Path("momentId") momentId: String,
        @Path("expenseId") expenseId: String,
    ): SuccessEnvelope<CreateExpenseResultDto>

    @GET("v1/financial-accounts")
    suspend fun listFinancialAccounts(): SuccessEnvelope<List<FinancialAccountDto>>

    @POST("v1/financial-accounts")
    suspend fun createFinancialAccount(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateFinancialAccountBody,
    ): SuccessEnvelope<FinancialAccountDto>

    @POST("v1/moments/{momentId}/income")
    suspend fun createPersonalIncome(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreatePersonalIncomeBody,
    ): SuccessEnvelope<PersonalIncomeResultDto>

    @DELETE("v1/moments/{momentId}/income/{incomeId}")
    suspend fun voidPersonalIncome(
        @Path("momentId") momentId: String,
        @Path("incomeId") incomeId: String,
    ): SuccessEnvelope<PersonalIncomeResultDto>

    @GET("v1/moments/{momentId}/expenses/{expenseId}/attachments")
    suspend fun listExpenseAttachments(
        @Path("momentId") momentId: String,
        @Path("expenseId") expenseId: String,
    ): SuccessEnvelope<List<ExpenseAttachmentDto>>

    @POST("v1/moments/{momentId}/expenses/{expenseId}/attachments")
    suspend fun attachExpenseMedia(
        @Path("momentId") momentId: String,
        @Path("expenseId") expenseId: String,
        @Body body: AttachExpenseMediaBody,
    ): SuccessEnvelope<ExpenseAttachmentDto>

    @DELETE("v1/moments/{momentId}/expenses/{expenseId}/attachments/{uploadId}")
    suspend fun detachExpenseMedia(
        @Path("momentId") momentId: String,
        @Path("expenseId") expenseId: String,
        @Path("uploadId") uploadId: String,
    ): retrofit2.Response<Unit>

    @POST("v1/media/uploads")
    suspend fun createMediaUploadIntent(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: MediaUploadIntentBody,
    ): SuccessEnvelope<MediaUploadIntentResultDto>

    @POST("v1/media/uploads/{uploadId}/complete")
    suspend fun completeMediaUpload(
        @Path("uploadId") uploadId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: MediaUploadCompleteBody,
    ): SuccessEnvelope<MediaUploadCompleteResultDto>

    @GET("v1/moments/{momentId}/recurring-schedules")
    suspend fun listRecurringSchedules(
        @Path("momentId") momentId: String,
    ): SuccessEnvelope<List<RecurringScheduleDto>>

    @POST("v1/moments/{momentId}/recurring-schedules")
    suspend fun createRecurringSchedule(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateRecurringScheduleBody,
    ): SuccessEnvelope<RecurringScheduleDto>

    @PATCH("v1/moments/{momentId}/recurring-schedules/{scheduleId}")
    suspend fun updateRecurringSchedule(
        @Path("momentId") momentId: String,
        @Path("scheduleId") scheduleId: String,
        @Body body: UpdateRecurringScheduleBody,
    ): SuccessEnvelope<RecurringScheduleDto>

    @POST("v1/moments/{momentId}/recurring-schedules/{scheduleId}/generate")
    suspend fun generateRecurringInstance(
        @Path("momentId") momentId: String,
        @Path("scheduleId") scheduleId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
    ): SuccessEnvelope<GenerateRecurringInstanceResultDto>

    @GET("v1/personal/activity")
    suspend fun getPersonalActivity(
        @Query("momentId") momentId: String? = null,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20,
    ): SuccessEnvelope<CursorPageDto<ActivityItemDto>>

    @GET("v1/personal/memory")
    suspend fun getPersonalMemory(): SuccessEnvelope<PersonalMemoryDto>

    @GET("v1/personal/attention")
    suspend fun getPersonalAttention(): SuccessEnvelope<PersonalAttentionDto>

    @GET("v1/personal/setups")
    suspend fun getPersonalSetups(): SuccessEnvelope<PersonalSetupsDto>

    @POST("v1/personal/setups/{systemCode}/activate")
    suspend fun activatePersonalSetup(
        @Path("systemCode") systemCode: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: ActivatePersonalSetupBody,
    ): SuccessEnvelope<ActivatePersonalSetupResultDto>

    @PATCH("v1/personal/setups/{setupId}")
    suspend fun patchPersonalSetup(
        @Path("setupId") setupId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: PatchPersonalSetupBody,
    ): SuccessEnvelope<PatchPersonalSetupResultDto>

    @GET("v1/business/setups")
    suspend fun getBusinessSetups(): SuccessEnvelope<BusinessSetupsDto>

    @POST("v1/business/setups/{familyCode}/activate")
    suspend fun activateBusinessSetup(
        @Path("familyCode") familyCode: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: ActivateBusinessSetupBody,
    ): SuccessEnvelope<ActivateBusinessSetupResultDto>

    @GET("v1/finance/expense-categories")
    suspend fun listExpenseCategories(): SuccessEnvelope<ExpenseCategoriesDto>

    @POST("v1/moments/{momentId}/goals")
    suspend fun createGoal(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateGoalBody,
    ): SuccessEnvelope<CreateGoalResultDto>

    @POST("v1/moments/{momentId}/tasks")
    suspend fun createTask(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateTaskBody,
    ): SuccessEnvelope<CreateTaskResultDto>

    @POST("v1/ai/action-proposals/{actionProposalId}/execute")
    suspend fun executeActionProposal(
        @Path("actionProposalId") actionProposalId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: Map<String, @JvmSuppressWildcards Any> = emptyMap(),
    ): SuccessEnvelope<ExecuteActionProposalResultDto>

    @POST("v1/moments/{momentId}/relationship-activities")
    suspend fun recordRelationshipActivity(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateRelationshipActivityBody,
    ): SuccessEnvelope<CreateRelationshipActivityResultDto>

    // --- S2A Group invites ---
    @POST("v1/group/invites")
    suspend fun mintGroupInvite(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: MintGroupInviteBody,
    ): SuccessEnvelope<GroupInviteDto>

    @GET("v1/group/invites/{code}")
    suspend fun getGroupInvite(@Path("code") code: String): SuccessEnvelope<GroupInviteDto>

    @POST("v1/group/invites/{code}/redeem")
    suspend fun redeemGroupInvite(
        @Path("code") code: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: Map<String, @JvmSuppressWildcards Any> = emptyMap(),
    ): SuccessEnvelope<RedeemGroupInviteResultDto>

    // --- Company invites ---
    @POST("v1/company/invites")
    suspend fun mintCompanyInvite(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: MintCompanyInviteBody,
    ): SuccessEnvelope<CompanyInviteDto>

    @GET("v1/company/invites/{code}")
    suspend fun getCompanyInvite(@Path("code") code: String): SuccessEnvelope<CompanyInviteDto>

    @POST("v1/company/invites/{code}/redeem")
    suspend fun redeemCompanyInvite(
        @Path("code") code: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: Map<String, @JvmSuppressWildcards Any> = emptyMap(),
    ): SuccessEnvelope<RedeemCompanyInviteResultDto>

    // --- S3 Group slice ---
    @GET("v1/group/moments/{momentId}/pulse")
    suspend fun getGroupPulse(
        @Path("momentId") momentId: String,
    ): SuccessEnvelope<GroupFacetDto<GroupPulsePayloadDto>>

    @GET("v1/group/moments/{momentId}/life")
    suspend fun getGroupLife(
        @Path("momentId") momentId: String,
    ): SuccessEnvelope<GroupFacetDto<GroupLifePayloadDto>>

    @GET("v1/group/moments/{momentId}/memory")
    suspend fun getGroupMemory(
        @Path("momentId") momentId: String,
    ): SuccessEnvelope<GroupFacetDto<GroupMemoryPayloadDto>>

    @GET("v1/group/moments/{momentId}/finance")
    suspend fun getGroupFinance(
        @Path("momentId") momentId: String,
    ): SuccessEnvelope<GroupFacetDto<GroupFinancePayloadDto>>

    @GET("v1/group/moments/{momentId}/activity")
    suspend fun getGroupActivity(
        @Path("momentId") momentId: String,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20,
    ): SuccessEnvelope<CursorPageDto<ActivityItemDto>>

    @GET("v1/group/moments/{momentId}/participants")
    suspend fun getGroupParticipants(
        @Path("momentId") momentId: String,
    ): SuccessEnvelope<GroupParticipantsDto>

    @POST("v1/group/moments/{momentId}/leave")
    suspend fun leaveGroupMoment(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: LeaveMomentBody,
    ): SuccessEnvelope<LeaveMomentResultDto>

    @POST("v1/companies/{companyId}/leave")
    suspend fun leaveCompany(
        @Path("companyId") companyId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: LeaveMomentBody,
    ): SuccessEnvelope<LeaveCompanyResultDto>

    @PATCH("v1/group/moments/{momentId}/participants/{participantId}")
    suspend fun updateGroupParticipantRole(
        @Path("momentId") momentId: String,
        @Path("participantId") participantId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: UpdateGroupParticipantRoleBody,
    ): SuccessEnvelope<UpdateGroupParticipantRoleResultDto>

    @POST("v1/group/moments/{momentId}/participants/{participantId}/remove")
    suspend fun removeGroupParticipant(
        @Path("momentId") momentId: String,
        @Path("participantId") participantId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?> = emptyMap(),
    ): SuccessEnvelope<RemoveGroupParticipantResultDto>

    @PATCH("v1/group/moments/{momentId}/budget")
    suspend fun patchGroupBudget(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: PatchGroupBudgetBody,
    ): SuccessEnvelope<PatchGroupBudgetResultDto>

    @POST("v1/moments/{momentId}/group-expenses")
    suspend fun createGroupExpense(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateGroupExpenseBody,
    ): SuccessEnvelope<CreateGroupExpenseResultDto>

    @GET("v1/group/moments/{momentId}/group-expenses")
    suspend fun listGroupExpenses(
        @Path("momentId") momentId: String,
        @Query("limit") limit: Int? = 20,
    ): SuccessEnvelope<GroupExpensesListDto>

    @GET("v1/moments/{momentId}/group-expenses/{expenseId}")
    suspend fun getGroupExpense(
        @Path("momentId") momentId: String,
        @Path("expenseId") expenseId: String,
    ): SuccessEnvelope<GroupExpenseDetailDto>

    @PATCH("v1/moments/{momentId}/group-expenses/{expenseId}")
    suspend fun updateGroupExpense(
        @Path("momentId") momentId: String,
        @Path("expenseId") expenseId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateGroupExpenseBody,
    ): SuccessEnvelope<CreateGroupExpenseResultDto>

    @DELETE("v1/moments/{momentId}/group-expenses/{expenseId}")
    suspend fun voidGroupExpense(
        @Path("momentId") momentId: String,
        @Path("expenseId") expenseId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
    ): SuccessEnvelope<CreateGroupExpenseResultDto>

    @POST("v1/moments/{momentId}/contributions")
    suspend fun recordContribution(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: RecordContributionBody,
    ): SuccessEnvelope<RecordContributionResultDto>

    @POST("v1/moments/{momentId}/settlements")
    suspend fun createSettlement(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateSettlementBody,
    ): SuccessEnvelope<CreateSettlementResultDto>

    // --- GX2-C Group collaboration ---
    @GET("v1/group/moments/{momentId}/planning-items")
    suspend fun listPlanningItems(@Path("momentId") momentId: String): SuccessEnvelope<GroupPlanningItemsDto>

    @POST("v1/moments/{momentId}/planning-items")
    suspend fun createPlanningItem(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreatePlanningItemBody,
    ): SuccessEnvelope<IdResultDto>

    @GET("v1/group/moments/{momentId}/bookings")
    suspend fun listBookings(@Path("momentId") momentId: String): SuccessEnvelope<GroupBookingsDto>

    @POST("v1/moments/{momentId}/bookings")
    suspend fun createBooking(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateBookingBody,
    ): SuccessEnvelope<IdResultDto>

    @GET("v1/group/moments/{momentId}/polls")
    suspend fun listPolls(@Path("momentId") momentId: String): SuccessEnvelope<GroupPollsDto>

    @POST("v1/moments/{momentId}/polls")
    suspend fun createPoll(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreatePollBody,
    ): SuccessEnvelope<IdResultDto>

    @GET("v1/polls/{pollId}")
    suspend fun getPoll(@Path("pollId") pollId: String): SuccessEnvelope<GroupPollDetailDto>

    @POST("v1/polls/{pollId}/votes")
    suspend fun votePoll(
        @Path("pollId") pollId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: VotePollBody,
    ): SuccessEnvelope<IdResultDto>

    @POST("v1/polls/{pollId}/close")
    suspend fun closePoll(
        @Path("pollId") pollId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: Map<String, Nothing> = emptyMap(),
    ): SuccessEnvelope<IdResultDto>

    @GET("v1/group/moments/{momentId}/living-rules")
    suspend fun listLivingRules(@Path("momentId") momentId: String): SuccessEnvelope<GroupLivingRulesDto>

    @POST("v1/moments/{momentId}/living-rules")
    suspend fun createLivingRule(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateLivingRuleBody,
    ): SuccessEnvelope<IdResultDto>

    @GET("v1/group/moments/{momentId}/updates")
    suspend fun listUpdates(@Path("momentId") momentId: String): SuccessEnvelope<GroupUpdatesDto>

    @POST("v1/moments/{momentId}/updates")
    suspend fun postUpdate(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: PostUpdateBody,
    ): SuccessEnvelope<IdResultDto>

    @GET("v1/group/moments/{momentId}/purchase-items")
    suspend fun listPurchaseItems(@Path("momentId") momentId: String): SuccessEnvelope<GroupPurchaseItemsDto>

    @POST("v1/moments/{momentId}/purchase-items")
    suspend fun createPurchaseItem(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreatePurchaseItemBody,
    ): SuccessEnvelope<IdResultDto>

    @GET("v1/group/moments/{momentId}/delivery-handovers")
    suspend fun listDeliveryHandovers(@Path("momentId") momentId: String): SuccessEnvelope<Map<String, Any?>>

    @POST("v1/moments/{momentId}/delivery-handovers")
    suspend fun createDeliveryHandover(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateDeliveryHandoverBody,
    ): SuccessEnvelope<IdResultDto>

    @GET("v1/group/moments/{momentId}/ownership-records")
    suspend fun listOwnershipRecords(@Path("momentId") momentId: String): SuccessEnvelope<GroupOwnershipDto>

    @POST("v1/moments/{momentId}/ownership-records")
    suspend fun createOwnershipRecord(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateOwnershipRecordBody,
    ): SuccessEnvelope<IdResultDto>

    @GET("v1/group/moments/{momentId}/residents")
    suspend fun listResidents(@Path("momentId") momentId: String): SuccessEnvelope<GroupResidentsDto>

    @POST("v1/moments/{momentId}/residents")
    suspend fun addResident(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: AddResidentBody,
    ): SuccessEnvelope<IdResultDto>

    @GET("v1/group/moments/{momentId}/shared-assets")
    suspend fun listSharedAssets(@Path("momentId") momentId: String): SuccessEnvelope<GroupSharedAssetsDto>

    @POST("v1/moments/{momentId}/shared-assets")
    suspend fun createSharedAsset(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateSharedAssetBody,
    ): SuccessEnvelope<IdResultDto>

    @GET("v1/group/moments/{momentId}/maintenance-records")
    suspend fun listMaintenanceRecords(@Path("momentId") momentId: String): SuccessEnvelope<GroupMaintenanceRecordsDto>

    @POST("v1/moments/{momentId}/maintenance-records")
    suspend fun createMaintenanceRecord(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateMaintenanceRecordBody,
    ): SuccessEnvelope<IdResultDto>

    @GET("v1/group/moments/{momentId}/memories")
    suspend fun listMemories(@Path("momentId") momentId: String): SuccessEnvelope<GroupMemoriesListDto>

    @POST("v1/moments/{momentId}/memories")
    suspend fun createMemory(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateMemoryBody,
    ): SuccessEnvelope<IdResultDto>

    @POST("v1/moments/{momentId}/vendors")
    suspend fun createGroupVendor(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateGroupVendorBody,
    ): SuccessEnvelope<IdResultDto>

    @GET("v1/group/moments/{momentId}/attendance")
    suspend fun listAttendance(@Path("momentId") momentId: String): SuccessEnvelope<GroupAttendanceDto>

    @POST("v1/moments/{momentId}/attendance")
    suspend fun recordAttendance(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: RecordAttendanceBody,
    ): SuccessEnvelope<IdResultDto>

    @POST("v1/moments/{momentId}/memories/{memoryId}/media")
    suspend fun attachMemoryMedia(
        @Path("momentId") momentId: String,
        @Path("memoryId") memoryId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: AttachMemoryMediaBody,
    ): SuccessEnvelope<MemoryAttachmentDto>

    @POST("v1/moments/{momentId}/participants")
    suspend fun addParticipant(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: AddParticipantBody,
    ): SuccessEnvelope<AddParticipantResultDto>

    @GET("v1/personal/moments/{momentId}/future-axis-snapshot")
    suspend fun getFutureAxisSnapshot(
        @Path("momentId") momentId: String,
    ): SuccessEnvelope<FutureAxisSnapshotDto>

    @GET("v1/personal/moments/{momentId}/lifestyle-vitality-snapshot")
    suspend fun getLifestyleVitalitySnapshot(
        @Path("momentId") momentId: String,
    ): SuccessEnvelope<LifestyleVitalitySnapshotDto>

    @GET("v1/personal/moments/{momentId}/relationships-bond-snapshot")
    suspend fun getRelationshipsBondSnapshot(
        @Path("momentId") momentId: String,
    ): SuccessEnvelope<RelationshipsBondSnapshotDto>

    // --- S4 Business slice ---
    @GET("v1/business/moments/{momentId}/pulse")
    suspend fun getBusinessPulse(
        @Path("momentId") momentId: String,
    ): SuccessEnvelope<BusinessFacetDto<BusinessPulsePayloadDto>>

    @GET("v1/business/moments/{momentId}/life")
    suspend fun getBusinessLife(
        @Path("momentId") momentId: String,
    ): SuccessEnvelope<BusinessFacetDto<BusinessLifePayloadDto>>

    @GET("v1/business/moments/{momentId}/memory")
    suspend fun getBusinessMemory(
        @Path("momentId") momentId: String,
    ): SuccessEnvelope<BusinessFacetDto<BusinessMemoryPayloadDto>>

    @GET("v1/business/moments/{momentId}/finance")
    suspend fun getBusinessFinance(
        @Path("momentId") momentId: String,
    ): SuccessEnvelope<BusinessFacetDto<BusinessFinancePayloadDto>>

    @GET("v1/business/moments/{momentId}/activity")
    suspend fun getBusinessActivity(
        @Path("momentId") momentId: String,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20,
    ): SuccessEnvelope<CursorPageDto<ActivityItemDto>>

    @POST("v1/moments/{momentId}/business-expenses")
    suspend fun createBusinessExpense(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateBusinessExpenseBody,
    ): SuccessEnvelope<CreateBusinessExpenseResultDto>

    @POST("v1/moments/{momentId}/revenues")
    suspend fun createBusinessRevenue(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateBusinessRevenueBody,
    ): SuccessEnvelope<CreateBusinessRevenueResultDto>

    @POST("v1/moments/{momentId}/invoices")
    suspend fun createBusinessInvoice(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateBusinessInvoiceBody,
    ): SuccessEnvelope<CreateBusinessInvoiceResultDto>

    @POST("v1/approvals/{approvalRequestId}/decide")
    suspend fun decideApproval(
        @Path("approvalRequestId") approvalRequestId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: DecideApprovalBody,
    ): SuccessEnvelope<DecideApprovalResultDto>

    @POST("v1/companies/{companyId}/vendors")
    suspend fun createBusinessVendor(
        @Path("companyId") companyId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateBusinessVendorBody,
    ): SuccessEnvelope<CreateBusinessVendorResultDto>

    @PATCH("v1/companies/{companyId}/vendors/{vendorId}")
    suspend fun updateBusinessVendor(
        @Path("companyId") companyId: String,
        @Path("vendorId") vendorId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: UpdateBusinessVendorBody,
    ): SuccessEnvelope<UpdateBusinessVendorResultDto>

    @POST("v1/companies/{companyId}/vendors/{vendorId}/contracts")
    suspend fun createVendorContract(
        @Path("companyId") companyId: String,
        @Path("vendorId") vendorId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateVendorContractBody,
    ): SuccessEnvelope<CreateVendorContractResultDto>

    @POST("v1/companies/{companyId}/vendors/{vendorId}/sla-definitions")
    suspend fun createSlaDefinition(
        @Path("companyId") companyId: String,
        @Path("vendorId") vendorId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateSlaDefinitionBody,
    ): SuccessEnvelope<CreateSlaDefinitionResultDto>

    @POST("v1/companies/{companyId}/sla-definitions/{slaDefinitionId}/checks")
    suspend fun createSlaCheck(
        @Path("companyId") companyId: String,
        @Path("slaDefinitionId") slaDefinitionId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateSlaCheckBody,
    ): SuccessEnvelope<CreateSlaCheckResultDto>

    @POST("v1/moments/{momentId}/issues")
    suspend fun createBusinessIssue(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateBusinessIssueBody,
    ): SuccessEnvelope<CreateBusinessIssueResultDto>

    @POST("v1/moments/{momentId}/improvements")
    suspend fun createBusinessImprovement(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateBusinessImprovementBody,
    ): SuccessEnvelope<CreateBusinessImprovementResultDto>

    @POST("v1/moments/{momentId}/business-updates")
    suspend fun createBusinessUpdate(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateBusinessUpdateBody,
    ): SuccessEnvelope<CreateBusinessUpdateResultDto>

    @POST("v1/moments/{momentId}/approval-requests")
    suspend fun createBusinessApprovalRequest(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateBusinessApprovalRequestBody,
    ): SuccessEnvelope<CreateBusinessApprovalRequestResultDto>

    @GET("v1/business/moments/{momentId}/moments")
    suspend fun getBusinessMomentTimeline(
        @Path("momentId") momentId: String,
        @Query("limit") limit: Int = 50,
    ): SuccessEnvelope<BusinessTimelineDto>

    @POST("v1/business/moments/{momentId}/memories")
    suspend fun createBusinessMemory(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateBusinessMemoryBody,
    ): SuccessEnvelope<CreateBusinessMemoryResultDto>

    @GET("v1/companies/{companyId}/members")
    suspend fun listCompanyMembers(
        @Path("companyId") companyId: String,
    ): SuccessEnvelope<CompanyMembersDto>

    @POST("v1/companies/{companyId}/members")
    suspend fun addCompanyMember(
        @Path("companyId") companyId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: AddCompanyMemberBody,
    ): SuccessEnvelope<AddCompanyMemberResultDto>

    // --- Business Deployment Closure POST routes ---

    @POST("v1/moments/{momentId}/milestones")
    suspend fun createMilestone(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateMilestoneBody,
    ): SuccessEnvelope<CreateMilestoneResultDto>

    @POST("v1/moments/{momentId}/risks")
    suspend fun createRisk(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateRiskBody,
    ): SuccessEnvelope<CreateRiskResultDto>

    @POST("v1/moments/{momentId}/tax-obligations")
    suspend fun createTaxObligation(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateTaxObligationBody,
    ): SuccessEnvelope<CreateTaxObligationResultDto>

    @POST("v1/moments/{momentId}/forecast-scenarios")
    suspend fun createForecastScenario(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateForecastScenarioBody,
    ): SuccessEnvelope<CreateForecastScenarioResultDto>

    @POST("v1/moments/{momentId}/investor-updates")
    suspend fun createInvestorUpdate(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateInvestorUpdateBody,
    ): SuccessEnvelope<CreateInvestorUpdateResultDto>

    @POST("v1/moments/{momentId}/budget-alerts")
    suspend fun createBudgetAlert(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateBudgetAlertBody,
    ): SuccessEnvelope<CreateBudgetAlertResultDto>

    @POST("v1/moments/{momentId}/business-reviews")
    suspend fun createBusinessReview(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateBusinessReviewBody,
    ): SuccessEnvelope<CreateBusinessReviewResultDto>

    @POST("v1/moments/{momentId}/decisions")
    suspend fun createDecision(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateDecisionBody,
    ): SuccessEnvelope<CreateDecisionResultDto>

    @POST("v1/moments/{momentId}/meeting-records")
    suspend fun createMeetingRecord(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateMeetingRecordBody,
    ): SuccessEnvelope<CreateMeetingRecordResultDto>

    @POST("v1/moments/{momentId}/recognitions")
    suspend fun createRecognition(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateRecognitionBody,
    ): SuccessEnvelope<CreateRecognitionResultDto>

    @POST("v1/moments/{momentId}/retrospectives")
    suspend fun createRetrospective(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateRetrospectiveBody,
    ): SuccessEnvelope<CreateRetrospectiveResultDto>

    @POST("v1/moments/{momentId}/activity-log-entries")
    suspend fun createActivityLogEntry(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateActivityLogEntryBody,
    ): SuccessEnvelope<CreateActivityLogEntryResultDto>

    @POST("v1/moments/{momentId}/issues/{issueId}/evidence")
    suspend fun createIssueEvidence(
        @Path("momentId") momentId: String,
        @Path("issueId") issueId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateIssueEvidenceBody,
    ): SuccessEnvelope<CreateIssueEvidenceResultDto>

    @POST("v1/business/moments/{momentId}/share-link")
    suspend fun createShareLink(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: Map<String, @JvmSuppressWildcards Any> = emptyMap(),
    ): SuccessEnvelope<ShareLinkResultDto>

    // --- Wave 3 GET projections ---

    @GET("v1/business/moments/{momentId}/actions")
    suspend fun getBusinessActions(
        @Path("momentId") momentId: String,
    ): SuccessEnvelope<BusinessActionsDto>

    @GET("v1/business/moments/{momentId}/capacity")
    suspend fun getBusinessCapacity(
        @Path("momentId") momentId: String,
    ): SuccessEnvelope<CapacityDto>

    @GET("v1/business/moments/{momentId}/workload")
    suspend fun getBusinessWorkload(
        @Path("momentId") momentId: String,
    ): SuccessEnvelope<WorkloadDto>

    @GET("v1/business/moments/{momentId}/mom-deltas")
    suspend fun getBusinessMomDeltas(
        @Path("momentId") momentId: String,
    ): SuccessEnvelope<MomDeltasDto>

    @GET("v1/business/moments/{momentId}/progress-snapshot")
    suspend fun getBusinessProgressSnapshot(
        @Path("momentId") momentId: String,
    ): SuccessEnvelope<ProgressSnapshotDto>

    @GET("v1/business/moments/{momentId}/roster")
    suspend fun getBusinessRoster(
        @Path("momentId") momentId: String,
    ): SuccessEnvelope<RosterDto>

    @GET("v1/business/moments/{momentId}/weekly-report")
    suspend fun getBusinessWeeklyReport(
        @Path("momentId") momentId: String,
    ): SuccessEnvelope<WeeklyReportDto>

    @GET("v1/companies/{companyId}/vendors")
    suspend fun listCompanyVendors(
        @Path("companyId") companyId: String,
    ): SuccessEnvelope<VendorListDto>

    // --- Route parity (SUPP-010) ---
    @GET("v1/companies/{companyId}")
    suspend fun getCompany(@Path("companyId") companyId: String): SuccessEnvelope<Map<String, @JvmSuppressWildcards Any?>>

    @PATCH("v1/companies/{companyId}")
    suspend fun patchCompany(
        @Path("companyId") companyId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>,
    ): SuccessEnvelope<Map<String, @JvmSuppressWildcards Any?>>

    @PATCH("v1/companies/{companyId}/locations/{locationId}")
    suspend fun patchLocation(
        @Path("companyId") companyId: String,
        @Path("locationId") locationId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>,
    ): SuccessEnvelope<Map<String, @JvmSuppressWildcards Any?>>

    @GET("v1/companies/{companyId}/teams")
    suspend fun listTeams(@Path("companyId") companyId: String): SuccessEnvelope<Map<String, @JvmSuppressWildcards Any?>>

    @POST("v1/companies/{companyId}/teams")
    suspend fun createTeam(
        @Path("companyId") companyId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>,
    ): SuccessEnvelope<Map<String, @JvmSuppressWildcards Any?>>

    @GET("v1/moments/{momentId}/activity")
    suspend fun getMomentActivity(
        @Path("momentId") momentId: String,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20,
    ): SuccessEnvelope<CursorPageDto<ActivityItemDto>>

    @GET("v1/moments/{momentId}/income/{incomeId}")
    suspend fun getIncome(
        @Path("momentId") momentId: String,
        @Path("incomeId") incomeId: String,
    ): SuccessEnvelope<Map<String, @JvmSuppressWildcards Any?>>

    @PATCH("v1/moments/{momentId}/income/{incomeId}")
    suspend fun patchIncome(
        @Path("momentId") momentId: String,
        @Path("incomeId") incomeId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>,
    ): SuccessEnvelope<Map<String, @JvmSuppressWildcards Any?>>

    @GET("v1/personal/moments/{momentId}/mood-history")
    suspend fun getPersonalMoodHistory(@Path("momentId") momentId: String): SuccessEnvelope<Map<String, @JvmSuppressWildcards Any?>>

    @GET("v1/personal/moments/{momentId}/adjustment-insight")
    suspend fun getPersonalAdjustmentInsight(@Path("momentId") momentId: String): SuccessEnvelope<Map<String, @JvmSuppressWildcards Any?>>

    @GET("v1/personal/moments/{momentId}/activity-summary")
    suspend fun getPersonalActivitySummary(@Path("momentId") momentId: String): SuccessEnvelope<Map<String, @JvmSuppressWildcards Any?>>

    @GET("v1/personal/moments/{momentId}/money-journey")
    suspend fun getPersonalMoneyJourney(@Path("momentId") momentId: String): SuccessEnvelope<Map<String, @JvmSuppressWildcards Any?>>

    @GET("v1/personal/moments/{momentId}/future-runtime-summary")
    suspend fun getPersonalFutureRuntimeSummary(@Path("momentId") momentId: String): SuccessEnvelope<Map<String, @JvmSuppressWildcards Any?>>

    @GET("v1/personal/moments/{momentId}/future-inventory")
    suspend fun getPersonalFutureInventory(@Path("momentId") momentId: String): SuccessEnvelope<Map<String, @JvmSuppressWildcards Any?>>

    @GET("v1/personal/moments/{momentId}/future-journey")
    suspend fun getPersonalFutureJourney(@Path("momentId") momentId: String): SuccessEnvelope<Map<String, @JvmSuppressWildcards Any?>>

    @PATCH("v1/personal/moments/{momentId}/future-profile")
    suspend fun patchPersonalFutureProfile(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>,
    ): SuccessEnvelope<Map<String, @JvmSuppressWildcards Any?>>

    @GET("v1/personal/moments/{momentId}/lifestyle-runtime-summary")
    suspend fun getPersonalLifestyleRuntimeSummary(@Path("momentId") momentId: String): SuccessEnvelope<Map<String, @JvmSuppressWildcards Any?>>

    @GET("v1/personal/moments/{momentId}/lifestyle-inventory")
    suspend fun getPersonalLifestyleInventory(@Path("momentId") momentId: String): SuccessEnvelope<Map<String, @JvmSuppressWildcards Any?>>

    @GET("v1/personal/moments/{momentId}/lifestyle-journey")
    suspend fun getPersonalLifestyleJourney(@Path("momentId") momentId: String): SuccessEnvelope<Map<String, @JvmSuppressWildcards Any?>>

    @PATCH("v1/personal/moments/{momentId}/lifestyle-profile")
    suspend fun patchPersonalLifestyleProfile(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>,
    ): SuccessEnvelope<Map<String, @JvmSuppressWildcards Any?>>

    @GET("v1/personal/moments/{momentId}/relationships-runtime-summary")
    suspend fun getPersonalRelationshipsRuntimeSummary(@Path("momentId") momentId: String): SuccessEnvelope<Map<String, @JvmSuppressWildcards Any?>>

    @GET("v1/personal/moments/{momentId}/relationships-connections")
    suspend fun getPersonalRelationshipsConnections(@Path("momentId") momentId: String): SuccessEnvelope<Map<String, @JvmSuppressWildcards Any?>>

    @GET("v1/personal/moments/{momentId}/relationships-journey")
    suspend fun getPersonalRelationshipsJourney(@Path("momentId") momentId: String): SuccessEnvelope<Map<String, @JvmSuppressWildcards Any?>>

    @PATCH("v1/personal/moments/{momentId}/relationships-profile")
    suspend fun patchPersonalRelationshipsProfile(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>,
    ): SuccessEnvelope<Map<String, @JvmSuppressWildcards Any?>>

    @PATCH("v1/personal/moments/{momentId}/life-ops-profile")
    suspend fun patchPersonalLifeOpsProfile(
        @Path("momentId") momentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>,
    ): SuccessEnvelope<Map<String, @JvmSuppressWildcards Any?>>

    @GET("v1/group/moments/{momentId}/vendors")
    suspend fun listGroupVendors(@Path("momentId") momentId: String): SuccessEnvelope<GroupVendorsDto>

    @GET("v1/moments/{momentId}/memories/{memoryId}/media")
    suspend fun listMemoryMedia(
        @Path("momentId") momentId: String,
        @Path("memoryId") memoryId: String,
    ): SuccessEnvelope<Map<String, @JvmSuppressWildcards Any?>>

    @GET("v1/business/moments/{momentId}/expenses")
    suspend fun listBusinessExpenses(@Path("momentId") momentId: String): SuccessEnvelope<Map<String, @JvmSuppressWildcards Any?>>

    @GET("v1/business/moments/{momentId}/revenues")
    suspend fun listBusinessRevenues(@Path("momentId") momentId: String): SuccessEnvelope<Map<String, @JvmSuppressWildcards Any?>>

    @GET("v1/business/moments/{momentId}/invoices")
    suspend fun listBusinessInvoices(@Path("momentId") momentId: String): SuccessEnvelope<Map<String, @JvmSuppressWildcards Any?>>

    @GET("v1/business/moments/{momentId}/issues")
    suspend fun listBusinessIssues(@Path("momentId") momentId: String): SuccessEnvelope<Map<String, @JvmSuppressWildcards Any?>>

    @GET("v1/business/moments/{momentId}/improvements")
    suspend fun listBusinessImprovements(@Path("momentId") momentId: String): SuccessEnvelope<Map<String, @JvmSuppressWildcards Any?>>

    @GET("v1/business/moments/{momentId}/updates")
    suspend fun listBusinessUpdates(@Path("momentId") momentId: String): SuccessEnvelope<Map<String, @JvmSuppressWildcards Any?>>

    @GET("v1/business/moments/{momentId}/approvals")
    suspend fun listBusinessApprovals(@Path("momentId") momentId: String): SuccessEnvelope<Map<String, @JvmSuppressWildcards Any?>>

    @GET("v1/business/moments/{momentId}/memories")
    suspend fun listBusinessMemories(@Path("momentId") momentId: String): SuccessEnvelope<Map<String, @JvmSuppressWildcards Any?>>
}
