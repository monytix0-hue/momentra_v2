import Foundation

struct MomentCurrencyContext: Equatable {
    let primary: String
    let preferred: [String]

    var pickerOptions: [String] { MomentCurrencyResolver.pickerOptions(preferred: preferred) }
}

enum MomentCurrencyContextLoader {
    static func loadGroup(momentId: String) async -> MomentCurrencyContext {
        let totals = (try? await APIClient.shared.getGroupFinance(momentId: momentId).payload?.totals) ?? []
        let prefill = await MomentCreateModel().getGroupSetupPrefill(momentId: momentId)
        let finance = MomentCurrencyResolver.fromGroupTotals(totals)
        let budgets = MomentCurrencyResolver.fromGroupSetupBudgets(prefill?.budgets ?? [])
        return MomentCurrencyContext(
            primary: MomentCurrencyResolver.resolveMomentCurrency(financeTotals: finance, setupBudgets: budgets),
            preferred: MomentCurrencyResolver.resolveMomentCurrencies(financeTotals: finance, setupBudgets: budgets)
        )
    }

    static func loadDomain(
        momentId: String,
        financeTotals: [MomentCurrencyResolver.FinanceTotal] = [],
        accountCurrency: String? = nil
    ) async -> MomentCurrencyContext {
        let prefs = await MomentCreateModel().getDomainSetupPrefill(momentId: momentId)?.preferences?.mapValues { $0.value }
        return MomentCurrencyContext(
            primary: MomentCurrencyResolver.resolveMomentCurrency(
                financeTotals: financeTotals,
                setupPreferences: prefs,
                accountCurrency: accountCurrency
            ),
            preferred: MomentCurrencyResolver.resolveMomentCurrencies(
                financeTotals: financeTotals,
                setupPreferences: prefs,
                accountCurrency: accountCurrency
            )
        )
    }

    static func loadBusiness(momentId: String) async -> MomentCurrencyContext {
        let totals = (try? await APIClient.shared.getBusinessFinance(momentId: momentId).payload?.totals) ?? []
        let prefs = await MomentCreateModel().getDomainSetupPrefill(momentId: momentId)?.preferences?.mapValues { $0.value }
        let finance = MomentCurrencyResolver.fromBusinessTotals(totals)
        return MomentCurrencyContext(
            primary: MomentCurrencyResolver.resolveMomentCurrency(financeTotals: finance, setupPreferences: prefs),
            preferred: MomentCurrencyResolver.resolveMomentCurrencies(financeTotals: finance, setupPreferences: prefs)
        )
    }

    static func loadPersonal(momentId: String) async -> MomentCurrencyContext {
        let accounts = (try? await APIClient.shared.listFinancialAccounts()) ?? []
        let accountCurrency = accounts.first?.currencyCode
        return await loadDomain(momentId: momentId, accountCurrency: accountCurrency)
    }
}
