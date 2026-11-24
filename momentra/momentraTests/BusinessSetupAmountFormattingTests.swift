import Foundation
import Testing
@testable import momentra

struct BusinessSetupAmountFormattingTests {

    @Test func formatsIndianGroupingWhileTyping() {
        #expect(BusinessSetupAmountFormatting.formatIndianAmount("9000000") == "90,00,000")
        #expect(BusinessSetupAmountFormatting.formatIndianAmount("86000.5") == "86,000.5")
        #expect(BusinessSetupAmountFormatting.formatIndianAmount("86000.50") == "86,000.50")
        #expect(BusinessSetupAmountFormatting.formatIndianAmount("86000.509") == "86,000.50")
    }

    @Test func plainAmountStripsCurrencyNoise() {
        #expect(BusinessSetupAmountFormatting.plainAmount("₹90,00,000") == "9000000")
        #expect(BusinessSetupAmountFormatting.plainAmount("86,000.50") == "86000.50")
    }

    @Test func amountHelperDetection() {
        #expect(BusinessSetupAmountFormatting.isAmountHelper("Amount"))
        #expect(BusinessSetupAmountFormatting.isAmountHelper("Amount / count"))
        #expect(!BusinessSetupAmountFormatting.isAmountHelper("State"))
        #expect(!BusinessSetupAmountFormatting.isAmountHelper("Optional"))
    }
}
