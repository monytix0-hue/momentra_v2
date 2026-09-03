import SwiftUI

/// Themed Business Memory — live memory facet only.
struct BusinessMemoryActiveView: View {
    let refreshToken: UInt64
    let momentId: String?
    let momentTitle: String?
    var momentTypeCode: String? = nil
    var onOpenQuickAdd: () -> Void = {}

    @State private var memory: APIClient.BusinessMemoryPayload?
    @State private var loading = true
    @State private var error: String?

    private var theme: BusinessActiveTheme { .forTypeCode(momentTypeCode) }
    private var items: [APIClient.BusinessMemoryPayload.MemoryInner.BusinessMemoryItem] {
        memory?.payload?.items ?? []
    }

    var body: some View {
        Group {
            if loading && memory == nil {
                ProgressView().tint(theme.accent)
            } else {
                NativeDashboardScaffold(background: theme.bg) {

                    NativeListSection {

                    VStack(alignment: .leading, spacing: 12) {
                        if let error {
                            Text(error).font(.caption).foregroundStyle(Color(hex: "#F87171"))
                        }
                        if let momentTitle, !momentTitle.isEmpty {
                            Text(momentTitle)
                                .font(.plusJakarta(size: 12, weight: .semibold))
                                .foregroundStyle(theme.secondary)
                        }
                        HStack {
                            Text(theme.memoryTitle)
                                .font(.plusJakarta(size: 22, weight: .heavy))
                                .foregroundStyle(theme.text)
                            Spacer()
                            Button("Save", action: onOpenQuickAdd)
                                .font(.plusJakarta(size: 12, weight: .bold))
                                .foregroundStyle(theme.accent)
                        }

                        if items.isEmpty {
                            VStack(alignment: .leading, spacing: 8) {
                                Text("No memories yet")
                                    .font(.plusJakarta(size: 15, weight: .bold))
                                    .foregroundStyle(theme.text)
                                Text("Memories appear when the Memory write path saves live items. count: \(memory?.payload?.memoryCount ?? 0)")
                                    .font(.plusJakarta(size: 13))
                                    .foregroundStyle(theme.secondary)
                            }
                            .padding(14)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(theme.card)
                            .overlay(RoundedRectangle(cornerRadius: 16).stroke(theme.border))
                            .clipShape(RoundedRectangle(cornerRadius: 16))
                        } else {
                            ForEach(items) { item in
                                Text(item.title ?? "Memory")
                                    .font(.plusJakarta(size: 14, weight: .bold))
                                    .foregroundStyle(theme.text)
                                    .padding(12)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                    .background(theme.card)
                                    .overlay(RoundedRectangle(cornerRadius: 14).stroke(theme.border))
                                    .clipShape(RoundedRectangle(cornerRadius: 14))
                            }
                        }
                    }
                

                    }

                }
            }
        }
        .background(theme.bg)
        .task(id: "\(refreshToken)-\(momentId ?? "")") { await load() }
    }

    private func load() async {
        guard let momentId else {
            loading = false
            error = "Select a Business Moment."
            return
        }
        error = nil
        if let cached = BusinessTabDataCache.peekMemory(momentId)?.memory {
            memory = cached
            loading = false
        } else {
            loading = memory == nil
        }
        do {
            let tab = try await BusinessTabLoad.loadMemoryTab(momentId: momentId)
            memory = tab.memory
        } catch {
            self.error = error.localizedDescription
        }
        loading = false
    }
}
