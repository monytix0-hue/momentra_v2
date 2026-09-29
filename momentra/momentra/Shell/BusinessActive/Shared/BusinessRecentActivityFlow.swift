import SwiftUI

/// Shared business activity sheet for TeamOps / Runway / Ops history CTAs.
struct BusinessRecentActivityFlow: View {
    let momentId: String
    @Binding var isPresented: Bool
    var accent: Color = Color(hex: "#818CF8")
    var subtitle: String = "Updates, spend, memories, and other events."

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
        NavigationStack {
            VStack(alignment: .leading, spacing: 8) {
                Text(subtitle)
                    .font(.system(size: 12))
                    .foregroundStyle(Color(hex: "#94A3B8"))
                    .padding(.horizontal, 16)
                chipRow
                content
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            .background(Color(hex: "#0C0F15").ignoresSafeArea())
            .navigationTitle("All activity")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { isPresented = false }
                }
            }
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
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundStyle(selected ? .white : Color(hex: "#C9C4D8"))
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(selected ? accent : Color.white.opacity(0.06), in: Capsule())
                            .overlay(Capsule().stroke(selected ? accent : Color.white.opacity(0.08), lineWidth: 1))
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
            Text(error)
                .font(.system(size: 13))
                .foregroundStyle(Color(hex: "#F87171"))
                .padding(16)
        } else if items.isEmpty {
            Text("No activity yet")
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(.white)
                .padding(24)
        } else if filteredItems.isEmpty {
            Text("No activity in this category")
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(.white)
                .padding(24)
        } else {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 4) {
                    if let error {
                        Text(error).font(.caption).foregroundStyle(Color(hex: "#F87171")).padding(.horizontal, 16)
                    }
                    ForEach(activityNodes) { node in
                        GroupActivityRow(
                            item: node.item,
                            accent: accent,
                            showChevron: false,
                            compactPadding: false
                        )
                        ForEach(node.children, id: \.id) { child in
                            GroupActivityRow(
                                item: child,
                                accent: accent,
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
                                    .font(.system(size: 14, weight: .semibold))
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
