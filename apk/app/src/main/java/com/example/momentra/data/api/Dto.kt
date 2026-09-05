package com.example.momentra.data.api

import com.google.gson.annotations.SerializedName

/** Success envelope — matches backend command/projection envelopes. */
data class SuccessEnvelope<T>(
    val data: T,
    @SerializedName("resourceVersion") val resourceVersion: Long? = null,
    @SerializedName("correlationId") val correlationId: String,
    @SerializedName("projectionHints") val projectionHints: List<ProjectionHintDto>? = null,
    @SerializedName("projectionVersion") val projectionVersion: Long? = null,
    @SerializedName("nextCursor") val nextCursor: String? = null,
)

data class ProjectionHintDto(
    val projection: String,
    val action: String? = "invalidate",
)

data class ErrorEnvelope(
    val code: String,
    val message: String,
    @SerializedName("correlationId") val correlationId: String,
)

data class PersonalPulseDto(
    @SerializedName("userId") val userId: String,
    @SerializedName("attentionCount") val attentionCount: Int,
    @SerializedName("activeMomentCount") val activeMomentCount: Int,
    @SerializedName("recoveryScore") val recoveryScore: String? = null,
    @SerializedName("moodState") val moodState: String? = null,
    @SerializedName("rhythmScore") val rhythmScore: String? = null,
    @SerializedName("wellbeingScore") val wellbeingScore: String? = null,
    @SerializedName("widgetPayload") val widgetPayload: Map<String, Any>? = null,
    @SerializedName("projectionVersion") val projectionVersion: Long,
    @SerializedName("updatedAt") val updatedAt: String,
)

/** Personal Life Health — Figma `1047:7689`. */
data class PersonalLifeDto(
    @SerializedName("userId") val userId: String,
    @SerializedName("activeAreaCount") val activeAreaCount: Int = 0,
    /** FIGMA_SEEDED | REAL — never treat seeded sections as production PASS (S2 G3). */
    @SerializedName("dataQuality") val dataQuality: String = "REAL",
    @SerializedName("sectionQuality") val sectionQuality: Map<String, String> = emptyMap(),
    val score: Int = 0,
    @SerializedName("scoreMax") val scoreMax: Int = 100,
    @SerializedName("statusLabel") val statusLabel: String = "",
    @SerializedName("trendLabel") val trendLabel: String = "",
    val insight: String = "",
    @SerializedName("areaScores") val areaScores: List<LifeAreaScoreDto> = emptyList(),
    val drift: LifeDriftDto? = null,
    val leverage: LifeLeverageDto? = null,
    val balance: List<LifeBalanceAxisDto> = emptyList(),
    @SerializedName("emotionalTrend") val emotionalTrend: LifeEmotionalTrendDto? = null,
    @SerializedName("dominantEmotion") val dominantEmotion: LifeDominantEmotionDto? = null,
    @SerializedName("happyDrivers") val happyDrivers: LifeHappyDriversDto? = null,
    val journey: LifeJourneyDto? = null,
    @SerializedName("aiInsights") val aiInsights: LifeAiInsightsDto? = null,
    @SerializedName("projectionVersion") val projectionVersion: Long = 0,
    @SerializedName("updatedAt") val updatedAt: String = "",
)

data class LifeAreaScoreDto(
    val code: String,
    val label: String,
    val score: Int,
    val color: String,
)

data class LifeDriftDto(
    val title: String,
    val headline: String,
    val body: String,
    @SerializedName("ctaLabel") val ctaLabel: String,
)

data class LifeLeverageDto(
    val title: String,
    @SerializedName("actionTitle") val actionTitle: String,
    @SerializedName("actionBody") val actionBody: String,
    @SerializedName("ctaLabel") val ctaLabel: String,
    val impacts: List<LifeImpactDto> = emptyList(),
)

data class LifeImpactDto(
    val label: String,
    val delta: String,
    val tone: String = "neutral",
)

data class LifeBalanceAxisDto(
    val code: String,
    val label: String,
    val score: Int,
    val badge: String,
    @SerializedName("badgeTone") val badgeTone: String = "green",
)

data class LifeEmotionalTrendDto(
    val subtitle: String,
    val series: List<LifeEmotionSeriesDto> = emptyList(),
)

data class LifeEmotionSeriesDto(
    val code: String,
    val label: String,
    val color: String,
    val points: List<Double> = emptyList(),
)

data class LifeDominantEmotionDto(
    val title: String,
    val headline: String,
    val segments: List<LifeEmotionSegmentDto> = emptyList(),
)

data class LifeEmotionSegmentDto(
    val label: String,
    val percent: Int,
    val color: String,
)

data class LifeHappyDriversDto(
    val title: String,
    val subtitle: String,
    val items: List<String> = emptyList(),
)

data class LifeJourneyDto(
    val title: String,
    val subtitle: String,
    val items: List<LifeJourneyItemDto> = emptyList(),
)

data class LifeJourneyItemDto(
    val icon: String,
    val title: String,
    @SerializedName("when") val whenLabel: String = "",
    val value: String,
    val tone: String = "neutral",
)

data class LifeAiInsightsDto(
    val title: String,
    val lead: String,
    val body: String,
)

data class PersonalMomentItemDto(
    @SerializedName("momentId") val momentId: String,
    val title: String,
    val status: String,
    @SerializedName("momentTypeCode") val momentTypeCode: String,
)

data class GroupMomentItemDto(
    @SerializedName("momentId") val momentId: String,
    val title: String,
    val status: String,
    @SerializedName("groupFamily") val groupFamily: String? = null,
)

data class BusinessMomentItemDto(
    @SerializedName("momentId") val momentId: String,
    val title: String,
    val status: String,
    @SerializedName("businessFamily") val businessFamily: String? = null,
    @SerializedName("companyId") val companyId: String? = null,
)

data class CursorPageDto<T>(
    val items: List<T>,
    @SerializedName("nextCursor") val nextCursor: String? = null,
)

data class MeBootstrapDto(
    @SerializedName("userId") val userId: String,
    @SerializedName("displayName") val displayName: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("firebaseUid") val firebaseUid: String? = null,
    val timezone: String? = null,
    val locale: String? = null,
    val status: String? = null,
    val roles: List<String>? = null,
    val permissions: List<String>? = null,
    val capabilities: List<String>? = null,
    @SerializedName("supportedContexts") val supportedContexts: List<String>? = null,
    @SerializedName("currentlySelectedContext") val currentlySelectedContext: String? = null,
    @SerializedName("activeMoments") val activeMoments: BootstrapMomentsDto? = null,
    val companies: List<CompanyItemDto>? = null,
    @SerializedName("selectedCompany") val selectedCompany: CompanyItemDto? = null,
    val preferences: BootstrapPreferencesDto? = null,
    @SerializedName("featureFlags") val featureFlags: Map<String, Any?>? = null,
)

data class BootstrapMomentsDto(
    val personal: List<BootstrapMomentDto> = emptyList(),
    val group: List<BootstrapMomentDto> = emptyList(),
    val business: List<BootstrapMomentDto> = emptyList(),
)

data class BootstrapMomentDto(
    @SerializedName("momentId") val momentId: String,
    val title: String,
    val status: String,
    @SerializedName("momentTypeCode") val momentTypeCode: String? = null,
    @SerializedName("domainCode") val domainCode: String? = null,
    @SerializedName("companyId") val companyId: String? = null,
)

data class BootstrapPreferencesDto(
    val timezone: String? = null,
    val locale: String? = null,
    @SerializedName("pushNotificationsEnabled") val pushNotificationsEnabled: Boolean? = null,
)

data class GlobalNotificationPrefsDto(
    @SerializedName("pushNotificationsEnabled") val pushNotificationsEnabled: Boolean,
    @SerializedName("categories") val categories: NotificationCategoriesDto? = null,
    @SerializedName("quietHoursStart") val quietHoursStart: String? = null,
    @SerializedName("quietHoursEnd") val quietHoursEnd: String? = null,
    @SerializedName("digestEnabled") val digestEnabled: Boolean? = null,
)

data class NotificationCategoriesDto(
    val finance: Boolean? = null,
    val tasks: Boolean? = null,
    val social: Boolean? = null,
    val invites: Boolean? = null,
    val approvals: Boolean? = null,
    val reminders: Boolean? = null,
)

data class PatchGlobalNotificationPrefsBody(
    @SerializedName("pushNotificationsEnabled") val pushNotificationsEnabled: Boolean? = null,
    val categories: NotificationCategoriesDto? = null,
    @SerializedName("quietHoursStart") val quietHoursStart: String? = null,
    @SerializedName("quietHoursEnd") val quietHoursEnd: String? = null,
    @SerializedName("digestEnabled") val digestEnabled: Boolean? = null,
)

data class MomentNotificationPrefsDto(
    @SerializedName("momentId") val momentId: String,
    @SerializedName("notifyOnChanges") val notifyOnChanges: Boolean,
    @SerializedName("reminderPreferences") val reminderPreferences: Map<String, Boolean>? = null,
)

data class PatchMomentNotificationPrefsBody(
    @SerializedName("notifyOnChanges") val notifyOnChanges: Boolean? = null,
    @SerializedName("reminderPreferences") val reminderPreferences: Map<String, Boolean>? = null,
)

data class NotificationInboxDto(
    val items: List<NotificationInboxItemDto>,
    @SerializedName("unreadCount") val unreadCount: Int,
)

data class NotificationInboxItemDto(
    @SerializedName("notificationId") val notificationId: String,
    @SerializedName("eventName") val eventName: String,
    @SerializedName("categoryCode") val categoryCode: String,
    @SerializedName("priorityCode") val priorityCode: String,
    val title: String,
    val body: String,
    @SerializedName("momentId") val momentId: String? = null,
    @SerializedName("deepLink") val deepLink: String? = null,
    @SerializedName("actorDisplayName") val actorDisplayName: String? = null,
    @SerializedName("readAt") val readAt: String? = null,
    @SerializedName("createdAt") val createdAt: String,
)

data class MarkNotificationsReadBody(
    @SerializedName("notificationIds") val notificationIds: List<String>? = null,
    val all: Boolean? = null,
)

data class MarkNotificationsReadResultDto(
    @SerializedName("updatedCount") val updatedCount: Int,
)

data class NotificationDeliveryMetricsDto(
    val today: NotificationDeliveryTodayDto,
    @SerializedName("byEvent") val byEvent: List<NotificationDeliveryByEventDto> = emptyList(),
    @SerializedName("inboxUnread") val inboxUnread: Int = 0,
)

data class NotificationDeliveryTodayDto(
    val attempted: Int = 0,
    val sent: Int = 0,
    val failed: Int = 0,
    @SerializedName("revokedTokens") val revokedTokens: Int = 0,
    @SerializedName("digestBatched") val digestBatched: Int = 0,
    val inbox: Int = 0,
)

data class NotificationDeliveryByEventDto(
    @SerializedName("eventName") val eventName: String,
    val attempted: Int = 0,
    val sent: Int = 0,
    val failed: Int = 0,
)

data class RegisterDeviceBody(
    @SerializedName("deviceId") val deviceId: String? = null,
    val platform: String = "ANDROID",
    @SerializedName("pushToken") val pushToken: String? = null,
    @SerializedName("appVersion") val appVersion: String? = null,
)

data class PatchMeBody(
    @SerializedName("displayName") val displayName: String? = null,
    val timezone: String? = null,
    val locale: String? = null,
)

data class PatchMeResultDto(
    @SerializedName("userId") val userId: String,
    @SerializedName("displayName") val displayName: String? = null,
    val timezone: String? = null,
    val locale: String? = null,
)

data class SoftDeleteMeResultDto(
    @SerializedName("userId") val userId: String,
    val status: String,
)

data class DeviceListDto(
    val items: List<DeviceItemDto> = emptyList(),
)

data class DeviceItemDto(
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("userDeviceId") val userDeviceId: String? = null,
    val platform: String? = null,
    @SerializedName("appVersion") val appVersion: String? = null,
    @SerializedName("lastSeenAt") val lastSeenAt: String? = null,
    val revoked: Boolean = false,
)

data class ConsentListDto(
    val purposes: List<ConsentPurposeDto> = emptyList(),
)

data class ConsentPurposeDto(
    val code: String,
    @SerializedName("displayName") val displayName: String? = null,
    val description: String? = null,
    val status: String? = null,
    val granted: Boolean = false,
    @SerializedName("consentId") val consentId: String? = null,
    @SerializedName("grantedAt") val grantedAt: String? = null,
)

data class ConsentPurposeBody(
    @SerializedName("purposeCode") val purposeCode: String,
    @SerializedName("scopeType") val scopeType: String = "USER",
)

data class ConsentMutationResultDto(
    @SerializedName("consentId") val consentId: String? = null,
    @SerializedName("purposeCode") val purposeCode: String? = null,
    val status: String? = null,
)

data class AnalyticsRefreshBody(
    val context: String,
    @SerializedName("companyId") val companyId: String? = null,
    @SerializedName("momentId") val momentId: String? = null,
)

data class AnalyticsRefreshResultDto(
    @SerializedName("metricsWritten") val metricsWritten: Int = 0,
    val narrative: Boolean = false,
    @SerializedName("skippedReason") val skippedReason: String? = null,
)

data class AnalyticsMetricItemDto(
    @SerializedName("metricCode") val metricCode: String,
    @SerializedName("numericValue") val numericValue: Double? = null,
    @SerializedName("computedAt") val computedAt: String? = null,
    @SerializedName("dataThrough") val dataThrough: String? = null,
    val status: String? = null,
    val version: String? = null,
)

data class AnalyticsMetricsDto(
    val items: List<AnalyticsMetricItemDto> = emptyList(),
)

data class AnalyticsInsightItemDto(
    @SerializedName("insightId") val insightId: String,
    val source: String? = null,
    @SerializedName("insightCode") val insightCode: String? = null,
    val title: String? = null,
    val body: String? = null,
    @SerializedName("computedAt") val computedAt: String? = null,
    @SerializedName("dataThrough") val dataThrough: String? = null,
    val status: String? = null,
    val version: String? = null,
)

data class AnalyticsInsightsMetaDto(
    @SerializedName("contractVersion") val contractVersion: String? = null,
    val status: String? = null,
    @SerializedName("computedAt") val computedAt: String? = null,
    @SerializedName("dataThrough") val dataThrough: String? = null,
    val version: String? = null,
)

data class AnalyticsInsightsDto(
    val items: List<AnalyticsInsightItemDto> = emptyList(),
    val meta: AnalyticsInsightsMetaDto? = null,
)

data class RegisterDeviceResultDto(
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("userId") val userId: String,
    val platform: String,
    val status: String,
)

data class RevokeDeviceResultDto(
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("userId") val userId: String,
    val status: String,
)

data class CompanyListDto(
    val items: List<CompanyItemDto> = emptyList(),
)

data class CompanyItemDto(
    @SerializedName("companyId") val companyId: String,
    @SerializedName("displayName") val displayName: String,
    val status: String? = null,
)

data class CreateCompanyBody(
    @SerializedName("displayName") val displayName: String,
    @SerializedName("legalName") val legalName: String,
    val timezone: String = "UTC",
    @SerializedName("companyType") val companyType: String? = null,
    @SerializedName("taxIdentifier") val taxIdentifier: String? = null,
    @SerializedName("profileJson") val profileJson: Map<String, Any>? = null,
)

data class CreateCompanyResultDto(
    @SerializedName("companyId") val companyId: String,
    @SerializedName("displayName") val displayName: String,
    val version: Long,
)

data class LocationListDto(
    val items: List<LocationItemDto> = emptyList(),
)

data class LocationItemDto(
    @SerializedName("locationId") val locationId: String,
    val name: String,
    @SerializedName("addressText") val addressText: String? = null,
    val status: String,
)

data class CreateLocationBody(
    val name: String,
    @SerializedName("addressText") val addressText: String? = null,
    val timezone: String? = null,
)

data class CreateLocationResultDto(
    @SerializedName("locationId") val locationId: String,
    @SerializedName("companyId") val companyId: String,
    val name: String,
)

data class ProjectionUpdatedEvent(
    val type: String = "PROJECTION_UPDATED",
    val projection: String? = null,
    val projections: List<String>? = null,
    @SerializedName("scopeType") val scopeType: String? = null,
    @SerializedName("scopeId") val scopeId: String? = null,
    @SerializedName("projectionVersion") val projectionVersion: Long = 0,
    @SerializedName("correlationId") val correlationId: String? = null,
)

data class TelemetryUserSnapshotDto(
    @SerializedName("userName") val userName: String? = null,
    @SerializedName("userEmail") val userEmail: String? = null,
    @SerializedName("userPhone") val userPhone: String? = null,
    @SerializedName("userAge") val userAge: String? = null,
    @SerializedName("userSex") val userSex: String? = null,
    @SerializedName("hasPhoto") val hasPhoto: Boolean? = null,
    @SerializedName("photoUrl") val photoUrl: String? = null,
    @SerializedName("authProviders") val authProviders: String? = null,
)

data class TelemetryEventDto(
    @SerializedName("eventName") val eventName: String,
    @SerializedName("screenName") val screenName: String? = null,
    @SerializedName("widgetName") val widgetName: String? = null,
    @SerializedName("clientOccurredAt") val clientOccurredAt: String,
    val properties: Map<String, String> = emptyMap(),
)

data class TelemetryIngestBody(
    @SerializedName("sessionId") val sessionId: String,
    @SerializedName("anonymousId") val anonymousId: String,
    val platform: String,
    @SerializedName("appVersion") val appVersion: String? = null,
    @SerializedName("deviceModel") val deviceModel: String? = null,
    @SerializedName("sessionStartedAt") val sessionStartedAt: String? = null,
    @SerializedName("sessionEndedAt") val sessionEndedAt: String? = null,
    @SerializedName("userSnapshot") val userSnapshot: TelemetryUserSnapshotDto? = null,
    val events: List<TelemetryEventDto>,
)

data class TelemetryIngestResultDto(
    val accepted: Int,
    @SerializedName("sessionId") val sessionId: String,
)

data class CreateMomentBody(
    @SerializedName("domainCode") val domainCode: String = "PERSONAL",
    @SerializedName("momentTypeCode") val momentTypeCode: String,
    val title: String,
    val description: String? = null,
    @SerializedName("startAt") val startAt: String? = null,
    @SerializedName("endAt") val endAt: String? = null,
    val timezone: String = "UTC",
    @SerializedName("customTypeLabel") val customTypeLabel: String? = null,
    val participants: List<CreateMomentParticipantBody>? = null,
    @SerializedName("inviteCode") val inviteCode: String? = null,
    @SerializedName("companyId") val companyId: String? = null,
    @SerializedName("personalSetup") val personalSetup: PersonalSetupBlockDto? = null,
    @SerializedName("businessSetup") val businessSetup: BusinessSetupBlockDto? = null,
    @SerializedName("groupSetup") val groupSetup: GroupSetupBlockDto? = null,
)

data class PersonalSetupBlockDto(
    @SerializedName("systemCode") val systemCode: String,
    val preferences: Map<String, Any>? = null,
)

data class BusinessSetupBlockDto(
    @SerializedName("familyCode") val familyCode: String,
    val preferences: Map<String, Any>? = null,
)

data class GroupSetupBlockDto(
    @SerializedName("budgetAmount") val budgetAmount: String,
    @SerializedName("budgetCurrencyCode") val budgetCurrencyCode: String,
    @SerializedName("destinationText") val destinationText: String? = null,
    @SerializedName("reminderPreferences") val reminderPreferences: Map<String, Boolean>? = null,
)

data class PatchGroupBudgetBody(
    @SerializedName("budgetAmount") val budgetAmount: String,
    @SerializedName("budgetCurrencyCode") val budgetCurrencyCode: String,
)

data class PatchGroupBudgetResultDto(
    @SerializedName("momentId") val momentId: String,
    @SerializedName("budgetAmount") val budgetAmount: String,
    @SerializedName("budgetCurrencyCode") val budgetCurrencyCode: String,
    @SerializedName("budgetId") val budgetId: String,
)

data class CreateMomentParticipantBody(
    @SerializedName("userId") val userId: String? = null,
    @SerializedName("displayName") val displayName: String? = null,
    @SerializedName("roleCode") val roleCode: String = "PARTICIPANT",
    val email: String? = null,
    val phone: String? = null,
)

data class GroupInviteDto(
    @SerializedName("inviteId") val inviteId: String,
    @SerializedName("inviteCode") val inviteCode: String,
    @SerializedName("invitePath") val invitePath: String = "",
    @SerializedName("inviteUrl") val inviteUrl: String = "",
    val status: String = "PENDING",
    val title: String = "",
    @SerializedName("momentTypeCode") val momentTypeCode: String = "",
    @SerializedName("momentId") val momentId: String? = null,
)

data class MintGroupInviteBody(
    val title: String,
    @SerializedName("momentTypeCode") val momentTypeCode: String,
    @SerializedName("momentId") val momentId: String? = null,
)

data class RedeemGroupInviteResultDto(
    @SerializedName("inviteId") val inviteId: String,
    @SerializedName("inviteCode") val inviteCode: String,
    val status: String,
    @SerializedName("momentId") val momentId: String? = null,
    @SerializedName("participantId") val participantId: String? = null,
    @SerializedName("alreadyMember") val alreadyMember: Boolean = false,
)

data class CompanyInviteDto(
    @SerializedName("inviteId") val inviteId: String,
    @SerializedName("inviteCode") val inviteCode: String,
    @SerializedName("invitePath") val invitePath: String = "",
    @SerializedName("inviteUrl") val inviteUrl: String = "",
    val status: String = "ACTIVE",
    val title: String = "",
    @SerializedName("companyId") val companyId: String = "",
    @SerializedName("membershipType") val membershipType: String = "MEMBER",
    @SerializedName("expiresAt") val expiresAt: String? = null,
)

data class MintCompanyInviteBody(
    @SerializedName("companyId") val companyId: String,
    @SerializedName("membershipType") val membershipType: String = "MEMBER",
)

data class RedeemCompanyInviteResultDto(
    @SerializedName("inviteId") val inviteId: String,
    @SerializedName("inviteCode") val inviteCode: String,
    val status: String,
    @SerializedName("companyId") val companyId: String,
    @SerializedName("membershipId") val membershipId: String? = null,
    @SerializedName("membershipType") val membershipType: String? = null,
    @SerializedName("alreadyMember") val alreadyMember: Boolean = false,
)

data class CreateMomentResultDto(
    @SerializedName("momentId") val momentId: String,
    @SerializedName("domainCode") val domainCode: String,
    val title: String,
    val status: String,
    val version: Long,
    @SerializedName("setupId") val setupId: String? = null,
)

data class MomentDetailDto(
    @SerializedName("momentId") val momentId: String,
    val title: String,
    val status: String,
    @SerializedName("domainCode") val domainCode: String,
    val version: Long = 1,
)

data class UpdateMomentBody(
    val title: String? = null,
    val description: String? = null,
    @SerializedName("expectedVersion") val expectedVersion: Long,
)

data class MomentVersionBody(
    @SerializedName("expectedVersion") val expectedVersion: Long,
)

data class MomentLifecycleResultDto(
    @SerializedName("momentId") val momentId: String,
    @SerializedName("domainCode") val domainCode: String,
    val title: String,
    val status: String,
    val version: Long,
)

data class CreateExpenseBody(
    val amount: String,
    @SerializedName("currencyCode") val currencyCode: String,
    val description: String? = null,
    @SerializedName("merchantName") val merchantName: String? = null,
    @SerializedName("categoryCode") val categoryCode: String? = null,
    @SerializedName("subcategoryCode") val subcategoryCode: String? = null,
    @SerializedName("financialAccountId") val financialAccountId: String? = null,
    @SerializedName("paymentMethodCode") val paymentMethodCode: String? = null,
    @SerializedName("effectiveAt") val effectiveAt: String? = null,
    @SerializedName("recurringScheduleId") val recurringScheduleId: String? = null,
)

data class CreateExpenseResultDto(
    @SerializedName("expenseId") val expenseId: String,
    @SerializedName("momentId") val momentId: String,
    val amount: String,
    @SerializedName("currencyCode") val currencyCode: String,
    val status: String,
    val version: Long,
)

data class CreateMovementBody(
    @SerializedName("movementType") val movementType: String,
    val amount: String,
    @SerializedName("currencyCode") val currencyCode: String,
    @SerializedName("accountId") val accountId: String? = null,
    @SerializedName("goalId") val goalId: String? = null,
    val description: String? = null,
    @SerializedName("effectiveAt") val effectiveAt: String? = null,
)

data class CreateMovementResultDto(
    @SerializedName("movementId") val movementId: String,
    @SerializedName("momentId") val momentId: String,
    val amount: String,
    @SerializedName("movementType") val movementType: String,
)

data class CreateObservationBody(
    @SerializedName("observationType") val observationType: String,
    @SerializedName("numericValue") val numericValue: Double? = null,
    @SerializedName("textValue") val textValue: String? = null,
    val note: String? = null,
    @SerializedName("observedAt") val observedAt: String? = null,
    @SerializedName("activityTypeCode") val activityTypeCode: String? = null,
    @SerializedName("durationMinutes") val durationMinutes: Int? = null,
    @SerializedName("energyBeforePct") val energyBeforePct: Double? = null,
    @SerializedName("energyAfterPct") val energyAfterPct: Double? = null,
    @SerializedName("feelingStateCode") val feelingStateCode: String? = null,
    @SerializedName("moodDrivers") val moodDrivers: List<String>? = null,
)

data class CreateAttentionCaptureBody(
    @SerializedName("categoryCode") val categoryCode: String,
    @SerializedName("intensityCode") val intensityCode: String,
    @SerializedName("timeBlockCode") val timeBlockCode: String,
    @SerializedName("energyRemaining") val energyRemaining: Int? = null,
    val note: String? = null,
)

data class CreateAttentionCaptureResultDto(
    @SerializedName("attentionCaptureId") val attentionCaptureId: String,
    @SerializedName("momentId") val momentId: String,
)

data class CreateLifeOpsAdjustBody(
    @SerializedName("rhythmActionCode") val rhythmActionCode: String? = null,
    @SerializedName("signalDirectionCode") val signalDirectionCode: String? = null,
    val reason: String? = null,
    @SerializedName("priorityWeights") val priorityWeights: List<LifeOpsPriorityWeightBody>? = null,
)

data class LifeOpsPriorityWeightBody(
    @SerializedName("priorityCode") val priorityCode: String,
    @SerializedName("weightPct") val weightPct: Double,
)

data class CreateLifeOpsAdjustResultDto(
    @SerializedName("adjustmentId") val adjustmentId: String,
    @SerializedName("momentId") val momentId: String,
)

data class PersonalRuntimeSummaryDto(
    @SerializedName("momentId") val momentId: String,
    @SerializedName("entriesTodayCount") val entriesTodayCount: Int,
    @SerializedName("lastEntryAt") val lastEntryAt: String?,
)

data class CreateObservationResultDto(
    @SerializedName("observationId") val observationId: String,
    @SerializedName("momentId") val momentId: String,
    @SerializedName("observationType") val observationType: String,
)

data class CreateFutureItemBody(
    val kind: String,
    val title: String,
    val description: String? = null,
    @SerializedName("targetDate") val targetDate: String? = null,
    @SerializedName("progressValue") val progressValue: Double? = null,
    @SerializedName("unitCode") val unitCode: String? = null,
    @SerializedName("opportunityType") val opportunityType: String? = null,
    @SerializedName("pivotReason") val pivotReason: String? = null,
    @SerializedName("providerName") val providerName: String? = null,
    @SerializedName("progressType") val progressType: String? = null,
)

data class CreateFutureItemResultDto(
    @SerializedName("itemId") val itemId: String,
    val kind: String,
    val title: String,
)

data class PersonalMemoryDto(
    @SerializedName("userId") val userId: String,
    val items: List<Map<String, Any?>> = emptyList(),
)

data class PersonalAttentionItemDto(
    @SerializedName("attentionCaptureId") val attentionCaptureId: String,
    @SerializedName("momentId") val momentId: String,
    @SerializedName("categoryCode") val categoryCode: String,
    @SerializedName("intensityCode") val intensityCode: String,
    @SerializedName("timeBlockCode") val timeBlockCode: String,
    @SerializedName("energyRemaining") val energyRemaining: Int? = null,
    @SerializedName("observedAt") val observedAt: String,
    val note: String? = null,
)

data class PersonalAttentionDto(
    @SerializedName("userId") val userId: String,
    val items: List<PersonalAttentionItemDto> = emptyList(),
)

data class PersonalSetupCatalogItemDto(
    @SerializedName("systemCode") val systemCode: String,
    val title: String,
    val subtitle: String,
    @SerializedName("figmaNodeId") val figmaNodeId: String? = null,
    @SerializedName("defaultMomentTypeCode") val defaultMomentTypeCode: String,
    @SerializedName("activateLabel") val activateLabel: String? = null,
    @SerializedName("defaultTitle") val defaultTitle: String? = null,
)

data class PersonalSetupItemDto(
    @SerializedName("setupId") val setupId: String,
    @SerializedName("systemCode") val systemCode: String,
    val title: String,
    @SerializedName("momentId") val momentId: String,
    val status: String,
    val preferences: Map<String, Any?> = emptyMap(),
    val version: Int? = null,
    @SerializedName("createdAt") val createdAt: String,
)

data class PersonalSetupsDto(
    val catalog: List<PersonalSetupCatalogItemDto> = emptyList(),
    val items: List<PersonalSetupItemDto> = emptyList(),
)

data class ActivatePersonalSetupBody(
    val title: String? = null,
    @SerializedName("momentTypeCode") val momentTypeCode: String? = null,
    val preferences: Map<String, @JvmSuppressWildcards Any>? = null,
    val timezone: String = "UTC",
)

data class ActivatePersonalSetupResultDto(
    @SerializedName("setupId") val setupId: String,
    @SerializedName("systemCode") val systemCode: String,
    @SerializedName("momentId") val momentId: String,
    @SerializedName("momentTypeCode") val momentTypeCode: String,
    val title: String,
    val status: String,
    val version: Int,
)

data class PatchPersonalSetupBody(
    @SerializedName("expectedVersion") val expectedVersion: Int,
    val title: String? = null,
    val preferences: Map<String, @JvmSuppressWildcards Any>? = null,
)

data class PatchPersonalSetupResultDto(
    @SerializedName("setupId") val setupId: String,
    @SerializedName("systemCode") val systemCode: String,
    @SerializedName("momentId") val momentId: String,
    val title: String,
    val status: String,
    val version: Int,
    val preferences: Map<String, Any?> = emptyMap(),
)

data class BusinessSetupCatalogItemDto(
    @SerializedName("familyCode") val familyCode: String,
    val title: String,
    val subtitle: String? = null,
    @SerializedName("defaultMomentTypeCode") val defaultMomentTypeCode: String? = null,
    @SerializedName("activateLabel") val activateLabel: String? = null,
    @SerializedName("defaultTitle") val defaultTitle: String? = null,
)

data class BusinessSetupItemDto(
    @SerializedName("setupId") val setupId: String,
    @SerializedName("familyCode") val familyCode: String,
    val title: String,
    @SerializedName("momentId") val momentId: String,
    @SerializedName("companyId") val companyId: String,
    val status: String,
    val preferences: Map<String, Any?> = emptyMap(),
    @SerializedName("createdAt") val createdAt: String,
)

data class BusinessSetupsDto(
    val catalog: List<BusinessSetupCatalogItemDto> = emptyList(),
    val mine: List<BusinessSetupItemDto> = emptyList(),
)

data class ExpenseSubcategoryDto(
    @SerializedName("subcategoryCode") val subcategoryCode: String,
    val label: String,
    @SerializedName("sortOrder") val sortOrder: Int,
)

data class ExpenseCategoryDto(
    @SerializedName("categoryCode") val categoryCode: String,
    val label: String,
    @SerializedName("sortOrder") val sortOrder: Int,
    val subcategories: List<ExpenseSubcategoryDto> = emptyList(),
)

data class ExpenseCategoriesDto(
    val categories: List<ExpenseCategoryDto> = emptyList(),
)

data class CreateGoalBody(
    val title: String,
    val description: String? = null,
    @SerializedName("targetAt") val targetAt: String? = null,
    @SerializedName("expectedVersion") val expectedVersion: Int? = null,
)

data class CreateGoalResultDto(
    @SerializedName("goalId") val goalId: String,
    @SerializedName("momentId") val momentId: String,
    val title: String,
    val version: Int,
)

data class CreateTaskBody(
    val title: String,
    val description: String? = null,
    @SerializedName("goalId") val goalId: String? = null,
    @SerializedName("milestoneId") val milestoneId: String? = null,
    @SerializedName("dueAt") val dueAt: String? = null,
    @SerializedName("expectedVersion") val expectedVersion: Int? = null,
)

data class CreateTaskResultDto(
    @SerializedName("taskId") val taskId: String,
    @SerializedName("momentId") val momentId: String,
    val title: String,
    val version: Int,
)

data class ExecuteActionProposalResultDto(
    val status: String,
    @SerializedName("executedResourceId") val executedResourceId: String? = null,
)

data class ActivateBusinessSetupBody(
    @SerializedName("companyId") val companyId: String,
    val title: String? = null,
    @SerializedName("momentTypeCode") val momentTypeCode: String? = null,
    val preferences: Map<String, @JvmSuppressWildcards Any>? = null,
    val timezone: String = "UTC",
)

data class ActivateBusinessSetupResultDto(
    @SerializedName("setupId") val setupId: String,
    @SerializedName("familyCode") val familyCode: String,
    @SerializedName("momentId") val momentId: String,
    @SerializedName("companyId") val companyId: String,
    @SerializedName("momentTypeCode") val momentTypeCode: String,
    val title: String,
    val status: String,
    val version: Int,
)

data class BusinessActionsDto(
    val items: List<Map<String, Any?>> = emptyList(),
)

data class CreateRelationshipActivityBody(
    @SerializedName("activityKind") val activityKind: String,
    @SerializedName("displayName") val displayName: String,
    val note: String? = null,
    @SerializedName("occurredAt") val occurredAt: String? = null,
    @SerializedName("relationshipType") val relationshipType: String? = null,
    @SerializedName("investmentValue") val investmentValue: Double? = null,
    @SerializedName("unitCode") val unitCode: String? = null,
)

data class CreateRelationshipActivityResultDto(
    @SerializedName("activityId") val activityId: String,
    @SerializedName("connectionId") val connectionId: String? = null,
    @SerializedName("activityKind") val activityKind: String? = null,
    @SerializedName("displayName") val displayName: String? = null,
)

data class CreateLifestyleActivityBody(
    @SerializedName("lifestyleContext") val lifestyleContext: String,
    val title: String,
    val description: String? = null,
    @SerializedName("occurredAt") val occurredAt: String? = null,
    @SerializedName("wellbeingRating") val wellbeingRating: Double? = null,
    @SerializedName("locationText") val locationText: String? = null,
    @SerializedName("startAt") val startAt: String? = null,
    @SerializedName("endAt") val endAt: String? = null,
)

data class CreateLifestyleActivityResultDto(
    @SerializedName("activityId") val activityId: String,
    @SerializedName("lifestyleContext") val lifestyleContext: String,
    val title: String,
)

data class VoidLifestyleActivityResultDto(
    @SerializedName("activityId") val activityId: String,
    @SerializedName("lifestyleContext") val lifestyleContext: String,
    val title: String,
    val status: String,
)

data class UpdateLifestyleActivityBody(
    val title: String? = null,
    val description: String? = null,
    @SerializedName("wellbeingRating") val wellbeingRating: Double? = null,
)

data class UpdateExpenseBody(
    val amount: String? = null,
    @SerializedName("currencyCode") val currencyCode: String? = null,
    val description: String? = null,
    @SerializedName("merchantName") val merchantName: String? = null,
    @SerializedName("categoryCode") val categoryCode: String? = null,
    @SerializedName("subcategoryCode") val subcategoryCode: String? = null,
    @SerializedName("financialAccountId") val financialAccountId: String? = null,
    @SerializedName("paymentMethodCode") val paymentMethodCode: String? = null,
    @SerializedName("effectiveAt") val effectiveAt: String? = null,
    @SerializedName("recurringScheduleId") val recurringScheduleId: String? = null,
)

data class ExpenseDetailDto(
    @SerializedName("expenseId") val expenseId: String,
    @SerializedName("momentId") val momentId: String,
    val amount: String,
    @SerializedName("currencyCode") val currencyCode: String,
    val status: String,
    val version: Long,
    val description: String? = null,
    @SerializedName("merchantName") val merchantName: String? = null,
    @SerializedName("categoryCode") val categoryCode: String? = null,
    @SerializedName("subcategoryCode") val subcategoryCode: String? = null,
    @SerializedName("financialAccountId") val financialAccountId: String? = null,
    @SerializedName("paymentMethodCode") val paymentMethodCode: String? = null,
    @SerializedName("effectiveAt") val effectiveAt: String? = null,
    @SerializedName("recurringScheduleId") val recurringScheduleId: String? = null,
    @SerializedName("attachmentIds") val attachmentIds: List<String> = emptyList(),
)

data class FinancialAccountDto(
    @SerializedName("financialAccountId") val financialAccountId: String,
    @SerializedName("accountType") val accountType: String,
    @SerializedName("accountName") val accountName: String,
    @SerializedName("currencyCode") val currencyCode: String,
    @SerializedName("institutionName") val institutionName: String? = null,
    val status: String,
)

data class CreateFinancialAccountBody(
    @SerializedName("accountType") val accountType: String,
    @SerializedName("accountName") val accountName: String,
    @SerializedName("currencyCode") val currencyCode: String,
    @SerializedName("institutionName") val institutionName: String? = null,
)

data class CreatePersonalIncomeBody(
    val amount: String,
    @SerializedName("currencyCode") val currencyCode: String,
    val description: String? = null,
    @SerializedName("merchantName") val merchantName: String? = null,
    @SerializedName("categoryCode") val categoryCode: String? = null,
    @SerializedName("financialAccountId") val financialAccountId: String? = null,
    @SerializedName("paymentMethodCode") val paymentMethodCode: String? = null,
    @SerializedName("effectiveAt") val effectiveAt: String? = null,
)

data class PersonalIncomeResultDto(
    @SerializedName("incomeId") val incomeId: String,
    @SerializedName("momentId") val momentId: String,
    val amount: String,
    @SerializedName("currencyCode") val currencyCode: String,
    val status: String,
    val version: Long,
)

data class ExpenseAttachmentDto(
    @SerializedName("uploadId") val uploadId: String,
    @SerializedName("contentType") val contentType: String? = null,
    val status: String,
    @SerializedName("createdAt") val createdAt: String? = null,
)

data class MediaUploadIntentBody(
    @SerializedName("contentType") val contentType: String,
    @SerializedName("byteSize") val byteSize: Int,
    @SerializedName("scopeType") val scopeType: String = "MOMENT",
    @SerializedName("scopeId") val scopeId: String,
)

data class MediaUploadIntentResultDto(
    @SerializedName("uploadId") val uploadId: String,
    @SerializedName("signedUrl") val signedUrl: String,
    @SerializedName("storageKey") val storageKey: String? = null,
    @SerializedName("expiresAt") val expiresAt: String,
)

data class MediaUploadCompleteBody(
    @SerializedName("storageKey") val storageKey: String,
)

data class MediaUploadCompleteResultDto(
    @SerializedName("uploadId") val uploadId: String,
    @SerializedName("mediaId") val mediaId: String,
    val status: String,
)

data class AttachExpenseMediaBody(
    @SerializedName("uploadId") val uploadId: String,
)

data class RecurringScheduleDto(
    @SerializedName("recurringScheduleId") val recurringScheduleId: String,
    @SerializedName("momentId") val momentId: String,
    @SerializedName("resourceKind") val resourceKind: String,
    @SerializedName("templatePayload") val templatePayload: Map<String, Any?> = emptyMap(),
    val frequency: String,
    @SerializedName("intervalCount") val intervalCount: Int = 1,
    @SerializedName("startDate") val startDate: String,
    @SerializedName("endDate") val endDate: String? = null,
    @SerializedName("nextRunAt") val nextRunAt: String? = null,
    val status: String,
    val version: Long = 1,
)

data class CreateRecurringScheduleBody(
    @SerializedName("resourceKind") val resourceKind: String,
    @SerializedName("templatePayload") val templatePayload: Map<String, Any?>,
    val frequency: String,
    @SerializedName("intervalCount") val intervalCount: Int = 1,
    @SerializedName("startDate") val startDate: String,
    @SerializedName("endDate") val endDate: String? = null,
)

data class UpdateRecurringScheduleBody(
    val status: String? = null,
    @SerializedName("endDate") val endDate: String? = null,
    @SerializedName("templatePayload") val templatePayload: Map<String, Any?>? = null,
)

data class GenerateRecurringInstanceResultDto(
    @SerializedName("expenseId") val expenseId: String? = null,
    @SerializedName("incomeId") val incomeId: String? = null,
    @SerializedName("occurrenceDate") val occurrenceDate: String,
)

data class ActivityItemDto(
    @SerializedName("activityCode") val activityCode: String,
    val title: String,
    @SerializedName("occurredAt") val occurredAt: String,
    @SerializedName("activityPayload") val activityPayload: ActivityPayloadDto? = null,
)

data class ActivityPayloadDto(
    @SerializedName("expenseId") val expenseId: String? = null,
    @SerializedName("activityId") val activityId: String? = null,
    val amount: String? = null,
    @SerializedName("currencyCode") val currencyCode: String? = null,
    @SerializedName("lifestyleContext") val lifestyleContext: String? = null,
    val description: String? = null,
    @SerializedName("merchantName") val merchantName: String? = null,
    @SerializedName("categoryCode") val categoryCode: String? = null,
    @SerializedName("subcategoryCode") val subcategoryCode: String? = null,
    @SerializedName("financialAccountId") val financialAccountId: String? = null,
    @SerializedName("paymentMethodCode") val paymentMethodCode: String? = null,
    @SerializedName("incomeId") val incomeId: String? = null,
    val status: String? = null,
    @SerializedName("wellbeingRating") val wellbeingRating: Double? = null,
)

/** Group facet envelope — GET /v1/group/moments/:id/{pulse|life|memory|finance}. */
data class GroupFacetDto<T>(
    @SerializedName("momentId") val momentId: String,
    val facet: String? = null,
    val title: String? = null,
    @SerializedName("groupFamily") val groupFamily: String? = null,
    val status: String? = null,
    val payload: T? = null,
)

data class GroupFinancePayloadDto(
    @SerializedName("dataQuality") val dataQuality: String = "EMPTY",
    val positions: List<GroupFinancePositionDto> = emptyList(),
    val totals: List<GroupFinanceTotalDto> = emptyList(),
    @SerializedName("expenseCount") val expenseCount: Int = 0,
    @SerializedName("viewerPosition") val viewerPosition: GroupFinancePositionDto? = null,
    @SerializedName("positionTotalCount") val positionTotalCount: Int = 0,
)

data class GroupFinancePositionDto(
    @SerializedName("participantId") val participantId: String,
    @SerializedName("currencyCode") val currencyCode: String,
    @SerializedName("paidTotal") val paidTotal: String = "0",
    @SerializedName("allocatedTotal") val allocatedTotal: String = "0",
    @SerializedName("contributionTotal") val contributionTotal: String = "0",
    @SerializedName("payableTotal") val payableTotal: String = "0",
    @SerializedName("receivableTotal") val receivableTotal: String = "0",
    @SerializedName("settledTotal") val settledTotal: String = "0",
    @SerializedName("netPosition") val netPosition: String = "0",
)

data class GroupFinanceTotalDto(
    @SerializedName("currencyCode") val currencyCode: String,
    @SerializedName("expenseTotal") val expenseTotal: String = "0",
    @SerializedName("budgetTotal") val budgetTotal: String = "0",
    @SerializedName("contributionTotal") val contributionTotal: String = "0",
    @SerializedName("settledTotal") val settledTotal: String = "0",
    @SerializedName("outstandingTotal") val outstandingTotal: String = "0",
)

data class GroupPulsePayloadDto(
    @SerializedName("dataQuality") val dataQuality: String = "EMPTY",
    @SerializedName("participantCount") val participantCount: Int = 0,
    @SerializedName("attentionCount") val attentionCount: Int = 0,
    @SerializedName("openTaskCount") val openTaskCount: Int = 0,
    @SerializedName("widgetPayload") val widgetPayload: Map<String, Any?>? = null,
    val finance: GroupFinancePayloadDto? = null,
)

data class GroupLifePlanningItemDto(
    @SerializedName("planningItemId") val planningItemId: String? = null,
    val title: String? = null,
    @SerializedName("dueAt") val dueAt: String? = null,
    val status: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("categoryCode") val categoryCode: String? = null,
    val location: String? = null,
    @SerializedName("priorityCode") val priorityCode: String? = null,
    val description: String? = null,
)

data class GroupLifeBookingDto(
    @SerializedName("bookingId") val bookingId: String? = null,
    val title: String? = null,
    @SerializedName("bookingType") val bookingType: String? = null,
    @SerializedName("bookedAt") val bookedAt: String? = null,
    @SerializedName("startAt") val startAt: String? = null,
    @SerializedName("endAt") val endAt: String? = null,
    val status: String? = null,
)

data class GroupLifeUpdateDto(
    @SerializedName("updateId") val updateId: String? = null,
    val message: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("participantId") val participantId: String? = null,
    @SerializedName("authorDisplayName") val authorDisplayName: String? = null,
    @SerializedName("urgencyCode") val urgencyCode: String? = null,
)

data class GroupLifeDomainMetricDto(
    val score: Int? = null,
    val label: String? = null,
    val status: String? = null,
)

data class GroupLifeDomainsDto(
    val experience: GroupLifeDomainMetricDto? = null,
    val purchase: GroupLifeDomainMetricDto? = null,
    val living: GroupLifeDomainMetricDto? = null,
    val goal: GroupLifeDomainMetricDto? = null,
    val community: GroupLifeDomainMetricDto? = null,
)

data class GroupLifeHealthDto(
    val score: Int? = null,
    val label: String? = null,
)

data class GroupLifeBalanceBarDto(
    val value: Int? = null,
    val label: String? = null,
)

data class GroupLifeBalanceDto(
    val participation: GroupLifeBalanceBarDto? = null,
    val contribution: GroupLifeBalanceBarDto? = null,
    val coordination: GroupLifeBalanceBarDto? = null,
    val progress: GroupLifeBalanceBarDto? = null,
    val community: GroupLifeBalanceBarDto? = null,
)

data class GroupLifeCountsDto(
    @SerializedName("participantCount") val participantCount: Int? = null,
    @SerializedName("openTaskCount") val openTaskCount: Int? = null,
    @SerializedName("planningCount") val planningCount: Int? = null,
    @SerializedName("bookingCount") val bookingCount: Int? = null,
    @SerializedName("updateCount") val updateCount: Int? = null,
    @SerializedName("pollCount") val pollCount: Int? = null,
    @SerializedName("purchaseItemCount") val purchaseItemCount: Int? = null,
    @SerializedName("residentCount") val residentCount: Int? = null,
    @SerializedName("expenseTotal") val expenseTotal: String? = null,
    @SerializedName("budgetTotal") val budgetTotal: String? = null,
    @SerializedName("contributionTotal") val contributionTotal: String? = null,
)

data class GroupLifeDriverDto(
    val domain: String? = null,
    val title: String? = null,
    val detail: String? = null,
)

data class GroupLifeActivityDto(
    val kind: String? = null,
    val id: String? = null,
    val title: String? = null,
    val at: String? = null,
)

data class GroupLifePayloadDto(
    @SerializedName("dataQuality") val dataQuality: String = "EMPTY",
    @SerializedName("metricVersion") val metricVersion: String? = null,
    val sections: Map<String, String> = emptyMap(),
    @SerializedName("openTaskCount") val openTaskCount: Int = 0,
    @SerializedName("participantCount") val participantCount: Int = 0,
    val counts: GroupLifeCountsDto? = null,
    val domains: GroupLifeDomainsDto? = null,
    val health: GroupLifeHealthDto? = null,
    val balance: GroupLifeBalanceDto? = null,
    val drivers: List<GroupLifeDriverDto> = emptyList(),
    val activity: List<GroupLifeActivityDto> = emptyList(),
    @SerializedName("planningItems") val planningItems: List<GroupLifePlanningItemDto> = emptyList(),
    val bookings: List<GroupLifeBookingDto> = emptyList(),
    val updates: List<GroupLifeUpdateDto> = emptyList(),
)

data class GroupMemoryMediaDto(
    @SerializedName("uploadId") val uploadId: String? = null,
    @SerializedName("contentType") val contentType: String? = null,
    val status: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("downloadUrl") val downloadUrl: String? = null,
)

data class GroupMemoryItemDto(
    @SerializedName("memoryId") val memoryId: String? = null,
    val title: String? = null,
    @SerializedName("occurredAt") val occurredAt: String? = null,
    val status: String? = null,
    val media: List<GroupMemoryMediaDto> = emptyList(),
    @SerializedName("mediaCount") val mediaCount: Int = 0,
)

data class GroupMemoryPayloadDto(
    @SerializedName("dataQuality") val dataQuality: String = "EMPTY",
    val items: List<GroupMemoryItemDto> = emptyList(),
    @SerializedName("memoryCount") val memoryCount: Int = 0,
)

data class GroupPlanningItemsDto(
    @SerializedName("momentId") val momentId: String,
    val items: List<GroupLifePlanningItemDto> = emptyList(),
    @SerializedName("openCount") val openCount: Int = 0,
)

data class GroupBookingsDto(
    @SerializedName("momentId") val momentId: String,
    val items: List<GroupLifeBookingDto> = emptyList(),
)

data class GroupUpdatesDto(
    @SerializedName("momentId") val momentId: String,
    val items: List<GroupLifeUpdateDto> = emptyList(),
)

data class GroupMemoriesListDto(
    @SerializedName("momentId") val momentId: String,
    val items: List<GroupMemoryItemDto> = emptyList(),
    @SerializedName("memoryCount") val memoryCount: Int = 0,
)

data class GroupPurchaseItemDto(
    @SerializedName("purchaseItemId") val purchaseItemId: String? = null,
    val label: String? = null,
    val amount: String? = null,
    val status: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
)

data class GroupPurchaseItemsDto(
    @SerializedName("momentId") val momentId: String? = null,
    val items: List<GroupPurchaseItemDto> = emptyList(),
)

data class GroupVendorItemDto(
    @SerializedName("groupVendorId") val groupVendorId: String? = null,
    @SerializedName("vendorName") val vendorName: String? = null,
    @SerializedName("vendorType") val vendorType: String? = null,
    val status: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
)

data class GroupVendorsDto(
    @SerializedName("momentId") val momentId: String? = null,
    val items: List<GroupVendorItemDto> = emptyList(),
)

data class GroupAttendanceItemDto(
    @SerializedName("attendanceId") val attendanceId: String? = null,
    @SerializedName("participantId") val participantId: String? = null,
    @SerializedName("displayName") val displayName: String? = null,
    @SerializedName("attendanceStatus") val attendanceStatus: String? = null,
    val note: String? = null,
    @SerializedName("checkedAt") val checkedAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null,
)

data class GroupAttendanceDto(
    @SerializedName("momentId") val momentId: String? = null,
    val items: List<GroupAttendanceItemDto> = emptyList(),
)

data class GroupOwnershipItemDto(
    @SerializedName("ownershipRecordId") val ownershipRecordId: String? = null,
    @SerializedName("purchaseItemId") val purchaseItemId: String? = null,
    @SerializedName("participantId") val participantId: String? = null,
    @SerializedName("displayName") val displayName: String? = null,
    @SerializedName("ownershipShare") val ownershipShare: String? = null,
    @SerializedName("ownershipNote") val ownershipNote: String? = null,
    val status: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
)

data class GroupOwnershipDto(
    @SerializedName("momentId") val momentId: String? = null,
    val items: List<GroupOwnershipItemDto> = emptyList(),
)

data class GroupLivingRuleItemDto(
    @SerializedName("livingRuleId") val livingRuleId: String? = null,
    val title: String? = null,
    @SerializedName("ruleText") val ruleText: String? = null,
    val status: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
)

data class GroupLivingRulesDto(
    @SerializedName("momentId") val momentId: String? = null,
    val items: List<GroupLivingRuleItemDto> = emptyList(),
)

data class GroupResidentItemDto(
    @SerializedName("residentId") val residentId: String? = null,
    @SerializedName("participantId") val participantId: String? = null,
    @SerializedName("roleCode") val roleCode: String? = null,
    val status: String? = null,
    val name: String? = null,
)

data class GroupResidentsDto(
    @SerializedName("momentId") val momentId: String? = null,
    val items: List<GroupResidentItemDto> = emptyList(),
)

data class GroupSharedAssetItemDto(
    @SerializedName("sharedAssetId") val sharedAssetId: String? = null,
    val title: String? = null,
    @SerializedName("assetType") val assetType: String? = null,
    @SerializedName("conditionCode") val conditionCode: String? = null,
    val status: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
)

data class GroupSharedAssetsDto(
    @SerializedName("momentId") val momentId: String? = null,
    val items: List<GroupSharedAssetItemDto> = emptyList(),
)

data class GroupMaintenanceRecordItemDto(
    @SerializedName("maintenanceRecordId") val maintenanceRecordId: String? = null,
    @SerializedName("sharedAssetId") val sharedAssetId: String? = null,
    val title: String? = null,
    val description: String? = null,
    val status: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
)

data class GroupMaintenanceRecordsDto(
    @SerializedName("momentId") val momentId: String? = null,
    val items: List<GroupMaintenanceRecordItemDto> = emptyList(),
)

data class CreatePlanningItemBody(
    val title: String,
    @SerializedName("dueAt") val dueAt: String? = null,
    @SerializedName("categoryCode") val categoryCode: String? = null,
    val location: String? = null,
    @SerializedName("priorityCode") val priorityCode: String? = null,
    val description: String? = null,
)
data class CreateBookingBody(val title: String, @SerializedName("bookedAt") val bookedAt: String? = null)
data class CreatePollBody(
    val question: String,
    val options: List<String>,
    @SerializedName("closesAt") val closesAt: String? = null,
    @SerializedName("pollType") val pollType: String? = null,
)

data class GroupPollOptionDto(
    @SerializedName("pollOptionId") val pollOptionId: String? = null,
    val text: String? = null,
    @SerializedName("sortOrder") val sortOrder: Int? = null,
    @SerializedName("voteCount") val voteCount: Int? = null,
    @SerializedName("votedByMe") val votedByMe: Boolean? = null,
)

data class GroupPollItemDto(
    @SerializedName("pollId") val pollId: String? = null,
    val question: String? = null,
    val status: String? = null,
    @SerializedName("closesAt") val closesAt: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("createdByUserId") val createdByUserId: String? = null,
    @SerializedName("createdByDisplayName") val createdByDisplayName: String? = null,
    @SerializedName("totalVotes") val totalVotes: Int? = null,
    val options: List<GroupPollOptionDto> = emptyList(),
)

data class GroupPollsDto(
    @SerializedName("momentId") val momentId: String? = null,
    val items: List<GroupPollItemDto> = emptyList(),
)

data class GroupPollDetailDto(
    @SerializedName("pollId") val pollId: String? = null,
    @SerializedName("momentId") val momentId: String? = null,
    val question: String? = null,
    val status: String? = null,
    @SerializedName("pollType") val pollType: String? = null,
    @SerializedName("closesAt") val closesAt: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("createdByUserId") val createdByUserId: String? = null,
    @SerializedName("canClose") val canClose: Boolean? = null,
    val options: List<GroupPollOptionDto> = emptyList(),
)

data class PostUpdateBody(
    val message: String,
    @SerializedName("notifyMembers") val notifyMembers: Boolean? = true,
    @SerializedName("urgencyCode") val urgencyCode: String? = "NORMAL",
)
data class CreateMemoryBody(val title: String, @SerializedName("capturedAt") val capturedAt: String? = null)
data class CreateGroupVendorBody(
    @SerializedName("vendorName") val vendorName: String,
    @SerializedName("vendorType") val vendorType: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val notes: String? = null,
    @SerializedName("quotedPrice") val quotedPrice: String? = null,
    @SerializedName("statusLabel") val statusLabel: String? = null,
)
data class RecordAttendanceBody(
    @SerializedName("participantId") val participantId: String,
    @SerializedName("attendanceStatus") val attendanceStatus: String,
    val note: String? = null,
)
data class AttachMemoryMediaBody(@SerializedName("uploadId") val uploadId: String)
data class MemoryAttachmentDto(
    @SerializedName("uploadId") val uploadId: String,
    @SerializedName("contentType") val contentType: String? = null,
    val status: String,
    @SerializedName("createdAt") val createdAt: String? = null,
)
data class AddParticipantBody(
    @SerializedName("displayName") val displayName: String? = null,
    @SerializedName("userId") val userId: String? = null,
    @SerializedName("roleCode") val roleCode: String? = null,
    val email: String? = null,
    val phone: String? = null,
)
data class AddParticipantResultDto(
    @SerializedName("participantId") val participantId: String,
    @SerializedName("momentId") val momentId: String,
)
data class CreatePurchaseItemBody(
    val label: String,
    val amount: String? = null,
    @SerializedName("customTypeLabel") val customTypeLabel: String? = null,
)

data class CreateDeliveryHandoverBody(
    @SerializedName("recipientName") val recipientName: String? = null,
    @SerializedName("handoverType") val handoverType: String? = null,
    @SerializedName("scheduledAt") val scheduledAt: String? = null,
    val address: String? = null,
    val note: String? = null,
    @SerializedName("purchaseItemId") val purchaseItemId: String? = null,
)

data class CreateOwnershipRecordBody(
    @SerializedName("purchaseItemId") val purchaseItemId: String? = null,
    @SerializedName("toParticipantName") val toParticipantName: String? = null,
    @SerializedName("fromOwnerName") val fromOwnerName: String? = null,
    @SerializedName("ownershipShare") val ownershipShare: Double? = null,
    @SerializedName("ownershipNote") val ownershipNote: String? = null,
    @SerializedName("effectiveAt") val effectiveAt: String? = null,
    @SerializedName("assetLabel") val assetLabel: String? = null,
)

data class VotePollBody(@SerializedName("pollOptionId") val pollOptionId: String)
data class CreateLivingRuleBody(
    val title: String,
    @SerializedName("ruleText") val ruleText: String,
)
data class AddResidentBody(val name: String, @SerializedName("roleCode") val roleCode: String? = null)

data class CreateSharedAssetBody(
    val title: String,
    @SerializedName("assetType") val assetType: String? = null,
    @SerializedName("conditionCode") val conditionCode: String? = null,
)

data class CreateMaintenanceRecordBody(
    val title: String,
    val description: String? = null,
    @SerializedName("sharedAssetId") val sharedAssetId: String? = null,
)

data class IdResultDto(
    @SerializedName("planningItemId") val planningItemId: String? = null,
    @SerializedName("bookingId") val bookingId: String? = null,
    @SerializedName("pollId") val pollId: String? = null,
    @SerializedName("updateId") val updateId: String? = null,
    @SerializedName("memoryId") val memoryId: String? = null,
    @SerializedName("purchaseItemId") val purchaseItemId: String? = null,
    @SerializedName("residentId") val residentId: String? = null,
    @SerializedName("sharedAssetId") val sharedAssetId: String? = null,
    @SerializedName("maintenanceRecordId") val maintenanceRecordId: String? = null,
    @SerializedName("momentId") val momentId: String? = null,
)

data class GroupParticipantsDto(
    @SerializedName("momentId") val momentId: String,
    val participants: List<GroupParticipantDto> = emptyList(),
)

data class GroupParticipantDto(
    @SerializedName("participantId") val participantId: String,
    @SerializedName("userId") val userId: String? = null,
    @SerializedName("roleCode") val roleCode: String = "PARTICIPANT",
    val status: String = "ACTIVE",
    @SerializedName("displayName") val displayName: String? = null,
)

data class LeaveMomentBody(
    @SerializedName("transferUserId") val transferUserId: String? = null,
)

data class LeaveMomentResultDto(
    @SerializedName("momentId") val momentId: String,
    val status: String = "LEFT",
    @SerializedName("transferredToUserId") val transferredToUserId: String? = null,
)

data class LeaveCompanyResultDto(
    @SerializedName("companyId") val companyId: String,
    val status: String = "LEFT",
    @SerializedName("transferredToUserId") val transferredToUserId: String? = null,
)

data class UpdateGroupParticipantRoleBody(
    @SerializedName("roleCode") val roleCode: String,
)

data class UpdateGroupParticipantRoleResultDto(
    @SerializedName("momentId") val momentId: String,
    @SerializedName("participantId") val participantId: String,
    @SerializedName("roleCode") val roleCode: String,
)

data class RemoveGroupParticipantResultDto(
    @SerializedName("momentId") val momentId: String,
    @SerializedName("participantId") val participantId: String,
    val status: String = "REMOVED",
)

data class GroupExpenseSplitInputDto(
    @SerializedName("participantId") val participantId: String,
    val amount: String? = null,
    val percent: String? = null,
    val shares: Double? = null,
)

data class CreateGroupExpenseBody(
    val amount: String,
    @SerializedName("currencyCode") val currencyCode: String,
    val description: String? = null,
    @SerializedName("paidByParticipantId") val paidByParticipantId: String,
    @SerializedName("splitStrategy") val splitStrategy: String,
    @SerializedName("splitInputs") val splitInputs: List<GroupExpenseSplitInputDto>,
)

data class CreateGroupExpenseResultDto(
    @SerializedName("expenseId") val expenseId: String,
    @SerializedName("momentId") val momentId: String,
    val amount: String,
    @SerializedName("currencyCode") val currencyCode: String,
    val status: String,
    val version: Long,
    @SerializedName("paidByParticipantId") val paidByParticipantId: String,
    @SerializedName("splitStrategy") val splitStrategy: String,
)

data class GroupExpenseShareDto(
    @SerializedName("expenseShareId") val expenseShareId: String? = null,
    @SerializedName("participantId") val participantId: String,
    @SerializedName("shareAmount") val shareAmount: String,
    @SerializedName("sharePercent") val sharePercent: String? = null,
)

data class GroupExpenseDetailDto(
    @SerializedName("expenseId") val expenseId: String,
    @SerializedName("momentId") val momentId: String,
    val amount: String,
    @SerializedName("currencyCode") val currencyCode: String,
    val status: String,
    val version: Long,
    val description: String? = null,
    @SerializedName("paidByParticipantId") val paidByParticipantId: String,
    @SerializedName("splitStrategy") val splitStrategy: String,
    val shares: List<GroupExpenseShareDto> = emptyList(),
)

data class GroupExpenseListItemDto(
    @SerializedName("expenseId") val expenseId: String? = null,
    val description: String? = null,
    @SerializedName("categoryCode") val categoryCode: String? = null,
    val amount: String? = null,
    @SerializedName("currencyCode") val currencyCode: String? = null,
    @SerializedName("paidByParticipantId") val paidByParticipantId: String? = null,
    @SerializedName("paidByDisplayName") val paidByDisplayName: String? = null,
    @SerializedName("effectiveAt") val effectiveAt: String? = null,
)

data class GroupExpensesListDto(
    @SerializedName("momentId") val momentId: String? = null,
    val items: List<GroupExpenseListItemDto> = emptyList(),
)

data class CreateSettlementBody(
    @SerializedName("payerParticipantId") val payerParticipantId: String,
    @SerializedName("payeeParticipantId") val payeeParticipantId: String,
    val amount: String,
    @SerializedName("currencyCode") val currencyCode: String,
    @SerializedName("obligationIds") val obligationIds: List<String>? = null,
)

data class CreateSettlementResultDto(
    @SerializedName("settlementId") val settlementId: String,
    @SerializedName("momentId") val momentId: String,
    val amount: String,
    @SerializedName("currencyCode") val currencyCode: String,
    val status: String,
)

data class FutureAxisSnapshotDto(
    @SerializedName("momentId") val momentId: String,
    @SerializedName("familyCode") val familyCode: String? = null,
    @SerializedName("visionScore") val visionScore: Double? = null,
    @SerializedName("growthScore") val growthScore: Double? = null,
    @SerializedName("momentumScore") val momentumScore: Double? = null,
    @SerializedName("disciplineScore") val disciplineScore: Double? = null,
    val source: String? = null,
)

data class LifestyleVitalitySnapshotDto(
    @SerializedName("momentId") val momentId: String,
    @SerializedName("joyScore") val joyScore: Double? = null,
    @SerializedName("fulfillmentScore") val fulfillmentScore: Double? = null,
    @SerializedName("vitalityScore") val vitalityScore: Double? = null,
    @SerializedName("explorationScore") val explorationScore: Double? = null,
    val source: String? = null,
)

data class RelationshipsBondSnapshotDto(
    @SerializedName("momentId") val momentId: String,
    @SerializedName("trustScore") val trustScore: Double? = null,
    @SerializedName("careScore") val careScore: Double? = null,
    @SerializedName("supportScore") val supportScore: Double? = null,
    @SerializedName("presenceScore") val presenceScore: Double? = null,
    @SerializedName("bondIndex") val bondIndex: Double? = null,
    val source: String? = null,
)

data class RecordContributionBody(
    val amount: String,
    @SerializedName("currencyCode") val currencyCode: String,
    val label: String? = null,
)

data class RecordContributionResultDto(
    @SerializedName("contributionId") val contributionId: String,
    @SerializedName("momentId") val momentId: String,
)

/** Business facet envelope — GET /v1/business/moments/:id/{pulse|life|memory|finance}. */
data class BusinessFacetDto<T>(
    @SerializedName("momentId") val momentId: String,
    val facet: String? = null,
    val title: String? = null,
    @SerializedName("companyId") val companyId: String? = null,
    @SerializedName("businessFamily") val businessFamily: String? = null,
    val status: String? = null,
    val payload: T? = null,
)

data class BusinessFinanceTotalDto(
    @SerializedName("currencyCode") val currencyCode: String,
    @SerializedName("expenseTotal") val expenseTotal: String = "0",
    @SerializedName("revenueTotal") val revenueTotal: String = "0",
    @SerializedName("invoiceOutstandingTotal") val invoiceOutstandingTotal: String = "0",
)

data class BusinessFinancePayloadDto(
    @SerializedName("dataQuality") val dataQuality: String = "EMPTY",
    val totals: List<BusinessFinanceTotalDto> = emptyList(),
    @SerializedName("snapshotPayload") val snapshotPayload: Map<String, Any?>? = null,
)

data class OpsSpendCategoryDto(
    val label: String = "",
    val pct: Int = 0,
)

data class OpsAttentionDto(
    val title: String = "",
    val severity: String? = null,
    @SerializedName("issueId") val issueId: String? = null,
)

data class OpsPulseExtrasDto(
    @SerializedName("monthlySpend") val monthlySpend: String? = null,
    @SerializedName("activeVendorCount") val activeVendorCount: Int = 0,
    @SerializedName("slaCompliancePct") val slaCompliancePct: Int? = null,
    @SerializedName("openIssueCount") val openIssueCount: Int = 0,
    @SerializedName("spendByCategory") val spendByCategory: List<OpsSpendCategoryDto> = emptyList(),
    @SerializedName("needsAttention") val needsAttention: List<OpsAttentionDto> = emptyList(),
    @SerializedName("sectionQuality") val sectionQuality: Map<String, String> = emptyMap(),
)

data class BusinessPulsePayloadDto(
    @SerializedName("dataQuality") val dataQuality: String = "EMPTY",
    @SerializedName("attentionCount") val attentionCount: Int = 0,
    @SerializedName("activeMomentCount") val activeMomentCount: Int = 0,
    @SerializedName("runwayMonths") val runwayMonths: String? = null,
    @SerializedName("financialHealthScore") val financialHealthScore: String? = null,
    @SerializedName("widgetPayload") val widgetPayload: Map<String, Any?>? = null,
    val finance: BusinessFinancePayloadDto? = null,
    val operations: OpsPulseExtrasDto? = null,
    val activity: List<ActivityItemDto>? = null,
)

data class BusinessLifeKpisDto(
    @SerializedName("activeModuleCount") val activeModuleCount: Int = 0,
    @SerializedName("activeMomentCount") val activeMomentCount: Int = 0,
    @SerializedName("runwayMonths") val runwayMonths: String? = null,
    @SerializedName("financialHealthScore") val financialHealthScore: String? = null,
    @SerializedName("attentionCount") val attentionCount: Int = 0,
)

data class BusinessLifeModuleCardDto(
    val active: Boolean = false,
    @SerializedName("statusLabel") val statusLabel: String? = null,
    @SerializedName("runwayMonths") val runwayMonths: String? = null,
    val score: String? = null,
    @SerializedName("revenueMomPct") val revenueMomPct: Int? = null,
    @SerializedName("expenseMomPct") val expenseMomPct: Int? = null,
)

data class BusinessLifeModulesDto(
    @SerializedName("teamOperations") val teamOperations: BusinessLifeModuleCardDto? = null,
    val runway: BusinessLifeModuleCardDto? = null,
    @SerializedName("businessOperations") val businessOperations: BusinessLifeModuleCardDto? = null,
    @SerializedName("vendorOperations") val vendorOperations: BusinessLifeModuleCardDto? = null,
)

data class BusinessLifeSignalDto(
    @SerializedName("signalId") val signalId: String = "",
    @SerializedName("signalType") val signalType: String? = null,
    val title: String = "",
    val family: String = "OPERATIONS",
    @SerializedName("statusLabel") val statusLabel: String = "Watch",
    val severity: String? = null,
    @SerializedName("metricValue") val metricValue: Any? = null,
)

data class BusinessLifeActivityDto(
    @SerializedName("activityCode") val activityCode: String = "",
    val title: String = "",
    @SerializedName("occurredAt") val occurredAt: String = "",
    val family: String = "OPERATIONS",
    val description: String? = null,
)

data class BusinessLifeJourneyDto(
    @SerializedName("familyCode") val familyCode: String = "",
    val family: String = "OPERATIONS",
    val title: String = "",
    @SerializedName("createdAt") val createdAt: String = "",
)

data class BusinessLifeTrendPointDto(
    val month: String = "",
    @SerializedName("financialHealthScore") val financialHealthScore: Int? = null,
    @SerializedName("teamScore") val teamScore: Int? = null,
    @SerializedName("runwayScore") val runwayScore: Int? = null,
    @SerializedName("opsScore") val opsScore: Int? = null,
)

data class BusinessLifeTrendsDto(
    val status: String = "EMPTY_SUPPORTED",
    val series: List<BusinessLifeTrendPointDto> = emptyList(),
)

data class BusinessLifePayloadDto(
    @SerializedName("dataQuality") val dataQuality: String = "EMPTY",
    val sections: Map<String, String> = emptyMap(),
    @SerializedName("teamOperationsPayload") val teamOperationsPayload: Map<String, Any?>? = null,
    @SerializedName("runwayPayload") val runwayPayload: Map<String, Any?>? = null,
    @SerializedName("businessOperationsPayload") val businessOperationsPayload: Map<String, Any?>? = null,
    @SerializedName("vendorOperationsPayload") val vendorOperationsPayload: Map<String, Any?>? = null,
    val kpis: BusinessLifeKpisDto? = null,
    val modules: BusinessLifeModulesDto? = null,
    val signals: List<BusinessLifeSignalDto> = emptyList(),
    val activity: List<BusinessLifeActivityDto> = emptyList(),
    val journey: List<BusinessLifeJourneyDto> = emptyList(),
    val trends: BusinessLifeTrendsDto? = null,
)

data class BusinessMemoryPayloadDto(
    @SerializedName("dataQuality") val dataQuality: String = "EMPTY",
    val items: List<Map<String, Any?>> = emptyList(),
    @SerializedName("memoryCount") val memoryCount: Int = 0,
)

data class BusinessTimelineItemDto(
    @SerializedName("eventId") val eventId: String,
    @SerializedName("eventType") val eventType: String,
    val title: String,
    val category: String,
    val description: String? = null,
    @SerializedName("occurredAt") val occurredAt: String,
)

data class BusinessTimelineKpisDto(
    @SerializedName("spendEvents") val spendEvents: Int = 0,
    @SerializedName("issueCount") val issueCount: Int = 0,
    @SerializedName("highPriorityIssues") val highPriorityIssues: Int = 0,
    @SerializedName("updateCount") val updateCount: Int = 0,
    @SerializedName("activeContracts") val activeContracts: Int = 0,
    @SerializedName("vendorCount") val vendorCount: Int = 0,
)

data class BusinessTimelineDto(
    @SerializedName("momentId") val momentId: String,
    @SerializedName("companyId") val companyId: String,
    val items: List<BusinessTimelineItemDto> = emptyList(),
    val kpis: BusinessTimelineKpisDto? = null,
)

data class CreateBusinessMemoryBody(
    val title: String,
    val body: String? = null,
    @SerializedName("memoryType") val memoryType: String? = null,
    @SerializedName("occurredAt") val occurredAt: String? = null,
)

data class CreateBusinessMemoryResultDto(
    @SerializedName("memoryId") val memoryId: String,
    @SerializedName("momentId") val momentId: String,
    val title: String,
)

data class CreateBusinessExpenseBody(
    val amount: String,
    @SerializedName("currencyCode") val currencyCode: String,
    val description: String? = null,
    @SerializedName("merchantName") val merchantName: String? = null,
    @SerializedName("categoryCode") val categoryCode: String? = null,
    @SerializedName("vendorId") val vendorId: String? = null,
)

data class CreateBusinessExpenseResultDto(
    @SerializedName("expenseId") val expenseId: String,
    @SerializedName("momentId") val momentId: String,
    @SerializedName("companyId") val companyId: String? = null,
    val amount: String,
    @SerializedName("currencyCode") val currencyCode: String,
    @SerializedName("categoryCode") val categoryCode: String? = null,
    val status: String,
    @SerializedName("approvalRequestId") val approvalRequestId: String? = null,
    val version: Long = 0,
)

data class CreateBusinessRevenueBody(
    val amount: String,
    @SerializedName("currencyCode") val currencyCode: String,
    val description: String? = null,
    @SerializedName("categoryCode") val categoryCode: String? = null,
)

data class CreateBusinessRevenueResultDto(
    @SerializedName("revenueId") val revenueId: String,
    @SerializedName("companyId") val companyId: String? = null,
    val amount: String,
    @SerializedName("currencyCode") val currencyCode: String,
    val status: String,
)

data class BusinessInvoiceLineDto(
    val description: String,
    val quantity: String,
    @SerializedName("unitPrice") val unitPrice: String,
    @SerializedName("taxAmount") val taxAmount: String? = null,
)

data class CreateBusinessInvoiceBody(
    @SerializedName("invoiceNumber") val invoiceNumber: String,
    @SerializedName("invoiceDate") val invoiceDate: String,
    @SerializedName("dueDate") val dueDate: String? = null,
    @SerializedName("currencyCode") val currencyCode: String,
    val lines: List<BusinessInvoiceLineDto>,
)

data class CreateBusinessInvoiceResultDto(
    @SerializedName("invoiceId") val invoiceId: String,
    @SerializedName("companyId") val companyId: String? = null,
    val status: String? = null,
)

data class DecideApprovalBody(
    val decision: String,
    val reason: String? = null,
)

data class DecideApprovalResultDto(
    @SerializedName("approvalRequestId") val approvalRequestId: String,
    val status: String? = null,
)

data class CreateBusinessVendorBody(
    val name: String,
    @SerializedName("vendorType") val vendorType: String? = null,
)

data class CreateBusinessVendorResultDto(
    @SerializedName("vendorId") val vendorId: String,
    @SerializedName("companyId") val companyId: String? = null,
    val name: String? = null,
)

data class UpdateBusinessVendorBody(
    val name: String? = null,
    @SerializedName("vendorType") val vendorType: String? = null,
    val status: String? = null,
    val note: String? = null,
)

data class UpdateBusinessVendorResultDto(
    @SerializedName("vendorId") val vendorId: String,
    @SerializedName("companyId") val companyId: String? = null,
)

data class CreateVendorContractBody(
    @SerializedName("contractName") val contractName: String,
    @SerializedName("contractReference") val contractReference: String? = null,
    @SerializedName("startDate") val startDate: String? = null,
    @SerializedName("endDate") val endDate: String? = null,
    @SerializedName("contractValue") val contractValue: String? = null,
    @SerializedName("currencyCode") val currencyCode: String? = null,
)

data class CreateVendorContractResultDto(
    @SerializedName("vendorContractId") val vendorContractId: String,
)

data class CreateSlaDefinitionBody(
    val name: String,
    @SerializedName("metricCode") val metricCode: String,
    @SerializedName("targetValue") val targetValue: Double? = null,
    val comparator: String,
    @SerializedName("unitCode") val unitCode: String? = null,
    @SerializedName("measurementPeriod") val measurementPeriod: String? = null,
    @SerializedName("vendorContractId") val vendorContractId: String? = null,
)

data class CreateSlaDefinitionResultDto(
    @SerializedName("slaDefinitionId") val slaDefinitionId: String,
)

data class CreateSlaCheckBody(
    @SerializedName("observedAt") val observedAt: String? = null,
    @SerializedName("observedValue") val observedValue: Double? = null,
    val result: String,
    val evidence: Map<String, Any?>? = null,
    val note: String? = null,
)

data class CreateSlaCheckResultDto(
    @SerializedName("slaCheckId") val slaCheckId: String,
    @SerializedName("vendorId") val vendorId: String? = null,
)

data class CreateBusinessIssueBody(
    val title: String,
    val description: String? = null,
    val severity: String? = null,
    @SerializedName("vendorId") val vendorId: String? = null,
)

data class CreateBusinessIssueResultDto(
    @SerializedName("issueId") val issueId: String,
    @SerializedName("momentId") val momentId: String? = null,
)

data class CreateBusinessImprovementBody(
    val title: String,
    val description: String? = null,
    @SerializedName("categoryCode") val categoryCode: String? = null,
    @SerializedName("impactEstimate") val impactEstimate: String? = null,
)

data class CreateBusinessImprovementResultDto(
    @SerializedName("improvementId") val improvementId: String,
    @SerializedName("momentId") val momentId: String? = null,
)

data class CreateBusinessUpdateBody(
    val title: String? = null,
    val body: String,
)

data class CreateBusinessUpdateResultDto(
    @SerializedName("updateId") val updateId: String,
)

data class CreateBusinessApprovalRequestBody(
    val title: String,
    val amount: String? = null,
    @SerializedName("currencyCode") val currencyCode: String? = null,
    val note: String? = null,
)

data class CreateBusinessApprovalRequestResultDto(
    @SerializedName("approvalRequestId") val approvalRequestId: String,
    @SerializedName("momentId") val momentId: String? = null,
)

data class CompanyMembersDto(
    @SerializedName("companyId") val companyId: String,
    val members: List<CompanyMemberDto> = emptyList(),
)

data class CompanyMemberDto(
    @SerializedName("membershipId") val membershipId: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("membershipType") val membershipType: String,
    val status: String = "ACTIVE",
    @SerializedName("displayName") val displayName: String? = null,
)

data class AddCompanyMemberBody(
    @SerializedName("userId") val userId: String,
    @SerializedName("membershipType") val membershipType: String = "MEMBER",
)

data class AddCompanyMemberResultDto(
    @SerializedName("membershipId") val membershipId: String,
    @SerializedName("companyId") val companyId: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("membershipType") val membershipType: String,
    val status: String,
)

// --- Business Deployment Closure POST bodies + results ---

data class CreateMilestoneBody(
    val title: String,
    @SerializedName("targetAt") val targetAt: String? = null,
    val status: String? = null,
    @SerializedName("goalId") val goalId: String? = null,
)

data class CreateMilestoneResultDto(
    @SerializedName("milestoneId") val milestoneId: String,
    @SerializedName("momentId") val momentId: String? = null,
)

data class CreateRiskBody(
    val title: String,
    val description: String? = null,
    val likelihood: String? = null,
    val impact: String? = null,
    @SerializedName("mitigationText") val mitigationText: String? = null,
)

data class CreateRiskResultDto(
    @SerializedName("riskId") val riskId: String,
    @SerializedName("momentId") val momentId: String? = null,
)

data class CreateTaxObligationBody(
    val title: String,
    @SerializedName("taxType") val taxType: String? = null,
    val amount: String? = null,
    @SerializedName("currencyCode") val currencyCode: String? = null,
    @SerializedName("dueDate") val dueDate: String? = null,
    val notes: String? = null,
)

data class CreateTaxObligationResultDto(
    @SerializedName("taxObligationId") val taxObligationId: String,
    @SerializedName("momentId") val momentId: String? = null,
)

data class ForecastScenarioLineBody(
    @SerializedName("lineLabel") val lineLabel: String,
    val amount: String,
    @SerializedName("currencyCode") val currencyCode: String? = null,
    @SerializedName("periodLabel") val periodLabel: String? = null,
)

data class CreateForecastScenarioBody(
    val name: String,
    @SerializedName("horizonMonths") val horizonMonths: Int? = null,
    val assumptions: String? = null,
    val lines: List<ForecastScenarioLineBody>? = null,
)

data class CreateForecastScenarioResultDto(
    @SerializedName("forecastScenarioId") val forecastScenarioId: String,
    @SerializedName("momentId") val momentId: String? = null,
)

data class CreateInvestorUpdateBody(
    @SerializedName("updateType") val updateType: String? = null,
    val subject: String,
    @SerializedName("keyMetrics") val keyMetrics: String? = null,
    @SerializedName("runwayStatus") val runwayStatus: String? = null,
    val highlights: String? = null,
    @SerializedName("nextSteps") val nextSteps: String? = null,
)

data class CreateInvestorUpdateResultDto(
    @SerializedName("investorUpdateId") val investorUpdateId: String,
    @SerializedName("momentId") val momentId: String? = null,
)

data class CreateBudgetAlertBody(
    val title: String,
    @SerializedName("metricLabel") val metricLabel: String? = null,
    @SerializedName("thresholdValue") val thresholdValue: String? = null,
    @SerializedName("currencyCode") val currencyCode: String? = null,
    val severity: String? = null,
    val note: String? = null,
)

data class CreateBudgetAlertResultDto(
    @SerializedName("budgetAlertId") val budgetAlertId: String,
    @SerializedName("momentId") val momentId: String? = null,
)

data class CreateBusinessReviewBody(
    val period: String? = null,
    val summary: String,
    val outcome: String? = null,
)

data class CreateBusinessReviewResultDto(
    @SerializedName("businessReviewId") val businessReviewId: String,
    @SerializedName("momentId") val momentId: String? = null,
)

data class CreateDecisionBody(
    val title: String,
    @SerializedName("decisionText") val decisionText: String,
    val rationale: String? = null,
)

data class CreateDecisionResultDto(
    @SerializedName("decisionId") val decisionId: String,
    @SerializedName("momentId") val momentId: String? = null,
)

data class CreateMeetingRecordBody(
    val title: String,
    @SerializedName("meetingAt") val meetingAt: String? = null,
    @SerializedName("attendeesText") val attendeesText: String? = null,
    val notes: String? = null,
    @SerializedName("decisionsText") val decisionsText: String? = null,
)

data class CreateMeetingRecordResultDto(
    @SerializedName("meetingRecordId") val meetingRecordId: String,
    @SerializedName("momentId") val momentId: String? = null,
)

data class CreateRecognitionBody(
    @SerializedName("recipientName") val recipientName: String,
    @SerializedName("recognitionType") val recognitionType: String? = null,
    @SerializedName("whyText") val whyText: String,
)

data class CreateRecognitionResultDto(
    @SerializedName("recognitionId") val recognitionId: String,
    @SerializedName("momentId") val momentId: String? = null,
)

data class CreateRetrospectiveBody(
    @SerializedName("wentWell") val wentWell: String? = null,
    @SerializedName("improveNext") val improveNext: String? = null,
)

data class CreateRetrospectiveResultDto(
    @SerializedName("retrospectiveId") val retrospectiveId: String,
    @SerializedName("momentId") val momentId: String? = null,
)

data class CreateActivityLogEntryBody(
    val title: String,
    @SerializedName("ownerLabel") val ownerLabel: String? = null,
    @SerializedName("categoryCode") val categoryCode: String? = null,
)

data class CreateActivityLogEntryResultDto(
    @SerializedName("activityLogEntryId") val activityLogEntryId: String,
    @SerializedName("momentId") val momentId: String? = null,
)

data class CreateIssueEvidenceBody(
    val note: String? = null,
    val url: String? = null,
)

data class CreateIssueEvidenceResultDto(
    @SerializedName("evidenceId") val evidenceId: String,
    @SerializedName("issueId") val issueId: String? = null,
)

data class ShareLinkResultDto(
    @SerializedName("shareUrl") val shareUrl: String? = null,
    @SerializedName("shareToken") val shareToken: String? = null,
    @SerializedName("expiresAt") val expiresAt: String? = null,
    val note: String? = null,
)

// --- Wave 3 GET projection DTOs (flat backend envelope) ---

data class CapacityDto(
    @SerializedName("capacityPct") val capacityPct: Int? = null,
    val note: String? = null,
)

data class WorkloadDto(
    @SerializedName("byDepartment") val byDepartment: List<WorkloadDepartmentDto> = emptyList(),
    val note: String? = null,
)

data class WorkloadDepartmentDto(
    val name: String = "",
    val count: Int = 0,
)

data class MomDeltasDto(
    @SerializedName("revenueMomPct") val revenueMomPct: Int? = null,
    @SerializedName("expenseMomPct") val expenseMomPct: Int? = null,
    val note: String? = null,
)

data class ProgressSnapshotDto(
    val collections: List<Map<String, Any?>> = emptyList(),
    val health: String? = null,
    val note: String? = null,
)

data class RosterMemberDto(
    @SerializedName("userId") val userId: String = "",
    @SerializedName("displayName") val displayName: String = "",
    @SerializedName("membershipType") val membershipType: String = "",
    val status: String = "",
)

data class RosterDto(
    val members: List<RosterMemberDto> = emptyList(),
    val note: String? = null,
)

data class WeeklyReportSectionDto(
    val heading: String = "",
    val items: List<String> = emptyList(),
)

data class WeeklyReportDto(
    val title: String = "",
    val sections: List<WeeklyReportSectionDto> = emptyList(),
    @SerializedName("generatedAt") val generatedAt: String? = null,
    val period: String? = null,
    val note: String? = null,
)

data class VendorItemDto(
    @SerializedName("vendorId") val vendorId: String,
    val name: String,
    @SerializedName("vendorType") val vendorType: String? = null,
    val status: String? = null,
)

data class VendorListDto(
    val items: List<VendorItemDto> = emptyList(),
)
