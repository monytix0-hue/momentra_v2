import Foundation

enum AppContextKind: String, CaseIterable, Identifiable {
    case personal = "PERSONAL"
    case group = "GROUP"
    case business = "BUSINESS"
    case circle = "CIRCLE"

    var id: String { rawValue }

    var label: String {
        switch self {
        case .personal: return "Personal"
        case .group: return "Group"
        case .business: return "Business"
        case .circle: return "Circle"
        }
    }
}

enum BottomDestination: String, CaseIterable, Identifiable {
    case pulse, moments, create, life, memory

    var id: String { rawValue }

    var label: String {
        switch self {
        case .pulse: return "Pulse"
        case .moments: return "Moments"
        case .create: return "Create"
        case .life: return "Life"
        case .memory: return "Memory"
        }
    }

    /// Label under bottom-nav tab icons — matches APK `BottomDestination.label` extension.
    var shellNavLabel: String {
        switch self {
        case .pulse: return "Pulse"
        case .moments: return "Moments"
        case .create: return "quickadds"
        case .life: return "Life"
        case .memory: return "Memory"
        }
    }

    /// Figma-exported asset names (template-rendered in tab bar, except Create).
    var tabAssetName: String {
        switch self {
        case .pulse: return "NavPulse"
        case .moments: return "NavMoments"
        case .create: return "NavCreate"
        case .life: return "NavLife"
        case .memory: return "NavMemory"
        }
    }

    var systemImage: String {
        switch self {
        case .pulse: return "waveform.path.ecg"
        case .moments: return "square.grid.2x2"
        case .create: return "plus.circle.fill"
        case .life: return "heart"
        case .memory: return "sparkles"
        }
    }
}

/// Nested Create flow for Group (chooser → section setup wizards).
enum GroupCreatePhase: String, Equatable {
    case chooser
    case experienceSetup
    case purchaseSetup
    case livingSetup
}

enum AuthPhase: Equatable {
    case launching
    case restoringSession
    case signedOut
    case authenticating
    case authenticatedBootstrapping
    case authenticated
    case sessionExpired
    case authError
}

struct ShellIdentity: Equatable {
    let userId: String
    let displayName: String?
    let email: String?
    let firebaseUid: String?
}

struct CompanySummary: Equatable, Identifiable {
    let companyId: String
    let displayName: String
    var id: String { companyId }
}

enum ShellContentState: Equatable {
    case idle
    case loading
    case empty
    case ready(detail: String?)
    case error(code: String?, message: String)
    case forbidden
    case offline
    case deferred
}
