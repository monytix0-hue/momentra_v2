package com.example.momentra.ui.shell.shared

import com.example.momentra.data.repository.BusinessSliceRepository
import com.example.momentra.data.repository.GroupSliceRepository
import com.example.momentra.data.repository.MomentCreateRepository
import com.example.momentra.data.repository.PersonalSliceRepository

data class MomentCurrencyContext(
    val primary: String,
    val preferred: List<String>,
) {
    val pickerOptions: List<String> get() = MomentCurrencyResolver.pickerOptions(preferred)
}

suspend fun loadGroupCurrencyContext(
    momentId: String,
    groupRepo: GroupSliceRepository = GroupSliceRepository(),
    createRepo: MomentCreateRepository = MomentCreateRepository(),
): MomentCurrencyContext {
    val totals = groupRepo.getFinance(momentId).getOrNull()?.payload?.totals.orEmpty()
    val prefill = createRepo.getGroupSetupPrefill(momentId).getOrNull()
    val finance = MomentCurrencyResolver.fromGroupTotals(totals)
    val budgets = MomentCurrencyResolver.fromGroupSetupBudgets(prefill?.budgets.orEmpty())
    return MomentCurrencyContext(
        primary = MomentCurrencyResolver.resolveMomentCurrency(finance, budgets),
        preferred = MomentCurrencyResolver.resolveMomentCurrencies(finance, budgets),
    )
}

suspend fun loadDomainCurrencyContext(
    momentId: String,
    financeTotals: List<MomentCurrencyResolver.FinanceTotal> = emptyList(),
    createRepo: MomentCreateRepository = MomentCreateRepository(),
    accountCurrency: String? = null,
): MomentCurrencyContext {
    val prefs = createRepo.getDomainSetupPrefill(momentId).getOrNull()?.preferences
    return MomentCurrencyContext(
        primary = MomentCurrencyResolver.resolveMomentCurrency(
            financeTotals = financeTotals,
            setupPreferences = prefs,
            accountCurrency = accountCurrency,
        ),
        preferred = MomentCurrencyResolver.resolveMomentCurrencies(
            financeTotals = financeTotals,
            setupPreferences = prefs,
            accountCurrency = accountCurrency,
        ),
    )
}

suspend fun loadBusinessCurrencyContext(
    momentId: String,
    businessRepo: BusinessSliceRepository = BusinessSliceRepository(),
    createRepo: MomentCreateRepository = MomentCreateRepository(),
): MomentCurrencyContext {
    val totals = businessRepo.getFinance(momentId).getOrNull()?.payload?.totals.orEmpty()
    val prefs = createRepo.getDomainSetupPrefill(momentId).getOrNull()?.preferences
    val finance = MomentCurrencyResolver.fromBusinessTotals(totals)
    return MomentCurrencyContext(
        primary = MomentCurrencyResolver.resolveMomentCurrency(finance, setupPreferences = prefs),
        preferred = MomentCurrencyResolver.resolveMomentCurrencies(finance, setupPreferences = prefs),
    )
}

suspend fun loadPersonalCurrencyContext(
    momentId: String,
    personalRepo: PersonalSliceRepository = PersonalSliceRepository(),
    createRepo: MomentCreateRepository = MomentCreateRepository(),
): MomentCurrencyContext {
    val accounts = personalRepo.listFinancialAccounts().getOrNull().orEmpty()
    val accountCurrency = accounts.firstOrNull()?.currencyCode
    return loadDomainCurrencyContext(momentId, accountCurrency = accountCurrency, createRepo = createRepo)
}
