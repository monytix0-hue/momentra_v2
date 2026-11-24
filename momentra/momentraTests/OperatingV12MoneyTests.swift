import Foundation
import Testing
@testable import momentra

struct OperatingV12MoneyTests {

    @Test func groupedUsesIndianCommasAndTwoDecimals() {
        #expect(OperatingV12Money.grouped(940000, currency: "INR") == "₹9,40,000")
        #expect(OperatingV12Money.grouped(86000.5, currency: "INR") == "₹86,000.50")
        #expect(OperatingV12Money.amount("86000.50", currency: "INR") == "₹86,000.50")
        #expect(OperatingV12Money.amount("940000.00", currency: "INR") == "₹9,40,000")
    }
}
