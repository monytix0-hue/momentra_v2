package com.example.momentra.ui.shell.personal.lifeops.create

import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalMoneyMovementPayloadTest {
    @Test
    fun transferDescription_includesToAccount() {
        val desc = buildTransferDescription(
            note = "",
            transferType = "One-time",
            whenCode = "Now",
            toAccountName = "HDFC Savings",
        )
        assertTrue(desc.contains("HDFC Savings"))
        assertTrue(desc.contains("to=HDFC Savings"))
    }
}