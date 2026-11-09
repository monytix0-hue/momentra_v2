import SwiftUI

/// One bucket section under its `BUCKET · N` eyebrow.
private struct MomentSwitcherSection: Identifiable {
    let bucket: MomentSwitcherBucket
    let items: [MomentSummary]

    var id: String { bucket.id }
}

/// The shared Group + Business moment switcher, built against Figma `2678:37791`.
///
/// States (`2678:37481` Ongoing, `2678:37561` Completed, `2678:37634` Empty, `2678:37715`
/// SearchEmpty, `2678:37790` LoadError) are resolved by `resolveMomentSwitcherState`; everything
/// that differs between Group and Business arrives through `MomentSwitcherSpec`.
///
/// Keep in lockstep with `MomentSwitcherDirectory.kt` on Android.
///
/// Two intentional corrections to the Figma source are marked `Figma deviation` below.
struct MomentSwitcherDirectoryView: View {
    let spec: MomentSwitcherSpec
    let moments: [MomentSummary]
    let selectedMomentId: String?
    let onDismiss: () -> Void
    let onSelectMoment: (String) -> Void
    var onSelectCompletedMoment: (MomentSummary) -> Void = { _ in }
    var onOpenStory: (String) -> Void = { _ in }
    let onCreateMoment: () -> Void
    var initialCompletedTab: Bool = false
    /// Test/gallery seam: when non-nil the caller owns Completed-tab loading.
    var completedMoments: [MomentSummary]? = nil
    var completedLoading: Bool? = nil
    var completedError: String? = nil
    /// Test/preview seam: seeds the search field so SearchEmpty is reachable without typing.
    var initialQuery: String = ""

    @State private var tab: MomentSwitcherTab
    @State private var query: String
    @State private var bucketFilter: String?
    @State private var fetchedCompleted: [MomentSummary] = []
    @State private var fetching = false
    @State private var fetchError: String?
    @State private var reloadToken = 0

    init(
        spec: MomentSwitcherSpec,
        moments: [MomentSummary],
        selectedMomentId: String?,
        onDismiss: @escaping () -> Void,
        onSelectMoment: @escaping (String) -> Void,
        onSelectCompletedMoment: @escaping (MomentSummary) -> Void = { _ in },
        onOpenStory: @escaping (String) -> Void = { _ in },
        onCreateMoment: @escaping () -> Void,
        initialCompletedTab: Bool = false,
        completedMoments: [MomentSummary]? = nil,
        completedLoading: Bool? = nil,
        completedError: String? = nil,
        initialQuery: String = ""
    ) {
        self.spec = spec
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
        self.initialQuery = initialQuery
        _tab = State(initialValue: initialCompletedTab ? .completed : .ongoing)
        _query = State(initialValue: initialQuery)
    }

    // MARK: Derived

    private var activeMoments: [MomentSummary] {
        ongoingSwitcherMoments(moments, selectedMomentId: selectedMomentId)
    }

    private var completed: [MomentSummary] {
        completedMoments ?? fetchedCompleted
    }

    private var source: [MomentSummary] {
        tab == .completed ? completed : activeMoments
    }

    private var filtered: [MomentSummary] {
        filterSwitcherMoments(source, query: query, bucketId: bucketFilter, spec: spec)
    }

    private var presentBuckets: [MomentSwitcherBucket] {
        var seen = Set<String>()
        var result: [MomentSwitcherBucket] = []
        for moment in source {
            let bucket = spec.classify(moment.momentTypeCode)
            if seen.insert(bucket.id).inserted { result.append(bucket) }
        }
        return result
    }

    private var grouped: [MomentSwitcherSection] {
        var order: [String] = []
        var bucketsById: [String: MomentSwitcherBucket] = [:]
        var itemsById: [String: [MomentSummary]] = [:]
        for moment in filtered {
            let bucket = spec.classify(moment.momentTypeCode)
            if bucketsById[bucket.id] == nil {
                order.append(bucket.id)
                bucketsById[bucket.id] = bucket
                itemsById[bucket.id] = []
            }
            itemsById[bucket.id]?.append(moment)
        }
        return order.compactMap { id in
            guard let bucket = bucketsById[id], let items = itemsById[id] else { return nil }
            return MomentSwitcherSection(bucket: bucket, items: items)
        }
    }

    private var state: MomentSwitcherState {
        resolveMomentSwitcherState(
            tab: tab,
            sourceCount: source.count,
            filteredCount: filtered.count,
            completedLoading: completedLoading ?? fetching,
            completedError: completedError ?? fetchError
        )
    }

    private var hasData: Bool {
        state == .ongoing || state == .completed
    }

    private var chromeAlpha: Double {
        switch state {
        case .empty: return MomentSwitcherTokens.mutedEmpty
        case .loadError: return MomentSwitcherTokens.mutedError
        default: return 1
        }
    }

    private var chromeEnabled: Bool {
        hasData || state == .searchEmpty
    }

    // MARK: Body

    var body: some View {
        ZStack(alignment: .topLeading) {
            MomentSwitcherTokens.page.ignoresSafeArea()
            wash

            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: MomentSwitcherTokens.bodyGap) {
                    header

                    // Figma deviation: the source mutes tabs + search (55% in Empty, 45% in
                    // LoadError) while leaving them hit-testable. We mute *and* disable, matching
                    // `MomentSwitcherDirectory.kt`.
                    VStack(spacing: 12) {
                        tabs
                        searchField
                    }
                    .opacity(chromeAlpha)
                    .allowsHitTesting(chromeEnabled)

                    if hasData {
                        metrics
                        if !presentBuckets.isEmpty {
                            chips
                        }
                    }

                    stateBody(for: state)

                    Spacer().frame(height: 8)
                }
                .padding(.horizontal, MomentSwitcherTokens.screenPadding)
                .padding(.top, 16)
                .padding(.bottom, 32)
            }
        }
        .accessibilityIdentifier(spec.testTag)
        .task(id: reloadToken) { await loadCompletedIfNeeded() }
        .onChange(of: tab) { _, _ in Task { await loadCompletedIfNeeded() } }
    }

    /// The 360pt emerald wash, blurred 50 and pegged to (-120, -80).
    private var wash: some View {
        RadialGradient(
            colors: [
                MomentSwitcherTokens.wash.opacity(MomentSwitcherTokens.washAlpha),
                .clear,
            ],
            center: .center,
            startRadius: 0,
            endRadius: MomentSwitcherTokens.washSize / 2
        )
        .frame(width: MomentSwitcherTokens.washSize, height: MomentSwitcherTokens.washSize)
        .blur(radius: MomentSwitcherTokens.washBlur)
        .offset(x: MomentSwitcherTokens.washOffset.width, y: MomentSwitcherTokens.washOffset.height)
        .allowsHitTesting(false)
    }

    // MARK: Header

    private var header: some View {
        HStack(spacing: 12) {
            Image("ShellTopbarLogo")
                .resizable()
                .scaledToFill()
                .frame(width: MomentSwitcherTokens.avatarSize, height: MomentSwitcherTokens.avatarSize)
                .clipShape(Circle())

            VStack(alignment: .leading, spacing: 2) {
                Text(state == .loadError ? "Moments" : headline)
                    .font(.plusJakarta(size: MomentSwitcherTokens.headlineSize, weight: .heavy))
                    .foregroundStyle(MomentSwitcherTokens.text)
                    .lineLimit(1)
                Text(momentSwitcherSubtitle(spec: spec, state: state))
                    .font(.plusJakarta(size: MomentSwitcherTokens.subtitleSize, weight: .medium))
                    .foregroundStyle(MomentSwitcherTokens.textMuted)
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            if spec.allowDismiss {
                Button(action: onDismiss) {
                    Image(systemName: "xmark")
                        .font(.system(size: MomentSwitcherTokens.closeGlyphSize, weight: .semibold))
                        .foregroundStyle(MomentSwitcherTokens.text)
                        .frame(
                            width: MomentSwitcherTokens.closeSize,
                            height: MomentSwitcherTokens.closeSize
                        )
                        .background(MomentSwitcherTokens.iconButtonFill, in: Capsule())
                        .overlay(
                            Capsule().strokeBorder(MomentSwitcherTokens.iconButtonStroke, lineWidth: 1)
                        )
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Close")
            }
        }
    }

    private var headline: String {
        momentSwitcherHeadline(
            count: tab == .completed ? completed.count : activeMoments.count,
            tab: tab
        )
    }

    // MARK: Tabs

    private var tabs: some View {
        HStack(spacing: 4) {
            ForEach(MomentSwitcherTab.allCases) { entry in
                let selected = entry == tab
                Button {
                    tab = entry
                    clearFilters()
                } label: {
                    Text(entry.label)
                        .font(.plusJakarta(
                            size: MomentSwitcherTokens.tabLabelSize,
                            weight: selected ? .bold : .semibold
                        ))
                        .foregroundStyle(selected ? MomentSwitcherTokens.text : MomentSwitcherTokens.tabIdle)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .background {
                            if selected {
                                LinearGradient(
                                    colors: [MomentSwitcherTokens.tabStart, MomentSwitcherTokens.tabEnd],
                                    startPoint: .top,
                                    endPoint: .bottom
                                )
                                .clipShape(Capsule())
                                .overlay(Capsule().strokeBorder(MomentSwitcherTokens.accent, lineWidth: 1))
                            }
                        }
                }
                .buttonStyle(.plain)
                .accessibilityAddTraits(selected ? .isSelected : [])
            }
        }
        .padding(4)
        .background(MomentSwitcherTokens.sheetFill, in: Capsule())
        .overlay(Capsule().strokeBorder(MomentSwitcherTokens.sheetStroke, lineWidth: 1))
    }

    // MARK: Metrics

    private var metrics: some View {
        HStack(spacing: 12) {
            metric(
                value: "\(activeMoments.count)",
                label: "Active now",
                valueColor: MomentSwitcherTokens.accent,
                stroke: MomentSwitcherTokens.accent
            )
            metric(
                value: "\(presentBuckets.count)",
                label: "Categories",
                valueColor: MomentSwitcherTokens.warm,
                stroke: MomentSwitcherTokens.warmStroke
            )
        }
    }

    private func metric(value: String, label: String, valueColor: Color, stroke: Color) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(value)
                .font(.plusJakarta(size: MomentSwitcherTokens.metricValueSize, weight: .heavy))
                .foregroundStyle(valueColor)
            Text(label)
                .font(.plusJakarta(size: MomentSwitcherTokens.metricLabelSize, weight: .semibold))
                .foregroundStyle(MomentSwitcherTokens.textMuted)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 18)
        .padding(.vertical, 16)
        .background(
            RoundedRectangle(cornerRadius: MomentSwitcherTokens.metricRadius, style: .continuous)
                .fill(MomentSwitcherTokens.glassFill)
        )
        .overlay(
            RoundedRectangle(cornerRadius: MomentSwitcherTokens.metricRadius, style: .continuous)
                .strokeBorder(stroke, lineWidth: 1)
        )
    }

    // MARK: Search

    private var searchField: some View {
        HStack(spacing: 10) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: MomentSwitcherTokens.searchIconSize, weight: .regular))
                .foregroundStyle(MomentSwitcherTokens.textSubtle)

            TextField(
                "",
                text: $query,
                prompt: Text("Find a moment")
                    .foregroundStyle(MomentSwitcherTokens.textSubtle)
            )
            .textFieldStyle(.plain)
            .font(.plusJakarta(size: MomentSwitcherTokens.searchTextSize, weight: .semibold))
            .foregroundStyle(MomentSwitcherTokens.text)
            .autocorrectionDisabled()
            .textInputAutocapitalization(.never)

            if !query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                // SearchEmpty frame `2678:37662`.
                Button(action: clearFilters) {
                    Text("✕ Clear")
                        .font(.plusJakarta(size: MomentSwitcherTokens.clearPillSize, weight: .bold))
                        .foregroundStyle(MomentSwitcherTokens.text)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 4)
                        .background(MomentSwitcherTokens.clearPillFill, in: Capsule())
                }
                .buttonStyle(.plain)
            } else {
                // Figma deviation: the source draws the Filter pill with an emerald fill *and* an
                // emerald label/icon, which renders invisible. White label on emerald.
                HStack(spacing: 6) {
                    Image(systemName: "line.3.horizontal")
                        .font(.system(size: MomentSwitcherTokens.filterIconSize, weight: .bold))
                        .foregroundStyle(.white)
                    Text("Filter")
                        .font(.plusJakarta(size: MomentSwitcherTokens.filterLabelSize, weight: .bold))
                        .foregroundStyle(.white)
                }
                .padding(.horizontal, 10)
                .padding(.vertical, 4)
                .background(MomentSwitcherTokens.accent, in: Capsule())
            }
        }
        .padding(.horizontal, 18)
        .padding(.vertical, 12)
        .background(
            RoundedRectangle(cornerRadius: MomentSwitcherTokens.searchRadius, style: .continuous)
                .fill(MomentSwitcherTokens.glassFill)
        )
        .overlay(
            RoundedRectangle(cornerRadius: MomentSwitcherTokens.searchRadius, style: .continuous)
                .strokeBorder(MomentSwitcherTokens.glassStroke, lineWidth: 1)
        )
    }

    // MARK: Chips

    private var chips: some View {
        MomentSwitcherChipFlowLayout(spacing: 8) {
            chip(label: "All", selected: bucketFilter == nil) {
                bucketFilter = nil
            }
            ForEach(presentBuckets) { bucket in
                chip(label: bucket.label, selected: bucketFilter == bucket.id) {
                    bucketFilter = bucket.id
                }
            }
        }
    }

    private func chip(label: String, selected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label)
                .font(.plusJakarta(size: MomentSwitcherTokens.chipLabelSize, weight: .semibold))
                .foregroundStyle(MomentSwitcherTokens.text)
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background {
                    if selected {
                        LinearGradient(
                            colors: [MomentSwitcherTokens.emberStart, MomentSwitcherTokens.emberEnd],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                        .clipShape(Capsule())
                    } else {
                        Capsule()
                            .fill(MomentSwitcherTokens.chipFill)
                            .overlay(Capsule().strokeBorder(MomentSwitcherTokens.chipStroke, lineWidth: 1))
                    }
                }
        }
        .buttonStyle(.plain)
    }

    // MARK: State body

    @ViewBuilder
    private func stateBody(for state: MomentSwitcherState) -> some View {
        switch state {
        case .ongoing, .completed:
            VStack(alignment: .leading, spacing: 12) {
                ForEach(grouped) { section in
                    Text("\(section.bucket.label.uppercased()) · \(section.items.count)")
                        .font(.plusJakarta(size: MomentSwitcherTokens.eyebrowSize, weight: .heavy))
                        .tracking(MomentSwitcherTokens.eyebrowTracking)
                        .foregroundStyle(MomentSwitcherTokens.accent)

                    ForEach(section.items) { moment in
                        liveCard(moment, bucket: section.bucket)
                    }
                }

                if tab == .ongoing {
                    footer
                }
            }

        case .empty:
            stateCard(
                glow: MomentSwitcherTokens.warm,
                ring: MomentSwitcherTokens.warm,
                badgeColor: MomentSwitcherTokens.warm,
                badgeGlyph: "+",
                title: spec.emptyTitle,
                body: spec.emptyBody,
                primaryLabel: spec.emptyCta,
                onPrimary: onCreateMoment
            )

        case .searchEmpty:
            stateCard(
                glow: MomentSwitcherTokens.accent,
                ring: MomentSwitcherTokens.accent,
                badgeColor: MomentSwitcherTokens.accent,
                badgeGlyph: nil,
                title: "No moments match",
                body: "Nothing in \(tab == .completed ? "Completed" : "Ongoing") matches "
                    + "\u{201C}\(query.trimmingCharacters(in: .whitespacesAndNewlines))\u{201D}. "
                    + "Try another word or clear filters.",
                primaryLabel: "Clear search",
                onPrimary: clearFilters,
                heroSize: MomentSwitcherTokens.heroSearchSize,
                secondary: true
            )

        case .loadError:
            stateCard(
                glow: MomentSwitcherTokens.error,
                ring: MomentSwitcherTokens.error,
                badgeColor: MomentSwitcherTokens.error,
                badgeGlyph: "!",
                title: spec.errorTitle,
                body: spec.errorBody,
                primaryLabel: "Retry",
                onPrimary: retry,
                errorCode: spec.errorCode
            )

        case .completedEmpty:
            stateCard(
                glow: MomentSwitcherTokens.accent,
                ring: MomentSwitcherTokens.accent,
                badgeColor: MomentSwitcherTokens.accent,
                badgeGlyph: "✓",
                title: spec.completedEmptyTitle,
                body: spec.completedEmptyBody,
                primaryLabel: spec.completedEmptyCta,
                onPrimary: onCreateMoment
            )

        case .loading:
            loadingCard
        }
    }

    // MARK: Live card

    private func liveCard(_ moment: MomentSummary, bucket: MomentSwitcherBucket) -> some View {
        let radius = MomentSwitcherTokens.liveCardRadius
        let shape = RoundedRectangle(cornerRadius: radius, style: .continuous)
        let completedMoment = moment.isCompletedStatus

        return VStack(alignment: .leading, spacing: 14) {
            HStack {
                statusChip(completed: completedMoment)
                Spacer(minLength: 8)
                storyPill(for: moment)
            }

            Text(moment.title)
                .font(.plusJakarta(size: MomentSwitcherTokens.cardTitleSize, weight: .heavy))
                .foregroundStyle(MomentSwitcherTokens.text)
                .lineLimit(2)
                .multilineTextAlignment(.leading)

            Text(spec.memberLabel(moment, bucket))
                .font(.plusJakarta(size: MomentSwitcherTokens.cardMetaSize, weight: .semibold))
                .foregroundStyle(MomentSwitcherTokens.textOnCard)
                .lineLimit(1)

            Text(completedMoment ? "Tap to relive this moment" : "Tap Story to relive this moment")
                .font(.plusJakarta(size: MomentSwitcherTokens.cardMicroSize, weight: .medium))
                .foregroundStyle(MomentSwitcherTokens.textOnCard)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 20)
        .padding(.top, 18)
        .padding(.bottom, 20)
        .background(shape.fill(liveCardGradient(bucket)))
        .overlay(shape.strokeBorder(MomentSwitcherTokens.accent, lineWidth: 1))
        .shadow(
            color: MomentSwitcherTokens.accent.opacity(0.25),
            radius: MomentSwitcherTokens.liveCardShadowRadius,
            x: 0,
            y: MomentSwitcherTokens.liveCardShadowY
        )
        .contentShape(shape)
        .onTapGesture { openMoment(moment) }
    }

    private func liveCardGradient(_ bucket: MomentSwitcherBucket) -> LinearGradient {
        let colors = bucket.cardGradient
        if colors.count >= 3 {
            return LinearGradient(
                stops: [
                    .init(color: colors[0], location: 0),
                    .init(color: colors[1], location: 0.55),
                    .init(color: colors[2], location: 1),
                ],
                startPoint: .top,
                endPoint: .bottom
            )
        }
        return LinearGradient(colors: colors, startPoint: .topLeading, endPoint: .bottomTrailing)
    }

    private func statusChip(completed: Bool) -> some View {
        HStack(spacing: 6) {
            Circle()
                .fill(completed ? MomentSwitcherTokens.warm : MomentSwitcherTokens.liveDot)
                .frame(width: MomentSwitcherTokens.liveDotSize, height: MomentSwitcherTokens.liveDotSize)
            Text(completed ? "WRAPPED" : "LIVE NOW")
                .font(.plusJakarta(size: MomentSwitcherTokens.liveChipSize, weight: .heavy))
                .tracking(MomentSwitcherTokens.liveChipTracking)
                .foregroundStyle(completed ? MomentSwitcherTokens.warm : MomentSwitcherTokens.liveDot)
        }
        .padding(.leading, 10)
        .padding(.trailing, 12)
        .padding(.vertical, 6)
        .background(MomentSwitcherTokens.liveChipFill, in: Capsule())
        .overlay(Capsule().strokeBorder(MomentSwitcherTokens.liveChipStroke, lineWidth: 1))
    }

    private func storyPill(for moment: MomentSummary) -> some View {
        Button {
            onOpenStory(moment.momentId)
        } label: {
            HStack(spacing: 8) {
                Image("ShellTopbarLogo")
                    .resizable()
                    .scaledToFill()
                    .frame(
                        width: MomentSwitcherTokens.storyAvatarSize,
                        height: MomentSwitcherTokens.storyAvatarSize
                    )
                    .clipShape(Circle())
                Text("Story")
                    .font(.plusJakarta(size: MomentSwitcherTokens.storyLabelSize, weight: .heavy))
                    .foregroundStyle(MomentSwitcherTokens.text)
                Text("▶")
                    .font(.plusJakarta(size: MomentSwitcherTokens.storyGlyphSize, weight: .bold))
                    .foregroundStyle(MomentSwitcherTokens.warm)
            }
            .padding(.leading, 6)
            .padding(.trailing, 16)
            .padding(.vertical, 6)
            .background(MomentSwitcherTokens.storyPillFill, in: Capsule())
            .overlay(Capsule().strokeBorder(MomentSwitcherTokens.warm, lineWidth: 2))
        }
        .buttonStyle(.plain)
        .accessibilityLabel("Open Story")
    }

    // MARK: Footer

    private var footer: some View {
        VStack(spacing: 16) {
            VStack(spacing: 4) {
                Text(spec.footerTitle)
                    .font(.plusJakarta(size: MomentSwitcherTokens.footerTitleSize, weight: .bold))
                    .foregroundStyle(MomentSwitcherTokens.text)
                Text(spec.footerBody)
                    .font(.plusJakarta(size: MomentSwitcherTokens.footerBodySize, weight: .medium))
                    .foregroundStyle(MomentSwitcherTokens.text.opacity(0.6))
                    .multilineTextAlignment(.center)
            }
            .frame(maxWidth: .infinity)
            .padding(.top, 6)

            primaryButton(label: spec.footerCta, action: onCreateMoment)
        }
    }

    // MARK: State cards

    private func stateCard(
        glow: Color,
        ring: Color,
        badgeColor: Color,
        badgeGlyph: String?,
        title: String,
        body: String,
        primaryLabel: String,
        onPrimary: @escaping () -> Void,
        heroSize: CGFloat = MomentSwitcherTokens.heroSize,
        errorCode: String? = nil,
        secondary: Bool = false
    ) -> some View {
        let radius = MomentSwitcherTokens.stateCardRadius
        let shape = RoundedRectangle(cornerRadius: radius, style: .continuous)

        return VStack(spacing: 12) {
            heroMark(
                glow: glow,
                ring: ring,
                badgeColor: badgeColor,
                badgeGlyph: badgeGlyph,
                size: heroSize
            )

            Text(title)
                .font(.plusJakarta(size: MomentSwitcherTokens.stateTitleSize, weight: .heavy))
                .foregroundStyle(MomentSwitcherTokens.text)
                .multilineTextAlignment(.center)

            Text(body)
                .font(.plusJakarta(size: MomentSwitcherTokens.stateBodySize, weight: .medium))
                .foregroundStyle(MomentSwitcherTokens.textMuted)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)

            if let errorCode {
                Text(errorCode)
                    .font(.plusJakarta(size: MomentSwitcherTokens.errorCodeSize, weight: .heavy))
                    .tracking(MomentSwitcherTokens.errorCodeTracking)
                    .foregroundStyle(MomentSwitcherTokens.error)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background(MomentSwitcherTokens.errorPillFill, in: Capsule())
                    .overlay(Capsule().strokeBorder(MomentSwitcherTokens.errorPillStroke, lineWidth: 1))
            }

            if secondary {
                secondaryButton(label: primaryLabel, action: onPrimary)
            } else {
                primaryButton(label: primaryLabel, action: onPrimary)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal, 22)
        .padding(.vertical, 26)
        .background(shape.fill(MomentSwitcherTokens.glassFill))
        .overlay(
            shape.strokeBorder(
                errorCode != nil ? MomentSwitcherTokens.errorStroke : MomentSwitcherTokens.glassStroke,
                lineWidth: 1
            )
        )
    }

    /// Hero mark: 136pt (124 in SearchEmpty) with a halo, a 3pt ring, the 96pt brand mark and a
    /// 40pt badge. Figma `hero · mark ring` in all four state cards.
    private func heroMark(
        glow: Color,
        ring: Color,
        badgeColor: Color,
        badgeGlyph: String?,
        size: CGFloat
    ) -> some View {
        let search = size == MomentSwitcherTokens.heroSearchSize
        let ringSize = search
            ? MomentSwitcherTokens.heroSearchRingSize
            : MomentSwitcherTokens.heroRingSize
        let markSize = search
            ? MomentSwitcherTokens.heroSearchMarkSize
            : MomentSwitcherTokens.heroMarkSize
        let badgeInset = search
            ? MomentSwitcherTokens.heroSearchBadgeInset
            : MomentSwitcherTokens.heroBadgeInset

        return ZStack(alignment: .topLeading) {
            Circle()
                .fill(
                    RadialGradient(
                        colors: [glow.opacity(0.45), .clear],
                        center: .center,
                        startRadius: 0,
                        endRadius: size / 2
                    )
                )
                .frame(width: size, height: size)
                .blur(radius: 18)

            Circle()
                .stroke(ring.opacity(0.85), lineWidth: 3)
                .frame(width: ringSize, height: ringSize)
                .offset(x: MomentSwitcherTokens.heroInset, y: MomentSwitcherTokens.heroInset)

            Image("ShellTopbarLogo")
                .resizable()
                .scaledToFill()
                .frame(width: markSize, height: markSize)
                .clipShape(Circle())
                .offset(x: MomentSwitcherTokens.heroMarkInset, y: MomentSwitcherTokens.heroMarkInset)

            badge(glyph: badgeGlyph, color: badgeColor)
                .offset(x: badgeInset, y: badgeInset)
        }
        .frame(width: size, height: size)
    }

    private func badge(glyph: String?, color: Color) -> some View {
        ZStack {
            Circle().fill(color)
            Circle()
                .stroke(MomentSwitcherTokens.page, lineWidth: MomentSwitcherTokens.heroBadgeBorder)
            if let glyph {
                Text(glyph)
                    .font(.plusJakarta(size: MomentSwitcherTokens.heroBadgeGlyphSize, weight: .heavy))
                    .foregroundStyle(.white)
            } else {
                Image(systemName: "magnifyingglass")
                    .font(.system(size: 19, weight: .semibold))
                    .foregroundStyle(.white)
            }
        }
        .frame(width: MomentSwitcherTokens.heroBadgeSize, height: MomentSwitcherTokens.heroBadgeSize)
    }

    // MARK: Buttons

    private func primaryButton(label: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label.uppercased())
                .font(.plusJakarta(size: MomentSwitcherTokens.ctaLabelSize, weight: .heavy))
                .tracking(MomentSwitcherTokens.ctaTracking)
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 16)
                .background(
                    LinearGradient(
                        colors: [MomentSwitcherTokens.emberStart, MomentSwitcherTokens.emberEnd],
                        startPoint: .top,
                        endPoint: .bottom
                    ),
                    in: Capsule()
                )
                .shadow(
                    color: MomentSwitcherTokens.emberStart.opacity(0.33),
                    radius: MomentSwitcherTokens.ctaShadowRadius,
                    x: 0,
                    y: MomentSwitcherTokens.ctaShadowY
                )
        }
        .buttonStyle(.plain)
    }

    /// Secondary CTA: white 8% fill, white 22% border, capsule (SearchEmpty `2678:37682`).
    private func secondaryButton(label: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label)
                .font(.plusJakarta(size: MomentSwitcherTokens.ctaLabelSize, weight: .bold))
                .foregroundStyle(MomentSwitcherTokens.text)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(MomentSwitcherTokens.glassFill, in: Capsule())
                .overlay(Capsule().strokeBorder(MomentSwitcherTokens.chipStroke, lineWidth: 1))
        }
        .buttonStyle(.plain)
    }

    private var loadingCard: some View {
        let radius = MomentSwitcherTokens.stateCardRadius
        let shape = RoundedRectangle(cornerRadius: radius, style: .continuous)
        return VStack(spacing: 16) {
            ProgressView()
                .progressViewStyle(.circular)
                .tint(MomentSwitcherTokens.accent)
            Text("Loading moments…")
                .font(.plusJakarta(size: MomentSwitcherTokens.stateBodySize, weight: .medium))
                .foregroundStyle(MomentSwitcherTokens.textMuted)
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal, 22)
        .padding(.vertical, 32)
        .background(shape.fill(MomentSwitcherTokens.glassFill))
        .overlay(shape.strokeBorder(MomentSwitcherTokens.glassStroke, lineWidth: 1))
    }

    // MARK: Actions

    private func clearFilters() {
        query = ""
        bucketFilter = nil
    }

    private func retry() {
        guard completedMoments == nil else { return }
        reloadToken += 1
    }

    private func openMoment(_ moment: MomentSummary) {
        if moment.isCompletedStatus {
            onSelectCompletedMoment(moment)
        } else {
            onSelectMoment(moment.momentId)
        }
        onDismiss()
    }

    /// Real Completed-tab loading. The pre-existing views left this permanently empty and made
    /// Retry a no-op; the spec now carries the fetch so Group and Business both get real history.
    @MainActor
    private func loadCompletedIfNeeded() async {
        guard tab == .completed, completedMoments == nil, !fetching else { return }
        fetching = true
        fetchError = nil
        defer { fetching = false }
        do {
            fetchedCompleted = completedSwitcherMoments(try await spec.loadCompleted())
        } catch {
            fetchError = error.localizedDescription
            fetchedCompleted = []
        }
    }
}

/// Minimal wrapping layout for the switcher's chip row.
///
/// Named uniquely — `FlowLayout` already exists for the money quick-add sheets.
struct MomentSwitcherChipFlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let maxWidth = proposal.width ?? .infinity
        var rowWidth: CGFloat = 0
        var rowHeight: CGFloat = 0
        var totalHeight: CGFloat = 0
        var maxRowWidth: CGFloat = 0

        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if rowWidth > 0, rowWidth + spacing + size.width > maxWidth {
                totalHeight += rowHeight + spacing
                maxRowWidth = max(maxRowWidth, rowWidth)
                rowWidth = 0
                rowHeight = 0
            }
            rowWidth += (rowWidth == 0 ? 0 : spacing) + size.width
            rowHeight = max(rowHeight, size.height)
        }

        totalHeight += rowHeight
        maxRowWidth = max(maxRowWidth, rowWidth)
        return CGSize(width: min(maxRowWidth, maxWidth), height: totalHeight)
    }

    func placeSubviews(
        in bounds: CGRect,
        proposal: ProposedViewSize,
        subviews: Subviews,
        cache: inout ()
    ) {
        var x = bounds.minX
        var y = bounds.minY
        var rowHeight: CGFloat = 0

        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x > bounds.minX, x + size.width > bounds.maxX {
                x = bounds.minX
                y += rowHeight + spacing
                rowHeight = 0
            }
            subview.place(
                at: CGPoint(x: x, y: y),
                proposal: ProposedViewSize(width: size.width, height: size.height)
            )
            x += size.width + spacing
            rowHeight = max(rowHeight, size.height)
        }
    }
}
