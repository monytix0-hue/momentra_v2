package com.example.momentra.ui.shell.shared

import com.example.momentra.data.api.BusinessFinanceTotalDto
import com.example.momentra.data.api.GroupFinanceTotalDto
import com.example.momentra.data.api.GroupSetupBudgetDto
import com.example.momentra.ui.shell.group.shared.GroupFinanceFormat
import com.example.momentra.ui.shell.group.shared.TravelCurrencyCatalog
import java.math.BigDecimal

/**
 * Setup-aware currency resolution for money sheets.
 * Callers fetch finance / setup data, then pass it here — no network I/O.
 */
object MomentCurrencyResolver {
    data class FinanceTotal(
        val currencyCode: String,
        val budgetTotal: String? = null,
        val expenseTotal: String? = null,
    )

    data class SetupBudget(
        val currencyCode: String,
        val isPrimary: Boolean = false,
    )

    fun fromGroupTotals(totals: List<GroupFinanceTotalDto>): List<FinanceTotal> =
        totals.map { FinanceTotal(it.currencyCode, it.budgetTotal, it.expenseTotal) }

    fun fromBusinessTotals(totals: List<BusinessFinanceTotalDto>): List<FinanceTotal> =
        totals.map { FinanceTotal(it.currencyCode, expenseTotal = it.expenseTotal) }

    fun fromGroupSetupBudgets(budgets: List<GroupSetupBudgetDto>): List<SetupBudget> =
        budgets.map { SetupBudget(it.currencyCode, it.isPrimary == true) }

    fun resolveMomentCurrency(
        financeTotals: List<FinanceTotal> = emptyList(),
        setupBudgets: List<SetupBudget> = emptyList(),
        setupPreferences: Map<String, Any?>? = null,
        accountCurrency: String? = null,
    ): String {
        primaryFinanceTotal(financeTotals)?.currencyCode?.let { normalize(it) }?.let { return it }
        primarySetupBudget(setupBudgets)?.currencyCode?.let { normalize(it) }?.let { return it }
        preferenceCurrency(setupPreferences)?.let { return it }
        accountCurrency?.let { normalize(it) }?.let { return it }
        return "INR"
    }

    fun resolveMomentCurrencies(
        financeTotals: List<FinanceTotal> = emptyList(),
        setupBudgets: List<SetupBudget> = emptyList(),
        setupPreferences: Map<String, Any?>? = null,
        accountCurrency: String? = null,
    ): List<String> {
        val out = mutableListOf<String>()
        fun append(code: String?) {
            val normalized = code?.let { normalize(it) } ?: return
            if (normalized !in out) out.add(normalized)
        }

        financeTotals
            .sortedByDescending { financeWeight(it) }
            .forEach { append(it.currencyCode) }

        val primaryBudget = primarySetupBudget(setupBudgets)
        append(primaryBudget?.currencyCode)
        setupBudgets
            .filter { it != primaryBudget }
            .forEach { append(it.currencyCode) }

        append(preferenceCurrency(setupPreferences))
        extraCurrencies(setupPreferences).forEach { append(it) }
        append(accountCurrency?.let { normalize(it) })

        return out
    }

    fun pickerOptions(preferred: List<String>): List<String> {
        val preferredNorm = preferred.mapNotNull { normalize(it) }.distinct()
        val rest = TravelCurrencyCatalog.codes.filter { it !in preferredNorm }
        return preferredNorm + rest
    }

    private fun primaryFinanceTotal(totals: List<FinanceTotal>): FinanceTotal? {
        if (totals.isEmpty()) return null
        return totals.maxByOrNull { financeWeight(it) }
    }

    private fun primarySetupBudget(budgets: List<SetupBudget>): SetupBudget? {
        if (budgets.isEmpty()) return null
        return budgets.firstOrNull { it.isPrimary } ?: budgets.first()
    }

    private fun financeWeight(total: FinanceTotal): Int {
        if (GroupFinanceFormat.parseAmount(total.budgetTotal) > BigDecimal.ZERO) return 2
        if (GroupFinanceFormat.parseAmount(total.expenseTotal) > BigDecimal.ZERO) return 1
        return 0
    }

    private fun preferenceCurrency(prefs: Map<String, Any?>?): String? =
        normalize(prefs?.get("currency")?.toString())

    private fun extraCurrencies(prefs: Map<String, Any?>?): List<String> {
        val raw = prefs?.get("extraCurrencies") ?: return emptyList()
        return when (raw) {
            is List<*> -> raw.mapNotNull { normalize(it?.toString()) }
            is Array<*> -> raw.mapNotNull { normalize(it?.toString()) }
            else -> emptyList()
        }
    }

    private fun normalize(code: String?): String? {
        val upper = code?.trim()?.uppercase()?.take(3) ?: return null
        if (upper.length != 3) return null
        return if (TravelCurrencyCatalog.find(upper) != null) upper else upper
    }
}
