import Foundation
import os

/// Snapshot storage shared by the app and the widget extension.
///
/// This is the app-extension-safe half of `WidgetSnapshotManager`. It reads and writes the JSON
/// files in the App Group container (`group.resolvingpoint.momentra/Snapshots/`), so both targets compile
/// the identical access code instead of the extension reimplementing the format and drifting.
///
/// Format contract, unchanged by this split:
///   - one JSON object per file, `Codable`, encoded with `JSONEncoder()`
///   - filename is `<kind>-<id>.json`, e.g. `business-pulse-<momentId>.json`,
///     `business-weekly-<momentId>-7d.json`, `khata-<companyId>.json`
///   - every snapshot carries `updatedAt` as Unix seconds (`Date().timeIntervalSince1970`)
enum WidgetSnapshotStore {
    static let appGroupId = "group.resolvingpoint.momentra"

    private static let logger = Logger(subsystem: "com.momentra.app", category: "WidgetSnapshot")

    /// App Group container, resolved once for the process lifetime.
    static let containerURL: URL? = {
        FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: appGroupId)
    }()

    /// `Snapshots/` directory inside the App Group container (nil if unavailable).
    private static let snapshotsDirectory: URL? = {
        containerURL?.appendingPathComponent("Snapshots", isDirectory: true)
    }()

    private static let directoryLock = NSLock()
    private static var didPrepareDirectory = false

    /// Create `Snapshots/` once per process, protected and excluded from backup.
    private static func preparedSnapshotsDirectory() -> URL? {
        guard let dir = snapshotsDirectory else {
            logger.error("App Group container unavailable (\(self.appGroupId, privacy: .public))")
            return nil
        }

        directoryLock.lock()
        defer { directoryLock.unlock() }

        if !didPrepareDirectory {
            do {
                try FileManager.default.createDirectory(
                    at: dir,
                    withIntermediateDirectories: true,
                    attributes: [.protectionKey: FileProtectionType.completeUntilFirstUserAuthentication]
                )
                try excludeFromBackup(dir)
                didPrepareDirectory = true
            } catch {
                logger.error(
                    "Failed to prepare Snapshots directory: \(error.localizedDescription, privacy: .public)"
                )
                return nil
            }
        }
        return dir
    }

    private static func excludeFromBackup(_ url: URL) throws {
        var values = URLResourceValues()
        values.isExcludedFromBackup = true
        var mutable = url
        try mutable.setResourceValues(values)
    }

    /// Log a read failure from a caller that resolves its own path (e.g. the outbox mirror).
    static func logReadFailure(_ error: Error, what: String) {
        logger.error(
            "Failed to read \(what, privacy: .public): \(error.localizedDescription, privacy: .public)"
        )
    }

    /// Atomically write one snapshot file. Failures are logged, never swallowed.
    static func write<T: Encodable>(_ value: T, filename: String) {
        guard let dir = preparedSnapshotsDirectory() else { return }
        let url = dir.appendingPathComponent(filename)
        do {
            let data = try JSONEncoder().encode(value)
            try data.write(
                to: url,
                options: [.atomic, .completeFileProtectionUntilFirstUserAuthentication]
            )
            try excludeFromBackup(url)
        } catch {
            logger.error(
                "Failed to write widget snapshot \(filename, privacy: .public): \(error.localizedDescription, privacy: .public)"
            )
        }
    }

    /// Decode one snapshot file. A missing file is `nil` (the normal "never written" case).
    static func read<T: Decodable>(filename: String) -> T? {
        guard let dir = snapshotsDirectory else { return nil }
        let url = dir.appendingPathComponent(filename)
        do {
            let data = try Data(contentsOf: url)
            return try JSONDecoder().decode(T.self, from: data)
        } catch let error as NSError
            where error.domain == NSCocoaErrorDomain && error.code == NSFileReadNoSuchFileError
        {
            return nil
        } catch {
            logger.error(
                "Failed to read widget snapshot \(filename, privacy: .public): \(error.localizedDescription, privacy: .public)"
            )
            return nil
        }
    }
}
