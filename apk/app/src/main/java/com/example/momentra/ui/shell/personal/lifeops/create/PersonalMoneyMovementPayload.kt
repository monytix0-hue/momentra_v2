package com.example.momentra.ui.shell.personal.lifeops.create

fun buildTransferDescription(
    note: String,
    transferType: String,
    whenCode: String,
    toAccountName: String,
): String = buildString {
    val trimmedNote = note.trim()
    if (trimmedNote.isNotEmpty()) {
        append(trimmedNote)
        append(" · ")
    }
    append("Transfer · ")
    append(transferType)
    append(" · ")
    append(whenCode)
    append(" · to=")
    append(toAccountName)
}

fun buildSavingsDescription(
    note: String,
    goalName: String,
    frequency: String,
    whenCode: String,
): String = buildString {
    val trimmedNote = note.trim()
    if (trimmedNote.isNotEmpty()) {
        append(trimmedNote)
        append(" · ")
    }
    append(goalName)
    append(" · ")
    append(frequency)
    append(" · ")
    append(whenCode)
}