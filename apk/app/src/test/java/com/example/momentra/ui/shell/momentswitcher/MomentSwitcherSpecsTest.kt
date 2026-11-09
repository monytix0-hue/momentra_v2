package com.example.momentra.ui.shell.momentswitcher

import com.example.momentra.domain.MomentSummary
import com.example.momentra.ui.shell.business.appstate.BusinessAppStateAccent
import com.example.momentra.ui.shell.business.appstate.BusinessAppStateVertical
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the shared switcher contract: state resolution order (identical on iOS), bucket
 * classification, copy selection, and the invariants the composition relies on.
 */
class MomentSwitcherSpecsTest {

    private val business = businessMomentSwitcherSpec("Northwind Traders")
    private val group = groupMomentSwitcherSpec(allowDismiss = false)

    // MARK: State resolution

    @Test
    fun completedErrorWinsOverEverythingOnTheCompletedTab() {
        assertEquals(
            MomentSwitcherState.LoadError,
            resolveMomentSwitcherState(
                tab = MomentSwitcherTab.COMPLETED,
                sourceCount = 3,
                filteredCount = 3,
                completedLoading = true,
                completedError = "boom",
            ),
        )
    }

    @Test
    fun completedErrorIsIgnoredOnTheOngoingTab() {
        assertEquals(
            MomentSwitcherState.Ongoing,
            resolveMomentSwitcherState(
                tab = MomentSwitcherTab.ONGOING,
                sourceCount = 3,
                filteredCount = 3,
                completedLoading = false,
                completedError = "boom",
            ),
        )
    }

    @Test
    fun inFlightCompletedFetchWithNoCacheShowsLoading() {
        // Regression: the previous implementation rendered an empty Completed tab here.
        assertEquals(
            MomentSwitcherState.Loading,
            resolveMomentSwitcherState(
                tab = MomentSwitcherTab.COMPLETED,
                sourceCount = 0,
                filteredCount = 0,
                completedLoading = true,
                completedError = null,
            ),
        )
    }

    @Test
    fun loadingDoesNotOverrideACachedCompletedList() {
        assertEquals(
            MomentSwitcherState.Completed,
            resolveMomentSwitcherState(
                tab = MomentSwitcherTab.COMPLETED,
                sourceCount = 2,
                filteredCount = 2,
                completedLoading = true,
                completedError = null,
            ),
        )
    }

    @Test
    fun emptySourceSplitsIntoEmptyAndCompletedEmpty() {
        assertEquals(
            MomentSwitcherState.Empty,
            resolveMomentSwitcherState(MomentSwitcherTab.ONGOING, 0, 0, false, null),
        )
        assertEquals(
            MomentSwitcherState.CompletedEmpty,
            resolveMomentSwitcherState(MomentSwitcherTab.COMPLETED, 0, 0, false, null),
        )
    }

    @Test
    fun filtersThatRemoveEverythingShowSearchEmpty() {
        assertEquals(
            MomentSwitcherState.SearchEmpty,
            resolveMomentSwitcherState(MomentSwitcherTab.ONGOING, 4, 0, false, null),
        )
        assertEquals(
            MomentSwitcherState.SearchEmpty,
            resolveMomentSwitcherState(MomentSwitcherTab.COMPLETED, 4, 0, false, null),
        )
    }

    @Test
    fun dataShowsTheMatchingTabState() {
        assertEquals(
            MomentSwitcherState.Ongoing,
            resolveMomentSwitcherState(MomentSwitcherTab.ONGOING, 2, 2, false, null),
        )
        assertEquals(
            MomentSwitcherState.Completed,
            resolveMomentSwitcherState(MomentSwitcherTab.COMPLETED, 2, 2, false, null),
        )
    }

    // MARK: Headline + subtitle copy

    @Test
    fun headlinePluralisesPerTab() {
        assertEquals("1 active moment", momentSwitcherHeadline(1, MomentSwitcherTab.ONGOING))
        assertEquals("3 active moments", momentSwitcherHeadline(3, MomentSwitcherTab.ONGOING))
        assertEquals("1 completed moment", momentSwitcherHeadline(1, MomentSwitcherTab.COMPLETED))
        assertEquals("0 completed moments", momentSwitcherHeadline(0, MomentSwitcherTab.COMPLETED))
    }

    @Test
    fun subtitlesMatchTheFigmaStrings() {
        assertEquals(
            "Your live moment · ready to open",
            momentSwitcherSubtitle(business, MomentSwitcherState.Ongoing),
        )
        assertEquals(
            "Wrapped moments and their Stories",
            momentSwitcherSubtitle(business, MomentSwitcherState.Completed),
        )
        assertEquals(
            "Wrapped moments and their Stories",
            momentSwitcherSubtitle(business, MomentSwitcherState.CompletedEmpty),
        )
        assertEquals(
            "Nothing live right now",
            momentSwitcherSubtitle(business, MomentSwitcherState.Empty),
        )
        assertEquals(
            "Searching Ongoing",
            momentSwitcherSubtitle(business, MomentSwitcherState.SearchEmpty),
        )
        assertEquals(
            "Couldn't refresh",
            momentSwitcherSubtitle(business, MomentSwitcherState.LoadError),
        )
    }

    @Test
    fun emptyAndCompletedEmptyPickTheirOwnCopy() {
        assertTrue(business.emptyTitle.contains("live business"))
        assertTrue(business.completedEmptyTitle.contains("completed"))
        assertTrue(business.completedEmptyBody.contains("Northwind Traders"))
        assertTrue(group.emptyBody.contains("group"))
        assertTrue(group.completedEmptyBody.contains("group"))
        assertEquals("Create first moment", business.emptyCta)
        assertEquals("Create another moment", business.completedEmptyCta)
    }

    @Test
    fun footerCopyDiffersByContext() {
        assertTrue(business.footerBody.contains("business"))
        assertTrue(group.footerBody.contains("shared experience"))
        assertEquals("Create another moment", business.footerCta)
        // Home mode must not offer a close affordance.
        assertFalse(group.allowDismiss)
        assertTrue(business.allowDismiss)
    }

    // MARK: Classification

    @Test
    fun businessClassifierMapsEveryDocumentedTypeCode() {
        assertEquals("marketing", business.classify("OWN_BUSINESS_CAMPAIGN").id)
        assertEquals("marketing", business.classify("CLIENT_CAMPAIGN").id)
        assertEquals("trading", business.classify("TRADING_WHOLESALE").id)
        assertEquals("distribution", business.classify("DISTRIBUTION_DEALERSHIP").id)
        assertEquals("retail", business.classify("RETAIL_STORE").id)
        assertEquals("food", business.classify("RESTAURANT_FOOD").id)
        assertEquals("service", business.classify("CONSUMER_SERVICE").id)
        assertEquals("manufacturing", business.classify("MANUFACTURING").id)
        assertEquals("manufacturing", business.classify("MADE_TO_ORDER").id)
        // Fallbacks.
        assertEquals("trading", business.classify(null).id)
        assertEquals("trading", business.classify("SOMETHING_NEW").id)
    }

    @Test
    fun businessBucketsKeepTheVerticalAndUnifiedAccent() {
        assertEquals(
            BusinessAppStateVertical.Retail,
            business.classify("RETAIL_STORE").vertical,
        )
        assertEquals(
            BusinessAppStateVertical.Rst,
            business.classify("RESTAURANT_FOOD").vertical,
        )
        business.buckets.forEach { bucket ->
            assertEquals(
                "${bucket.id} must use the unified emerald accent",
                BusinessAppStateAccent.Emerald,
                bucket.accent,
            )
        }
    }

    @Test
    fun groupClassifierMapsEveryBucket() {
        assertEquals(GroupMomentDirectoryBucket.CELEBRATIONS, groupMomentDirectoryBucket("WEDDING"))
        assertEquals(
            GroupMomentDirectoryBucket.CELEBRATIONS,
            groupMomentDirectoryBucket("BIRTHDAY_DINNER"),
        )
        assertEquals(GroupMomentDirectoryBucket.TRIPS, groupMomentDirectoryBucket("TRIP"))
        assertEquals(GroupMomentDirectoryBucket.TRIPS, groupMomentDirectoryBucket("WEEKEND_HIKE"))
        assertEquals(
            GroupMomentDirectoryBucket.PLANS,
            groupMomentDirectoryBucket("OFFICE_OUTING"),
        )
        assertEquals(
            GroupMomentDirectoryBucket.PLANS,
            groupMomentDirectoryBucket("FLATMATES"),
        )
        assertEquals(
            GroupMomentDirectoryBucket.GATHERINGS,
            groupMomentDirectoryBucket("SHARED_GENERIC"),
        )
        assertEquals(GroupMomentDirectoryBucket.GATHERINGS, groupMomentDirectoryBucket(null))
    }

    @Test
    fun classifiersAlwaysReturnABucketFromTheSpecList() {
        listOf(business, group).forEach { spec ->
            listOf(null, "", "WEDDING", "TRIP", "RETAIL_STORE", "SOMETHING_NEW").forEach { code ->
                val bucket = spec.classify(code)
                assertTrue(
                    "${spec.testTag}: $code classified to ${bucket.id} outside the spec list",
                    spec.buckets.any { it.id == bucket.id },
                )
            }
        }
    }

    @Test
    fun noBucketHasAnEmptyGradient() {
        listOf(business, group).forEach { spec ->
            spec.buckets.forEach { bucket ->
                assertTrue("${spec.testTag}/${bucket.id}", bucket.cardGradient.isNotEmpty())
                assertEquals(
                    "${spec.testTag}/${bucket.id} must have at least two stops",
                    true,
                    bucket.cardGradient.size >= 2,
                )
            }
        }
    }

    @Test
    fun groupBucketIdsAreStableForPersistedFilters() {
        assertEquals(
            listOf("CELEBRATIONS", "GATHERINGS", "TRIPS", "PLANS"),
            group.buckets.map { it.id },
        )
        assertEquals(
            listOf(
                "marketing", "trading", "distribution", "retail",
                "food", "service", "manufacturing",
            ),
            business.buckets.map { it.id },
        )
    }

    // MARK: Loading + filtering helpers

    @Test
    fun ongoingOnlyKeepsLiveStatusesPlusTheSelectedMoment() {
        val moments = listOf(
            MomentSummary("1", "Live", "ACTIVE"),
            MomentSummary("2", "Draft", "DRAFT"),
            MomentSummary("3", "Done", "COMPLETED"),
            MomentSummary("4", "Paused", "PAUSED"),
        )
        assertEquals(
            listOf("1", "2"),
            ongoingSwitcherMoments(moments, selectedMomentId = null).map { it.momentId },
        )
        assertEquals(
            listOf("1", "2", "4"),
            ongoingSwitcherMoments(moments, selectedMomentId = "4").map { it.momentId },
        )
    }

    @Test
    fun completedOnlyKeepsCompletedStatuses() {
        val completed = listOf(
            MomentSummary("1", "Wrapped", "COMPLETED"),
            MomentSummary("2", "Still live", "ACTIVE"),
        )
        assertEquals(
            listOf("1"),
            completedSwitcherMoments(completed).map { it.momentId },
        )
    }

    @Test
    fun filteringAppliesQueryAndBucketTogether() {
        val moments = listOf(
            MomentSummary("1", "Sunrise Launch", "ACTIVE", "CLIENT_CAMPAIGN"),
            MomentSummary("2", "Retail Floor", "ACTIVE", "RETAIL_STORE"),
            MomentSummary("3", "Sunset Push", "ACTIVE", "TRADING_WHOLESALE"),
        )
        assertEquals(
            listOf("1", "2", "3"),
            filterSwitcherMoments(moments, "", null, business).map { it.momentId },
        )
        assertEquals(
            listOf("1", "3"),
            filterSwitcherMoments(moments, "sun", null, business).map { it.momentId },
        )
        assertEquals(
            listOf("2"),
            filterSwitcherMoments(moments, "", "retail", business).map { it.momentId },
        )
        assertEquals(
            emptyList<String>(),
            filterSwitcherMoments(moments, "sun", "retail", business).map { it.momentId },
        )
    }

    @Test
    fun queryIsTrimmedBeforeMatching() {
        val moments = listOf(MomentSummary("1", "Sunrise Launch", "ACTIVE"))
        assertEquals(
            listOf("1"),
            filterSwitcherMoments(moments, "  sunrise  ", null, business).map { it.momentId },
        )
    }

    @Test
    fun memberLabelUsesTheBucketsSingularForm() {
        val bucket = business.classify("TRADING_WHOLESALE")
        assertEquals(
            "1 member · Trading",
            business.memberLabel(MomentSummary("1", "t", "ACTIVE", "TRADING_WHOLESALE", participantCount = 1), bucket),
        )
        assertEquals(
            "4 members · Trading",
            business.memberLabel(MomentSummary("1", "t", "ACTIVE", "TRADING_WHOLESALE", participantCount = 4), bucket),
        )
    }

    @Test
    fun specsCarryDistinctTestTags() {
        assertEquals("business.moment.directory", business.testTag)
        assertEquals("group.moment.directory", group.testTag)
    }
}
