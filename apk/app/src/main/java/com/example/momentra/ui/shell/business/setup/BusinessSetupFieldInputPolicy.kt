package com.example.momentra.ui.shell.business.setup

import androidx.compose.ui.text.input.ImeAction

/**
 * Every Business setup field is single-line, so the IME action both commits the entry and
 * dismisses the keyboard. Lockstep with iOS `BusinessSetupFieldInputPolicy`.
 */
object BusinessSetupFieldInputPolicy {
    fun imeAction(): ImeAction = ImeAction.Done

    const val CLEAR_FOCUS_ON_SUBMIT: Boolean = true
}
