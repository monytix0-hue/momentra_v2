package com.example.momentra.ui.shell.business.setup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BusinessSetupAmountFormattingTest {

    @Test
    fun formatsIndianGroupingWhileTyping() {
        assertEquals("90,00,000", BusinessSetupAmountFormatting.formatIndianAmount("9000000"))
        assertEquals("86,000.5", BusinessSetupAmountFormatting.formatIndianAmount("86000.5"))
        assertEquals("86,000.50", BusinessSetupAmountFormatting.formatIndianAmount("86000.50"))
        assertEquals("86,000.50", BusinessSetupAmountFormatting.formatIndianAmount("86000.509"))
    }

    @Test
    fun plainAmountStripsCurrencyNoise() {
        assertEquals("9000000", BusinessSetupAmountFormatting.plainAmount("₹90,00,000"))
        assertEquals("86000.50", BusinessSetupAmountFormatting.plainAmount("86,000.50"))
    }

    @Test
    fun amountHelperDetection() {
        assertTrue(BusinessSetupAmountFormatting.isAmountHelper("Amount"))
        assertTrue(BusinessSetupAmountFormatting.isAmountHelper("Amount / count"))
        assertFalse(BusinessSetupAmountFormatting.isAmountHelper("State"))
        assertFalse(BusinessSetupAmountFormatting.isAmountHelper("Optional"))
    }
}
