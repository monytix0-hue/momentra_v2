package com.example.momentra.ui.shell.empty.group

internal object GroupInviteLink {
    private const val HTTPS_BASE = "https://momentra.app/j"

    fun displayPath(code: String): String = "$HTTPS_BASE/${code.trim().lowercase()}"

    fun copyText(code: String): String = displayPath(code)

    /** Same HTTPS URL as share/copy so camera apps and messengers hit the invite landing. */
    fun qrPayload(code: String): String = displayPath(code)
}
