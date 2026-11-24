import Foundation
import ActivityKit

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
    static let appGroupId = "group.resolvingpoint.momentra"

    private let lock = NSLock()
    private var userId: String?
    private var lastQueuedCommandId: String?
    private var bootstrapRefresh = false
    private var didMigrateLegacyStorage = false
    var onChanged: (() -> Void)?

    private init() {}

    func bindUser(_ userId: String) {
        guard !userId.isEmpty else { return }
        migrateLegacyStorageIfNeeded()
        lock.lock()
        if self.userId != userId {
            lastQueuedCommandId = nil
            bootstrapRefresh = false
        }
        self.userId = userId
        lock.unlock()
    }

    func saveRoster(momentId: String, participants: [APIClient.GroupParticipantPayload]) {
        lock.lock()
        defer { lock.unlock() }
        guard let userId, !momentId.isEmpty else { return }
        var map = loadRoster(userId)
        map[momentId] = participants
        saveRoster(userId, map)
        // Mirror Business roster publishing: keep the Group widget snapshot in sync.
        let snapshot = GroupRosterWidgetSnapshot(
            members: participants.map {
                GroupRosterWidgetSnapshot.RosterMember(
                    displayName: $0.displayName,
                    membershipType: $0.roleLabel ?? $0.roleCode
                )
            },
            updatedAt: Date().timeIntervalSince1970
        )
        WidgetSnapshotManager.writeGroupRoster(momentId: momentId, payload: snapshot)
    }

    func roster(momentId: String) -> [APIClient.GroupParticipantPayload]? {
        lock.lock()
        defer { lock.unlock() }
        guard let userId else { return nil }
        return loadRoster(userId)[momentId]
    }

    func purge(userId: String) {
        lock.lock()
        defer { lock.unlock() }
        let queued = load(userId)
        for command in queued {
            deleteMedia(command.fileName)
        }
        if let url = file(userId) { try? FileManager.default.removeItem(at: url) }
        if let url = rosterFile(userId) { try? FileManager.default.removeItem(at: url) }
        if self.userId == userId {
            self.userId = nil
            lastQueuedCommandId = nil
            bootstrapRefresh = false
        }
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
        writeProtected(bytes, to: url)
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

        if #available(iOS 16.1, *) {
            await FlushLiveActivity.shared.start(total: snapshot.count)
        }

        var synced = 0
        var remaining: [OfflineCommand] = []
        var deferred: [OfflineCommand] = []
        var createdIds: [String: String] = [:]
        var outcomes: [String: String] = [:]
        var stop = false
        var needsBootstrap = false

        for (index, item) in snapshot.enumerated() {
            if stop {
                remaining.append(item)
                outcomes[item.id] = item.status == "failed" ? "failed" : "stopped"
                continue
            }
            if #available(iOS 16.1, *) {
                let desc = item.fileName != nil ? "Photo #\(index + 1)" : "Command #\(index + 1)"
                await FlushLiveActivity.shared.update(synced: synced, remaining: snapshot.count - synced, current: desc)
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
        for (index, item) in waiting.enumerated() {
            if stop {
                remaining.append(item)
                continue
            }
            if #available(iOS 16.1, *) {
                await FlushLiveActivity.shared.update(synced: synced, remaining: remaining.count + waiting.count - index, current: "Retry #\(index + 1)")
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

        commitFlush(uid: uid, snapshot: snapshot, remaining: remaining, needsBootstrap: needsBootstrap)

        if #available(iOS 16.1, *) {
            await FlushLiveActivity.shared.end()
        }
        return synced
    }

    private func snapshotPending() -> (uid: String, items: [OfflineCommand])? {
        lock.lock()
        defer { lock.unlock() }
        guard let userId else { return nil }
        return (userId, load(userId))
    }

    private func commitFlush(
        uid: String,
        snapshot: [OfflineCommand],
        remaining: [OfflineCommand],
        needsBootstrap: Bool
    ) {
        lock.lock()
        defer { lock.unlock() }
        if needsBootstrap { bootstrapRefresh = true }
        let snapshotIds = Set(snapshot.map(\.id))
        let queuedDuringFlush = load(uid).filter { !snapshotIds.contains($0.id) }
        save(uid, remaining + queuedDuringFlush)
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
            guard mediaExists(command.fileName) else {
                command.status = "failed"
                command.error = "The saved photo is missing on this device."
                return .kept(command, outcome: "failed")
            }
            return .stopped(command)
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

    // MARK: - App Group container

    private func sharedContainer() -> URL? {
        FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: Self.appGroupId)
    }

    // MARK: - Protected, non-backed-up storage

    private func queueDirectory() -> URL? {
        guard let base = sharedContainer() else {
            guard let base = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first else {
                return nil
            }
            let dir = base.appendingPathComponent("OfflineOutbox", isDirectory: true)
            createDirectory(at: dir)
            return dir
        }
        let dir = base.appendingPathComponent("OfflineOutbox", isDirectory: true)
        createDirectory(at: dir)
        return dir
    }

    private func createDirectory(at url: URL) {
        try? FileManager.default.createDirectory(
            at: url,
            withIntermediateDirectories: true,
            attributes: [.protectionKey: FileProtectionType.completeUntilFirstUserAuthentication]
        )
        var values = URLResourceValues()
        values.isExcludedFromBackup = true
        var mutable = url
        try? mutable.setResourceValues(values)
    }

    private func writeProtected(_ data: Data, to url: URL) {
        try? data.write(to: url, options: [.atomic, .completeFileProtectionUntilFirstUserAuthentication])
        var values = URLResourceValues()
        values.isExcludedFromBackup = true
        var mutable = url
        try? mutable.setResourceValues(values)
    }

    private func migrateLegacyStorageIfNeeded() {
        lock.lock()
        defer { lock.unlock() }
        guard !didMigrateLegacyStorage else { return }
        didMigrateLegacyStorage = true
        guard let base = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first,
              let dir = queueDirectory() else { return }
        let fm = FileManager.default
        if let entries = try? fm.contentsOfDirectory(at: base, includingPropertiesForKeys: nil) {
            for url in entries {
                let name = url.lastPathComponent
                guard name.hasPrefix("offline-outbox-") || name.hasPrefix("group-roster-") else { continue }
                let destination = dir.appendingPathComponent(name)
                if !fm.fileExists(atPath: destination.path) {
                    try? fm.moveItem(at: url, to: destination)
                }
            }
        }
        let legacyMedia = base.appendingPathComponent("offline-media", isDirectory: true)
        if fm.fileExists(atPath: legacyMedia.path),
           let destination = mediaDirectory() {
            if let files = try? fm.contentsOfDirectory(at: legacyMedia, includingPropertiesForKeys: nil) {
                for url in files {
                    let target = destination.appendingPathComponent(url.lastPathComponent)
                    if !fm.fileExists(atPath: target.path) {
                        try? fm.moveItem(at: url, to: target)
                    }
                }
            }
            try? fm.removeItem(at: legacyMedia)
        }
    }

    private func file(_ userId: String) -> URL? {
        guard let dir = queueDirectory() else { return nil }
        return dir.appendingPathComponent("offline-outbox-\(userId).json")
    }

    private func rosterFile(_ userId: String) -> URL? {
        guard let dir = queueDirectory() else { return nil }
        return dir.appendingPathComponent("group-roster-\(userId).json")
    }

    private func mediaDirectory() -> URL? {
        guard let dir = queueDirectory() else { return nil }
        let media = dir.appendingPathComponent("media", isDirectory: true)
        createDirectory(at: media)
        return media
    }

    private func loadRoster(_ userId: String) -> [String: [APIClient.GroupParticipantPayload]] {
        guard let url = rosterFile(userId), let data = try? Data(contentsOf: url) else { return [:] }
        return (try? JSONDecoder().decode([String: [APIClient.GroupParticipantPayload]].self, from: data)) ?? [:]
    }

    private func saveRoster(_ userId: String, _ map: [String: [APIClient.GroupParticipantPayload]]) {
        guard let url = rosterFile(userId), let data = try? JSONEncoder().encode(map) else { return }
        writeProtected(data, to: url)
    }

    private func mediaURL(_ name: String) -> URL? {
        guard let dir = mediaDirectory() else { return nil }
        return dir.appendingPathComponent(name)
    }

    private func mediaExists(_ name: String?) -> Bool {
        guard let name, let url = mediaURL(name) else { return false }
        return FileManager.default.fileExists(atPath: url.path)
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
        do {
            return try JSONDecoder().decode([OfflineCommand].self, from: data)
        } catch {
            let aside = url.appendingPathExtension("undecodable")
            try? FileManager.default.removeItem(at: aside)
            try? FileManager.default.copyItem(at: url, to: aside)
            return []
        }
    }

    private func save(_ userId: String, _ items: [OfflineCommand]) {
        guard let url = file(userId), let data = try? JSONEncoder().encode(items) else { return }
        writeProtected(data, to: url)
        writeSnapshot(userId: userId)
    }

    // MARK: - Widget snapshot

    private func writeSnapshot(userId: String) {
        guard let dir = queueDirectory() else { return }
        let items = load(userId)
        let snapshot = OutboxWidgetSnapshot(
            pendingCount: items.filter { $0.status == "pending" }.count,
            failedCount: items.filter { $0.status == "failed" }.count,
            failedMessage: items.first { $0.status == "failed" }?.error,
            totalCount: items.count,
            lastUpdated: Date().timeIntervalSince1970
        )
        let url = dir.appendingPathComponent("outbox-snapshot-\(userId).json")
        if let data = try? JSONEncoder().encode(snapshot) {
            try? data.write(to: url, options: [.atomic])
        }
    }
}

// MARK: - Live Activity Reporter

// `OutboxFlushAttributes` is declared in `OutboxFlushAttributes.swift` — shared with the widget
// extension, which renders the activity.

@available(iOS 16.1, *)
actor FlushLiveActivity {
    static let shared = FlushLiveActivity()
    private var activity: Activity<OutboxFlushAttributes>?

    func start(total: Int) {
        guard ActivityAuthorizationInfo().areActivitiesEnabled else { return }
        let attributes = OutboxFlushAttributes(total: total)
        let state = OutboxFlushAttributes.ContentState(synced: 0, remaining: total, currentItemDescription: "Starting sync…")
        do {
            activity = try Activity.request(
                attributes: attributes,
                content: .init(state: state, staleDate: nil),
                pushType: nil
            )
        } catch {
            activity = nil
        }
    }

    func update(synced: Int, remaining: Int, current: String) {
        guard let activity else { return }
        let state = OutboxFlushAttributes.ContentState(synced: synced, remaining: remaining, currentItemDescription: current)
        Task {
            await activity.update(.init(state: state, staleDate: nil))
        }
    }

    func end() {
        guard let activity else { return }
        let final = OutboxFlushAttributes.ContentState(synced: activity.attributes.total, remaining: 0, currentItemDescription: "Done")
        Task {
            await activity.end(.init(state: final, staleDate: nil), dismissalPolicy: .default)
        }
        self.activity = nil
    }
}
