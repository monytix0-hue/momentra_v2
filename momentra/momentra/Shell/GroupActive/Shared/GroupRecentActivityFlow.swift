import SwiftUI

/// Full Group activity list with cursor pagination + expense/contribution edit/void.
struct GroupRecentActivityFlow: View {
    let momentId: String
    var momentTypeCode: String? = nil
    @Binding var isPresented: Bool
    var onChanged: () -> Void = {}

    @State private var items: [APIClient.ActivityItemPayload] = []
    @State private var nextCursor: String?
    @State private var loading = true
    @State private var loadingMore = false
    @State private var error: String?
    @State private var filter = GroupActivityCategoryFilter.allId
    @State private var editingExpenseId: String?
    @State private var editExpensePresented = false
    @State private var editingContribution: APIClient.GroupContributionItem?
    @State private var editContributionPresented = false
    @State private var resolvingContribution = false

    private var isWedding: Bool {
        GroupExperienceFamily.forTypeCode(momentTypeCode).isWedding
    }

    private var accent: Color {
        MomentThemes.resolve(context: .group, momentTypeCode: momentTypeCode).primary
    }

    private var chips: [GroupActivityCategoryFilter.FilterChip] {
        GroupActivityCategoryFilter.chips(for: momentTypeCode)
    }

    private var filteredItems: [APIClient.ActivityItemPayload] {
        items.filter { GroupActivityCategoryFilter.matches($0, chipId: filter) }
    }

    private var activityNodes: [GroupActivityTreeNode] {
        GroupActivityPresentation.activityTree(from: filteredItems)
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                chipRow
                    .padding(.top, 8)
                    .padding(.bottom, 4)

                Group {
                    if loading && items.isEmpty {
                        ProgressView().tint(accent)
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                    } else if items.isEmpty {
                        emptyState(
                            title: "No activity yet",
                            detail: "Expenses, settlements, and updates will show here."
                        )
                    } else if filteredItems.isEmpty {
                        emptyState(
                            title: "No activity in this category",
                            detail: "Try another hub filter, or load more if the list is paginated."
                        )
                    } else {
                        ScrollView {
                            LazyVStack(alignment: .leading, spacing: 0) {
                                if let error {
                                    Text(error)
                                        .font(.caption)
                                        .foregroundStyle(Color(hex: "#F87171"))
                                        .padding(.horizontal, 16)
                                        .padding(.vertical, 8)
                                }
                                ForEach(Array(activityNodes.enumerated()), id: \.element.id) { _, node in
                                    let item = node.item
                                    let expenseId = item.activityPayload?.expenseId
                                    let contributionId = item.activityPayload?.contributionId
                                    let canEditExpense = PersonalActivityTimelineDerived.isExpense(item) && expenseId != nil
                                    let canEditContribution = Self.isContribution(item) && contributionId != nil
                                    let canEdit = !GroupActivityPresentation.nodeHasVoidChild(node)
                                        && (canEditExpense || canEditContribution)
                                    VStack(alignment: .leading, spacing: 4) {
                                        GroupActivityRow(
                                            item: item,
                                            accent: accent,
                                            showChevron: canEdit,
                                            compactPadding: false,
                                            action: canEdit ? {
                                                if canEditExpense, let expenseId {
                                                    editingExpenseId = expenseId
                                                    editExpensePresented = true
                                                } else if canEditContribution, let contributionId {
                                                    Task { await openContributionEdit(contributionId: contributionId) }
                                                }
                                            } : nil
                                        )
                                        ForEach(Array(node.children.enumerated()), id: \.offset) { _, child in
                                            GroupActivityRow(
                                                item: child,
                                                accent: accent,
                                                showChevron: false,
                                                compactPadding: false,
                                                isChild: true
                                            )
                                        }
                                    }
                                    Divider().overlay(Color(hex: "#2A2624"))
                                }
                                if nextCursor != nil {
                                    Button {
                                        Task { await loadMore() }
                                    } label: {
                                        if loadingMore {
                                            ProgressView().tint(accent)
                                                .frame(maxWidth: .infinity)
                                                .padding(.vertical, 16)
                                        } else {
                                            Text("Load more")
                                                .font(.system(size: 14, weight: .semibold))
                                                .foregroundStyle(accent)
                                                .frame(maxWidth: .infinity)
                                                .padding(.vertical, 16)
                                        }
                                    }
                                    .disabled(loadingMore)
                                }
                            }
                            .padding(.bottom, 24)
                        }
                    }
                }
            }
            .background(Color(hex: "#14121B"))
            .navigationTitle("All activity")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { isPresented = false }
                        .foregroundStyle(accent)
                }
            }
            .overlay {
                if resolvingContribution {
                    ProgressView().tint(accent)
                }
            }
        }
        .task { await reload() }
        .sheet(isPresented: $editExpensePresented) {
            if let editingExpenseId {
                GroupExpenseSheet(
                    momentId: momentId,
                    isPresented: $editExpensePresented,
                    expenseId: editingExpenseId,
                    isWedding: isWedding,
                    momentTypeCode: momentTypeCode,
                    onSaved: {
                        editExpensePresented = false
                        onChanged()
                        Task { await reload() }
                    },
                    onDeleted: {
                        editExpensePresented = false
                        onChanged()
                        Task { await reload() }
                    }
                )
            }
        }
        .sheet(isPresented: $editContributionPresented) {
            if let editingContribution {
                GroupContributionSheet(
                    momentId: momentId,
                    isPresented: $editContributionPresented,
                    isWedding: isWedding,
                    editingContribution: editingContribution,
                    onSaved: {
                        editContributionPresented = false
                        onChanged()
                        Task { await reload() }
                    },
                    onDeleted: {
                        editContributionPresented = false
                        onChanged()
                        Task { await reload() }
                    }
                )
            }
        }
        .onChange(of: editExpensePresented) { _, open in
            if !open { editingExpenseId = nil }
        }
        .onChange(of: editContributionPresented) { _, open in
            if !open { editingContribution = nil }
        }
        .onChange(of: momentTypeCode) { _, _ in
            filter = GroupActivityCategoryFilter.allId
        }
    }

    private var chipRow: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(chips) { chip in
                    filterChip(chip)
                }
            }
            .padding(.horizontal, 16)
        }
    }

    private func filterChip(_ chip: GroupActivityCategoryFilter.FilterChip) -> some View {
        let selected = filter == chip.id
        return Text("\(chip.emoji) \(chip.label)")
            .font(.system(size: 12, weight: .semibold))
            .foregroundStyle(selected ? .white : Color(hex: "#C9C4D8"))
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(selected ? accent : Color.white.opacity(0.06))
            .overlay(Capsule().stroke(selected ? accent : Color.white.opacity(0.08)))
            .clipShape(Capsule())
            .onTapGesture {
                filter = filter == chip.id ? GroupActivityCategoryFilter.allId : chip.id
            }
    }

    private func emptyState(title: String, detail: String) -> some View {
        VStack(spacing: 8) {
            Text(title)
                .font(.system(size: 16, weight: .semibold))
                .foregroundStyle(Color(hex: "#E5E0EE"))
            Text(detail)
                .font(.system(size: 13))
                .foregroundStyle(Color(hex: "#A8A3B5"))
                .multilineTextAlignment(.center)
        }
        .padding(24)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private func reload() async {
        loading = true
        error = nil
        nextCursor = nil
        do {
            let page = try await APIClient.shared.listGroupActivityPage(momentId: momentId, limit: 20)
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
            let page = try await APIClient.shared.listGroupActivityPage(
                momentId: momentId,
                cursor: cursor,
                limit: 20
            )
            items.append(contentsOf: page.items)
            nextCursor = page.nextCursor
        } catch {
            self.error = error.localizedDescription
        }
        loadingMore = false
    }

    private func openContributionEdit(contributionId: String) async {
        resolvingContribution = true
        defer { resolvingContribution = false }
        do {
            let payload = try await APIClient.shared.listContributions(momentId: momentId, limit: 100)
            if let found = payload.items?.first(where: { $0.contributionId == contributionId }) {
                editingContribution = found
                editContributionPresented = true
            } else {
                error = "Contribution not found"
            }
        } catch {
            self.error = error.localizedDescription
        }
    }

    private static func isContribution(_ item: APIClient.ActivityItemPayload) -> Bool {
        let upper = item.activityCode.uppercased()
        if item.activityPayload?.contributionId != nil { return true }
        return upper.contains("CONTRIBUTION") || (upper.contains("CONTRIB") && !upper.contains("CONTRIBUTOR"))
    }
}
