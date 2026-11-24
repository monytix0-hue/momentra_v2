import Testing
@testable import momentra

struct TradingSheetStateTests {
    @Test func listsExactlyEighteenSurfaces() {
        #expect(TradingSheetState.allCases.count == 18)
        #expect(Set(TradingSheetState.allCases.map(\.spec.code)).count == 18)
    }

    @Test func pulseAndBoardRoutersCoverAllEighteen() {
        let pulse = ["TARGET_BUDGET", "FINANCE", "PAYMENT_READINESS", "ATTENTION", "ACTIVITY", "GST"]
        let board = ["orders", "stock", "network", "M4", "L1", "L2", "L3", "L4", "ME1", "ME2", "fulfilment", "ME4"]
        for code in pulse {
            #expect(tradingSheetForPulseOrBoard(code) != nil, "missing pulse \(code)")
        }
        for code in board {
            #expect(tradingSheetForPulseOrBoard(code) != nil, "missing board \(code)")
        }
        #expect(pulse.count + board.count == 18)
    }

    @Test func peopleNetworkMatchesFigmaTitle() {
        #expect(TradingSheetState.peopleNetwork.spec.title == "People & Network")
    }

    @Test func attentionFooterIsResolveAll() {
        #expect(TradingSheetState.needsAttention.spec.footerCta == "Resolve all")
        #expect(TradingSheetState.openCommitments.spec.footerCta == "Resolve commitments")
        #expect(TradingSheetState.paymentReadiness.spec.footerCta == "Record payment")
    }
}
