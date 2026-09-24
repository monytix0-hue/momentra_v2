package com.example.momentra.data.api

data class MomentStoryStatusDto(
    val status: String? = null,
    val storyId: String? = null,
    val storyVersion: Int? = null,
    val familyProfile: String? = null,
    val errorMessage: String? = null,
)

data class MomentStoryDto(
    val storyId: String? = null,
    val storyVersion: Int? = null,
    val status: String? = null,
    val familyProfile: String? = null,
    val chapters: List<String>? = null,
    val snapshot: MomentStorySnapshotDto? = null,
)

data class MomentStorySnapshotDto(
    val identity: MomentStoryIdentityDto? = null,
    val metrics: Map<String, Any?>? = null,
    val narrative: MomentStoryNarrativeDto? = null,
    val display: MomentStoryDisplayDto? = null,
    val chapters: List<String>? = null,
    val timeline: List<MomentStoryTimelineItemDto>? = null,
    val decisions: List<MomentStoryDecisionDto>? = null,
    val money: MomentStoryMoneyDto? = null,
    val memories: List<MomentStoryMemoryDto>? = null,
    val photos: List<MomentStoryPhotoDto>? = null,
    val places: List<MomentStoryPlaceDto>? = null,
    val people: List<MomentStoryPersonDto>? = null,
)

data class MomentStoryIdentityDto(
    val momentId: String? = null,
    val title: String? = null,
    val familyProfile: String? = null,
    val startAt: String? = null,
    val endAt: String? = null,
    val currencyCode: String? = null,
)

data class MomentStoryNarrativeDto(
    val opening: String? = null,
    val insights: List<String>? = null,
)

data class MomentStoryMetricKeyDto(
    val key: String? = null,
    val label: String? = null,
)

data class MomentStoryDisplayDto(
    val displayLabel: String? = null,
    val coverEyebrow: String? = null,
    val closeLine: String? = null,
    val metricKeys: List<MomentStoryMetricKeyDto>? = null,
)

data class MomentStoryTimelineItemDto(
    val at: String? = null,
    val label: String? = null,
    val detail: String? = null,
)

data class MomentStoryDecisionDto(
    val title: String? = null,
    val status: String? = null,
)

data class MomentStoryPlaceDto(
    val label: String? = null,
    val startAt: String? = null,
    val endAt: String? = null,
)

data class MomentStoryPersonDto(
    val userId: String? = null,
    val displayName: String? = null,
    val roleCode: String? = null,
)

data class MomentStoryPhotoDto(
    val url: String? = null,
    val at: String? = null,
)

data class MomentStoryMoneyCategoryDto(
    val name: String? = null,
    val amount: Double? = null,
)

data class MomentStoryMoneyPayerDto(
    val name: String? = null,
    val amount: Double? = null,
    val payments: Int? = null,
)

data class MomentStoryMoneyExpenseDto(
    val description: String? = null,
    val category: String? = null,
    val payer: String? = null,
    val amount: Double? = null,
    val at: String? = null,
)

data class MomentStoryMoneyDto(
    val contributed: Double? = null,
    val spent: Double? = null,
    val remaining: Double? = null,
    val unsettled: Double? = null,
    val target: Double? = null,
    val categories: List<MomentStoryMoneyCategoryDto>? = null,
    val payers: List<MomentStoryMoneyPayerDto>? = null,
    val contributors: List<MomentStoryMoneyCategoryDto>? = null,
    val expenses: List<MomentStoryMoneyExpenseDto>? = null,
)

data class MomentStoryMemoryDto(
    val text: String? = null,
    val mediaUrl: String? = null,
    val at: String? = null,
)

data class MomentStorySharePackDto(
    val coverSvg: String? = null,
    val blurb: String? = null,
    val webUrl: String? = null,
    val webPath: String? = null,
    val appDeepLink: String? = null,
    val storyId: String? = null,
    val title: String? = null,
)

data class MomentStoryArtifactDto(
    val artifactId: String? = null,
    val contentType: String? = null,
    val body: String? = null,
)
