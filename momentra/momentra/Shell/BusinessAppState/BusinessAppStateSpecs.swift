import SwiftUI

/// The eight screens covered by the shared Figma app-state kit.
enum BusinessAppStateScreen: String, CaseIterable {
    case pulse
    case moments
    case life
    case memory
    case hub
    case viewAll
    case noCompany
    case offline

    /// Bottom-bar destination this screen belongs to, if any.
    var destinationLabel: String? {
        switch self {
        case .pulse: return "Pulse"
        case .moments: return "Moments"
        case .life: return "Life"
        case .memory: return "Memory"
        case .hub: return "Quick adds"
        case .viewAll, .noCompany, .offline: return nil
        }
    }
}

/// The four states each screen can be in.
enum BusinessAppStatePhase: String {
    case empty
    case loading
    case error
    case setup
}

/// How a row's value should be toned.
enum AppStateValueTone {
    /// Normal white value.
    case normal
    /// Amber — "Not recorded" / "Hidden until online". Never renders 0.
    case warning
}

struct AppStateRow: Equatable {
    let label: String
    let value: String
    var tone: AppStateValueTone = .normal
}

/// The lucide glyph Figma places inside the hero badge (18pt, white stroke).
enum AppStateIcon: String, CaseIterable {
    case sparkle
    case moments
    case sun
    case memory
    case plus
    case check
    case alert
    case offline

    /// SF Symbol equivalent, tinted white at 18pt inside the badge.
    var systemName: String {
        switch self {
        case .sparkle: return "sparkles"
        case .moments: return "bolt.fill"
        case .sun: return "sun.max"
        case .memory: return "clock.arrow.circlepath"
        case .plus: return "plus"
        case .check: return "checkmark"
        case .alert: return "exclamationmark.triangle"
        case .offline: return "wifi.slash"
        }
    }
}

/// One capture tile in the Hub grid.
struct HubTileSpec: Identifiable {
    /// Lucide glyph shown above the label (BusinessIcons asset, tinted white).
    let icon: BusinessIcon
    let label: String
    var id: String { label }
}

struct BusinessAppStateSpec {
    let screen: BusinessAppStateScreen
    let phase: BusinessAppStatePhase
    let icon: AppStateIcon
    let title: String
    let body: String
    var rows: [AppStateRow] = []
    var eyebrow: String? = nil
    var primaryLabel: String? = nil
    var secondaryLabel: String? = nil
    var errorCode: String? = nil
    var footnote: String? = nil
}

/// Copy and row data for every state in the Figma kit (node 2666:32397).
///
/// The kit's governing rule: a missing figure is never rendered as 0. Rows either say
/// "Not recorded" / "None yet" / "Quiet" or the row is omitted entirely.
enum BusinessAppStateSpecs {

    static let footnote = "Nothing was lost. Values stay hidden until they reload — never shown as 0."

    /// Banner text shown above the ring on the offline state.
    static let offlineBanner = "You're offline · showing last known"

    /// Hub tile labels are per-vertical text overrides (Figma "Hub label overrides").
    ///
    /// Keyed on the business *vertical*, not the accent — the accent is unified across verticals,
    /// but the Services and Marketing label sets still differ.
    ///
    /// Figma's six Hub tiles, in frame order: Sale, Purchase, Collection, Payment, Expense, Stock.
    /// The glyph set is fixed; only the two copy-override verticals relabel them.
    static func hubTileLabels(vertical: BusinessAppStateVertical) -> [HubTileSpec] {
        let glyphs: [BusinessIcon] = [.camera, .receipt, .clipboardList, .banknote, .calculator, .package]
        let labels: [String] = switch vertical {
        case .services: ["Invoice", "Purchase", "Collection", "Payment", "Expense", "Appointment"]
        case .marketing: ["Client invoice", "Vendor bill", "Collection", "Payment", "Expense", "Campaign"]
        default: ["Sale", "Purchase", "Collection", "Payment", "Expense", "Stock"]
        }
        return zip(glyphs, labels).map { HubTileSpec(icon: $0, label: $1) }
    }

    /// "OCTOBER 2026" — Memory eyebrow.
    static func monthLabel(_ now: Date = Date()) -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "MMMM yyyy"
        return formatter.string(from: now).uppercased()
    }

    /// "TODAY · 31 OCT" — Life eyebrow.
    static func todayLabel(_ now: Date = Date()) -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "d MMM"
        let day = formatter.string(from: now).uppercased()
        return "TODAY · \(day)"
    }

    static func spec(
        _ screen: BusinessAppStateScreen,
        _ phase: BusinessAppStatePhase,
        now: Date = Date()
    ) -> BusinessAppStateSpec {
        switch screen {
        case .pulse: return pulse(phase)
        case .moments: return moments(phase)
        case .life: return life(phase, now: now)
        case .memory: return memory(phase, now: now)
        case .hub: return hub(phase)
        case .viewAll: return viewAll(phase)
        case .noCompany: return noCompany(phase)
        case .offline: return offline(phase)
        }
    }

    private static func pulse(_ phase: BusinessAppStatePhase) -> BusinessAppStateSpec {
        switch phase {
        case .empty:
            return BusinessAppStateSpec(
                screen: .pulse,
                phase: phase,
                icon: .plus,
                title: "No pulse yet",
                body: "Nothing has been recorded for your business this period. Pulse fills in as sales, collections and stock land.",
                rows: [
                    .init(label: "Sales & collections", value: "Not recorded", tone: .warning),
                    .init(label: "Needs attention", value: "Nothing open"),
                    .init(label: "GST position", value: "Not recorded", tone: .warning),
                ],
                eyebrow: "WHAT SHOWS UP HERE",
                primaryLabel: "Record first sale",
                secondaryLabel: "Open Quick adds"
            )
        case .loading:
            return BusinessAppStateSpec(
                screen: .pulse, phase: phase, icon: .sparkle,
                title: "Loading Pulse…", body: ""
            )
        case .error:
            return BusinessAppStateSpec(
                screen: .pulse, phase: phase, icon: .alert,
                title: "Couldn't load Pulse",
                body: "We couldn't reach your business data. Check your connection and try again.",
                primaryLabel: "Retry",
                secondaryLabel: "Open Quick adds",
                errorCode: "PULSE_FETCH_FAILED",
                footnote: footnote
            )
        case .setup:
            return BusinessAppStateSpec(
                screen: .pulse, phase: .empty, icon: .plus,
                title: "No pulse yet",
                body: "Nothing has been recorded for your business this period."
            )
        }
    }

    private static func moments(_ phase: BusinessAppStatePhase) -> BusinessAppStateSpec {
        switch phase {
        case .empty:
            return BusinessAppStateSpec(
                screen: .moments, phase: phase, icon: .moments,
                title: "No moment details yet",
                body: "Your moment is live, but nothing is attached yet. Orders, stock and people appear here as you add them.",
                rows: [
                    .init(label: "Orders & commitments", value: "None yet"),
                    .init(label: "Stock & products", value: "None yet"),
                    .init(label: "People & network", value: "Just you"),
                ],
                eyebrow: "A MOMENT HOLDS",
                primaryLabel: "Add first order",
                secondaryLabel: "Invite your team"
            )
        case .loading:
            return BusinessAppStateSpec(
                screen: .moments, phase: phase, icon: .sparkle,
                title: "Loading Moments…", body: ""
            )
        case .error:
            return BusinessAppStateSpec(
                screen: .moments, phase: phase, icon: .alert,
                title: "Couldn't load Moments",
                body: "The moment is safe — we just couldn't fetch its details right now.",
                primaryLabel: "Retry",
                secondaryLabel: "Switch moment",
                errorCode: "MOMENT_DETAIL_FAILED",
                footnote: footnote
            )
        case .setup:
            return BusinessAppStateSpec(
                screen: .moments, phase: .empty, icon: .moments,
                title: "No moment details yet",
                body: "Your moment is live, but nothing is attached yet."
            )
        }
    }

    private static func life(_ phase: BusinessAppStatePhase, now: Date) -> BusinessAppStateSpec {
        switch phase {
        case .empty:
            return BusinessAppStateSpec(
                screen: .life, phase: phase, icon: .sun,
                title: "Nothing moving today",
                body: "A calm day. No dispatches, collections or people activity yet — Life updates the moment something moves.",
                rows: [
                    .init(label: "Money moving", value: "Nothing yet"),
                    .init(label: "Dealers & channels", value: "Quiet"),
                    .init(label: "People & activity", value: "Quiet"),
                ],
                eyebrow: todayLabel(now),
                primaryLabel: "Open Quick adds",
                secondaryLabel: "Back to Pulse"
            )
        case .loading:
            return BusinessAppStateSpec(
                screen: .life, phase: phase, icon: .sparkle,
                title: "Loading Life…", body: ""
            )
        case .error:
            return BusinessAppStateSpec(
                screen: .life, phase: phase, icon: .alert,
                title: "Couldn't load Life",
                body: "Today's activity couldn't be fetched. Your records are safe.",
                primaryLabel: "Retry",
                secondaryLabel: "Back to Pulse",
                errorCode: "LIFE_FEED_FAILED",
                footnote: footnote
            )
        case .setup:
            return BusinessAppStateSpec(
                screen: .life, phase: .empty, icon: .sun,
                title: "Nothing moving today",
                body: "A calm day. Life updates the moment something moves.",
                eyebrow: todayLabel(now)
            )
        }
    }

    private static func memory(_ phase: BusinessAppStatePhase, now: Date) -> BusinessAppStateSpec {
        let month = monthLabel(now)
        switch phase {
        case .empty:
            return BusinessAppStateSpec(
                screen: .memory, phase: phase, icon: .memory,
                title: "No Memory yet",
                body: "No closed activity for \(month) yet. Memory writes itself as days close — nothing is estimated.",
                rows: [
                    .init(label: "Days closed", value: "None yet", tone: .warning),
                    .init(label: "Money memory", value: "Not recorded", tone: .warning),
                    .init(label: "Story", value: "Needs closed days"),
                ],
                eyebrow: month,
                primaryLabel: "Pick another month",
                secondaryLabel: "Back to Pulse"
            )
        case .loading:
            return BusinessAppStateSpec(
                screen: .memory, phase: phase, icon: .sparkle,
                title: "Loading Memory…", body: ""
            )
        case .error:
            return BusinessAppStateSpec(
                screen: .memory, phase: phase, icon: .alert,
                title: "Couldn't load Memory",
                body: "Your history is intact — this month just didn't load.",
                primaryLabel: "Retry",
                secondaryLabel: "Back to Pulse",
                errorCode: "MEMORY_FETCH_FAILED",
                footnote: footnote
            )
        case .setup:
            return BusinessAppStateSpec(
                screen: .memory, phase: .empty, icon: .memory,
                title: "No Memory yet",
                body: "Memory writes itself as days close — nothing is estimated.",
                eyebrow: month
            )
        }
    }

    private static func hub(_ phase: BusinessAppStatePhase) -> BusinessAppStateSpec {
        switch phase {
        case .empty:
            return BusinessAppStateSpec(
                screen: .hub, phase: phase, icon: .plus,
                title: "Start with a capture",
                body: "Nothing recorded yet. One sale, purchase or stock update is enough to light up Pulse.",
                footnote: "Tap Sale to begin — the rest unlock as you go."
            )
        case .error:
            return BusinessAppStateSpec(
                screen: .hub, phase: phase, icon: .alert,
                title: "Couldn't load Quick adds",
                body: "Capture forms couldn't load. Nothing you entered was lost.",
                primaryLabel: "Retry",
                secondaryLabel: "Back to Pulse",
                errorCode: "HUB_FETCH_FAILED",
                footnote: footnote
            )
        case .loading, .setup:
            return BusinessAppStateSpec(
                screen: .hub, phase: .empty, icon: .plus,
                title: "Start with a capture",
                body: "Nothing recorded yet. One sale, purchase or stock update is enough to light up Pulse."
            )
        }
    }

    private static func viewAll(_ phase: BusinessAppStatePhase) -> BusinessAppStateSpec {
        BusinessAppStateSpec(
            screen: .viewAll,
            phase: (phase == .loading || phase == .error) ? .empty : phase,
            icon: .check,
            title: "Nothing here",
            body: "No open items under “Open”. Everything that needed attention is handled — or nothing has been recorded yet.",
            eyebrow: "PULSE · VIEW ALL",
            primaryLabel: nil,
            secondaryLabel: "Show all items",
            footnote: "Filter: Open · Memory keeps closed items"
        )
    }

    private static func noCompany(_ phase: BusinessAppStatePhase) -> BusinessAppStateSpec {
        BusinessAppStateSpec(
            screen: .noCompany,
            phase: .setup,
            icon: .plus,
            title: "Add your business to open Pulse",
            body: "Pulse, Moments, Life and Memory need a business first. Takes about two minutes — GSTIN optional.",
            rows: [
                .init(label: "Business name & type", value: "Step 1"),
                .init(label: "Financial year & currency", value: "Step 2"),
                .init(label: "Invite team (optional)", value: "Step 3"),
            ],
            eyebrow: "YOU'LL SET UP",
            primaryLabel: "Create business",
            secondaryLabel: "Join with invite code"
        )
    }

    private static func offline(_ phase: BusinessAppStatePhase) -> BusinessAppStateSpec {
        BusinessAppStateSpec(
            screen: .offline,
            phase: .error,
            icon: .offline,
            title: "Waiting for connection",
            body: "Last-known Pulse is shown read-only. Captures queue on this device and sync when you're back.",
            rows: [
                .init(label: "Last synced", value: "—"),
                .init(label: "Queued captures", value: "None"),
                .init(label: "Live figures", value: "Hidden until online", tone: .warning),
            ],
            eyebrow: "LAST KNOWN",
            primaryLabel: "Retry"
        )
    }
}
