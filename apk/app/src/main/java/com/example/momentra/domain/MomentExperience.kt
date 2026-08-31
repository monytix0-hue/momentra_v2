package com.example.momentra.domain

/** Presentation-level Moment experience — derived from real list reads, not a backend SM. */
enum class MomentExperienceKind {
    LOADING,
    ERROR,
    FIRST_MOMENT,
    ACTIVE,
    BETWEEN_MOMENTS,
    PAUSED_ONLY,
}

data class MomentSummary(
    val momentId: String,
    val title: String,
    val status: String,
    val momentTypeCode: String? = null,
    /** Present for BUSINESS bootstrap moments — used for Company→Moment scoping. */
    val companyId: String? = null,
)

fun MomentSummary.isActiveStatus(): Boolean =
    status.equals("ACTIVE", ignoreCase = true) || status.equals("DRAFT", ignoreCase = true)

fun MomentSummary.isPausedStatus(): Boolean =
    status.equals("PAUSED", ignoreCase = true)

fun MomentSummary.isHistoricalStatus(): Boolean =
    status.equals("COMPLETED", ignoreCase = true) ||
        status.equals("CANCELLED", ignoreCase = true) ||
        status.equals("ARCHIVED", ignoreCase = true) ||
        status.equals("DELETED", ignoreCase = true)

fun resolveMomentExperience(moments: List<MomentSummary>): MomentExperienceKind {
    if (moments.isEmpty()) return MomentExperienceKind.FIRST_MOMENT
    val hasActive = moments.any { it.isActiveStatus() }
    if (hasActive) return MomentExperienceKind.ACTIVE
    val hasPaused = moments.any { it.isPausedStatus() }
    val hasHistory = moments.any { it.isHistoricalStatus() || !it.isPausedStatus() }
    return when {
        hasPaused && !hasHistory -> MomentExperienceKind.PAUSED_ONLY
        else -> MomentExperienceKind.BETWEEN_MOMENTS
    }
}

fun recentHistoryMoments(moments: List<MomentSummary>, limit: Int = 5): List<MomentSummary> =
    moments
        .filter {
            !it.isActiveStatus() &&
                !it.status.equals("ARCHIVED", ignoreCase = true)
        }
        .take(limit)

fun activeMomentCount(moments: List<MomentSummary>): Int =
    moments.count { it.isActiveStatus() }
