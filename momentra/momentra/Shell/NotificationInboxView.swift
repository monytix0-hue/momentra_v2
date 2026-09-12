import SwiftUI

/// In-app notification inbox — the surface that shows group activity even when
/// OS push is denied or undelivered. Backed by `GET /v1/me/notifications`.
struct NotificationInboxView: View {
    var onOpenMoment: (String) -> Void
    var onClose: () -> Void

    @State private var items: [APIClient.NotificationInboxItemPayload] = []
    @State private var loading = true
    @State private var errorText: String?

    var body: some View {
        NavigationStack {
            Group {
                if loading {
                    ProgressView()
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .accessibilityIdentifier("inbox.loading")
                } else if let errorText {
                    emptyState(
                        title: "Couldn't load notifications",
                        detail: errorText,
                        id: "inbox.error"
                    )
                } else if items.isEmpty {
                    emptyState(
                        title: "You're all caught up",
                        detail: "Group updates, polls, bookings, and expenses will show up here.",
                        id: "inbox.empty"
                    )
                } else {
                    let grouped = Dictionary(grouping: items) { $0.threadKey ?? $0.momentId ?? "other" }
                    let keys = grouped.keys.sorted { a, b in
                        let aDate = grouped[a]?.first?.createdAt ?? ""
                        let bDate = grouped[b]?.first?.createdAt ?? ""
                        return aDate > bDate
                    }
                    List {
                        ForEach(keys, id: \.self) { key in
                            Section {
                                ForEach(grouped[key] ?? [], id: \.notificationId) { item in
                                    Button {
                                        open(item)
                                    } label: {
                                        row(item)
                                    }
                                    .buttonStyle(.plain)
                                    .listRowBackground(Color(hex: "#14121B"))
                                }
                            } header: {
                                Text(grouped[key]?.first?.momentTitle ?? "Updates")
                                    .foregroundStyle(MomentraBrandTokens.textOnDark.opacity(0.6))
                            }
                        }
                    }
                    .listStyle(.plain)
                    .accessibilityIdentifier("inbox.list")
                }
            }
            .background(Color(hex: "#14121B").ignoresSafeArea())
            .navigationTitle("Notifications")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close", action: onClose)
                        .accessibilityIdentifier("inbox.close")
                }
                ToolbarItem(placement: .primaryAction) {
                    Button("Mark all read") {
                        Task { await markAllRead() }
                    }
                    .disabled(items.allSatisfy { $0.readAt != nil })
                    .accessibilityIdentifier("inbox.mark_all_read")
                }
            }
        }
        .preferredColorScheme(.dark)
        .task { await load() }
    }

    private func row(_ item: APIClient.NotificationInboxItemPayload) -> some View {
        HStack(alignment: .top, spacing: 10) {
            Circle()
                .fill(item.readAt == nil ? Color(hex: "#F43F5E") : Color.clear)
                .frame(width: 7, height: 7)
                .padding(.top, 6)
            VStack(alignment: .leading, spacing: 3) {
                Text(item.title)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(MomentraBrandTokens.textOnDark)
                Text(item.body)
                    .font(.system(size: 13))
                    .foregroundStyle(MomentraBrandTokens.textOnDark.opacity(0.7))
                    .fixedSize(horizontal: false, vertical: true)
            }
            Spacer(minLength: 0)
        }
        .padding(.vertical, 6)
        .contentShape(Rectangle())
        .accessibilityIdentifier("inbox.item")
    }

    private func emptyState(title: String, detail: String, id: String) -> some View {
        VStack(spacing: 8) {
            Image(systemName: "bell.slash")
                .font(.system(size: 28))
                .foregroundStyle(MomentraBrandTokens.textOnDark.opacity(0.5))
            Text(title)
                .font(.system(size: 16, weight: .semibold))
                .foregroundStyle(MomentraBrandTokens.textOnDark)
            Text(detail)
                .font(.system(size: 13))
                .multilineTextAlignment(.center)
                .foregroundStyle(MomentraBrandTokens.textOnDark.opacity(0.7))
        }
        .padding(24)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .accessibilityIdentifier(id)
    }

    private func load() async {
        loading = true
        errorText = nil
        do {
            let payload = try await APIClient.shared.listMyNotifications(limit: 50)
            items = payload.items
            NotificationInboxBadge.shared.set(payload.unreadCount)
        } catch {
            errorText = error.localizedDescription
        }
        loading = false
    }

    private func open(_ item: APIClient.NotificationInboxItemPayload) {
        Task { await markRead([item.notificationId]) }
        let momentId = item.deepLink.flatMap(PushDeepLinkStore.parseMomentId) ?? item.momentId
        guard let momentId, !momentId.isEmpty else { return }
        onClose()
        onOpenMoment(momentId)
    }

    private func markRead(_ ids: [String]) async {
        _ = try? await APIClient.shared.markMyNotificationsRead(notificationIds: ids)
        await refreshBadge()
    }

    private func markAllRead() async {
        _ = try? await APIClient.shared.markMyNotificationsRead(all: true)
        await load()
    }

    private func refreshBadge() async {
        guard let payload = try? await APIClient.shared.listMyNotifications(limit: 50) else { return }
        items = payload.items
        NotificationInboxBadge.shared.set(payload.unreadCount)
    }
}
