package com.example.momentra.ui.shell.business.shared

import com.example.momentra.data.api.OperatingPaymentBody
import java.math.BigDecimal
import java.math.RoundingMode

/** Shared POS payable + split-tender helpers for Retail / Restaurant Charge. */
object PosSettlement {
    val tenderMethods = listOf("UPI", "Cash", "Card")

    fun money(value: BigDecimal): String =
        value.setScale(4, RoundingMode.HALF_UP).toPlainString()

    fun parseAmount(raw: String?): BigDecimal =
        raw?.trim()?.takeIf { it.isNotBlank() }?.toBigDecimalOrNull()?.max(BigDecimal.ZERO)
            ?: BigDecimal.ZERO

    fun payable(
        gross: BigDecimal,
        discount: BigDecimal,
        cessAmount: BigDecimal = BigDecimal.ZERO,
    ): BigDecimal {
        val net = (gross - discount).max(BigDecimal.ZERO)
        return net + cessAmount.max(BigDecimal.ZERO)
    }

    fun buildPayments(
        split: Boolean,
        primary: String,
        payable: BigDecimal,
        splitAmounts: Map<String, String>,
        openAr: Boolean = false,
    ): List<OperatingPaymentBody>? {
        if (openAr || payable.signum() <= 0) return null
        if (!split) {
            return listOf(OperatingPaymentBody(method = primary, amount = money(payable)))
        }
        val rows = tenderMethods.mapNotNull { method ->
            val amount = parseAmount(splitAmounts[method])
            if (amount.signum() <= 0) null
            else OperatingPaymentBody(method = method, amount = money(amount))
        }
        return rows.takeIf { it.isNotEmpty() }
    }

    fun splitSumsToPayable(splitAmounts: Map<String, String>, payable: BigDecimal): Boolean {
        val sum = tenderMethods.fold(BigDecimal.ZERO) { acc, method ->
            acc + parseAmount(splitAmounts[method])
        }
        return sum.setScale(2, RoundingMode.HALF_UP)
            .compareTo(payable.setScale(2, RoundingMode.HALF_UP)) == 0
    }

    fun tenderLabel(split: Boolean, primary: String, payments: List<OperatingPaymentBody>?): String {
        if (!split) return primary
        val parts = payments.orEmpty().map { "${it.method} ${it.amount.trimEnd('0').trimEnd('.')}" }
        return parts.joinToString(" + ").ifBlank { "Split" }
    }
}
