import SwiftUI

enum GroupMomentDirectoryBucket: String, CaseIterable, Identifiable {
    case celebrations = "Celebrations"
    case gatherings = "Gatherings"
    case trips = "Trips"
    case plans = "Plans"

    var id: String { rawValue }
    var label: String { rawValue }

    var singular: String {
        switch self {
        case .celebrations: return "Celebration"
        case .gatherings: return "Gathering"
        case .trips: return "Trip"
        case .plans: return "Plan"
        }
    }

    static func forTypeCode(_ momentTypeCode: String?) -> GroupMomentDirectoryBucket {
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

    func gradients(at index: Int) -> [Color] {
        let palettes: [[Color]]
        switch self {
        case .celebrations:
            palettes = [
                [Color(hex: "#E879A8"), Color(hex: "#C084FC")],
                [Color(hex: "#818CF8"), Color(hex: "#A78BFA")],
                [Color(hex: "#F472B6"), Color(hex: "#FB7185")],
            ]
        case .gatherings:
            palettes = [
                [Color(hex: "#FB923C"), Color(hex: "#F97316")],
                [Color(hex: "#A78BFA"), Color(hex: "#6366F1")],
                [Color(hex: "#FBBF24"), Color(hex: "#F59E0B")],
                [Color(hex: "#FB7185"), Color(hex: "#F97316")],
            ]
        case .trips:
            palettes = [
                [Color(hex: "#38BDF8"), Color(hex: "#0EA5E9")],
                [Color(hex: "#6366F1"), Color(hex: "#3B82F6")],
                [Color(hex: "#2DD4BF"), Color(hex: "#14B8A6")],
                [Color(hex: "#22D3EE"), Color(hex: "#06B6D4")],
            ]
        case .plans:
            palettes = [
                [Color(hex: "#4ADE80"), Color(hex: "#22C55E")],
                [Color(hex: "#A3E635"), Color(hex: "#84CC16")],
            ]
        }
        return palettes[index % palettes.count]
    }
}

/// Figma Active Moments directory — Group moment selector (1–4 large cards vs 5+ horizontal grids).
struct GroupActiveMomentsDirectoryView: View {
    enum LifecycleTab: String, CaseIterable, Identifiable {
        case ongoing = "Ongoing"
        case completed = "Completed"
        var id: String { rawValue }
    }

    let moments: [MomentSummary]
    let selectedMomentId: String?
    var onDismiss: () -> Void
    var onSelectMoment: (String) -> Void
    var onSelectCompletedMoment: (MomentSummary) -> Void = { _ in }
    var onOpenStory: (String) -> Void = { _ in }
    var onCreateMoment: () -> Void
    var initialCompletedTab: Bool = false

    @State private var query = ""
    @State private var selectedBucket: GroupMomentDirectoryBucket?
    @State private var lifecycleTab: LifecycleTab
    @State private var completedMoments: [MomentSummary] = []
    @State private var loadingCompleted = false
    @State private var completedError: String?

    init(
        moments: [MomentSummary],
        selectedMomentId: String?,
        onDismiss: @escaping () -> Void,
        onSelectMoment: @escaping (String) -> Void,
        onSelectCompletedMoment: @escaping (MomentSummary) -> Void = { _ in },
        onOpenStory: @escaping (String) -> Void = { _ in },
        onCreateMoment: @escaping () -> Void,
        initialCompletedTab: Bool = false
    ) {
        self.moments = moments
        self.selectedMomentId = selectedMomentId
        self.onDismiss = onDismiss
        self.onSelectMoment = onSelectMoment
        self.onSelectCompletedMoment = onSelectCompletedMoment
        self.onOpenStory = onOpenStory
        self.onCreateMoment = onCreateMoment
        self.initialCompletedTab = initialCompletedTab
        _lifecycleTab = State(initialValue: initialCompletedTab ? .completed : .ongoing)
    }

    private var active: [MomentSummary] {
        moments.filter(\.isActiveStatus)
    }

    private var source: [MomentSummary] {
        lifecycleTab == .ongoing ? active : completedMoments
    }

    private var isCompletedTab: Bool { lifecycleTab == .completed }

    private var filtered: [MomentSummary] {
        source.filter { moment in
            let matchesQuery = query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                || moment.title.localizedCaseInsensitiveContains(query.trimmingCharacters(in: .whitespacesAndNewlines))
            let bucket = GroupMomentDirectoryBucket.forTypeCode(moment.momentTypeCode)
            let matchesBucket = selectedBucket == nil || bucket == selectedBucket
            return matchesQuery && matchesBucket
        }
    }

    private var grouped: [(GroupMomentDirectoryBucket, [MomentSummary])] {
        GroupMomentDirectoryBucket.allCases.compactMap { bucket in
            let items = filtered.filter { GroupMomentDirectoryBucket.forTypeCode($0.momentTypeCode) == bucket }
            return items.isEmpty ? nil : (bucket, items)
        }
    }

    private var sparse: Bool { (1...4).contains(filtered.count) }

    private var headline: String {
        if isCompletedTab {
            switch filtered.count {
            case 0: return "No completed moments"
            case 1: return "1 completed moment"
            default: return "\(filtered.count) completed moments"
            }
        }
        switch filtered.count {
        case 0: return "No active moments"
        case 1: return "1 active moment"
        default: return "\(filtered.count) active moments"
        }
    }

    private var subtitle: String {
        if isCompletedTab {
            if filtered.isEmpty { return "Complete a moment to reopen it or relive its Story" }
            return "Open to settle expenses · Story on each card"
        }
        if filtered.isEmpty { return "Create a group moment to get started" }
        if sparse { return "Your live moment · ready to open" }
        return "Visual directory · all live"
    }

    var body: some View {
        VStack(spacing: 0) {
            header
            lifecycleTabs
            summaryTiles
            searchRow
            chipsRow
            content
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(GlobalTheme.surfaceContent.ignoresSafeArea())
        .accessibilityIdentifier("group.moment.directory")
        .task(id: lifecycleTab) {
            if lifecycleTab == .completed {
                await loadCompleted()
            }
        }
    }

    private var lifecycleTabs: some View {
        HStack(spacing: 0) {
            ForEach(LifecycleTab.allCases) { tab in
                Button {
                    lifecycleTab = tab
                    selectedBucket = nil
                } label: {
                    Text(tab.rawValue)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(lifecycleTab == tab ? Color(hex: "#1A1625") : MomentraBrandTokens.textOnDark.opacity(0.7))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .background(
                            lifecycleTab == tab
                                ? Color(hex: "#E8E2D6")
                                : Color.clear,
                            in: Capsule()
                        )
                }
                .buttonStyle(.plain)
            }
        }
        .padding(4)
        .background(Color(hex: "#1C1A24"), in: Capsule())
        .padding(.horizontal, 16)
        .padding(.bottom, 10)
    }

    @MainActor
    private func loadCompleted() async {
        loadingCompleted = true
        completedError = nil
        defer { loadingCompleted = false }
        do {
            completedMoments = try await APIClient.shared.listGroupMoments(limit: 50, lifecycle: "completed")
        } catch {
            completedError = error.localizedDescription
            completedMoments = []
        }
    }

    private func openMoment(_ moment: MomentSummary) {
        if isCompletedTab {
            onSelectCompletedMoment(moment)
            onDismiss()
        } else {
            onSelectMoment(moment.momentId)
            onDismiss()
        }
    }

    private var header: some View {
        HStack(alignment: .top) {
            VStack(alignment: .leading, spacing: 4) {
                Text(headline)
                    .font(.system(size: 26, weight: .bold))
                    .foregroundStyle(MomentraBrandTokens.textOnDark)
                Text(subtitle)
                    .font(.system(size: 13))
                    .foregroundStyle(Color(hex: "#9A94A8"))
            }
            Spacer(minLength: 8)
            Button(action: onDismiss) {
                Image(systemName: "xmark")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(MomentraBrandTokens.textOnDark.opacity(0.7))
                    .frame(width: 36, height: 36)
                    .contentShape(Circle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Close")
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
    }

    private var summaryTiles: some View {
        HStack(spacing: 12) {
            summaryTile(
                value: "\(filtered.count)",
                label: isCompletedTab ? "Completed" : "Active now"
            )
            summaryTile(value: "\(grouped.count)", label: "Categories")
        }
        .padding(.horizontal, 16)
    }

    private func summaryTile(value: String, label: String) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(value)
                .font(.system(size: 28, weight: .bold))
                .foregroundStyle(Color(hex: "#1A1625"))
            Text(label)
                .font(.system(size: 13))
                .foregroundStyle(Color(hex: "#5C5668"))
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .background(Color(hex: "#E8E2D6"), in: RoundedRectangle(cornerRadius: 18, style: .continuous))
    }

    private var searchRow: some View {
        HStack(spacing: 10) {
            HStack(spacing: 8) {
                Image(systemName: "magnifyingglass")
                    .foregroundStyle(Color(hex: "#9A94A8"))
                TextField("Find a moment", text: $query)
                    .foregroundStyle(MomentraBrandTokens.textOnDark)
                    .autocorrectionDisabled()
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .background(Color(hex: "#1C1A24"), in: RoundedRectangle(cornerRadius: 14, style: .continuous))

            Image(systemName: "slider.horizontal.3")
                .foregroundStyle(MomentraBrandTokens.textOnDark.opacity(0.7))
                .frame(width: 44, height: 44)
                .background(Color(hex: "#1C1A24"), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
        }
        .padding(.horizontal, 16)
        .padding(.top, 14)
    }

    private var chipsRow: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                chip("All", selected: selectedBucket == nil) { selectedBucket = nil }
                ForEach(GroupMomentDirectoryBucket.allCases.filter { bucket in
                    source.contains { GroupMomentDirectoryBucket.forTypeCode($0.momentTypeCode) == bucket }
                }) { bucket in
                    chip(bucket.label, selected: selectedBucket == bucket) {
                        selectedBucket = selectedBucket == bucket ? nil : bucket
                    }
                }
            }
            .padding(.horizontal, 16)
        }
        .padding(.top, 12)
        .padding(.bottom, 8)
    }

    private func chip(_ label: String, selected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label)
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(selected ? Color.black : MomentraBrandTokens.textOnDark)
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(selected ? Color.white : Color(hex: "#1C1A24"), in: Capsule())
                .overlay(Capsule().stroke(selected ? Color.white : Color(hex: "#3A3648"), lineWidth: 1))
        }
        .buttonStyle(.plain)
    }

    @ViewBuilder
    private var content: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 16) {
                if loadingCompleted && isCompletedTab {
                    ProgressView()
                        .tint(MomentraBrandTokens.textOnDark)
                        .frame(maxWidth: .infinity)
                        .padding(.top, 40)
                } else if let completedError, isCompletedTab {
                    Text(completedError)
                        .font(.system(size: 14))
                        .foregroundStyle(Color(hex: "#F87171"))
                        .frame(maxWidth: .infinity)
                        .padding(.top, 40)
                } else if filtered.isEmpty {
                    Text(isCompletedTab ? "No completed moments yet." : "No moments match your filters.")
                        .font(.system(size: 14))
                        .foregroundStyle(Color(hex: "#9A94A8"))
                        .frame(maxWidth: .infinity)
                        .padding(.top, 40)
                } else if sparse {
                    ForEach(grouped, id: \.0.id) { bucket, items in
                        sectionHeader(bucket.label.uppercased(), count: items.count)
                        ForEach(Array(items.enumerated()), id: \.element.momentId) { idx, moment in
                            largeCard(moment: moment, bucket: bucket, index: idx, height: 148)
                        }
                    }
                    if !isCompletedTab { createFooter }
                } else {
                    ForEach(grouped, id: \.0.id) { bucket, items in
                        sectionHeader(bucket.label.uppercased(), count: items.count)
                        if items.count == 1, let only = items.first {
                            largeCard(moment: only, bucket: bucket, index: 0, height: 120)
                        } else {
                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 10) {
                                    ForEach(Array(items.enumerated()), id: \.element.momentId) { idx, moment in
                                        compactCard(moment: moment, bucket: bucket, index: idx)
                                    }
                                }
                                .padding(.trailing, 8)
                            }
                        }
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 24)
            .padding(.top, 8)
        }
    }

    private func sectionHeader(_ title: String, count: Int) -> some View {
        HStack {
            Text(title)
                .font(.system(size: 12, weight: .bold))
                .foregroundStyle(Color(hex: "#9A94A8"))
            Spacer()
            Text("\(count)")
                .font(.system(size: 12))
                .foregroundStyle(Color(hex: "#9A94A8").opacity(0.7))
        }
    }

    private var createFooter: some View {
        VStack(spacing: 10) {
            Text("That's everything live")
                .font(.system(size: 16, weight: .bold))
                .foregroundStyle(MomentraBrandTokens.textOnDark)
            Text("Spin up another shared experience when you're ready.")
                .font(.system(size: 13))
                .foregroundStyle(Color(hex: "#9A94A8"))
                .multilineTextAlignment(.center)
            Button(action: onCreateMoment) {
                Text("CREATE ANOTHER MOMENT")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(
                        LinearGradient(
                            colors: [Color(hex: "#E8621A"), Color(hex: "#FDBA74")],
                            startPoint: .leading,
                            endPoint: .trailing
                        ),
                        in: Capsule()
                    )
            }
            .buttonStyle(.plain)
            .accessibilityIdentifier("group.moment.directory.create")
        }
        .padding(20)
        .frame(maxWidth: .infinity)
        .background(Color(hex: "#1C1A24"), in: RoundedRectangle(cornerRadius: 20, style: .continuous))
    }

    private func largeCard(moment: MomentSummary, bucket: GroupMomentDirectoryBucket, index: Int, height: CGFloat) -> some View {
        let selected = moment.momentId == selectedMomentId
        let members = max(0, moment.participantCount)
        let meta = "\(members) member\(members == 1 ? "" : "s") · \(bucket.singular)"
        return Button {
            openMoment(moment)
        } label: {
            VStack(alignment: .leading, spacing: 0) {
                HStack {
                    statusBadge
                    Spacer(minLength: 0)
                    if isCompletedTab {
                        Button {
                            onOpenStory(moment.momentId)
                            onDismiss()
                        } label: {
                            Text("Story")
                                .font(.system(size: 12, weight: .bold))
                                .foregroundStyle(.white)
                                .padding(.horizontal, 10)
                                .padding(.vertical, 6)
                                .background(Color.black.opacity(0.35), in: Capsule())
                        }
                        .buttonStyle(.plain)
                    }
                }
                Spacer(minLength: 0)
                Text(moment.title)
                    .font(.system(size: 24, weight: .bold))
                    .foregroundStyle(.white)
                    .lineLimit(2)
                Text(meta)
                    .font(.system(size: 13))
                    .foregroundStyle(.white.opacity(0.9))
                    .padding(.top, 4)
            }
            .padding(18)
            .frame(maxWidth: .infinity, minHeight: height, maxHeight: height, alignment: .leading)
            .background(
                LinearGradient(colors: bucket.gradients(at: index), startPoint: .topLeading, endPoint: .bottomTrailing),
                in: RoundedRectangle(cornerRadius: 22, style: .continuous)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 22, style: .continuous)
                    .stroke(selected ? Color.white.opacity(0.85) : Color.clear, lineWidth: 2)
            )
        }
        .buttonStyle(DirectoryCardPressStyle())
    }

    private func compactCard(moment: MomentSummary, bucket: GroupMomentDirectoryBucket, index: Int) -> some View {
        let selected = moment.momentId == selectedMomentId
        let members = max(0, moment.participantCount)
        return Button {
            openMoment(moment)
        } label: {
            VStack(alignment: .leading, spacing: 0) {
                HStack {
                    statusBadge
                    Spacer(minLength: 0)
                    if isCompletedTab {
                        Button {
                            onOpenStory(moment.momentId)
                            onDismiss()
                        } label: {
                            Text("Story")
                                .font(.system(size: 10, weight: .bold))
                                .foregroundStyle(.white)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 4)
                                .background(Color.black.opacity(0.35), in: Capsule())
                        }
                        .buttonStyle(.plain)
                    }
                }
                Spacer(minLength: 0)
                Text(moment.title)
                    .font(.system(size: 15, weight: .bold))
                    .foregroundStyle(.white)
                    .lineLimit(2)
                Text("\(members) members")
                    .font(.system(size: 11))
                    .foregroundStyle(.white.opacity(0.88))
                    .padding(.top, 2)
            }
            .padding(14)
            .frame(width: 148, height: 132, alignment: .leading)
            .background(
                LinearGradient(colors: bucket.gradients(at: index), startPoint: .topLeading, endPoint: .bottomTrailing),
                in: RoundedRectangle(cornerRadius: 20, style: .continuous)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .stroke(selected ? Color.white.opacity(0.85) : Color.clear, lineWidth: 2)
            )
        }
        .buttonStyle(DirectoryCardPressStyle())
    }

    @ViewBuilder
    private var statusBadge: some View {
        if isCompletedTab {
            HStack(spacing: 6) {
                Circle()
                    .fill(Color(hex: "#94A3B8"))
                    .frame(width: 7, height: 7)
                Text("COMPLETED")
                    .font(.system(size: 10, weight: .bold))
                    .foregroundStyle(.white.opacity(0.95))
            }
        } else {
            TimelineView(.animation(minimumInterval: 0.45, paused: false)) { context in
                let phase = context.date.timeIntervalSinceReferenceDate
                let alpha = 0.45 + 0.55 * (0.5 + 0.5 * sin(phase * .pi * 2 / 0.9))
                HStack(spacing: 6) {
                    Circle()
                        .fill(Color(hex: "#4ADE80").opacity(alpha))
                        .frame(width: 7, height: 7)
                    Text("LIVE NOW")
                        .font(.system(size: 10, weight: .bold))
                        .foregroundStyle(.white.opacity(0.95))
                }
            }
        }
    }
}

private struct DirectoryCardPressStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.98 : 1)
            .animation(.easeOut(duration: 0.12), value: configuration.isPressed)
    }
}
