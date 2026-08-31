package com.example.momentra.domain.finance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseMoneyTest {

    @Test
    fun acceptsValidAmounts() {
        assertEquals("1250.50", ExpenseMoney.validateForSubmit("1250.50"))
        assertEquals("0.01", ExpenseMoney.validateForSubmit("0.01"))
        assertEquals("100", ExpenseMoney.validateForSubmit("100"))
        assertEquals("1.2345", ExpenseMoney.validateForSubmit("1.2345"))
    }

    @Test
    fun rejectsZeroNegativeEmptyAndTooManyDecimals() {
        assertNull(ExpenseMoney.validateForSubmit("0"))
        assertNull(ExpenseMoney.validateForSubmit("0.00"))
        assertNull(ExpenseMoney.validateForSubmit("-1"))
        assertNull(ExpenseMoney.validateForSubmit(""))
        assertNull(ExpenseMoney.validateForSubmit("1.23456"))
        assertFalse(ExpenseMoney.isValidPositive("abc"))
    }

    @Test
    fun normalizesLeadingZerosAndComma() {
        assertEquals("10.5", ExpenseMoney.normalize("010.5"))
        assertEquals("10.5", ExpenseMoney.normalize("10,5"))
        assertTrue(ExpenseMoney.isValidPositive("10,5"))
    }
}
