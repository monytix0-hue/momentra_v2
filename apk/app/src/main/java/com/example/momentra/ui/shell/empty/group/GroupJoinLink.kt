package com.example.momentra.ui.shell.empty.group

import android.net.Uri

/**
 * Parses a short join token from a QR payload, custom scheme, or https link.
 * Rejects JWTs and API/auth URLs — invite codes are opaque 8-character tokens.
 */
object GroupJoinLink {
    private val SHORT = Regex("^[a-hj-np-z2-9]{8}$", RegexOption.IGNORE_CASE)
    private val LEGACY = Regex("^[a-z0-9]+-[a-z0-9-]+-[a-f0-9]{8}$", RegexOption.IGNORE_CASE)
    private val JWTISH = Regex("""eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+""")

    fun parse(uri: Uri?): String? = parse(uri?.toString())

    fun parse(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val trimmed = raw.trim()
        if (JWTISH.containsMatchIn(trimmed) || trimmed.count { it == '.' } >= 2 && trimmed.length > 60) {
            return null
        }
        extractFromSlashes(trimmed)?.let { return it }

        val uri = runCatching { Uri.parse(trimmed) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase().orEmpty()
        val host = uri.host?.lowercase().orEmpty()
        val segments = uri.pathSegments
        val candidate = when {
            scheme == "momentra" && (host == "j" || host == "join") ->
                segments.lastOrNull() ?: uri.getQueryParameter("code") ?: host.takeIf { it != "j" && it != "join" }
            (host == "momentra.app" || host == "www.momentra.app") &&
                segments.isNotEmpty() && (segments[0].equals("j", true) || segments[0].equals("join", true)) ->
                segments.getOrNull(1)
            scheme == "momentra" && segments.getOrNull(0).equals("j", true) ->
                segments.getOrNull(1)
            scheme == "momentra" && segments.getOrNull(0).equals("join", true) ->
                segments.getOrNull(1)
            else -> uri.lastPathSegment
        }
        return sanitize(candidate) ?: extractFromSlashes(trimmed)
    }

    private fun extractFromSlashes(raw: String): String? {
        val parts = raw.substringBefore('?').substringBefore('#').split('/')
        val marker = parts.indexOfFirst { it.equals("j", true) || it.equals("join", true) }
        if (marker >= 0) return sanitize(parts.getOrNull(marker + 1))
        return sanitize(parts.lastOrNull())
    }

    private fun sanitize(raw: String?): String? {
        val value = raw?.trim()?.lowercase()?.trim('/') ?: return null
        return value.takeIf { SHORT.matches(it) || LEGACY.matches(it) }
    }
}
