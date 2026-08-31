package com.example.momentra.domain

/** Command result from POST /v1/moments — authoritative for post-create selection. */
data class CreateMomentOutcome(
    val momentId: String,
    val title: String,
    val domainCode: String,
    val status: String,
    val version: Long,
    val momentTypeCode: String? = null,
    val setupId: String? = null,
    val projectionHints: List<ProjectionHint> = emptyList(),
)

data class ProjectionHint(
    val projection: String,
    val action: String = "invalidate",
)
