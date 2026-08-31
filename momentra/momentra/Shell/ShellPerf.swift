import Foundation
import os

/// S9-J lightweight shell performance marks (source parity with Android ShellPerf).
enum ShellPerf {
    private static let log = Logger(subsystem: "com.momentra", category: "perf")
    private static var lastStore: [String: Int64] = [:]

    struct Mark {
        let name: String
        let startedAt: Date
        init(name: String, startedAt: Date = Date()) {
            self.name = name
            self.startedAt = startedAt
        }
    }

    static func start(_ name: String) -> Mark { Mark(name: name) }

    @discardableResult
    static func end(_ mark: Mark, extras: [String: Any] = [:]) -> Int64 {
        let elapsed = Int64(Date().timeIntervalSince(mark.startedAt) * 1000)
        var parts = ["event=\(mark.name)", "elapsedMs=\(elapsed)"]
        for (k, v) in extras {
            parts.append("\(k)=\(v)")
        }
        log.info("\(parts.joined(separator: " "), privacy: .public)")
        lastStore[mark.name] = elapsed
        return elapsed
    }

    static func instant(_ name: String, extras: [String: Any] = [:]) {
        _ = end(Mark(name: name, startedAt: Date()), extras: extras)
    }

    static var last: [String: Int64] { lastStore }

    static func clear() { lastStore.removeAll() }
}
