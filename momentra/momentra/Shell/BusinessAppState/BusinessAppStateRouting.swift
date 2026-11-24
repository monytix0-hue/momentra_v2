import Foundation

/// Maps shell destination + content state onto the Obsidian AppState kit.
/// Keep in lockstep with `BusinessAppStateRouting.kt` on Android.
enum BusinessAppStateRouting {
    static func screen(
        for destination: BottomDestination,
        hasCompany: Bool
    ) -> BusinessAppStateScreen {
        if !hasCompany { return .noCompany }
        switch destination {
        case .pulse: return .pulse
        case .moments: return .moments
        case .life: return .life
        case .memory: return .memory
        case .create: return .hub
        }
    }

    static func phase(
        for content: ShellContentState,
        hasCompany: Bool
    ) -> BusinessAppStatePhase {
        if !hasCompany { return .setup }
        switch content {
        case .idle, .loading: return .loading
        case .offline, .error, .forbidden: return .error
        case .empty, .ready, .deferred: return .empty
        }
    }

    /// Offline is a dedicated screen, not the destination's error variant.
    static func resolveScreen(
        destination: BottomDestination,
        content: ShellContentState,
        hasCompany: Bool
    ) -> BusinessAppStateScreen {
        if case .offline = content { return .offline }
        return screen(for: destination, hasCompany: hasCompany)
    }

    static func loadingLabel(for destination: BottomDestination) -> String {
        BusinessAppStateSpecs.spec(screen(for: destination, hasCompany: true), .loading).title
    }

    static func title(
        destination: BottomDestination,
        content: ShellContentState,
        hasCompany: Bool
    ) -> String {
        let resolved = resolveScreen(destination: destination, content: content, hasCompany: hasCompany)
        let resolvedPhase = phase(for: content, hasCompany: hasCompany)
        if resolvedPhase == .loading {
            return loadingLabel(for: destination)
        }
        return BusinessAppStateSpecs.spec(resolved, resolvedPhase).title
    }
}
