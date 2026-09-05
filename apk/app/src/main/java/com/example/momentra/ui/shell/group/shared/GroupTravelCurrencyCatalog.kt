package com.example.momentra.ui.shell.group.shared

/**
 * Major travel / international ISO 4217 currencies for Trip multi-currency setup & expenses.
 */
data class TravelCurrency(
    val code: String,
    val symbol: String,
    val name: String,
)

object GroupTravelCurrencyCatalog {
    val all: List<TravelCurrency> = listOf(
        TravelCurrency("USD", "$", "US Dollar"),
        TravelCurrency("EUR", "€", "Euro"),
        TravelCurrency("GBP", "£", "British Pound"),
        TravelCurrency("INR", "₹", "Indian Rupee"),
        TravelCurrency("AED", "د.إ", "UAE Dirham"),
        TravelCurrency("SAR", "﷼", "Saudi Riyal"),
        TravelCurrency("JPY", "¥", "Japanese Yen"),
        TravelCurrency("CNY", "¥", "Chinese Yuan"),
        TravelCurrency("HKD", "HK$", "Hong Kong Dollar"),
        TravelCurrency("SGD", "S$", "Singapore Dollar"),
        TravelCurrency("THB", "฿", "Thai Baht"),
        TravelCurrency("MYR", "RM", "Malaysian Ringgit"),
        TravelCurrency("IDR", "Rp", "Indonesian Rupiah"),
        TravelCurrency("PHP", "₱", "Philippine Peso"),
        TravelCurrency("VND", "₫", "Vietnamese Dong"),
        TravelCurrency("KRW", "₩", "South Korean Won"),
        TravelCurrency("TWD", "NT$", "New Taiwan Dollar"),
        TravelCurrency("AUD", "A$", "Australian Dollar"),
        TravelCurrency("NZD", "NZ$", "New Zealand Dollar"),
        TravelCurrency("CAD", "C$", "Canadian Dollar"),
        TravelCurrency("CHF", "CHF", "Swiss Franc"),
        TravelCurrency("SEK", "kr", "Swedish Krona"),
        TravelCurrency("NOK", "kr", "Norwegian Krone"),
        TravelCurrency("DKK", "kr", "Danish Krone"),
        TravelCurrency("PLN", "zł", "Polish Zloty"),
        TravelCurrency("CZK", "Kč", "Czech Koruna"),
        TravelCurrency("HUF", "Ft", "Hungarian Forint"),
        TravelCurrency("TRY", "₺", "Turkish Lira"),
        TravelCurrency("ILS", "₪", "Israeli Shekel"),
        TravelCurrency("EGP", "E£", "Egyptian Pound"),
        TravelCurrency("ZAR", "R", "South African Rand"),
        TravelCurrency("MAD", "MAD", "Moroccan Dirham"),
        TravelCurrency("KES", "KSh", "Kenyan Shilling"),
        TravelCurrency("MXN", "Mex$", "Mexican Peso"),
        TravelCurrency("BRL", "R$", "Brazilian Real"),
        TravelCurrency("ARS", "AR$", "Argentine Peso"),
        TravelCurrency("CLP", "CLP$", "Chilean Peso"),
        TravelCurrency("COP", "COL$", "Colombian Peso"),
        TravelCurrency("PEN", "S/", "Peruvian Sol"),
        TravelCurrency("PKR", "₨", "Pakistani Rupee"),
        TravelCurrency("BDT", "৳", "Bangladeshi Taka"),
        TravelCurrency("LKR", "Rs", "Sri Lankan Rupee"),
        TravelCurrency("NPR", "Rs", "Nepalese Rupee"),
        TravelCurrency("RUB", "₽", "Russian Ruble"),
        TravelCurrency("QAR", "QR", "Qatari Riyal"),
        TravelCurrency("KWD", "KD", "Kuwaiti Dinar"),
        TravelCurrency("BHD", "BD", "Bahraini Dinar"),
        TravelCurrency("OMR", "OMR", "Omani Rial"),
        TravelCurrency("RON", "lei", "Romanian Leu"),
        TravelCurrency("ISK", "kr", "Icelandic Krona"),
        TravelCurrency("NGN", "₦", "Nigerian Naira"),
        TravelCurrency("FJD", "FJ$", "Fijian Dollar"),
        TravelCurrency("BTN", "Nu.", "Bhutanese Ngultrum"),
        TravelCurrency("MMK", "K", "Myanmar Kyat"),
        TravelCurrency("KHR", "៛", "Cambodian Riel"),
        TravelCurrency("LAK", "₭", "Lao Kip"),
    )

    val codes: List<String> = all.map { it.code }

    fun find(code: String): TravelCurrency? =
        all.firstOrNull { it.code.equals(code, ignoreCase = true) }

    fun symbol(code: String): String = find(code)?.symbol ?: code

    fun display(code: String): String {
        val c = find(code) ?: return code
        return "${c.code} · ${c.symbol} · ${c.name}"
    }
}
