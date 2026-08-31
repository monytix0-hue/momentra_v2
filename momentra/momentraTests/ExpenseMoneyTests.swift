import XCTest
@testable import momentra

final class ExpenseMoneyTests: XCTestCase {
    func testAcceptsValidAmounts() {
        XCTAssertEqual(ExpenseMoney.validateForSubmit("1250.50"), "1250.50")
        XCTAssertEqual(ExpenseMoney.validateForSubmit("0.01"), "0.01")
        XCTAssertEqual(ExpenseMoney.validateForSubmit("100"), "100")
    }

    func testRejectsInvalid() {
        XCTAssertNil(ExpenseMoney.validateForSubmit("0"))
        XCTAssertNil(ExpenseMoney.validateForSubmit("-1"))
        XCTAssertNil(ExpenseMoney.validateForSubmit(""))
        XCTAssertNil(ExpenseMoney.validateForSubmit("1.23456"))
        XCTAssertFalse(ExpenseMoney.isValidPositive("abc"))
    }

    func testNormalizesComma() {
        XCTAssertEqual(ExpenseMoney.normalize("10,5"), "10.5")
        XCTAssertTrue(ExpenseMoney.isValidPositive("10,5"))
    }
}
