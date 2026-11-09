package com.example.momentra.ui.shell.momentswitcher

import androidx.compose.ui.graphics.Color
import com.example.momentra.data.repository.MeRepository
import com.example.momentra.domain.MomentSummary
import com.example.momentra.domain.isActiveStatus
import com.example.momentra.domain.isCompletedStatus
import com.example.momentra.ui.shell.business.appstate.BusinessAppStateAccent
import com.example.momentra.ui.shell.business.appstate.BusinessAppStateVertical
import com.example.momentra.ui.shell.group.shared.GroupExperienceFamily
import com.example.momentra.ui.shell.group.shared.groupExperienceFamilyFor
import com.example.momentra.ui.shell.group.shared.isThemedLiving
import com.example.momentra.ui.shell.group.shared.isThemedPurchase
import java.util.Locale

/** Lifecycle tab of the moment switcher. */
enum class MomentSwitcherTab { ONGOING, COMPLETED }

/**
 * The rendered states of the switcher.
 *
 * The five Figma states are `Ongoing`, `Completed`, `Empty`, `SearchEmpty`, `LoadError`. Two extra
 * states exist for runtime correctness: [CompletedEmpty] (Completed tab resolved with zero rows —
 * Figma draws this as the Completed frame's own empty card) and [Loading].
 */
enum class MomentSwitcherState {
    Ongoing,
    Completed,
    CompletedEmpty,
    Empty,
    SearchEmpty,
    LoadError,
    Loading,
}

/**
 * One category bucket. Presentation only — classification mirrors the pre-existing directory
 * classifiers so no moment changes category.
 */
data class MomentSwitcherBucket(
    val id: String,
    val label: String,
    /** Single label form used in the card meta line ("3 members · Trading"). */
    val singular: String,
    /** Card gradient; one stop per entry in [MomentSwitcherTokens.LiveCardStops] when three long. */
    val cardGradient: List<Color>,
    /** Unified accent. The kit is emerald everywhere, matching Figma `appstate/accent`. */
    val accent: BusinessAppStateAccent = BusinessAppStateAccent.Emerald,
    /**
     * Business vertical, carried so the pre-existing `businessDirectoryBucketFor` contract and its
     * copy overrides survive. Null for Group buckets.
     */
    val vertical: BusinessAppStateVertical? = null,
)

/**
 * Everything the switcher needs that differs between Group and Business.
 *
 * Keeping this a pure data contract means both contexts render one identical screen and the
 * differences are testable without a composition.
 */
data class MomentSwitcherSpec(
    val testTag: String,
    val buckets: List<MomentSwitcherBucket>,
    val classify: (momentTypeCode: String?) -> MomentSwitcherBucket,
    val memberLabel: (MomentSummary, MomentSwitcherBucket) -> String,
    /** Copy for the Ongoing-tab empty card. */
    val emptyTitle: String,
    val emptyBody: String,
    val emptyCta: String,
    /** Copy for the Completed-tab empty card. */
    val completedEmptyTitle: String,
    val completedEmptyBody: String,
    val completedEmptyCta: String,
    /** Copy for the diagnostic card. */
    val errorTitle: String,
    val errorBody: String,
    val errorCode: String,
    /** Copy under the ongoing list. */
    val footerTitle: String,
    val footerBody: String,
    val footerCta: String,
    /** Subtitle when the active tab has no rows. */
    val emptySubtitle: String,
    /** Subtitle while a search is active. */
    val searchingSubtitle: String,
    val allowDismiss: Boolean,
    val loadCompleted: suspend (MeRepository) -> Result<List<MomentSummary>>,
)

/**
 * Canonical state resolution, shared by both platforms.
 *
 * The ordering matters: an in-flight Completed fetch must show [MomentSwitcherState.Loading]
 * rather than an empty list, and an error must win over everything else on the Completed tab.
 */
fun resolveMomentSwitcherState(
    tab: MomentSwitcherTab,
    sourceCount: Int,
    filteredCount: Int,
    completedLoading: Boolean,
    completedError: String?,
): MomentSwitcherState = when {
    tab == MomentSwitcherTab.COMPLETED && completedError != null -> MomentSwitcherState.LoadError
    tab == MomentSwitcherTab.COMPLETED && completedLoading && sourceCount == 0 ->
        MomentSwitcherState.Loading
    sourceCount == 0 ->
        if (tab == MomentSwitcherTab.COMPLETED) {
            MomentSwitcherState.CompletedEmpty
        } else {
            MomentSwitcherState.Empty
        }
    filteredCount == 0 -> MomentSwitcherState.SearchEmpty
    tab == MomentSwitcherTab.COMPLETED -> MomentSwitcherState.Completed
    else -> MomentSwitcherState.Ongoing
}

/** Header headline: `N active moment(s)` / `N completed moment(s)`. */
fun momentSwitcherHeadline(count: Int, tab: MomentSwitcherTab): String = when (tab) {
    MomentSwitcherTab.ONGOING -> "$count active moment${if (count == 1) "" else "s"}"
    MomentSwitcherTab.COMPLETED -> "$count completed moment${if (count == 1) "" else "s"}"
}

/** Header subtitle for the resolved state — Figma `who / subtitle`. */
fun momentSwitcherSubtitle(
    spec: MomentSwitcherSpec,
    state: MomentSwitcherState,
): String = when (state) {
    MomentSwitcherState.Empty -> spec.emptySubtitle
    MomentSwitcherState.SearchEmpty -> spec.searchingSubtitle
    MomentSwitcherState.LoadError -> "Couldn't refresh"
    MomentSwitcherState.Loading -> "Refreshing moments"
    MomentSwitcherState.CompletedEmpty, MomentSwitcherState.Completed ->
        "Wrapped moments and their Stories"
    MomentSwitcherState.Ongoing -> "Your live moment · ready to open"
}

// ---------------------------------------------------------------------------------------------
// Business
// ---------------------------------------------------------------------------------------------

private val BusinessBuckets: List<MomentSwitcherBucket> = listOf(
    Triple("marketing", "Marketing", BusinessAppStateVertical.Marketing),
    Triple("trading", "Trading", BusinessAppStateVertical.Trading),
    Triple("distribution", "Distribution", BusinessAppStateVertical.Distribution),
    Triple("retail", "Retail", BusinessAppStateVertical.Retail),
    Triple("food", "Food", BusinessAppStateVertical.Rst),
    Triple("service", "Service", BusinessAppStateVertical.Services),
    Triple("manufacturing", "Manufacturing", BusinessAppStateVertical.Mfg),
).map { (id, label, vertical) ->
    MomentSwitcherBucket(
        id = id,
        label = label,
        singular = label,
        cardGradient = MomentSwitcherTokens.LiveCardGradient,
        vertical = vertical,
    )
}

/**
 * Bucket for a Business moment type code.
 *
 * Moved verbatim from the retired `BusinessActiveMomentsDirectory.kt` so the existing
 * `BusinessAppStateSpecsTest.directoryBuckets*` contract still holds: same ids, same verticals,
 * same unified emerald accent.
 */
fun businessDirectoryBucketFor(momentTypeCode: String?): MomentSwitcherBucket {
    val code = momentTypeCode?.uppercase(Locale.US).orEmpty()
    val id = when {
        code.contains("MARKETING") || code.contains("CAMPAIGN") -> "marketing"
        code.contains("TRADING") || code.contains("WHOLESALE") -> "trading"
        code.contains("DISTRIBUTION") || code.contains("DEALERSHIP") -> "distribution"
        code.contains("RETAIL") || code.contains("STORE") -> "retail"
        code.contains("RESTAURANT") || code.contains("FOOD") -> "food"
        code.contains("CONSUMER") || code.contains("SERVICE") -> "service"
        code.contains("MANUFACTURING") || code.contains("PRODUCTION") ||
            code.contains("MADE_TO_ORDER") -> "manufacturing"
        else -> "trading"
    }
    return BusinessBuckets.first { it.id == id }
}

/** Business spec — the pixel reference. */
fun businessMomentSwitcherSpec(
    companyName: String = "Your business",
): MomentSwitcherSpec = MomentSwitcherSpec(
    testTag = "business.moment.directory",
    buckets = BusinessBuckets,
    classify = ::businessDirectoryBucketFor,
    memberLabel = { moment, bucket ->
        val members = moment.participantCount.coerceAtLeast(1)
        "$members member${if (members == 1) "" else "s"} · ${bucket.singular}"
    },
    emptyTitle = "No live business moments",
    emptyBody = "Start a moment for $companyName and its Story will build from verified activity.",
    emptyCta = "Create first moment",
    completedEmptyTitle = "No completed moments yet",
    completedEmptyBody = "When $companyName wraps, its Story lands here.",
    completedEmptyCta = "Create another moment",
    errorTitle = "Couldn't load moments",
    errorBody = "Your moments are safe — we just couldn't reach them. Check your connection and try again.",
    errorCode = "MOMENTS_FETCH_FAILED",
    footerTitle = "That's everything live",
    footerBody = "Spin up another business moment when you're ready.",
    footerCta = "Create another moment",
    emptySubtitle = "Nothing live right now",
    searchingSubtitle = "Searching Ongoing",
    allowDismiss = true,
    loadCompleted = { repo -> repo.listBusinessMoments(limit = 50, lifecycle = "completed") },
)

// ---------------------------------------------------------------------------------------------
// Group
// ---------------------------------------------------------------------------------------------

/** Group buckets, moved verbatim from the retired `GroupActiveMomentsDirectory.kt`. */
enum class GroupMomentDirectoryBucket(val label: String) {
    CELEBRATIONS("Celebrations"),
    GATHERINGS("Gatherings"),
    TRIPS("Trips"),
    PLANS("Plans"),
}

/**
 * Bucket for a Group moment type code. Behaviour is identical to the retired classifier, so no
 * existing group moment changes category.
 */
fun groupMomentDirectoryBucket(momentTypeCode: String?): GroupMomentDirectoryBucket {
    val code = momentTypeCode?.uppercase(Locale.US).orEmpty()
    val family = groupExperienceFamilyFor(momentTypeCode)
    return when {
        code.contains("TRIP") || code.contains("TRAVEL") || code.contains("HIKE") ||
            code.contains("WEEKEND") || code.contains("WATER") -> GroupMomentDirectoryBucket.TRIPS
        family == GroupExperienceFamily.WEDDING ||
            family == GroupExperienceFamily.HOUSE_PARTY ||
            code.contains("BIRTHDAY") ||
            code.contains("CELEBRAT") -> GroupMomentDirectoryBucket.CELEBRATIONS
        family == GroupExperienceFamily.OFFICE_OUTING ||
            family.isThemedLiving() ||
            family.isThemedPurchase() -> GroupMomentDirectoryBucket.PLANS
        else -> GroupMomentDirectoryBucket.GATHERINGS
    }
}

private fun groupBucketSingular(bucket: GroupMomentDirectoryBucket): String = when (bucket) {
    GroupMomentDirectoryBucket.CELEBRATIONS -> "Celebration"
    GroupMomentDirectoryBucket.GATHERINGS -> "Gathering"
    GroupMomentDirectoryBucket.TRIPS -> "Trip"
    GroupMomentDirectoryBucket.PLANS -> "Plan"
}

/** Retains the original per-bucket palette pairs from `bucketPaletteList`; first pair wins. */
private fun groupBucketGradient(bucket: GroupMomentDirectoryBucket): List<Color> = when (bucket) {
    GroupMomentDirectoryBucket.CELEBRATIONS -> listOf(Color(0xFFE879A8), Color(0xFFC084FC))
    GroupMomentDirectoryBucket.GATHERINGS -> listOf(Color(0xFFFB923C), Color(0xFFF97316))
    GroupMomentDirectoryBucket.TRIPS -> listOf(Color(0xFF38BDF8), Color(0xFF0EA5E9))
    GroupMomentDirectoryBucket.PLANS -> listOf(Color(0xFF4ADE80), Color(0xFF22C55E))
}

private val GroupBuckets: List<MomentSwitcherBucket> =
    GroupMomentDirectoryBucket.entries.map { bucket ->
        MomentSwitcherBucket(
            id = bucket.name,
            label = bucket.label,
            singular = groupBucketSingular(bucket),
            cardGradient = groupBucketGradient(bucket),
        )
    }

/** Group spec — same geometry, group taxonomy and copy. */
fun groupMomentSwitcherSpec(allowDismiss: Boolean = true): MomentSwitcherSpec =
    MomentSwitcherSpec(
        testTag = "group.moment.directory",
        buckets = GroupBuckets,
        classify = { code -> GroupBuckets.first { it.id == groupMomentDirectoryBucket(code).name } },
        memberLabel = { moment, bucket ->
            val members = moment.participantCount.coerceAtLeast(0)
            "$members member${if (members == 1) "" else "s"} · ${bucket.singular}"
        },
        emptyTitle = "No live group moments",
        emptyBody = "Start a moment for your group and its Story will build from what everyone shares.",
        emptyCta = "Create first moment",
        completedEmptyTitle = "No completed moments yet",
        completedEmptyBody = "When your group wraps, its Story lands here.",
        completedEmptyCta = "Create another moment",
        errorTitle = "Couldn't load moments",
        errorBody = "Your moments are safe — we just couldn't reach them. Check your connection and try again.",
        errorCode = "MOMENTS_FETCH_FAILED",
        footerTitle = "That's everything live",
        footerBody = "Spin up another shared experience when you're ready.",
        footerCta = "Create another moment",
        emptySubtitle = "Nothing live right now",
        searchingSubtitle = "Searching Ongoing",
        allowDismiss = allowDismiss,
        loadCompleted = { repo -> repo.listGroupMoments(limit = 50, lifecycle = "completed") },
    )

/** Moments shown on the Ongoing tab: live statuses plus the currently selected moment. */
fun ongoingSwitcherMoments(
    moments: List<MomentSummary>,
    selectedMomentId: String?,
): List<MomentSummary> =
    moments.filter { it.isActiveStatus() || it.momentId == selectedMomentId }

/** Completed-tab rows, defensively filtered to completed statuses. */
fun completedSwitcherMoments(completed: List<MomentSummary>): List<MomentSummary> =
    completed.filter { it.isCompletedStatus() }

/** Query + bucket filter, applied identically on both tabs. */
fun filterSwitcherMoments(
    source: List<MomentSummary>,
    query: String,
    bucketId: String?,
    spec: MomentSwitcherSpec,
): List<MomentSummary> {
    val trimmed = query.trim()
    return source.filter { moment ->
        val matchesQuery = trimmed.isEmpty() || moment.title.contains(trimmed, ignoreCase = true)
        val matchesBucket = bucketId == null || spec.classify(moment.momentTypeCode).id == bucketId
        matchesQuery && matchesBucket
    }
}
