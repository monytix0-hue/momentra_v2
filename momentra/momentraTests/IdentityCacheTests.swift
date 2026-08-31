import Foundation
import Testing
@testable import momentra

struct IdentityCacheTests {
    @Test func rejectsFirebaseUidAsMomentraUserIdShape() {
        // Momentra user ids are UUIDs; Firebase UIDs are not.
        let firebaseUid = "abc123FirebaseUidNotUuid"
        let momentraId = "a4548cbf-57d8-5e31-a5c7-6b118588a932"
        #expect(UUID(uuidString: firebaseUid) == nil)
        #expect(UUID(uuidString: momentraId) != nil)
    }

    @Test func identityCacheRoundTripClearsPerUser() {
        let fb = "test-fb-\(UUID().uuidString)"
        MomentraIdentityCache.save(
            firebaseUid: fb,
            userId: "11111111-2222-3333-4444-555555555555",
            email: "a@b.co",
            displayName: "Ada"
        )
        let loaded = MomentraIdentityCache.load(firebaseUid: fb)
        #expect(loaded?.userId == "11111111-2222-3333-4444-555555555555")
        MomentraIdentityCache.clear(firebaseUid: fb)
        #expect(MomentraIdentityCache.load(firebaseUid: fb) == nil)
    }
}
