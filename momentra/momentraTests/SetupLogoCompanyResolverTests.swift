import Testing
@testable import momentra

struct SetupLogoCompanyResolverTests {
    @Test func preferExistingUsesPassedCompanyFirst() {
        let id = SetupLogoCompanyResolver.preferExisting(
            passedCompanyId: "company-a",
            momentLinkedCompanyId: "company-b"
        )
        #expect(id == "company-a")
    }

    @Test func preferExistingFallsBackToMomentLinked() {
        let id = SetupLogoCompanyResolver.preferExisting(
            passedCompanyId: "  ",
            momentLinkedCompanyId: "company-b"
        )
        #expect(id == "company-b")
    }

    @Test func resolveForUploadWouldCreateWhenMissing() {
        let decision = SetupLogoCompanyResolver.resolveForUpload(
            passedCompanyId: nil,
            momentLinkedCompanyId: nil
        )
        #expect(decision == .wouldCreate)
    }

    @Test func resolveForUploadUsesExisting() {
        let decision = SetupLogoCompanyResolver.resolveForUpload(
            passedCompanyId: nil,
            momentLinkedCompanyId: "company-c"
        )
        #expect(decision == .useExisting("company-c"))
    }
}
