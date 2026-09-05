import Foundation

struct TravelCurrency: Identifiable, Hashable {
    var id: String { code }
    let code: String
    let symbol: String
    let name: String
}

enum GroupTravelCurrencyCatalog {
    static let all: [TravelCurrency] = [
        .init(code: "USD", symbol: "$", name: "US Dollar"),
        .init(code: "EUR", symbol: "€", name: "Euro"),
        .init(code: "GBP", symbol: "£", name: "British Pound"),
        .init(code: "INR", symbol: "₹", name: "Indian Rupee"),
        .init(code: "AED", symbol: "د.إ", name: "UAE Dirham"),
        .init(code: "SAR", symbol: "﷼", name: "Saudi Riyal"),
        .init(code: "JPY", symbol: "¥", name: "Japanese Yen"),
        .init(code: "CNY", symbol: "¥", name: "Chinese Yuan"),
        .init(code: "HKD", symbol: "HK$", name: "Hong Kong Dollar"),
        .init(code: "SGD", symbol: "S$", name: "Singapore Dollar"),
        .init(code: "THB", symbol: "฿", name: "Thai Baht"),
        .init(code: "MYR", symbol: "RM", name: "Malaysian Ringgit"),
        .init(code: "IDR", symbol: "Rp", name: "Indonesian Rupiah"),
        .init(code: "PHP", symbol: "₱", name: "Philippine Peso"),
        .init(code: "VND", symbol: "₫", name: "Vietnamese Dong"),
        .init(code: "KRW", symbol: "₩", name: "South Korean Won"),
        .init(code: "TWD", symbol: "NT$", name: "New Taiwan Dollar"),
        .init(code: "AUD", symbol: "A$", name: "Australian Dollar"),
        .init(code: "NZD", symbol: "NZ$", name: "New Zealand Dollar"),
        .init(code: "CAD", symbol: "C$", name: "Canadian Dollar"),
        .init(code: "CHF", symbol: "CHF", name: "Swiss Franc"),
        .init(code: "SEK", symbol: "kr", name: "Swedish Krona"),
        .init(code: "NOK", symbol: "kr", name: "Norwegian Krone"),
        .init(code: "DKK", symbol: "kr", name: "Danish Krone"),
        .init(code: "PLN", symbol: "zł", name: "Polish Zloty"),
        .init(code: "CZK", symbol: "Kč", name: "Czech Koruna"),
        .init(code: "HUF", symbol: "Ft", name: "Hungarian Forint"),
        .init(code: "TRY", symbol: "₺", name: "Turkish Lira"),
        .init(code: "ILS", symbol: "₪", name: "Israeli Shekel"),
        .init(code: "EGP", symbol: "E£", name: "Egyptian Pound"),
        .init(code: "ZAR", symbol: "R", name: "South African Rand"),
        .init(code: "MAD", symbol: "MAD", name: "Moroccan Dirham"),
        .init(code: "KES", symbol: "KSh", name: "Kenyan Shilling"),
        .init(code: "MXN", symbol: "Mex$", name: "Mexican Peso"),
        .init(code: "BRL", symbol: "R$", name: "Brazilian Real"),
        .init(code: "ARS", symbol: "AR$", name: "Argentine Peso"),
        .init(code: "CLP", symbol: "CLP$", name: "Chilean Peso"),
        .init(code: "COP", symbol: "COL$", name: "Colombian Peso"),
        .init(code: "PEN", symbol: "S/", name: "Peruvian Sol"),
        .init(code: "PKR", symbol: "₨", name: "Pakistani Rupee"),
        .init(code: "BDT", symbol: "৳", name: "Bangladeshi Taka"),
        .init(code: "LKR", symbol: "Rs", name: "Sri Lankan Rupee"),
        .init(code: "NPR", symbol: "Rs", name: "Nepalese Rupee"),
        .init(code: "RUB", symbol: "₽", name: "Russian Ruble"),
        .init(code: "QAR", symbol: "QR", name: "Qatari Riyal"),
        .init(code: "KWD", symbol: "KD", name: "Kuwaiti Dinar"),
        .init(code: "BHD", symbol: "BD", name: "Bahraini Dinar"),
        .init(code: "OMR", symbol: "OMR", name: "Omani Rial"),
        .init(code: "RON", symbol: "lei", name: "Romanian Leu"),
        .init(code: "ISK", symbol: "kr", name: "Icelandic Krona"),
        .init(code: "NGN", symbol: "₦", name: "Nigerian Naira"),
        .init(code: "FJD", symbol: "FJ$", name: "Fijian Dollar"),
        .init(code: "BTN", symbol: "Nu.", name: "Bhutanese Ngultrum"),
        .init(code: "MMK", symbol: "K", name: "Myanmar Kyat"),
        .init(code: "KHR", symbol: "៛", name: "Cambodian Riel"),
        .init(code: "LAK", symbol: "₭", name: "Lao Kip"),
    ]

    static var codes: [String] { all.map(\.code) }

    static func find(_ code: String) -> TravelCurrency? {
        all.first { $0.code.caseInsensitiveCompare(code) == .orderedSame }
    }

    static func symbol(_ code: String) -> String {
        find(code)?.symbol ?? code
    }

    static func display(_ code: String) -> String {
        guard let c = find(code) else { return code }
        return "\(c.code) · \(c.symbol) · \(c.name)"
    }
}
