package com.example.momentra.ui.shell.empty.group

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

fun inviteMessage(title: String, url: String): String {
    val label = title.trim().ifBlank { "this Moment" }
    return "Join $label on Momentra: $url"
}

fun invitePhoneDigits(phone: String?): String? {
    if (phone.isNullOrBlank()) return null
    val digits = phone.filter { it.isDigit() }
    return digits.takeIf { it.length >= 8 }
}

fun looksLikeInvitePhone(raw: String?): Boolean {
    if (raw.isNullOrBlank()) return false
    val t = raw.trim()
    if (t.contains('@')) return false
    return t.any { it.isDigit() } && invitePhoneDigits(t) != null
}

fun shareInviteLink(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, url)
    }
    context.startActivity(Intent.createChooser(intent, "Share invite link"))
}

/** Opens the SMS compose UI with a prefilled body (and optional recipient). */
fun sendInviteSms(context: Context, phone: String?, message: String) {
    val digits = invitePhoneDigits(phone)
    val uri = if (digits != null) {
        Uri.parse("smsto:$digits")
    } else {
        Uri.parse("smsto:")
    }
    val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
        putExtra("sms_body", message)
    }
    runCatching {
        context.startActivity(intent)
    }.onFailure {
        shareInviteLink(context, message)
    }
}

/**
 * Opens WhatsApp with a prefilled invite message.
 * Prefer `wa.me` when a phone is known; otherwise open WhatsApp share / system share fallback.
 */
fun sendInviteWhatsApp(context: Context, phone: String?, message: String) {
    val encoded = URLEncoder.encode(message, StandardCharsets.UTF_8.toString())
    val digits = invitePhoneDigits(phone)
    val candidates = buildList {
        if (digits != null) {
            add(Uri.parse("https://wa.me/$digits?text=$encoded"))
            add(Uri.parse("whatsapp://send?phone=$digits&text=$encoded"))
        }
        add(Uri.parse("https://api.whatsapp.com/send?text=$encoded"))
        add(Uri.parse("whatsapp://send?text=$encoded"))
    }
    for (uri in candidates) {
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val resolved = intent.resolveActivity(context.packageManager) != null
        if (resolved) {
            runCatching {
                context.startActivity(intent)
                return
            }
        }
    }
    // WhatsApp not installed — fall back to system share of the message text.
    shareInviteLink(context, message)
}

fun shareInviteQr(context: Context, bitmap: Bitmap, inviteUrl: String) {
    val file = File(context.cacheDir, "momentra-invite-qr.png")
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, inviteUrl)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share QR"))
}

fun saveInviteQrToPhotos(context: Context, bitmap: Bitmap): Boolean {
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "momentra-invite-qr.png")
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Momentra")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        ?: return false
    context.contentResolver.openOutputStream(uri)?.use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    } ?: return false
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        context.contentResolver.update(uri, values, null, null)
    }
    return true
}
