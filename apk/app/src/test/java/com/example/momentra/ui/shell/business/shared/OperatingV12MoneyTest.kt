package com.example.momentra.ui.shell.business.shared

import org.junit.Assert.assertEquals
import org.junit.Test

class OperatingV12MoneyTest {

    @Test
    fun groupedUsesIndianCommasAndTwoDecimals() {
        assertEquals("₹9,40,000", OperatingV12Money.grouped(940000.0, "INR"))
        assertEquals("₹86,000.50", OperatingV12Money.grouped(86000.5, "INR"))
        assertEquals("₹86,000.50", OperatingV12Money.grouped("86000.50", "INR"))
        assertEquals("₹9,40,000", OperatingV12Money.amount("940000.00", "INR"))
    }
}
