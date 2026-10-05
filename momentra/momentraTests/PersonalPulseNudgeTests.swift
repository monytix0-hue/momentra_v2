import Foundation
import Testing
@testable import momentra

struct PersonalPulseNudgeTests {
    @Test func everydayNudgeIsOmittedWhenTodayHasNoLogs() {
        #expect(PersonalPulseFamily.lifeOperations.visibleNudge(todayLogCount: 0) == nil)
    }

    @Test func everydayNudgeWithLogsKeepsRecoveryCtaAndDoesNotClaimBusy() {
        let nudge = PersonalPulseFamily.lifeOperations.visibleNudge(todayLogCount: 1)
        #expect(nudge?.cta == "Log Recovery")
        let text = [nudge?.title, nudge?.body].compactMap { $0 }.joined(separator: " ")
        #expect(!text.localizedCaseInsensitiveContains("busy"))
    }

    @Test func otherFamiliesKeepTheirInstructionCopy() {
        let future = PersonalPulseFamily.futureBuilding.visibleNudge(todayLogCount: 0)
        #expect(future?.body == "Log a milestone to keep momentum compounding.")
        let people = PersonalPulseFamily.relationships.visibleNudge(todayLogCount: 0)
        #expect(people?.body == "Log a connection before the next busy stretch.")
    }
}
