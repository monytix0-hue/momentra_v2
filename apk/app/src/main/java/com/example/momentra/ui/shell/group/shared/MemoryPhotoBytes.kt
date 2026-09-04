package com.example.momentra.ui.shell.group.shared

import android.content.ContentResolver
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import java.io.ByteArrayOutputStream

/** Prefer in-memory bitmap (survives lost URI grants); fall back to content URI. */
fun encodeMemoryPhotoBytes(
    resolver: ContentResolver,
    photoUri: Uri?,
    photoBitmap: Bitmap?,
): ByteArray? {
    photoBitmap?.let { bmp ->
        val encoded = ByteArrayOutputStream().use { out ->
            val ok = bmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
            if (ok) out.toByteArray() else null
        }
        if (encoded != null && encoded.isNotEmpty()) return encoded
    }
    if (photoUri == null) return null
    return runCatching {
        resolver.openInputStream(photoUri)?.use { it.readBytes() }
    }.getOrNull()?.takeIf { it.isNotEmpty() }
}

/** Best-effort persistable grant so gallery URIs remain readable after the picker closes. */
fun tryTakePersistableReadPermission(resolver: ContentResolver, uri: Uri) {
    runCatching {
        resolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
    }
}
