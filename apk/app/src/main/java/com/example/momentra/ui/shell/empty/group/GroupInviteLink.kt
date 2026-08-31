package com.example.momentra.ui.shell.empty.group

internal object GroupInviteLink {
    fun displayPath(code: String): String = "momentra.app/j/${code.trim().lowercase()}"

    fun copyText(code: String): String = displayPath(code)

    fun qrPayload(code: String): String = "momentra://j/${code.trim().lowercase()}"
}
