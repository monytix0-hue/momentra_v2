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
)

data class MomentStoryIdentityDto(
    val momentId: String? = null,
    val title: String? = null,
    val familyProfile: String? = null,
    val startAt: String? = null,
    val endAt: String? = null,
)

data class MomentStoryNarrativeDto(
    val opening: String? = null,
    val insights: List<String>? = null,
)

data class MomentStoryDisplayDto(
    val displayLabel: String? = null,
    val coverEyebrow: String? = null,
    val closeLine: String? = null,
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

data class MomentStoryMoneyCategoryDto(
    val name: String? = null,
    val amount: Double? = null,
)

data class MomentStoryMoneyDto(
    val contributed: Double? = null,
    val spent: Double? = null,
    val remaining: Double? = null,
    val unsettled: Double? = null,
    val categories: List<MomentStoryMoneyCategoryDto>? = null,
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
