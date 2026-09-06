import Foundation

/// SSE payload for `event: PROJECTION_UPDATED` from `GET /v1/realtime/sse`.
struct ProjectionUpdatedEvent: Decodable, Sendable {
    var type: String?
    var projection: String?
    var projections: [String]?
    var scopeType: String?
    var scopeId: String?
    var projectionVersion: Int64?
    var correlationId: String?
}

/// Streams `PROJECTION_UPDATED` from the backend realtime plane.
final class SseClient: @unchecked Sendable {
    static let shared = SseClient()

    private actor EventHandlers {
        private var onEvent: (@Sendable (ProjectionUpdatedEvent) -> Void)?

        func set(_ handler: (@Sendable (ProjectionUpdatedEvent) -> Void)?) {
            onEvent = handler
        }

        func current() -> (@Sendable (ProjectionUpdatedEvent) -> Void)? {
            onEvent
        }
    }

    private let handlers = EventHandlers()
    private var task: Task<Void, Never>?

    func connect(onProjectionUpdated: @escaping @Sendable (ProjectionUpdatedEvent) -> Void) {
        disconnect()
        task = Task.detached(priority: .utility) { [handlers] in
            await handlers.set(onProjectionUpdated)
            await Self.runLoop(handlers: handlers)
        }
    }

    func disconnect() {
        task?.cancel()
        task = nil
        Task { await handlers.set(nil) }
    }

    private static func runLoop(handlers: EventHandlers) async {
        while !Task.isCancelled {
            do {
                try await streamOnce(handlers: handlers)
            } catch is CancellationError {
                return
            } catch {
                try? await Task.sleep(nanoseconds: 2_000_000_000)
            }
        }
    }

    private static func streamOnce(handlers: EventHandlers) async throws {
        let token = try await APIClient.shared.firebaseIdToken()
        var request = URLRequest(url: APIConfig.baseURL.appendingPathComponent("v1/realtime/sse"))
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        request.setValue("text/event-stream", forHTTPHeaderField: "Accept")
        request.setValue("true", forHTTPHeaderField: "ngrok-skip-browser-warning")
        request.timeoutInterval = 60 * 60

        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 60 * 60
        config.timeoutIntervalForResource = 60 * 60
        config.waitsForConnectivity = true
        let session = URLSession(configuration: config)

        let (bytes, response) = try await session.bytes(for: request)
        if let http = response as? HTTPURLResponse, !(200..<300).contains(http.statusCode) {
            throw URLError(.badServerResponse)
        }

        var eventName: String?
        var dataLine: String?
        for try await line in bytes.lines {
            if Task.isCancelled { return }
            if line.hasPrefix("event:") {
                eventName = String(line.dropFirst(6)).trimmingCharacters(in: .whitespaces)
            } else if line.hasPrefix("data:") {
                dataLine = String(line.dropFirst(5)).trimmingCharacters(in: .whitespaces)
            } else if line.isEmpty, let payload = dataLine {
                if eventName == "PROJECTION_UPDATED",
                   let data = payload.data(using: .utf8),
                   let parsed = try? JSONDecoder().decode(ProjectionUpdatedEvent.self, from: data) {
                    let handler = await handlers.current()
                    handler?(parsed)
                }
                eventName = nil
                dataLine = nil
            }
        }
    }
}
