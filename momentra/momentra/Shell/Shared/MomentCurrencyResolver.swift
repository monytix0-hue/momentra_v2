import Foundation

/// Setup-aware currency resolution for money sheets.
/// Callers fetch finance / setup data, then pass it here — no network I/O.
enum MomentCurrencyResolver {
    struct FinanceTotal: Equatable {
        let currencyCode: String
        var budgetTotal: String? = nil
        var expenseTotal: String? = nil
    }

    struct SetupBudget: Equatable {
        let currencyCode: String
        var isPrimary: Bool = false
    }

    static func fromGroupTotals(_ totals: [APIClient.GroupFinanceTotalsPayload]) -> [FinanceTotal] {
        totals.map { FinanceTotal(currencyCode: $0.currencyCode, budgetTotal: $0.budgetTotal, expenseTotal: $0.expenseTotal) }
    }

    static func fromBusinessTotals(_ totals: [APIClient.BusinessFinanceTotalsPayload]) -> [FinanceTotal] {
        totals.map { FinanceTotal(currencyCode: $0.currencyCode, expenseTotal: $0.expenseTotal) }
    }

    static func fromGroupSetupBudgets(_ budgets: [GroupSetupBudgetPrefill]) -> [SetupBudget] {
        budgets.map { SetupBudget(currencyCode: $0.currencyCode, isPrimary: $0.isPrimary == true) }
    }

    static func resolveMomentCurrency(
        financeTotals: [FinanceTotal] = [],
        setupBudgets: [SetupBudget] = [],
        setupPreferences: [String: Any]? = nil,
        accountCurrency: String? = nil
    ) -> String {
        if let code = normalize(primaryFinanceTotal(financeTotals)?.currencyCode) { return code }
        if let code = normalize(primarySetupBudget(setupBudgets)?.currencyCode) { return code }
        if let code = preferenceCurrency(setupPreferences) { return code }
        if let code = accountCurrency.flatMap({ normalize($0) }) { return code }
        return "INR"
    }

    static func resolveMomentCurrencies(
        financeTotals: [FinanceTotal] = [],
        setupBudgets: [SetupBudget] = [],
        setupPreferences: [String: Any]? = nil,
        accountCurrency: String? = nil
    ) -> [String] {
        var out: [String] = []
        func append(_ code: String?) {
            guard let normalized = code.flatMap({ normalize($0) }) else { return }
            if !out.contains(normalized) { out.append(normalized) }
        }

        financeTotals
            .sorted { financeWeight($0) > financeWeight($1) }
            .forEach { append($0.currencyCode) }

        let primaryBudget = primarySetupBudget(setupBudgets)
        append(primaryBudget?.currencyCode)
        setupBudgets
            .filter { $0.currencyCode != primaryBudget?.currencyCode }
            .forEach { append($0.currencyCode) }

        append(preferenceCurrency(setupPreferences))
        extraCurrencies(setupPreferences).forEach { append($0) }
        append(accountCurrency.flatMap { normalize($0) })

        return out
    }

    static func pickerOptions(preferred: [String]) -> [String] {
        let preferredNorm = preferred.compactMap { normalize($0) }
        var seen = Set<String>()
        let orderedPreferred = preferredNorm.filter { seen.insert($0).inserted }
        let rest = TravelCurrencyCatalog.codes.filter { !seen.contains($0) }
        return orderedPreferred + rest
    }

    private static func primaryFinanceTotal(_ totals: [FinanceTotal]) -> FinanceTotal? {
        totals.max { financeWeight($0) < financeWeight($1) }
    }

    private static func primarySetupBudget(_ budgets: [SetupBudget]) -> SetupBudget? {
        guard !budgets.isEmpty else { return nil }
        return budgets.first(where: \.isPrimary) ?? budgets.first
    }

    private static func financeWeight(_ total: FinanceTotal) -> Int {
        if GroupFinanceFormat.parseAmount(total.budgetTotal) > 0 { return 2 }
        if GroupFinanceFormat.parseAmount(total.expenseTotal) > 0 { return 1 }
        return 0
    }

    private static func preferenceCurrency(_ prefs: [String: Any]?) -> String? {
        guard let raw = prefs?["currency"] else { return nil }
        if let s = raw as? String { return normalize(s) }
        return normalize(String(describing: raw))
    }

    private static func extraCurrencies(_ prefs: [String: Any]?) -> [String] {
        guard let raw = prefs?["extraCurrencies"] else { return [] }
        if let list = raw as? [String] { return list.compactMap { normalize($0) } }
        if let list = raw as? [Any] { return list.compactMap { normalize(String(describing: $0)) } }
        return []
    }

    private static func normalize(_ code: String?) -> String? {
        guard let trimmed = code?.trimmingCharacters(in: .whitespacesAndNewlines).uppercased(),
              trimmed.count == 3 else { return nil }
        return trimmed
    }
}
