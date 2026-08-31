import SwiftUI

/// Themed Business Moments — activity list from live API.
struct BusinessMomentsActiveView: View {
    let refreshToken: UInt64
    let momentId: String?
    let momentTitle: String?
    var momentTypeCode: String? = nil
    var onAddExpense: () -> Void = {}
    var onOpenQuickAdd: () -> Void = {}

    @State private var activities: [APIClient.ActivityItemPayload] = []
    @State private var loading = true
    @State private var error: String?

    private var theme: BusinessActiveTheme { .forTypeCode(momentTypeCode) }

    var body: some View {
        Group {
            if loading && activities.isEmpty {
                ProgressView().tint(theme.accent)
            } else {
                ZStack(alignment: .bottomTrailing) {
                    ScrollView {
                        VStack(alignment: .leading, spacing: 12) {
                            if let error {
                                Text(error).font(.caption).foregroundStyle(Color(hex: "#F87171"))
                            }
                            if let momentTitle, !momentTitle.isEmpty {
                                Text(momentTitle)
                                    .font(.plusJakarta(size: 12, weight: .semibold))
                                    .foregroundStyle(theme.secondary)
                            }
                            Text(theme.momentsTitle)
                                .font(.plusJakarta(size: 20, weight: .heavy))
                                .foregroundStyle(theme.text)
                            if activities.isEmpty {
                                VStack(alignment: .leading, spacing: 8) {
                                    Text("Nothing recorded yet")
                                        .font(.plusJakarta(size: 15, weight: .bold))
                                        .foregroundStyle(theme.text)
                                    Text("Activity appears here after live writes. Empty stays empty.")
                                        .font(.plusJakarta(size: 13))
                                        .foregroundStyle(theme.secondary)
                                    Button("Open Action Center", action: onOpenQuickAdd)
                                        .font(.plusJakarta(size: 14, weight: .heavy))
                                        .foregroundStyle(.white)
                                        .padding(.horizontal, 16)
                                        .padding(.vertical, 10)
                                        .background(theme.accent)
                                        .clipShape(Capsule())
                                        .padding(.top, 4)
                                }
                                .padding(14)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .background(theme.card)
                                .overlay(RoundedRectangle(cornerRadius: 16).stroke(theme.border))
                                .clipShape(RoundedRectangle(cornerRadius: 16))
                            } else {
                                ForEach(activities) { activityRow($0) }
                            }
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 12)
                        .padding(.bottom, 56)
                    }
                    Button(action: onOpenQuickAdd) {
                        Image(systemName: "plus")
                            .font(.system(size: 18, weight: .heavy))
                            .foregroundStyle(.white)
                            .frame(width: 52, height: 52)
                            .background(theme.accent)
                            .clipShape(Circle())
                    }
                    .buttonStyle(.plain)
                    .padding(16)
                }
            }
        }
        .background(theme.bg)
        .task(id: "\(refreshToken)-\(momentId ?? "")") { await load() }
    }

    private func activityRow(_ item: APIClient.ActivityItemPayload) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(item.title.isEmpty ? item.activityCode : item.title)
                .font(.plusJakarta(size: 14, weight: .bold))
                .foregroundStyle(theme.text)
            Text(item.occurredAt)
                .font(.plusJakarta(size: 12))
                .foregroundStyle(theme.secondary)
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(theme.card)
        .overlay(RoundedRectangle(cornerRadius: 14).stroke(theme.border))
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }

    private func load() async {
        guard let momentId else {
            loading = false
            error = "Select a Business Moment."
            return
        }
        error = nil
        if let cached = BusinessTabDataCache.peekPulse(momentId), !cached.activities.isEmpty {
            activities = cached.activities
            loading = false
        } else {
            loading = activities.isEmpty
        }
        do {
            activities = try await APIClient.shared.listBusinessActivity(momentId: momentId)
        } catch {
            self.error = error.localizedDescription
        }
        loading = false
    }
}
