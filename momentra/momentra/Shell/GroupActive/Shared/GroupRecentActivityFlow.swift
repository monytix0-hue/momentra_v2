import SwiftUI

/// Full Group activity list with cursor pagination + expense edit/void.
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
    @State private var editingExpenseId: String?
    @State private var editExpensePresented = false

    private var isWedding: Bool {
        GroupExperienceFamily.forTypeCode(momentTypeCode).isWedding
    }

    private var accent: Color {
        MomentThemes.resolve(context: .group, momentTypeCode: momentTypeCode).primary
    }

    var body: some View {
        NavigationStack {
            Group {
                if loading && items.isEmpty {
                    ProgressView().tint(accent)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if items.isEmpty {
                    VStack(spacing: 8) {
                        Text("No activity yet")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundStyle(Color(hex: "#E5E0EE"))
                        Text("Expenses, settlements, and updates will show here.")
                            .font(.system(size: 13))
                            .foregroundStyle(Color(hex: "#A8A3B5"))
                            .multilineTextAlignment(.center)
                    }
                    .padding(24)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
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
                            ForEach(Array(items.enumerated()), id: \.offset) { _, item in
                                let expenseId = item.activityPayload?.expenseId
                                let canEdit = PersonalActivityTimelineDerived.isExpense(item) && expenseId != nil
                                GroupActivityRow(
                                    item: item,
                                    accent: accent,
                                    showChevron: canEdit,
                                    compactPadding: false,
                                    action: canEdit ? {
                                        guard let expenseId else { return }
                                        editingExpenseId = expenseId
                                        editExpensePresented = true
                                    } : nil
                                )
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
            .background(Color(hex: "#14121B"))
            .navigationTitle("All activity")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { isPresented = false }
                        .foregroundStyle(accent)
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
        .onChange(of: editExpensePresented) { _, open in
            if !open { editingExpenseId = nil }
        }
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
}
