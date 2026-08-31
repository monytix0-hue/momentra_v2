package com.example.momentra.ui.shell.empty.business

import android.net.Uri

/**
 * Company invite deeplinks — `momentra://c/{code}` / `momentra.app/c/{code}`.
 * Bare codes are only accepted via [parseTyped] (Company Setup text entry).
 */
object CompanyJoinLink {
    private val SHORT = Regex("^[a-hj-np-z2-9]{8}$", RegexOption.IGNORE_CASE)

    fun displayPath(code: String): String =
        "momentra.app/c/${code.trim().lowercase()}"

    fun qrPayload(code: String): String =
        "momentra://c/${code.trim().lowercase()}"

    fun parse(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val trimmed = raw.trim()
        extractFromSlashes(trimmed)?.let { return it }

        val uri = runCatching { Uri.parse(trimmed) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase().orEmpty()
        val host = uri.host?.lowercase().orEmpty()
        val segments = uri.pathSegments
        val candidate = when {
            scheme == "momentra" && (host == "c" || host == "company") ->
                segments.lastOrNull()
            (host == "momentra.app" || host == "www.momentra.app") &&
                segments.isNotEmpty() &&
                (segments[0].equals("c", true) || segments[0].equals("company", true)) ->
                segments.getOrNull(1)
            scheme == "momentra" &&
                (segments.getOrNull(0).equals("c", true) || segments.getOrNull(0).equals("company", true)) ->
                segments.getOrNull(1)
            else -> null
        }
        return sanitize(candidate)
    }

    fun parseTyped(raw: String?): String? =
        parse(raw) ?: sanitize(raw?.trim()?.lowercase())

    private fun extractFromSlashes(raw: String): String? {
        val parts = raw.substringBefore('?').substringBefore('#').split('/')
        val marker = parts.indexOfFirst { it.equals("c", true) || it.equals("company", true) }
        if (marker >= 0) return sanitize(parts.getOrNull(marker + 1))
        return null
    }

    private fun sanitize(raw: String?): String? {
        val value = raw?.trim()?.lowercase()?.trim('/') ?: return null
        return value.takeIf { SHORT.matches(it) }
    }
}

sealed class InviteJoinKind {
    data class Group(val code: String) : InviteJoinKind()
    data class Company(val code: String) : InviteJoinKind()
}

object InviteJoinLink {
    fun parse(raw: String?): InviteJoinKind? {
        CompanyJoinLink.parse(raw)?.let { return InviteJoinKind.Company(it) }
        com.example.momentra.ui.shell.empty.group.GroupJoinLink.parse(raw)?.let {
            return InviteJoinKind.Group(it)
        }
        return null
    }
}
