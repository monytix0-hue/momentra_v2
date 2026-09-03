package com.example.momentra.ui.shell.empty.group

import com.example.momentra.ui.setup.SetupFieldKind

/** Maps Group setup preference keys to input kinds (title / date pickers vs chips). */
object GroupSetupFields {
    const val NAME = "name"
    const val DATES = "dates"
    const val TARGET_DATE = "targetDate"
    const val MOVE_IN = "moveIn"

    fun kindFor(key: String): SetupFieldKind = when (key) {
        NAME -> SetupFieldKind.TITLE
        DATES, TARGET_DATE, MOVE_IN -> SetupFieldKind.DATE
        else -> SetupFieldKind.CHIPS
    }
}
