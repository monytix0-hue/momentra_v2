import SwiftUI

/// Advisory HSN / SAC suggestions from `GET /business/gst/hsn`.
struct HsnLookupHints: View {
    let query: String
    let accent: Color
    var onPick: (String, String?) -> Void

    @State private var results: [HsnLookupResultPayload] = []
    @State private var task: Task<Void, Never>?

    var body: some View {
        Group {
            if !results.isEmpty {
                VStack(alignment: .leading, spacing: 4) {
                    Text("HSN suggestions · advisory rate")
                        .font(.plusJakarta(size: 10))
                        .foregroundStyle(accent)
                    ForEach(Array(results.prefix(5).enumerated()), id: \.offset) { _, hit in
                        Button {
                            onPick(hit.code, hit.gstRate)
                        } label: {
                            Text(label(for: hit))
                                .font(.plusJakarta(size: 12, weight: .medium))
                                .foregroundStyle(OperatingV12.title)
                                .frame(maxWidth: .infinity, alignment: .leading)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.top, 4)
            }
        }
        .onChange(of: query) { _, next in
            schedule(next)
        }
        .onAppear { schedule(query) }
        .onDisappear { task?.cancel() }
    }

    private func label(for hit: HsnLookupResultPayload) -> String {
        var parts = [hit.code, String(hit.description.prefix(48))]
        if let rate = hit.gstRate?.trimmingCharacters(in: .whitespaces), !rate.isEmpty {
            parts.append("\(rate)%")
        }
        return parts.joined(separator: " · ")
    }

    private func schedule(_ raw: String) {
        task?.cancel()
        let digits = raw.filter(\.isNumber)
        let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard digits.count >= 2 || trimmed.count >= 3 else {
            results = []
            return
        }
        task = Task {
            try? await Task.sleep(nanoseconds: 280_000_000)
            guard !Task.isCancelled else { return }
            let q = digits.isEmpty ? trimmed : String(digits)
            do {
                let payload = try await APIClient.shared.searchHsn(query: q, limit: 6)
                guard !Task.isCancelled else { return }
                await MainActor.run { results = payload.results ?? [] }
            } catch {
                guard !Task.isCancelled else { return }
                await MainActor.run { results = [] }
            }
        }
    }
}
