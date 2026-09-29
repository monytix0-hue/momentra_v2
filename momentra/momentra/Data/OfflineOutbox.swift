import Foundation

enum OfflineReplay {
    case synced(String?)
    case offline
    case unauthorized
    case rejected(String?)
}

struct OfflineCommand: Codable {
    var id: String
    var path: String
    var momentId: String
    var idempotencyKey: String
    var bodyJson: String
    var createdAt: TimeInterval
    var status: String
    var error: String?
    var method: String?
    var parentCommandId: String?
    var fileName: String?
}

/// Per-user file of writes waiting for a connection.
final class OfflineOutbox {
    static let shared = OfflineOutbox()
    static let localId = "offline"

    private let lock = NSLock()
    private var userId: String?
    private var lastQueuedCommandId: String?
    private var bootstrapRefresh = false
    var onChanged: (() -> Void)?

    private init() {}

    func bindUser(_ userId: String) {
        guard !userId.isEmpty else { return }
        lock.lock()
        self.userId = userId
        lock.unlock()
    }

    /// Last successful member list for a moment, used when the network read fails.
    func saveRoster(momentId: String, participants: [APIClient.GroupParticipantPayload]) {
        lock.lock()
        defer { lock.unlock() }
        guard let userId, !momentId.isEmpty else { return }
        var map = loadRoster(userId)
        map[momentId] = participants
        saveRoster(userId, map)
    }

    func roster(momentId: String) -> [APIClient.GroupParticipantPayload]? {
        lock.lock()
        defer { lock.unlock() }
        guard let userId else { return nil }
        return loadRoster(userId)[momentId]
    }

    func enqueue(
        path: String,
        momentId: String,
        idempotencyKey: String,
        bodyJson: String,
        method: String = "POST",
        parentCommandId: String? = nil
    ) {
        lock.lock()
        defer { lock.unlock() }
        guard let userId else { return }
        var items = load(userId)
        if let existing = items.first(where: { $0.idempotencyKey == idempotencyKey && $0.status == "pending" }) {
            lastQueuedCommandId = existing.id
            return
        }
        let id = UUID().uuidString
        items.append(
            OfflineCommand(
                id: id,
                path: path,
                momentId: momentId,
                idempotencyKey: idempotencyKey,
                bodyJson: bodyJson,
                createdAt: Date().timeIntervalSince1970,
                status: "pending",
                error: nil,
                method: method,
                parentCommandId: parentCommandId,
                fileName: nil
            )
        )
        save(userId, items)
        lastQueuedCommandId = id
        let changed = onChanged
        DispatchQueue.main.async { changed?() }
    }

    /// Store photo bytes and attach them after the parent command syncs when the resource id is still local.
    func enqueueMedia(
        attachPath: String,
        momentId: String,
        bytes: Data,
        contentType: String,
        scopeId: String,
        resourceId: String
    ) {
        lock.lock()
        defer { lock.unlock() }
        guard let userId else { return }
        let id = UUID().uuidString
        guard let url = mediaURL(id) else { return }
        try? FileManager.default.createDirectory(at: url.deletingLastPathComponent(), withIntermediateDirectories: true)
        try? bytes.write(to: url, options: .atomic)
        let body: [String: Any] = [
            "contentType": contentType,
            "byteSize": bytes.count,
            "scopeType": "MOMENT",
            "scopeId": scopeId,
        ]
        let bodyJson = String(data: (try? JSONSerialization.data(withJSONObject: body)) ?? Data(), encoding: .utf8) ?? "{}"
        var items = load(userId)
        items.append(
            OfflineCommand(
                id: id,
                path: attachPath,
                momentId: momentId,
                idempotencyKey: UUID().uuidString,
                bodyJson: bodyJson,
                createdAt: Date().timeIntervalSince1970,
                status: "pending",
                error: nil,
                method: "MEDIA",
                parentCommandId: resourceId == Self.localId ? lastQueuedCommandId : nil,
                fileName: id
            )
        )
        save(userId, items)
        let changed = onChanged
        DispatchQueue.main.async { changed?() }
    }

    func pendingCount() -> Int {
        lock.lock()
        defer { lock.unlock() }
        guard let userId else { return 0 }
        return load(userId).filter { $0.status == "pending" }.count
    }

    func failedMessage() -> String? {
        lock.lock()
        defer { lock.unlock() }
        guard let userId else { return nil }
        return load(userId).first { $0.status == "failed" }?.error
    }

    func takeBootstrapRefresh() -> Bool {
        lock.lock()
        defer { lock.unlock() }
        let value = bootstrapRefresh
        bootstrapRefresh = false
        return value
    }

    func flush() async -> Int {
        guard let pending = snapshotPending() else { return 0 }
        let uid = pending.uid
        let snapshot = pending.items

        var synced = 0
        var remaining: [OfflineCommand] = []
        var deferred: [OfflineCommand] = []
        var createdIds: [String: String] = [:]
        var outcomes: [String: String] = [:]
        var stop = false
        var needsBootstrap = false

        for item in snapshot {
            if stop {
                remaining.append(item)
                outcomes[item.id] = item.status == "failed" ? "failed" : "stopped"
                continue
            }
            let step = await step(item, allowDefer: true, createdIds: createdIds, outcomes: outcomes, all: snapshot)
            apply(
                step,
                synced: &synced,
                remaining: &remaining,
                deferred: &deferred,
                createdIds: &createdIds,
                outcomes: &outcomes,
                stop: &stop,
                needsBootstrap: &needsBootstrap
            )
        }
        let waiting = deferred
        deferred = []
        for item in waiting {
            if stop {
                remaining.append(item)
                continue
            }
            let step = await step(item, allowDefer: false, createdIds: createdIds, outcomes: outcomes, all: snapshot)
            apply(
                step,
                synced: &synced,
                remaining: &remaining,
                deferred: &deferred,
                createdIds: &createdIds,
                outcomes: &outcomes,
                stop: &stop,
                needsBootstrap: &needsBootstrap
            )
        }

        commitFlush(uid: uid, remaining: remaining, needsBootstrap: needsBootstrap)
        return synced
    }

    /// Synchronous so the lock is never taken from an async function.
    private func snapshotPending() -> (uid: String, items: [OfflineCommand])? {
        lock.lock()
        defer { lock.unlock() }
        guard let userId else { return nil }
        return (userId, load(userId))
    }

    private func commitFlush(uid: String, remaining: [OfflineCommand], needsBootstrap: Bool) {
        lock.lock()
        defer { lock.unlock() }
        if needsBootstrap { bootstrapRefresh = true }
        save(uid, remaining)
    }

    private enum Step {
        case kept(OfflineCommand, outcome: String)
        case deferred(OfflineCommand)
        case synced(commandId: String, resourceId: String?, bootstrap: Bool)
        case stopped(OfflineCommand)
    }

    private func step(
        _ item: OfflineCommand,
        allowDefer: Bool,
        createdIds: [String: String],
        outcomes: [String: String],
        all: [OfflineCommand]
    ) async -> Step {
        if item.status == "failed" { return .kept(item, outcome: "failed") }
        let prepared = prepare(item, createdIds: createdIds, outcomes: outcomes, all: all)
        switch prepared.kind {
        case "defer":
            if allowDefer { return .deferred(item) }
            return .kept(item, outcome: "stopped")
        case "fail":
            var failed = item
            failed.status = "failed"
            failed.error = prepared.message
            return .kept(failed, outcome: "failed")
        default:
            break
        }
        var command = prepared.command
        let bytes = readMedia(command.fileName)
        if (command.method ?? "POST") == "MEDIA", bytes == nil {
            command.status = "failed"
            command.error = "The saved photo is missing on this device."
            return .kept(command, outcome: "failed")
        }
        switch await APIClient.shared.replayQueued(command, media: bytes) {
        case .synced(let body):
            deleteMedia(command.fileName)
            return .synced(
                commandId: command.id,
                resourceId: Self.createdResourceId(from: body),
                bootstrap: Self.needsBootstrap(command.path)
            )
        case .offline, .unauthorized:
            return .stopped(command)
        case .rejected(let message):
            if (message ?? "").lowercased().contains("already deleted") {
                deleteMedia(command.fileName)
                return .synced(
                    commandId: command.id,
                    resourceId: nil,
                    bootstrap: Self.needsBootstrap(command.path)
                )
            }
            command.status = "failed"
            command.error = message ?? "Could not sync a saved change."
            return .kept(command, outcome: "failed")
        }
    }

    private func apply(
        _ step: Step,
        synced: inout Int,
        remaining: inout [OfflineCommand],
        deferred: inout [OfflineCommand],
        createdIds: inout [String: String],
        outcomes: inout [String: String],
        stop: inout Bool,
        needsBootstrap: inout Bool
    ) {
        switch step {
        case .kept(let item, let outcome):
            remaining.append(item)
            outcomes[item.id] = outcome
        case .deferred(let item):
            deferred.append(item)
        case .synced(let commandId, let resourceId, let bootstrap):
            synced += 1
            outcomes[commandId] = "synced"
            if let resourceId { createdIds[commandId] = resourceId }
            if bootstrap { needsBootstrap = true }
        case .stopped(let item):
            remaining.append(item)
            outcomes[item.id] = "stopped"
            stop = true
        }
    }

    private struct Prepared {
        var kind: String
        var command: OfflineCommand
        var message: String?
    }

    private func prepare(
        _ item: OfflineCommand,
        createdIds: [String: String],
        outcomes: [String: String],
        all: [OfflineCommand]
    ) -> Prepared {
        var ready = item
        ready.method = item.method ?? "POST"
        guard let parent = item.parentCommandId, item.path.contains("/\(Self.localId)/") else {
            return Prepared(kind: "ready", command: ready, message: nil)
        }
        if let id = createdIds[parent] {
            ready.path = item.path.replacingOccurrences(of: "/\(Self.localId)/", with: "/\(id)/")
            return Prepared(kind: "ready", command: ready, message: nil)
        }
        switch outcomes[parent] {
        case "failed":
            return Prepared(kind: "fail", command: item, message: "The saved record could not sync, so its photo stayed on this device.")
        case "stopped":
            return Prepared(kind: "defer", command: item, message: nil)
        case "synced":
            return Prepared(kind: "fail", command: item, message: "The saved record synced without an id, so its photo could not be attached.")
        default:
            if all.contains(where: { $0.id == parent }) {
                return Prepared(kind: "defer", command: item, message: nil)
            }
            return Prepared(kind: "fail", command: item, message: "The saved record is gone, so its photo could not be attached.")
        }
    }

    private static func createdResourceId(from body: String?) -> String? {
        guard let body, let data = body.data(using: .utf8),
              let obj = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else { return nil }
        let dataObj = obj["data"] as? [String: Any] ?? obj
        for key in ["memoryId", "expenseId", "momentId"] {
            if let value = dataObj[key] as? String, !value.isEmpty { return value }
        }
        return nil
    }

    private static func needsBootstrap(_ path: String) -> Bool {
        if path == "v1/moments" { return true }
        if path.hasPrefix("v1/group/invites/"), path.hasSuffix("/redeem") { return true }
        if path.hasPrefix("v1/company/invites/"), path.hasSuffix("/redeem") { return true }
        if path.range(of: #"^v1/moments/[^/]+$"#, options: .regularExpression) != nil { return true }
        if path.range(of: #"^v1/moments/[^/]+/delete$"#, options: .regularExpression) != nil { return true }
        return false
    }

    private func file(_ userId: String) -> URL? {
        guard let dir = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first else {
            return nil
        }
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return dir.appendingPathComponent("offline-outbox-\(userId).json")
    }

    private func rosterFile(_ userId: String) -> URL? {
        guard let dir = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first else {
            return nil
        }
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return dir.appendingPathComponent("group-roster-\(userId).json")
    }

    private func loadRoster(_ userId: String) -> [String: [APIClient.GroupParticipantPayload]] {
        guard let url = rosterFile(userId), let data = try? Data(contentsOf: url) else { return [:] }
        return (try? JSONDecoder().decode([String: [APIClient.GroupParticipantPayload]].self, from: data)) ?? [:]
    }

    private func saveRoster(_ userId: String, _ map: [String: [APIClient.GroupParticipantPayload]]) {
        guard let url = rosterFile(userId), let data = try? JSONEncoder().encode(map) else { return }
        try? data.write(to: url, options: .atomic)
    }

    private func mediaURL(_ name: String) -> URL? {
        guard let dir = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first else {
            return nil
        }
        return dir.appendingPathComponent("offline-media", isDirectory: true).appendingPathComponent(name)
    }

    private func readMedia(_ name: String?) -> Data? {
        guard let name, let url = mediaURL(name) else { return nil }
        return try? Data(contentsOf: url)
    }

    private func deleteMedia(_ name: String?) {
        guard let name, let url = mediaURL(name) else { return }
        try? FileManager.default.removeItem(at: url)
    }

    private func load(_ userId: String) -> [OfflineCommand] {
        guard let url = file(userId), let data = try? Data(contentsOf: url) else { return [] }
        return (try? JSONDecoder().decode([OfflineCommand].self, from: data)) ?? []
    }

    private func save(_ userId: String, _ items: [OfflineCommand]) {
        guard let url = file(userId), let data = try? JSONEncoder().encode(items) else { return }
        try? data.write(to: url, options: .atomic)
    }
}
