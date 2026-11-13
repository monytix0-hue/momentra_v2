import Foundation

/// Mints a company join code for a role, once.
///
/// The invite system's failure mode is proliferation: every role tap minted a fresh code with a
/// random idempotency key, so an owner tapping through the roles left several live codes behind —
/// one of them granting `FINANCE`. A stable key per company+role makes a retry after a dropped
/// response return the code the server already minted instead of creating a second one, and the
/// cache keeps a role's code stable for the life of the session.
///
/// It does not revoke. An already minted code stays live until the backend learns to revoke it,
/// or until role moves from mint-time to redemption-time. `retire` exists so call sites are ready
/// for that change: it only frees the idempotency key, so the next mint asks for a different code.
actor CompanyInviteMinter {
    static let shared = CompanyInviteMinter()

    private let client: APIClient
    private let idempotency: IdempotencyKeyStore
    private var cache: [String: CompanyInvite] = [:]
    private var inFlight: [String: Task<CompanyInvite, Error>] = [:]

    init(client: APIClient = .shared, idempotency: IdempotencyKeyStore = IdempotencyKeyStore()) {
        self.client = client
        self.idempotency = idempotency
    }

    private static func slot(companyId: String, operatingRole: String) -> String {
        "company-invite:\(companyId):\(operatingRole.uppercased())"
    }

    /// The code for `operatingRole`: minted on first use, then cached. Concurrent callers for the
    /// same role share one request rather than racing.
    func invite(
        companyId: String,
        membershipType: String,
        operatingRole: String
    ) async throws -> CompanyInvite {
        let slot = Self.slot(companyId: companyId, operatingRole: operatingRole)
        if let cached = cache[slot] { return cached }
        if let running = inFlight[slot] { return try await running.value }

        let client = self.client
        let key = idempotency.keyFor(draftKey: slot)
        let task = Task<CompanyInvite, Error> {
            try await client.mintCompanyInvite(
                companyId: companyId,
                membershipType: membershipType,
                operatingRole: operatingRole,
                idempotencyKey: key
            )
        }
        inFlight[slot] = task
        do {
            let minted = try await task.value
            cache[slot] = minted
            inFlight[slot] = nil
            return minted
        } catch {
            inFlight[slot] = nil
            throw error
        }
    }

    /// Drops the cached code and retires the idempotency key, so the next `invite` for this role
    /// mints a different one. Used by an explicit retry, never by a role change.
    func retire(companyId: String, operatingRole: String) {
        let slot = Self.slot(companyId: companyId, operatingRole: operatingRole)
        cache[slot] = nil
        idempotency.clear(draftKey: slot)
    }
}
