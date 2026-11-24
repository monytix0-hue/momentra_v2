package com.example.momentra.ui.shell.business.setup

private const val SETUP_SUFFIX = " setup"

object BusinessSetupHeroTitle {
    fun format(name: String?, placeholder: String): String {
        val trimmed = name?.trim().orEmpty()
        if (trimmed.isBlank()) return placeholder
        if (trimmed.endsWith(SETUP_SUFFIX, ignoreCase = true)) {
            val prefix = trimmed.dropLast(SETUP_SUFFIX.length).trimEnd()
            return "${prefix.replaceFirstChar { it.titlecase() }} Setup"
        }
        return "$trimmed Setup"
    }
}
