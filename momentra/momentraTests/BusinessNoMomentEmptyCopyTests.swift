import Testing
@testable import momentra

struct BusinessNoMomentEmptyCopyTests {
    @Test func noMomentEmptyDoesNotLookLikeLiveData() {
        #expect(BusinessNoMomentEmptyCopy.momentsSampleRows.isEmpty)
        for title in BusinessNoMomentEmptyCopy.forbiddenSampleTitles {
            #expect(BusinessNoMomentEmptyCopy.momentsSampleRows.contains { $0.title == title } == false)
        }
        #expect(BusinessNoMomentEmptyCopy.absent == "Not set up")
        #expect(BusinessNoMomentEmptyCopy.absent != "-")
        #expect(BusinessNoMomentEmptyCopy.absent != "0")
    }
}
