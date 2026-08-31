package com.example.momentra.data.auth

/**
 * Maps Firebase / Credential Manager failures to short, actionable copy.
 * Raw vendor codes remain available via analytics `error_code`.
 */
object AuthErrorMapper {

    fun userMessage(raw: String?): String {
        val message = raw?.trim().orEmpty()
        if (message.isEmpty()) return "Sign-in failed. Try again."

        val upper = message.uppercase()
        return when {
            upper.contains("BILLING_NOT_ENABLED") ->
                "Phone SMS requires a billed Firebase project (Blaze). Use email or Google for now, or enable billing in Firebase Console."

            upper.contains("28444") ||
                upper.contains("DEVELOPER CONSOLE IS NOT SET UP") ->
                "Google Sign-In isn’t set up for this build. In Firebase → Project settings → Your apps → Android, confirm SHA-1 0A:B8:5D:0A:13:32:83:FC:83:4E:84:43:65:A8:4B:4E:25:15:21:FD (and SHA-256), then download a fresh google-services.json. Also open Google Cloud → APIs & Services → Credentials and ensure an Android OAuth client exists for com.example.momentra."

            upper.contains("10.0.2.2") ||
                (upper.contains("FAILED TO CONNECT") && upper.contains("3000")) ->
                "Cannot reach the API. On a physical device set API_BASE_URL to your computer’s LAN IP (not 10.0.2.2)."

            upper.contains("NETWORK_UNAVAILABLE") ||
                upper.contains("UNABLE TO RESOLVE HOST") ||
                upper.contains("FAILED TO CONNECT") ->
                "Network unavailable. Check Wi‑Fi and that the Momentra API is running."

            upper.contains("ERROR_INVALID_EMAIL") ||
                upper.contains("THE EMAIL ADDRESS IS BADLY FORMATTED") ->
                "Enter a valid email address."

            upper.contains("ERROR_WRONG_PASSWORD") ||
                upper.contains("INVALID_LOGIN_CREDENTIALS") ||
                upper.contains("THE PASSWORD IS INVALID") ->
                "Incorrect email or password."

            upper.contains("ERROR_USER_NOT_FOUND") ->
                "No account found for that email. Register first."

            upper.contains("ERROR_EMAIL_ALREADY_IN_USE") ->
                "That email is already registered. Sign in instead."

            upper.contains("ERROR_WEAK_PASSWORD") ->
                "Password must be at least 6 characters."

            upper.contains("SIGN-IN CANCELLED") ||
                upper.contains("CANCELED") ||
                upper.contains("CANCELLED") ->
                "Sign-in cancelled."

            else -> message
        }
    }
}
