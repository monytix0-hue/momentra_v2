package com.example.momentra.ui.shell.business.setup

import androidx.compose.ui.text.input.ImeAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BusinessSetupFieldInputPolicyTest {

    @Test
    fun setupFieldsDismissTheKeyboardOnImeAction() {
        assertEquals(ImeAction.Done, BusinessSetupFieldInputPolicy.imeAction())
        assertTrue(BusinessSetupFieldInputPolicy.CLEAR_FOCUS_ON_SUBMIT)
    }
}
