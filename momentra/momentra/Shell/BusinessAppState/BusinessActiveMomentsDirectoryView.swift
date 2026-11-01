import SwiftUI

/// Lifecycle tab of the business moments directory.
enum BusinessDirectoryTab: String, CaseIterable {
    case ongoing
    case completed

    var label: String {
        switch self {
        case .ongoing: return "Ongoing"
        case .completed: return "Completed"
        }
    }
}

/// The five states the Figma `MomentSwitcher` covers (node 2678:37791).
enum BusinessDirectoryState {
    case ongoing
    case completed
    case empty
    case searchEmpty
    case loadError
}

/// Accent bucket used to group business moments. Presentation only — classification is unchanged.
struct BusinessDirectoryBucket: Identifiable, Equatable {
    let id: String
    let label: String
    let accent: BusinessAppStateAccent
}

func businessDirectoryBucketFor(_ momentTypeCode: String?) -> BusinessDirectoryBucket {
    let code = (momentTypeCode ?? "").uppercased()
    if code.contains("MARKETING") || code.contains("CAMPAIGN") {
        return BusinessDirectoryBucket(id: "marketing", label: "Marketing", accent: .marketing)
    }
    if code.contains("TRADING") || code.contains("WHOLESALE") {
        return BusinessDirectoryBucket(id: "trading", label: "Trading", accent: .trading)
    }
    if code.contains("DISTRIBUTION") || code.contains("DEALERSHIP") {
        return BusinessDirectoryBucket(id: "distribution", label: "Distribution", accent: .distribution)
    }
    if code.contains("RETAIL") || code.contains("STORE") {
        return BusinessDirectoryBucket(id: "retail", label: "Retail", accent: .retail)
    }
    if code.contains("RESTAURANT") || code.contains("FOOD") {
        return BusinessDirectoryBucket(id: "food", label: "Food", accent: .rst)
    }
    if code.contains("CONSUMER") || code.contains("SERVICE") {
        return BusinessDirectoryBucket(id: "service", label: "Service", accent: .services)
    }
    if code.contains("MANUFACTURING") || code.contains("PRODUCTION") || code.contains("MADE_TO_ORDER") {
        return BusinessDirectoryBucket(id: "manufacturing", label: "Manufacturing", accent: .mfg)
    }
    return BusinessDirectoryBucket(id: "trading", label: "Business", accent: .trading)
}

/// One category section in the directory list.
struct BusinessDirectoryGroup: Identifiable {
    let bucket: BusinessDirectoryBucket
    var items: [MomentSummary]
    var id: String { bucket.id }
}

/// Business Active Moments directory, rebuilt against Figma `MomentSwitcher` (node 2678:37791).
///
/// Distinct from the Group `GroupActiveMomentsDirectoryView`, which serves a different context.
///
/// Keep in lockstep with `BusinessActiveMomentsDirectory.kt` on Android.
struct BusinessActiveMomentsDirectoryView: View {
    let moments: [MomentSummary]
    let selectedMomentId: String?
    let onDismiss: () -> Void
    let onSelectMoment: (String) -> Void
    var onSelectCompletedMoment: (MomentSummary) -> Void = { _ in }
    var onOpenStory: (String) -> Void = { _ in }
    let onCreateMoment: () -> Void
    var initialCompletedTab: Bool = false
    /// Completed moments loaded by the caller; nil means not yet loaded.
    var completedMoments: [MomentSummary]? = nil
    var completedLoading: Bool = false
    var completedError: String? = nil
    var onRetryCompleted: () -> Void = {}
    var companyName: String = "Your business"

    @State private var tab: BusinessDirectoryTab
    @State private var query = ""
    @State private var bucketFilter: String?

    init(
        moments: [MomentSummary],
        selectedMomentId: String?,
        onDismiss: @escaping () -> Void,
        onSelectMoment: @escaping (String) -> Void,
        onSelectCompletedMoment: @escaping (MomentSummary) -> Void = { _ in },
        onOpenStory: @escaping (String) -> Void = { _ in },
        onCreateMoment: @escaping () -> Void,
        initialCompletedTab: Bool = false,
        completedMoments: [MomentSummary]? = nil,
        completedLoading: Bool = false,
        completedError: String? = nil,
        onRetryCompleted: @escaping () -> Void = {},
        companyName: String = "Your business"
    ) {
        self.moments = moments
        self.selectedMomentId = selectedMomentId
        self.onDismiss = onDismiss
        self.onSelectMoment = onSelectMoment
        self.onSelectCompletedMoment = onSelectCompletedMoment
        self.onOpenStory = onOpenStory
        self.onCreateMoment = onCreateMoment
        self.initialCompletedTab = initialCompletedTab
        self.completedMoments = completedMoments
        self.completedLoading = completedLoading
        self.completedError = completedError
        self.onRetryCompleted = onRetryCompleted
        self.companyName = companyName
        _tab = State(initialValue: initialCompletedTab ? .completed : .ongoing)
    }

    // MARK: Derived

    private var activeMoments: [MomentSummary] {
        moments.filter { !$0.isCompletedStatus }
    }

    private var source: [MomentSummary] {
        tab == .completed ? (completedMoments ?? []) : activeMoments
    }

    private var buckets: [BusinessDirectoryBucket] {
        var seen = Set<String>()
        var result: [BusinessDirectoryBucket] = []
        for moment in source {
            let bucket = businessDirectoryBucketFor(moment.momentTypeCode)
            if seen.insert(bucket.id).inserted { result.append(bucket) }
        }
        return result
    }

    private var filtered: [MomentSummary] {
        source.filter { moment in
            let matchesQuery = query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                || moment.title.range(of: query, options: .caseInsensitive) != nil
            let matchesBucket = bucketFilter == nil
                || businessDirectoryBucketFor(moment.momentTypeCode).id == bucketFilter
            return matchesQuery && matchesBucket
        }
    }

    private var grouped: [BusinessDirectoryGroup] {
        var order: [String] = []
        var map: [String: BusinessDirectoryGroup] = [:]
        for moment in filtered {
            let bucket = businessDirectoryBucketFor(moment.momentTypeCode)
            if map[bucket.id] == nil {
                order.append(bucket.id)
                map[bucket.id] = BusinessDirectoryGroup(bucket: bucket, items: [])
            }
            map[bucket.id]?.items.append(moment)
        }
        return order.compactMap { map[$0] }
    }

    private var state: BusinessDirectoryState {
        if completedError != nil, tab == .completed { return .loadError }
        if completedLoading, tab == .completed, (completedMoments ?? []).isEmpty { return .completed }
        if source.isEmpty { return .empty }
        if filtered.isEmpty { return .searchEmpty }
        if tab == .completed { return .completed }
        return .ongoing
    }

    private var primaryAccent: BusinessAppStateAccent {
        buckets.first?.accent ?? .trading
    }

    // MARK: Body

    var body: some View {
        ZStack {
            BusinessAppStateTokens.page.ignoresSafeArea()

            // Ambient wash — Figma draws four large blurred ellipses behind the content.
            GeometryReader { _ in
                ZStack {
                    DirectoryWash(color: primaryAccent.color)
                        .frame(maxHeight: .infinity, alignment: .top)
                        .offset(y: -80)
                    DirectoryWash(color: BusinessAppStateTokens.warning.opacity(0.18))
                        .frame(maxHeight: .infinity, alignment: .bottom)
                        .offset(y: -20)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
            .allowsHitTesting(false)

            ScrollView(showsIndicators: false) {
                VStack(spacing: 16) {
                    header
                    tabs
                    metrics
                    searchField

                    if !buckets.isEmpty {
                        chips
                    }

                    switch state {
                    case .empty:
                        emptyCard
                    case .searchEmpty:
                        searchEmptyCard
                    case .loadError:
                        loadErrorCard
                    case .ongoing, .completed:
                        groupedList
                    }

                    Spacer().frame(height: 8)
                }
                .padding(.horizontal, 24)
                .padding(.top, 16)
                .padding(.bottom, 32)
            }
        }
    }

    // MARK: Header

    private var header: some View {
        HStack(spacing: 12) {
            Circle()
                .fill(Color(hex: "#14121B"))
                .overlay(Circle().stroke(primaryAccent.color, lineWidth: 2))
                .overlay(
                    Text("M")
                        .font(.plusJakarta(size: 16, weight: .heavy))
                        .foregroundStyle(.white)
                )
                .frame(width: 40, height: 40)

            VStack(alignment: .leading, spacing: 2) {
                Text(companyName)
                    .font(.plusJakarta(size: 15, weight: .semibold))
                    .foregroundStyle(.white)
                    .lineLimit(1)
                Text(headerSubtitle)
                    .font(.plusJakarta(size: 12, weight: .medium))
                    .foregroundStyle(primaryAccent.color)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            AppStateIconButton(systemName: "xmark", accessibilityLabel: "Close", action: onDismiss)
        }
    }

    private var headerSubtitle: String {
        switch tab {
        case .ongoing:
            let count = activeMoments.count
            return "\(count) active moment\(count == 1 ? "" : "s")"
        case .completed:
            let count = (completedMoments ?? []).count
            return "\(count) completed moments"
        }
    }

    // MARK: Tabs

    private var tabs: some View {
        HStack(spacing: 4) {
            ForEach(BusinessDirectoryTab.allCases, id: \.self) { entry in
                Button {
                    tab = entry
                    bucketFilter = nil
                    query = ""
                } label: {
                    Text(entry.label)
                        .font(.plusJakarta(size: 13, weight: entry == tab ? Font.Weight.bold : Font.Weight.medium))
                        .foregroundStyle(entry == tab ? Color(hex: "#0E0E0E") : Color.white.opacity(0.75))
                        .frame(maxWidth: .infinity)
                        .frame(height: 40)
                        .background(
                            Capsule().fill(
                                entry == tab
                                    ? AnyShapeStyle(
                                        LinearGradient(
                                            colors: [
                                                primaryAccent.color.opacity(0.85),
                                                primaryAccent.color.opacity(0.55),
                                            ],
                                            startPoint: .leading,
                                            endPoint: .trailing
                                        )
                                    )
                                    : AnyShapeStyle(Color.clear)
                            )
                        )
                        .overlay(
                            Capsule().stroke(entry == tab ? primaryAccent.color : .clear, lineWidth: 1)
                        )
                }
                .buttonStyle(.plain)
            }
        }
        .padding(5)
        .background(Capsule().fill(Color.white.opacity(0.06)))
        .overlay(Capsule().stroke(Color.white.opacity(0.12), lineWidth: 1))
    }

    // MARK: Metrics

    private var metrics: some View {
        HStack(spacing: 12) {
            metric(value: activeMoments.count, label: "Active now", accent: primaryAccent.color, strokeAlpha: 1)
            metric(value: buckets.count, label: "Categories", accent: BusinessAppStateTokens.warning, strokeAlpha: 0.28)
        }
    }

    private func metric(value: Int, label: String, accent: Color, strokeAlpha: Double) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text("\(value)")
                .font(.plusJakarta(size: 24, weight: .heavy))
                .foregroundStyle(.white)
            Text(label)
                .font(.plusJakarta(size: 12, weight: .medium))
                .foregroundStyle(Color.white.opacity(0.70))
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: BusinessAppStateTokens.cardRadius)
                .fill(Color.white.opacity(0.08))
        )
        .overlay(
            RoundedRectangle(cornerRadius: BusinessAppStateTokens.cardRadius)
                .stroke(accent.opacity(strokeAlpha), lineWidth: 1)
        )
    }

    // MARK: Search

    private var searchField: some View {
        HStack(spacing: 10) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 15, weight: .medium))
                .foregroundStyle(Color.white.opacity(0.60))
            TextField(
                "",
                text: $query,
                prompt: Text("Find a moment").foregroundStyle(Color.white.opacity(0.55))
            )
            .font(.plusJakarta(size: 14))
            .foregroundStyle(.white)
            .textInputAutocapitalization(.never)
            .autocorrectionDisabled()
            .tint(.white)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 14)
        .background(
            RoundedRectangle(cornerRadius: BusinessAppStateTokens.cardRadius)
                .fill(Color.white.opacity(0.08))
        )
        .overlay(
            RoundedRectangle(cornerRadius: BusinessAppStateTokens.cardRadius)
                .stroke(Color.white.opacity(0.14), lineWidth: 1)
        )
    }

    // MARK: Chips

    private var chips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                chip(label: "All", selected: bucketFilter == nil) { bucketFilter = nil }
                ForEach(buckets) { bucket in
                    chip(label: bucket.label, selected: bucketFilter == bucket.id) {
                        bucketFilter = bucket.id
                    }
                }
            }
        }
    }

    private func chip(label: String, selected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label)
                .font(.plusJakarta(size: 13, weight: .semibold))
                .foregroundStyle(.white)
                .padding(.horizontal, 16)
                .padding(.vertical, 9)
                .background(
                    Capsule().fill(
                        selected
                            ? AnyShapeStyle(
                                LinearGradient(
                                    colors: [BusinessAppStateTokens.ctaStart, BusinessAppStateTokens.ctaEnd],
                                    startPoint: .leading,
                                    endPoint: .trailing
                                )
                            )
                            : AnyShapeStyle(Color.white.opacity(0.12))
                    )
                )
                .overlay(
                    Capsule().stroke(selected ? .clear : Color.white.opacity(0.22), lineWidth: 1)
                )
        }
        .buttonStyle(.plain)
    }

    // MARK: Grouped list

    private var groupedList: some View {
        VStack(alignment: .leading, spacing: 12) {
            ForEach(grouped) { group in
                Text("\(group.bucket.label.uppercased()) · \(group.items.count)")
                    .font(.plusJakarta(size: 12, weight: .heavy))
                    .tracking(2)
                    .foregroundStyle(group.bucket.accent.color)
                    .frame(maxWidth: .infinity, alignment: .leading)

                ForEach(group.items) { moment in
                    momentCard(moment, bucket: group.bucket)
                }
            }

            if tab == .ongoing {
                VStack(spacing: 4) {
                    Text("That's everything live")
                        .font(.plusJakarta(size: 15, weight: .bold))
                        .foregroundStyle(.white)
                    Text("Spin up another business moment when you're ready.")
                        .font(.plusJakarta(size: 12, weight: .medium))
                        .foregroundStyle(Color.white.opacity(0.70))
                        .multilineTextAlignment(.center)
                }
                .frame(maxWidth: .infinity)
                .padding(.top, 8)

                AppStatePrimaryButton(label: "Create another moment", action: onCreateMoment)
            }
        }
    }

    private func momentCard(_ moment: MomentSummary, bucket: BusinessDirectoryBucket) -> some View {
        return Button {
            if moment.isCompletedStatus {
                onSelectCompletedMoment(moment)
            } else {
                onSelectMoment(moment.momentId)
            }
        } label: {
            VStack(alignment: .leading, spacing: 6) {
                Text(moment.title)
                    .font(.plusJakarta(size: 26, weight: .heavy))
                    .foregroundStyle(.white)
                    .multilineTextAlignment(.leading)

                Text("\(max(moment.participantCount, 1)) member\(moment.participantCount == 1 ? "" : "s") · \(bucket.label)")
                    .font(.plusJakarta(size: 13, weight: .semibold))
                    .foregroundStyle(.white)

                Text(moment.isCompletedStatus ? "Tap to relive this moment" : "Tap Story to relive this moment")
                    .font(.plusJakarta(size: 12, weight: .medium))
                    .foregroundStyle(Color.white.opacity(0.85))
                    .contentShape(Rectangle())
                    .onTapGesture { onOpenStory(moment.momentId) }
            }
            .padding(19)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: 28)
                    .fill(
                        LinearGradient(
                            colors: [
                                Color(hex: "#312E81"),
                                Color(hex: "#0E7C66"),
                                bucket.accent.color,
                            ],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
            )
            .overlay(
                RoundedRectangle(cornerRadius: 28)
                    .stroke(
                        bucket.accent.color.opacity(moment.momentId == selectedMomentId ? 1 : 0.6),
                        lineWidth: 1
                    )
            )
        }
        .buttonStyle(.plain)
    }

    // MARK: State cards

    private var emptyCard: some View {
        stateCard(accent: primaryAccent) {
            AppStateMarkRing(accent: primaryAccent.color, icon: .moments, size: 136)
            AppStateTitle(text: tab == .completed ? "No completed moments" : "No live business moments")
            AppStateBodyText(
                text: tab == .completed
                    ? "Wrapped moments and their Stories appear here once a moment is completed."
                    : "Start a moment for your business and its Story will build from verified activity."
            )
            AppStatePrimaryButton(
                label: tab == .completed ? "Start a moment" : "Create first moment",
                action: onCreateMoment
            )
        }
    }

    private var searchEmptyCard: some View {
        stateCard(accent: primaryAccent) {
            AppStateMarkRing(accent: primaryAccent.color, icon: .check, size: 124)
            AppStateTitle(text: "No moments match")
            AppStateBodyText(
                text: "Nothing in \(tab == .completed ? "Completed" : "Ongoing") matches "
                    + "“\(query)”. Try another word or clear filters."
            )
            AppStateSecondaryButton(label: "Clear search") {
                query = ""
                bucketFilter = nil
            }
        }
    }

    private var loadErrorCard: some View {
        stateCard(accent: primaryAccent, error: true) {
            AppStateMarkRing(accent: BusinessAppStateTokens.errorRing, icon: .alert, size: 136)
            AppStateTitle(text: "Couldn't load moments")
            AppStateBodyText(text: "Your moments are safe — we just couldn't reach them. Check your connection and try again.")
            AppStateErrorCodePill(code: "MOMENTS_FETCH_FAILED", accent: BusinessAppStateTokens.errorRing)
            AppStatePrimaryButton(label: "Retry", action: onRetryCompleted)
        }
    }

    private func stateCard<Content: View>(
        accent: BusinessAppStateAccent,
        error: Bool = false,
        @ViewBuilder content: () -> Content
    ) -> some View {
        VStack(spacing: BusinessAppStateTokens.bodyGap) {
            content()
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 22)
        .frame(maxWidth: .infinity)
        .background(
            RoundedRectangle(cornerRadius: 32)
                .fill(Color.white.opacity(0.08))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 32)
                .stroke(
                    error ? BusinessAppStateTokens.errorRing.opacity(0.5) : accent.color.opacity(0.35),
                    lineWidth: 1
                )
        )
    }
}

private struct DirectoryWash: View {
    let color: Color

    var body: some View {
        Circle()
            .fill(
                RadialGradient(
                    colors: [color.opacity(0.16), .clear],
                    center: .center,
                    startRadius: 0,
                    endRadius: 180
                )
            )
            .frame(width: 360, height: 360)
            .allowsHitTesting(false)
    }
}
