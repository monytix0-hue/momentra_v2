import Foundation
import SwiftUI
import Testing
@testable import momentra

/// Mirrors `MomentSwitcherSpecsTest` on Android — the two platforms must resolve the same state for
/// the same inputs, classify the same code into the same bucket, and pick the same copy.
struct MomentSwitcherSpecsTests {

    private let business = businessMomentSwitcherSpec(companyName: "Northwind Traders")
    private let group = groupMomentSwitcherSpec(allowDismiss: false)

    // MARK: State resolution

    @Test func completedErrorWinsOverEverythingOnTheCompletedTab() {
        #expect(
            resolveMomentSwitcherState(
                tab: .completed,
                sourceCount: 3,
                filteredCount: 3,
                completedLoading: true,
                completedError: "boom"
            ) == .loadError
        )
    }

    @Test func completedErrorIsIgnoredOnTheOngoingTab() {
        #expect(
            resolveMomentSwitcherState(
                tab: .ongoing,
                sourceCount: 3,
                filteredCount: 3,
                completedLoading: false,
                completedError: "boom"
            ) == .ongoing
        )
    }

    @Test func inFlightCompletedFetchWithNoCacheShowsLoading() {
        // Regression: the retired views rendered an empty Completed tab here.
        #expect(
            resolveMomentSwitcherState(
                tab: .completed,
                sourceCount: 0,
                filteredCount: 0,
                completedLoading: true,
                completedError: nil
            ) == .loading
        )
    }

    @Test func loadingDoesNotOverrideACachedCompletedList() {
        #expect(
            resolveMomentSwitcherState(
                tab: .completed,
                sourceCount: 2,
                filteredCount: 2,
                completedLoading: true,
                completedError: nil
            ) == .completed
        )
    }

    @Test func emptySourceSplitsIntoEmptyAndCompletedEmpty() {
        #expect(resolveMomentSwitcherState(tab: .ongoing, sourceCount: 0, filteredCount: 0, completedLoading: false, completedError: nil) == .empty)
        #expect(resolveMomentSwitcherState(tab: .completed, sourceCount: 0, filteredCount: 0, completedLoading: false, completedError: nil) == .completedEmpty)
    }

    @Test func filtersThatRemoveEverythingShowSearchEmpty() {
        #expect(resolveMomentSwitcherState(tab: .ongoing, sourceCount: 4, filteredCount: 0, completedLoading: false, completedError: nil) == .searchEmpty)
        #expect(resolveMomentSwitcherState(tab: .completed, sourceCount: 4, filteredCount: 0, completedLoading: false, completedError: nil) == .searchEmpty)
    }

    @Test func dataShowsTheMatchingTabState() {
        #expect(resolveMomentSwitcherState(tab: .ongoing, sourceCount: 2, filteredCount: 2, completedLoading: false, completedError: nil) == .ongoing)
        #expect(resolveMomentSwitcherState(tab: .completed, sourceCount: 2, filteredCount: 2, completedLoading: false, completedError: nil) == .completed)
    }

    // MARK: Headline + subtitle copy

    @Test func headlinePluralisesPerTab() {
        #expect(momentSwitcherHeadline(count: 1, tab: .ongoing) == "1 active moment")
        #expect(momentSwitcherHeadline(count: 3, tab: .ongoing) == "3 active moments")
        #expect(momentSwitcherHeadline(count: 1, tab: .completed) == "1 completed moment")
        #expect(momentSwitcherHeadline(count: 0, tab: .completed) == "0 completed moments")
    }

    @Test func subtitlesMatchTheFigmaStrings() {
        #expect(momentSwitcherSubtitle(spec: business, state: .ongoing) == "Your live moment · ready to open")
        #expect(momentSwitcherSubtitle(spec: business, state: .completed) == "Wrapped moments and their Stories")
        #expect(momentSwitcherSubtitle(spec: business, state: .completedEmpty) == "Wrapped moments and their Stories")
        #expect(momentSwitcherSubtitle(spec: business, state: .empty) == "Nothing live right now")
        #expect(momentSwitcherSubtitle(spec: business, state: .searchEmpty) == "Searching Ongoing")
        #expect(momentSwitcherSubtitle(spec: business, state: .loadError) == "Couldn't refresh")
    }

    @Test func emptyAndCompletedEmptyPickTheirOwnCopy() {
        #expect(business.emptyTitle.contains("live business"))
        #expect(business.completedEmptyTitle.contains("completed"))
        #expect(business.completedEmptyBody.contains("Northwind Traders"))
        #expect(group.emptyBody.contains("group"))
        #expect(group.completedEmptyBody.contains("group"))
        #expect(business.emptyCta == "Create first moment")
        #expect(business.completedEmptyCta == "Create another moment")
    }

    @Test func footerCopyDiffersByContext() {
        #expect(business.footerBody.contains("business"))
        #expect(group.footerBody.contains("shared experience"))
        #expect(business.footerCta == "Create another moment")
        // Home mode must not offer a close affordance.
        #expect(group.allowDismiss == false)
        #expect(business.allowDismiss)
    }

    // MARK: Classification

    @Test func businessClassifierMapsEveryDocumentedTypeCode() {
        #expect(business.classify("OWN_BUSINESS_CAMPAIGN").id == "marketing")
        #expect(business.classify("CLIENT_CAMPAIGN").id == "marketing")
        #expect(business.classify("TRADING_WHOLESALE").id == "trading")
        #expect(business.classify("DISTRIBUTION_DEALERSHIP").id == "distribution")
        #expect(business.classify("RETAIL_STORE").id == "retail")
        #expect(business.classify("RESTAURANT_FOOD").id == "food")
        #expect(business.classify("CONSUMER_SERVICE").id == "service")
        #expect(business.classify("MANUFACTURING").id == "manufacturing")
        #expect(business.classify("MADE_TO_ORDER").id == "manufacturing")
        // Fallbacks.
        #expect(business.classify(nil).id == "trading")
        #expect(business.classify("SOMETHING_NEW").id == "trading")
    }

    @Test func businessBucketsKeepTheVerticalAndUnifiedAccent() {
        #expect(business.classify("RETAIL_STORE").vertical == BusinessAppStateVertical.retail)
        #expect(business.classify("RESTAURANT_FOOD").vertical == BusinessAppStateVertical.rst)
        #expect(business.classify("CONSUMER_SERVICE").vertical == BusinessAppStateVertical.services)
        #expect(business.classify("OWN_BUSINESS_CAMPAIGN").vertical == BusinessAppStateVertical.marketing)
        #expect(business.classify("DISTRIBUTION_DEALERSHIP").vertical == BusinessAppStateVertical.distribution)
        #expect(business.classify("MADE_TO_ORDER").vertical == BusinessAppStateVertical.mfg)
        #expect(business.classify("TRADING_WHOLESALE").vertical == BusinessAppStateVertical.trading)

        for bucket in business.buckets {
            #expect(bucket.accent == BusinessAppStateAccent.emerald, "\(bucket.id) must use the unified emerald accent")
        }
    }

    @Test func groupClassifierMapsEveryBucket() {
        #expect(groupMomentDirectoryBucket("WEDDING") == .celebrations)
        #expect(groupMomentDirectoryBucket("BIRTHDAY_DINNER") == .celebrations)
        #expect(groupMomentDirectoryBucket("TRIP") == .trips)
        #expect(groupMomentDirectoryBucket("WEEKEND_HIKE") == .trips)
        #expect(groupMomentDirectoryBucket("OFFICE_OUTING") == .plans)
        #expect(groupMomentDirectoryBucket("FLATMATES") == .plans)
        #expect(groupMomentDirectoryBucket("SHARED_GENERIC") == .gatherings)
        #expect(groupMomentDirectoryBucket(nil) == .gatherings)
    }

    @Test func classifiersAlwaysReturnABucketFromTheSpecList() {
        for spec in [business, group] {
            let specIds = spec.buckets.map(\.id)
            for code in [nil, "", "WEDDING", "TRIP", "RETAIL_STORE", "SOMETHING_NEW"] {
                let bucketId = spec.classify(code).id
                #expect(
                    specIds.contains(bucketId),
                    "\(spec.testTag): \(code ?? "nil") classified to \(bucketId) outside the spec list"
                )
            }
        }
    }

    @Test func noBucketHasAnEmptyGradient() {
        for spec in [business, group] {
            for bucket in spec.buckets {
                #expect(bucket.cardGradient.count >= 2, "\(spec.testTag)/\(bucket.id) needs two stops")
            }
        }
    }

    @Test func bucketIdsAreStableForPersistedFilters() {
        #expect(group.buckets.map(\.id) == ["CELEBRATIONS", "GATHERINGS", "TRIPS", "PLANS"])
        #expect(business.buckets.map(\.id) == [
            "marketing", "trading", "distribution", "retail",
            "food", "service", "manufacturing",
        ])
    }

    // MARK: Loading + filtering helpers

    @Test func ongoingOnlyKeepsLiveStatusesPlusTheSelectedMoment() {
        let moments = [
            MomentSummary(momentId: "1", title: "Live", status: "ACTIVE"),
            MomentSummary(momentId: "2", title: "Draft", status: "DRAFT"),
            MomentSummary(momentId: "3", title: "Done", status: "COMPLETED"),
            MomentSummary(momentId: "4", title: "Paused", status: "PAUSED"),
        ]
        #expect(ongoingSwitcherMoments(moments, selectedMomentId: nil).map(\.momentId) == ["1", "2"])
        #expect(ongoingSwitcherMoments(moments, selectedMomentId: "4").map(\.momentId) == ["1", "2", "4"])
    }

    @Test func completedOnlyKeepsCompletedStatuses() {
        let completed = [
            MomentSummary(momentId: "1", title: "Wrapped", status: "COMPLETED"),
            MomentSummary(momentId: "2", title: "Still live", status: "ACTIVE"),
        ]
        #expect(completedSwitcherMoments(completed).map(\.momentId) == ["1"])
    }

    @Test func filteringAppliesQueryAndBucketTogether() {
        let moments = [
            MomentSummary(momentId: "1", title: "Sunrise Launch", status: "ACTIVE", momentTypeCode: "CLIENT_CAMPAIGN"),
            MomentSummary(momentId: "2", title: "Retail Floor", status: "ACTIVE", momentTypeCode: "RETAIL_STORE"),
            MomentSummary(momentId: "3", title: "Sunset Push", status: "ACTIVE", momentTypeCode: "TRADING_WHOLESALE"),
        ]
        #expect(filterSwitcherMoments(moments, query: "", bucketId: nil, spec: business).map(\.momentId) == ["1", "2", "3"])
        #expect(filterSwitcherMoments(moments, query: "sun", bucketId: nil, spec: business).map(\.momentId) == ["1", "3"])
        #expect(filterSwitcherMoments(moments, query: "", bucketId: "retail", spec: business).map(\.momentId) == ["2"])
        #expect(filterSwitcherMoments(moments, query: "sun", bucketId: "retail", spec: business).isEmpty)
    }

    @Test func queryIsTrimmedBeforeMatching() {
        let moments = [MomentSummary(momentId: "1", title: "Sunrise Launch", status: "ACTIVE")]
        #expect(filterSwitcherMoments(moments, query: "  sunrise  ", bucketId: nil, spec: business).map(\.momentId) == ["1"])
    }

    @Test func memberLabelUsesTheBucketsSingularForm() {
        let bucket = business.classify("TRADING_WHOLESALE")
        let solo = MomentSummary(
            momentId: "1",
            title: "t",
            status: "ACTIVE",
            momentTypeCode: "TRADING_WHOLESALE",
            participantCount: 1
        )
        let team = MomentSummary(
            momentId: "1",
            title: "t",
            status: "ACTIVE",
            momentTypeCode: "TRADING_WHOLESALE",
            participantCount: 4
        )
        #expect(business.memberLabel(solo, bucket) == "1 member · Trading")
        #expect(business.memberLabel(team, bucket) == "4 members · Trading")
    }

    @Test func specsCarryDistinctTestTags() {
        #expect(business.testTag == "business.moment.directory")
        #expect(group.testTag == "group.moment.directory")
    }
}
