package com.example.momentra.data.api

data class TradingStoryOptionsDto(
    val momentId: String? = null,
    val mode: String? = null,
    val timezone: String? = null,
    val currency: String? = null,
    val allowedPeriods: List<String> = emptyList(),
    val gstAvailable: Boolean = false,
    val permissions: TradingStoryPermissionsDto? = null,
)

data class TradingStoryPermissionsDto(
    val canGenerate: Boolean = false,
    val canExport: Boolean = false,
    val canShare: Boolean = false,
)

data class TradingStoryPreviewBody(
    val startDate: String,
    val endDate: String,
    val detail: String? = "SUMMARY",
    val includeGst: Boolean? = false,
    val includeRegister: Boolean? = false,
)

data class TradingStoryPreviewStartDto(
    val jobId: String? = null,
    val status: String? = null,
    val previewId: String? = null,
)

data class TradingStoryJobDto(
    val jobId: String? = null,
    val previewId: String? = null,
    val momentId: String? = null,
    val status: String? = null,
    val progress: Int? = null,
    val warnings: List<String> = emptyList(),
)

data class TradingStoryPageDto(
    val pageId: String? = null,
    val title: String? = null,
    val hero: TradingStoryHeroDto? = null,
    val cards: List<TradingStoryCardDto> = emptyList(),
    val narrative: List<String> = emptyList(),
    val sourceRefs: List<String> = emptyList(),
    val state: String? = null,
)

data class TradingStoryHeroDto(
    val metricId: String? = null,
    val value: Double? = null,
    val label: String? = null,
    val unit: String? = null,
)

data class TradingStoryCardDto(
    val label: String? = null,
    val value: String? = null,
    val metricId: String? = null,
)

data class TradingStoryPreviewDto(
    val previewId: String? = null,
    val jobId: String? = null,
    val status: String? = null,
    val momentId: String? = null,
    val mode: String? = null,
    val currency: String? = null,
    val detail: String? = null,
    val includeGst: Boolean = false,
    val pages: List<TradingStoryPageDto> = emptyList(),
    val warnings: List<String> = emptyList(),
    val businessName: String? = null,
    val coverage: List<TradingStoryCoverageDto> = emptyList(),
)

data class TradingStoryCoverageDto(
    val metricId: String? = null,
    val missing: Boolean = false,
)

data class TradingStorySaveResultDto(
    val storyId: String? = null,
    val version: Int? = null,
    val snapshotWatermark: String? = null,
    val savedAt: String? = null,
)

data class OperatingGstReturnsDto(
    val month: String? = null,
    val note: String? = null,
)
