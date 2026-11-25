import Foundation

/// Prefers an already-linked company over creating a second one during HeroCard logo upload.
/// Lockstep with Android `SetupLogoCompanyResolver`.
enum SetupLogoCompanyDecision: Equatable {
    case useExisting(String)
    case wouldCreate
}

enum SetupLogoCompanyResolver {
    static func preferExisting(passedCompanyId: String?, momentLinkedCompanyId: String?) -> String? {
        if let passed = passedCompanyId?.trimmingCharacters(in: .whitespacesAndNewlines), !passed.isEmpty {
            return passed
        }
        if let linked = momentLinkedCompanyId?.trimmingCharacters(in: .whitespacesAndNewlines), !linked.isEmpty {
            return linked
        }
        return nil
    }

    static func resolveForUpload(
        passedCompanyId: String?,
        momentLinkedCompanyId: String?
    ) -> SetupLogoCompanyDecision {
        if let existing = preferExisting(
            passedCompanyId: passedCompanyId,
            momentLinkedCompanyId: momentLinkedCompanyId
        ) {
            return .useExisting(existing)
        }
        return .wouldCreate
    }
}

struct SetupLogoUploadResult {
    let companyId: String
    let mediaId: String
}
