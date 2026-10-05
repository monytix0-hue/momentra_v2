package com.example.momentra.ui.shell.shared

/**
 * Visible name for a person. A real email may be shown separately as contact text.
 * Never print a user id, an id prefix, or the synthetic sign-in address as a name.
 */
object PersonLabel {
    private const val PLACEHOLDER_EMAIL_SUFFIX = "@users.momentra.local"
    private val UUID = Regex(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
    )

    fun personName(displayName: String?, fallback: String = "Member"): String {
        val name = displayName?.trim().orEmpty()
        if (name.isEmpty() || isPlaceholder(name)) return fallback
        return name
    }

    /** Signed-in account line. Empty when there is no display name. */
    fun accountName(displayName: String?): String = personName(displayName, fallback = "")

    fun contactEmail(email: String?): String? {
        val value = email?.trim().orEmpty()
        if (value.isEmpty() || isPlaceholder(value)) return null
        return value
    }

    private fun isPlaceholder(value: String): Boolean {
        if (value.endsWith(PLACEHOLDER_EMAIL_SUFFIX, ignoreCase = true)) return true
        return UUID.matches(value)
    }
}
