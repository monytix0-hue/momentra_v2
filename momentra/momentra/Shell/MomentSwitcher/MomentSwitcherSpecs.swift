import Foundation
import SwiftUI

/// Lifecycle tab of the moment switcher.
enum MomentSwitcherTab: String, CaseIterable, Identifiable {
    case ongoing
    case completed

    var id: String { rawValue }

    var label: String {
        switch self {
        case .ongoing: return "Ongoing"
        case .completed: return "Completed"
        }
    }
}

/// The rendered states of the switcher.
///
/// The five Figma states are `Ongoing`, `Completed`, `Empty`, `SearchEmpty`, `LoadError`. Two extra
/// states exist for runtime correctness: `.completedEmpty` (Completed tab resolved with zero rows —
/// Figma draws this as the Completed frame's own empty card) and `.loading`.
enum MomentSwitcherState: Equatable {
    case ongoing
    case completed
    case completedEmpty
    case empty
    case searchEmpty
    case loadError
    case loading
}

/// One category bucket. Presentation only — classification mirrors the pre-existing directory
/// classifiers so no moment changes category.
struct MomentSwitcherBucket: Identifiable, Equatable {
    let id: String
    let label: String
    /// Single label form used in the card meta line ("3 members · Trading").
    let singular: String
    /// Card gradient: `#312E81` -> emerald-ish stops.
    let cardGradient: [Color]
    /// Business vertical, carried so the pre-existing `businessDirectoryBucketFor` contract and its
    /// copy overrides survive. `nil` for Group buckets.
    let vertical: BusinessAppStateVertical?
    var accent: BusinessAppStateAccent { .emerald }

    static func == (lhs: MomentSwitcherBucket, rhs: MomentSwitcherBucket) -> Bool {
        lhs.id == rhs.id
    }
}

/// Everything the switcher needs that differs between Group and Business.
///
/// A pure data contract so both contexts render one identical screen and the differences are
/// testable without a view hierarchy.
struct MomentSwitcherSpec {
    let testTag: String
    let buckets: [MomentSwitcherBucket]
    let classify: (String?) -> MomentSwitcherBucket
    let memberLabel: (MomentSummary, MomentSwitcherBucket) -> String

    let emptyTitle: String
    let emptyBody: String
    let emptyCta: String
    let completedEmptyTitle: String
    let completedEmptyBody: String
    let completedEmptyCta: String

    let errorTitle: String
    let errorBody: String
    let errorCode: String

    let footerTitle: String
    let footerBody: String
    let footerCta: String

    let emptySubtitle: String
    let searchingSubtitle: String

    let allowDismiss: Bool
    /// Real Completed-tab fetch. Hits `APIClient` directly, following the precedent already inside
    /// the retired `GroupActiveMomentsDirectoryView` — this avoids widening `ShellMeGatewaying` and
    /// its test stub.
    let loadCompleted: () async throws -> [MomentSummary]
}

/// Canonical state resolution, shared by both platforms.
///
/// The ordering matters: an in-flight Completed fetch must show `.loading` rather than an empty
/// list, and an error must win over everything else on the Completed tab.
func resolveMomentSwitcherState(
    tab: MomentSwitcherTab,
    sourceCount: Int,
    filteredCount: Int,
    completedLoading: Bool,
    completedError: String?
) -> MomentSwitcherState {
    if tab == .completed, completedError != nil { return .loadError }
    if tab == .completed, completedLoading, sourceCount == 0 { return .loading }
    if sourceCount == 0 { return tab == .completed ? .completedEmpty : .empty }
    if filteredCount == 0 { return .searchEmpty }
    return tab == .completed ? .completed : .ongoing
}

/// Header headline: `N active moment(s)` / `N completed moment(s)`.
func momentSwitcherHeadline(count: Int, tab: MomentSwitcherTab) -> String {
    let noun = count == 1 ? "moment" : "moments"
    switch tab {
    case .ongoing: return "\(count) active \(noun)"
    case .completed: return "\(count) completed \(noun)"
    }
}

/// Header subtitle for the resolved state — Figma `who / subtitle`.
func momentSwitcherSubtitle(
    spec: MomentSwitcherSpec,
    state: MomentSwitcherState
) -> String {
    switch state {
    case .empty: return spec.emptySubtitle
    case .searchEmpty: return spec.searchingSubtitle
    case .loadError: return "Couldn't refresh"
    case .loading: return "Refreshing moments"
    case .completedEmpty, .completed: return "Wrapped moments and their Stories"
    case .ongoing: return "Your live moment · ready to open"
    }
}

// MARK: - Business

private let businessBuckets: [MomentSwitcherBucket] = [
    ("marketing", "Marketing", BusinessAppStateVertical.marketing),
    ("trading", "Trading", BusinessAppStateVertical.trading),
    ("distribution", "Distribution", BusinessAppStateVertical.distribution),
    ("retail", "Retail", BusinessAppStateVertical.retail),
    ("food", "Food", BusinessAppStateVertical.rst),
    ("service", "Service", BusinessAppStateVertical.services),
    ("manufacturing", "Manufacturing", BusinessAppStateVertical.mfg),
].map { entry in
    MomentSwitcherBucket(
        id: entry.0,
        label: entry.1,
        singular: entry.1,
        cardGradient: MomentSwitcherTokens.liveCardColors,
        vertical: entry.2
    )
}

private let businessBucketsById: [String: MomentSwitcherBucket] = Dictionary(
    uniqueKeysWithValues: businessBuckets.map { ($0.id, $0) }
)

/// Bucket for a Business moment type code.
///
/// Moved verbatim from the retired `BusinessActiveMomentsDirectoryView` so the existing
/// `BusinessAppStateSpecsTests.directoryBuckets*` contract still holds: same ids, same verticals,
/// same unified emerald accent.
func businessDirectoryBucketFor(_ momentTypeCode: String?) -> MomentSwitcherBucket {
    let code = (momentTypeCode ?? "").uppercased()
    let id: String
    if code.contains("MARKETING") || code.contains("CAMPAIGN") {
        id = "marketing"
    } else if code.contains("TRADING") || code.contains("WHOLESALE") {
        id = "trading"
    } else if code.contains("DISTRIBUTION") || code.contains("DEALERSHIP") {
        id = "distribution"
    } else if code.contains("RETAIL") || code.contains("STORE") {
        id = "retail"
    } else if code.contains("RESTAURANT") || code.contains("FOOD") {
        id = "food"
    } else if code.contains("CONSUMER") || code.contains("SERVICE") {
        id = "service"
    } else if code.contains("MANUFACTURING") || code.contains("PRODUCTION")
        || code.contains("MADE_TO_ORDER") {
        id = "manufacturing"
    } else {
        id = "trading"
    }
    return businessBucketsById[id] ?? businessBucketsById["trading"]!
}

/// Business spec — the pixel reference.
func businessMomentSwitcherSpec(companyName: String = "Your business") -> MomentSwitcherSpec {
    MomentSwitcherSpec(
        testTag: "business.moment.directory",
        buckets: businessBuckets,
        classify: businessDirectoryBucketFor,
        memberLabel: { moment, bucket in
            let members = max(moment.participantCount, 1)
            return "\(members) member\(members == 1 ? "" : "s") · \(bucket.singular)"
        },
        emptyTitle: "No live business moments",
        emptyBody: "Start a moment for \(companyName) and its Story will build from verified activity.",
        emptyCta: "Create first moment",
        completedEmptyTitle: "No completed moments yet",
        completedEmptyBody: "When \(companyName) wraps, its Story lands here.",
        completedEmptyCta: "Create another moment",
        errorTitle: "Couldn't load moments",
        errorBody: "Your moments are safe — we just couldn't reach them. Check your connection and try again.",
        errorCode: "MOMENTS_FETCH_FAILED",
        footerTitle: "That's everything live",
        footerBody: "Spin up another business moment when you're ready.",
        footerCta: "Create another moment",
        emptySubtitle: "Nothing live right now",
        searchingSubtitle: "Searching Ongoing",
        allowDismiss: true,
        loadCompleted: {
            try await APIClient.shared.listBusinessMoments(limit: 50, lifecycle: "completed")
        }
    )
}

// MARK: - Group

/// Group buckets, moved verbatim from the retired `GroupActiveMomentsDirectoryView`.
enum GroupMomentDirectoryBucket: String, CaseIterable, Identifiable {
    case celebrations = "CELEBRATIONS"
    case gatherings = "GATHERINGS"
    case trips = "TRIPS"
    case plans = "PLANS"

    var id: String { rawValue }

    var label: String {
        switch self {
        case .celebrations: return "Celebrations"
        case .gatherings: return "Gatherings"
        case .trips: return "Trips"
        case .plans: return "Plans"
        }
    }

    var singular: String {
        switch self {
        case .celebrations: return "Celebration"
        case .gatherings: return "Gathering"
        case .trips: return "Trip"
        case .plans: return "Plan"
        }
    }

    /// Retains the original per-bucket palette pair from `bucketPaletteList`; first pair wins.
    var gradient: [Color] {
        switch self {
        case .celebrations: return [Color(hex: "#E879A8"), Color(hex: "#C084FC")]
        case .gatherings: return [Color(hex: "#FB923C"), Color(hex: "#F97316")]
        case .trips: return [Color(hex: "#38BDF8"), Color(hex: "#0EA5E9")]
        case .plans: return [Color(hex: "#4ADE80"), Color(hex: "#22C55E")]
        }
    }
}

/// Bucket for a Group moment type code. Behaviour is identical to the retired classifier, so no
/// existing group moment changes category.
func groupMomentDirectoryBucket(_ momentTypeCode: String?) -> GroupMomentDirectoryBucket {
    let code = (momentTypeCode ?? "").uppercased()
    let family = GroupExperienceFamily.forTypeCode(momentTypeCode)

    if code.contains("TRIP") || code.contains("TRAVEL") || code.contains("HIKE")
        || code.contains("WEEKEND") || code.contains("WATER") {
        return .trips
    }
    if family == .wedding || family == .houseParty
        || code.contains("BIRTHDAY") || code.contains("CELEBRAT") {
        return .celebrations
    }
    if family == .officeOuting || family.isThemedLiving || family.isThemedPurchase {
        return .plans
    }
    return .gatherings
}

private let groupBuckets: [MomentSwitcherBucket] = GroupMomentDirectoryBucket.allCases.map { bucket in
    MomentSwitcherBucket(
        id: bucket.rawValue,
        label: bucket.label,
        singular: bucket.singular,
        cardGradient: bucket.gradient,
        vertical: nil
    )
}

private let groupBucketsById: [String: MomentSwitcherBucket] = Dictionary(
    uniqueKeysWithValues: groupBuckets.map { ($0.id, $0) }
)

/// Group spec — same geometry, group taxonomy and copy.
func groupMomentSwitcherSpec(allowDismiss: Bool = true) -> MomentSwitcherSpec {
    MomentSwitcherSpec(
        testTag: "group.moment.directory",
        buckets: groupBuckets,
        classify: { code in
            let id = groupMomentDirectoryBucket(code).rawValue
            return groupBucketsById[id] ?? groupBuckets[0]
        },
        memberLabel: { moment, bucket in
            let members = max(moment.participantCount, 0)
            return "\(members) member\(members == 1 ? "" : "s") · \(bucket.singular)"
        },
        emptyTitle: "No live group moments",
        emptyBody: "Start a moment for your group and its Story will build from what everyone shares.",
        emptyCta: "Create first moment",
        completedEmptyTitle: "No completed moments yet",
        completedEmptyBody: "When your group wraps, its Story lands here.",
        completedEmptyCta: "Create another moment",
        errorTitle: "Couldn't load moments",
        errorBody: "Your moments are safe — we just couldn't reach them. Check your connection and try again.",
        errorCode: "MOMENTS_FETCH_FAILED",
        footerTitle: "That's everything live",
        footerBody: "Spin up another shared experience when you're ready.",
        footerCta: "Create another moment",
        emptySubtitle: "Nothing live right now",
        searchingSubtitle: "Searching Ongoing",
        allowDismiss: allowDismiss,
        loadCompleted: {
            try await APIClient.shared.listGroupMoments(limit: 50, lifecycle: "completed")
        }
    )
}

// MARK: - Loading + filtering helpers

/// Moments shown on the Ongoing tab: live statuses plus the currently selected moment.
func ongoingSwitcherMoments(_ moments: [MomentSummary], selectedMomentId: String?) -> [MomentSummary] {
    moments.filter { $0.isActiveStatus || $0.momentId == selectedMomentId }
}

/// Completed-tab rows, defensively filtered to completed statuses.
func completedSwitcherMoments(_ completed: [MomentSummary]) -> [MomentSummary] {
    completed.filter(\.isCompletedStatus)
}

/// Query + bucket filter, applied identically on both tabs.
func filterSwitcherMoments(
    _ source: [MomentSummary],
    query: String,
    bucketId: String?,
    spec: MomentSwitcherSpec
) -> [MomentSummary] {
    let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
    return source.filter { moment in
        let matchesQuery = trimmed.isEmpty || moment.title.range(of: trimmed, options: .caseInsensitive) != nil
        let matchesBucket = bucketId == nil || spec.classify(moment.momentTypeCode).id == bucketId
        return matchesQuery && matchesBucket
    }
}
