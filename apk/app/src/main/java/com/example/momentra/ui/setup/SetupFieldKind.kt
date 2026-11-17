package com.example.momentra.ui.setup

data class SetupChipOption(
    val value: String,
    val label: String,
    val emoji: String? = null,
)

enum class SetupFieldKind {
    TEXT,
    CHIPS,
    DROPDOWN,
    TOGGLE,
    DATE,
    DATETIME,
    TIME,
    TITLE,
}
