import SwiftUI

/// Shared business activity sheet for TeamOps / Runway / Ops history CTAs.
struct BusinessRecentActivityFlow: View {
    let momentId: String
    @Binding var isPresented: Bool
    var accent: Color = Color(hex: "#818CF8")
    var title: String = "All activity"
    var subtitle: String = "Updates, spend, memories, and other events."
    var emptyTitle: String = "No activity yet"
    var emptyBody: String = "Updates, spend, and collections will show here."

    @State private var items: [APIClient.ActivityItemPayload] = []
    @State private var nextCursor: String?
    @State private var loading = true
    @State private var loadingMore = false
    @State private var error: String?
    @State private var filter = GroupActivityCategoryFilter.allId

    private var chips: [GroupActivityCategoryFilter.FilterChip] {
        GroupActivityCategoryFilter.chips(for: nil)
    }
    private var filteredItems: [APIClient.ActivityItemPayload] {
        items.filter { GroupActivityCategoryFilter.matches($0, chipId: filter) }
    }
    private var activityNodes: [GroupActivityTreeNode] {
        GroupActivityPresentation.activityTree(from: filteredItems)
    }

    var body: some View {
        NativeSheetScaffold(
            title: title,
            onClose: { isPresented = false },
            background: BusinessSheetTokens.bg
        ) {
            VStack(alignment: .leading, spacing: 8) {
                Text(subtitle)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(BusinessSheetTokens.muted)
                    .padding(.horizontal, 16)
                chipRow
                content
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            .task(id: momentId) { await reload() }
        }
    }

    private var chipRow: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(chips, id: \.id) { chip in
                    let selected = filter == chip.id
                    Button {
                        filter = selected ? GroupActivityCategoryFilter.allId : chip.id
                    } label: {
                        Text("\(chip.emoji) \(chip.label)")
                            .font(.plusJakarta(size: 12, weight: .semibold))
                            .foregroundStyle(selected ? BusinessSheetTokens.text : BusinessSheetTokens.muted)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(selected ? accent : BusinessSheetTokens.field, in: Capsule())
                            .overlay(
                                Capsule().stroke(
                                    selected ? accent : BusinessSheetTokens.border,
                                    lineWidth: 1
                                )
                            )
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, 16)
        }
    }

    @ViewBuilder
    private var content: some View {
        if loading && items.isEmpty {
            ProgressView().tint(accent).frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if let error, items.isEmpty {
            BusinessEmptyState(
                title: "Could not load activity",
                message: error,
                systemIcon: "exclamationmark.triangle",
                accent: Color(hex: "#F87171")
            )
        } else if items.isEmpty {
            BusinessEmptyState(
                title: emptyTitle,
                message: emptyBody,
                systemIcon: "clock.arrow.circlepath",
                accent: accent
            )
        } else if filteredItems.isEmpty {
            BusinessEmptyState(
                title: "No activity in this category",
                message: "Try another filter, or load more if the list is paginated.",
                systemIcon: "line.3.horizontal.decrease.circle",
                accent: accent
            )
        } else {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 0) {
                    if let error {
                        Text(error)
                            .font(.plusJakarta(size: 12))
                            .foregroundStyle(Color(hex: "#F87171"))
                            .padding(.horizontal, 16)
                            .padding(.vertical, 6)
                    }
                    ForEach(activityNodes) { node in
                        GroupActivityRow(
                            item: node.item,
                            accent: accent,
                            textColor: BusinessSheetTokens.text,
                            secondaryColor: BusinessSheetTokens.muted,
                            showChevron: false,
                            compactPadding: false
                        )
                        ForEach(node.children, id: \.id) { child in
                            GroupActivityRow(
                                item: child,
                                accent: accent,
                                textColor: BusinessSheetTokens.text,
                                secondaryColor: BusinessSheetTokens.muted,
                                showChevron: false,
                                compactPadding: false,
                                isChild: true
                            )
                        }
                    }
                    if nextCursor != nil {
                        Button {
                            Task { await loadMore() }
                        } label: {
                            if loadingMore {
                                ProgressView().tint(accent)
                            } else {
                                Text("Load more")
                                    .font(.plusJakarta(size: 14, weight: .semibold))
                                    .foregroundStyle(accent)
                            }
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .disabled(loadingMore)
                    }
                }
            }
        }
    }

    private func reload() async {
        loading = true
        error = nil
        nextCursor = nil
        filter = GroupActivityCategoryFilter.allId
        do {
            let page = try await APIClient.shared.listBusinessActivityPage(momentId: momentId, limit: 20)
            items = page.items
            nextCursor = page.nextCursor
        } catch {
            self.error = error.localizedDescription
            items = []
        }
        loading = false
    }

    private func loadMore() async {
        guard let cursor = nextCursor, !loadingMore else { return }
        loadingMore = true
        do {
            let page = try await APIClient.shared.listBusinessActivityPage(momentId: momentId, cursor: cursor, limit: 20)
            items += page.items
            nextCursor = page.nextCursor
        } catch {
            self.error = error.localizedDescription
        }
        loadingMore = false
    }
}
