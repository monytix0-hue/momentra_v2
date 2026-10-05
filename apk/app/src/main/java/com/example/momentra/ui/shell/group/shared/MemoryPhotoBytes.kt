package com.example.momentra.ui.shell.group.shared

import android.content.ContentResolver
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import java.io.ByteArrayOutputStream

/**
 * Always decode with EXIF respect, then compress upright JPEG.
 * Never upload raw URI bytes (those keep sideways sensor pixels + Orientation tag,
 * or lose the tag after a bad decode and stay sideways forever).
 */
fun encodeMemoryPhotoBytes(
    resolver: ContentResolver,
    photoUri: Uri?,
    photoBitmap: Bitmap?,
): ByteArray? {
    // Prefer already-decoded upright bitmap from the picker/camera path.
    photoBitmap?.let { bmp ->
        val encoded = compressUprightJpeg(bmp)
        if (encoded != null) return encoded
    }
    if (photoUri == null) return null
    return runCatching {
        val raw = resolver.openInputStream(photoUri)?.use { it.readBytes() } ?: return@runCatching null
        val upright = decodeBitmapRespectingExif(raw) ?: return@runCatching null
        compressUprightJpeg(upright)
    }.getOrNull()
}

private fun compressUprightJpeg(bmp: Bitmap): ByteArray? {
    return ByteArrayOutputStream().use { out ->
        val ok = bmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
        if (ok) out.toByteArray().takeIf { it.isNotEmpty() } else null
    }
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
